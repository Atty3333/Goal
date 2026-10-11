package com.example.goals

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Locale

class AddTaskBottomSheet(
    private val existingTask: Goal? = null,
    private val onTaskSaved: ((title: String, category: String, priority: String, notes: String, dateMs: Long, linkedGoalPayload: String?) -> Unit)? = null
) : BottomSheetDialogFragment() {

    private lateinit var etDate: EditText
    private lateinit var etNotes: EditText
    private lateinit var cbLinkTaskToGoal: CheckBox
    private lateinit var spTaskGoalLink: Spinner

    private var selectedDateMs: Long = System.currentTimeMillis()
    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val masterGoalsOptionsCache = mutableListOf<Goal>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.add_task_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvHeader = view.findViewById<TextView>(R.id.tvTaskSheetHeader)
        val etTitle = view.findViewById<EditText>(R.id.etTaskTitle)
        val spCategory = view.findViewById<AutoCompleteTextView>(R.id.spTaskCategory)
        val spPriority = view.findViewById<AutoCompleteTextView>(R.id.spTaskPriority)
        etDate = view.findViewById(R.id.etTaskDate)
        etNotes = view.findViewById(R.id.etTaskNotes)
        cbLinkTaskToGoal = view.findViewById(R.id.cbLinkTaskToGoal)
        spTaskGoalLink = view.findViewById(R.id.spTaskGoalLink)
        val btnSave = view.findViewById<Button>(R.id.btnSaveTask)

        val categories = arrayOf("General", "Coding", "Fitness", "Music", "Finance", "Work", "Custom Option")
        val priorities = arrayOf("Low", "Medium", "High")

        spCategory.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories))
        spCategory.setOnItemClickListener { parent, _, position, _ ->
            val selection = parent.getItemAtPosition(position).toString()
            if (selection.equals("Custom Option", ignoreCase = true)) {
                spCategory.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                spCategory.text = null
                spCategory.requestFocus()

                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(spCategory, InputMethodManager.SHOW_IMPLICIT)
            } else {
                spCategory.inputType = InputType.TYPE_NULL
            }
        }

        spPriority.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, priorities))
        etDate.setText(dateFormatter.format(selectedDateMs))

        cbLinkTaskToGoal.setOnCheckedChangeListener { _, isChecked ->
            spTaskGoalLink.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        loadMasterObjectivesPipeline()

        etDate.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Task Schedule Date")
                .setSelection(selectedDateMs)
                .build()

            picker.addOnPositiveButtonClickListener { selection ->
                selection?.let {
                    selectedDateMs = it
                    etDate.setText(dateFormatter.format(it))
                }
            }
            picker.show(childFragmentManager, "TaskDatePickerTag")
        }

        if (existingTask != null) {
            tvHeader.text = "Update Task Entry"
            etTitle.setText(existingTask.title)
            etNotes.setText(existingTask.notes)

            selectedDateMs = existingTask.startDate
            etDate.setText(dateFormatter.format(selectedDateMs))

            spCategory.setText(existingTask.category, false)
            spPriority.setText(existingTask.priority, false)
            btnSave.text = "Modify Item"
        }

        btnSave.setOnClickListener {
            val titleText = etTitle.text.toString().trim()
            val notesText = etNotes.text.toString().trim()

            if (titleText.isEmpty()) {
                etTitle.error = "Task label required"
                return@setOnClickListener
            }

            // Matches the token your GoalAdapter is scanning for
            val finalLinkedPayload = if (cbLinkTaskToGoal.isChecked && spTaskGoalLink.selectedItemPosition > 0) {
                "ParentGoalId: ${masterGoalsOptionsCache[spTaskGoalLink.selectedItemPosition - 1].title}"
            } else {
                ""
            }

            onTaskSaved?.invoke(
                titleText,
                spCategory.text.toString(),
                spPriority.text.toString(),
                notesText,
                selectedDateMs,
                finalLinkedPayload
            )
            dismiss()
        }
    }

    private fun generateThemedSpinnerAdapter(options: List<String>): ArrayAdapter<String> {
        return object : ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, options) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                view.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                return view
            }
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                view.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                return view
            }
        }
    }

    private fun loadMasterObjectivesPipeline() {
        kotlin.concurrent.thread {
            val records = AppDatabase.getDatabase(requireContext()).goalDao().getAllGoals()
                .filter { it.itemType == GoalType.GOAL || it.itemType == GoalType.HABIT }

            activity?.runOnUiThread {
                masterGoalsOptionsCache.clear()
                masterGoalsOptionsCache.addAll(records)

                val displayStringsList = mutableListOf("Select Goal")
                records.forEach {
                    val iconPrefix = if (it.itemType == GoalType.HABIT) "🔥 [HABIT]" else "🎯 [GOAL]"
                    displayStringsList.add("$iconPrefix ${it.title.uppercase()}")
                }

                spTaskGoalLink.adapter = generateThemedSpinnerAdapter(displayStringsList)

                existingTask?.durationText?.let { currentPayload ->
                    if (currentPayload.contains("ParentGoalId:")) {
                        val targetedTitle = currentPayload.replace("ParentGoalId:", "").trim()
                        cbLinkTaskToGoal.isChecked = true

                        for (i in masterGoalsOptionsCache.indices) {
                            if (masterGoalsOptionsCache[i].title.equals(targetedTitle, ignoreCase = true)) {
                                spTaskGoalLink.setSelection(i + 1)
                                break
                            }
                        }
                    }
                }
            }
        }
    }
}