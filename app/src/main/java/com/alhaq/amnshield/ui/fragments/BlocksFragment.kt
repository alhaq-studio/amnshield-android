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
import com.alhaq.amnshield.data.blockers.UnifiedFeatureScheduleRule
import com.alhaq.amnshield.utils.ScheduleUtils
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
                        onNavigateToAppBlocker = { openFeatureConfig("app_blocker", requiresPremium = false) },
                        onNavigateToKeywordBlocker = { openFeatureConfig("keyword_blocker", requiresPremium = false) },
                        onNavigateToWebBlocker = { openFeatureConfig("social_media_blocker", requiresPremium = false) },
                        onNavigateToFocusMode = { openFeatureConfig("focus_mode", requiresPremium = false) },
                        onNavigateToCheatHours = { openCheatHours() },
                        onNavigateToSchedules = { openSchedules() },
                        onNavigateToLaunchLimits = { openLaunchLimits() },
                        onNavigateToAntiUninstall = { openFeatureConfig("anti_uninstall", requiresPremium = true) },
                        onNavigateToUsageTracker = { openFeatureConfig("usage_tracker", requiresPremium = false) },
                        onNavigateToReelsBlocker = { openFeatureConfig("reel_blocker", requiresPremium = false) },
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

        val rulesList = loadSchedulesAndLimits()

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
                keywords = blocksLoader.loadBlockedKeywords().toList(),
                isAdvancedMode = blocksLoader.getEnforcementMode() == "ADVANCED",
                scheduleRules = rulesList
            )
        )
    }

    private fun loadSchedulesAndLimits(): List<com.alhaq.amnshield.ui.state.ScheduleRule> {
        val appSchedules = blocksLoader.loadAppBlockerScheduleRules()
        val featureSchedules = blocksLoader.loadUnifiedFeatureScheduleRules()
        val launchLimits = blocksLoader.loadAppLaunchLimitRules()

        val rulesList = mutableListOf<com.alhaq.amnshield.ui.state.ScheduleRule>()

        // 1. Find all unique group IDs across appSchedules and featureSchedules
        val allAppGroupIds = appSchedules.map { it.groupId ?: it.id }.distinct()
        val allFeatureGroupIds = featureSchedules.map { it.groupId ?: it.id }.distinct()
        val allGroupIds = (allAppGroupIds + allFeatureGroupIds).distinct()

        allGroupIds.forEach { groupId ->
            val associatedApps = appSchedules.filter { (it.groupId ?: it.id) == groupId }
            val associatedFeatures = featureSchedules.filter { (it.groupId ?: it.id) == groupId }

            if (associatedApps.isNotEmpty() || associatedFeatures.isNotEmpty()) {
                val firstApp = associatedApps.firstOrNull()
                val firstFeature = associatedFeatures.firstOrNull()

                val name = firstApp?.groupTitle ?: firstApp?.title
                    ?: firstFeature?.groupTitle ?: firstFeature?.title
                    ?: "Unified Rule"

                val isEnabled = firstApp?.isEnabled ?: firstFeature?.isEnabled ?: true

                val restrictionTypeStr = when {
                    firstApp?.type == AppBlockScheduleRule.RuleType.CHEAT ||
                    firstFeature?.type == UnifiedFeatureScheduleRule.RuleType.CHEAT -> "Cheat Window"
                    firstApp != null && firstApp.durationHours > 0 -> "Usage Limit"
                    else -> "Block Schedule"
                }

                // Apps
                val apps = associatedApps.map { it.packageName }.distinct()

                // Blockers list
                val selectedBlockers = mutableListOf<String>()
                if (associatedApps.isNotEmpty()) {
                    selectedBlockers.add("App Blocker")
                }
                associatedFeatures.forEach { featRule ->
                    if (featRule.targets.contains(UnifiedFeatureScheduleRule.FeatureTarget.KEYWORD_BLOCKER)) {
                        selectedBlockers.add("Keyword Blocker")
                    }
                    if (featRule.targets.contains(UnifiedFeatureScheduleRule.FeatureTarget.WEBSITE_BLOCKER)) {
                        selectedBlockers.add("Website Blocker")
                    }
                    if (featRule.targets.contains(UnifiedFeatureScheduleRule.FeatureTarget.REEL_BLOCKER)) {
                        selectedBlockers.add("Reels Blocker")
                    }
                    if (featRule.targets.contains(UnifiedFeatureScheduleRule.FeatureTarget.FOCUS_MODE)) {
                        selectedBlockers.add("Notification Shield")
                    }
                }
                val distinctBlockers = selectedBlockers.distinct()

                // Display blockers / apps string
                val appOrCategory = if (distinctBlockers.size == 1) {
                    when (distinctBlockers.first()) {
                        "App Blocker" -> if (apps.size == 1) {
                            try {
                                requireContext().packageManager.getApplicationLabel(
                                    requireContext().packageManager.getApplicationInfo(apps.first(), 0)
                                ).toString()
                            } catch (_: Exception) {
                                apps.first()
                            }
                        } else {
                            "${apps.size} Apps"
                        }
                        else -> distinctBlockers.first()
                    }
                } else {
                    distinctBlockers.joinToString(" • ")
                }

                // Collect periods
                val periods = mutableListOf<com.alhaq.amnshield.ui.state.SchedulePeriod>()
                associatedApps.forEach { item ->
                    val start = String.format("%02d:%02d", item.startMinute / 60, item.startMinute % 60)
                    val end = String.format("%02d:%02d", item.endMinute / 60, item.endMinute % 60)
                    val daysList = item.selectedDays.map { ScheduleUtils.calendarIntToDay(it) }
                    periods.add(com.alhaq.amnshield.ui.state.SchedulePeriod(start, end, daysList))
                }
                associatedFeatures.forEach { item ->
                    val start = String.format("%02d:%02d", item.startMinute / 60, item.startMinute % 60)
                    val end = String.format("%02d:%02d", item.endMinute / 60, item.endMinute % 60)
                    val daysList = item.selectedDays.map { ScheduleUtils.calendarIntToDay(it) }
                    periods.add(com.alhaq.amnshield.ui.state.SchedulePeriod(start, end, daysList))
                }

                val distinctPeriods = periods.distinctBy { Triple(it.startTime, it.endTime, it.days.sorted()) }
                val firstPeriod = distinctPeriods.firstOrNull() ?: com.alhaq.amnshield.ui.state.SchedulePeriod("09:00", "17:00", listOf("Mon", "Tue", "Wed", "Thu", "Fri"))

                val limitVal = when (restrictionTypeStr) {
                    "Usage Limit" -> firstApp?.durationHours ?: 0
                    else -> 0
                }

                rulesList.add(
                    com.alhaq.amnshield.ui.state.ScheduleRule(
                        id = groupId,
                        name = name,
                        appOrCategory = appOrCategory,
                        restrictionType = restrictionTypeStr,
                        startTime = firstPeriod.startTime,
                        endTime = firstPeriod.endTime,
                        days = firstPeriod.days,
                        limitValue = limitVal,
                        isActive = isEnabled,
                        periods = distinctPeriods,
                        targetBlockerType = distinctBlockers.firstOrNull() ?: "App Blocker",
                        selectedApps = apps,
                        selectedBlockers = distinctBlockers
                    )
                )
            }
        }

        // 3. Launch Limits
        launchLimits.forEach { limit ->
            val appName = try {
                requireContext().packageManager.getApplicationLabel(
                    requireContext().packageManager.getApplicationInfo(limit.packageName, 0)
                ).toString()
            } catch (_: Exception) {
                limit.packageName
            }
            rulesList.add(
                com.alhaq.amnshield.ui.state.ScheduleRule(
                    id = "limit::${limit.packageName}",
                    name = "Limit: $appName",
                    appOrCategory = appName,
                    restrictionType = "Launch Limit",
                    startTime = "00:00",
                    endTime = "23:59",
                    days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
                    limitValue = limit.maxLaunches,
                    isActive = true,
                    targetBlockerType = "App Blocker",
                    selectedApps = listOf(limit.packageName),
                    selectedBlockers = listOf("App Blocker")
                )
            )
        }

        return rulesList
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
        val intent = Intent(requireContext(), FragmentActivity::class.java).apply {
            putExtra("fragment", BlocksManagerFragment.FRAGMENT_ID)
        }
        startActivity(intent, activityOptions.toBundle())
    }

    private fun openLaunchLimits() {
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