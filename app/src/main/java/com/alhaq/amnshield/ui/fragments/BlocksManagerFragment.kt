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
import com.alhaq.amnshield.data.blockers.AppLaunchLimitRule
import com.alhaq.amnshield.premium.PremiumManager
import com.alhaq.amnshield.services.AmnShieldAccessibilityService
import com.alhaq.amnshield.ui.screens.BlocksManagerScreen
import com.alhaq.amnshield.ui.screens.CreateRuleScreen
import com.alhaq.amnshield.ui.screens.CreateKeywordBlockerRuleScreen
import com.alhaq.amnshield.ui.screens.CreateWebsiteBlockerRuleScreen
import com.alhaq.amnshield.ui.screens.CreateReelsBlockerRuleScreen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

                val defaultScreen = remember(action, prefillTarget) {
                    if (action == "create") {
                        when (prefillTarget) {
                            "KEYWORD_BLOCKER" -> "create_keyword"
                            "WEBSITE_BLOCKER" -> "create_website"
                            "REEL_BLOCKER" -> "create_reels"
                            else -> "create_app"
                        }
                    } else {
                        "manage"
                    }
                }

                var currentScreen by remember(defaultScreen) { mutableStateOf(defaultScreen) } // "manage", "create_app", "create_keyword", "create_website", "create_reels"
                var initialType by remember { mutableStateOf(prefillType ?: "Block Schedule") }
                var editingRule by remember { mutableStateOf<com.alhaq.amnshield.ui.state.ScheduleRule?>(null) }
                var showSelectBlockerDialog by remember { mutableStateOf(false) }

                AmnShieldTheme(appTheme = state.currentTheme) {
                    when (currentScreen) {
                        "manage" -> {
                            BlocksManagerScreen(
                                state = state,
                                viewModel = viewModel,
                                onNavigateToCreateRule = { type ->
                                    editingRule = null
                                    initialType = type
                                    showSelectBlockerDialog = true
                                },
                                onEditRule = { rule ->
                                    editingRule = rule
                                    initialType = rule.restrictionType
                                    val firstBlocker = rule.selectedBlockers.firstOrNull() ?: rule.targetBlockerType
                                    currentScreen = when (firstBlocker) {
                                        "Keyword Blocker" -> "create_keyword"
                                        "Website Blocker" -> "create_website"
                                        "Reels Blocker" -> "create_reels"
                                        else -> "create_app"
                                    }
                                },
                                onToggleRule = { id -> toggleScheduleRuleActive(id) },
                                onDeleteRule = { id -> deleteScheduleRule(id) },
                                onBack = {
                                    if (!parentFragmentManager.popBackStackImmediate()) {
                                        requireActivity().finish()
                                    }
                                }
                            )

                            if (showSelectBlockerDialog) {
                                AlertDialog(
                                    onDismissRequest = { showSelectBlockerDialog = false },
                                    title = { Text("Select Blocker", fontWeight = FontWeight.Bold) },
                                    text = {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("Choose which blocker you want to schedule:")
                                            Spacer(modifier = Modifier.height(8.dp))
                                            val options = listOf("App Blocker", "Keyword Blocker", "Website Blocker", "Reels Blocker")
                                            options.forEach { option ->
                                                TextButton(
                                                    onClick = {
                                                        showSelectBlockerDialog = false
                                                        currentScreen = when (option) {
                                                            "App Blocker" -> "create_app"
                                                            "Keyword Blocker" -> "create_keyword"
                                                            "Website Blocker" -> "create_website"
                                                            "Reels Blocker" -> "create_reels"
                                                            else -> "create_app"
                                                        }
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(option, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = { showSelectBlockerDialog = false }) {
                                            Text("Cancel")
                                        }
                                    }
                                )
                            }
                        }
                        "create_app" -> {
                            CreateRuleScreen(
                                state = state,
                                prefillTarget = prefillTarget ?: "APP_BLOCKER",
                                editingRule = editingRule,
                                onSaveRule = { rule ->
                                    if (editingRule != null) {
                                        deleteScheduleRule(editingRule!!.id)
                                    }
                                    saveScheduleRule(rule)
                                    editingRule = null
                                    if (action == "create") {
                                        if (!parentFragmentManager.popBackStackImmediate()) {
                                            requireActivity().finish()
                                        }
                                    } else {
                                        currentScreen = "manage"
                                    }
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
                        "create_keyword" -> {
                            CreateKeywordBlockerRuleScreen(
                                state = state,
                                viewModel = viewModel,
                                initialType = initialType,
                                editingRule = editingRule,
                                onSaveRule = { rule ->
                                    if (editingRule != null) {
                                        deleteScheduleRule(editingRule!!.id)
                                    }
                                    saveScheduleRule(rule)
                                    editingRule = null
                                    if (action == "create") {
                                        if (!parentFragmentManager.popBackStackImmediate()) {
                                            requireActivity().finish()
                                        }
                                    } else {
                                        currentScreen = "manage"
                                    }
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
                        "create_website" -> {
                            CreateWebsiteBlockerRuleScreen(
                                state = state,
                                viewModel = viewModel,
                                initialType = initialType,
                                editingRule = editingRule,
                                onSaveRule = { rule ->
                                    if (editingRule != null) {
                                        deleteScheduleRule(editingRule!!.id)
                                    }
                                    saveScheduleRule(rule)
                                    editingRule = null
                                    if (action == "create") {
                                        if (!parentFragmentManager.popBackStackImmediate()) {
                                            requireActivity().finish()
                                        }
                                    } else {
                                        currentScreen = "manage"
                                    }
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
                        "create_reels" -> {
                            CreateReelsBlockerRuleScreen(
                                state = state,
                                viewModel = viewModel,
                                initialType = initialType,
                                editingRule = editingRule,
                                onSaveRule = { rule ->
                                    if (editingRule != null) {
                                        deleteScheduleRule(editingRule!!.id)
                                    }
                                    saveScheduleRule(rule)
                                    editingRule = null
                                    if (action == "create") {
                                        if (!parentFragmentManager.popBackStackImmediate()) {
                                            requireActivity().finish()
                                        }
                                    } else {
                                        currentScreen = "manage"
                                    }
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
        val launchLimits = savedPreferencesLoader.loadAppLaunchLimitRules()

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
                    else -> "App Blocker"
                }

                val apps = associatedApps.map { it.packageName }.filter {
                    it != null && it != "keyword_blocker" && it != "website_blocker" && it != "reel_blocker"
                }.distinct()

                val appOrCategory = when (targetBlocker) {
                    "Keyword Blocker" -> "Keywords Blocker"
                    "Website Blocker" -> "Website Blocker"
                    "Reels Blocker" -> "Reels Blocker"
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
                    firstBlock.selectedDays.map { calendarIntToDay(it) }
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
                    firstCheat.selectedDays.map { calendarIntToDay(it) }
                } else {
                    listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                }

                // 3. Usage Limit
                val usageRules = associatedApps.filter { it.type == AppBlockScheduleRule.RuleType.BLOCK && it.durationHours > 0 }
                val isUsageLimitEnabled = usageRules.isNotEmpty()
                val usageLimitHours = if (isUsageLimitEnabled) usageRules.first().durationHours else 0

                // 4. Launch Limit
                val associatedLaunchLimit = launchLimits.firstOrNull { apps.contains(it.packageName) }
                val isLaunchLimitEnabled = associatedLaunchLimit != null
                val launchLimitCount = associatedLaunchLimit?.maxLaunches ?: 0

                // Determine display type/badge
                val restrictionTypeStr = when {
                    isAlwaysBlockEnabled -> "Always Block (24/7)"
                    isScheduleEnabled -> "Block Schedule"
                    isUsageLimitEnabled -> "Usage Limit"
                    isLaunchLimitEnabled -> "Launch Limit"
                    isCheatEnabled -> "Cheat Window"
                    else -> "Block Schedule"
                }

                // Construct periods list for backward compatibility
                val periods = mutableListOf<com.alhaq.amnshield.ui.state.SchedulePeriod>()
                associatedApps.forEach { item ->
                    val start = String.format("%02d:%02d", item.startMinute / 60, item.startMinute % 60)
                    val end = String.format("%02d:%02d", item.endMinute / 60, item.endMinute % 60)
                    val daysList = item.selectedDays?.map { calendarIntToDay(it) } ?: emptyList()
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
                        limitValue = if (isUsageLimitEnabled) usageLimitHours else launchLimitCount,
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
                        usageLimitHours = usageLimitHours,
                        isLaunchLimitEnabled = isLaunchLimitEnabled,
                        launchLimitCount = launchLimitCount
                    )
                )
            }
        }

        // 3. Launch Limits (only load orphans not associated with any group)
        val groupAppPkgs = rulesList.flatMap { it.selectedApps }.toSet()
        launchLimits.filter { !groupAppPkgs.contains(it.packageName) }.forEach { limit ->
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
                    selectedBlockers = listOf("App Blocker"),
                    
                    isLaunchLimitEnabled = true,
                    launchLimitCount = limit.maxLaunches
                )
            )
        }

        val isAdvanced = true

        viewModel.loadState(
            viewModel.state.value.copy(
                scheduleRules = rulesList,
                isAdvancedMode = isAdvanced
            )
        )
    }

    private fun saveScheduleRule(rule: com.alhaq.amnshield.ui.state.ScheduleRule) {
        // Delete any existing rules with the same groupId/id to avoid conflicts
        savedPreferencesLoader.removeAppBlockerScheduleGroup(rule.id)
        savedPreferencesLoader.removeAppBlockerScheduleRule(rule.id)

        // Clean up launch limits for all of the group's packages
        rule.selectedApps?.forEach { appPkg ->
            savedPreferencesLoader.removeAppLaunchLimitRule(appPkg)
        }

        val groupId = rule.id
        val groupTitle = rule.name

        // Determine target packages for database entries
        val targetPackages = when (rule.targetBlockerType) {
            "Keyword Blocker" -> listOf("keyword_blocker")
            "Website Blocker" -> listOf("website_blocker")
            "Reels Blocker" -> listOf("reel_blocker")
            else -> rule.selectedApps ?: emptyList()
        }

        // 1. Save Block Schedule Rules if enabled
        if (rule.isAlwaysBlockEnabled || rule.isScheduleEnabled) {
            val startMin = if (rule.isAlwaysBlockEnabled) 0 else timeToMinutes(rule.startTime)
            val endMin = if (rule.isAlwaysBlockEnabled) 0 else timeToMinutes(rule.endTime)
            val calendarDays = if (rule.isAlwaysBlockEnabled) emptySet() else rule.days.map { dayToCalendarInt(it) }.toSet()
            val recurrence = if (rule.isAlwaysBlockEnabled) {
                AppBlockScheduleRule.Recurrence.ALWAYS
            } else if (rule.days.size == 7) {
                AppBlockScheduleRule.Recurrence.DAILY
            } else {
                AppBlockScheduleRule.Recurrence.WEEKLY
            }

            targetPackages.forEach { pkg ->
                val appRule = AppBlockScheduleRule(
                    id = UUID.randomUUID().toString(),
                    title = if (rule.isAlwaysBlockEnabled) "Block Always • $groupTitle" else "Block • $groupTitle",
                    packageName = pkg,
                    type = AppBlockScheduleRule.RuleType.BLOCK,
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

        // 2. Save Cheat Hours Rules if enabled
        if (rule.isCheatEnabled) {
            val startMin = timeToMinutes(rule.cheatStartTime)
            val endMin = timeToMinutes(rule.cheatEndTime)
            val calendarDays = rule.cheatDays.map { dayToCalendarInt(it) }.toSet()
            val recurrence = if (rule.cheatDays.size == 7) {
                AppBlockScheduleRule.Recurrence.DAILY
            } else {
                AppBlockScheduleRule.Recurrence.WEEKLY
            }

            targetPackages.forEach { pkg ->
                val appRule = AppBlockScheduleRule(
                    id = UUID.randomUUID().toString(),
                    title = "Cheat • $groupTitle",
                    packageName = pkg,
                    type = AppBlockScheduleRule.RuleType.CHEAT,
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

        // 3. Save Usage Limit Rules if App Blocker and enabled
        if (rule.targetBlockerType == "App Blocker" && rule.isUsageLimitEnabled) {
            targetPackages.forEach { pkg ->
                val appRule = AppBlockScheduleRule(
                    id = UUID.randomUUID().toString(),
                    title = "Usage Limit • $groupTitle • ${rule.usageLimitHours}h",
                    packageName = pkg,
                    type = AppBlockScheduleRule.RuleType.BLOCK,
                    recurrence = AppBlockScheduleRule.Recurrence.DAILY,
                    startMinute = 0,
                    endMinute = 0,
                    selectedDays = emptySet(),
                    durationHours = rule.usageLimitHours,
                    createdAt = System.currentTimeMillis(),
                    groupId = groupId,
                    groupTitle = groupTitle,
                    isEnabled = rule.isActive
                )
                savedPreferencesLoader.upsertAppBlockerScheduleRule(appRule)
            }
        }

        // 4. Save Launch Limit Rules if App Blocker and enabled
        if (rule.targetBlockerType == "App Blocker" && rule.isLaunchLimitEnabled) {
            targetPackages.forEach { pkg ->
                val limitRule = AppLaunchLimitRule(
                    id = "limit::$pkg",
                    packageName = pkg,
                    maxLaunches = rule.launchLimitCount,
                    timePeriod = AppLaunchLimitRule.TimePeriod.DAILY,
                    createdAt = System.currentTimeMillis()
                )
                savedPreferencesLoader.addAppLaunchLimitRule(limitRule)
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
            val appSchedules = savedPreferencesLoader.loadAppBlockerScheduleRules()
            val associatedApps = appSchedules.filter { (it.groupId ?: it.id) == id }.map { it.packageName }
            associatedApps.forEach { pkg ->
                savedPreferencesLoader.removeAppLaunchLimitRule(pkg)
            }
            savedPreferencesLoader.removeAppBlockerScheduleGroup(id)
            savedPreferencesLoader.removeAppBlockerScheduleRule(id)
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
