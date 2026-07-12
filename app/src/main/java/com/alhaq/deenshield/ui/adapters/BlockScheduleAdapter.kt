package com.alhaq.deenshield.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alhaq.deenshield.R
import com.alhaq.deenshield.data.blockers.AppBlockScheduleRule
import com.alhaq.deenshield.databinding.BlockScheduleItemBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BlockScheduleAdapter(
    private val onEdit: (AppBlockScheduleRule) -> Unit,
    private val onDelete: (AppBlockScheduleRule) -> Unit
) : ListAdapter<AppBlockScheduleRule, BlockScheduleAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(
        private val binding: BlockScheduleItemBinding,
        private val onEdit: (AppBlockScheduleRule) -> Unit,
        private val onDelete: (AppBlockScheduleRule) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(rule: AppBlockScheduleRule) {
            val ctx = binding.root.context
            binding.scheduleTitle.text = rule.title

            // Color the type indicator circle: green for CHEAT windows, primary for BLOCK rules
            val isCheat = rule.type == AppBlockScheduleRule.RuleType.CHEAT
            val circleColor = if (isCheat) R.color.emerald_primaryContainer else R.color.md_theme_primaryContainer
            val iconColor  = if (isCheat) R.color.emerald_primary else R.color.md_theme_primary
            binding.cardTypeIndicator.setCardBackgroundColor(ContextCompat.getColorStateList(ctx, circleColor))
            binding.imgTypeIcon.imageTintList = ContextCompat.getColorStateList(ctx, iconColor)

            if (rule.groupId.isNullOrBlank()) {
                binding.scheduleGroupBadge.visibility = View.GONE
            } else {
                binding.scheduleGroupBadge.visibility = View.VISIBLE
                binding.scheduleGroupBadge.text = rule.groupTitle?.takeIf { it.isNotBlank() } ?: "Batch schedule"
            }

            val timeWindow = when (rule.recurrence) {
                AppBlockScheduleRule.Recurrence.ALWAYS -> "Always active"
                AppBlockScheduleRule.Recurrence.HOURLY -> {
                    if (rule.activeUntilMillis > System.currentTimeMillis()) {
                        val until = SimpleDateFormat("HH:mm", Locale.getDefault())
                            .format(Date(rule.activeUntilMillis))
                        "From now until $until"
                    } else {
                        "From now for ${rule.durationHours.coerceAtLeast(1)}h"
                    }
                }

                else -> {
                    val startHour = rule.startMinute / 60
                    val startMin = rule.startMinute % 60
                    val endHour = rule.endMinute / 60
                    val endMin = rule.endMinute % 60
                    String.format("%02d:%02d – %02d:%02d", startHour, startMin, endHour, endMin)
                }
            }
            binding.scheduleTimeWindow.text = timeWindow

            binding.scheduleRecurrence.text = when (rule.recurrence) {
                AppBlockScheduleRule.Recurrence.HOURLY -> "Repeats hourly"
                AppBlockScheduleRule.Recurrence.DAILY -> "Repeats daily"
                AppBlockScheduleRule.Recurrence.WEEKLY -> {
                    val days = mapOf(
                        1 to "Sun", 2 to "Mon", 3 to "Tue", 4 to "Wed",
                        5 to "Thu", 6 to "Fri", 7 to "Sat"
                    )
                    val selectedDayNames = rule.selectedDays
                        .map { days[it] ?: "" }
                        .filter { it.isNotEmpty() }
                        .joinToString(", ")
                    if (selectedDayNames.isBlank()) {
                        "Repeats weekly (no days set — edit to fix)"
                    } else {
                        "Repeats weekly on $selectedDayNames"
                    }
                }
                AppBlockScheduleRule.Recurrence.ALWAYS -> "Always active"
            }

            binding.btnEdit.setOnClickListener { onEdit(rule) }
            binding.btnDelete.setOnClickListener { onDelete(rule) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = BlockScheduleItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onEdit, onDelete)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    object DiffCallback : DiffUtil.ItemCallback<AppBlockScheduleRule>() {
        override fun areItemsTheSame(oldItem: AppBlockScheduleRule, newItem: AppBlockScheduleRule): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AppBlockScheduleRule, newItem: AppBlockScheduleRule): Boolean {
            return oldItem == newItem
        }
    }
}
