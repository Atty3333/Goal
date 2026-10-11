package com.example.goals

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Locale

class AddGoalActivity : AppCompatActivity() {

    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private var startMs: Long = System.currentTimeMillis()
    private var endMs: Long? = null

    private val subtaskBuffer = mutableListOf<Goal>()
    private val availableHabitsList = mutableListOf<Goal>()

    private lateinit var etTitle: EditText
    private lateinit var etCategory: AutoCompleteTextView
    private lateinit var etPriority: AutoCompleteTextView

    private lateinit var etStartDate: EditText
    private lateinit var etEndDate: EditText
    private lateinit var tilGoalEndDate: TextInputLayout
    private lateinit var cbHasEndDate: CheckBox
    private lateinit var llDateBoundaryContainer: LinearLayout

    private lateinit var etSubtaskInput: EditText
    private lateinit var tlSubtasks: TableLayout
    private lateinit var etNotes: EditText

    private lateinit var cbHasGoalTarget: CheckBox
    private lateinit var llGoalHabitContainer: LinearLayout
    private lateinit var spLinkHabit: Spinner

    private lateinit var cbHasGoalTotal: CheckBox
    private lateinit var llTargetMetricContainer: LinearLayout
    private lateinit var etGoalTargetValue: EditText
    private lateinit var spGoalTargetUnit: AutoCompleteTextView

