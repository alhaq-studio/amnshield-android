package com.alhaq.amnshield.utils

import android.content.Context
import android.content.pm.PackageManager
import com.alhaq.amnshield.ui.dto.Report
import com.alhaq.amnshield.ui.dto.ReportDetail
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Utility class to generate comprehensive daily reports from blocking statistics
 */
class ReportGenerator(private val context: Context) {

    private val statsManager = BlockingStatsManager.getInstance(context)
    private val packageManager = context.packageManager
    private val savedPreferencesLoader = SavedPreferencesLoader(context)
    
    /**
     * Generate all reports for today
     */
    fun generateTodayReports(): List<Report> {
        return generateReportsForDate(LocalDate.now())
    }
    
    /**
     * Generate all reports for a specific date
     */
    fun generateReportsForDate(date: LocalDate): List<Report> {
        val reports = mutableListOf<Report>()
        val prevDate = date.minusDays(1)
        val summary = statsManager.getStatsSummaryForDate(date)
        val prevSummary = statsManager.getStatsSummaryForDate(prevDate)
        val events = statsManager.getBlockEventsForDate(date)
        val focusSessions = statsManager.getFocusSessionsForDate(date)
        val prevFocusSessions = statsManager.getFocusSessionsForDate(prevDate)
        val reelsData = savedPreferencesLoader.getReelsScrolled()

        // App Blocker Report
        reports.add(generateAppBlockerReport(summary.appBlocksCount, prevSummary.appBlocksCount, events))

        // Focus Mode Report
        reports.add(generateFocusModeReport(
            summary.focusSessionsCount, summary.totalFocusMinutes, focusSessions,
            prevSummary.totalFocusMinutes
        ))

        // Keyword Blocker Report
        reports.add(generateKeywordBlockerReport(summary.keywordBlocksCount, prevSummary.keywordBlocksCount, events))

        // Shorts Blocker Report (view/reel blocks)
        reports.add(generateViewBlockerReport(summary.viewBlocksCount, prevSummary.viewBlocksCount, events, date, reelsData))

        // Reel Tracker Report (passive reel scroll tracking)
        val todayReels = reelsData[date.toString()] ?: 0
        val prevReels = reelsData[prevDate.toString()] ?: 0
        if (todayReels > 0 || prevReels > 0) {
            reports.add(generateReelTrackerReport(date, reelsData))
        }

        return reports
    }

    private fun generateAppBlockerReport(count: Int, prevCount: Int, events: List<BlockingStatsManager.BlockEvent>): Report {
        val appBlockEvents = events.filter { it.type == BlockingStatsManager.BlockType.APP_BLOCK }

        val detailedStats = mutableListOf<ReportDetail>()

        if (count > 0) {
            val appBlockCounts = appBlockEvents
                .groupBy { it.packageName }
                .mapValues { it.value.size }
                .toList()
                .sortedByDescending { it.second }

            appBlockCounts.take(5).forEach { (packageName, blockCount) ->
                val appName = getAppName(packageName ?: "Unknown")
                detailedStats.add(ReportDetail(appName, "$blockCount times"))
            }
        }

        val summary = when {
            count == 0 -> "No apps blocked. Keep distractions at bay!"
            count == 1 -> "Blocked 1 app attempt today."
            else -> "Blocked $count app attempts today."
        }

        return Report(
            title = "App Blocker",
            summary = summary,
            count = count,
            yesterdayCount = prevCount,
            type = com.alhaq.amnshield.ui.dto.ReportType.APP_BLOCKER,
            detailedStats = detailedStats,
            additionalInfo = if (count > 0) "You stayed focused by avoiding blocked apps." else null
        )
    }

