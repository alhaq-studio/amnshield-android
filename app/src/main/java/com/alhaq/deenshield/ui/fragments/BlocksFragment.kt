package com.alhaq.deenshield.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.alhaq.deenshield.R
import com.alhaq.deenshield.databinding.FragmentBlocksBinding
import com.alhaq.deenshield.premium.PremiumManager
import com.alhaq.deenshield.services.DeenShieldAccessibilityService
import com.alhaq.deenshield.ui.activity.FragmentActivity
import com.alhaq.deenshield.ui.activity.TimedActionActivity
import com.alhaq.deenshield.ui.fragments.ManageBlockSchedulesFragment
import com.alhaq.deenshield.ui.fragments.ManageLaunchLimitsFragment
import com.alhaq.deenshield.ui.fragments.features.BaseFeatureFragment
import com.alhaq.deenshield.utils.SavedPreferencesLoader
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class BlocksFragment : BaseFeatureFragment() {

    private var _binding: FragmentBlocksBinding? = null
    private val binding get() = _binding!!
    private val premiumManager by lazy { PremiumManager.getInstance(requireContext().applicationContext) }
    private val blocksLoader by lazy { SavedPreferencesLoader(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBlocksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClicks()
        refreshDashboard()
    }

    override fun onResume() {
        super.onResume()
        refreshDashboard()
    }

    private fun setupClicks() {
        binding.cardAppBlocker.setOnClickListener { openFeatureConfig("app_blocker", requiresPremium = true) }
        binding.cardKeywordBlocker.setOnClickListener { openFeatureConfig("keyword_blocker", requiresPremium = false) }
        binding.cardFocusMode.setOnClickListener { openFeatureConfig("focus_mode", requiresPremium = true) }
        binding.cardCheatHours.setOnClickListener { openCheatHours() }
        binding.cardSchedules.setOnClickListener { openSchedules() }
        binding.cardLaunchLimits.setOnClickListener { openLaunchLimits() }
        binding.fabAddBlock.setOnClickListener { showBlockSetupMenu() }
    }

    private fun refreshDashboard() {
        val serviceEnabled = isAccessibilityServiceEnabled(DeenShieldAccessibilityService::class.java)
        val premiumEnabled = premiumManager.isPremium()

        val appBlockedApps = blocksLoader.loadBlockedApps().size
        val appActive = premiumEnabled && blocksLoader.isAppBlockerFeatureEnabled() && appBlockedApps > 0 && serviceEnabled
        updateStatus(binding.chipAppBlockerStatus, appActive, if (appBlockedApps > 0) "$appBlockedApps apps" else getString(R.string.off))

        val keywordCount = blocksLoader.loadBlockedKeywords().size
        val keywordPackPrefs = requireContext().getSharedPreferences("keyword_blocker_packs", android.content.Context.MODE_PRIVATE)
        val adultPackEnabled = keywordPackPrefs.getBoolean("adult_blocker", false)
        val keywordActive = blocksLoader.isKeywordBlockerFeatureEnabled() && (keywordCount > 0 || adultPackEnabled) && serviceEnabled
        val keywordLabel = if (keywordCount > 0) "$keywordCount keywords" else if (adultPackEnabled) "Adult pack" else getString(R.string.off)
        updateStatus(binding.chipKeywordBlockerStatus, keywordActive, keywordLabel)

        val focusData = blocksLoader.getFocusModeData()
        val focusActive = premiumEnabled && focusData.isTurnedOn && serviceEnabled
        updateStatus(binding.chipFocusModeStatus, focusActive, if (focusActive) getString(R.string.on) else getString(R.string.off))

        val cheatCount = blocksLoader.loadAppBlockerCheatHoursList().size
        val scheduleCount = blocksLoader.loadAppBlockerScheduleRules().size
        val launchLimitCount = blocksLoader.loadAppLaunchLimitRules().size

        updateCountStatus(binding.chipCheatHoursStatus, cheatCount, "${cheatCount} windows")
        updateCountStatus(binding.chipSchedulesStatus, scheduleCount, "${scheduleCount} rules")
        updateCountStatus(binding.chipLaunchLimitsStatus, launchLimitCount, "${launchLimitCount} limits")

        val activeCount = listOf(appActive, keywordActive, focusActive, cheatCount > 0, scheduleCount > 0, launchLimitCount > 0).count { it }
        binding.txtActiveSummary.text = getString(R.string.active_blocks)
        binding.txtActiveCount.text = "$activeCount active"
    }

    private fun updateStatus(chip: com.google.android.material.chip.Chip, isEnabled: Boolean, label: String) {
        chip.text = label
        if (isEnabled) {
            chip.chipBackgroundColor = context?.let { ContextCompat.getColorStateList(it, R.color.md_theme_primaryContainer) }
            chip.setChipIconResource(R.drawable.baseline_done_24)
            chip.chipIconTint = context?.let { ContextCompat.getColorStateList(it, R.color.md_theme_primary) }
        } else {
            chip.chipBackgroundColor = context?.let { ContextCompat.getColorStateList(it, R.color.md_theme_errorContainer) }
            chip.setChipIconResource(R.drawable.baseline_stop_24)
            chip.chipIconTint = context?.let { ContextCompat.getColorStateList(it, R.color.md_theme_onErrorContainer) }
        }
    }

    private fun updateCountStatus(chip: com.google.android.material.chip.Chip, count: Int, label: String) {
        chip.text = label
        if (count > 0) {
            chip.chipBackgroundColor = context?.let { ContextCompat.getColorStateList(it, R.color.md_theme_secondaryContainer) }
            chip.setChipIconResource(R.drawable.baseline_done_24)
            chip.chipIconTint = context?.let { ContextCompat.getColorStateList(it, R.color.md_theme_secondary) }
        } else {
            chip.chipBackgroundColor = context?.let { ContextCompat.getColorStateList(it, R.color.md_theme_errorContainer) }
            chip.setChipIconResource(R.drawable.baseline_stop_24)
            chip.chipIconTint = context?.let { ContextCompat.getColorStateList(it, R.color.md_theme_onErrorContainer) }
        }
    }

    private fun showBlockSetupMenu() {
        val options = arrayOf(
            getString(R.string.app_blocker),
            getString(R.string.keyword_blocker),
            getString(R.string.focus_mode),
            getString(R.string.manage_cheat_hours),
            getString(R.string.manage_block_schedules),
            getString(R.string.manage_launch_limits)
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.block_setup)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openFeatureConfig("app_blocker", requiresPremium = true)
                    1 -> openFeatureConfig("keyword_blocker", requiresPremium = false)
                    2 -> openFeatureConfig("focus_mode", requiresPremium = true)
                    3 -> openCheatHours()
                    4 -> openSchedules()
                    5 -> openLaunchLimits()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun openFeatureConfig(featureType: String, requiresPremium: Boolean) {
        if (requiresPremium && !premiumManager.isPremium()) {
            showPremiumUpsell()
            return
        }

        val intent = Intent(requireContext(), FragmentActivity::class.java).apply {
            putExtra("feature_type", featureType)
        }
        startActivity(intent, activityOptions.toBundle())
    }

    private fun openCheatHours() {
        if (!premiumManager.isPremium()) {
            showPremiumUpsell()
            return
        }
        val intent = Intent(requireContext(), TimedActionActivity::class.java).apply {
            putExtra("selected_mode", TimedActionActivity.MODE_APP_BLOCKER_CHEAT_HOURS)
        }
        startActivity(intent, activityOptions.toBundle())
    }

    private fun openSchedules() {
        if (!premiumManager.isPremium()) {
            showPremiumUpsell()
            return
        }
        val intent = Intent(requireContext(), FragmentActivity::class.java).apply {
            putExtra("fragment", ManageBlockSchedulesFragment.FRAGMENT_ID)
        }
        startActivity(intent, activityOptions.toBundle())
    }

    private fun openLaunchLimits() {
        if (!premiumManager.isPremium()) {
            showPremiumUpsell()
            return
        }
        val intent = Intent(requireContext(), FragmentActivity::class.java).apply {
            putExtra("fragment", ManageLaunchLimitsFragment.FRAGMENT_ID)
        }
        startActivity(intent, activityOptions.toBundle())
    }

    private fun showPremiumUpsell() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.premium_required_title)
            .setMessage(getString(R.string.premium_required_message))
            .setPositiveButton(R.string.premium_view_plans) { _, _ ->
                val intent = Intent(requireContext(), FragmentActivity::class.java).apply {
                    putExtra("feature_type", "premium_features")
                }
                startActivity(intent, activityOptions.toBundle())
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}