    private var targetGoalId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_goal_screen)

        initializeViews()
        setupDropdownAdapters()
        setupTargetMetricToggle()
        loadSavedHabitsPipeline()

        targetGoalId = intent.getIntExtra("GOAL_ID", 0)
        if (targetGoalId != 0) {
            loadExistingGoalBlueprintData()
        }

        etStartDate.setOnClickListener { triggerSingleDatePicker(true) }
        etEndDate.setOnClickListener { triggerSingleDatePicker(false) }

        findViewById<Button>(R.id.btnAppendSubtask).setOnClickListener {
            executeSubtaskAppending()
        }

        findViewById<Button>(R.id.btnSaveGoalBlueprint).setOnClickListener {
            validateAndExecuteSaving()
        }
    }

    override fun onResume() {
        super.onResume()
        if (targetGoalId != 0) {
            reloadSubtaskEntitiesFromDatabase()
        }
    }

    private fun initializeViews() {
        etTitle = findViewById(R.id.etGoalTitle)
        etCategory = findViewById(R.id.etGoalCategory)
        etPriority = findViewById(R.id.etGoalPriority)

        etStartDate = findViewById(R.id.etGoalStartDate)
        etEndDate = findViewById(R.id.etGoalEndDate)
        tilGoalEndDate = findViewById(R.id.tilGoalEndDate)
        cbHasEndDate = findViewById(R.id.cbHasEndDate)
        llDateBoundaryContainer = findViewById(R.id.llDateBoundaryContainer)

        etSubtaskInput = findViewById(R.id.etSubtaskInput)
        tlSubtasks = findViewById(R.id.tlSubtasksContainer)
        etNotes = findViewById(R.id.etGoalNotes)

        cbHasGoalTarget = findViewById(R.id.cbHasGoalTarget)
        llGoalHabitContainer = findViewById(R.id.llGoalHabitContainer)
        spLinkHabit = findViewById(R.id.spLinkHabit)

        cbHasGoalTotal = findViewById(R.id.cbHasGoalTotal)
        llTargetMetricContainer = findViewById(R.id.llTargetMetricContainer)
        etGoalTargetValue = findViewById(R.id.etGoalTargetValue)
        spGoalTargetUnit = findViewById(R.id.spGoalTargetUnit)

        etStartDate.setText(dateFormatter.format(startMs))

        findViewById<ImageView>(R.id.ivGoalBackButton).setOnClickListener {
            finish()
        }
    }

    private fun setupDropdownAdapters() {
        etCategory.inputType = InputType.TYPE_NULL
        etPriority.inputType = InputType.TYPE_NULL
        spGoalTargetUnit.inputType = InputType.TYPE_NULL

        val defaultCategories = arrayOf("None", "Health", "Fitness", "Work", "Study", "Finance", "Creativity", "Social", "Custom option")
        etCategory.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, defaultCategories))

        val priorityOptions = arrayOf("High", "Medium", "Low", "None")
        etPriority.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, priorityOptions))

        val targetUnits = arrayOf("Count", "Hours", "Mins", "Pages", "Km", "Miles", "Reps", "Cycles", "Custom option")
        spGoalTargetUnit.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, targetUnits))

        etCategory.setOnItemClickListener { parent, _, position, _ ->
            val selection = parent.getItemAtPosition(position).toString()
            if (selection == "Custom option") {
                etCategory.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                etCategory.text = null
                etCategory.requestFocus()
            } else {
                etCategory.inputType = InputType.TYPE_NULL
            }
        }

        spGoalTargetUnit.setOnItemClickListener { parent, _, position, _ ->
            val selection = parent.getItemAtPosition(position).toString()
            if (selection == "Custom option") {
                spGoalTargetUnit.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                spGoalTargetUnit.text = null
                spGoalTargetUnit.requestFocus()

                val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.showSoftInput(spGoalTargetUnit, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            } else {
                spGoalTargetUnit.inputType = InputType.TYPE_NULL
            }
        }
    }

    private fun setupTargetMetricToggle() {
        cbHasGoalTotal.setOnCheckedChangeListener { _, isChecked ->
            llTargetMetricContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) {
                etGoalTargetValue.text = null
                spGoalTargetUnit.text = null
            }
        }

        val updateVisibilityFlows = {
            if (cbHasGoalTarget.isChecked) {
                llGoalHabitContainer.visibility = View.VISIBLE
                llDateBoundaryContainer.visibility = View.GONE
                endMs = null
            } else {
                llGoalHabitContainer.visibility = View.GONE
                llDateBoundaryContainer.visibility = View.VISIBLE
                spLinkHabit.setSelection(0)

                if (cbHasEndDate.isChecked) {
                    tilGoalEndDate.visibility = View.VISIBLE
                    if (endMs == null) {
                        endMs = startMs + (7 * 24 * 60 * 60 * 1000L)
                        etEndDate.setText(dateFormatter.format(endMs!!))
                    }
                } else {
                    tilGoalEndDate.visibility = View.GONE
                    endMs = null
                }
            }
        }
        cbHasGoalTarget.setOnCheckedChangeListener { _, _ -> updateVisibilityFlows() }
        cbHasEndDate.setOnCheckedChangeListener { _, _ -> updateVisibilityFlows() }
    }

    private fun loadSavedHabitsPipeline() {
        kotlin.concurrent.thread {
            val habits = AppDatabase.getDatabase(this).goalDao().getAllGoals().filter { it.itemType == GoalType.HABIT }
            runOnUiThread {
                availableHabitsList.clear()
                availableHabitsList.addAll(habits)
                val spinnerOptions = mutableListOf("None / Standalone Objective Context")
                habits.forEach { spinnerOptions.add("🔥 ${it.title.uppercase()}") }

                val customSpinnerAdapter = object : ArrayAdapter<String>(this@AddGoalActivity, android.R.layout.simple_spinner_item, spinnerOptions) {
                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val view = super.getView(position, convertView, parent) as TextView
                        view.setTextColor(ContextCompat.getColor(this@AddGoalActivity, R.color.tiertiry))
                        return view
                    }

                    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val view = super.getDropDownView(position, convertView, parent) as TextView
                        view.setTextColor(ContextCompat.getColor(this@AddGoalActivity, R.color.tiertiry))
                        return view
                    }
                }
                spLinkHabit.adapter = customSpinnerAdapter
            }
        }
    }

    private fun loadExistingGoalBlueprintData() {
        kotlin.concurrent.thread {
            val dao = AppDatabase.getDatabase(this).goalDao()
            val goal = dao.getGoalById(targetGoalId)
            goal?.let {
                runOnUiThread {
                    etTitle.setText(it.title)
                    etCategory.setText(it.category, false)
                    etPriority.setText(it.priority, false)
                    etNotes.setText(it.description)
                    startMs = it.startDate
                    etStartDate.setText(dateFormatter.format(startMs))

                    if (it.endDate != null && it.endDate!! > 0L) {
                        cbHasEndDate.isChecked = true
                        endMs = it.endDate
                        etEndDate.setText(dateFormatter.format(endMs!!))
                    }

                    val rawDuration = it.durationText ?: ""
                    if (rawDuration.isNotEmpty()) {
                        val parts = rawDuration.split("|").map { token -> token.trim() }
                        parts.forEach { part ->
                            when {
                                part.startsWith("TargetValue:", ignoreCase = true) -> {
                                    etGoalTargetValue.setText(part.substringAfter("TargetValue:").trim())
                                    cbHasGoalTotal.isChecked = true
                                }
                                part.startsWith("MetricUnit:", ignoreCase = true) -> {
                                    val fullUnit = part.substringAfter("MetricUnit:").trim()
                                    val unitWord = fullUnit.substringAfter(" ").trim()
                                    spGoalTargetUnit.setText(unitWord, false)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun reloadSubtaskEntitiesFromDatabase() {
        // Reserved for matching subtask schema sets if added manually
    }

    private fun executeSubtaskAppending() {
        val subtaskTitle = etSubtaskInput.text.toString().trim()
        if (subtaskTitle.isEmpty()) return

        val tableRow = TableRow(this)
        val tv = TextView(this).apply {
            text = subtaskTitle
            setTextColor(ContextCompat.getColor(this@AddGoalActivity, R.color.tiertiry))
            setBackgroundColor(ContextCompat.getColor(this@AddGoalActivity, R.color.Secondary))
            textSize = 16f
            setPadding(16, 16, 16, 16)
        }
        tableRow.addView(tv)
        tlSubtasks.addView(tableRow)
        etSubtaskInput.text = null
    }

    private fun triggerSingleDatePicker(isStartDate: Boolean) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(if (isStartDate) "Select Start Date" else "Select Target Deadline")
            .build()

        picker.addOnPositiveButtonClickListener { timeMs ->
            timeMs?.let {
                if (isStartDate) {
                    startMs = it
                    etStartDate.setText(dateFormatter.format(it))
                } else {
                    endMs = it
                    etEndDate.setText(dateFormatter.format(it))
                }
            }
        }
        picker.show(supportFragmentManager, "GoalDatePicker")
    }

    private fun validateAndExecuteSaving() {
        val title = etTitle.text.toString().trim()
        val category = etCategory.text.toString().trim()
        val priority = etPriority.text.toString().trim()
        val notes = etNotes.text.toString().trim()

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a Goal Title", Toast.LENGTH_SHORT).show()
            return
        }

        val payloadBuilder = StringBuilder()

        if (cbHasGoalTotal.isChecked) {
            val targetValue = etGoalTargetValue.text.toString().trim()
            val targetUnit = spGoalTargetUnit.text.toString().trim()

            if (targetValue.isNotEmpty()) {
                payloadBuilder.append("TargetValue: $targetValue")
                payloadBuilder.append(" | MetricUnit: $targetValue $targetUnit")
                payloadBuilder.append(" | CurrentProgress: 0.0")
            }
        }

        val finalizedDurationPayload = payloadBuilder.toString()

        kotlin.concurrent.thread {
            val dao = AppDatabase.getDatabase(this).goalDao()

            if (targetGoalId != 0) {
                val existingGoal = dao.getGoalById(targetGoalId)
                existingGoal?.let {
                    it.title = title
                    it.category = category
                    it.priority = priority
                    it.description = notes
                    it.notes = notes
                    it.startDate = startMs
                    it.endDate = if (cbHasEndDate.isChecked && !cbHasGoalTarget.isChecked) endMs else null

                    val previousText = it.durationText ?: ""
                    if (previousText.contains("CurrentProgress:")) {
                        val savedProgress = previousText.split("|")
                            .firstOrNull { token -> token.trim().startsWith("CurrentProgress:", ignoreCase = true) }
                            ?.trim() ?: "CurrentProgress: 0.0"

                        val structuralTokens = finalizedDurationPayload.split("|")
                            .map { t -> t.trim() }
                            .filter { t -> !t.startsWith("CurrentProgress:", ignoreCase = true) && t.isNotEmpty() }
                            .toMutableList()

                        structuralTokens.add(savedProgress)
                        it.durationText = structuralTokens.joinToString(" | ")
                    } else {
                        it.durationText = finalizedDurationPayload
                    }

                    dao.updateGoal(it)
                }
            } else {
                val newGoal = Goal(
                    title = title,
                    itemType = GoalType.GOAL,
                    category = category,
                    priority = priority,
                    description = notes,
                    notes = notes,
                    startDate = startMs,
                    endDate = if (cbHasEndDate.isChecked && !cbHasGoalTarget.isChecked) endMs else null,
                    durationText = finalizedDurationPayload,
                    isAchieved = false
                )
                dao.insertGoal(newGoal)
            }

            runOnUiThread {
                Toast.makeText(this@AddGoalActivity, "Goal Saved Successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}