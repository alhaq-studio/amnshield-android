package com.alhaq.amnshield.ui.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.alhaq.amnshield.R
import com.alhaq.amnshield.data.blockers.AppBlockScheduleRule
import com.alhaq.amnshield.data.blockers.UnifiedFeatureScheduleRule
import com.alhaq.amnshield.data.blockers.AppLaunchLimitRule
import com.alhaq.amnshield.premium.PremiumManager
import com.alhaq.amnshield.services.AmnShieldAccessibilityService
import com.alhaq.amnshield.ui.screens.BlocksManagerScreen
import com.alhaq.amnshield.ui.screens.CreateRuleScreen
import com.alhaq.amnshield.ui.theme.AmnShieldTheme
import com.alhaq.amnshield.ui.viewmodel.AmnShieldViewModel
import com.alhaq.amnshield.utils.SavedPreferencesLoader
import java.util.Calendar
import java.util.UUID

class BlocksManagerFragment : Fragment() {

    private lateinit var viewModel: AmnShieldViewModel
    private lateinit var savedPreferencesLoader: SavedPreferencesLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[AmnShieldViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val state by viewModel.state.collectAsState()
                val action = remember { arguments?.getString("action") }
                val prefillTarget = remember { arguments?.getString("prefill_target") }
                val prefillType = remember { arguments?.getString("prefill_type") }

                var currentScreen by remember { mutableStateOf(if (action == "create") "create" else "manage") } // "manage" or "create"
                var initialType by remember { mutableStateOf(prefillType ?: "Block Schedule") }
                var editingRule by remember { mutableStateOf<com.alhaq.amnshield.ui.state.ScheduleRule?>(null) }

                AmnShieldTheme(appTheme = state.currentTheme) {
                    if (currentScreen == "manage") {
                        BlocksManagerScreen(
                            state = state,
                            viewModel = viewModel,
                            onNavigateToCreateRule = { type ->
                                editingRule = null
                                initialType = type
                                currentScreen = "create"
                            },
                            onEditRule = { rule ->
                                editingRule = rule
                                initialType = rule.restrictionType
                                currentScreen = "create"
                            },
                            onToggleRule = { id -> toggleScheduleRuleActive(id) },
                            onDeleteRule = { id -> deleteScheduleRule(id) },
                            onBack = {
                                if (!parentFragmentManager.popBackStackImmediate()) {
                                    requireActivity().finish()
                                }
                            }
                        )
                    } else {
                        CreateRuleScreen(
                            state = state,
                            viewModel = viewModel,
                            initialType = initialType,
                            prefillTarget = prefillTarget,
                            editingRule = editingRule,
                            onSaveRule = { rule ->
                                if (editingRule != null) {
                                    deleteScheduleRule(editingRule!!.id)
                                }
                                saveScheduleRule(rule)
                                editingRule = null
                            },
                            onBack = {
                                editingRule = null
                                if (action == "create") {
                                    if (!parentFragmentManager.popBackStackImmediate()) {
                                        requireActivity().finish()
                                    }
                                } else {
                                    currentScreen = "manage"
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!PremiumManager.getInstance(requireContext().applicationContext).isPremium()) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Premium Required")
                .setMessage("Schedules and launch limits are available for premium users.")
                .setPositiveButton("View Plans") { _, _ ->
                    val intent = Intent(requireContext(), com.alhaq.amnshield.ui.activity.FragmentActivity::class.java)
                    intent.putExtra("feature_type", "premium_features")
                    startActivity(intent)
                    requireActivity().finish()
                }
                .setNegativeButton("Close") { _, _ -> requireActivity().finish() }
                .setCancelable(false)
                .show()
            return
        }
        
        savedPreferencesLoader = SavedPreferencesLoader(requireContext())
        loadSchedulesAndLimits()
    }

    override fun onResume() {
        super.onResume()
        if (::savedPreferencesLoader.isInitialized) {
            loadSchedulesAndLimits()
        }
    }

    private fun loadSchedulesAndLimits() {
        val appSchedules = savedPreferencesLoader.loadAppBlockerScheduleRules()
        val featureSchedules = savedPreferencesLoader.loadUnifiedFeatureScheduleRules()
        val launchLimits = savedPreferencesLoader.loadAppLaunchLimitRules()

        val rulesList = mutableListOf<com.alhaq.amnshield.ui.state.ScheduleRule>()

        // Find all unique group IDs across appSchedules and featureSchedules
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
                    val daysList = item.selectedDays.map { calendarIntToDay(it) }
                    periods.add(com.alhaq.amnshield.ui.state.SchedulePeriod(start, end, daysList))
                }
                associatedFeatures.forEach { item ->
                    val start = String.format("%02d:%02d", item.startMinute / 60, item.startMinute % 60)
                    val end = String.format("%02d:%02d", item.endMinute / 60, item.endMinute % 60)
                    val daysList = item.selectedDays.map { calendarIntToDay(it) }
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

        val enforcementPrefs = requireContext().getSharedPreferences("enforcement_prefs", Context.MODE_PRIVATE)
        val isAdvanced = enforcementPrefs.getString("enforcement_mode", "SIMPLE") == "ADVANCED"

        viewModel.loadState(
            viewModel.state.value.copy(
                scheduleRules = rulesList,
                isAdvancedMode = isAdvanced
            )
        )
    }

    private fun saveScheduleRule(rule: com.alhaq.amnshield.ui.state.ScheduleRule) {
        if (rule.restrictionType == "Launch Limit") {
            val appPkg = rule.selectedApps.firstOrNull() ?: ""
            val limitRule = AppLaunchLimitRule(
                id = rule.id,
                packageName = appPkg,
                maxLaunches = rule.limitValue,
                timePeriod = AppLaunchLimitRule.TimePeriod.DAILY,
                createdAt = System.currentTimeMillis()
            )
            savedPreferencesLoader.addAppLaunchLimitRule(limitRule)
            sendRefreshRequest()
            loadSchedulesAndLimits()
            Toast.makeText(requireContext(), "Rule applied successfully", Toast.LENGTH_SHORT).show()
            return
        }

        if (rule.restrictionType == "Usage Limit") {
            // Delete existing rules with the same groupId to avoid conflicts
            savedPreferencesLoader.removeAppBlockerScheduleGroup(rule.id)
            savedPreferencesLoader.removeAppBlockerScheduleRule(rule.id)

            rule.selectedApps.forEach { appPkg ->
                val appName = try {
                    requireContext().packageManager.getApplicationLabel(
                        requireContext().packageManager.getApplicationInfo(appPkg, 0)
                    ).toString()
                } catch (_: Exception) {
                    appPkg
                }

                val appRule = AppBlockScheduleRule(
                    id = UUID.randomUUID().toString(),
                    title = "Usage Limit • $appName • ${rule.limitValue}h",
                    packageName = appPkg,
                    type = AppBlockScheduleRule.RuleType.BLOCK,
                    recurrence = AppBlockScheduleRule.Recurrence.DAILY,
                    startMinute = 0,
                    endMinute = 0,
                    selectedDays = emptySet(),
                    durationHours = rule.limitValue,
                    createdAt = System.currentTimeMillis(),
                    groupId = rule.id,
                    groupTitle = rule.name,
                    isEnabled = rule.isActive
                )
                savedPreferencesLoader.upsertAppBlockerScheduleRule(appRule)
            }
            sendRefreshRequest()
            loadSchedulesAndLimits()
            Toast.makeText(requireContext(), "Usage limit rule applied successfully", Toast.LENGTH_SHORT).show()
            return
        }

        // If not Launch Limit, delete any existing rules with the same groupId/id to avoid conflicts
        savedPreferencesLoader.removeAppBlockerScheduleGroup(rule.id)
        savedPreferencesLoader.removeAppBlockerScheduleRule(rule.id)
        savedPreferencesLoader.removeUnifiedFeatureScheduleGroup(rule.id)
        savedPreferencesLoader.removeUnifiedFeatureScheduleRule(rule.id)

        val groupId = rule.id
        val groupTitle = rule.name
        val selectedBlockers = rule.selectedBlockers.ifEmpty { listOf(rule.targetBlockerType) }

        val appRuleType = if (rule.restrictionType == "Cheat Window") {
            AppBlockScheduleRule.RuleType.CHEAT
        } else {
            AppBlockScheduleRule.RuleType.BLOCK
        }

        val ufsRuleType = if (rule.restrictionType == "Cheat Window") {
            UnifiedFeatureScheduleRule.RuleType.CHEAT
        } else {
            UnifiedFeatureScheduleRule.RuleType.BLOCK
        }

        // Loop through each period and save
        rule.periods.forEach { period ->
            val startMin = timeToMinutes(period.startTime)
            val endMin = timeToMinutes(period.endTime)
            val calendarDays = period.days.map { dayToCalendarInt(it) }.toSet()

            val recurrence = if (period.days.size == 7) {
                AppBlockScheduleRule.Recurrence.DAILY
            } else {
                AppBlockScheduleRule.Recurrence.WEEKLY
            }

            val ufsRecurrence = if (period.days.size == 7) {
                UnifiedFeatureScheduleRule.Recurrence.DAILY
            } else {
                UnifiedFeatureScheduleRule.Recurrence.WEEKLY
            }

            // 1. Save App Block Rules if App Blocker is selected
            if (selectedBlockers.contains("App Blocker")) {
                rule.selectedApps.forEach { appPkg ->
                    val appName = try {
                        requireContext().packageManager.getApplicationLabel(
                            requireContext().packageManager.getApplicationInfo(appPkg, 0)
                        ).toString()
                    } catch (_: Exception) {
                        appPkg
                    }

                    val appRule = AppBlockScheduleRule(
                        id = UUID.randomUUID().toString(),
                        title = "Unified • $appName • ${appRuleType.name} • ${recurrence.name}",
                        packageName = appPkg,
                        type = appRuleType,
                        recurrence = recurrence,
                        startMinute = startMin,
                        endMinute = endMin,
                        selectedDays = calendarDays,
                        createdAt = System.currentTimeMillis(),
                        groupId = groupId,
                        groupTitle = groupTitle,
                        isEnabled = rule.isActive
                    )
                    savedPreferencesLoader.upsertAppBlockerScheduleRule(appRule)
                }
            }

            // 2. Save Unified Feature Rules if other blockers are selected
            val featureTargets = mutableSetOf<UnifiedFeatureScheduleRule.FeatureTarget>()
            if (selectedBlockers.contains("App Blocker")) featureTargets.add(UnifiedFeatureScheduleRule.FeatureTarget.APP_BLOCKER)
            if (selectedBlockers.contains("Keyword Blocker")) featureTargets.add(UnifiedFeatureScheduleRule.FeatureTarget.KEYWORD_BLOCKER)
            if (selectedBlockers.contains("Website Blocker")) featureTargets.add(UnifiedFeatureScheduleRule.FeatureTarget.WEBSITE_BLOCKER)
            if (selectedBlockers.contains("Reels Blocker")) featureTargets.add(UnifiedFeatureScheduleRule.FeatureTarget.REEL_BLOCKER)
            if (selectedBlockers.contains("Notification Shielder") || selectedBlockers.contains("Notification Shield")) {
                featureTargets.add(UnifiedFeatureScheduleRule.FeatureTarget.FOCUS_MODE)
            }

            if (featureTargets.isNotEmpty()) {
                val ufsRule = UnifiedFeatureScheduleRule(
                    id = UUID.randomUUID().toString(),
                    title = groupTitle,
                    type = ufsRuleType,
                    recurrence = ufsRecurrence,
                    targets = featureTargets,
                    startMinute = startMin,
                    endMinute = endMin,
                    selectedDays = calendarDays,
                    createdAt = System.currentTimeMillis(),
                    groupId = groupId,
                    groupTitle = groupTitle,
                    isEnabled = rule.isActive,
                    selectedWebsites = rule.selectedWebsites,
                    selectedKeywords = rule.selectedKeywords,
                    selectedPlatforms = rule.selectedPlatforms
                )
                savedPreferencesLoader.upsertUnifiedFeatureScheduleRule(ufsRule)
            }
        }

        sendRefreshRequest()
        loadSchedulesAndLimits()
        Toast.makeText(requireContext(), "Rule applied successfully", Toast.LENGTH_SHORT).show()
    }

    // Callbacks from ManageSchedulesScreen
    fun toggleScheduleRuleActive(id: String) {
        viewModel.toggleScheduleRuleActive(id)
        
        if (id.startsWith("limit::")) {
            // Launch Limits don't have built-in switch toggles in legacy backend, they are deleted
        } else {
            val appRules = savedPreferencesLoader.loadAppBlockerScheduleRules()
            var appModified = false
            var targetState: Boolean? = null
            
            appRules.forEachIndexed { idx, item ->
                if (item.groupId == id || item.id == id) {
                    if (targetState == null) {
                        targetState = !(item.isEnabled ?: true)
                    }
                    appRules[idx] = item.copy(isEnabled = targetState)
                    appModified = true
                }
            }
            if (appModified) {
                savedPreferencesLoader.saveAppBlockerScheduleRules(appRules)
            }

            val featRules = savedPreferencesLoader.loadUnifiedFeatureScheduleRules()
            var featModified = false
            featRules.forEachIndexed { idx, item ->
                if (item.groupId == id || item.id == id) {
                    if (targetState == null) {
                        targetState = !(item.isEnabled ?: true)
                    }
                    featRules[idx] = item.copy(isEnabled = targetState)
                    featModified = true
                }
            }
            if (featModified) {
                savedPreferencesLoader.saveUnifiedFeatureScheduleRules(featRules)
            }
        }
        sendRefreshRequest()
        loadSchedulesAndLimits()
    }

    fun deleteScheduleRule(id: String) {
        viewModel.deleteScheduleRule(id)
        
        if (id.startsWith("limit::")) {
            val pkg = id.removePrefix("limit::")
            savedPreferencesLoader.removeAppLaunchLimitRule(pkg)
        } else {
            savedPreferencesLoader.removeAppBlockerScheduleGroup(id)
            savedPreferencesLoader.removeAppBlockerScheduleRule(id)
            savedPreferencesLoader.removeUnifiedFeatureScheduleGroup(id)
            savedPreferencesLoader.removeUnifiedFeatureScheduleRule(id)
        }
        sendRefreshRequest()
        loadSchedulesAndLimits()
        Toast.makeText(requireContext(), "Rule deleted", Toast.LENGTH_SHORT).show()
    }

    private fun sendRefreshRequest() {
        val intent = Intent(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_APP_BLOCKER)
        requireContext().sendBroadcast(intent.setPackage(requireContext().packageName))
        val unifiedIntent = Intent(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_UNIFIED_FEATURE_SCHEDULES)
        requireContext().sendBroadcast(unifiedIntent.setPackage(requireContext().packageName))
    }

    private fun timeToMinutes(timeStr: String): Int {
        val parts = timeStr.split(":")
        if (parts.size != 2) return 0
        val hours = parts[0].toIntOrNull() ?: 0
        val minutes = parts[1].toIntOrNull() ?: 0
        return hours * 60 + minutes
    }

    private fun dayToCalendarInt(day: String): Int {
        return when (day.lowercase()) {
            "sun" -> Calendar.SUNDAY
            "mon" -> Calendar.MONDAY
            "tue" -> Calendar.TUESDAY
            "wed" -> Calendar.WEDNESDAY
            "thu" -> Calendar.THURSDAY
            "fri" -> Calendar.FRIDAY
            "sat" -> Calendar.SATURDAY
            else -> Calendar.MONDAY
        }
    }

    private fun calendarIntToDay(dayInt: Int): String {
        return when (dayInt) {
            Calendar.SUNDAY -> "Sun"
            Calendar.MONDAY -> "Mon"
            Calendar.TUESDAY -> "Tue"
            Calendar.WEDNESDAY -> "Wed"
            Calendar.THURSDAY -> "Thu"
            Calendar.FRIDAY -> "Fri"
            Calendar.SATURDAY -> "Sat"
            else -> "Mon"
        }
    }

    companion object {
        private const val FEATURE_RULE_PREFIX = "ufs::"
        private const val APP_GROUP_RULE_PREFIX = "app_group::"
        private const val FEATURE_GROUP_RULE_PREFIX = "feature_group::"
        const val FRAGMENT_ID = "blocks_manager_fragment"
    }
}
