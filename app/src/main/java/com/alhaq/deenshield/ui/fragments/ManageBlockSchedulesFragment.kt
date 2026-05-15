package com.alhaq.deenshield.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.alhaq.deenshield.R
import com.alhaq.deenshield.data.blockers.AppBlockScheduleRule
import com.alhaq.deenshield.databinding.FragmentManageBlockSchedulesBinding
import com.alhaq.deenshield.services.DeenShieldAccessibilityService
import com.alhaq.deenshield.data.blockers.AppLaunchLimitRule
import com.alhaq.deenshield.ui.adapters.BlockScheduleAdapter
import com.alhaq.deenshield.ui.adapters.LaunchLimitAdapter
import com.alhaq.deenshield.ui.dialogs.SetLaunchLimitDialog
import com.alhaq.deenshield.utils.SavedPreferencesLoader
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ManageBlockSchedulesFragment : Fragment() {

    private var _binding: FragmentManageBlockSchedulesBinding? = null
    private val binding get() = _binding!!
    private lateinit var savedPreferencesLoader: SavedPreferencesLoader
    private lateinit var scheduleAdapter: BlockScheduleAdapter
    private var launchLimitAdapter: LaunchLimitAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageBlockSchedulesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        savedPreferencesLoader = SavedPreferencesLoader(requireContext())
        
        // Setup Block Schedules RecyclerView
        scheduleAdapter = BlockScheduleAdapter(
            onEdit = { rule -> editSchedule(rule) },
            onDelete = { rule -> deleteSchedule(rule) }
        )
        binding.schedulesList.layoutManager = LinearLayoutManager(requireContext())
        binding.schedulesList.adapter = scheduleAdapter

        // Setup Launch Limits RecyclerView
        launchLimitAdapter = LaunchLimitAdapter(
            context = requireContext(),
            onEdit = { rule -> editLaunchLimit(rule) },
            onDelete = { rule -> deleteLaunchLimit(rule) }
        )
        binding.launchLimitsList.layoutManager = LinearLayoutManager(requireContext())
        binding.launchLimitsList.adapter = launchLimitAdapter

        binding.btnBackArrow.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        loadSchedulesAndLimits()
    }

    override fun onResume() {
        super.onResume()
        loadSchedulesAndLimits()
    }

    private fun loadSchedulesAndLimits() {
        // Load block schedules
        val schedules = savedPreferencesLoader.loadAppBlockerScheduleRules()
        if (schedules.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.schedulesList.visibility = View.GONE
            binding.emptyStateText.text = "No block schedules configured yet.\n\nCreate schedules from the App Blocker settings or per-app recommendation cards."
        } else {
            binding.emptyState.visibility = View.GONE
            binding.schedulesList.visibility = View.VISIBLE
            scheduleAdapter.submitList(schedules)
        }

        // Load launch limits
        val launchLimits = savedPreferencesLoader.loadAppLaunchLimitRules()
        if (launchLimits.isEmpty()) {
            binding.launchLimitsEmptyState.visibility = View.VISIBLE
            binding.launchLimitsList.visibility = View.GONE
        } else {
            binding.launchLimitsEmptyState.visibility = View.GONE
            binding.launchLimitsList.visibility = View.VISIBLE
            launchLimitAdapter?.submitList(launchLimits)
        }
    }

    private fun editSchedule(rule: AppBlockScheduleRule) {
        // For now, show a delete-and-recreate flow. In a full implementation,
        // you'd open an edit dialog with the rule's current values pre-filled.
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Schedule")
            .setMessage("Edit is not yet available. Would you like to delete this schedule and create a new one?")
            .setPositiveButton("Delete & Create New") { _, _ ->
                deleteSchedule(rule)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteSchedule(rule: AppBlockScheduleRule) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Schedule")
            .setMessage("Delete schedule: ${rule.title}?")
            .setPositiveButton("Delete") { _, _ ->
                savedPreferencesLoader.removeAppBlockerScheduleRule(rule.id)
                sendRefreshRequest()
                loadSchedulesAndLimits()
                Toast.makeText(requireContext(), "Schedule deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun editLaunchLimit(rule: AppLaunchLimitRule) {
        try {
            val appInfo = requireContext().packageManager.getApplicationInfo(rule.packageName, 0)
            val appName = appInfo.loadLabel(requireContext().packageManager).toString()

            val dialog = SetLaunchLimitDialog(
                packageName = rule.packageName,
                appName = appName,
                onSave = { updatedRule ->
                    if (updatedRule != null) {
                        savedPreferencesLoader.addAppLaunchLimitRule(updatedRule)
                        Toast.makeText(
                            requireContext(),
                            "Launch limit updated: ${updatedRule.getDescription()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    sendRefreshRequest()
                    loadSchedulesAndLimits()
                }
            )
            dialog.show(childFragmentManager, "edit_launch_limit_${rule.id}")
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error editing limit", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteLaunchLimit(rule: AppLaunchLimitRule) {
        try {
            val appInfo = requireContext().packageManager.getApplicationInfo(rule.packageName, 0)
            val appName = appInfo.loadLabel(requireContext().packageManager).toString()

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Launch Limit?")
                .setMessage("Remove launch limit for $appName?")
                .setPositiveButton("Delete") { _, _ ->
                    savedPreferencesLoader.removeAppLaunchLimitRule(rule.packageName)
                    Toast.makeText(
                        requireContext(),
                        "Launch limit removed",
                        Toast.LENGTH_SHORT
                    ).show()
                    sendRefreshRequest()
                    loadSchedulesAndLimits()
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error deleting limit", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendRefreshRequest() {
        val intent = Intent(DeenShieldAccessibilityService.INTENT_ACTION_REFRESH_APP_BLOCKER)
        requireContext().sendBroadcast(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val FRAGMENT_ID = "manage_block_schedules"
    }
}
