package com.example.goals

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum  class GoalType { GOAL, HABIT, TASK }
enum class HabitFrequency { DAILY, INTERVAL, SOME_DAYS_PERIOD, SPECIFIC_DAYS }

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var title: String,
    var description: String = "",
    var notes: String = "",
    var itemType: GoalType = GoalType.GOAL,
    var priority: String = "Medium",
    var isAchieved: Boolean = false,

    var startDate: Long = System.currentTimeMillis(),
    var endDate: Long? = null,
    var durationText: String = "",

    var category: String = "General",
    var subtasks: List<String> = emptyList(),
    
    var completedSteps: Int = 0,
    var currentStreak: Int = 0,
    var linkedGoalId: Int? = null,
    var frequency: HabitFrequency = HabitFrequency.DAILY
) {
    val progressPercentage: Int
        @Ignore
        get() {
            if (subtasks.isEmpty()) return if (isAchieved) 100 else 0
            return ((completedSteps.toDouble() / subtasks.size.toDouble()) * 100).toInt().coerceIn(0, 100)
        }
}

class AppTypeConverters {
    @TypeConverter
    fun fromSubtaskList(subtasks: List<String>): String = subtasks.joinToString(separator = "|||")

    @TypeConverter
    fun toSubtaskList(flatString: String): List<String> {
        if (flatString.isBlank()) return emptyList()
        return flatString.split("|||")
    }

    @TypeConverter
    fun fromGoalType(type: GoalType): String = type.name

    @TypeConverter
    fun toGoalType(name: String): GoalType = GoalType.valueOf(name)

    @TypeConverter
    fun fromFrequency(freq: HabitFrequency): String = freq.name

    @TypeConverter
    fun toFrequency(name: String): HabitFrequency = HabitFrequency.valueOf(name)
}
