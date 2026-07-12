package com.alhaq.amnshield.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.alhaq.amnshield.R
import com.alhaq.amnshield.databinding.FragmentBlocksBinding
import com.alhaq.amnshield.premium.PremiumManager
import com.alhaq.amnshield.services.AmnShieldAccessibilityService
import com.alhaq.amnshield.ui.activity.FragmentActivity
import com.alhaq.amnshield.ui.activity.TimedActionActivity
import com.alhaq.amnshield.ui.fragments.ManageBlockSchedulesFragment
import com.alhaq.amnshield.ui.fragments.ManageLaunchLimitsFragment
import com.alhaq.amnshield.ui.fragments.features.BaseFeatureFragment
import com.alhaq.amnshield.utils.SavedPreferencesLoader
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
        val serviceEnabled = isAccessibilityServiceEnabled(AmnShieldAccessibilityService::class.java)
        val premiumEnabled = premiumManager.isPremium()

        val appBlockedApps = blocksLoader.loadBlockedApps().size
        val appActive = premiumEnabled && blocksLoader.isAppBlockerFeatureEnabled() && appBlockedApps > 0 && serviceEnabled
        updateStatus(binding.chipAppBlockerStatus, appActive, if (appBlockedApps > 0) "$appBlockedApps apps" else getString(R.string.off))

        val keywordCount = blocksLoader.loadBlockedKeywords().size
        val adultPackEnabled = blocksLoader.isKeywordBlockerAdultPackEnabled()
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
        
        context?.let { ctx ->
            if (activeCount > 0) {
                // Secure Green Active State
                binding.cardActiveStatus.setCardBackgroundColor(ContextCompat.getColorStateList(ctx, R.color.emerald_primaryContainer))
                binding.cardActiveStatus.strokeColor = ContextCompat.getColor(ctx, R.color.emerald_primary)
                binding.txtActiveSummary.text = "Blocking Active"
                binding.txtActiveSummary.setTextColor(ContextCompat.getColor(ctx, R.color.emerald_onPrimaryContainer))
                binding.txtActiveCount.text = "$activeCount active protection rules running"
                binding.txtActiveCount.setTextColor(ContextCompat.getColor(ctx, R.color.emerald_onPrimaryContainer))
                
                binding.cardLockContainer.setCardBackgroundColor(ContextCompat.getColorStateList(ctx, R.color.emerald_primary))
                binding.imgLockIcon.imageTintList = ContextCompat.getColorStateList(ctx, R.color.white)
            } else {
                // Inactive Grey State
                binding.cardActiveStatus.setCardBackgroundColor(ContextCompat.getColorStateList(ctx, R.color.md_theme_surfaceContainerHigh))
                binding.cardActiveStatus.strokeColor = ContextCompat.getColor(ctx, R.color.md_theme_outlineVariant)
                binding.txtActiveSummary.text = "No Active Blocks"
                binding.txtActiveSummary.setTextColor(ContextCompat.getColor(ctx, R.color.md_theme_onSurface))
                binding.txtActiveCount.text = "Enable protection features below"
                binding.txtActiveCount.setTextColor(ContextCompat.getColor(ctx, R.color.md_theme_onSurfaceVariant))
                
                binding.cardLockContainer.setCardBackgroundColor(ContextCompat.getColorStateList(ctx, R.color.md_theme_outline))
                binding.imgLockIcon.imageTintList = ContextCompat.getColorStateList(ctx, R.color.white)
            }
        }
    }

    private fun updateStatus(chip: com.google.android.material.chip.Chip, isEnabled: Boolean, label: String) {
        chip.text = label
        val ctx = context ?: return
        if (isEnabled) {
            chip.chipBackgroundColor = ContextCompat.getColorStateList(ctx, R.color.emerald_primaryContainer)
            chip.setTextColor(ContextCompat.getColor(ctx, R.color.emerald_primary))
            chip.setChipIconResource(R.drawable.baseline_done_24)
            chip.chipIconTint = ContextCompat.getColorStateList(ctx, R.color.emerald_primary)
        } else {
            chip.chipBackgroundColor = ContextCompat.getColorStateList(ctx, R.color.md_theme_surfaceVariant)
            chip.setTextColor(ContextCompat.getColor(ctx, R.color.md_theme_onSurfaceVariant))
            chip.setChipIconResource(R.drawable.baseline_stop_24)
            chip.chipIconTint = ContextCompat.getColorStateList(ctx, R.color.md_theme_onSurfaceVariant)
        }
    }

    private fun updateCountStatus(chip: com.google.android.material.chip.Chip, count: Int, label: String) {
        chip.text = label
        val ctx = context ?: return
        if (count > 0) {
            chip.chipBackgroundColor = ContextCompat.getColorStateList(ctx, R.color.md_theme_secondaryContainer)
            chip.setTextColor(ContextCompat.getColor(ctx, R.color.md_theme_secondary))
            chip.setChipIconResource(R.drawable.baseline_done_24)
            chip.chipIconTint = ContextCompat.getColorStateList(ctx, R.color.md_theme_secondary)
        } else {
            chip.chipBackgroundColor = ContextCompat.getColorStateList(ctx, R.color.md_theme_surfaceVariant)
            chip.setTextColor(ContextCompat.getColor(ctx, R.color.md_theme_onSurfaceVariant))
            chip.setChipIconResource(R.drawable.baseline_stop_24)
            chip.chipIconTint = ContextCompat.getColorStateList(ctx, R.color.md_theme_onSurfaceVariant)
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