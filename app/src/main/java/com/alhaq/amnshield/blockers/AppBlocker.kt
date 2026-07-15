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

        // 1. Core exclusions (Never block)
        if (packageName.equals("com.alhaq.amnshield", ignoreCase = true) ||
            packageName.equals("com.android.systemui", ignoreCase = true) ||
            packageName.equals("android", ignoreCase = true)
        ) {
            return AppBlockerResult(isBlocked = false)
        }

        val packageRules = scheduleRules.filter { it.packageName == packageName && it.isRuleEnabled }

        // 2. Check for active CHEAT rules (highest priority bypass)
        val activeCheatEnd = getActiveRuleEndTime(
            packageRules.filter { it.type == AppBlockScheduleRule.RuleType.CHEAT }
        )
        if (activeCheatEnd != null) {
            return AppBlockerResult(isBlocked = false, cheatHoursEndTime = activeCheatEnd)
        }

        // 4. Check for Cooldown/Bypass (grace period)
        if (cooldownAppsList.containsKey(packageName)) {
            val now = System.currentTimeMillis()
            val bypassEnd = cooldownAppsList[packageName]!!
            if (now < bypassEnd) {
                return AppBlockerResult(isBlocked = false, cooldownEndTime = bypassEnd)
            } else {
                removeCooldownFrom(packageName)
                // Period expired, proceed to check if it should be blocked
            }
        }

        // 5. Check if the app is scheduled or manually blocked
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

        // B) Block Schedules check
        val blockRules = packageRules.filter { it.type == AppBlockScheduleRule.RuleType.BLOCK }
        if (blockRules.isNotEmpty()) {
            val activeBlockEnd = getActiveRuleEndTime(blockRules)
            if (activeBlockEnd != null) {
                shouldBeBlocked = true
            }
        }

        // C) Manual Block List check
        if (blockedApps.contains(packageName)) {
            // If there are BLOCK rules defined for this app, they override the manual list.
            // If none are active, we don't block. If there are NO rules, we block always.
            if (blockRules.isEmpty()) {
                shouldBeBlocked = true
            }
        }

        return AppBlockerResult(isBlocked = shouldBeBlocked)
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
            val candidateEnd = when (rule.recurrence) {
                AppBlockScheduleRule.Recurrence.ALWAYS -> nowMillis + (24L * 60L * 60L * 1000L)
                AppBlockScheduleRule.Recurrence.HOURLY -> {
                    if (rule.activeUntilMillis > nowMillis) rule.activeUntilMillis else null
                }
                AppBlockScheduleRule.Recurrence.DAILY -> {
                    ScheduleUtils.getDailyWindowEndTime(rule.startMinute, rule.endMinute, nowMillis)
                }
                AppBlockScheduleRule.Recurrence.WEEKLY -> {
                    ScheduleUtils.getWeeklyWindowEndTime(rule.startMinute, rule.endMinute, rule.selectedDays, nowMillis)
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
