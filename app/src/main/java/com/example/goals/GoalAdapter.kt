package com.example.goals

import android.content.res.ColorStateList
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.helper.widget.Flow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class GoalAdapter(
    private var items: List<Any>,
    private val viewTypeMode: Int,
    private val onGoalStatusChanged: (Goal) -> Unit,
    private val onGoalEditRequested: (Goal) -> Unit,
    private val onGoalDeleteRequested: (Goal) -> Unit,
    private val onGoalDetailsRequested: (Goal) -> Unit,
    private val onHeaderClicked: ((String) -> Unit)? = null
) : RecyclerView.Adapter<GoalAdapter.UnifiedViewHolder>() {

    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    companion object {
        const val MODE_HOME_CHECKBOX = 1
        const val MODE_CARD_BLOCK = 2
        const val VIEW_TYPE_HEADER = 3
    }

    class UnifiedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDateHeaderTitle: TextView? = itemView.findViewById(R.id.tvDateHeaderTitle)
        val cvGoalCardRoot: com.google.android.material.card.MaterialCardView? = itemView.findViewById(R.id.cvGoalCardRoot)
        val goalCheckBackground: ConstraintLayout? = itemView.findViewById(R.id.goalCheckBackground)
        val llTextContainer: LinearLayout? = itemView.findViewById(R.id.llTextContainer)
        val ivGoalArrow: ImageView? = itemView.findViewById(R.id.ivGoalArrow)
        val tvGoalTitle: TextView? = itemView.findViewById(R.id.tvGoalTitle)


        val tvGoalCategoryBadge: TextView? = itemView.findViewById(R.id.tvGoalCategoryBadge)
        val tvGoalPriorityBadge: TextView? = itemView.findViewById(R.id.tvGoalPriorityBadge)

        val tvBadgeEndDate: TextView? = itemView.findViewById(R.id.tvBadgeEndDate)
        val tvBadgeTarget: TextView? = itemView.findViewById(R.id.tvBadgeTarget)
        val tvBadgeRecurrence: TextView? = itemView.findViewById(R.id.tvBadgeRecurrence)
        val tvBadgeLinked: TextView? = itemView.findViewById(R.id.tvBadgeLinked)

        val pbGoalTimeline: ProgressBar? = itemView.findViewById(R.id.pbGoalTimeline)
        val ivGoalIcon: ImageView? = itemView.findViewById(R.id.ivGoalIcon)
        val flowMetadata: Flow? = itemView.findViewById(R.id.flowMetadata)
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position] is String) VIEW_TYPE_HEADER else viewTypeMode
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UnifiedViewHolder {
        val layout = when (viewType) {
            VIEW_TYPE_HEADER -> R.layout.item_date_header
            MODE_HOME_CHECKBOX -> R.layout.item_goal_card
            else -> R.layout.item_goal
        }
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return UnifiedViewHolder(view)
    }

    override fun onBindViewHolder(holder: UnifiedViewHolder, position: Int) {
        if (getItemViewType(position) == VIEW_TYPE_HEADER) {
            val headerText = items[position] as String
            holder.tvDateHeaderTitle?.text = headerText
            holder.itemView.setOnClickListener { onHeaderClicked?.invoke(headerText) }
            return
        }

        val currentGoal = items[position] as Goal
        val isCardBlockMode = getItemViewType(position) == MODE_HOME_CHECKBOX

        holder.tvGoalTitle?.text = currentGoal.title.uppercase()

        if (currentGoal.category.isNotEmpty()) {
            holder.tvGoalCategoryBadge?.visibility = View.VISIBLE
            holder.tvGoalCategoryBadge?.text = currentGoal.category.uppercase()
        } else holder.tvGoalCategoryBadge?.visibility = View.GONE

        if (currentGoal.priority.isNotEmpty()) {
            holder.tvGoalPriorityBadge?.visibility = View.VISIBLE
            holder.tvGoalPriorityBadge?.text = currentGoal.priority.uppercase()
        } else holder.tvGoalPriorityBadge?.visibility = View.GONE

        configureDynamicGoalFields(holder, currentGoal)

        val clickListener = View.OnClickListener {
            if (isCardBlockMode) {
                onGoalDetailsRequested(currentGoal)
            } else {
                currentGoal.isAchieved = !currentGoal.isAchieved
                updateLayoutVisualState(holder, currentGoal, false)
                onGoalStatusChanged(currentGoal)
            }
        }

        val longClickListener = View.OnLongClickListener { view ->
            showContextMenu(view, currentGoal, holder, isCardBlockMode)
            true
        }

        holder.llTextContainer?.setOnClickListener(clickListener)
        holder.llTextContainer?.setOnLongClickListener(longClickListener)
        holder.ivGoalArrow?.setOnClickListener { onGoalDetailsRequested(currentGoal) }

        updateLayoutVisualState(holder, currentGoal, isCardBlockMode)
    }

    private fun configureDynamicGoalFields(holder: UnifiedViewHolder, currentGoal: Goal) {
        holder.tvBadgeEndDate?.visibility = View.GONE
        holder.tvBadgeTarget?.visibility = View.GONE
        holder.tvBadgeRecurrence?.visibility = View.GONE
        holder.tvBadgeLinked?.visibility = View.GONE
        holder.pbGoalTimeline?.visibility = View.GONE

        // Maintain single line auto-shrinking text constraints across badges
        val allBadgesList = listOfNotNull(
            holder.tvGoalCategoryBadge, holder.tvGoalPriorityBadge,
            holder.tvBadgeEndDate, holder.tvBadgeTarget,
            holder.tvBadgeRecurrence, holder.tvBadgeLinked
        )
        allBadgesList.forEach { badgeTextView ->
            badgeTextView.maxLines = 1
            badgeTextView.setHorizontallyScrolling(true)
            TextViewCompat.setAutoSizeTextTypeWithDefaults(
                badgeTextView,
                TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM
            )
        }

        val payload = currentGoal.durationText ?: ""
        var targetExtracted = ""
        var recurrenceExtracted = ""
        var isLinkedToParent = false

        // Added properties to extract numeric tracking milestones
        var targetValueExtracted: Double? = null
        var currentProgressExtracted: Double? = null

        if (payload.isNotEmpty()) {
            val parts = payload.split("|")
            parts.forEach { part ->
                val trimmedPart = part.trim()
                when {
                    trimmedPart.contains("Frequency:", ignoreCase = true) -> {
                        recurrenceExtracted = trimmedPart.substringAfter("Frequency:").trim()
                    }
                    trimmedPart.contains("MetricUnit:", ignoreCase = true) -> {
                        targetExtracted = trimmedPart.substringAfter("MetricUnit:").trim()
                    }
                    trimmedPart.contains("ParentGoalId:", ignoreCase = true) -> {
                        isLinkedToParent = true
                    }
                    trimmedPart.contains("TargetValue:", ignoreCase = true) -> {
                        targetValueExtracted = trimmedPart.substringAfter("TargetValue:").trim().toDoubleOrNull()
                    }
                    trimmedPart.contains("CurrentProgress:", ignoreCase = true) -> {
                        currentProgressExtracted = trimmedPart.substringAfter("CurrentProgress:").trim().toDoubleOrNull()
                    }
                }
            }
        }

        val hasValidEndDate = currentGoal.endDate != null && currentGoal.endDate!! > 0L

        // 1. Re-render individual text item badges safely based on entity rules
        when (currentGoal.itemType) {
            GoalType.GOAL -> {
                if (hasValidEndDate) {
                    holder.tvBadgeEndDate?.text = dateFormatter.format(currentGoal.endDate)
                    holder.tvBadgeEndDate?.visibility = View.VISIBLE
                }
                if (targetExtracted.isNotEmpty()) {
                    holder.tvBadgeTarget?.text = targetExtracted
                    holder.tvBadgeTarget?.visibility = View.VISIBLE
                }
            }
            GoalType.HABIT, GoalType.TASK -> {
                if (hasValidEndDate) {
                    holder.tvBadgeEndDate?.text = dateFormatter.format(currentGoal.endDate)
                    holder.tvBadgeEndDate?.visibility = View.VISIBLE
                }
                if (recurrenceExtracted.isNotEmpty()) {
                    holder.tvBadgeRecurrence?.text = recurrenceExtracted
                    holder.tvBadgeRecurrence?.visibility = View.VISIBLE
                }

                if (isLinkedToParent) {
                    holder.tvBadgeLinked?.text = "LINKED 🎯"
                    holder.tvBadgeLinked?.visibility = View.VISIBLE
                }
            }
        }

        // 2. Extracted Progress Bar execution pipeline logic
        val shouldShowProgress = hasValidEndDate || isLinkedToParent || (targetValueExtracted != null && targetValueExtracted > 0)

        if (shouldShowProgress && holder.pbGoalTimeline != null && currentGoal.itemType == GoalType.GOAL) {
            holder.pbGoalTimeline.visibility = View.VISIBLE
            calculateFlexibleProgress(holder.pbGoalTimeline, currentGoal, targetValueExtracted, currentProgressExtracted)
        } else {
            holder.pbGoalTimeline?.visibility = View.GONE
        }

        holder.flowMetadata?.requestLayout()
    }

    private fun calculateFlexibleProgress(
        progressBar: ProgressBar,
        currentGoal: Goal,
        targetValue: Double?,
        currentProgress: Double?
    ) {
        progressBar.isIndeterminate = false
        progressBar.max = 100

        // If numeric targets are passed down, track numerical metrics. Otherwise, fallback to timeframe durations.
        if (targetValue != null && targetValue > 0) {
            val progressAmount = currentProgress ?: 0.0
            val percentage = ((progressAmount / targetValue) * 100).toInt()
            progressBar.progress = percentage.coerceIn(0, 100)
        } else {
            val endDateValue = currentGoal.endDate
            if (endDateValue == null || endDateValue <= 0L) {
                progressBar.progress = 0
                return
            }

            val totalDuration = endDateValue - currentGoal.startDate
            val currentTime = System.currentTimeMillis()

            if (totalDuration <= 0) {
                progressBar.progress = 100
                return
            }

            if (currentTime < currentGoal.startDate) {
                progressBar.progress = 0
                return
            }

            val elapsed = currentTime - currentGoal.startDate
            val percentage = ((elapsed.toDouble() / totalDuration.toDouble()) * 100).toInt()
            progressBar.progress = percentage.coerceIn(0, 100)
        }
    }

    private fun updateLayoutVisualState(holder: UnifiedViewHolder, currentGoal: Goal, isCard: Boolean) {
        val context = holder.itemView.context

        if (currentGoal.isAchieved) {
            holder.tvGoalTitle?.paintFlags = holder.tvGoalTitle!!.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.tvGoalTitle?.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))


            holder.cvGoalCardRoot?.setCardBackgroundColor(ContextCompat.getColor(context, R.color.Card_bg))
            holder.cvGoalCardRoot?.scaleX = 0.95f
            holder.cvGoalCardRoot?.scaleY = 0.95f

            holder.ivGoalIcon?.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.Secondary))
        } else {
            holder.tvGoalTitle?.paintFlags = holder.tvGoalTitle!!.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.tvGoalTitle?.setTextColor(ContextCompat.getColor(context, R.color.Primary))

            holder.cvGoalCardRoot?.setCardBackgroundColor(ContextCompat.getColor(context, R.color.Secondary))
            holder.cvGoalCardRoot?.scaleX = 1.0f
            holder.cvGoalCardRoot?.scaleY = 1.0f

            holder.ivGoalIcon?.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.Primary))
        }
    }

    private fun showContextMenu(anchor: View, goal: Goal, holder: UnifiedViewHolder, isCard: Boolean) {
        val popup = PopupMenu(anchor.context, anchor)

        popup.menu.add(0, 1, 0, "Edit Item")
        popup.menu.add(0, 2, 1, if (goal.isAchieved) "Mark as Active" else "Archive / Mark Done")
        popup.menu.add(0, 3, 2, "Delete Item")

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                1 -> onGoalEditRequested(goal)
                2 -> {
                    goal.isAchieved = !goal.isAchieved
                    updateLayoutVisualState(holder, goal, isCard)
                    onGoalStatusChanged(goal)
                }
                3 -> onGoalDeleteRequested(goal)
            }
            true
        }
        popup.show()
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<Any>) {
        this.items = newItems
        notifyDataSetChanged()
    }
}