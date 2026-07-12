package com.alhaq.amnshield.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.alhaq.amnshield.data.blockers.AppLaunchLimitRule
import com.alhaq.amnshield.databinding.FragmentManageLaunchLimitsBinding
import com.alhaq.amnshield.premium.PremiumManager
import com.alhaq.amnshield.services.AmnShieldAccessibilityService
import com.alhaq.amnshield.ui.adapters.LaunchLimitAdapter
import com.alhaq.amnshield.ui.dialogs.SetLaunchLimitDialog
import com.alhaq.amnshield.utils.SavedPreferencesLoader

/**
 * Fragment for viewing and managing all app launch limit rules.
 * Shows a list of apps with launch limits and allows editing/deleting.
 */
class ManageLaunchLimitsFragment : Fragment() {

    private var _binding: FragmentManageLaunchLimitsBinding? = null
    private val binding get() = _binding!!

    private var adapter: LaunchLimitAdapter? = null
    private lateinit var savedPrefs: SavedPreferencesLoader

    companion object {
        const val FRAGMENT_ID = "manage_launch_limits"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageLaunchLimitsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!PremiumManager.getInstance(requireContext().applicationContext).isPremium()) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Premium Required")
                .setMessage("Launch limits are available for premium users.")
                .setPositiveButton("View Plans") { _, _ ->
                    val intent = Intent(requireContext(), com.alhaq.amnshield.ui.activity.FragmentActivity::class.java)
                    intent.putExtra("feature_type", "premium_features")
                    startActivity(intent)
                    requireActivity().finish()
                }
                .setNegativeButton("Close") { _, _ -> requireActivity().finish() }
                .setCancelable(false)
                .show()
            return
        }

        savedPrefs = SavedPreferencesLoader(requireContext())

        // Setup back button
        binding.btnBackArrow.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Setup adapter
        adapter = LaunchLimitAdapter(
            context = requireContext(),
            onEdit = { rule -> editLaunchLimit(rule) },
            onDelete = { rule -> deleteLaunchLimit(rule) }
        )

        binding.launchLimitsList.adapter = adapter

        // Load and display limits
        loadLaunchLimits()
    }

    override fun onResume() {
        super.onResume()
        // Reload limits each time fragment becomes visible
        loadLaunchLimits()
    }

    private fun loadLaunchLimits() {
        val rules = savedPrefs.loadAppLaunchLimitRules()

        if (rules.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.launchLimitsList.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.launchLimitsList.visibility = View.VISIBLE
            adapter?.submitList(rules)
        }
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
                        savedPrefs.addAppLaunchLimitRule(updatedRule)
                        Toast.makeText(
                            requireContext(),
                            "Launch limit updated: ${updatedRule.getDescription()}",
                            Toast.LENGTH_SHORT
                        ).show()
                        sendRefreshRequest()
                        loadLaunchLimits()
                    }
                }
            )
            dialog.show(childFragmentManager, "edit_launch_limit_${rule.id}")
        } catch (e: Exception) {
            android.util.Log.e("ManageLaunchLimits", "Error editing launch limit for ${rule.packageName}", e)
            Toast.makeText(requireContext(), "Could not edit limit — try again", Toast.LENGTH_SHORT).show()
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
                    savedPrefs.removeAppLaunchLimitRule(rule.packageName)
                    Toast.makeText(
                        requireContext(),
                        "Launch limit removed",
                        Toast.LENGTH_SHORT
                    ).show()
                    sendRefreshRequest()
                    loadLaunchLimits()
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            android.util.Log.e("ManageLaunchLimits", "Error deleting launch limit for ${rule.packageName}", e)
            Toast.makeText(requireContext(), "Could not delete limit — try again", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendRefreshRequest() {
        val intent = Intent(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_APP_BLOCKER)
        requireContext().sendBroadcast(intent.setPackage(requireContext().packageName))
        val unifiedIntent = Intent(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_UNIFIED_FEATURE_SCHEDULES)
        requireContext().sendBroadcast(unifiedIntent.setPackage(requireContext().packageName))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
