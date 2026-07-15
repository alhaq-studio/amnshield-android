package com.alhaq.amnshield.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.ViewModelProvider
import com.alhaq.amnshield.R
import com.alhaq.amnshield.premium.PremiumManager
import com.alhaq.amnshield.services.AmnShieldAccessibilityService
import android.content.ComponentName
import android.content.Context
import android.app.admin.DevicePolicyManager
import com.alhaq.amnshield.receivers.AdminReceiver
import com.alhaq.amnshield.ui.activity.FragmentActivity
import com.alhaq.amnshield.ui.fragments.features.BaseFeatureFragment
import com.alhaq.amnshield.data.blockers.AppBlockScheduleRule
import com.alhaq.amnshield.utils.SavedPreferencesLoader
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.alhaq.amnshield.ui.theme.AmnShieldTheme
import com.alhaq.amnshield.ui.screens.BlocksScreen
import com.alhaq.amnshield.ui.viewmodel.AmnShieldViewModel
import com.alhaq.amnshield.ui.state.AmnShieldState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

class BlocksFragment : BaseFeatureFragment() {

    private val premiumManager by lazy { PremiumManager.getInstance(requireContext().applicationContext) }
    private val blocksLoader by lazy { SavedPreferencesLoader(requireContext()) }
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
                    BlocksScreen(
                        state = state,
                        viewModel = viewModel,
                        onNavigateToAppBlocker = { openFeatureConfig("app_blocker", requiresPremium = true) },
                        onNavigateToKeywordBlocker = { openFeatureConfig("keyword_blocker", requiresPremium = false) },
                        onNavigateToWebBlocker = { openFeatureConfig("social_media_blocker", requiresPremium = true) },
                        onNavigateToFocusMode = { openFeatureConfig("focus_mode", requiresPremium = true) },
                        onNavigateToCheatHours = { openCheatHours() },
                        onNavigateToSchedules = { openSchedules() },
                        onNavigateToLaunchLimits = { openLaunchLimits() },
                        onNavigateToAntiUninstall = { openFeatureConfig("anti_uninstall", requiresPremium = true) },
                        onNavigateToUsageTracker = { openFeatureConfig("usage_tracker", requiresPremium = false) },
                        onNavigateToReelsBlocker = { openFeatureConfig("reel_blocker", requiresPremium = true) },
                        onNavigateToPremium = { openFeatureConfig("premium_features", requiresPremium = false) }
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

        val appBlockedApps = blocksLoader.loadBlockedApps().size
        val appActive = premiumEnabled && blocksLoader.isAppBlockerFeatureEnabled() && appBlockedApps > 0 && serviceEnabled

        val keywordCount = blocksLoader.loadBlockedKeywords().size
        val adultPackEnabled = blocksLoader.isKeywordBlockerAdultPackEnabled()
        val keywordActive = blocksLoader.isKeywordBlockerFeatureEnabled() && (keywordCount > 0 || adultPackEnabled) && serviceEnabled

        val focusData = blocksLoader.getFocusModeData()
        val focusActive = premiumEnabled && focusData.isTurnedOn && serviceEnabled

        val websiteActive = premiumEnabled && blocksLoader.isWebsiteBlockerEnabled() && serviceEnabled

        val allSchedules = blocksLoader.loadAppBlockerScheduleRules()
        val cheatCount = allSchedules.count { it.type == AppBlockScheduleRule.RuleType.CHEAT }
        val scheduleCount = allSchedules.count { it.type == AppBlockScheduleRule.RuleType.BLOCK }
        val launchLimitCount = blocksLoader.loadAppLaunchLimitRules().size

        val viewBlockerPrefs = requireContext().getSharedPreferences("view_blocker", Context.MODE_PRIVATE)
        val legacyEnabled = viewBlockerPrefs.getBoolean("is_enabled", false)
        val reelBlockerPrefs = requireContext().getSharedPreferences("reel_blocker", Context.MODE_PRIVATE)
        val reelsActive = reelBlockerPrefs.getBoolean("is_enabled", legacyEnabled) && serviceEnabled

        val antiUninstallPrefs = requireContext().getSharedPreferences("anti_uninstall", Context.MODE_PRIVATE)
        val hasDeviceAdmin = isDeviceAdminEnabled(requireContext())
        val antiUninstallActive = antiUninstallPrefs.getBoolean("is_anti_uninstall_on", false) && hasDeviceAdmin

        val usageTrackerActive = blocksLoader.isUsageTrackerFeatureEnabled() && serviceEnabled

        // Load actual live configurations to AmnShieldState
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
                keywords = blocksLoader.loadBlockedKeywords().toList()
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

    private fun openCheatHours() {
        openSchedules()
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

    private fun isDeviceAdminEnabled(ctx: Context): Boolean {
        val devicePolicyManager = ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val adminComponent = ComponentName(ctx, AdminReceiver::class.java)
        return devicePolicyManager?.isAdminActive(adminComponent) == true
    }
}