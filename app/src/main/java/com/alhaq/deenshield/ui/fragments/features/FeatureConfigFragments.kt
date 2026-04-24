package com.alhaq.deenshield.ui.fragments.features

import android.content.Intent
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
import com.alhaq.deenshield.R
import com.alhaq.deenshield.services.DeenShieldAccessibilityService
import com.alhaq.deenshield.ui.activity.SelectAppsActivity
import com.alhaq.deenshield.ui.activity.TimedActionActivity
import com.alhaq.deenshield.ui.activity.ManageKeywordsActivity
import com.alhaq.deenshield.ui.dialogs.TweakAppBlockerWarning
import com.alhaq.deenshield.ui.dialogs.TweakViewBlockerCheatHours
import com.alhaq.deenshield.ui.dialogs.TweakViewBlockerWarning
import com.alhaq.deenshield.ui.dialogs.TweakUsageTracker
import com.alhaq.deenshield.ui.dialogs.TweakKeywordBlocker
import com.alhaq.deenshield.ui.dialogs.TweakKeywordPack
import com.alhaq.deenshield.utils.SavedPreferencesLoader
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.alhaq.deenshield.premium.PremiumManager
import com.alhaq.deenshield.data.blockers.PackageWand
import com.google.android.material.materialswitch.MaterialSwitch

/**
 * Comprehensive App Blocker configuration screen with all options
 */
class AppBlockerConfigFragment : BaseFeatureFragment() {
    
    private var _binding: com.alhaq.deenshield.databinding.FragmentAppBlockerConfigBinding? = null
    private val binding get() = _binding!!
    
    private val selectAppsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val selectedApps = result.data?.getStringArrayListExtra("SELECTED_APPS")
            selectedApps?.let {
                savedPreferencesLoader.saveBlockedApps(it.toSet())
                sendRefreshRequest(DeenShieldAccessibilityService.INTENT_ACTION_REFRESH_APP_BLOCKER)
                updateSelectedAppsCount(it.size)
            }
        }
    }

    private val cheatHoursLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            sendRefreshRequest(DeenShieldAccessibilityService.INTENT_ACTION_REFRESH_APP_BLOCKER)
            Toast.makeText(requireContext(), "Cheat hours updated", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = com.alhaq.deenshield.databinding.FragmentAppBlockerConfigBinding.inflate(inflater, container, false)
        
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
        if (!isAccessibilityServiceEnabled(DeenShieldAccessibilityService::class.java)) {
            binding.configContainer.visibility = View.GONE
            binding.statusCard.visibility = View.VISIBLE
            binding.statusMessage.text = "Please enable the main DeenShield Accessibility Service"
            binding.btnStatusAction.text = "Enable Service"
            binding.btnStatusAction.setOnClickListener {
                showAccessibilityInfoDialog("DeenShield Accessibility Service", DeenShieldAccessibilityService::class.java)
            }
            return binding.root
        }

        binding.statusCard.visibility = View.GONE
        binding.configContainer.visibility = View.VISIBLE
        
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
        val intent = Intent(requireContext(), com.alhaq.deenshield.ui.activity.FragmentActivity::class.java)
        intent.putExtra("feature_type", "premium_features")
        startActivity(intent)
    }

    private fun updateSelectedAppsCount(count: Int) {
        binding.txtSelectedAppsCount.text = getString(R.string.app_s_selected, count)
    }

    companion object {
        const val FRAGMENT_ID = "app_blocker_config"
    }
}

/**
 * Comprehensive View Blocker configuration screen
 */
class ViewBlockerConfigFragment : BaseFeatureFragment() {
    
    private var _binding: com.alhaq.deenshield.databinding.FragmentViewBlockerConfigBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = com.alhaq.deenshield.databinding.FragmentViewBlockerConfigBinding.inflate(inflater, container, false)

        // Check Premium status first
        val premiumManager = com.alhaq.deenshield.premium.PremiumManager.getInstance(requireContext().applicationContext)
        if (!premiumManager.isPremium()) {
            binding.configContainer.visibility = View.GONE
            binding.statusCard.visibility = View.VISIBLE
            binding.statusMessage.text = getString(R.string.premium_required_message)
            binding.btnStatusAction.text = getString(R.string.premium_view_plans)
            binding.btnStatusAction.setOnClickListener {
                val intent = Intent(requireContext(), com.alhaq.deenshield.ui.activity.FragmentActivity::class.java)
                intent.putExtra("feature_type", "premium_features")
                startActivity(intent)
            }
            return binding.root
        }

