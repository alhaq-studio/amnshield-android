package com.alhaq.amnshield.ui.fragments.features

import android.content.Intent
import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.alhaq.amnshield.R
import com.alhaq.amnshield.blockers.ReelBlocker
import com.alhaq.amnshield.services.AmnShieldAccessibilityService
import com.alhaq.amnshield.ui.activity.SelectAppsActivity
import com.alhaq.amnshield.ui.activity.TimedActionActivity
import com.alhaq.amnshield.ui.activity.ManageKeywordsActivity
import com.alhaq.amnshield.ui.activity.FragmentActivity
import com.alhaq.amnshield.ui.fragments.ManageBlockSchedulesFragment
import com.alhaq.amnshield.ui.dialogs.TweakAppBlockerWarning
import com.alhaq.amnshield.ui.dialogs.TweakViewBlockerCheatHours
import com.alhaq.amnshield.ui.dialogs.TweakViewBlockerWarning
import com.alhaq.amnshield.ui.dialogs.TweakUsageTracker
import com.alhaq.amnshield.ui.dialogs.TweakKeywordBlocker
import com.alhaq.amnshield.ui.dialogs.TweakKeywordPack
import com.alhaq.amnshield.ui.fragments.usage.AllAppsUsageFragment
import com.alhaq.amnshield.utils.SavedPreferencesLoader
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.alhaq.amnshield.premium.PremiumManager
import com.alhaq.amnshield.data.blockers.PackageWand
import com.google.android.material.materialswitch.MaterialSwitch

/**
 * Comprehensive App Blocker configuration screen with all options
 */
class AppBlockerConfigFragment : BaseFeatureFragment() {
    
    private var _binding: com.alhaq.amnshield.databinding.FragmentAppBlockerConfigBinding? = null
    private val binding get() = _binding!!
    
