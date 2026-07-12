package com.alhaq.amnshield.utils

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.alhaq.amnshield.ui.fragments.usage.AllAppsUsageFragment
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class UsageStatsHelper(private val context: Context) {

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    private val guardian = UnmatchedCloseEventGuardian()
    fun getForegroundStatsByTimestamps(start: Long, end: Long): List<AllAppsUsageFragment.Stat> {
        // Query usage events from the UsageStatsManager
        val events = usageStatsManager.queryEvents(start, end)
        // Map to track when apps move to the foreground (nullable Long to handle null values)
        val moveToForegroundMap = mutableMapOf<AppClass, Long?>()
        // List to store foreground stats for each app
        val componentForegroundStats = mutableListOf<ComponentForegroundStat>()

        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)

            // Skip if className is null
            val className = event.className ?: continue

            val appClass = AppClass(event.packageName, className)

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED, 4 -> {
                    // App moved to the foreground: store the timestamp
                    moveToForegroundMap[appClass] = event.timeStamp
                }
                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED, 3 -> {
                    // App moved to the background: calculate usage duration
                    var eventBeginTime: Long? = moveToForegroundMap[appClass]
                    if (eventBeginTime != null) {
                        // If there's a start time, set it to null (app is no longer in the foreground)
                        moveToForegroundMap[appClass] = null
                    } else if (moveToForegroundMap.keys.none { event.packageName == it.packageName } &&
                        guardian.test(event, start)) {
                        // If no start time exists and the guardian confirms it's a valid unmatched close event, use the start time
                        eventBeginTime = start
                    } else {
                        // Skip if it's a faulty unmatched close event
                        continue
                    }

                    // Calculate the end time, handling null values
                    val endTime = moveToForegroundMap.entries
                        .filter { event.packageName == it.key.packageName }
                        .filter { it.value != null }
                        .map { it.value!! } // Use non-null assertion here since we filtered out nulls
                        .minOrNull() ?: event.timeStamp

                    // Add the foreground stat
                    addComponentForegroundStat(componentForegroundStats, eventBeginTime, endTime, event.packageName, start, end)
                }
                UsageEvents.Event.DEVICE_SHUTDOWN -> {
                    // Handle device shutdown: treat all open events as closed
                    for (key in moveToForegroundMap.keys) {
                        val startTime = moveToForegroundMap[key]
                        if (startTime == null) continue // Skip if no start time exists

                        addComponentForegroundStat(componentForegroundStats, startTime, event.timeStamp, key.packageName, start, end)

                        // Set all components of the app to null (no longer in the foreground)
                        moveToForegroundMap.keys.filter { key.packageName == it.packageName }.forEach { moveToForegroundMap[it] = null }
                    }
                }
                UsageEvents.Event.DEVICE_STARTUP -> {
                    // Handle device startup: clear all open events
                    for (key in moveToForegroundMap.keys) {
                        moveToForegroundMap[key] = null
                    }
                    // Update the start time to the startup event's timestamp
                    start.coerceAtLeast(event.timeStamp)
                }
            }
        }

        // Handle remaining open events (apps still in the foreground)
        for (key in moveToForegroundMap.keys) {
            val startTime = moveToForegroundMap[key]
            if (startTime == null) continue // Skip if no start time exists

            // UsageEvents does not always emit a matching pause/stop for the
            // currently visible activity before the query end. Close open
            // sessions at the query boundary so "today" stats stay live.
            addComponentForegroundStat(componentForegroundStats, startTime, minOf(System.currentTimeMillis(), end), key.packageName, start, end)
        }

        // Aggregate the foreground stats into usage stats
        return aggregateForegroundStats(componentForegroundStats)
    }
    fun getForegroundStatsByRelativeDay(offset: Int): List<AllAppsUsageFragment.Stat> {
        val queryDay = LocalDate.now().minusDays(offset.toLong())
        val start = queryDay.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = queryDay.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return getForegroundStatsByTimestamps(start, end)
    }

    fun getForegroundStatsByDay(queryDate: LocalDate): List<AllAppsUsageFragment.Stat> {
        val start = queryDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = queryDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return getForegroundStatsByTimestamps(start, end)
    }

    private fun addComponentForegroundStat(
        foregroundStats: MutableList<ComponentForegroundStat>,
        beginTime: Long,
        endTime: Long,
        packageName: String,
        queryStart: Long,
        queryEnd: Long
    ) {
        val clampedBegin = beginTime.coerceIn(queryStart, queryEnd)
        val clampedEnd = endTime.coerceIn(queryStart, queryEnd)
        if (clampedEnd <= clampedBegin) return
        foregroundStats.add(ComponentForegroundStat(clampedBegin, clampedEnd, packageName))
    }

    private fun aggregateForegroundStats(foregroundStats: List<ComponentForegroundStat>): List<AllAppsUsageFragment.Stat> {
        val usageStats = mutableListOf<AllAppsUsageFragment.Stat>()
        if (foregroundStats.isEmpty()) return usageStats

        // Map to store total foreground time for each app
        val applicationTotalForegroundTime = mutableMapOf<String, Long>()
        val applicationSessions = mutableMapOf<String, MutableList<AllAppsUsageFragment.UsageSession>>()

        for (foregroundStat in foregroundStats) {
            val durationMillis = foregroundStat.endTime - foregroundStat.beginTime
            if (durationMillis <= 0) continue

            applicationTotalForegroundTime[foregroundStat.packageName] =
                applicationTotalForegroundTime.getOrDefault(foregroundStat.packageName, 0) +
                        durationMillis

            val startTime = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(foregroundStat.beginTime),
                ZoneId.systemDefault()
            )
            applicationSessions.getOrPut(foregroundStat.packageName) { mutableListOf() }.add(
                AllAppsUsageFragment.UsageSession(startTime, durationMillis)
            )
        }

        // Create Stat objects with total time and start times
        for ((packageName, totalTime) in applicationTotalForegroundTime) {
            val sessions = applicationSessions[packageName].orEmpty()
            val sortedSessions = sessions.sortedBy { it.startTime.toInstant() }
            usageStats.add(
                AllAppsUsageFragment.Stat(
                    packageName = packageName,
                    totalTime = totalTime,
                    sessions = sortedSessions
                )
            )
        }

        // Sort by total time in descending order
        return usageStats.sortedByDescending { it.totalTime }
    }

    private class AppClass(val packageName: String, val className: String) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as AppClass
            if (packageName != other.packageName) return false
            if (className != other.className) return false
            return true
        }

        override fun hashCode(): Int {
            var result = packageName.hashCode()
            result = 31 * result + className.hashCode()
            return result
        }
    }

    private class ComponentForegroundStat(val beginTime: Long, val endTime: Long, val packageName: String)

    private class UnmatchedCloseEventGuardian {
        fun test(event: UsageEvents.Event, start: Long): Boolean {
            // Implement logic to test for unmatched close events
            return true
        }
    }

    fun getDefaultLauncherPackageName(context: Context): String? {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName
    }
}
