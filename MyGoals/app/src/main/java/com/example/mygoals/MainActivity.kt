package com.example.mygoals

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mygoals.R

// 1. DATA MODEL (Must be outside the Activity class)
data class Goal(
    val title: String,
    var isAchieved: Boolean = false
)

// 2. THE ADAPTER (Handles how the list looks)
class GoalAdapter(private val goals: MutableList<Goal>) :
    RecyclerView.Adapter<GoalAdapter.GoalViewHolder>() {

    class GoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvGoalTitle: TextView = itemView.findViewById(R.id.tvGoalTitle)
        val cbAchieved: CheckBox = itemView.findViewById(R.id.cbAchieved)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_goal, parent, false)
        return GoalViewHolder(view)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        val currentGoal = goals[position]
        holder.tvGoalTitle.text = currentGoal.title
        holder.cbAchieved.isChecked = currentGoal.isAchieved

        holder.cbAchieved.setOnCheckedChangeListener { _, isChecked ->
            currentGoal.isAchieved = isChecked
        }
    }

    override fun getItemCount() = goals.size
}

// 3. THE MAIN ACTIVITY
class MainActivity : androidx.appcompat.app.AppCompatActivity() {

    private lateinit var goalAdapter: GoalAdapter
    private val goalList = mutableListOf<Goal>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val rvGoals: RecyclerView = findViewById(R.id.rvGoals)
        val etGoalInput: EditText = findViewById(R.id.etGoalInput)
        val btnAddGoal: Button = findViewById(R.id.btnAddGoal)

        goalAdapter = _root_ide_package_.com.example.mygoals.GoalAdapter(goalList)
        rvGoals.adapter = goalAdapter
        rvGoals.layoutManager = LinearLayoutManager(this)

        btnAddGoal.setOnClickListener {
            val title = etGoalInput.text.toString()
            if (title.isNotEmpty()) {
                val newGoal = _root_ide_package_.com.example.mygoals.Goal(title)
                goalList.add(newGoal)
                goalAdapter.notifyItemInserted(goalList.size - 1)
                etGoalInput.text.clear()
            }
        }
    }
}