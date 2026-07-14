package com.alhaq.amnshield.ui.fragments

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.app.admin.DevicePolicyManager
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.alhaq.amnshield.ui.screens.HomeScreen
import com.alhaq.amnshield.ui.theme.AmnShieldTheme
import com.alhaq.amnshield.R
import com.alhaq.amnshield.receivers.AdminReceiver
import com.alhaq.amnshield.services.AmnShieldAccessibilityService
import com.alhaq.amnshield.utils.SavedPreferencesLoader
import com.alhaq.amnshield.premium.PremiumManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class HomeFragment : Fragment() {

    private val premiumManager by lazy { PremiumManager.getInstance(requireContext().applicationContext) }
    private val savedPreferencesLoader by lazy { SavedPreferencesLoader(requireContext()) }
    private var serviceSnackbar: Snackbar? = null

    // Compose States
    private var isMainServiceEnabledState = mutableStateOf(false)
    private var isPremiumUserState = mutableStateOf(false)
    private var isFocusModeActiveState = mutableStateOf(false)
    private var isAppBlockerEnabledState = mutableStateOf(false)
    private var isKeywordBlockerEnabledState = mutableStateOf(false)
    private var isUsageTrackerEnabledState = mutableStateOf(false)
    private var isReelBlockerEnabledState = mutableStateOf(false)
    private var isAntiUninstallEnabledState = mutableStateOf(false)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AmnShieldTheme {
                    HomeScreen(
                        isMainServiceEnabled = isMainServiceEnabledState.value,
                        isPremiumUser = isPremiumUserState.value,
                        isFocusModeActive = isFocusModeActiveState.value,
                        isAppBlockerEnabled = isAppBlockerEnabledState.value,
                        isKeywordBlockerEnabled = isKeywordBlockerEnabledState.value,
                        isUsageTrackerEnabled = isUsageTrackerEnabledState.value,
                        isReelBlockerEnabled = isReelBlockerEnabledState.value,
                        isAntiUninstallEnabled = isAntiUninstallEnabledState.value,
                        onFeatureClicked = { featureType ->
                            launchFeatureActivity(featureType)
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateServiceStatus()
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
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
            val isMainServiceEnabled = isAccessibilityServiceEnabled(AmnShieldAccessibilityService::class.java)
            val isPremiumUser = premiumManager.isPremium()
            
            val focusModeData = savedPreferencesLoader.getFocusModeData()
            val isFocusModeActive = isPremiumUser && focusModeData.isTurnedOn && isMainServiceEnabled

            val hasBlockedApps = savedPreferencesLoader.loadBlockedApps().isNotEmpty()
            val isAppBlockerEnabled = isPremiumUser && savedPreferencesLoader.isAppBlockerFeatureEnabled() && hasBlockedApps && isMainServiceEnabled

            val hasCustomKeywords = savedPreferencesLoader.loadBlockedKeywords().isNotEmpty()
            val hasAdultPack = savedPreferencesLoader.isKeywordBlockerAdultPackEnabled()
            val isKeywordBlockerConfigured = hasCustomKeywords || hasAdultPack
            val isKeywordBlockerEnabled = savedPreferencesLoader.isKeywordBlockerFeatureEnabled() && isKeywordBlockerConfigured && isMainServiceEnabled

            val isUsageTrackerEnabled = savedPreferencesLoader.isUsageTrackerFeatureEnabled() && isMainServiceEnabled

            val viewBlockerPrefs = ctx.getSharedPreferences("view_blocker", android.content.Context.MODE_PRIVATE)
            val legacyEnabled = viewBlockerPrefs.getBoolean("is_enabled", false)
            val reelBlockerPrefs = ctx.getSharedPreferences("reel_blocker", android.content.Context.MODE_PRIVATE)
            val isReelBlockerEnabled = reelBlockerPrefs.getBoolean("is_enabled", legacyEnabled) && isMainServiceEnabled

            val antiUninstallPrefs = ctx.getSharedPreferences("anti_uninstall", Context.MODE_PRIVATE)
            val hasDeviceAdmin = isDeviceAdminEnabled(ctx)
            val isAntiUninstallEnabled = antiUninstallPrefs.getBoolean("is_anti_uninstall_on", false) && hasDeviceAdmin

            // Update Compose states
            isMainServiceEnabledState.value = isMainServiceEnabled
            isPremiumUserState.value = isPremiumUser
            isFocusModeActiveState.value = isFocusModeActive
            isAppBlockerEnabledState.value = isAppBlockerEnabled
            isKeywordBlockerEnabledState.value = isKeywordBlockerEnabled
            isUsageTrackerEnabledState.value = isUsageTrackerEnabled
            isReelBlockerEnabledState.value = isReelBlockerEnabled
            isAntiUninstallEnabledState.value = isAntiUninstallEnabled

            if (!isMainServiceEnabled) {
                if (serviceSnackbar == null) {
                    serviceSnackbar = Snackbar.make(
                        requireView(),
                        "AmnShield service is disabled. Please enable it for full protection.",
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

    private fun isAccessibilityServiceEnabled(serviceClass: Class<out AccessibilityService>): Boolean {
        val serviceName = context?.let { ComponentName(it, serviceClass).flattenToString() }
        val enabledServices = Settings.Secure.getString(
            context?.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(serviceName ?: "") == true
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
    }
}
