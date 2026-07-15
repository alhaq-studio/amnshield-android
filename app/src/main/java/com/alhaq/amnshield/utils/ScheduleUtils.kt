package com.alhaq.amnshield.utils

import android.content.Context
import com.alhaq.amnshield.data.blockers.AppBlockScheduleRule
import com.alhaq.amnshield.data.blockers.UnifiedFeatureScheduleRule
import com.alhaq.amnshield.ui.state.SchedulePeriod
import java.util.Calendar
import java.util.UUID

object ScheduleUtils {

    fun isDailyWindowActive(startMinutes: Int, endMinutes: Int, nowMillis: Long): Boolean {
        if (startMinutes == endMinutes) return false
        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        return if (startMinutes < endMinutes) {
            currentMinutes in startMinutes until endMinutes
        } else {
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        }
    }

    fun isWeeklyWindowActive(
        startMinutes: Int,
        endMinutes: Int,
        selectedDays: Set<Int>,
        nowMillis: Long
    ): Boolean {
        if (startMinutes == endMinutes || selectedDays.isEmpty()) return false

        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val today = now.get(Calendar.DAY_OF_WEEK)
        val yesterday = if (today == Calendar.SUNDAY) Calendar.SATURDAY else today - 1

        return if (startMinutes < endMinutes) {
            selectedDays.contains(today) && currentMinutes in startMinutes until endMinutes
        } else {
            (selectedDays.contains(today) && currentMinutes >= startMinutes) ||
                (selectedDays.contains(yesterday) && currentMinutes < endMinutes)
        }
    }

