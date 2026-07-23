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
import com.alhaq.amnshield.ui.activity.ManageKeywordsActivity
import com.alhaq.amnshield.ui.activity.FragmentActivity
import com.alhaq.amnshield.ui.fragments.BlocksManagerFragment
import com.alhaq.amnshield.ui.dialogs.TweakAppBlockerWarning
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

        setupAppBlockerSwitch()

        val appRulesCount = savedPreferencesLoader.loadAppBlockerScheduleRules()
            .filter { it.packageName != "keyword_blocker" && it.packageName != "website_blocker" && it.packageName != "reel_blocker" && it.packageName != "FOCUS_MODE" }
            .map { it.groupId ?: it.id }
            .distinct()
            .size
        binding.txtSelectedAppsCount.text = "$appRulesCount Active App Rules"
        binding.btnSelectApps.text = "Manage App Rules"

        binding.btnSelectApps.setOnClickListener {
            val intent = Intent(requireContext(), FragmentActivity::class.java).apply {
                putExtra("fragment", BlocksManagerFragment.FRAGMENT_ID)
                putExtra("filter_type", "App Blocker")
            }
            startActivity(intent, activityOptions.toBundle())
        }

        binding.btnBlockSchedules.setOnClickListener {
            val intent = Intent(requireContext(), FragmentActivity::class.java).apply {
                putExtra("fragment", BlocksManagerFragment.FRAGMENT_ID)
                putExtra("filter_type", "App Blocker")
            }
            startActivity(intent, activityOptions.toBundle())
        }

        binding.btnLaunchLimits.setOnClickListener {
            val intent = Intent(requireContext(), FragmentActivity::class.java).apply {
                putExtra("fragment", com.alhaq.amnshield.ui.fragments.ManageLaunchLimitsFragment.FRAGMENT_ID)
            }
            startActivity(intent, activityOptions.toBundle())
        }

        binding.btnUsageLimits.setOnClickListener {
            val intent = Intent(requireContext(), FragmentActivity::class.java).apply {
                putExtra("feature_type", "usage_tracker")
            }
            startActivity(intent, activityOptions.toBundle())
        }

        binding.btnWarningScreen.setOnClickListener {
            TweakAppBlockerWarning(savedPreferencesLoader).show(
                childFragmentManager,
                "tweak_app_blocker_warning"
            )
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
    
    private fun setupAppBlockerSwitch() {
        val isFeatureEnabled = savedPreferencesLoader.isAppBlockerFeatureEnabled()
        
        binding.switchAppBlockerEnabled.setOnCheckedChangeListener(null)
        binding.switchAppBlockerEnabled.isChecked = isFeatureEnabled
        setAppBlockerControlsEnabled(isFeatureEnabled)

        binding.switchAppBlockerEnabled.setOnCheckedChangeListener { buttonView, isChecked ->
            savedPreferencesLoader.setAppBlockerFeatureEnabled(isChecked, updateManual = true)
            setAppBlockerControlsEnabled(isChecked)
            sendRefreshRequest(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_APP_BLOCKER)
            sendRefreshRequest(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_UNIFIED_FEATURE_SCHEDULES)
        }
    }

    private fun updateSelectedAppsCount(count: Int) {
        binding.txtSelectedAppsCount.text = getString(R.string.app_s_selected, count)
    }

    private fun setAppBlockerControlsEnabled(enabled: Boolean) {
        binding.btnSelectApps.isEnabled = true
        binding.btnWarningScreen.isEnabled = true
        binding.switchAutoBlock.isEnabled = true
        binding.btnSelectCategories.isEnabled = true
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
        setupReelBlockerSwitch()

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

        // Reel Blocker switch listener is managed inside setupReelBlockerSwitch()

        binding.btnWarningScreen.setOnClickListener {
            TweakViewBlockerWarning(savedPreferencesLoader).show(
                childFragmentManager,
                "tweak_reel_blocker_warning"
            )
        }

        ((binding.btnCheatHours.parent as? View)?.parent as? View)?.visibility = View.GONE

        return binding.root
    }
    
    private fun setupReelBlockerSwitch() {
        val isFeatureEnabled = savedPreferencesLoader.isReelBlockerEnabled()
        
        binding.switchReelBlocker.setOnCheckedChangeListener(null)
        binding.switchReelBlocker.isChecked = isFeatureEnabled

        binding.switchReelBlocker.setOnCheckedChangeListener { buttonView, isChecked ->
            savedPreferencesLoader.setReelBlockerEnabled(isChecked, updateManual = true)
            sendRefreshRequest(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_REEL_BLOCKER)
            sendRefreshRequest(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_UNIFIED_FEATURE_SCHEDULES)
        }
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

        return binding.root
    }

    private fun setUsageTrackerControlsEnabled(enabled: Boolean) {
        binding.btnTrackerToggles.isEnabled = enabled
        binding.btnSelectOverlayApps.isEnabled = enabled
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

        setupKeywordBlockerSwitch()

        binding.btnKeywordConfig.setOnClickListener {
            TweakKeywordBlocker(savedPreferencesLoader).show(
                childFragmentManager,
                "tweak_keyword_blocker"
            )
        }

        binding.cardScheduleKeywordBlocker.visibility = View.GONE

        return binding.root
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupKeywordBlockerSwitch() {
        val isFeatureEnabled = savedPreferencesLoader.isKeywordBlockerFeatureEnabled()
        
        binding.switchKeywordBlockerEnabled.setOnCheckedChangeListener(null)
        binding.switchKeywordBlockerEnabled.isChecked = isFeatureEnabled
        setKeywordBlockerControlsEnabled(isFeatureEnabled)

        binding.switchKeywordBlockerEnabled.setOnCheckedChangeListener { buttonView, isChecked ->
            savedPreferencesLoader.setKeywordBlockerFeatureEnabled(isChecked, updateManual = true)
            setKeywordBlockerControlsEnabled(isChecked)
            sendRefreshRequest(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_BLOCKED_KEYWORD_LIST)
            sendRefreshRequest(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_UNIFIED_FEATURE_SCHEDULES)
        }
    }

    private fun setKeywordBlockerControlsEnabled(enabled: Boolean) {
        binding.btnKeywordConfig.isEnabled = enabled
    }

    companion object {
        const val FRAGMENT_ID = "keyword_blocker_config"
    }
}

