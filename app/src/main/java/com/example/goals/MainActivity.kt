package com.example.goals

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView

    private lateinit var btnToolbarSettings: ImageView
    private lateinit var btnAddGoal: FloatingActionButton // FIX 1: Declared the Activity-level FAB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigationView = findViewById(R.id.bottom_navigation)

        btnToolbarSettings = findViewById(R.id.btnToolbarSettings)
        btnAddGoal = findViewById(R.id.btnAddGoal) // FIX 2: Initialize it from activity_main.xml

        // Setup top action panel interactions


        btnToolbarSettings.setOnClickListener {
            Toast.makeText(this, "Settings Configuration Clicked!", Toast.LENGTH_SHORT).show()
        }

        // Establish core navigation mapping paths
        bottomNavigationView.setOnItemSelectedListener { item ->
            var selectedFragment: Fragment? = null
            when (item.itemId) {
                R.id.nav_home -> {
                    selectedFragment = HomeFragment()
                    btnAddGoal.show() // FIX 3: Animate FAB smoothly into view when switching to Home
                }
                R.id.nav_goals -> {
                    selectedFragment = MyGoalsFragment("goals")
                    btnAddGoal.hide() // FIX 4: Hide FAB out of sight when looking at Goal stats/list
                }
                R.id.nav_habits -> {
                    selectedFragment = MyGoalsFragment("habits")
                    btnAddGoal.hide() // FIX 4: Hide FAB out of sight when looking at Goal stats/list
                }
                R.id.nav_tasks -> {
                    selectedFragment = MyGoalsFragment("tasks")
                    btnAddGoal.hide() // FIX 4: Hide FAB out of sight when looking at Goal stats/list
                }
            }
            if (selectedFragment != null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit()
            }
            true
        }

        // Set initial entry framework viewing layout window point default state
        if (savedInstanceState == null) {
            btnAddGoal.show() // Ensure it is visible on cold launch default view
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
        }
    }
}