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
                val activeTheme = com.alhaq.amnshield.utils.ThemeUtils.resolveAppTheme(requireContext())
                viewModel.updateTheme(activeTheme)
                val state by viewModel.state.collectAsState()
                AmnShieldTheme(appTheme = activeTheme) {
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
        val launchLimits = blocksLoader.loadAppLaunchLimitRules()

        val rulesList = mutableListOf<com.alhaq.amnshield.ui.state.ScheduleRule>()

        val allGroupIds = appSchedules.map { it.groupId ?: it.id }.distinct()

        allGroupIds.forEach { groupId ->
            val associatedApps = appSchedules.filter { (it.groupId ?: it.id) == groupId }

            if (associatedApps.isNotEmpty()) {
                val firstApp = associatedApps.first()

                val name = firstApp.groupTitle ?: firstApp.title ?: "Unnamed Blocker"
                val isEnabled = firstApp.isEnabled ?: true

                val targetBlocker = when {
                    associatedApps.any { it.packageName == "keyword_blocker" } -> "Keyword Blocker"
                    associatedApps.any { it.packageName == "website_blocker" } -> "Website Blocker"
                    associatedApps.any { it.packageName == "reel_blocker" } -> "Reels Blocker"
                    associatedApps.any { it.packageName == "FOCUS_MODE" || it.packageName == "focus_mode" } -> "Focus Mode"
                    else -> "App Blocker"
                }

                val apps = associatedApps.map { it.packageName }.filter {
                    it != null && it != "keyword_blocker" && it != "website_blocker" && it != "reel_blocker" && it != "FOCUS_MODE" && it != "focus_mode"
                }.distinct()

                val appOrCategory = when (targetBlocker) {
                    "Keyword Blocker" -> "Keywords Blocker"
                    "Website Blocker" -> "Website Blocker"
                    "Reels Blocker" -> "Reels Blocker"
                    "Focus Mode" -> "Focus Mode Schedules"
                    else -> {
                        if (apps.size == 1) {
                            try {
                                requireContext().packageManager.getApplicationLabel(
                                    requireContext().packageManager.getApplicationInfo(apps.first(), 0)
                                ).toString()
                            } catch (e: Exception) {
                                apps.first()
                            }
                        } else {
                            "${apps.size} Apps"
                        }
                    }
                }

                // 1. Standard Schedule & Always Block
                val blockRules = associatedApps.filter { it.type == AppBlockScheduleRule.RuleType.BLOCK && it.durationHours <= 0 }
                val isAlwaysBlockEnabled = blockRules.any { it.recurrence == AppBlockScheduleRule.Recurrence.ALWAYS }
                val isScheduleEnabled = blockRules.isNotEmpty() && !isAlwaysBlockEnabled
                val firstBlock = blockRules.firstOrNull()
                val scheduleStartTime = if (firstBlock != null) String.format("%02d:%02d", firstBlock.startMinute / 60, firstBlock.startMinute % 60) else "09:00"
                val scheduleEndTime = if (firstBlock != null) String.format("%02d:%02d", firstBlock.endMinute / 60, firstBlock.endMinute % 60) else "17:00"
                val scheduleDays = if (firstBlock?.selectedDays != null && firstBlock.selectedDays.isNotEmpty()) {
                    firstBlock.selectedDays.map { ScheduleUtils.calendarIntToDay(it) }
                } else {
                    listOf("Mon", "Tue", "Wed", "Thu", "Fri")
                }

                // 2. Cheat Window
                val cheatRules = associatedApps.filter { it.type == AppBlockScheduleRule.RuleType.CHEAT }
                val isCheatEnabled = cheatRules.isNotEmpty()
                val firstCheat = cheatRules.firstOrNull()
                val cheatStartTime = if (firstCheat != null) String.format("%02d:%02d", firstCheat.startMinute / 60, firstCheat.startMinute % 60) else "12:00"
                val cheatEndTime = if (firstCheat != null) String.format("%02d:%02d", firstCheat.endMinute / 60, firstCheat.endMinute % 60) else "13:00"
                val cheatDays = if (firstCheat?.selectedDays != null) {
                    firstCheat.selectedDays.map { ScheduleUtils.calendarIntToDay(it) }
                } else {
                    listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                }

                // 3. Usage Limit
                val usageRules = associatedApps.filter { it.type == AppBlockScheduleRule.RuleType.BLOCK && it.durationHours > 0 }
                val isUsageLimitEnabled = usageRules.isNotEmpty()
                val usageLimitHours = if (isUsageLimitEnabled) usageRules.first().durationHours else 0

                val restrictionTypeStr = when {
                    isAlwaysBlockEnabled -> "Always Block (24/7)"
                    isScheduleEnabled -> "Block Schedule"
                    isUsageLimitEnabled -> "Usage Limit"
                    isCheatEnabled -> "Cheat Window"
                    else -> "Block Schedule"
                }

                val periods = mutableListOf<com.alhaq.amnshield.ui.state.SchedulePeriod>()
                associatedApps.forEach { item ->
                    val start = String.format("%02d:%02d", item.startMinute / 60, item.startMinute % 60)
                    val end = String.format("%02d:%02d", item.endMinute / 60, item.endMinute % 60)
                    val daysList = item.selectedDays?.map { ScheduleUtils.calendarIntToDay(it) } ?: emptyList()
                    periods.add(com.alhaq.amnshield.ui.state.SchedulePeriod(start, end, daysList))
                }
                val distinctPeriods = periods.distinctBy { Triple(it.startTime, it.endTime, it.days.sorted()) }

                rulesList.add(
                    com.alhaq.amnshield.ui.state.ScheduleRule(
                        id = groupId,
                        name = name,
                        appOrCategory = appOrCategory,
                        restrictionType = restrictionTypeStr,
                        startTime = scheduleStartTime,
                        endTime = scheduleEndTime,
                        days = scheduleDays,
                        limitValue = if (isUsageLimitEnabled) usageLimitHours else 0,
                        isActive = isEnabled,
                        periods = distinctPeriods,
                        targetBlockerType = targetBlocker,
                        selectedApps = apps,
                        selectedBlockers = listOf(targetBlocker),
                        
                        isAlwaysBlockEnabled = isAlwaysBlockEnabled,
                        isScheduleEnabled = isScheduleEnabled,
                        isCheatEnabled = isCheatEnabled,
                        cheatStartTime = cheatStartTime,
                        cheatEndTime = cheatEndTime,
                        cheatDays = cheatDays,
                        isUsageLimitEnabled = isUsageLimitEnabled,
                        usageLimitHours = usageLimitHours
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