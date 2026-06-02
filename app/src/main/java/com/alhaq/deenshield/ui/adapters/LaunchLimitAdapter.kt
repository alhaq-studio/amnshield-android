package com.alhaq.deenshield.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alhaq.deenshield.R
import com.alhaq.deenshield.data.blockers.AppLaunchLimitRule
import com.alhaq.deenshield.databinding.LaunchLimitItemBinding
import com.alhaq.deenshield.utils.SavedPreferencesLoader

/**
 * Adapter for displaying and managing app launch limit rules.
 * Shows app name, limit description, and current launch count.
 */
class LaunchLimitAdapter(
    private val context: Context,
    private val onEdit: (rule: AppLaunchLimitRule) -> Unit,
    private val onDelete: (rule: AppLaunchLimitRule) -> Unit
) : ListAdapter<AppLaunchLimitRule, LaunchLimitAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = LaunchLimitItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: LaunchLimitItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(rule: AppLaunchLimitRule) {
            try {
                val appInfo = context.packageManager.getApplicationInfo(rule.packageName, 0)
                val appName = appInfo.loadLabel(context.packageManager).toString()
                val appIcon = appInfo.loadIcon(context.packageManager)
                binding.appName.text = appName
            } catch (e: Exception) {
                binding.appName.text = rule.packageName
            }

            binding.limitDescription.text = rule.getDescription()

            // Get current launch count
            val savedPrefs = SavedPreferencesLoader(context)
            val currentCount = savedPrefs.getCurrentLaunchCount(rule.packageName, rule)
            binding.currentCount.text = "Current: $currentCount / ${rule.maxLaunches}"

            binding.btnEdit.setOnClickListener {
                onEdit(rule)
            }

            binding.btnDelete.setOnClickListener {
                onDelete(rule)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AppLaunchLimitRule>() {
        override fun areItemsTheSame(oldItem: AppLaunchLimitRule, newItem: AppLaunchLimitRule): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AppLaunchLimitRule, newItem: AppLaunchLimitRule): Boolean {
            return oldItem == newItem
        }
    }
}
