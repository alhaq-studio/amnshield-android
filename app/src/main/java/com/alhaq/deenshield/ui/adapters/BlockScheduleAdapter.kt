package com.alhaq.deenshield.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alhaq.deenshield.data.blockers.AppBlockScheduleRule
import com.alhaq.deenshield.databinding.BlockScheduleItemBinding

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
            binding.scheduleTitle.text = rule.title

            // Format time window
            val startHour = rule.startMinute / 60
            val startMin = rule.startMinute % 60
            val endHour = rule.endMinute / 60
            val endMin = rule.endMinute % 60
            val timeWindow = String.format("%02d:%02d - %02d:%02d", startHour, startMin, endHour, endMin)
            binding.scheduleTimeWindow.text = timeWindow

            // Format recurrence
            val recurrenceText = when (rule.recurrence) {
                AppBlockScheduleRule.Recurrence.HOURLY -> "Hourly"
                AppBlockScheduleRule.Recurrence.DAILY -> "Daily"
                AppBlockScheduleRule.Recurrence.WEEKLY -> {
                    val days = mapOf(
                        1 to "Sun", 2 to "Mon", 3 to "Tue", 4 to "Wed",
                        5 to "Thu", 6 to "Fri", 7 to "Sat"
                    )
                    val selectedDayNames = rule.selectedDays
                        .map { days[it] ?: "" }
                        .filter { it.isNotEmpty() }
                        .joinToString(", ")
                    "Weekly: $selectedDayNames"
                }
                AppBlockScheduleRule.Recurrence.ALWAYS -> "Always Active"
            }
            binding.scheduleRecurrence.text = recurrenceText

            binding.btnEdit.setOnClickListener {
                onEdit(rule)
            }

            binding.btnDelete.setOnClickListener {
                onDelete(rule)
            }
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