    fun getDailyWindowEndTime(startMinutes: Int, endMinutes: Int, nowMillis: Long): Long? {
        if (!isDailyWindowActive(startMinutes, endMinutes, nowMillis)) return null

        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

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

    fun getWeeklyWindowEndTime(
        startMinutes: Int,
        endMinutes: Int,
        selectedDays: Set<Int>,
        nowMillis: Long
    ): Long? {
        if (!isWeeklyWindowActive(startMinutes, endMinutes, selectedDays, nowMillis)) return null

        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val today = now.get(Calendar.DAY_OF_WEEK)

        val endCal = now.clone() as Calendar
        if (startMinutes > endMinutes && selectedDays.contains(today) && currentMinutes >= startMinutes) {
            endCal.add(Calendar.DATE, 1)
        }
        endCal.set(Calendar.HOUR_OF_DAY, endMinutes / 60)
        endCal.set(Calendar.MINUTE, endMinutes % 60)
        endCal.set(Calendar.SECOND, 0)
        endCal.set(Calendar.MILLISECOND, 0)

        return endCal.timeInMillis.coerceAtLeast(nowMillis + 60_000L)
    }

    fun timeToMinutes(timeStr: String): Int {
        val parts = timeStr.split(":")
        if (parts.size != 2) return 0
        val hours = parts[0].toIntOrNull() ?: 0
        val minutes = parts[1].toIntOrNull() ?: 0
        return hours * 60 + minutes
    }

    fun dayToCalendarInt(day: String): Int {
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

    fun calendarIntToDay(dayInt: Int): String {
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

    fun mergeSchedules(periods: List<SchedulePeriod>): List<SchedulePeriod> {
        if (periods.size <= 1) return periods

        val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val dayIntervals = daysOfWeek.associateWith { mutableListOf<Pair<Int, Int>>() }.toMutableMap()

        for (p in periods) {
            val s = timeToMinutes(p.startTime)
            val e = timeToMinutes(p.endTime)
            val intervals = if (e >= s) {
                listOf(Pair(s, e))
            } else {
                listOf(Pair(s, 1440), Pair(0, e))
            }
            for (day in p.days) {
                dayIntervals[day]?.addAll(intervals)
            }
        }

        val mergedDayIntervals = mutableMapOf<String, List<Pair<Int, Int>>>()
        for ((day, intervals) in dayIntervals) {
            if (intervals.isEmpty()) continue
            intervals.sortBy { it.first }
            val merged = mutableListOf<Pair<Int, Int>>()
            var current = intervals[0]
            for (i in 1 until intervals.size) {
                val next = intervals[i]
                if (next.first <= current.second) {
                    current = Pair(current.first, maxOf(current.second, next.second))
                } else {
                    merged.add(current)
                    current = next
                }
            }
            merged.add(current)
            mergedDayIntervals[day] = merged
        }

        val intervalToDays = mutableMapOf<Pair<Int, Int>, MutableList<String>>()
        for ((day, intervals) in mergedDayIntervals) {
            for (interval in intervals) {
                intervalToDays.getOrPut(interval) { mutableListOf() }.add(day)
            }
        }

        val result = mutableListOf<SchedulePeriod>()
        for ((interval, days) in intervalToDays) {
            val startStr = String.format("%02d:%02d", interval.first / 60, interval.first % 60)
            val endStr = if (interval.second >= 1440) "23:59" else String.format("%02d:%02d", interval.second / 60, interval.second % 60)
            result.add(SchedulePeriod(startStr, endStr, days))
        }

        return result
    }

    fun autoResolveAndSaveFeatureSchedule(
        context: Context,
        loader: SavedPreferencesLoader,
        featureName: String,
        newStartTimeStr: String,
        newEndTimeStr: String,
        newDaysList: List<String>
    ) {
        val target = when (featureName) {
            "App Blocker" -> UnifiedFeatureScheduleRule.FeatureTarget.APP_BLOCKER
            "Keyword Blocker" -> UnifiedFeatureScheduleRule.FeatureTarget.KEYWORD_BLOCKER
            "Website Blocker" -> UnifiedFeatureScheduleRule.FeatureTarget.WEBSITE_BLOCKER
            "Reels Blocker" -> UnifiedFeatureScheduleRule.FeatureTarget.REEL_BLOCKER
            else -> null
        } ?: return

        val existingPeriods = mutableListOf<SchedulePeriod>()
        val appRulesToDelete = mutableListOf<String>()
        val ufsRulesToDelete = mutableListOf<String>()

        val ufsRules = loader.loadUnifiedFeatureScheduleRules().filter { it.isEnabled == true }
        ufsRules.forEach { ufsRule ->
            if (ufsRule.targets.contains(target)) {
                val daysList = ufsRule.selectedDays.map { calendarIntToDay(it) }
                val startStr = String.format("%02d:%02d", ufsRule.startMinute / 60, ufsRule.startMinute % 60)
                val endStr = String.format("%02d:%02d", ufsRule.endMinute / 60, ufsRule.endMinute % 60)
                existingPeriods.add(SchedulePeriod(startStr, endStr, daysList))

                ufsRulesToDelete.add(ufsRule.id)
                ufsRule.groupId?.let { ufsRulesToDelete.add(it) }
            }
        }

        if (target == UnifiedFeatureScheduleRule.FeatureTarget.APP_BLOCKER) {
            val appRules = loader.loadAppBlockerScheduleRules().filter { it.isEnabled == true }
            appRules.forEach { appRule ->
                val daysList = appRule.selectedDays.map { calendarIntToDay(it) }
                val startStr = String.format("%02d:%02d", appRule.startMinute / 60, appRule.startMinute % 60)
                val endStr = String.format("%02d:%02d", appRule.endMinute / 60, appRule.endMinute % 60)
                existingPeriods.add(SchedulePeriod(startStr, endStr, daysList))

                appRulesToDelete.add(appRule.id)
                appRule.groupId?.let { appRulesToDelete.add(it) }
            }
        }

        existingPeriods.add(SchedulePeriod(newStartTimeStr, newEndTimeStr, newDaysList))
        val mergedPeriods = mergeSchedules(existingPeriods)

        ufsRulesToDelete.distinct().forEach { id ->
            loader.removeUnifiedFeatureScheduleGroup(id)
            loader.removeUnifiedFeatureScheduleRule(id)
        }
        appRulesToDelete.distinct().forEach { id ->
            loader.removeAppBlockerScheduleGroup(id)
            loader.removeAppBlockerScheduleRule(id)
        }

        mergedPeriods.forEach { period ->
            val startMin = timeToMinutes(period.startTime)
            val endMin = timeToMinutes(period.endTime)
            val calendarDays = period.days.map { dayToCalendarInt(it) }.toSet()
            val ruleId = UUID.randomUUID().toString()
            val ruleTitle = "Config • $featureName Schedule"

            val ufsRecurrence = if (period.days.size == 7) {
                UnifiedFeatureScheduleRule.Recurrence.DAILY
            } else {
                UnifiedFeatureScheduleRule.Recurrence.WEEKLY
            }

            val ufsRule = UnifiedFeatureScheduleRule(
                id = ruleId,
                title = ruleTitle,
                type = UnifiedFeatureScheduleRule.RuleType.BLOCK,
                recurrence = ufsRecurrence,
                targets = setOf(target),
                startMinute = startMin,
                endMinute = endMin,
                selectedDays = calendarDays,
                createdAt = System.currentTimeMillis(),
                groupId = ruleId,
                groupTitle = ruleTitle,
                isEnabled = true
            )
            loader.upsertUnifiedFeatureScheduleRule(ufsRule)

            if (target == UnifiedFeatureScheduleRule.FeatureTarget.APP_BLOCKER) {
                val blockedApps = loader.loadBlockedApps()
                val appRecurrence = if (period.days.size == 7) {
                    AppBlockScheduleRule.Recurrence.DAILY
                } else {
                    AppBlockScheduleRule.Recurrence.WEEKLY
                }
                blockedApps.forEach { appPkg ->
                    val appName = try {
                        context.packageManager.getApplicationLabel(
                            context.packageManager.getApplicationInfo(appPkg, 0)
                        ).toString()
                    } catch (_: Exception) {
                        appPkg
                    }
                    val appRule = AppBlockScheduleRule(
                        id = UUID.randomUUID().toString(),
                        title = "Config • $appName • BLOCK",
                        packageName = appPkg,
                        type = AppBlockScheduleRule.RuleType.BLOCK,
                        recurrence = appRecurrence,
                        startMinute = startMin,
                        endMinute = endMin,
                        selectedDays = calendarDays,
                        createdAt = System.currentTimeMillis(),
                        groupId = ruleId,
                        groupTitle = ruleTitle,
                        isEnabled = true
                    )
                    loader.upsertAppBlockerScheduleRule(appRule)
                }
            }
        }

        when (featureName) {
            "App Blocker" -> loader.setAppBlockerFeatureEnabled(true, updateManual = true)
            "Keyword Blocker" -> loader.setKeywordBlockerFeatureEnabled(true, updateManual = true)
            "Website Blocker" -> loader.setWebsiteBlockerEnabled(true, updateManual = true)
            "Reels Blocker" -> loader.setReelBlockerEnabled(true, updateManual = true)
        }
    }

    fun disableFeatureSchedules(
        loader: SavedPreferencesLoader,
        featureName: String
    ) {
        when (featureName) {
            "App Blocker" -> {
                loader.setAppBlockerFeatureEnabled(false, updateManual = true)
                val appRules = loader.loadAppBlockerScheduleRules()
                appRules.forEachIndexed { index, rule ->
                    appRules[index] = rule.copy(isEnabled = false)
                }
                loader.saveAppBlockerScheduleRules(appRules)
            }
            "Keyword Blocker" -> {
                loader.setKeywordBlockerFeatureEnabled(false, updateManual = true)
            }
            "Website Blocker" -> {
                loader.setWebsiteBlockerEnabled(false, updateManual = true)
            }
            "Reels Blocker" -> {
                loader.setReelBlockerEnabled(false, updateManual = true)
            }
        }

        val target = when (featureName) {
            "App Blocker" -> UnifiedFeatureScheduleRule.FeatureTarget.APP_BLOCKER
            "Keyword Blocker" -> UnifiedFeatureScheduleRule.FeatureTarget.KEYWORD_BLOCKER
            "Website Blocker" -> UnifiedFeatureScheduleRule.FeatureTarget.WEBSITE_BLOCKER
            "Reels Blocker" -> UnifiedFeatureScheduleRule.FeatureTarget.REEL_BLOCKER
            else -> null
        }
        if (target != null) {
            val featRules = loader.loadUnifiedFeatureScheduleRules()
            var modified = false
            featRules.forEachIndexed { index, rule ->
                if (rule.targets.contains(target) && rule.isEnabled == true) {
                    featRules[index] = rule.copy(isEnabled = false)
                    modified = true
                }
            }
            if (modified) {
                loader.saveUnifiedFeatureScheduleRules(featRules)
            }
        }
    }
}

