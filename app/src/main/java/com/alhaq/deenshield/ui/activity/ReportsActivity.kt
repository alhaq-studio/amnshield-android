package com.alhaq.deenshield.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.alhaq.deenshield.R
import com.alhaq.deenshield.ui.adapters.ReportsAdapter
import com.alhaq.deenshield.ui.dto.Report
import com.alhaq.deenshield.utils.ReportGenerator
import java.io.File

class ReportsActivity : AppCompatActivity() {
    
    private lateinit var reportGenerator: ReportGenerator
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme before super.onCreate
        val sharedPreferences = getSharedPreferences("com.alhaq.deenshield_preferences", MODE_PRIVATE)
        val themeStyle = sharedPreferences.getString("theme_style", "default")
        if (themeStyle == "gradient") {
            setTheme(R.style.Theme_DeenShield_Gradient)
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reports)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Daily Reports"

        // Initialize report generator
        reportGenerator = ReportGenerator(this)

        val recyclerView = findViewById<RecyclerView>(R.id.reports_recycler_view)
        
        // Generate real reports from actual blocking data
        val reports = reportGenerator.generateTodayReports()
        
        recyclerView.adapter = ReportsAdapter(reports)

        val exportButton = findViewById<Button>(R.id.export_button)
        exportButton.setOnClickListener {
            exportReport(reports)
        }
    }

    private fun exportReport(reports: List<Report>) {
        val reportBuilder = StringBuilder()
        reportBuilder.append("DeenShield Daily Report\n")
        reportBuilder.append("========================\n\n")
        
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
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}