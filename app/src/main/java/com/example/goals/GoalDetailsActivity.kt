package com.example.goals

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Locale

class GoalDetailsActivity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var tvCategory: TextView
    private lateinit var tvPriority: TextView
    private lateinit var tvItemType: TextView
    private lateinit var tvDates: TextView
    private lateinit var tvPayload: TextView
    private lateinit var tvNotes: TextView

    private lateinit var llProgressSection: LinearLayout
    private lateinit var pbTimeline: ProgressBar
    private lateinit var tvProgressText: TextView
    private lateinit var btnUpdateProgress: Button

    private lateinit var cvSubtasksCard: com.google.android.material.card.MaterialCardView
    private lateinit var tlSubtasksContainer: TableLayout

    private lateinit var btnToggleAchieved: Button
    private lateinit var btnEditItem: Button

    private var goalId: Int = 0
    private var currentGoal: Goal? = null
    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goal_details)

        goalId = intent.getIntExtra("GOAL_ID", 0)
        if (goalId == 0) {
            Toast.makeText(this, "Invalid Item ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeViews()
    }

    override fun onResume() {
        super.onResume()
        loadGoalData()
    }

    private fun initializeViews() {
        tvTitle = findViewById(R.id.tvDetailTitle)
        tvCategory = findViewById(R.id.tvDetailCategory)
        tvPriority = findViewById(R.id.tvDetailPriority)
        tvItemType = findViewById(R.id.tvDetailItemType)
        tvDates = findViewById(R.id.tvDetailDates)
        tvPayload = findViewById(R.id.tvDetailPayload)
        tvNotes = findViewById(R.id.tvDetailNotes)

        llProgressSection = findViewById(R.id.llProgressSection)
        pbTimeline = findViewById(R.id.pbDetailTimeline)
        tvProgressText = findViewById(R.id.tvDetailProgressText)
        btnUpdateProgress = findViewById(R.id.btnUpdateProgressDialog)

        cvSubtasksCard = findViewById(R.id.cvSubtasksCard)
        tlSubtasksContainer = findViewById(R.id.tlDetailSubtasksContainer)

        btnToggleAchieved = findViewById(R.id.btnToggleAchieved)
        btnEditItem = findViewById(R.id.btnEditItem)

        findViewById<ImageView>(R.id.ivDetailsBack).setOnClickListener { finish() }

        btnUpdateProgress.setOnClickListener { executeAddStepIncrement() }

        btnToggleAchieved.setOnClickListener {
            currentGoal?.let { goal ->
                val newStatus = !goal.isAchieved
                if (newStatus && goal.itemType == GoalType.GOAL) {
                    showCascadeArchiveConfirmation(goal)
                } else {
                    showCascadeArchiveConfirmation(goal)
                    goal.isAchieved = newStatus
                    saveGoalAndRefresh(goal)
                }
            }
        }

        btnEditItem.setOnClickListener {
            currentGoal?.let { goal ->
                val intent = when (goal.itemType) {
                    GoalType.GOAL -> Intent(this, AddGoalActivity::class.java).putExtra("GOAL_ID", goal.id)
                    GoalType.HABIT -> Intent(this, AddHabitActivity::class.java).putExtra("GOAL_ID", goal.id)
                    GoalType.TASK -> Intent(this, AddTaskBottomSheet::class.java).putExtra("GOAL_ID", goal.id)
                }
                startActivity(intent)
            }
        }
    }

    private fun loadGoalData() {
        kotlin.concurrent.thread {
            val db = AppDatabase.getDatabase(this)
            val goal = db.goalDao().getGoalById(goalId)
            runOnUiThread {
                if (goal != null) {
                    currentGoal = goal
                    updateUIState(goal)
                } else {
                    Toast.makeText(this, "Item not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun updateUIState(goal: Goal) {
        tvTitle.text = goal.title.uppercase()
        tvCategory.text = goal.category.uppercase()
        tvPriority.text = goal.priority.uppercase()
        tvItemType.text = goal.itemType.name.uppercase()
        tvNotes.text = if (goal.description.isNotEmpty()) goal.description else "No additional notes provided."

        val startStr = dateFormatter.format(goal.startDate)
        val endStr = if (goal.endDate != null && goal.endDate!! > 0L) dateFormatter.format(goal.endDate!!) else "No Deadline"
        tvDates.text = "Start: $startStr | End: $endStr"

        tvPayload.text = if (goal.durationText.isNotEmpty()) goal.durationText else "No specific payload parameters."
        btnToggleAchieved.text = if (goal.isAchieved) "UNDO" else "DONE"

        kotlin.concurrent.thread {
            val dao = AppDatabase.getDatabase(this).goalDao()
            val allItems = dao.getAllGoals()

            val hasValidEndDate = goal.endDate != null && goal.endDate!! > 0L
            val targetValue = extractTargetValue(goal.durationText)

            val isLinked = when (goal.itemType) {
                GoalType.GOAL -> allItems.any { it.itemType == GoalType.HABIT && isItemLinkedToTarget(it, goal) }
                GoalType.HABIT -> isHabitLinkedToGoal(goal, allItems)
                GoalType.TASK -> false
            }

            val showProgress = hasValidEndDate || (targetValue != null && targetValue > 0 && isLinked)

            val showAddStep = (goal.itemType == GoalType.GOAL || goal.itemType == GoalType.HABIT) &&
                    !goal.isAchieved && targetValue != null && targetValue > 0 && isLinked

            val linkedTasks = if (goal.itemType == GoalType.GOAL) {
                allItems.filter { it.itemType == GoalType.TASK && isItemLinkedToTarget(it, goal) }
            } else {
                emptyList()
            }

            runOnUiThread {
                if (showProgress) {
                    llProgressSection.visibility = View.VISIBLE
                    calculateAndDisplayProgress(goal, targetValue)
                } else {
                    llProgressSection.visibility = View.GONE
                }

                if (showAddStep) {
                    btnUpdateProgress.visibility = View.VISIBLE
                } else {
                    btnUpdateProgress.visibility = View.GONE
                }
                if(goal.isAchieved){
                    btnUpdateProgress.isEnabled = false
                }
                else {
                    btnUpdateProgress.isEnabled = true
                }

                if (goal.itemType == GoalType.GOAL) {
                    cvSubtasksCard.visibility = View.VISIBLE
                    rebuildSubtasksList(goal, linkedTasks)
                } else {
                    cvSubtasksCard.visibility = View.GONE
                }
            }
        }

        checkAutoArchiveTriggers(goal)
    }

    private fun extractTargetValue(durationText: String?): Double? {
        if (durationText.isNullOrEmpty()) return null
        durationText.split("|").forEach { token ->
            val trimmed = token.trim()
            if (trimmed.startsWith("TargetValue:", ignoreCase = true)) {
                return trimmed.substringAfter("TargetValue:").trim().toDoubleOrNull()
            }
        }
        return null
    }

    private fun isHabitLinkedToGoal(habit: Goal, allItems: List<Goal>): Boolean {
        val payload = habit.durationText
        return payload.split("|").any { token ->
            val trimmed = token.trim()
            if (trimmed.startsWith("ParentGoalId:", ignoreCase = true)) {
                val pIdStr = trimmed.substringAfter("ParentGoalId:").trim()
                val pId = pIdStr.toIntOrNull()
                allItems.any { (pId != null && it.id == pId) || it.title.equals(pIdStr, ignoreCase = true) }
            } else {
                false
            }
        }
    }

    private fun isItemLinkedToTarget(item: Goal, targetGoal: Goal): Boolean {
        val payload = item.durationText
        return payload.split("|").any { token ->
            val trimmed = token.trim()
            if (trimmed.startsWith("ParentGoalId:", ignoreCase = true)) {
                val pIdStr = trimmed.substringAfter("ParentGoalId:").trim()
                val pId = pIdStr.toIntOrNull()
                (pId != null && pId == targetGoal.id) || pIdStr.equals(targetGoal.title, ignoreCase = true)
            } else {
                false
            }
        }
    }

    private fun checkAutoArchiveTriggers(goal: Goal) {
        val currentTime = System.currentTimeMillis()
        if (goal.endDate != null && goal.endDate!! > 0L && currentTime >= goal.endDate!! && !goal.isAchieved) {
            goal.isAchieved = true
            saveGoalAndRefresh(goal)
        }
    }

    private fun rebuildSubtasksList(parentGoal: Goal, tasks: List<Goal>) {
        tlSubtasksContainer.removeAllViews()

        for (task in tasks) {
            val tableRow = TableRow(this)
            val checkBox = CheckBox(this).apply {
                text = task.title
                setTextColor(ContextCompat.getColor(this@GoalDetailsActivity, R.color.tiertiry))
                isChecked = task.isAchieved
                setOnCheckedChangeListener { _, isChecked ->
                    task.isAchieved = isChecked
                    kotlin.concurrent.thread {
                        AppDatabase.getDatabase(this@GoalDetailsActivity).goalDao().updateGoal(task)
                    }
                }
            }
            tableRow.addView(checkBox)
            tlSubtasksContainer.addView(tableRow)
        }
    }

    private fun saveGoalAndRefresh(goal: Goal) {
        kotlin.concurrent.thread {
            AppDatabase.getDatabase(this).goalDao().updateGoal(goal)
            runOnUiThread {
                updateUIState(goal)
            }
        }
    }

    private fun showCascadeArchiveConfirmation(goal: Goal) {
        AlertDialog.Builder(this)
            .setTitle("Archive Linked Items")
            .setMessage("Do you want to archive linked habits and tasks associated with this goal as well?")
            .setPositiveButton("Yes, Archive All") { _, _ ->
                goal.isAchieved = true
                kotlin.concurrent.thread {
                    val dao = AppDatabase.getDatabase(this).goalDao()
                    dao.updateGoal(goal)

                    val allItems = dao.getAllGoals()
                    allItems.forEach { item ->
                        if (isItemLinkedToTarget(item, goal)) {
                            item.isAchieved = true
                            dao.updateGoal(item)
                        }
                    }

                    runOnUiThread {
                        updateUIState(goal)
                        Toast.makeText(this, "Goal and linked items archived successfully", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Just This Goal") { _, _ ->
                goal.isAchieved = true
                saveGoalAndRefresh(goal)
            }
            .show()
    }

    private fun showCascadeinputConfirmation(goal: Goal){
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = "Enter target number"
        }

        AlertDialog.Builder(this)
            .setTitle("Enter Your New Goal Target")
            .setMessage("Please Enter your new goal target to undo the goal?")
            .setView(input)
            .setPositiveButton("Confirm") { _, _ ->
                val textValue = input.text.toString().trim()
                val numericValue = textValue.toDoubleOrNull() ?: 0.0
                val targetValue = numericValue
                // Handle your numericValue here
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    private fun executeAddStepIncrement() {
        val goal = currentGoal ?: return

        kotlin.concurrent.thread {
            val dao = AppDatabase.getDatabase(this).goalDao()
            val allItems = dao.getAllGoals()

            var incrementValue = 0.0
            var habitEntity: Goal? = null
            var parentGoalEntity: Goal? = null

            if (goal.itemType == GoalType.HABIT) {
                habitEntity = goal
                incrementValue = extractTargetValue(habitEntity.durationText) ?: 0.0

                val payload = habitEntity.durationText
                payload.split("|").forEach { token ->
                    val trimmed = token.trim()
                    if (trimmed.startsWith("ParentGoalId:", ignoreCase = true)) {
                        val pIdStr = trimmed.substringAfter("ParentGoalId:").trim()
                        val pId = pIdStr.toIntOrNull()
                        parentGoalEntity = allItems.find { (pId != null && it.id == pId) || it.title.equals(pIdStr, ignoreCase = true) }
                    }
                }
            } else if (goal.itemType == GoalType.GOAL) {
                parentGoalEntity = goal
                habitEntity = allItems.find { it.itemType == GoalType.HABIT && isItemLinkedToTarget(it, parentGoalEntity!!) }
                incrementValue = habitEntity?.let { extractTargetValue(it.durationText) } ?: extractTargetValue(parentGoalEntity!!.durationText) ?: 0.0
            }

            if (incrementValue <= 0.0) {
                runOnUiThread {
                    Toast.makeText(this, "No valid target value found from linked habit", Toast.LENGTH_SHORT).show()
                }
                return@thread
            }

            habitEntity?.let { habit ->
                val rawHabitPayload = habit.durationText
                val cleanHabitTokens = rawHabitPayload.split("|")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("CurrentProgress:", ignoreCase = true) }
                    .toMutableList()

                var habitProgress = 0.0
                rawHabitPayload.split("|").forEach { token ->
                    if (token.trim().startsWith("CurrentProgress:", ignoreCase = true)) {
                        habitProgress = token.substringAfter("CurrentProgress:").trim().toDoubleOrNull() ?: 0.0
                    }
                }

                val newHabitProgress = habitProgress + incrementValue
                cleanHabitTokens.add("CurrentProgress: $newHabitProgress")
                habit.durationText = cleanHabitTokens.joinToString(" | ")

                val habitTarget = extractTargetValue(habit.durationText) ?: 0.0

                if (habitTarget > 0 && newHabitProgress >= habitTarget) {
                    habit.isAchieved = true
                }
                dao.updateGoal(habit)
            }

            parentGoalEntity?.let { parent ->
                val rawParentPayload = parent.durationText
                val cleanParentTokens = rawParentPayload.split("|")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("CurrentProgress:", ignoreCase = true) }
                    .toMutableList()

                var parentProgress = 0.0
                rawParentPayload.split("|").forEach { token ->
                    if (token.trim().startsWith("CurrentProgress:", ignoreCase = true)) {
                        parentProgress = token.substringAfter("CurrentProgress:").trim().toDoubleOrNull() ?: 0.0
                    }
                }

                val newParentProgress = parentProgress + incrementValue
                cleanParentTokens.add("CurrentProgress: $newParentProgress")
                parent.durationText = cleanParentTokens.joinToString(" | ")

                val parentTarget = extractTargetValue(parent.durationText) ?: 0.0
                if (parentTarget > 0 && newParentProgress >= parentTarget) {
                    parent.isAchieved = true
                }
                dao.updateGoal(parent)
                if (parent.isAchieved){
                    showCascadeArchiveConfirmation(goal)
                }
            }

            val refreshedGoal = dao.getGoalById(goal.id)
            runOnUiThread {
                refreshedGoal?.let {
                    currentGoal = it
                    updateUIState(it)
                }
                Toast.makeText(this, "Step Added (+$incrementValue) & Synced!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun calculateAndDisplayProgress(goal: Goal, targetValue: Double?) {
        val payload = goal.durationText
        var currentProgress: Double? = null

        if (payload.isNotEmpty()) {
            payload.split("|").forEach { part ->
                val token = part.trim()
                if (token.startsWith("CurrentProgress:", ignoreCase = true)) {
                    currentProgress = token.substringAfter("CurrentProgress:").trim().toDoubleOrNull()
                }
            }
        }

        when {

            targetValue != null && targetValue > 0 -> {
                val progress = currentProgress ?: 0.0
                val percentage = ((progress / targetValue) * 100).toInt().coerceIn(0, 100)
                pbTimeline.progress = percentage
                tvProgressText.text = "Target Progress: $percentage% ($progress / $targetValue)"
            }
            else -> {
                val endDateValue = goal.endDate
                if (endDateValue != null && endDateValue > 0L) {
                    val totalDuration = endDateValue - goal.startDate
                    val currentTime = System.currentTimeMillis()
                    val percentage = when {
                        totalDuration <= 0 -> 100
                        currentTime < goal.startDate -> 0
                        else -> (((currentTime - goal.startDate).toDouble() / totalDuration.toDouble()) * 100).toInt().coerceIn(0, 100)
                    }
                    pbTimeline.progress = percentage
                    tvProgressText.text = "Timeline Progress: $percentage%"
                } else {
                    pbTimeline.progress = if (goal.isAchieved) 100 else 0
                    tvProgressText.text = if (goal.isAchieved) "Status: Completed (100%)" else "Status: In Progress"
                }
            }
        }
    }
}