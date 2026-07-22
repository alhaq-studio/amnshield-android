package com.alhaq.amnshield.blockers

import android.os.SystemClock
import com.alhaq.amnshield.data.blockers.AppBlockScheduleRule
import com.alhaq.amnshield.utils.SavedPreferencesLoader
import com.alhaq.amnshield.utils.ScheduleUtils
import com.alhaq.amnshield.utils.TimeTools
import java.util.Calendar

class AppBlocker : BaseBlocker() {

    // package-name -> end-time-in-millis (grace period / temporary bypass)
    private var cooldownAppsList: MutableMap<String, Long> = mutableMapOf()

    companion object {
        val ESSENTIAL_SYSTEM_APPS = setOf(
            "com.android.settings",
            "com.google.android.settings",
            "com.sec.android.app.launcher",
            "com.samsung.android.settings",
            "com.coloros.settings",
            "com.android.systemui",
            "com.google.android.apps.nexuslauncher",
            "com.android.launcher",
            "com.android.launcher3",
            "com.huawei.android.launcher",
            "com.miui.mihome2",
            "com.mi.android.globallauncher",
            "com.android.dialer",
            "com.android.phone",
            "com.android.contacts",
            "com.google.android.dialer",
            "com.google.android.contacts",
            "com.samsung.android.dialer",
            "com.samsung.android.contacts",
            "com.alhaq.amnshield",
            "com.alhaq.deenshield",
        )
    }

    private var scheduleRules: List<AppBlockScheduleRule> = emptyList()

    var blockedApps = hashSetOf<String>()

    /**
     * Check if app needs to be blocked
     *
     * @param packageName
     * @param savedPrefs Optional SavedPreferencesLoader for launch limit checking
     * @return
     */
    fun doesAppNeedToBeBlocked(packageName: String, savedPrefs: SavedPreferencesLoader? = null): AppBlockerResult {

        // 1. Core exclusions (Never block essential system apps, launchers, system UI)
        if (ESSENTIAL_SYSTEM_APPS.contains(packageName) ||
            packageName.equals("com.alhaq.amnshield", ignoreCase = true) ||
            packageName.equals("com.alhaq.deenshield", ignoreCase = true) ||
            packageName.startsWith("com.alhaq.deenshield.", ignoreCase = true) ||
            packageName.equals("com.android.systemui", ignoreCase = true) ||
            packageName.equals("android", ignoreCase = true)
        ) {
            return AppBlockerResult(isBlocked = false)
        }

        val packageRules = scheduleRules.filter { it.packageName == packageName && it.isRuleEnabled }
        val hasLaunchLimit = savedPrefs?.getAppLaunchLimitRule(packageName) != null
        val hasUsageLimit = packageRules.any { it.type == AppBlockScheduleRule.RuleType.BLOCK && it.durationHours > 0 }

        if (packageRules.isEmpty() && !hasLaunchLimit && !hasUsageLimit) {
            return AppBlockerResult(isBlocked = false)
        }

        // 2. Check for active CHEAT rules (highest priority bypass)
        val activeCheatEnd = getActiveRuleEndTime(
            packageRules.filter { it.type == AppBlockScheduleRule.RuleType.CHEAT }
        )
        if (activeCheatEnd != null) {
            return AppBlockerResult(isBlocked = false, cheatHoursEndTime = activeCheatEnd)
        }

        // 3. Check for Cooldown/Bypass (grace period after warning screen "Proceed")
        if (cooldownAppsList.containsKey(packageName)) {
            val now = System.currentTimeMillis()
            val bypassEnd = cooldownAppsList[packageName]!!
            if (now < bypassEnd) {
                return AppBlockerResult(isBlocked = false, cooldownEndTime = bypassEnd)
            } else {
                removeCooldownFrom(packageName)
            }
        }

        // 4. Check if the app should be blocked
        var shouldBeBlocked = false

        // A) Launch Limit check
        if (savedPrefs != null) {
            val launchLimitRule = savedPrefs.getAppLaunchLimitRule(packageName)
            if (launchLimitRule != null) {
                val currentCount = savedPrefs.getCurrentLaunchCount(packageName, launchLimitRule)
                if (currentCount >= launchLimitRule.maxLaunches) {
                    shouldBeBlocked = true
                }
            }
        }

        // B) Usage Limit check
        val usageLimitRules = packageRules.filter { it.type == AppBlockScheduleRule.RuleType.BLOCK && it.durationHours > 0 }
        if (usageLimitRules.isNotEmpty() && savedPrefs != null) {
            val dailyUsageMs = getDailyAppUsageMillis(packageName, savedPrefs.context)
            val maxAllowedMs = usageLimitRules.maxOf { it.durationHours } * 60L * 60L * 1000L
            if (dailyUsageMs >= maxAllowedMs) {
                shouldBeBlocked = true
            }
        }

        // C) Block Schedules check (Always Block 24/7 or Time Window schedules)
        val blockScheduleRules = packageRules.filter { it.type == AppBlockScheduleRule.RuleType.BLOCK && it.durationHours <= 0 }
        if (blockScheduleRules.isNotEmpty()) {
            val activeBlockEnd = getActiveRuleEndTime(blockScheduleRules)
            if (activeBlockEnd != null) {
                shouldBeBlocked = true
            }
        }

        return AppBlockerResult(isBlocked = shouldBeBlocked)
    }

