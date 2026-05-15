package com.alhaq.deenshield.blockers

import android.os.SystemClock
import com.alhaq.deenshield.data.blockers.AppBlockScheduleRule
import com.alhaq.deenshield.data.blockers.AppLaunchLimitRule
import com.alhaq.deenshield.ui.activity.TimedActionActivity
import com.alhaq.deenshield.utils.SavedPreferencesLoader
import com.alhaq.deenshield.utils.TimeTools
import java.util.Calendar

class AppBlocker:BaseBlocker() {

    // package-name -> end-time-in-millis
    private var cooldownAppsList:MutableMap<String,Long> = mutableMapOf()

    // package-name -> [(start-time, end-time), ...]
    private var cheatHours: MutableMap<String, List<Pair<Int, Int>>> = mutableMapOf()

    private var scheduleRules: List<AppBlockScheduleRule> = emptyList()

    var blockedApps = hashSetOf<String>()
    var cheatMinuteStartTime = -1
    var cheatMinutesEndTime = -1

    /**
     * Check if app needs to be blocked
     *
     * @param packageName
     * @param savedPrefs Optional SavedPreferencesLoader for launch limit checking
     * @return
     */
    fun doesAppNeedToBeBlocked(packageName: String, savedPrefs: SavedPreferencesLoader? = null): AppBlockerResult {

        // Never block DeenShield itself
        if (packageName.equals("com.alhaq.deenshield", ignoreCase = true)) {
            return AppBlockerResult(isBlocked = false)
        }

        // Never block core system surfaces to avoid launcher/system soft-lock behavior
        if (packageName.equals("com.android.systemui", ignoreCase = true) ||
            packageName.equals("android", ignoreCase = true)) {
            return AppBlockerResult(isBlocked = false)
        }

        // Check launch limit first - if app exceeded launch limit, block it
        if (savedPrefs != null) {
            val launchLimitRule = savedPrefs.getAppLaunchLimitRule(packageName)
            if (launchLimitRule != null) {
                val currentCount = savedPrefs.getCurrentLaunchCount(packageName)
                if (currentCount >= launchLimitRule.maxLaunches) {
                    return AppBlockerResult(isBlocked = true)
                }
            }
        }

        if(cooldownAppsList.containsKey(packageName)){
            val now = System.currentTimeMillis()
            // check if app has surpassed the cooldown period
            if (cooldownAppsList[packageName]!! <= now){
                removeCooldownFrom(packageName)
                return AppBlockerResult(isBlocked = true)
            }

            // app is still under cooldown
            return AppBlockerResult(
                isBlocked = false,
                cooldownEndTime = cooldownAppsList[packageName]!!
            )
        }

        val packageRules = scheduleRules.filter { it.packageName == packageName }
        if (packageRules.isNotEmpty()) {
            val activeCheatEnd = getActiveRuleEndTime(
                packageRules.filter { it.type == AppBlockScheduleRule.RuleType.CHEAT }
            )
            if (activeCheatEnd != null) {
                return AppBlockerResult(isBlocked = false, cheatHoursEndTime = activeCheatEnd)
            }
        }

        // check if app is under cheat-hours
        val endCheatMillis = getEndTimeInMillis(packageName)
        if (endCheatMillis != null) {
            return AppBlockerResult(isBlocked = false, cheatHoursEndTime = endCheatMillis)
        }

        if (blockedApps.contains(packageName)) {
            if (packageRules.isNotEmpty()) {
                val blockRules = packageRules.filter { it.type == AppBlockScheduleRule.RuleType.BLOCK }
                // If app has explicit block schedules, block only during active schedule windows.
                if (blockRules.isNotEmpty()) {
                    val activeBlockEnd = getActiveRuleEndTime(blockRules)
                    return AppBlockerResult(isBlocked = activeBlockEnd != null)
                }
            }
            return AppBlockerResult(
                isBlocked = true
            )
        }
        return AppBlockerResult(isBlocked = false)
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

    /**
     * Check if the package is currently under cheat hours.
     *
     * @param packageName The app package name.
     * @return Returns null if the app is not under cheat hours, or the timestamp (uptimeMillis) when it ends.
     */
    private fun getEndTimeInMillis(packageName: String): Long? {
        if (cheatHours[packageName] == null) return null

        val currentTime = Calendar.getInstance()
        val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
        val currentMinute = currentTime.get(Calendar.MINUTE)

        val currentMinutes = TimeTools.convertToMinutesFromMidnight(currentHour, currentMinute)
        val uptimeNow = SystemClock.uptimeMillis()

        cheatHours[packageName]?.forEach { (startMinutes, endMinutes) ->
            if ((startMinutes <= endMinutes && currentMinutes in startMinutes until endMinutes) ||
                (startMinutes > endMinutes && (currentMinutes >= startMinutes || currentMinutes < endMinutes))
            ) {
                var dayOffsetMinutes = 0

                // if cheat hours cross midnight and it is still the first day treat the end time as tomorrow
                if (startMinutes > endMinutes && currentMinutes > endMinutes) {
                    dayOffsetMinutes = 1440
                }

                // Convert endMinutes to uptimeMillis
                val diffMinutes = endMinutes + dayOffsetMinutes - currentMinutes

                val endTimeMillis = uptimeNow + (diffMinutes * 60 * 1000)

                return endTimeMillis
            }
        }
        return null
    }


    fun refreshCheatHoursData(cheatList: List<TimedActionActivity.AutoTimedActionItem>) {
        cheatHours.clear()
        cheatList.forEach { item ->
            val startTime = item.startTimeInMins
            val endTime = item.endTimeInMins
            val packageNames: ArrayList<String> = item.packages

            packageNames.forEach { packageName ->

                if (cheatHours.containsKey(packageName)) {
                    val cheatHourTimeData = cheatHours[packageName].orEmpty()
                    val cheatHourNewTimeData: MutableList<Pair<Int, Int>> =
                        cheatHourTimeData.toMutableList()

                    cheatHourNewTimeData.add(Pair(startTime, endTime))
                    cheatHours[packageName] = cheatHourNewTimeData
                } else {
                    cheatHours[packageName] = listOf(Pair(startTime, endTime))
                }
            }
        }

    }

    fun refreshScheduleRules(rules: List<AppBlockScheduleRule>) {
        scheduleRules = rules
    }

    private fun getActiveRuleEndTime(rules: List<AppBlockScheduleRule>): Long? {
        if (rules.isEmpty()) return null

        val nowMillis = System.currentTimeMillis()
        val now = Calendar.getInstance()
        val currentMinutes = TimeTools.convertToMinutesFromMidnight(
            now.get(Calendar.HOUR_OF_DAY),
            now.get(Calendar.MINUTE)
        )
        val today = now.get(Calendar.DAY_OF_WEEK)
        val yesterday = if (today == Calendar.SUNDAY) Calendar.SATURDAY else today - 1

        var earliestEnd: Long? = null

        rules.forEach { rule ->
            val candidateEnd = when (rule.recurrence) {
                AppBlockScheduleRule.Recurrence.ALWAYS -> nowMillis + (24L * 60L * 60L * 1000L)

                AppBlockScheduleRule.Recurrence.HOURLY -> {
                    if (rule.activeUntilMillis > nowMillis) rule.activeUntilMillis else null
                }

                AppBlockScheduleRule.Recurrence.DAILY -> {
                    evaluateDailyLikeRuleEnd(now, currentMinutes, rule.startMinute, rule.endMinute)
                }

                AppBlockScheduleRule.Recurrence.WEEKLY -> {
                    if (rule.selectedDays.isEmpty()) {
                        null
                    } else {
                        evaluateWeeklyRuleEnd(
                            now,
                            currentMinutes,
                            today,
                            yesterday,
                            rule.startMinute,
                            rule.endMinute,
                            rule.selectedDays
                        )
                    }
                }
            }

            if (candidateEnd != null) {
                earliestEnd = if (earliestEnd == null) candidateEnd else minOf(earliestEnd, candidateEnd)
            }
        }

        return earliestEnd
    }

    private fun evaluateDailyLikeRuleEnd(
        now: Calendar,
        currentMinutes: Int,
        startMinutes: Int,
        endMinutes: Int
    ): Long? {
        val nowMillis = System.currentTimeMillis()
        if (startMinutes == endMinutes) return null

        val isActive = if (startMinutes < endMinutes) {
            currentMinutes in startMinutes until endMinutes
        } else {
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        }
        if (!isActive) return null

        val endCal = now.clone() as Calendar
        if (startMinutes > endMinutes && currentMinutes >= startMinutes) {
            endCal.add(Calendar.DATE, 1)
        }
        endCal.set(Calendar.HOUR_OF_DAY, endMinutes / 60)
        endCal.set(Calendar.MINUTE, endMinutes % 60)
        endCal.set(Calendar.SECOND, 0)
        endCal.set(Calendar.MILLISECOND, 0)

        return endCal.timeInMillis.coerceAtLeast(nowMillis + 60_000L)
    }

    private fun evaluateWeeklyRuleEnd(
        now: Calendar,
        currentMinutes: Int,
        today: Int,
        yesterday: Int,
        startMinutes: Int,
        endMinutes: Int,
        selectedDays: Set<Int>
    ): Long? {
        if (startMinutes == endMinutes) return null

        val active = if (startMinutes < endMinutes) {
            selectedDays.contains(today) && currentMinutes in startMinutes until endMinutes
        } else {
            (selectedDays.contains(today) && currentMinutes >= startMinutes) ||
                (selectedDays.contains(yesterday) && currentMinutes < endMinutes)
        }
        if (!active) return null

        val endCal = now.clone() as Calendar
        if (startMinutes > endMinutes && selectedDays.contains(today) && currentMinutes >= startMinutes) {
            endCal.add(Calendar.DATE, 1)
        }
        endCal.set(Calendar.HOUR_OF_DAY, endMinutes / 60)
        endCal.set(Calendar.MINUTE, endMinutes % 60)
        endCal.set(Calendar.SECOND, 0)
        endCal.set(Calendar.MILLISECOND, 0)
        return endCal.timeInMillis
    }

    /**
     * App blocker check result
     *
     * @property isBlocked
     * @property cheatHoursEndTime specifies when cheat-hour ends. returns -1 if not in cheat-hour
     * @property cooldownEndTime specifies when cooldown ends. returns -1 if not in cooldown
     */
    data class AppBlockerResult(
        val isBlocked: Boolean,
        val cheatHoursEndTime: Long = -1L,
        val cooldownEndTime: Long = -1L
    )

}