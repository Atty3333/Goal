package com.example.goals

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Locale

class AddHabitActivity : AppCompatActivity() {

    private lateinit var etTitle: EditText
    private lateinit var etCategory: AutoCompleteTextView
    private lateinit var etPriority: AutoCompleteTextView
    private lateinit var actvHabitMetric: AutoCompleteTextView
    private lateinit var etStartDate: EditText
    private lateinit var etEndDate: EditText
    private lateinit var tilHabitEndDate: TextInputLayout
    private lateinit var etNotes: EditText
    private lateinit var cbHasEndDate: CheckBox

    private lateinit var cbHasAction: CheckBox
    private lateinit var tilHabitMetric: TextInputLayout
    private lateinit var tilActionValue: TextInputLayout
    private lateinit var etactionvalue: EditText

    private lateinit var cbLinkWithGoal: CheckBox
    private lateinit var llLinkGoalContainer: LinearLayout
    private lateinit var spLinkGoal: Spinner
    private lateinit var btnSave: Button

    private lateinit var spFrequencyType: Spinner
    private lateinit var llContainerIntervals: LinearLayout
    private lateinit var llContainerPeriodic: LinearLayout
    private lateinit var llContainerSpecificDays: LinearLayout

    private lateinit var etIntervalDays: EditText
    private lateinit var spIntervalUnit: Spinner
    private lateinit var etPeriodicCount: EditText
    private lateinit var spPeriodicTimeframe: Spinner

    private lateinit var btnSelectSpecificDates: Button
    private lateinit var cgSpecificDates: ChipGroup

    private val selectedCalendarDates = mutableListOf<Long>()

    private var updateTargetId: Int = 0
    private var selectedStartDateMs: Long = System.currentTimeMillis()
    private var selectedEndDateMs: Long? = null

