package com.alhaq.deenshield.ui.fragments

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.alhaq.deenshield.R
import com.alhaq.deenshield.databinding.FragmentHomeBinding
import com.alhaq.deenshield.services.DeenShieldAccessibilityService
import com.alhaq.deenshield.utils.SavedPreferencesLoader
import com.alhaq.deenshield.premium.PremiumManager

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val premiumManager by lazy { PremiumManager.getInstance(requireContext().applicationContext) }
    private val savedPreferencesLoader by lazy { SavedPreferencesLoader(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        updateServiceStatus()
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
    }

    private fun setupClickListeners() {
        // Card clicks now open dedicated feature configuration activities
        // These activities contain ALL configuration options for each feature
        
        binding.cardAdditionalFeatures.setOnClickListener {
            launchFeatureActivity("premium_features")
        }

        binding.cardFocusMode.setOnClickListener {
            launchFeatureActivity("focus_mode")
        }

        binding.cardAppBlocker.setOnClickListener {
            launchFeatureActivity("app_blocker")
        }

        binding.cardKeywordBlocker.setOnClickListener {
            launchFeatureActivity("keyword_blocker")
        }

        binding.cardUsageTracker.setOnClickListener {
            launchFeatureActivity("usage_tracker")
        }

        binding.cardViewBlocker.setOnClickListener {
            launchFeatureActivity("view_blocker")
        }
    }
    
    private fun launchFeatureActivity(featureType: String) {
        if (!premiumManager.isPremium() && (featureType == "app_blocker" || featureType == "focus_mode" || featureType == "view_blocker")) {
            showPremiumUpsell()
            return
        }
        val intent = Intent(requireContext(), com.alhaq.deenshield.ui.activity.FragmentActivity::class.java)
        intent.putExtra("feature_type", featureType)
        startActivity(intent)
    }

    private fun updateServiceStatus() {
        context?.let { ctx ->
            // Check if the main accessibility service is enabled
            val isMainServiceEnabled = isAccessibilityServiceEnabled(DeenShieldAccessibilityService::class.java)
            val isPremiumUser = premiumManager.isPremium()
            
            // Update Focus Mode status - check if a focus session is actually running
            val focusModeData = com.alhaq.deenshield.utils.SavedPreferencesLoader(ctx).getFocusModeData()
            val isFocusModeActive = isPremiumUser && focusModeData.isTurnedOn && isMainServiceEnabled
            updateChipStatus(
                binding.chipFocusStatus,
                isFocusModeActive
            )

            // Update App Blocker status - check service
            val hasBlockedApps = savedPreferencesLoader.loadBlockedApps().isNotEmpty()
            updateChipStatus(
                binding.chipAppBlockerStatus,
                isPremiumUser && hasBlockedApps && isMainServiceEnabled
            )

            // Update Keyword Blocker status
            // Check if any keywords are configured (custom keywords OR keyword packs)
            val hasCustomKeywords = savedPreferencesLoader.loadBlockedKeywords().isNotEmpty()
            val keywordPacksPrefs = ctx.getSharedPreferences("keyword_blocker_packs", android.content.Context.MODE_PRIVATE)
            val hasAdultPack = keywordPacksPrefs.getBoolean("adult_blocker", false)
            val isKeywordBlockerConfigured = hasCustomKeywords || hasAdultPack
            updateChipStatus(
                binding.chipKeywordBlockerStatus,
                isKeywordBlockerConfigured && isMainServiceEnabled
            )

            // Update Usage Tracker status - not yet integrated into main service
            updateChipStatus(
                binding.chipUsageTrackerStatus,
                isMainServiceEnabled
            )

            // Additional features card now points to premium plans/state
            updateChipStatus(binding.chipAdditionalFeaturesStatus, isPremiumUser)

            // Update View Blocker status
            val viewBlockerPrefs = ctx.getSharedPreferences("view_blocker", android.content.Context.MODE_PRIVATE)
            val isViewBlockerEnabled = viewBlockerPrefs.getBoolean("is_enabled", false)
            updateChipStatus(binding.chipViewBlockerStatus, isViewBlockerEnabled && isMainServiceEnabled)
        }
    }

    private fun updateChipStatus(chip: com.google.android.material.chip.Chip, isEnabled: Boolean) {
        if (isEnabled) {
            chip.text = getString(R.string.on)
            chip.chipBackgroundColor = context?.let { 
                ContextCompat.getColorStateList(it, R.color.md_theme_primaryContainer) 
            }
            chip.setChipIconResource(R.drawable.baseline_done_24)
            chip.chipIconTint = context?.let { 
                ContextCompat.getColorStateList(it, R.color.md_theme_primary) 
            }
        } else {
            chip.text = getString(R.string.off)
            // Resolve the error color from theme attribute
            val typedValue = TypedValue()
            context?.theme?.resolveAttribute(android.R.attr.colorError, typedValue, true)
            val errorColor = typedValue.data
            chip.chipBackgroundColor = ColorStateList.valueOf(errorColor)
            chip.setChipIconResource(R.drawable.baseline_stop_24)
            chip.chipIconTint = ColorStateList.valueOf(errorColor)
        }
    }

    private fun isAccessibilityServiceEnabled(serviceClass: Class<out AccessibilityService>): Boolean {
        val serviceName = context?.let { ComponentName(it, serviceClass).flattenToString() }
        val enabledServices = Settings.Secure.getString(
            context?.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(serviceName ?: "") == true
    }

    private fun showServiceEnableDialog(serviceName: String, serviceClass: Class<out AccessibilityService>) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Enable $serviceName")
            .setMessage("Please enable the $serviceName accessibility service to use this feature.")
            .setPositiveButton("Open Settings") { _, _ ->
                openAccessibilitySettings()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showPremiumUpsell() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.premium_required_title)
            .setMessage(getString(R.string.premium_required_message))
            .setPositiveButton(R.string.premium_view_plans) { _, _ ->
                launchFeatureActivity("premium_features")
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    fun refreshStatus() {
        if (isAdded) {
            updateServiceStatus()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
