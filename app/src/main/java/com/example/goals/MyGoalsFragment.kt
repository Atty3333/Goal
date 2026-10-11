package com.example.goals

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.concurrent.thread

class MyGoalsFragment(private val goalTypeStr: String) : Fragment(R.layout.my_goals_fragment) {

    private lateinit var myGoalsAdapter: GoalAdapter
    private val structuralPipelineList = mutableListOf<Any>()

    private lateinit var rvMyGoalsList: RecyclerView
    private lateinit var tvMyGoalsEmptyState: TextView
    private lateinit var tvMyGoalsHeader: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvMyGoalsList = view.findViewById(R.id.rvMyGoalsList)
        tvMyGoalsEmptyState = view.findViewById(R.id.tvMyGoalsEmptyState)

        // Dynamically update the header based on the active tab
        tvMyGoalsHeader = view.findViewById(R.id.tvMyGoalsHeader)
        tvMyGoalsHeader.text = when (goalTypeStr) {
            "habits" -> "My Habits"
            "tasks" -> "My Tasks"
            else -> "My Goals"
        }

        rvMyGoalsList.layoutManager = LinearLayoutManager(requireContext())

        // Configured to use MODE_CARD_BLOCK layout styles matching your cards archetype definition
        myGoalsAdapter = GoalAdapter(
            items = structuralPipelineList,
            viewTypeMode = GoalAdapter.MODE_HOME_CHECKBOX,
            onGoalStatusChanged = { item ->
                thread {
                    AppDatabase.getDatabase(requireContext()).goalDao().updateGoal(item)
                    refreshPipelineHub()
                }
            },
            onGoalEditRequested = { item ->
                // Handles direct task sheet presentation or goal routing parameters
            },
            onGoalDeleteRequested = { item ->
                thread {
                    AppDatabase.getDatabase(requireContext()).goalDao().deleteGoal(item)
                    refreshPipelineHub()
                }
            },
            onGoalDetailsRequested = { item ->
                val intent = Intent(requireContext(), GoalDetailsActivity::class.java).apply {
                    putExtra("GOAL_ID", item.id)
                }
                startActivity(intent)
            }
        )
        rvMyGoalsList.adapter = myGoalsAdapter

        refreshPipelineHub()
    }

    private fun refreshPipelineHub() {
        thread {
            // Map the passed string directly to the strict Enum type
            val targetType = when (goalTypeStr) {
                "habits" -> GoalType.HABIT
                "tasks" -> GoalType.TASK
                else -> GoalType.GOAL   
            }

            val allItems = AppDatabase.getDatabase(requireContext()).goalDao().getItemsByType(targetType)
            val now = System.currentTimeMillis()

            val doneBucket = allItems.filter { it.isAchieved }
            val activeItems = allItems.filter { !it.isAchieved }

            // 3-way split for active items utilizing both startDate and endDate parameters
            val overdueBucket = activeItems.filter { it.endDate != null && it.endDate!! < now }
            val upcomingBucket = activeItems.filter { it.startDate > now }
            val ongoingBucket = activeItems.filter {
                it.startDate <= now && (it.endDate == null || it.endDate!! >= now)
            }

            activity?.runOnUiThread {
                structuralPipelineList.clear()

                val populatedSections = mutableListOf<Any>()
                val emptySections = mutableListOf<Any>()

                // 1. Evaluate Overdue
                if (overdueBucket.isNotEmpty()) {
                    populatedSections.add("⚠️ OVERDUE")
                    populatedSections.addAll(overdueBucket)
                } else {
                    emptySections.add("⚠️ OVERDUE")
                    emptySections.add(" EMPTY")
                }

                // 2. Evaluate Ongoing
                if (ongoingBucket.isNotEmpty()) {
                    populatedSections.add("⏳ ONGOING")
                    populatedSections.addAll(ongoingBucket)
                } else {
                    emptySections.add("⏳ ONGOING")
                    emptySections.add(" EMPTY")
                }

                // 3. Evaluate Upcoming
                if (upcomingBucket.isNotEmpty()) {
                    populatedSections.add("📅 UPCOMING")
                    populatedSections.addAll(upcomingBucket)
                } else {
                    emptySections.add("📅 UPCOMING")
                    emptySections.add(" EMPTY")
                }

                // 4. Evaluate Done
                if (doneBucket.isNotEmpty()) {
                    populatedSections.add("✅ DONE")
                    populatedSections.addAll(doneBucket)
                } else {
                    emptySections.add("✅ DONE")
                    emptySections.add(" EMPTY")
                }

                // Assemble final payload by stacking populated data above empty states
                structuralPipelineList.addAll(populatedSections)
                structuralPipelineList.addAll(emptySections)

                myGoalsAdapter.notifyDataSetChanged()

                // Trigger Empty State accurately by checking database return payload
                if (allItems.isEmpty()) {
                    tvMyGoalsEmptyState.visibility = View.VISIBLE
                    rvMyGoalsList.visibility = View.GONE
                } else {
                    tvMyGoalsEmptyState.visibility = View.GONE
                    rvMyGoalsList.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPipelineHub()
    }
}