    private val availableGoalsList = mutableListOf<Goal>()
    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_habit_screen)

        initializeViews()
        setupDropdownAdapters()
        setupFrequencySpinners()
        setupTargetAndDateToggles()
        loadAvailableBlueprintsPipeline()

        updateTargetId = intent.getIntExtra("GOAL_ID", 0)
        if (updateTargetId != 0) {
            loadExistingHabitDataForEditing()
        }
    }

    private fun initializeViews() {
        etTitle = findViewById(R.id.etHabitTitle)
        etCategory = findViewById(R.id.etHabitCategory)
        etPriority = findViewById(R.id.etHabitPriority)
        actvHabitMetric = findViewById(R.id.actvHabitMetric)

        cbHasAction = findViewById(R.id.cbHasAction)
        tilHabitMetric = findViewById(R.id.tilHabitMetric)
        tilActionValue = findViewById(R.id.tilActionValue)
        etactionvalue = findViewById(R.id.etactionvalue)

        etStartDate = findViewById(R.id.etHabitStartDate)
        etEndDate = findViewById(R.id.etHabitEndDate)
        tilHabitEndDate = findViewById(R.id.tilHabitEndDate)
        etNotes = findViewById(R.id.etHabitNotes)
        cbHasEndDate = findViewById(R.id.cbHasEndDate)

        cbLinkWithGoal = findViewById(R.id.cbLinkWithGoal)
        llLinkGoalContainer = findViewById(R.id.llLinkGoalContainer)
        spLinkGoal = findViewById(R.id.spLinkGoal)
        btnSave = findViewById(R.id.btnSaveHabitTracker)

        spFrequencyType = findViewById(R.id.spFrequencyType)
        llContainerIntervals = findViewById(R.id.llContainerIntervals)
        llContainerPeriodic = findViewById(R.id.llContainerPeriodic)
        llContainerSpecificDays = findViewById(R.id.llContainerSpecificDays)

        etIntervalDays = findViewById(R.id.etIntervalDays)
        spIntervalUnit = findViewById(R.id.spIntervalUnit)
        etPeriodicCount = findViewById(R.id.etPeriodicCount)
        spPeriodicTimeframe = findViewById(R.id.spPeriodicTimeframe)

        btnSelectSpecificDates = findViewById(R.id.btnSelectSpecificDates)
        cgSpecificDates = findViewById(R.id.cgSpecificDates)

        etStartDate.setText(dateFormatter.format(selectedStartDateMs))

        findViewById<ImageView>(R.id.ivHabitBackButton).setOnClickListener {
            finish()
        }
    }

    private fun generateThemedSpinnerAdapter(options: List<String>): ArrayAdapter<String> {
        return object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, options) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                view.setTextColor(ContextCompat.getColor(this@AddHabitActivity, R.color.tiertiry))
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                view.setTextColor(ContextCompat.getColor(this@AddHabitActivity, R.color.tiertiry))
                return view
            }
        }
    }

    private fun setupDropdownAdapters() {
        etCategory.inputType = InputType.TYPE_NULL
        etPriority.inputType = InputType.TYPE_NULL
        actvHabitMetric.inputType = InputType.TYPE_NULL

        val defaultCategories = arrayOf("Health", "Fitness", "Work", "Study", "Finance", "Creativity", "Social", "Custom Option")
        etCategory.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, defaultCategories))

        val priorityOptions = arrayOf("High", "Medium", "Normal")
        etPriority.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, priorityOptions))

        val basicMetrics = arrayOf("step", "hour", "times", "pages", "km", "calories", "Custom Option")
        actvHabitMetric.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, basicMetrics))

        etCategory.setOnItemClickListener { parent, _, position, _ ->
            val selection = parent.getItemAtPosition(position).toString()
            if (selection.equals("Custom Option", ignoreCase = true)) {
                etCategory.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                etCategory.text = null
                etCategory.requestFocus()
            } else {
                etCategory.inputType = InputType.TYPE_NULL
            }
        }

        actvHabitMetric.setOnItemClickListener { parent, _, position, _ ->
            val selection = parent.getItemAtPosition(position).toString()
            if (selection.equals("Custom Option", ignoreCase = true)) {
                actvHabitMetric.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                actvHabitMetric.text = null
                actvHabitMetric.requestFocus()

                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(actvHabitMetric, InputMethodManager.SHOW_IMPLICIT)
            } else {
                actvHabitMetric.inputType = InputType.TYPE_NULL
            }
        }
    }

    private fun setupFrequencySpinners() {
        val freqOptions = arrayOf("Daily Loop", "Interval Sequences", "Set Days Per Period", "Specific Calendar Days")
        spFrequencyType.adapter = generateThemedSpinnerAdapter(freqOptions.toList())

        spFrequencyType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                llContainerIntervals.visibility = if (pos == 1) View.VISIBLE else View.GONE
                llContainerPeriodic.visibility = if (pos == 2) View.VISIBLE else View.GONE
                llContainerSpecificDays.visibility = if (pos == 3) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spIntervalUnit.adapter = generateThemedSpinnerAdapter(listOf("days", "weeks", "months"))
        spPeriodicTimeframe.adapter = generateThemedSpinnerAdapter(listOf("Week", "Month"))
    }

    private fun setupTargetAndDateToggles() {
        cbHasAction.setOnCheckedChangeListener { _, isChecked ->
            tilHabitMetric.visibility = if (isChecked) View.VISIBLE else View.GONE
            tilActionValue.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) {
                etactionvalue.text = null
                actvHabitMetric.text = null
                actvHabitMetric.inputType = InputType.TYPE_NULL
            }
        }

        cbHasEndDate.setOnCheckedChangeListener { _, isChecked ->
            tilHabitEndDate.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (isChecked) {
                cbLinkWithGoal.isChecked = false
                if (selectedEndDateMs == null) {
                    selectedEndDateMs = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L)
                    etEndDate.setText(dateFormatter.format(selectedEndDateMs!!))
                }
            } else {
                selectedEndDateMs = null
            }
        }

        cbLinkWithGoal.setOnCheckedChangeListener { _, isChecked ->
            llLinkGoalContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (isChecked) cbHasEndDate.isChecked = false
        }

        etStartDate.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker().setTitleText("Habit Commencement").build()
            picker.addOnPositiveButtonClickListener { it?.let { time -> selectedStartDateMs = time; etStartDate.setText(dateFormatter.format(time)) } }
            picker.show(supportFragmentManager, "StartDatePicker")
        }

        etEndDate.setOnClickListener {
            if (!cbHasEndDate.isChecked) return@setOnClickListener
            val picker = MaterialDatePicker.Builder.datePicker().setTitleText("Routine Termination").build()
            picker.addOnPositiveButtonClickListener { timeMs ->
                timeMs?.let { time ->
                    selectedEndDateMs = time
                    etEndDate.setText(dateFormatter.format(time))
                }
            }
            picker.show(supportFragmentManager, "EndDatePicker")
        }

        btnSelectSpecificDates.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker().setTitleText("Add Target Date").build()
            picker.addOnPositiveButtonClickListener { timeMs ->
                timeMs?.let {
                    if (!selectedCalendarDates.contains(it)) {
                        selectedCalendarDates.add(it)
                        selectedCalendarDates.sort()
                        rebuildCalendarDateChips()
                    }
                }
            }
            picker.show(supportFragmentManager, "SingleDatePickerForMulti")
        }

        btnSave.setOnClickListener { validateAndExecuteSaving() }
    }

    private fun rebuildCalendarDateChips() {
        cgSpecificDates.removeAllViews()
        for (timeMs in selectedCalendarDates) {
            val chip = Chip(this).apply {
                text = dateFormatter.format(timeMs)
                isCloseIconVisible = true
                setTextColor(ContextCompat.getColor(this@AddHabitActivity, R.color.tiertiry))
                setChipBackgroundColorResource(android.R.color.transparent)

                setOnCloseIconClickListener {
                    cgSpecificDates.removeView(this)
                    selectedCalendarDates.remove(timeMs)
                }
            }
            cgSpecificDates.addView(chip)
        }
    }

    private fun loadAvailableBlueprintsPipeline() {
        kotlin.concurrent.thread {
            val parentGoals = AppDatabase.getDatabase(this).goalDao().getAllGoals().filter { it.itemType == GoalType.GOAL }
            runOnUiThread {
                availableGoalsList.clear()
                availableGoalsList.addAll(parentGoals)
                val options = mutableListOf("None")
                parentGoals.forEach { options.add("🎯 ${it.title.uppercase()}") }
                spLinkGoal.adapter = generateThemedSpinnerAdapter(options)

                if (updateTargetId != 0) {
                    syncSpinnerSelectionToEditingGoal()
                }
            }
        }
    }

    private var targetParentGoalIdToSelect: Int? = null

    private fun syncSpinnerSelectionToEditingGoal() {
        targetParentGoalIdToSelect?.let { goalId ->
            val index = availableGoalsList.indexOfFirst { it.id == goalId }
            if (index != -1) {
                spLinkGoal.setSelection(index + 1)
            }
        }
    }

    private fun loadExistingHabitDataForEditing() {
        kotlin.concurrent.thread {
            val habit = AppDatabase.getDatabase(this).goalDao().getGoalById(updateTargetId)
            habit?.let {
                runOnUiThread {
                    etTitle.setText(it.title)
                    etCategory.setText(it.category, false)
                    etPriority.setText(it.priority, false)
                    etNotes.setText(it.description)

                    selectedStartDateMs = it.startDate
                    etStartDate.setText(dateFormatter.format(selectedStartDateMs))

                    if (it.endDate != null && it.endDate!! > 0L) {
                        cbHasEndDate.isChecked = true
                        selectedEndDateMs = it.endDate
                        etEndDate.setText(dateFormatter.format(selectedEndDateMs!!))
                    }

                    val rawDuration = it.durationText ?: ""
                    if (rawDuration.contains(" | ") || rawDuration.startsWith("Frequency:")) {
                        val components = rawDuration.split(" | ")
                        components.forEach { element ->
                            val token = element.trim()
                            when {
                                token.startsWith("Frequency:") -> {
                                    val freq = token.replace("Frequency:", "").trim()
                                    restoreFrequencyUIValues(freq)
                                }
                                token.startsWith("MetricUnit:") -> {
                                    val fullMetric = token.replace("MetricUnit:", "").trim()
                                    cbHasAction.isChecked = true
                                    val splitIndex = fullMetric.indexOf(" ")
                                    if (splitIndex != -1) {
                                        etactionvalue.setText(fullMetric.substring(0, splitIndex))
                                        actvHabitMetric.setText(fullMetric.substring(splitIndex + 1), false)
                                    } else {
                                        etactionvalue.setText(fullMetric)
                                    }
                                }
                                token.startsWith("ParentGoalId:") -> {
                                    cbLinkWithGoal.isChecked = true
                                    val pId = token.replace("ParentGoalId:", "").trim().toIntOrNull()
                                    targetParentGoalIdToSelect = pId
                                    syncSpinnerSelectionToEditingGoal()
                                }
                                // NEW: Load specific dates back into the chips when editing
                                token.startsWith("TargetDates:") -> {
                                    val datesString = token.replace("TargetDates:", "").trim()
                                    val dateFormat = SimpleDateFormat("MMMM d yyyy", Locale.US)

                                    datesString.split(",").forEach { dateStr ->
                                        try {
                                            val parsedTime = dateFormat.parse(dateStr.trim())?.time
                                            if (parsedTime != null && !selectedCalendarDates.contains(parsedTime)) {
                                                selectedCalendarDates.add(parsedTime)
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                    selectedCalendarDates.sort()
                                    rebuildCalendarDateChips()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun restoreFrequencyUIValues(freqText: String) {
        when {
            freqText.equals("Daily Loop", ignoreCase = true) -> {
                spFrequencyType.setSelection(0)
            }
            freqText.startsWith("Every", ignoreCase = true) -> {
                spFrequencyType.setSelection(1)
                val clean = freqText.replace("Every", "").trim()
                val chunks = clean.split(" ")
                if (chunks.size == 2) {
                    etIntervalDays.setText(chunks[0])
                    val unitIndex = when(chunks[1].lowercase()) {
                        "weeks" -> 1
                        "months" -> 2
                        else -> 0
                    }
                    spIntervalUnit.setSelection(unitIndex)
                }
            }
            freqText.contains("Days Per", ignoreCase = true) -> {
                spFrequencyType.setSelection(2)
                val chunks = freqText.split(" Days Per ")
                if (chunks.size == 2) {
                    etPeriodicCount.setText(chunks[0].trim())
                    val timeframeIndex = if (chunks[1].trim().equals("Month", ignoreCase = true)) 1 else 0
                    spPeriodicTimeframe.setSelection(timeframeIndex)
                }
            }
            freqText.startsWith("Specific Dates", ignoreCase = true) -> {
                spFrequencyType.setSelection(3)
            }
        }
    }

    private fun validateAndExecuteSaving() {
        val title = etTitle.text.toString().trim()
        val category = etCategory.text.toString().trim()
        val priority = etPriority.text.toString().trim()
        val notes = etNotes.text.toString().trim()

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a Habit Title", Toast.LENGTH_SHORT).show()
            return
        }

        // NEW: Determine Enum dynamically based on Spinner selection
        val selectedFrequencyEnum = when (spFrequencyType.selectedItemPosition) {
            1 -> HabitFrequency.INTERVAL
            2 -> HabitFrequency.SOME_DAYS_PERIOD
            3 -> HabitFrequency.SPECIFIC_DAYS
            else -> HabitFrequency.DAILY
        }

        val frequencyToken = when (spFrequencyType.selectedItemPosition) {
            1 -> {
                val num = etIntervalDays.text.toString().ifEmpty { "1" }
                val unit = spIntervalUnit.selectedItem.toString()
                "Frequency: Every $num $unit"
            }
            2 -> {
                val count = etPeriodicCount.text.toString().ifEmpty { "1" }
                val timeframe = spPeriodicTimeframe.selectedItem.toString()
                "Frequency: $count Days Per $timeframe"
            }
            3 -> {
                // NEW: Attach exactly formatted specific dates to payload text
                if (selectedCalendarDates.isNotEmpty()) {
                    val dateStrings = selectedCalendarDates.map { timeMs ->
                        SimpleDateFormat("MMMM d yyyy", Locale.US).format(timeMs).lowercase(Locale.US)
                    }
                    "Frequency: Specific Dates | TargetDates: ${dateStrings.joinToString(",")}"
                } else {
                    "Frequency: Specific Dates"
                }
            }
            else -> "Frequency: Daily"
        }

        val payloadBuilder = StringBuilder(frequencyToken)

        val habitValue = etactionvalue.text.toString().trim()
        val habitMetric = actvHabitMetric.text.toString().trim()

        if (cbHasAction.isChecked && habitValue.isNotEmpty()) {
            payloadBuilder.append(" | MetricUnit: $habitValue $habitMetric")
            payloadBuilder.append(" | TargetValue: $habitValue")
        }

        if (cbLinkWithGoal.isChecked && spLinkGoal.selectedItemPosition > 0) {
            val selectedParentGoal = availableGoalsList[spLinkGoal.selectedItemPosition - 1]
            payloadBuilder.append(" | ParentGoalId: ${selectedParentGoal.id}")
        }

        val finalizedDurationPayload = payloadBuilder.toString()

        kotlin.concurrent.thread {
            val dao = AppDatabase.getDatabase(this).goalDao()

            if (updateTargetId != 0) {
                val existingHabit = dao.getGoalById(updateTargetId)
                existingHabit?.let {
                    it.title = title
                    it.category = category
                    it.priority = priority
                    it.description = notes
                    it.notes = notes
                    it.startDate = selectedStartDateMs
                    it.endDate = if (cbHasEndDate.isChecked) selectedEndDateMs else null
                    it.durationText = finalizedDurationPayload
                    it.frequency = selectedFrequencyEnum // NEW: Set actual database Enum field
                    dao.updateGoal(it)
                }
            } else {
                val newHabit = Goal(
                    title = title,
                    itemType = GoalType.HABIT,
                    category = category,
                    priority = priority,
                    description = notes,
                    notes = notes,
                    startDate = selectedStartDateMs,
                    endDate = if (cbHasEndDate.isChecked) selectedEndDateMs else null,
                    durationText = finalizedDurationPayload,
                    frequency = selectedFrequencyEnum, // NEW: Set actual database Enum field
                    isAchieved = false
                )
                dao.insertGoal(newHabit)
            }

            // --- PARENT GOAL PROGRESS SYNC PIPELINE ---
            if (cbLinkWithGoal.isChecked && spLinkGoal.selectedItemPosition > 0 && habitValue.isNotEmpty()) {
                val selectedParentGoal = availableGoalsList[spLinkGoal.selectedItemPosition - 1]
                val parentGoalFromDb = dao.getGoalById(selectedParentGoal.id)

                parentGoalFromDb?.let { parent ->
                    val cleanTokens = (parent.durationText ?: "").split("|")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .toMutableList()

                    val hasProgressToken = cleanTokens.any { it.startsWith("CurrentProgress:", ignoreCase = true) }
                    if (!hasProgressToken) {
                        cleanTokens.add("CurrentProgress: 0.0")
                    }

                    parent.durationText = cleanTokens.joinToString(" | ")
                    dao.updateGoal(parent)
                }
            }

            runOnUiThread {
                Toast.makeText(this@AddHabitActivity, "Habit Saved & Goal Connected!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}