        // Check service status
        if (!isAccessibilityServiceEnabled(DeenShieldAccessibilityService::class.java)) {
            binding.configContainer.visibility = View.GONE
            binding.statusCard.visibility = View.VISIBLE
            binding.statusMessage.text = "Please enable the main DeenShield Accessibility Service"
            binding.btnStatusAction.text = "Enable Service"
            binding.btnStatusAction.setOnClickListener {
                showAccessibilityInfoDialog("DeenShield Accessibility Service", DeenShieldAccessibilityService::class.java)
            }
            return binding.root
        }

        binding.statusCard.visibility = View.GONE
        binding.configContainer.visibility = View.VISIBLE

        binding.btnWarningScreen.setOnClickListener {
            TweakViewBlockerWarning(savedPreferencesLoader).show(
                childFragmentManager,
                "tweak_view_blocker_warning"
            )
        }

        binding.btnCheatHours.setOnClickListener {
            TweakViewBlockerCheatHours(savedPreferencesLoader).show(
                childFragmentManager,
                "tweak_view_blocker_cheat_hours"
            )
        }

        binding.btnHelp.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.about_view_blocker))
                .setMessage(getString(R.string.this_option_has_the_ability_to_block_youtube_shorts_and_instagram_reels_while_allowing_access_to_other_app_features))
                .setPositiveButton(getString(R.string.ok), null)
                .show()
        }

        return binding.root
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val FRAGMENT_ID = "view_blocker_config"
    }
}

/**
 * Comprehensive Usage Tracker configuration screen with all options
 */
class UsageTrackerConfigFragment : BaseFeatureFragment() {
    
    private var _binding: com.alhaq.deenshield.databinding.FragmentUsageTrackerConfigBinding? = null
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
        _binding = com.alhaq.deenshield.databinding.FragmentUsageTrackerConfigBinding.inflate(inflater, container, false)

        // Check service status
        if (!isAccessibilityServiceEnabled(DeenShieldAccessibilityService::class.java)) {
            binding.configContainer.visibility = View.GONE
            binding.statusCard.visibility = View.VISIBLE
            binding.statusMessage.text = "Please enable the main DeenShield Accessibility Service"
            binding.btnStatusAction.text = "Enable Service"
            binding.btnStatusAction.setOnClickListener {
                showAccessibilityInfoDialog("DeenShield Accessibility Service", DeenShieldAccessibilityService::class.java)
            }
            return binding.root
        }

        binding.statusCard.visibility = View.GONE
        binding.configContainer.visibility = View.VISIBLE

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
            val intent = Intent(requireContext(), com.alhaq.deenshield.ui.activity.UsageMetricsActivity::class.java)
            startActivity(intent, activityOptions.toBundle())
        }

        return binding.root
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

    private var _binding: com.alhaq.deenshield.databinding.FragmentKeywordBlockerConfigBinding? = null
    private val binding get() = _binding!!
    
    private val selectKeywordsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val selectedKeywords = result.data?.getStringArrayListExtra("SELECTED_KEYWORDS")
            selectedKeywords?.let {
                savedPreferencesLoader.saveBlockedKeywords(it.toSet())
                sendRefreshRequest(DeenShieldAccessibilityService.INTENT_ACTION_REFRESH_BLOCKED_KEYWORD_LIST)
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
        _binding = com.alhaq.deenshield.databinding.FragmentKeywordBlockerConfigBinding.inflate(inflater, container, false)

        // Check service status
        if (!isAccessibilityServiceEnabled(DeenShieldAccessibilityService::class.java)) {
            binding.configContainer.visibility = View.GONE
            binding.statusCard.visibility = View.VISIBLE
            binding.statusMessage.text = "Please enable the main DeenShield Accessibility Service"
            binding.btnStatusAction.text = "Enable Service"
            binding.btnStatusAction.setOnClickListener {
                showAccessibilityInfoDialog("DeenShield Accessibility Service", DeenShieldAccessibilityService::class.java)
            }
            return binding.root
        }

        binding.statusCard.visibility = View.GONE
        binding.configContainer.visibility = View.VISIBLE

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

    companion object {
        const val FRAGMENT_ID = "keyword_blocker_config"
    }
}

