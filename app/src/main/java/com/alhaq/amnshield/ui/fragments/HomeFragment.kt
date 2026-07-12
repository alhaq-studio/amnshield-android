package com.alhaq.amnshield.ui.fragments

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.app.admin.DevicePolicyManager
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.alhaq.amnshield.R
import com.alhaq.amnshield.databinding.FragmentHomeBinding
import com.alhaq.amnshield.receivers.AdminReceiver
import com.alhaq.amnshield.services.DeenShieldAccessibilityService
import com.alhaq.amnshield.utils.SavedPreferencesLoader
import com.alhaq.amnshield.premium.PremiumManager

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val premiumManager by lazy { PremiumManager.getInstance(requireContext().applicationContext) }
    private val savedPreferencesLoader by lazy { SavedPreferencesLoader(requireContext()) }
    private var serviceSnackbar: Snackbar? = null

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
        updateWelcomeSectionVisibility()
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

        binding.cardReelBlocker.setOnClickListener {
            launchFeatureActivity("reel_blocker")
        }

        binding.cardAntiUninstall.setOnClickListener {
            launchFeatureActivity("anti_uninstall")
        }

        binding.btnDismissWelcome.setOnClickListener {
            savedPreferencesLoader.setHomeWelcomeCardVisible(false)
            updateWelcomeSectionVisibility()
            Snackbar.make(binding.root, R.string.welcome_card_hidden, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo) {
                    savedPreferencesLoader.setHomeWelcomeCardVisible(true)
                    updateWelcomeSectionVisibility()
                }
                .show()
        }
    }

    private fun updateWelcomeSectionVisibility() {
        val showWelcome = savedPreferencesLoader.isHomeWelcomeCardVisible()
        binding.cardWelcome.visibility = if (showWelcome) View.VISIBLE else View.GONE
        binding.txtQuickActions.visibility = if (showWelcome) View.VISIBLE else View.GONE
    }
    
    private fun launchFeatureActivity(featureType: String) {
        if (!premiumManager.isPremium() && (
                featureType == "app_blocker" ||
                featureType == "focus_mode" ||
                featureType == "reel_blocker" ||
                featureType == "anti_uninstall"
            )
        ) {
            showPremiumUpsell()
            return
        }
        val intent = Intent(requireContext(), com.alhaq.amnshield.ui.activity.FragmentActivity::class.java)
        intent.putExtra("feature_type", featureType)
        startActivity(intent)
    }

    private fun updateServiceStatus() {
        context?.let { ctx ->
            // Check if the main accessibility service is enabled
            val isMainServiceEnabled = isAccessibilityServiceEnabled(DeenShieldAccessibilityService::class.java)
            val isPremiumUser = premiumManager.isPremium()
            
            // Update Focus Mode status - check if a focus session is actually running
            val focusModeData = savedPreferencesLoader.getFocusModeData()
            val isFocusModeActive = isPremiumUser && focusModeData.isTurnedOn && isMainServiceEnabled
            updateChipStatus(
                binding.chipFocusStatus,
                isFocusModeActive
            )

            // Update App Blocker status - check service
            val hasBlockedApps = savedPreferencesLoader.loadBlockedApps().isNotEmpty()
            updateChipStatus(
                binding.chipAppBlockerStatus,
                isPremiumUser && savedPreferencesLoader.isAppBlockerFeatureEnabled() && hasBlockedApps && isMainServiceEnabled
            )

            // Update Keyword Blocker status
            // Check if any keywords are configured (custom keywords OR keyword packs)
            val hasCustomKeywords = savedPreferencesLoader.loadBlockedKeywords().isNotEmpty()
            val hasAdultPack = savedPreferencesLoader.isKeywordBlockerAdultPackEnabled()
            val isKeywordBlockerConfigured = hasCustomKeywords || hasAdultPack
            updateChipStatus(
                binding.chipKeywordBlockerStatus,
                savedPreferencesLoader.isKeywordBlockerFeatureEnabled() && isKeywordBlockerConfigured && isMainServiceEnabled
            )

            // Update Usage Tracker status - not yet integrated into main service
            updateChipStatus(
                binding.chipUsageTrackerStatus,
                savedPreferencesLoader.isUsageTrackerFeatureEnabled() && isMainServiceEnabled
            )

            // Premium card status reflects access type (free, premium, compassionate, special)
            updatePremiumCardStatus()

            // Update Reel Blocker status (formerly View Blocker — now consolidated).
            // Falls back to the legacy view_blocker enable flag for users upgrading
            // from versions where reel blocking lived under that key.
            val viewBlockerPrefs = ctx.getSharedPreferences("view_blocker", android.content.Context.MODE_PRIVATE)
            val legacyEnabled = viewBlockerPrefs.getBoolean("is_enabled", false)
            val reelBlockerPrefs = ctx.getSharedPreferences("reel_blocker", android.content.Context.MODE_PRIVATE)
            val isReelBlockerEnabled = reelBlockerPrefs.getBoolean("is_enabled", legacyEnabled)
            updateChipStatus(binding.chipReelBlockerStatus, isReelBlockerEnabled && isMainServiceEnabled)

            val antiUninstallPrefs = ctx.getSharedPreferences("anti_uninstall", Context.MODE_PRIVATE)
            val isAntiUninstallEnabled = antiUninstallPrefs.getBoolean("is_anti_uninstall_on", false)
            val hasDeviceAdmin = isDeviceAdminEnabled(ctx)
            updateChipStatus(binding.chipAntiUninstallStatus, isAntiUninstallEnabled && hasDeviceAdmin)

            if (!isMainServiceEnabled) {
                if (serviceSnackbar == null) {
                    serviceSnackbar = Snackbar.make(
                        binding.root,
                        "DeenShield service is disabled. Please enable it for full protection.",
                        Snackbar.LENGTH_INDEFINITE
                    ).setAction("Enable") {
                        openAccessibilitySettings()
                    }
                    serviceSnackbar?.show()
                }
            } else {
                serviceSnackbar?.dismiss()
                serviceSnackbar = null
            }
        }
    }

    private fun isDeviceAdminEnabled(ctx: Context): Boolean {
        val devicePolicyManager = ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val adminComponent = ComponentName(ctx, AdminReceiver::class.java)
        return devicePolicyManager?.isAdminActive(adminComponent) == true
    }

    private fun updatePremiumCardStatus() {
        val chip = binding.chipAdditionalFeaturesStatus
        when (premiumManager.getUserType()) {
            PremiumManager.UserType.FREE -> {
                chip.text = getString(R.string.premium_unlock_chip)
                chip.chipBackgroundColor = context?.let {
                    ContextCompat.getColorStateList(it, R.color.md_theme_errorContainer)
                }
                chip.setChipIconResource(R.drawable.baseline_stop_24)
                chip.chipIconTint = context?.let {
                    ContextCompat.getColorStateList(it, R.color.md_theme_onErrorContainer)
                }
            }
            PremiumManager.UserType.PREMIUM -> {
                chip.text = getString(R.string.premium_card_status_premium)
                chip.chipBackgroundColor = context?.let {
                    ContextCompat.getColorStateList(it, R.color.md_theme_primaryContainer)
                }
                chip.setChipIconResource(R.drawable.baseline_done_24)
                chip.chipIconTint = context?.let {
                    ContextCompat.getColorStateList(it, R.color.md_theme_primary)
                }
            }
            PremiumManager.UserType.COMPASSIONATE -> {
                chip.text = getString(R.string.premium_card_status_compassionate)
                chip.chipBackgroundColor = context?.let {
                    ContextCompat.getColorStateList(it, R.color.md_theme_tertiaryContainer)
                }
                chip.setChipIconResource(R.drawable.baseline_done_24)
                chip.chipIconTint = context?.let {
                    ContextCompat.getColorStateList(it, R.color.md_theme_onTertiaryContainer)
                }
            }
            PremiumManager.UserType.SPECIAL -> {
                chip.text = getString(R.string.premium_card_status_special)
                chip.chipBackgroundColor = context?.let {
                    ContextCompat.getColorStateList(it, R.color.md_theme_secondaryContainer)
                }
                chip.setChipIconResource(R.drawable.baseline_done_24)
                chip.chipIconTint = context?.let {
                    ContextCompat.getColorStateList(it, R.color.md_theme_onSecondaryContainer)
                }
            }
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
        serviceSnackbar?.dismiss()
        serviceSnackbar = null
        _binding = null
    }
}
