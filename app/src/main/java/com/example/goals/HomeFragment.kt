package com.example.goals

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeFragment : Fragment() {

    private lateinit var rvGoals: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var tvCurrentDate: TextView
    private lateinit var tvTodayHeader: TextView
    private lateinit var btnAddGoal: FloatingActionButton
    private lateinit var goalAdapter: GoalAdapter
    private lateinit var dateHeaderContainer: View

    private var currentSelectedDate = Calendar.getInstance()

    private var isGoalsExpanded = true
    private var isHabitsExpanded = true
    private var isTasksExpanded = true

    private var rawDatabaseGoalsList = listOf<Goal>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.home_fragment, container, false)

        rvGoals = view.findViewById(R.id.rvGoals)
        tvEmptyState = view.findViewById(R.id.tvHomeEmptyState)
        tvCurrentDate = view.findViewById(R.id.tvCurrentDate)
        tvTodayHeader = view.findViewById(R.id.tvTodayHeader)
        dateHeaderContainer = view.findViewById(R.id.dateHeaderContainer)

        btnAddGoal = requireActivity().findViewById(R.id.btnAddGoal)

        setupCalendarHeaderDisplay()
        setupDateNavigationPipeline()
        setupRecyclerViewPipeline()

        btnAddGoal.setOnClickListener { buttonView ->
            showCreationPopupMenu(buttonView)
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        refreshDatabasePayload()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::btnAddGoal.isInitialized) {
            btnAddGoal.setOnClickListener(null)
        }
    }

    private fun setupCalendarHeaderDisplay() {
        val format = SimpleDateFormat("MMM dd • EEEE", Locale.getDefault())
        tvCurrentDate.text = format.format(currentSelectedDate.time)

        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }

        val isSameYearAsToday = currentSelectedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR)

        tvTodayHeader.text = when {
            isSameYearAsToday && currentSelectedDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> {
                "Today"
            }
            currentSelectedDate.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) && currentSelectedDate.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> {
                "Yesterday"
            }
            currentSelectedDate.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR) && currentSelectedDate.get(Calendar.DAY_OF_YEAR) == tomorrow.get(Calendar.DAY_OF_YEAR) -> {
                "Tomorrow"
            }
            else -> {
                SimpleDateFormat("EEEE", Locale.getDefault()).format(currentSelectedDate.time)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDateNavigationPipeline() {
        val gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y

                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            shiftActiveTargetDate(-1) // Swipe Right (Go to Yesterday)
                        } else {
                            shiftActiveTargetDate(1)  // Swipe Left (Go to Tomorrow)
                        }
                        return true
                    }
                }
                return false
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                showDatePickerDialog()
                return true
            }
        })

        dateHeaderContainer.setOnTouchListener { v, event ->
            if (gestureDetector.onTouchEvent(event)) {
                return@setOnTouchListener true
            }
            if (event.action == MotionEvent.ACTION_UP) {
                v.performClick()
            }
            true
        }
    }

    private fun shiftActiveTargetDate(daysAmount: Int) {
        currentSelectedDate.add(Calendar.DAY_OF_YEAR, daysAmount)
        setupCalendarHeaderDisplay()
        refreshDatabasePayload()
    }

    private fun showDatePickerDialog() {
        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                currentSelectedDate.set(Calendar.YEAR, year)
                currentSelectedDate.set(Calendar.MONTH, month)
                currentSelectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                setupCalendarHeaderDisplay()
                refreshDatabasePayload()
            },
            currentSelectedDate.get(Calendar.YEAR),
            currentSelectedDate.get(Calendar.MONTH),
            currentSelectedDate.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun showCreationPopupMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add(0, 1, 0, "🎯 New Goal")
        popup.menu.add(0, 2, 1, "🔄 New Habit")
        popup.menu.add(0, 3, 2, "✅ New Task")

        popup.setOnMenuItemClickListener { menuItem ->
            val appContext = requireContext().applicationContext
            when (menuItem.itemId) {
                1 -> {
                    val intent = Intent(requireContext(), AddGoalActivity::class.java).apply {
                        putExtra("DEFAULT_TYPE", "GOAL")
                    }
                    startActivity(intent)
                }
                2 -> {
                    val intent = Intent(requireContext(), AddHabitActivity::class.java).apply {
                        putExtra("DEFAULT_TYPE", "HABIT")
                    }
                    startActivity(intent)
                }
                3 -> {
                    val bottomSheet = AddTaskBottomSheet(
                        onTaskSaved = { title, category, priority, notes, dateMs, linkedGoalPayload ->
                            kotlin.concurrent.thread {
                                val newTask = Goal(
                                    title = title,
                                    category = category,
                                    priority = priority,
                                    notes = notes,
                                    startDate = dateMs,
                                    endDate = dateMs,
                                    itemType = GoalType.TASK,
                                    durationText = linkedGoalPayload ?: "",
                                    isAchieved = false
                                )
                                AppDatabase.getDatabase(appContext).goalDao().insertGoal(newTask)
                                refreshDatabasePayload()
                            }
                        }
                    )
                    bottomSheet.show(childFragmentManager, "AddTaskBottomSheet")
                }
            }
            true
        }
        popup.show()
    }

    private fun setupRecyclerViewPipeline() {
        rvGoals.layoutManager = LinearLayoutManager(requireContext())

        goalAdapter = GoalAdapter(
            items = emptyList(),
            viewTypeMode = GoalAdapter.MODE_CARD_BLOCK,
            onGoalStatusChanged = { updatedItem ->
                val appContext = requireContext().applicationContext
                kotlin.concurrent.thread {
                    AppDatabase.getDatabase(appContext).goalDao().updateGoal(updatedItem)
                    refreshDatabasePayload()
                }
            },
            onGoalEditRequested = { targetEdit ->
                val appContext = requireContext().applicationContext
                if (targetEdit.itemType == GoalType.TASK) {
                    val bottomSheet = AddTaskBottomSheet(
                        existingTask = targetEdit,
                        onTaskSaved = { title, category, priority, notes, dateMs, linkedGoalPayload ->
                            kotlin.concurrent.thread {
                                val updatedTask = targetEdit.copy(
                                    title = title,
                                    category = category,
                                    priority = priority,
                                    notes = notes,
                                    startDate = dateMs,
                                    endDate = dateMs,
                                    durationText = linkedGoalPayload ?: ""
                                )
                                AppDatabase.getDatabase(appContext).goalDao().updateGoal(updatedTask)
                                refreshDatabasePayload()
                            }
                        }
                    )
                    bottomSheet.show(childFragmentManager, "AddTaskBottomSheet")
                } else {
                    val intent = Intent(requireContext(), AddHabitActivity::class.java).apply {
                        putExtra("GOAL_ID", targetEdit.id)
                    }
                    startActivity(intent)
                }
            },
            onGoalDeleteRequested = { targetDelete ->
                val appContext = requireContext().applicationContext
                kotlin.concurrent.thread {
                    AppDatabase.getDatabase(appContext).goalDao().deleteGoal(targetDelete)
                    refreshDatabasePayload()
                }
            },
            onGoalDetailsRequested = { targetGoal ->
                val intent = Intent(requireContext(), GoalDetailsActivity::class.java).apply {
                    putExtra("GOAL_ID", targetGoal.id)
                    putExtra("GOAL_TYPE", targetGoal.itemType.name)
                }
                startActivity(intent)
            },
            onHeaderClicked = { headerText ->
                when (headerText.trim().uppercase()) {
                    "GOALS" -> isGoalsExpanded = !isGoalsExpanded
                    "HABITS" -> isHabitsExpanded = !isHabitsExpanded
                    "TASKS" -> isTasksExpanded = !isTasksExpanded
                }
                compileAndRebuildDisplayData()
            }
        )
        rvGoals.adapter = goalAdapter
    }

    private fun refreshDatabasePayload() {
        val appContext = context?.applicationContext ?: return
        kotlin.concurrent.thread {
            val databaseItems = AppDatabase.getDatabase(appContext).goalDao().getAllGoals()
            activity?.runOnUiThread {
                rawDatabaseGoalsList = databaseItems
                compileAndRebuildDisplayData()
            }
        }
    }

    private fun compileAndRebuildDisplayData() {
        if (rawDatabaseGoalsList.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            rvGoals.visibility = View.GONE
            goalAdapter.updateData(emptyList())
            return
        }

        tvEmptyState.visibility = View.GONE
        rvGoals.visibility = View.VISIBLE

        // 1. Calculate boundaries for the active calendar selection
        val dayBounds = Calendar.getInstance().apply {
            timeInMillis = currentSelectedDate.timeInMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = dayBounds.timeInMillis

        dayBounds.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val endOfDay = dayBounds.timeInMillis

        // 2. The Smart Filtering Engine
        val itemsForToday = rawDatabaseGoalsList.filter { item ->
            val itemStart = item.startDate

            // RULE A: Time Travel Check - Items cannot appear before their creation/start date
            if (endOfDay < itemStart) return@filter false

            // RULE B: Parent Dependency Auto-Archive Check
            var isEffectivelyAchieved = item.isAchieved
            val payloadText = item.durationText ?: ""

            if (payloadText.contains("ParentGoalId:", ignoreCase = true)) {
                val parentIdStr = payloadText.substringAfter("ParentGoalId:").substringBefore("|").trim()
                val parentId = parentIdStr.toIntOrNull()
                if (parentId != null) {
                    val parentGoal = rawDatabaseGoalsList.find { it.id == parentId }
                    if (parentGoal?.isAchieved == true) {
                        isEffectivelyAchieved = true
                    }
                }
            }

            // RULE C: Archive/Achieved State Roll-Over Stop
            // If achieved (or parent achieved), it stops rolling forward into future days.
            // We only show it securely on the EXACT day it started so it isn't erased from history.
            if (isEffectivelyAchieved) {
                val isSameDayAsStart = startOfDay <= itemStart && endOfDay >= itemStart
                if (!isSameDayAsStart) return@filter false
            }

            // RULE D: End Date Boundary Restraint
            val hasValidEndDate = item.endDate != null && item.endDate!! > 0L
            if (hasValidEndDate) {
                if (startOfDay > item.endDate!!) return@filter false
            }

// RULE E: Habit Recurrence Matcher
            if (item.itemType == GoalType.HABIT) {
                // Standardize both timestamps to midnight/zero-hours for safe day-count math
                val calStart = Calendar.getInstance().apply {
                    timeInMillis = itemStart
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val calTarget = Calendar.getInstance().apply {
                    timeInMillis = startOfDay
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                when (item.frequency) {
                    HabitFrequency.DAILY -> {
                        // Show every day within its start/end window (Passed by default)
                    }

                    HabitFrequency.INTERVAL -> {
                        // Matches: "Frequency: Every X days/weeks/months"
                        val freqMatch = Regex("Frequency:\\s*Every\\s+(\\d+)\\s+(days?|weeks?|months?)", RegexOption.IGNORE_CASE).find(payloadText)
                        if (freqMatch != null) {
                            val num = freqMatch.groupValues[1].toIntOrNull() ?: 1
                            val unit = freqMatch.groupValues[2].lowercase()

                            when {
                                unit.startsWith("day") -> {
                                    val diffDays = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(calTarget.timeInMillis - calStart.timeInMillis)
                                    if (diffDays % num != 0L) return@filter false
                                }
                                unit.startsWith("week") -> {
                                    val diffDays = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(calTarget.timeInMillis - calStart.timeInMillis)
                                    if (diffDays % 7 != 0L) return@filter false // Fall on the exact matching day-of-week
                                    val diffWeeks = diffDays / 7
                                    if (diffWeeks % num != 0L) return@filter false
                                }
                                unit.startsWith("month") -> {
                                    if (calTarget.get(Calendar.DAY_OF_MONTH) != calStart.get(Calendar.DAY_OF_MONTH)) return@filter false
                                    val monthDiff = (calTarget.get(Calendar.YEAR) - calStart.get(Calendar.YEAR)) * 12 +
                                            (calTarget.get(Calendar.MONTH) - calStart.get(Calendar.MONTH))
                                    if (monthDiff % num != 0) return@filter false
                                }
                            }
                        }
                    }

                    HabitFrequency.SOME_DAYS_PERIOD -> {
                        // Flexible Quotas (e.g., "4 days per week")
                        // Stays visible every day so the user can manually select any day to log progress.
                    }

                    HabitFrequency.SPECIFIC_DAYS -> {
                        // 1. Get the saved string from the database
                        val payloadText = item.durationText ?: ""

                        // 2. Format the currently selected Home screen calendar day to match the saved format exactly
                        val targetDateString = SimpleDateFormat("MMMM d yyyy", Locale.US).format(calTarget.time)

                        // 3. Fallback format just in case
                        val fallbackDateString = SimpleDateFormat("MMM d yyyy", Locale.US).format(calTarget.time)

                        // 4. Check if the saved text contains today's date
                        val isDateExplicitlyTargeted = payloadText.contains(targetDateString, ignoreCase = true) ||
                                payloadText.contains(fallbackDateString, ignoreCase = true)

                        // 5. If today's date is NOT in the string, hide the habit
                        if (!isDateExplicitlyTargeted) {
                            return@filter false
                        }
                    }
                }
            }

            // If it passed all restraints, render it!
            true
        }

        // 3. Rebuild Display Groups
        val compositeFlattenedList = mutableListOf<Any>()

        val goals = itemsForToday.filter { it.itemType == GoalType.GOAL }
        if (goals.isNotEmpty()) {
            compositeFlattenedList.add("GOALS")
            if (isGoalsExpanded) compositeFlattenedList.addAll(goals)
        }

        val habits = itemsForToday.filter { it.itemType == GoalType.HABIT }
        if (habits.isNotEmpty()) {
            compositeFlattenedList.add("HABITS")
            if (isHabitsExpanded) compositeFlattenedList.addAll(habits)
        }

        val tasks = itemsForToday.filter { it.itemType == GoalType.TASK }
        if (tasks.isNotEmpty()) {
            compositeFlattenedList.add("TASKS")
            if (isTasksExpanded) compositeFlattenedList.addAll(tasks)
        }

        goalAdapter.updateData(compositeFlattenedList)
    }
}