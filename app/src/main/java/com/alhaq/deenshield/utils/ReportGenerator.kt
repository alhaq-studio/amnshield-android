package com.alhaq.deenshield.utils

import android.content.Context
import android.content.pm.PackageManager
import com.alhaq.deenshield.ui.dto.Report
import com.alhaq.deenshield.ui.dto.ReportDetail
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Utility class to generate comprehensive daily reports from blocking statistics
 */
class ReportGenerator(private val context: Context) {

    private val statsManager = BlockingStatsManager.getInstance(context)
    private val packageManager = context.packageManager
    
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
        val summary = statsManager.getStatsSummaryForDate(date)
        val events = statsManager.getBlockEventsForDate(date)
        val focusSessions = statsManager.getFocusSessionsForDate(date)
        
        // App Blocker Report
        reports.add(generateAppBlockerReport(summary.appBlocksCount, events))
        
        // Focus Mode Report
        reports.add(generateFocusModeReport(summary.focusSessionsCount, summary.totalFocusMinutes, focusSessions))
        
        // Keyword Blocker Report
        reports.add(generateKeywordBlockerReport(summary.keywordBlocksCount, events))
        
        // View/Reel Blocker Report
        reports.add(generateViewBlockerReport(summary.viewBlocksCount, events))
        
        return reports
    }
    
    private fun generateAppBlockerReport(count: Int, events: List<BlockingStatsManager.BlockEvent>): Report {
        val appBlockEvents = events.filter { it.type == BlockingStatsManager.BlockType.APP_BLOCK }
        
        val detailedStats = mutableListOf<ReportDetail>()
        
        if (count > 0) {
            // Group by app and count occurrences
            val appBlockCounts = appBlockEvents
                .groupBy { it.packageName }
                .mapValues { it.value.size }
                .toList()
                .sortedByDescending { it.second }
            
            // Add top blocked apps
            appBlockCounts.take(5).forEach { (packageName, blockCount) ->
                val appName = getAppName(packageName ?: "Unknown")
                detailedStats.add(ReportDetail(appName, "$blockCount times"))
            }
        }
        
        val summary = when {
            count == 0 -> "No apps blocked today. Keep it up!"
            count == 1 -> "Blocked 1 app attempt today."
            else -> "Blocked $count app attempts today."
        }
        
        return Report(
            title = "App Blocker",
            summary = summary,
            count = count,
            type = com.alhaq.deenshield.ui.dto.ReportType.APP_BLOCKER,
            detailedStats = detailedStats,
            additionalInfo = if (count > 0) "You stayed focused by avoiding blocked apps." else null
        )
    }
    
    private fun generateFocusModeReport(
        sessionCount: Int,
        totalMinutes: Long,
        sessions: List<BlockingStatsManager.FocusSession>
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
            
            // Add individual session durations
            sessions.forEachIndexed { index, session ->
                val formatter = DateTimeFormatter.ofPattern("HH:mm")
                val startTime = java.time.Instant.ofEpochMilli(session.startTime)
                    .atZone(java.time.ZoneId.systemDefault())
                    .format(formatter)
                detailedStats.add(
                    ReportDetail(
                        "Session ${index + 1} (${startTime})",
                        "${session.durationMinutes}m"
                    )
                )
            }
        }
        
        val summary = when {
            sessionCount == 0 -> "No focus sessions today. Start one to boost productivity!"
            sessionCount == 1 -> "Completed 1 focus session (${totalMinutes}m)."
            else -> "Completed $sessionCount focus sessions (${totalMinutes}m total)."
        }
        
        return Report(
            title = "Focus Mode",
            summary = summary,
            count = totalMinutes.toInt(),
            type = com.alhaq.deenshield.ui.dto.ReportType.FOCUS_MODE,
            detailedStats = detailedStats,
            additionalInfo = if (totalMinutes > 0) "Great work staying focused!" else null
        )
    }
    
    private fun generateKeywordBlockerReport(count: Int, events: List<BlockingStatsManager.BlockEvent>): Report {
        val keywordEvents = events.filter { it.type == BlockingStatsManager.BlockType.KEYWORD_BLOCK }
        
        val detailedStats = mutableListOf<ReportDetail>()
        
        if (count > 0) {
            // Group by app where keywords were blocked
            val appCounts = keywordEvents
                .groupBy { it.packageName ?: "Unknown" }
                .mapValues { it.value.size }
                .toList()
                .sortedByDescending { it.second }
            
            // Add apps where keywords were blocked
            appCounts.take(5).forEach { (packageName, blockCount) ->
                val appName = getAppName(packageName)
                detailedStats.add(ReportDetail(appName, "$blockCount keywords"))
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
            type = com.alhaq.deenshield.ui.dto.ReportType.KEYWORD_BLOCKER,
            detailedStats = detailedStats,
            additionalInfo = if (count > 0) "Protected you from harmful content." else null
        )
    }
    
    private fun generateViewBlockerReport(count: Int, events: List<BlockingStatsManager.BlockEvent>): Report {
        val viewBlockEvents = events.filter { it.type == BlockingStatsManager.BlockType.VIEW_REEL_BLOCK }
        
        val detailedStats = mutableListOf<ReportDetail>()
        
        if (count > 0) {
            // Group by app
            val appCounts = viewBlockEvents
                .groupBy { it.packageName ?: "Unknown" }
                .mapValues { it.value.size }
                .toList()
                .sortedByDescending { it.second }
            
            // Add apps where views/reels were blocked
            appCounts.take(5).forEach { (packageName, blockCount) ->
                val appName = getAppName(packageName)
                detailedStats.add(ReportDetail(appName, "$blockCount reels/views"))
            }
        }
        
        val summary = when {
            count == 0 -> "No reels or views blocked today."
            count == 1 -> "Blocked 1 reel or view today."
            else -> "Blocked $count reels or views today."
        }
        
        return Report(
            title = "View/Reel Blocker",
            summary = summary,
            count = count,
            type = com.alhaq.deenshield.ui.dto.ReportType.VIEW_BLOCKER,
            detailedStats = detailedStats,
            additionalInfo = if (count > 0) "Saved you from time-wasting content." else null
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
