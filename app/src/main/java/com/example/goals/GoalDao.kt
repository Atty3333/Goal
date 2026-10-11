package com.example.goals

import android.R
import androidx.room.*

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals")
    fun getAllGoals(): List<Goal>

    @Query("SELECT * FROM goals WHERE id = :id")
    fun getGoalById(id: Int): Goal?

    @Query("SELECT * FROM goals WHERE itemType = :type")
    fun getItemsByType(type: GoalType): List<Goal>

    @Query("SELECT * FROM goals WHERE itemType = :type AND linkedGoalId = :parentGoalId")
    fun getTasksForGoal(parentGoalId: Int, type: GoalType): List<Goal>

    @Query("SELECT * FROM goals WHERE isAchieved = 1")
    fun getCompletedItems(): List<Goal>

    @Query("SELECT * FROM goals WHERE isAchieved = 0 AND endDate IS NOT NULL AND endDate < :currentTimeMil")
    fun getOverdueItems(currentTimeMil: Long): List<Goal>

    @Query("SELECT * FROM goals WHERE isAchieved = 0 AND startDate > :currentTimeMil")
    fun getUpcomingItems(currentTimeMil: Long): List<Goal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertGoal(goal: Goal): Long

    @Update
    fun updateGoal(goal: Goal)

    @Delete
    fun deleteGoal(goal: Goal)
}
