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
import com.alhaq.amnshield.ui.screens.ManageSchedulesScreen
import com.alhaq.amnshield.ui.screens.CreateRuleScreen
import com.alhaq.amnshield.ui.theme.AmnShieldTheme
import com.alhaq.amnshield.ui.viewmodel.AmnShieldViewModel
import com.alhaq.amnshield.utils.SavedPreferencesLoader
import java.util.Calendar
import java.util.UUID

class ManageBlockSchedulesFragment : Fragment() {

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
                var currentScreen by remember { mutableStateOf("manage") } // "manage" or "create"
                var initialType by remember { mutableStateOf("Block Schedule") }

                AmnShieldTheme(appTheme = state.currentTheme) {
                    if (currentScreen == "manage") {
                        ManageSchedulesScreen(
                            state = state,
                            viewModel = viewModel,
                            onNavigateToCreateRule = { type ->
                                initialType = type
                                currentScreen = "create"
                            },
                            onToggleRule = { id -> toggleScheduleRuleActive(id) },
                            onDeleteRule = { id -> deleteScheduleRule(id) },
                            onBack = {
                                requireActivity().onBackPressedDispatcher.onBackPressed()
                            }
                        )
                    } else {
                        CreateRuleScreen(
                            state = state,
                            viewModel = viewModel,
                            initialType = initialType,
                            onSaveRule = { rule ->
                                saveScheduleRule(rule)
                            },
                            onBack = {
                                currentScreen = "manage"
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

        // 1. App Block Schedules
        val groupedApp = appSchedules.groupBy { it.groupId ?: it.id }
        groupedApp.forEach { (groupId, rules) ->
            val first = rules.first()
            val apps = rules.map { it.packageName }
            val displayDays = first.selectedDays.map { calendarIntToDay(it) }
            val restrictionTypeStr = if (first.type == AppBlockScheduleRule.RuleType.CHEAT) "Cheat Window" else "Block Schedule"
            val appOrCategory = if (apps.size == 1) {
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
            
            rulesList.add(
                com.alhaq.amnshield.ui.state.ScheduleRule(
                    id = if (first.groupId != null) "app_group::$groupId" else first.id,
                    name = first.groupTitle ?: first.title,
                    appOrCategory = appOrCategory,
                    restrictionType = restrictionTypeStr,
                    startTime = String.format("%02d:%02d", first.startMinute / 60, first.startMinute % 60),
                    endTime = String.format("%02d:%02d", first.endMinute / 60, first.endMinute % 60),
                    days = displayDays,
                    isActive = first.isRuleEnabled,
                    targetBlockerType = "App Blocker",
                    selectedApps = apps
                )
            )
        }

        // 2. Feature Schedules
        featureSchedules.forEach { rule ->
            val featureName = when {
                rule.targets.contains(UnifiedFeatureScheduleRule.FeatureTarget.REEL_BLOCKER) -> "Reels Blocker"
                rule.targets.contains(UnifiedFeatureScheduleRule.FeatureTarget.KEYWORD_BLOCKER) -> "Keyword Blocker"
                rule.targets.contains(UnifiedFeatureScheduleRule.FeatureTarget.WEBSITE_BLOCKER) -> "Website Blocker"
                else -> "Notification Shielder"
            }
            val displayDays = rule.selectedDays.map { calendarIntToDay(it) }
            val restrictionTypeStr = if (rule.type == UnifiedFeatureScheduleRule.RuleType.CHEAT) "Cheat Window" else "Block Schedule"
            
            rulesList.add(
                com.alhaq.amnshield.ui.state.ScheduleRule(
                    id = "ufs::${rule.id}",
                    name = rule.title,
                    appOrCategory = featureName,
                    restrictionType = restrictionTypeStr,
                    startTime = String.format("%02d:%02d", rule.startMinute / 60, rule.startMinute % 60),
                    endTime = String.format("%02d:%02d", rule.endMinute / 60, rule.endMinute % 60),
                    days = displayDays,
                    isActive = rule.isRuleEnabled,
                    targetBlockerType = featureName,
                    selectedKeywords = rule.targets.map { it.name }.toList()
                )
            )
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
                    selectedApps = listOf(limit.packageName)
                )
            )
        }

        viewModel.loadState(
            viewModel.state.value.copy(
                scheduleRules = rulesList
            )
        )
    }

    private fun saveScheduleRule(rule: com.alhaq.amnshield.ui.state.ScheduleRule) {
        val startMin = timeToMinutes(rule.startTime)
        val endMin = timeToMinutes(rule.endTime)
        val calendarDays = rule.days.map { dayToCalendarInt(it) }.toSet()

        when (rule.restrictionType) {
            "Launch Limit" -> {
                val appPkg = rule.selectedApps.firstOrNull() ?: ""
                val limitRule = AppLaunchLimitRule(
                    id = rule.id,
                    packageName = appPkg,
                    maxLaunches = rule.limitValue,
                    timePeriod = AppLaunchLimitRule.TimePeriod.DAILY,
                    createdAt = System.currentTimeMillis()
                )
                savedPreferencesLoader.addAppLaunchLimitRule(limitRule)
            }
            else -> {
                val ruleType = if (rule.restrictionType == "Cheat Window") {
                    AppBlockScheduleRule.RuleType.CHEAT
                } else {
                    AppBlockScheduleRule.RuleType.BLOCK
                }

                val recurrence = if (rule.days.size == 7) {
                    AppBlockScheduleRule.Recurrence.DAILY
                } else {
                    AppBlockScheduleRule.Recurrence.WEEKLY
                }

                if (rule.targetBlockerType == "App Blocker") {
                    val groupId = UUID.randomUUID().toString()
                    val groupTitle = rule.name
                    
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
                            title = "Unified • $appName • ${ruleType.name} • ${recurrence.name}",
                            packageName = appPkg,
                            type = ruleType,
                            recurrence = recurrence,
                            startMinute = startMin,
                            endMinute = endMin,
                            selectedDays = calendarDays,
                            createdAt = System.currentTimeMillis(),
                            groupId = groupId,
                            groupTitle = groupTitle,
                            isEnabled = true
                        )
                        savedPreferencesLoader.upsertAppBlockerScheduleRule(appRule)
                    }
                } else {
                    val featureTargets = mutableSetOf<UnifiedFeatureScheduleRule.FeatureTarget>()
                    when (rule.targetBlockerType) {
                        "Keyword Blocker" -> featureTargets.add(UnifiedFeatureScheduleRule.FeatureTarget.KEYWORD_BLOCKER)
                        "Website Blocker" -> featureTargets.add(UnifiedFeatureScheduleRule.FeatureTarget.WEBSITE_BLOCKER)
                        "Reels Blocker" -> featureTargets.add(UnifiedFeatureScheduleRule.FeatureTarget.REEL_BLOCKER)
                        "Notification Shielder" -> featureTargets.add(UnifiedFeatureScheduleRule.FeatureTarget.FOCUS_MODE)
                    }
                    
                    val ufsType = if (rule.restrictionType == "Cheat Window") {
                        UnifiedFeatureScheduleRule.RuleType.CHEAT
                    } else {
                        UnifiedFeatureScheduleRule.RuleType.BLOCK
                    }
                    
                    val ufsRecurrence = if (rule.days.size == 7) {
                        UnifiedFeatureScheduleRule.Recurrence.DAILY
                    } else {
                        UnifiedFeatureScheduleRule.Recurrence.WEEKLY
                    }
                    
                    val ufsRule = UnifiedFeatureScheduleRule(
                        id = rule.id,
                        title = rule.name,
                        type = ufsType,
                        recurrence = ufsRecurrence,
                        targets = featureTargets,
                        startMinute = startMin,
                        endMinute = endMin,
                        selectedDays = calendarDays,
                        createdAt = System.currentTimeMillis(),
                        isEnabled = true
                    )
                    savedPreferencesLoader.upsertUnifiedFeatureScheduleRule(ufsRule)
                }
            }
        }
        
        sendRefreshRequest()
        loadSchedulesAndLimits()
        Toast.makeText(requireContext(), "Rule applied successfully", Toast.LENGTH_SHORT).show()
    }

