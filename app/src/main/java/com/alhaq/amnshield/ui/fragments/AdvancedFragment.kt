package com.alhaq.amnshield.ui.fragments

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.app.admin.DevicePolicyManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.ViewModelProvider
import com.alhaq.amnshield.R
import com.alhaq.amnshield.premium.PremiumManager
import com.alhaq.amnshield.services.AmnShieldAccessibilityService
import com.alhaq.amnshield.receivers.AdminReceiver
import com.alhaq.amnshield.ui.activity.FragmentActivity
import com.alhaq.amnshield.ui.fragments.features.BaseFeatureFragment
import com.alhaq.amnshield.data.blockers.AppBlockScheduleRule
import com.alhaq.amnshield.utils.SavedPreferencesLoader
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.alhaq.amnshield.ui.theme.AmnShieldTheme
import com.alhaq.amnshield.ui.screens.AdvancedScreen
import com.alhaq.amnshield.ui.viewmodel.AmnShieldViewModel
import com.alhaq.amnshield.ui.state.AmnShieldState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

class AdvancedFragment : BaseFeatureFragment() {

    private val premiumManager by lazy { PremiumManager.getInstance(requireContext().applicationContext) }
    private val loader by lazy { SavedPreferencesLoader(requireContext()) }
    private lateinit var viewModel: AmnShieldViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[AmnShieldViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val state by viewModel.state.collectAsState()
                AmnShieldTheme(appTheme = state.currentTheme) {
                    AdvancedScreen(
                        state = state,
                        onNavigateToAppBlocker = { openFeatureConfig("app_blocker", requiresPremium = false) },
                        onNavigateToKeywordBlocker = { openFeatureConfig("keyword_blocker", requiresPremium = false) },
                        onNavigateToWebBlocker = { openFeatureConfig("website_blocker", requiresPremium = false) },
                        onNavigateToReelsBlocker = { openFeatureConfig("reel_blocker", requiresPremium = false) },
                        onNavigateToAntiUninstall = { openFeatureConfig("anti_uninstall", requiresPremium = true) },
                        onNavigateToUsageTracker = { openFeatureConfig("usage_tracker", requiresPremium = false) },
                        onNavigateToPremium = { openFeatureConfig("premium_features", requiresPremium = false) },
                        onTogglePinSecurity = { enabled, pin -> togglePinSecurity(enabled, pin) },
                        onToggleAppLock = { enabled -> toggleAppLock(enabled) },
                        onToggleBypassPinLock = { enabled -> toggleBypassPinLock(enabled) }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshDashboard()
    }

    private fun refreshDashboard() {
        val serviceEnabled = isAccessibilityServiceEnabled(AmnShieldAccessibilityService::class.java)
        val premiumEnabled = premiumManager.isPremium()

        val appBlockedApps = loader.loadBlockedApps().size
        val appActive = premiumEnabled && loader.isAppBlockerFeatureEnabled() && appBlockedApps > 0 && serviceEnabled

        val keywordCount = loader.loadBlockedKeywords().size
        val adultPackEnabled = loader.isKeywordBlockerAdultPackEnabled()
        val keywordActive = loader.isKeywordBlockerFeatureEnabled() && (keywordCount > 0 || adultPackEnabled) && serviceEnabled

        val focusData = loader.getFocusModeData()
        val focusActive = premiumEnabled && focusData.isTurnedOn && serviceEnabled

        val websiteActive = premiumEnabled && loader.isWebsiteBlockerEnabled() && serviceEnabled

        val allSchedules = loader.loadAppBlockerScheduleRules()
        val scheduleCount = allSchedules.count { it.type == AppBlockScheduleRule.RuleType.BLOCK }
        val launchLimitCount = loader.loadAppLaunchLimitRules().size

        val viewBlockerPrefs = requireContext().getSharedPreferences("view_blocker", Context.MODE_PRIVATE)
        val legacyEnabled = viewBlockerPrefs.getBoolean("is_enabled", false)
        val reelBlockerPrefs = requireContext().getSharedPreferences("reel_blocker", Context.MODE_PRIVATE)
        val reelsActive = reelBlockerPrefs.getBoolean("is_enabled", legacyEnabled) && serviceEnabled

        val antiUninstallPrefs = requireContext().getSharedPreferences("anti_uninstall", Context.MODE_PRIVATE)
        val hasDeviceAdmin = isDeviceAdminEnabled(requireContext())
        val antiUninstallActive = antiUninstallPrefs.getBoolean("is_anti_uninstall_on", false) && hasDeviceAdmin

        val usageTrackerActive = loader.isUsageTrackerFeatureEnabled() && serviceEnabled

        val pinEnabled = loader.isPinSecurityEnabled()
        val pinCode = loader.getPinCode()
        val appLockActive = loader.isAppLockEnabled()
        val bypassPinLockActive = loader.isBypassPinLockEnabled()

        viewModel.loadState(
            AmnShieldState(
                isMainServiceEnabled = serviceEnabled,
                isPremiumUser = premiumEnabled,
                isUsageTrackerEnabled = usageTrackerActive,
                isAntiUninstallEnabled = antiUninstallActive,
                isAppBlockerEnabled = appActive,
                isReelsBlockerEnabled = reelsActive,
                isKeywordBlockerEnabled = keywordActive,
                isWebFilterEnabled = websiteActive,
                isFocusModeActive = focusActive,
                isScheduleEnabled = scheduleCount > 0,
                isUsageLimitEnabled = launchLimitCount > 0,
                keywords = loader.loadBlockedKeywords().toList(),
                isPinProtectionEnabled = pinEnabled,
                profilePin = pinCode,
                isAppLockEnabled = appLockActive,
                isBypassPinLockEnabled = bypassPinLockActive,
                isAdvancedMode = loader.getEnforcementMode() == "ADVANCED"
            )
        )
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

    // Advanced rules and schedules are now handled via the main Blocks tab screen

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

    private fun isDeviceAdminEnabled(ctx: Context): Boolean {
        val devicePolicyManager = ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val adminComponent = ComponentName(ctx, AdminReceiver::class.java)
        return devicePolicyManager?.isAdminActive(adminComponent) == true
    }

    private fun togglePinSecurity(enabled: Boolean, pin: String) {
        loader.setPinSecurityEnabled(enabled)
        loader.setPinCode(pin)
        if (!enabled) {
            loader.setAppLockEnabled(false)
            loader.setBypassPinLockEnabled(false)
        }
        viewModel.updateProfile(
            name = viewModel.state.value.userName,
            email = viewModel.state.value.userEmail,
            bio = viewModel.state.value.userBio,
            goalMinutes = viewModel.state.value.userGoalMinutes,
            profileType = viewModel.state.value.focusProfileType,
            pinEnabled = enabled,
            pin = pin
        )
        viewModel.updatePinSettings(
            appLock = if (enabled) loader.isAppLockEnabled() else false,
            bypassLock = if (enabled) loader.isBypassPinLockEnabled() else false
        )
    }

    private fun toggleAppLock(enabled: Boolean) {
        loader.setAppLockEnabled(enabled)
        viewModel.updatePinSettings(
            appLock = enabled,
            bypassLock = viewModel.state.value.isBypassPinLockEnabled
        )
    }

    private fun toggleBypassPinLock(enabled: Boolean) {
        loader.setBypassPinLockEnabled(enabled)
        viewModel.updatePinSettings(
            appLock = viewModel.state.value.isAppLockEnabled,
            bypassLock = enabled
        )
    }
}
