package com.example.goals

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.concurrent.thread

class AchievedGoalsFragment : Fragment(R.layout.achieved_goals_fragment) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var svAchievedSearch: SearchView

    private val allCachedAchievedGoals = mutableListOf<Goal>()
    private val processedDisplayItems = mutableListOf<Any>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rvAchievedGoals)
        tvEmptyState = view.findViewById(R.id.tvAchievedEmptyState)
        svAchievedSearch = view.findViewById(R.id.svAchievedSearch)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        recyclerView.adapter = GoalAdapter(
            items = processedDisplayItems,
            viewTypeMode = GoalAdapter.MODE_CARD_BLOCK,
            onGoalStatusChanged = { updatedGoal ->
                allCachedAchievedGoals.find { it.id == updatedGoal.id }?.isAchieved = updatedGoal.isAchieved

                thread {
                    AppDatabase.getDatabase(requireContext()).goalDao().updateGoal(updatedGoal)
                    activity?.runOnUiThread { loadAchievedGoalsData() }
                }
            },
            onGoalEditRequested = { goalToEdit ->
                // FIXED: Expanded parameters from 5 to 8 to match the updated bottom sheet callback implementation contract


            },
            onGoalDeleteRequested = { goalToDelete ->
                thread {
                    AppDatabase.getDatabase(requireContext()).goalDao().deleteGoal(goalToDelete)
                    activity?.runOnUiThread { loadAchievedGoalsData() }
                }
            },
            onGoalDetailsRequested = { goal ->
                val intent = Intent(requireContext(), GoalDetailsActivity::class.java).apply {
                    putExtra("GOAL_ID", goal.id)
                }
                startActivity(intent)
            }
        )

        svAchievedSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterSearchList(newText.orEmpty())
                return true
            }
        })

        loadAchievedGoalsData()
    }

    private fun loadAchievedGoalsData() {
        thread {
            val db = AppDatabase.getDatabase(requireContext())
            val allGoals = db.goalDao().getAllGoals()
            val completeGoals = allGoals.filter { it.isAchieved }

            activity?.runOnUiThread {
                allCachedAchievedGoals.clear()
                allCachedAchievedGoals.addAll(completeGoals)
                filterSearchList(svAchievedSearch.query.toString())
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun filterSearchList(query: String) {
        val filteredGoalsList = mutableListOf<Goal>()
        if (query.trim().isEmpty()) {
            filteredGoalsList.addAll(allCachedAchievedGoals)
        } else {
            val lowercaseQuery = query.lowercase().trim()
            for (goal in allCachedAchievedGoals) {
                if (goal.title.lowercase().contains(lowercaseQuery) ||
                    goal.description.lowercase().contains(lowercaseQuery)) {
                    filteredGoalsList.add(goal)
                }
            }
        }

        groupGoalsByStartDate(filteredGoalsList)

        if (processedDisplayItems.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            tvEmptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            (recyclerView.adapter as? GoalAdapter)?.updateData(processedDisplayItems)
        }
    }

    private fun groupGoalsByStartDate(goals: List<Goal>) {
        processedDisplayItems.clear()
        val formatParser = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        val groupedMap = goals.groupBy { goal -> extractStartDateString(goal.durationText) }

        val sortedDates = groupedMap.keys.sortedWith { date1, date2 ->
            try {
                val d1 = formatParser.parse(date1)
                val d2 = formatParser.parse(date2)
                if (d1 != null && d2 != null) d1.compareTo(d2) else date1.compareTo(date2)
            } catch (e: Exception) {
                date1.compareTo(date2)
            }
        }

        for (dateHeader in sortedDates) {
            processedDisplayItems.add(dateHeader)
            groupedMap[dateHeader]?.let { processedDisplayItems.addAll(it) }
        }
    }

    private fun extractStartDateString(duration: String): String {
        var clean = duration.replace("On", "", ignoreCase = true).trim()
        if (clean.contains("-")) {
            val parts = clean.split("-")
            if (parts.isNotEmpty()) {
                clean = parts[0].trim()
            }
        }
        return if (clean.isEmpty()) "Undated Goals" else clean
    }
}