    private fun getDailyAppUsageMillis(packageName: String, context: android.content.Context): Long {
        val usageStatsManager = context.getSystemService(android.content.Context.USAGE_STATS_SERVICE) as? android.app.usage.UsageStatsManager ?: return 0L
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()
        
        val stats = usageStatsManager.queryUsageStats(
            android.app.usage.UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )
        if (stats.isNullOrEmpty()) return 0L
        
        val packageStat = stats.firstOrNull { it.packageName == packageName }
        return packageStat?.totalTimeInForeground ?: 0L
    }

    fun putCooldownTo(packageName: String, endTime: Long) {
        cooldownAppsList[packageName] = endTime
    }

    fun removeCooldownFrom(packageName: String) {
        cooldownAppsList.remove(packageName)
    }

    fun restoreCooldowns(cooldowns: Map<String, Long>) {
        cooldownAppsList.clear()
        val now = System.currentTimeMillis()
        cooldownAppsList.putAll(cooldowns.filterValues { it > now })
    }

    fun getCooldownSnapshot(): Map<String, Long> {
        return HashMap(cooldownAppsList)
    }



    fun refreshScheduleRules(rules: List<AppBlockScheduleRule>) {
        scheduleRules = rules
    }

    private fun getActiveRuleEndTime(rules: List<AppBlockScheduleRule>): Long? {
        if (rules.isEmpty()) return null

        val nowMillis = System.currentTimeMillis()
        var latestEnd: Long? = null

        rules.forEach { rule ->
            val recurrence = rule.recurrence ?: AppBlockScheduleRule.Recurrence.DAILY
            val candidateEnd = when (recurrence) {
                AppBlockScheduleRule.Recurrence.ALWAYS -> nowMillis + (24L * 60L * 60L * 1000L)
                AppBlockScheduleRule.Recurrence.HOURLY -> {
                    if (rule.activeUntilMillis > nowMillis) rule.activeUntilMillis else null
                }
                AppBlockScheduleRule.Recurrence.DAILY -> {
                    ScheduleUtils.getDailyWindowEndTime(rule.startMinute, rule.endMinute, nowMillis)
                }
                AppBlockScheduleRule.Recurrence.WEEKLY -> {
                    ScheduleUtils.getWeeklyWindowEndTime(rule.startMinute, rule.endMinute, rule.selectedDays ?: emptySet(), nowMillis)
                }
            }

            if (candidateEnd != null) {
                latestEnd = if (latestEnd == null) candidateEnd else maxOf(latestEnd, candidateEnd)
            }
        }

        return latestEnd
    }

    data class AppBlockerResult(
        val isBlocked: Boolean,
        val cheatHoursEndTime: Long = -1L,
        val cooldownEndTime: Long = -1L
    )
}
