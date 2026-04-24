package com.alhaq.deenshield.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alhaq.deenshield.R
import com.alhaq.deenshield.ui.dto.Report
import com.alhaq.deenshield.ui.dto.ReportDetail
import com.google.android.material.progressindicator.LinearProgressIndicator

class ReportsAdapter(private val reports: List<Report>) : RecyclerView.Adapter<ReportsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_report_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val report = reports[position]
        holder.bind(report)
    }

    override fun getItemCount() = reports.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.report_card_title)
        private val summary: TextView = itemView.findViewById(R.id.report_card_summary)
        private val count: TextView = itemView.findViewById(R.id.report_card_count)
        private val progressContainer: LinearLayout = itemView.findViewById(R.id.progress_container)
        private val progressLabel: TextView = itemView.findViewById(R.id.progress_label)
        private val progressPercentage: TextView = itemView.findViewById(R.id.progress_percentage)
        private val progressBar: LinearProgressIndicator = itemView.findViewById(R.id.report_progress_bar)
        private val detailedStatsContainer: LinearLayout = itemView.findViewById(R.id.detailed_stats_container)
        private val statsRecycler: RecyclerView = itemView.findViewById(R.id.stats_recycler)
        private val additionalInfo: TextView = itemView.findViewById(R.id.report_additional_info)
        private val expandButton: Button = itemView.findViewById(R.id.expand_button)
        
        private var isExpanded = false

        fun bind(report: Report) {
            title.text = report.title
            summary.text = report.summary
            count.text = report.count.toString()
            
            // Show/hide additional info
            if (report.additionalInfo != null) {
                additionalInfo.text = report.additionalInfo
                additionalInfo.visibility = View.VISIBLE
            } else {
                additionalInfo.visibility = View.GONE
            }
            
            // Calculate and show progress
            val progress = calculateProgress(report)
            if (progress >= 0) {
                progressContainer.visibility = View.VISIBLE
                progressLabel.text = getProgressLabel(report.title)
                progressPercentage.text = "${progress}%"
                progressBar.progress = progress
            } else {
                progressContainer.visibility = View.GONE
            }
            
            // Setup detailed stats
            if (report.detailedStats.isNotEmpty()) {
                statsRecycler.layoutManager = LinearLayoutManager(itemView.context)
                statsRecycler.adapter = StatDetailAdapter(report.detailedStats)
                expandButton.visibility = View.VISIBLE
                
                expandButton.setOnClickListener {
                    isExpanded = !isExpanded
                    detailedStatsContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE
                    expandButton.text = if (isExpanded) "Hide Details" else "View Details"
                    val iconRes = if (isExpanded) R.drawable.baseline_remove_24 else R.drawable.baseline_read_more_24
                    val icon = itemView.context.getDrawable(iconRes)
                    expandButton.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)
                }
            } else {
                expandButton.visibility = View.GONE
                detailedStatsContainer.visibility = View.GONE
            }
        }
        
        private fun calculateProgress(report: Report): Int {
            return when (report.type) {
                com.alhaq.deenshield.ui.dto.ReportType.APP_BLOCKER -> {
                    // Show progress as percentage of daily goal (assume 20 blocks = 100%)
                    minOf(100, (report.count * 100) / 20)
                }
                com.alhaq.deenshield.ui.dto.ReportType.FOCUS_MODE -> {
                    // Show progress based on focus time (assume 120 minutes = 100%)
                    val minutes = report.count // Count already represents total minutes
                    minOf(100, (minutes * 100) / 120)
                }
                com.alhaq.deenshield.ui.dto.ReportType.KEYWORD_BLOCKER -> {
                    // Show progress as protection level
                    minOf(100, (report.count * 100) / 15)
                }
                com.alhaq.deenshield.ui.dto.ReportType.VIEW_BLOCKER -> {
                    // Show progress as distraction avoidance
                    minOf(100, (report.count * 100) / 25)
                }
                else -> -1
            }
        }
        
        private fun extractMinutes(timeString: String): Int {
            return try {
                // Parse formats like "2h 30m" or "45m"
                var totalMinutes = 0
                if (timeString.contains("h")) {
                    val hours = timeString.substringBefore("h").trim().toIntOrNull() ?: 0
                    totalMinutes += hours * 60
                }
                if (timeString.contains("m")) {
                    val minutesPart = if (timeString.contains("h")) {
                        timeString.substringAfter("h").substringBefore("m").trim()
                    } else {
                        timeString.substringBefore("m").trim()
                    }
                    totalMinutes += minutesPart.toIntOrNull() ?: 0
                }
                totalMinutes
            } catch (e: Exception) {
                0
            }
        }
        
        private fun getProgressLabel(title: String): String {
            return when (title) {
                "App Blocker" -> "Daily Goal"
                "Focus Mode" -> "Focus Time"
                "Keyword Blocker" -> "Protection Level"
                "View/Reel Blocker" -> "Distractions Blocked"
                else -> "Progress"
            }
        }
    }
    
    class StatDetailAdapter(private val details: List<ReportDetail>) : 
        RecyclerView.Adapter<StatDetailAdapter.StatViewHolder>() {
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_stat_detail, parent, false)
            return StatViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: StatViewHolder, position: Int) {
            val detail = details[position]
            holder.label.text = detail.label
            holder.value.text = detail.value
        }
        
        override fun getItemCount() = details.size
        
        class StatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val label: TextView = itemView.findViewById(R.id.stat_label)
            val value: TextView = itemView.findViewById(R.id.stat_value)
        }
    }
}