    // Callbacks from ManageSchedulesScreen
    fun toggleScheduleRuleActive(id: String) {
        viewModel.toggleScheduleRuleActive(id)
        
        when {
            id.startsWith("ufs::") -> {
                val ruleId = id.removePrefix("ufs::")
                val rules = savedPreferencesLoader.loadUnifiedFeatureScheduleRules()
                val idx = rules.indexOfFirst { it.id == ruleId }
                if (idx >= 0) {
                    rules[idx] = rules[idx].copy(isEnabled = !(rules[idx].isEnabled ?: true))
                    savedPreferencesLoader.saveUnifiedFeatureScheduleRules(rules)
                }
            }
            id.startsWith("app_group::") -> {
                val groupId = id.removePrefix("app_group::")
                val rules = savedPreferencesLoader.loadAppBlockerScheduleRules()
                val targetState = !(rules.firstOrNull { it.groupId == groupId }?.isEnabled ?: true)
                rules.forEachIndexed { idx, item ->
                    if (item.groupId == groupId) {
                        rules[idx] = item.copy(isEnabled = targetState)
                    }
                }
                savedPreferencesLoader.saveAppBlockerScheduleRules(rules)
            }
            id.startsWith("limit::") -> {
                // Launch Limits don't have built-in switch toggles in legacy backend, they are deleted
            }
            else -> {
                val rules = savedPreferencesLoader.loadAppBlockerScheduleRules()
                val idx = rules.indexOfFirst { it.id == id }
                if (idx >= 0) {
                    rules[idx] = rules[idx].copy(isEnabled = !(rules[idx].isEnabled ?: true))
                    savedPreferencesLoader.saveAppBlockerScheduleRules(rules)
                }
            }
        }
        sendRefreshRequest()
        loadSchedulesAndLimits()
    }

    fun deleteScheduleRule(id: String) {
        viewModel.deleteScheduleRule(id)
        
        when {
            id.startsWith("ufs::") -> {
                val ruleId = id.removePrefix("ufs::")
                savedPreferencesLoader.removeUnifiedFeatureScheduleRule(ruleId)
            }
            id.startsWith("app_group::") -> {
                val groupId = id.removePrefix("app_group::")
                savedPreferencesLoader.removeAppBlockerScheduleGroup(groupId)
            }
            id.startsWith("limit::") -> {
                val pkg = id.removePrefix("limit::")
                savedPreferencesLoader.removeAppLaunchLimitRule(pkg)
            }
            else -> {
                savedPreferencesLoader.removeAppBlockerScheduleRule(id)
            }
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
        const val FRAGMENT_ID = "manage_block_schedules"
    }
}