    private val selectAppsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val selectedApps = result.data?.getStringArrayListExtra("SELECTED_APPS")
            selectedApps?.let {
                savedPreferencesLoader.saveBlockedApps(it.toSet())
                sendRefreshRequest(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_APP_BLOCKER)
                updateSelectedAppsCount(it.size)
            }
        }
    }

    private val cheatHoursLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            sendRefreshRequest(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_APP_BLOCKER)
            Toast.makeText(requireContext(), "Cheat hours updated", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = com.alhaq.amnshield.databinding.FragmentAppBlockerConfigBinding.inflate(inflater, container, false)
        
        val premiumManager = PremiumManager.getInstance(requireContext().applicationContext)
        if (!premiumManager.isPremium()) {
            binding.configContainer.visibility = View.GONE
            binding.statusCard.visibility = View.VISIBLE
            binding.statusMessage.text = getString(R.string.premium_required_message)
            binding.btnStatusAction.text = getString(R.string.premium_view_plans)
            binding.btnStatusAction.setOnClickListener { openPremiumScreen() }
            return binding.root
        }

        // Check service status
        if (!isAccessibilityServiceEnabled(AmnShieldAccessibilityService::class.java)) {
            binding.configContainer.visibility = View.GONE
            binding.statusCard.visibility = View.VISIBLE
            binding.statusMessage.text = "Please enable the main AmnShield Accessibility Service"
            binding.btnStatusAction.text = "Enable Service"
            binding.btnStatusAction.setOnClickListener {
                showAccessibilityInfoDialog("AmnShield Accessibility Service", AmnShieldAccessibilityService::class.java)
            }
            return binding.root
        }

        binding.statusCard.visibility = View.GONE
        binding.configContainer.visibility = View.VISIBLE

        val isFeatureEnabled = savedPreferencesLoader.isAppBlockerFeatureEnabled()
        binding.switchAppBlockerEnabled.isChecked = isFeatureEnabled
        setAppBlockerControlsEnabled(isFeatureEnabled)

        binding.switchAppBlockerEnabled.setOnCheckedChangeListener { _, isChecked ->
            savedPreferencesLoader.setAppBlockerFeatureEnabled(isChecked)
            setAppBlockerControlsEnabled(isChecked)
            sendRefreshRequest(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_APP_BLOCKER)
        }
        
        updateSelectedAppsCount(savedPreferencesLoader.loadBlockedApps().size)

        binding.btnSelectApps.setOnClickListener {
            val intent = Intent(requireContext(), SelectAppsActivity::class.java)
            intent.putStringArrayListExtra(
                "PRE_SELECTED_APPS",
                ArrayList(savedPreferencesLoader.loadBlockedApps())
            )
            selectAppsLauncher.launch(intent, activityOptions)
        }

        binding.btnCheatHours.setOnClickListener {
            val intent = Intent(requireContext(), TimedActionActivity::class.java)
            intent.putExtra("selected_mode", TimedActionActivity.MODE_APP_BLOCKER_CHEAT_HOURS)
            cheatHoursLauncher.launch(intent, activityOptions)
        }

        binding.btnBlockSchedules.setOnClickListener {
            val intent = Intent(requireContext(), FragmentActivity::class.java).apply {
                putExtra("fragment", ManageBlockSchedulesFragment.FRAGMENT_ID)
            }
            startActivity(intent)
        }

        binding.btnLaunchLimits.setOnClickListener {
            val intent = Intent(requireContext(), FragmentActivity::class.java).apply {
                putExtra("fragment", com.alhaq.amnshield.ui.fragments.ManageLaunchLimitsFragment.FRAGMENT_ID)
            }
            startActivity(intent)
        }

        binding.btnWarningScreen.setOnClickListener {
            TweakAppBlockerWarning(savedPreferencesLoader).show(
                childFragmentManager,
                "tweak_app_blocker_warning"
            )
        }
        
        binding.switchAutoBlock.isChecked = savedPreferencesLoader.isAutoBlockEnabled()
        binding.switchAutoBlock.setOnCheckedChangeListener { _, isChecked ->
            savedPreferencesLoader.setAutoBlockEnabled(isChecked)
        }
        
        binding.btnSelectCategories.setOnClickListener {
            showCategorySelectionDialog()
        }

        return binding.root
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    private fun showCategorySelectionDialog() {
        val categories = PackageWand.getAllCategories()
        val selectedCategories = savedPreferencesLoader.getAutoBlockCategories().toMutableSet()
        val categoryKeys = categories.map { it.first }.toTypedArray()
        val categoryNames = categories.map { it.second }.toTypedArray()
        val checkedItems = categoryKeys.map { selectedCategories.contains(it) }.toBooleanArray()
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Auto-Block Categories")
            .setMultiChoiceItems(categoryNames, checkedItems) { _, which, isChecked ->
                if (isChecked) {
                    selectedCategories.add(categoryKeys[which])
                } else {
                    selectedCategories.remove(categoryKeys[which])
                }
            }
            .setPositiveButton("Save") { _, _ ->
                savedPreferencesLoader.setAutoBlockCategories(selectedCategories)
                Toast.makeText(
                    requireContext(),
                    "Auto-block categories updated. New apps from these categories will be automatically blocked.",
                    Toast.LENGTH_LONG
                ).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openPremiumScreen() {
        val intent = Intent(requireContext(), com.alhaq.amnshield.ui.activity.FragmentActivity::class.java)
        intent.putExtra("feature_type", "premium_features")
        startActivity(intent)
    }

    private fun updateSelectedAppsCount(count: Int) {
        binding.txtSelectedAppsCount.text = getString(R.string.app_s_selected, count)
    }

    private fun setAppBlockerControlsEnabled(enabled: Boolean) {
        binding.btnSelectApps.isEnabled = enabled
        binding.btnCheatHours.isEnabled = enabled
        binding.btnBlockSchedules.isEnabled = enabled
        binding.btnLaunchLimits.isEnabled = enabled
        binding.btnWarningScreen.isEnabled = enabled
        binding.switchAutoBlock.isEnabled = enabled
        binding.btnSelectCategories.isEnabled = enabled
    }

    companion object {
        const val FRAGMENT_ID = "app_blocker_config"
    }
}

// NOTE: ViewBlockerConfigFragment was removed in v1.1.x when the View Blocker
// feature was consolidated into Reel Blocker (per-platform + browser toggles).
// Its layout (fragment_view_blocker_config.xml) was deleted alongside it. Any
// surviving "view_blocker" deep links are silently dropped by FragmentActivity.

/**
 * Dedicated Reel Blocker configuration screen with count-based limit mode.
 */
class ReelBlockerConfigFragment : BaseFeatureFragment() {

    private var _binding: com.alhaq.amnshield.databinding.FragmentReelBlockerConfigBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = com.alhaq.amnshield.databinding.FragmentReelBlockerConfigBinding.inflate(inflater, container, false)

        val premiumManager = PremiumManager.getInstance(requireContext().applicationContext)
        if (!premiumManager.isPremium()) {
            binding.configContainer.visibility = View.GONE
            binding.statusCard.visibility = View.VISIBLE
            binding.statusMessage.text = getString(R.string.premium_required_message)
            binding.btnStatusAction.text = getString(R.string.premium_view_plans)
            binding.btnStatusAction.setOnClickListener {
                val intent = Intent(requireContext(), com.alhaq.amnshield.ui.activity.FragmentActivity::class.java)
                intent.putExtra("feature_type", "premium_features")
                startActivity(intent)
            }
            return binding.root
        }

        if (!isAccessibilityServiceEnabled(AmnShieldAccessibilityService::class.java)) {
            binding.configContainer.visibility = View.GONE
            binding.statusCard.visibility = View.VISIBLE
            binding.statusMessage.text = "Please enable the main AmnShield Accessibility Service"
            binding.btnStatusAction.text = "Enable Service"
            binding.btnStatusAction.setOnClickListener {
                showAccessibilityInfoDialog("AmnShield Accessibility Service", AmnShieldAccessibilityService::class.java)
            }
            return binding.root
        }

        binding.statusCard.visibility = View.GONE
        binding.configContainer.visibility = View.VISIBLE

        val viewBlockerPrefs = requireContext().getSharedPreferences("view_blocker", Context.MODE_PRIVATE)
        val enabled = savedPreferencesLoader.isReelBlockerEnabled(
            viewBlockerPrefs.getBoolean("is_enabled", false)
        )
        val mode = savedPreferencesLoader.getReelBlockerMode(ReelBlocker.MODE_BLOCK_ALL)
        val limit = savedPreferencesLoader.getReelBlockerDailyLimit(200)

        binding.switchReelBlocker.isChecked = enabled
        binding.switchCountMode.isChecked = mode == ReelBlocker.MODE_BLOCK_AFTER_DAILY_COUNT
        binding.inputDailyLimit.setText(limit.toString())
        binding.inputLimitLayout.isEnabled = binding.switchCountMode.isChecked

        // Per-platform / browser toggles. ReelBlocker honors these at detection
        // time; flipping a switch broadcasts a refresh so the running service
        // picks up the change without an app restart.
        binding.switchYoutube.isChecked = savedPreferencesLoader.isReelBlockerYoutubeEnabled()
        binding.switchInstagram.isChecked = savedPreferencesLoader.isReelBlockerInstagramEnabled()
        binding.switchTiktok.isChecked = savedPreferencesLoader.isReelBlockerTiktokEnabled()
        binding.switchBrowser.isChecked = savedPreferencesLoader.isReelBlockerBrowserEnabled()

        binding.switchYoutube.setOnCheckedChangeListener { _, isChecked ->
            savedPreferencesLoader.setReelBlockerYoutubeEnabled(isChecked)
            sendRefreshRequest(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_REEL_BLOCKER)
        }
        binding.switchInstagram.setOnCheckedChangeListener { _, isChecked ->
            savedPreferencesLoader.setReelBlockerInstagramEnabled(isChecked)
            sendRefreshRequest(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_REEL_BLOCKER)
        }
        binding.switchTiktok.setOnCheckedChangeListener { _, isChecked ->
            savedPreferencesLoader.setReelBlockerTiktokEnabled(isChecked)
            sendRefreshRequest(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_REEL_BLOCKER)
        }
        binding.switchBrowser.setOnCheckedChangeListener { _, isChecked ->
            savedPreferencesLoader.setReelBlockerBrowserEnabled(isChecked)
            sendRefreshRequest(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_REEL_BLOCKER)
        }

        binding.switchReelBlocker.setOnCheckedChangeListener { _, isChecked ->
            savedPreferencesLoader.setReelBlockerEnabled(isChecked)
            sendRefreshRequest(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_REEL_BLOCKER)
        }

        binding.switchCountMode.setOnCheckedChangeListener { _, isChecked ->
            val selectedMode = if (isChecked) {
                ReelBlocker.MODE_BLOCK_AFTER_DAILY_COUNT
            } else {
                ReelBlocker.MODE_BLOCK_ALL
            }
            binding.inputLimitLayout.isEnabled = isChecked
            savedPreferencesLoader.setReelBlockerMode(selectedMode)
            sendRefreshRequest(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_REEL_BLOCKER)
        }

        binding.btnSaveMode.setOnClickListener {
            val parsed = binding.inputDailyLimit.text?.toString()?.trim()?.toIntOrNull()
            if (parsed == null || parsed < 1) {
                Toast.makeText(requireContext(), getString(R.string.reel_daily_limit_validation), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            savedPreferencesLoader.setReelBlockerDailyLimit(parsed)
            sendRefreshRequest(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_REEL_BLOCKER)
            Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show()
        }

        binding.btnWarningScreen.setOnClickListener {
            TweakViewBlockerWarning(savedPreferencesLoader).show(
                childFragmentManager,
                "tweak_reel_blocker_warning"
            )
        }

        binding.btnCheatHours.setOnClickListener {
            TweakViewBlockerCheatHours(savedPreferencesLoader).show(
                childFragmentManager,
                "tweak_reel_blocker_cheat_hours"
            )
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val FRAGMENT_ID = "reel_blocker_config"
    }
}

/**
 * Comprehensive Usage Tracker configuration screen with all options
 */
class UsageTrackerConfigFragment : BaseFeatureFragment() {
    
    private var _binding: com.alhaq.amnshield.databinding.FragmentUsageTrackerConfigBinding? = null
    private val binding get() = _binding!!
    
    private val selectOverlayAppsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val selectedApps = result.data?.getStringArrayListExtra("SELECTED_APPS")
            selectedApps?.let {
                val prefs = requireContext().getSharedPreferences("config_tracker", android.content.Context.MODE_PRIVATE)
                prefs.edit().putStringSet("overlay_apps", it.toSet()).apply()
                Toast.makeText(requireContext(), "Overlay apps updated", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = com.alhaq.amnshield.databinding.FragmentUsageTrackerConfigBinding.inflate(inflater, container, false)

        // Check service status
        if (!isAccessibilityServiceEnabled(AmnShieldAccessibilityService::class.java)) {
            binding.configContainer.visibility = View.GONE
            binding.statusCard.visibility = View.VISIBLE
            binding.statusMessage.text = "Please enable the main AmnShield Accessibility Service"
            binding.btnStatusAction.text = "Enable Service"
            binding.btnStatusAction.setOnClickListener {
                showAccessibilityInfoDialog("AmnShield Accessibility Service", AmnShieldAccessibilityService::class.java)
            }
            return binding.root
        }

        binding.statusCard.visibility = View.GONE
        binding.configContainer.visibility = View.VISIBLE

        val isFeatureEnabled = savedPreferencesLoader.isUsageTrackerFeatureEnabled()
        binding.switchUsageTrackerEnabled.isChecked = isFeatureEnabled
        setUsageTrackerControlsEnabled(isFeatureEnabled)

        binding.switchUsageTrackerEnabled.setOnCheckedChangeListener { _, isChecked ->
            savedPreferencesLoader.setUsageTrackerFeatureEnabled(isChecked)
            setUsageTrackerControlsEnabled(isChecked)
        }

        binding.btnTrackerToggles.setOnClickListener {
            TweakUsageTracker(savedPreferencesLoader).show(
                childFragmentManager,
                "tweak_usage_tracker"
            )
        }

        binding.btnSelectOverlayApps.setOnClickListener {
            val prefs = requireContext().getSharedPreferences("config_tracker", android.content.Context.MODE_PRIVATE)
            val overlayApps = prefs.getStringSet("overlay_apps", emptySet()) ?: emptySet()
            val intent = Intent(requireContext(), SelectAppsActivity::class.java)
            intent.putStringArrayListExtra(
                "PRE_SELECTED_APPS",
                ArrayList(overlayApps)
            )
            selectOverlayAppsLauncher.launch(intent, activityOptions)
        }

        binding.btnViewMetrics.setOnClickListener {
            val intent = Intent(requireContext(), com.alhaq.amnshield.ui.activity.UsageMetricsActivity::class.java)
            startActivity(intent, activityOptions.toBundle())
        }

        return binding.root
    }

    private fun setUsageTrackerControlsEnabled(enabled: Boolean) {
        binding.btnTrackerToggles.isEnabled = enabled
        binding.btnSelectOverlayApps.isEnabled = enabled
        binding.btnViewMetrics.isEnabled = enabled
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val FRAGMENT_ID = "usage_tracker_config"
    }
}

/**
 * Comprehensive Keyword Blocker configuration screen with all options
 */
class KeywordBlockerConfigFragment : BaseFeatureFragment() {

    private var _binding: com.alhaq.amnshield.databinding.FragmentKeywordBlockerConfigBinding? = null
    private val binding get() = _binding!!
    
    private val selectKeywordsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val selectedKeywords = result.data?.getStringArrayListExtra("SELECTED_KEYWORDS")
            selectedKeywords?.let {
                savedPreferencesLoader.saveBlockedKeywords(it.toSet())
                sendRefreshRequest(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_BLOCKED_KEYWORD_LIST)
                updateKeywordCount(it.size)
                Toast.makeText(requireContext(), "Keywords updated", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = com.alhaq.amnshield.databinding.FragmentKeywordBlockerConfigBinding.inflate(inflater, container, false)

        // Check service status
        if (!isAccessibilityServiceEnabled(AmnShieldAccessibilityService::class.java)) {
            binding.configContainer.visibility = View.GONE
            binding.statusCard.visibility = View.VISIBLE
            binding.statusMessage.text = "Please enable the main AmnShield Accessibility Service"
            binding.btnStatusAction.text = "Enable Service"
            binding.btnStatusAction.setOnClickListener {
                showAccessibilityInfoDialog("AmnShield Accessibility Service", AmnShieldAccessibilityService::class.java)
            }
            return binding.root
        }

        binding.statusCard.visibility = View.GONE
        binding.configContainer.visibility = View.VISIBLE

        val isFeatureEnabled = savedPreferencesLoader.isKeywordBlockerFeatureEnabled()
        binding.switchKeywordBlockerEnabled.isChecked = isFeatureEnabled
        setKeywordBlockerControlsEnabled(isFeatureEnabled)

        binding.switchKeywordBlockerEnabled.setOnCheckedChangeListener { _, isChecked ->
            savedPreferencesLoader.setKeywordBlockerFeatureEnabled(isChecked)
            setKeywordBlockerControlsEnabled(isChecked)
            sendRefreshRequest(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_BLOCKED_KEYWORD_LIST)
        }

        updateKeywordCount(savedPreferencesLoader.loadBlockedKeywords().size)

        binding.btnManageKeywords.setOnClickListener {
            val intent = Intent(requireContext(), ManageKeywordsActivity::class.java)
            intent.putStringArrayListExtra(
                "PRE_SAVED_KEYWORDS",
                ArrayList(savedPreferencesLoader.loadBlockedKeywords())
            )
            selectKeywordsLauncher.launch(intent, activityOptions)
        }

        binding.btnKeywordPacks.setOnClickListener {
            TweakKeywordPack().show(
                childFragmentManager,
                "tweak_keyword_pack"
            )
        }

        binding.btnKeywordConfig.setOnClickListener {
            TweakKeywordBlocker(savedPreferencesLoader).show(
                childFragmentManager,
                "tweak_keyword_blocker"
            )
        }

        return binding.root
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateKeywordCount(count: Int) {
        binding.txtKeywordCount.text = "$count keywords blocked"
    }

    private fun setKeywordBlockerControlsEnabled(enabled: Boolean) {
        binding.btnManageKeywords.isEnabled = enabled
        binding.btnKeywordPacks.isEnabled = enabled
        binding.btnKeywordConfig.isEnabled = enabled
    }

    companion object {
        const val FRAGMENT_ID = "keyword_blocker_config"
    }
}

