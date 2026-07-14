package com.alhaq.amnshield.ui.activity

import android.app.AppOpsManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.FileProvider
import com.alhaq.amnshield.ui.dto.Report
import com.alhaq.amnshield.ui.screens.ReportsScreen
import com.alhaq.amnshield.ui.theme.AmnShieldTheme
import com.alhaq.amnshield.utils.BlockingStatsManager
import com.alhaq.amnshield.utils.ReportGenerator
import com.alhaq.amnshield.utils.UsageStatsHelper
import com.alhaq.amnshield.utils.SavedPreferencesLoader
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class ReportsActivity : AppCompatActivity() {

    private lateinit var reportGenerator: ReportGenerator
    private var currentDate: LocalDate = LocalDate.now()
    private val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")

    // Compose States
    private val currentDateLabelState = mutableStateOf("Today")
    private val totalBlocksState = mutableStateOf(0)
    private val focusTimeLabelState = mutableStateOf("0m focus")
    private val usageRecommendationState = mutableStateOf("")
    private val isAppBlockerEnabledState = mutableStateOf(false)
    private val reportsState = mutableStateListOf<Report>()
    private val canGoNextState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        com.alhaq.amnshield.utils.ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        
        try {
            reportGenerator = ReportGenerator(this)

            setContent {
                AmnShieldTheme {
                    ReportsScreen(
                        currentDateLabel = currentDateLabelState.value,
                        totalBlocks = totalBlocksState.value,
                        focusTimeLabel = focusTimeLabelState.value,
                        usageRecommendation = usageRecommendationState.value,
                        isAppBlockerEnabled = isAppBlockerEnabledState.value,
                        reports = reportsState,
                        canGoNext = canGoNextState.value,
                        onPrevDay = {
                            currentDate = currentDate.minusDays(1)
                            refreshReports()
                        },
                        onNextDay = {
                            if (currentDate.isBefore(LocalDate.now())) {
                                currentDate = currentDate.plusDays(1)
                                refreshReports()
                            }
                        },
                        onExportReport = {
                            exportReport(reportsState)
                        }
                    )
                }
            }
            refreshReports()
        } catch (t: Throwable) {
            android.util.Log.e("ReportsActivity", "onCreate init failed", t)
            android.widget.Toast.makeText(
                this,
                "Reports could not be opened. Please try again.",
                android.widget.Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isFinishing || !::reportGenerator.isInitialized) return
        refreshReports()
    }

    private fun refreshReports() {
        try {
            currentDateLabelState.value = if (currentDate == LocalDate.now()) "Today" else currentDate.format(dateFormatter)
            canGoNextState.value = currentDate.isBefore(LocalDate.now())

            val newReports = reportGenerator.generateReportsForDate(currentDate)
            reportsState.clear()
            reportsState.addAll(newReports)

            val statsManager = BlockingStatsManager.getInstance(this)
            val summary = statsManager.getStatsSummaryForDate(currentDate)
            val totalBlocks = summary.appBlocksCount + summary.keywordBlocksCount + summary.viewBlocksCount
            totalBlocksState.value = totalBlocks

            val focusMin = summary.totalFocusMinutes
            focusTimeLabelState.value = if (focusMin >= 60) "${focusMin / 60}h ${focusMin % 60}m" else "${focusMin}m"

            val prefs = SavedPreferencesLoader(this)
            isAppBlockerEnabledState.value = prefs.isAppBlockerFeatureEnabled()

            refreshUsageRecommendation()
        } catch (t: Throwable) {
            android.util.Log.e("ReportsActivity", "refreshReports failed", t)
            reportsState.clear()
            totalBlocksState.value = 0
            focusTimeLabelState.value = "0m"
            usageRecommendationState.value = "Could not calculate app usage recommendation for this day."
            android.widget.Toast.makeText(
                this,
                "Could not load report for this day",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun refreshUsageRecommendation() {
        if (!hasUsageStatsPermission()) {
            usageRecommendationState.value = "Grant usage access to enable app-level recommendations."
            return
        }

        val stats = UsageStatsHelper(this).getForegroundStatsByDay(currentDate)
            .filter {
                it.totalTime >= 180_000L &&
                    !it.packageName.equals("com.alhaq.amnshield", ignoreCase = true) &&
                    !it.packageName.equals("android", ignoreCase = true) &&
                    !it.packageName.equals("com.android.systemui", ignoreCase = true)
            }

        val top = stats.maxByOrNull { it.totalTime }
        if (top == null) {
            usageRecommendationState.value = "No high-risk app usage detected for this day."
            return
        }

        val topRiskyAppName = getAppLabel(top.packageName)

        val minutes = (top.totalTime / 60000L).toInt()
        val sessions = top.sessions.size
        val riskText = when {
            minutes >= 240 || sessions >= 35 -> "High"
            minutes >= 120 || sessions >= 20 -> "Moderate"
            else -> "Elevated"
        }

        usageRecommendationState.value = "$riskText risk: ${topRiskyAppName ?: top.packageName} used ${minutes}m across $sessions sessions. Consider limiting usage in App Blocker."
    }

    private fun getAppLabel(packageName: String): String {
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            packageName
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOpsManager = getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOpsManager.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun exportReport(reports: List<Report>) {
        try {
            val reportBuilder = StringBuilder()
            reportBuilder.append("AmnShield Stats & Report\n")
            reportBuilder.append("=========================\n")
            reportBuilder.append("Date: ${currentDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}\n")

            val summary = BlockingStatsManager.getInstance(this).getStatsSummaryForDate(currentDate)
            val totalBlocks = summary.appBlocksCount + summary.keywordBlocksCount + summary.viewBlocksCount
            reportBuilder.append("Total Blocks: $totalBlocks\n")
            reportBuilder.append("Focus Time: ${summary.totalFocusMinutes} minutes\n")
            reportBuilder.append("Focus Sessions: ${summary.focusSessionsCount}\n\n")

            if (reports.isEmpty()) {
                reportBuilder.append("No report data available for this date.\n")
            }

            reports.forEach { report ->
                reportBuilder.append("${report.title}\n")
                reportBuilder.append("-".repeat(report.title.length)).append("\n")
                reportBuilder.append("${report.summary}\n")

                if (report.detailedStats.isNotEmpty()) {
                    reportBuilder.append("\nDetails:\n")
                    report.detailedStats.forEach { detail ->
                        reportBuilder.append("  • ${detail.label}: ${detail.value}\n")
                    }
                }

                report.additionalInfo?.let {
                    reportBuilder.append("\n$it\n")
                }

                reportBuilder.append("\n")
            }

            val file = File(cacheDir, "amnshield_report.txt")
            file.writeText(reportBuilder.toString())

            val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "text/plain"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Export Report"))
        } catch (t: Throwable) {
            android.util.Log.e("ReportsActivity", "exportReport failed", t)
            android.widget.Toast.makeText(
                this,
                "Could not export report",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
}