    private fun generateFocusModeReport(
        sessionCount: Int,
        totalMinutes: Long,
        sessions: List<BlockingStatsManager.FocusSession>,
        prevTotalMinutes: Long
    ): Report {
        val detailedStats = mutableListOf<ReportDetail>()

        if (sessionCount > 0) {
            val totalHours = totalMinutes / 60
            val remainingMinutes = totalMinutes % 60

            val timeText = when {
                totalHours > 0 && remainingMinutes > 0 -> "${totalHours}h ${remainingMinutes}m"
                totalHours > 0 -> "${totalHours}h"
                else -> "${totalMinutes}m"
            }

            detailedStats.add(ReportDetail("Total Focus Time", timeText))
            detailedStats.add(ReportDetail("Sessions Completed", "$sessionCount"))

            if (sessionCount > 0) {
                val avgDuration = totalMinutes / sessionCount
                detailedStats.add(ReportDetail("Avg Session", "${avgDuration}m"))
            }

            sessions.forEachIndexed { index, session ->
                val formatter = DateTimeFormatter.ofPattern("HH:mm")
                val startTime = java.time.Instant.ofEpochMilli(session.startTime)
                    .atZone(java.time.ZoneId.systemDefault())
                    .format(formatter)
                detailedStats.add(ReportDetail("Session ${index + 1} ($startTime)", "${session.durationMinutes}m"))
            }
        }

        val summary = when {
            sessionCount == 0 -> "No focus sessions yet. Start one to boost productivity!"
            sessionCount == 1 -> "Completed 1 focus session (${totalMinutes}m)."
            else -> "Completed $sessionCount focus sessions (${totalMinutes}m total)."
        }

        return Report(
            title = "Focus Mode",
            summary = summary,
            count = totalMinutes.toInt(),
            yesterdayCount = prevTotalMinutes.toInt(),
            type = com.alhaq.amnshield.ui.dto.ReportType.FOCUS_MODE,
            detailedStats = detailedStats,
            additionalInfo = if (totalMinutes > 0) "Great work staying focused!" else null
        )
    }

    private fun generateKeywordBlockerReport(count: Int, prevCount: Int, events: List<BlockingStatsManager.BlockEvent>): Report {
        val keywordEvents = events.filter { it.type == BlockingStatsManager.BlockType.KEYWORD_BLOCK }

        val detailedStats = mutableListOf<ReportDetail>()

        if (count > 0) {
            val appCounts = keywordEvents
                .groupBy { it.packageName ?: "Unknown" }
                .mapValues { it.value.size }
                .toList()
                .sortedByDescending { it.second }

            appCounts.take(5).forEach { (packageName, blockCount) ->
                val appName = getAppName(packageName)
                detailedStats.add(ReportDetail(appName, "$blockCount blocks"))
            }

            detailedStats.add(ReportDetail("Total Blocks", "$count"))
        }

        val summary = when {
            count == 0 -> "No harmful keywords detected today."
            count == 1 -> "Blocked 1 harmful keyword today."
            else -> "Blocked $count harmful keywords today."
        }

        return Report(
            title = "Keyword Blocker",
            summary = summary,
            count = count,
            yesterdayCount = prevCount,
            type = com.alhaq.amnshield.ui.dto.ReportType.KEYWORD_BLOCKER,
            detailedStats = detailedStats,
            additionalInfo = if (count > 0) "Protected you from harmful content." else null
        )
    }

    private fun generateViewBlockerReport(
        count: Int,
        prevCount: Int,
        events: List<BlockingStatsManager.BlockEvent>,
        date: LocalDate,
        reelsData: Map<String, Int>
    ): Report {
        val viewBlockEvents = events.filter { it.type == BlockingStatsManager.BlockType.VIEW_REEL_BLOCK }
        val reelsTracked = reelsData[date.toString()] ?: 0

        val detailedStats = mutableListOf<ReportDetail>()

        if (count > 0) {
            val appCounts = viewBlockEvents
                .groupBy { it.packageName ?: "Unknown" }
                .mapValues { it.value.size }
                .toList()
                .sortedByDescending { it.second }

            appCounts.take(5).forEach { (packageName, blockCount) ->
                val appName = getAppName(packageName)
                detailedStats.add(ReportDetail(appName, "$blockCount blocked"))
            }
        }

        if (reelsTracked > 0) {
            detailedStats.add(ReportDetail("Reels Scrolled (Tracked)", reelsTracked.toString()))
        }

        val summary = when {
            count == 0 && reelsTracked == 0 -> "No shorts or reels blocked today."
            count == 0 -> "No reels blocked. Tracked $reelsTracked reels scrolled."
            count == 1 -> "Blocked 1 reel/short today."
            else -> "Blocked $count reels/shorts today."
        }

        return Report(
            title = "Shorts Blocker",
            summary = summary,
            count = count,
            yesterdayCount = prevCount,
            type = com.alhaq.amnshield.ui.dto.ReportType.VIEW_BLOCKER,
            detailedStats = detailedStats,
            additionalInfo = if (count > 0) "Saved you from time-wasting short-form content." else null
        )
    }

