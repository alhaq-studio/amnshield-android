package com.alhaq.deenshield.ui.activity

import android.content.Intent
import android.app.AppOpsManager
import android.os.Bundle
import android.widget.TextView
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alhaq.deenshield.R
import com.alhaq.deenshield.ui.adapters.ReportsAdapter
import com.alhaq.deenshield.ui.dto.Report
import com.alhaq.deenshield.utils.ReportGenerator
import com.alhaq.deenshield.utils.BlockingStatsManager
import com.alhaq.deenshield.utils.SavedPreferencesLoader
import com.alhaq.deenshield.utils.UsageStatsHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import java.io.File
import java.time.format.FormatStyle
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ReportsActivity : AppCompatActivity() {

    private lateinit var reportGenerator: ReportGenerator
    private lateinit var reportsRecyclerView: RecyclerView
    private lateinit var txtReportDate: TextView
    private lateinit var btnPrevDay: MaterialButton
    private lateinit var btnNextDay: MaterialButton
    private lateinit var chipTotalBlocks: Chip
    private lateinit var chipFocusTime: Chip
    private lateinit var chipReelsScrolled: Chip
    private lateinit var txtUsageRecommendation: TextView
    private var topRiskyAppName: String? = null

    private var currentDate: LocalDate = LocalDate.now()
    private var latestReports: List<Report> = emptyList()

    private val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")

    override fun onCreate(savedInstanceState: Bundle?) {
        com.alhaq.deenshield.utils.ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reports)
        findViewById<com.google.android.material.appbar.MaterialToolbar?>(R.id.toolbar)?.let {
            setSupportActionBar(it)
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Stats & Reports"

        try {
            reportGenerator = ReportGenerator(this)

            reportsRecyclerView = findViewById(R.id.reports_recycler_view)
            reportsRecyclerView.layoutManager = LinearLayoutManager(this)
            txtReportDate = findViewById(R.id.txt_report_date)
            btnPrevDay = findViewById(R.id.btn_prev_day)
            btnNextDay = findViewById(R.id.btn_next_day)
            chipTotalBlocks = findViewById(R.id.chip_total_blocks)
            chipFocusTime = findViewById(R.id.chip_focus_time)
            chipReelsScrolled = findViewById(R.id.chip_reels_scrolled)
            txtUsageRecommendation = findViewById(R.id.txt_usage_recommendation)

            btnPrevDay.setOnClickListener {
                currentDate = currentDate.minusDays(1)
                refreshReports()
            }

            btnNextDay.setOnClickListener {
                if (currentDate.isBefore(LocalDate.now())) {
                    currentDate = currentDate.plusDays(1)
                    refreshReports()
                }
            }

            refreshReports()

            val exportButton = findViewById<com.google.android.material.button.MaterialButton>(R.id.export_button)
            exportButton.setOnClickListener {
                exportReport(latestReports)
            }
        } catch (t: Throwable) {
            // Last-resort guard: refuse to crash the whole app on the way into
            // Reports. Log the failure and bail out gracefully so the user can
            // navigate back instead of seeing the system FC dialog.
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
        // Skip if we already failed to initialize (finish() called from onCreate).
        if (isFinishing || !::reportGenerator.isInitialized) return
        refreshReports()
    }

    private fun refreshReports() {
        // Defensive: any single failure (corrupt JSON in stats prefs, missing
        // package metadata, etc.) must not take down the whole Reports screen.
        // We catch broadly, log, and fall back to an empty state so the user
        // still sees the date navigator + chips.
        try {
            // Update date label
            txtReportDate.text = if (currentDate == LocalDate.now()) "Today" else currentDate.format(dateFormatter)
            // Disable next button when on today
            btnNextDay.isEnabled = currentDate.isBefore(LocalDate.now())
            btnNextDay.alpha = if (btnNextDay.isEnabled) 1f else 0.4f

            latestReports = reportGenerator.generateReportsForDate(currentDate)
            reportsRecyclerView.adapter = ReportsAdapter(latestReports)

            // Update summary chips
            val statsManager = BlockingStatsManager.getInstance(this)
            val summary = statsManager.getStatsSummaryForDate(currentDate)
            val totalBlocks = summary.appBlocksCount + summary.keywordBlocksCount + summary.viewBlocksCount
            chipTotalBlocks.text = "$totalBlocks blocks"

            val focusMin = summary.totalFocusMinutes
            chipFocusTime.text = if (focusMin >= 60) "${focusMin / 60}h ${focusMin % 60}m focus" else "${focusMin}m focus"

            val reelsData = SavedPreferencesLoader(this).getReelsScrolled()
            val reelsToday = reelsData[currentDate.toString()] ?: 0
            chipReelsScrolled.text = "$reelsToday reels"

            refreshUsageRecommendation()
        } catch (t: Throwable) {
            android.util.Log.e("ReportsActivity", "refreshReports failed", t)
            latestReports = emptyList()
            reportsRecyclerView.adapter = ReportsAdapter(emptyList())
            chipTotalBlocks.text = "0 blocks"
            chipFocusTime.text = "0m focus"
            chipReelsScrolled.text = "0 reels"
            txtUsageRecommendation.text = "Could not calculate app usage recommendation for this day."
            android.widget.Toast.makeText(
                this,
                "Could not load report for this day",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun refreshUsageRecommendation() {
        if (!hasUsageStatsPermission()) {
            topRiskyAppName = null
            txtUsageRecommendation.text = "Grant usage access to enable app-level recommendations."
            return
        }

        val stats = UsageStatsHelper(this).getForegroundStatsByDay(currentDate)
            .filter {
                it.totalTime >= 180_000L &&
                    !it.packageName.equals("com.alhaq.deenshield", ignoreCase = true) &&
                    !it.packageName.equals("android", ignoreCase = true) &&
                    !it.packageName.equals("com.android.systemui", ignoreCase = true)
            }

        val top = stats.maxByOrNull { it.totalTime }
        if (top == null) {
            topRiskyAppName = null
            txtUsageRecommendation.text = "No high-risk app usage detected for this day."
            return
        }

        topRiskyAppName = getAppLabel(top.packageName)

        val minutes = (top.totalTime / 60000L).toInt()
        val sessions = top.sessions.size
        val riskText = when {
            minutes >= 240 || sessions >= 35 -> "High"
            minutes >= 120 || sessions >= 20 -> "Moderate"
            else -> "Elevated"
        }

        txtUsageRecommendation.text =
            "$riskText risk: ${topRiskyAppName ?: top.packageName} used ${minutes}m across $sessions sessions. " +
                "Consider limiting usage in App Blocker."
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
            reportBuilder.append("DeenShield Stats & Report\n")
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

            val file = File(cacheDir, "deenshield_report.txt")
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

    override fun onSupportNavigateUp(): Boolean {
        // Use the modern dispatcher instead of the deprecated onBackPressed().
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}