    private fun generateReelTrackerReport(date: LocalDate, reelsData: Map<String, Int>): Report {
        val todayCount = reelsData[date.toString()] ?: 0
        val prevCount = reelsData[date.minusDays(1).toString()] ?: 0

        val detailedStats = mutableListOf<ReportDetail>()

        // Last 7 days breakdown
        val formatter = DateTimeFormatter.ofPattern("EEE, MMM d")
        for (i in 6 downTo 0) {
            val d = date.minusDays(i.toLong())
            val count = reelsData[d.toString()] ?: 0
            if (count > 0 || i == 0) {
                val label = if (i == 0) "Today" else d.format(formatter)
                detailedStats.add(ReportDetail(label, "$count reels"))
            }
        }

        val weekTotal = (0 until 7).sumOf { i -> reelsData[date.minusDays(i.toLong()).toString()] ?: 0 }
        if (weekTotal > 0) {
            detailedStats.add(ReportDetail("7-Day Total", "$weekTotal reels"))
            detailedStats.add(ReportDetail("Daily Average", "${weekTotal / 7} reels"))
        }

        val summary = when {
            todayCount == 0 -> "No reels tracked today."
            todayCount == 1 -> "Scrolled 1 reel today."
            else -> "Scrolled $todayCount reels today."
        }

        return Report(
            title = "Reel Tracker",
            summary = summary,
            count = todayCount,
            yesterdayCount = prevCount,
            type = com.alhaq.amnshield.ui.dto.ReportType.REEL_TRACKER,
            detailedStats = detailedStats,
            additionalInfo = if (todayCount > 20) "High reel usage — consider setting a daily limit." else null
        )
    }
    
    /**
     * Get app name from package name, fallback to package name if not found
     */
    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }
    
    /**
     * Generate a summary report for notification purposes
     */
    fun generateDailySummaryText(date: LocalDate = LocalDate.now()): String {
        val summary = statsManager.getStatsSummaryForDate(date)
        val totalBlocks = summary.appBlocksCount + summary.keywordBlocksCount + summary.viewBlocksCount
        
        val builder = StringBuilder()
        builder.append("Daily Report - ${date.format(DateTimeFormatter.ofPattern("MMM dd"))}\n\n")
        
        if (totalBlocks == 0 && summary.focusSessionsCount == 0) {
            builder.append("No blocking activity today.")
            return builder.toString()
        }
        
        if (summary.appBlocksCount > 0) {
            builder.append("🚫 Apps blocked: ${summary.appBlocksCount}\n")
        }
        
        if (summary.focusSessionsCount > 0) {
            builder.append("⏱️ Focus sessions: ${summary.focusSessionsCount} (${summary.totalFocusMinutes}m)\n")
        }
        
        if (summary.keywordBlocksCount > 0) {
            builder.append("🔍 Keywords blocked: ${summary.keywordBlocksCount}\n")
        }
        
        if (summary.viewBlocksCount > 0) {
            builder.append("📱 Reels/Views blocked: ${summary.viewBlocksCount}\n")
        }
        
        builder.append("\nTap to view detailed report.")
        
        return builder.toString()
    }
}
