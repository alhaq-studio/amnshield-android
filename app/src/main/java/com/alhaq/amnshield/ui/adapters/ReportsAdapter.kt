package com.alhaq.amnshield.ui.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alhaq.amnshield.R
import com.alhaq.amnshield.ui.dto.Report
import com.alhaq.amnshield.ui.dto.ReportDetail
import com.alhaq.amnshield.ui.dto.ReportType
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.progressindicator.LinearProgressIndicator

class ReportsAdapter(private val reports: List<Report>) : RecyclerView.Adapter<ReportsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_report_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val report = reports[position]
        try {
            holder.bind(report)
        } catch (_: Throwable) {
            holder.bindFallback(report)
        }
    }

    override fun getItemCount() = reports.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.report_card_title)
        private val summary: TextView = itemView.findViewById(R.id.report_card_summary)
        private val count: TextView = itemView.findViewById(R.id.report_card_count)
        private val trendBadge: TextView = itemView.findViewById(R.id.report_trend_badge)
        private val typeIcon: ImageView = itemView.findViewById(R.id.report_type_icon)
        private val iconContainer: MaterialCardView = itemView.findViewById(R.id.report_icon_container)
        private val progressContainer: LinearLayout = itemView.findViewById(R.id.progress_container)
        private val progressLabel: TextView = itemView.findViewById(R.id.progress_label)
        private val progressPercentage: TextView = itemView.findViewById(R.id.progress_percentage)
        private val progressBar: LinearProgressIndicator = itemView.findViewById(R.id.report_progress_bar)
        private val detailedStatsContainer: LinearLayout = itemView.findViewById(R.id.detailed_stats_container)
        private val statsRecycler: RecyclerView = itemView.findViewById(R.id.stats_recycler)
        private val additionalInfo: TextView = itemView.findViewById(R.id.report_additional_info)
        private val expandButton: com.google.android.material.button.MaterialButton = itemView.findViewById(R.id.expand_button)

        private var isExpanded = false

        fun bind(report: Report) {
            title.text = report.title
            summary.text = report.summary
            count.text = report.count.toString()

            // Reset recycled expansion state on each bind.
            isExpanded = false
            detailedStatsContainer.visibility = View.GONE
            expandButton.text = "View Details"
            expandButton.setCompoundDrawablesWithIntrinsicBounds(
                ContextCompat.getDrawable(itemView.context, R.drawable.baseline_read_more_24), null, null, null
            )

            // Set type icon
            val iconDrawable = ContextCompat.getDrawable(itemView.context, report.type.iconRes)
            typeIcon.setImageDrawable(iconDrawable)

            // Apply type-specific accent color to icon container
            val (containerColorAttr, iconTintAttr) = getTypeColors(report.type)
            val containerColor = MaterialColors.getColor(itemView, containerColorAttr, 0)
            val iconTintColor = MaterialColors.getColor(itemView, iconTintAttr, 0)
            iconContainer.setCardBackgroundColor(containerColor)
            typeIcon.imageTintList = ColorStateList.valueOf(iconTintColor)

            // Trend badge
            if (report.yesterdayCount > 0 || report.count > 0) {
                val diff = report.count - report.yesterdayCount
                val pct = if (report.yesterdayCount > 0) {
                    (diff * 100f / report.yesterdayCount).toInt()
                } else if (report.count > 0) {
                    100
                } else {
                    0
                }
                if (report.yesterdayCount > 0) {
                    val arrow = if (diff >= 0) "↑" else "↓"
                    trendBadge.text = "$arrow ${Math.abs(pct)}% vs yesterday"
                    // For blocks: more is worse (red up, green down); for focus: more is better (green up, red down)
                    val isPositiveMetric = report.type == ReportType.FOCUS_MODE
                    val isGood = if (isPositiveMetric) diff >= 0 else diff <= 0
                    trendBadge.setTextColor(
                        if (isGood) ContextCompat.getColor(itemView.context, R.color.md_theme_primary)
                        else ContextCompat.getColor(itemView.context, android.R.color.holo_red_dark)
                    )
                    trendBadge.visibility = View.VISIBLE
                } else {
                    if (report.count > 0) {
                        trendBadge.text = "New activity today"
                        trendBadge.setTextColor(ContextCompat.getColor(itemView.context, R.color.md_theme_primary))
                        trendBadge.visibility = View.VISIBLE
                    } else {
                        trendBadge.visibility = View.GONE
                    }
                }
            } else {
                trendBadge.visibility = View.GONE
            }

            // Additional info
            if (report.additionalInfo != null) {
                additionalInfo.text = report.additionalInfo
                additionalInfo.visibility = View.VISIBLE
            } else {
                additionalInfo.visibility = View.GONE
            }

            // Progress bar
            val progress = calculateProgress(report)
            if (progress >= 0) {
                progressContainer.visibility = View.VISIBLE
                progressLabel.text = getProgressLabel(report.type)
                progressPercentage.text = "${progress}%"
                progressBar.progress = progress
                // Color the progress bar based on type
                val progressColor = getProgressColor(report.type, progress)
                progressBar.setIndicatorColor(progressColor)
            } else {
                progressContainer.visibility = View.GONE
            }

            // Detailed stats
            if (report.detailedStats.isNotEmpty()) {
                statsRecycler.layoutManager = LinearLayoutManager(itemView.context)
                statsRecycler.adapter = StatDetailAdapter(report.detailedStats)
                expandButton.visibility = View.VISIBLE

                expandButton.setOnClickListener {
                    isExpanded = !isExpanded
                    detailedStatsContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE
                    expandButton.text = if (isExpanded) "Hide Details" else "View Details"
                    val iconRes = if (isExpanded) R.drawable.baseline_remove_24 else R.drawable.baseline_read_more_24
                    expandButton.setCompoundDrawablesWithIntrinsicBounds(
                        ContextCompat.getDrawable(itemView.context, iconRes), null, null, null
                    )
                }
            } else {
                expandButton.visibility = View.GONE
                detailedStatsContainer.visibility = View.GONE
            }
        }

        fun bindFallback(report: Report) {
            title.text = report.title.ifBlank { "Report" }
            summary.text = report.summary.ifBlank { "Details unavailable for this entry." }
            count.text = report.count.toString()
            trendBadge.visibility = View.GONE
            progressContainer.visibility = View.GONE
            detailedStatsContainer.visibility = View.GONE
            expandButton.visibility = View.GONE
            additionalInfo.visibility = View.GONE
            typeIcon.setImageDrawable(ContextCompat.getDrawable(itemView.context, R.drawable.baseline_info_24))
            val containerColor = MaterialColors.getColor(itemView, com.google.android.material.R.attr.colorSurfaceVariant, 0)
            val iconTintColor = MaterialColors.getColor(itemView, com.google.android.material.R.attr.colorOnSurfaceVariant, 0)
            iconContainer.setCardBackgroundColor(containerColor)
            typeIcon.imageTintList = ColorStateList.valueOf(iconTintColor)
        }

        private fun getTypeColors(type: ReportType): Pair<Int, Int> {
            val ctx = itemView.context
            return when (type) {
                ReportType.APP_BLOCKER     -> Pair(
                    com.google.android.material.R.attr.colorTertiaryContainer,
                    com.google.android.material.R.attr.colorOnTertiaryContainer
                )
                ReportType.KEYWORD_BLOCKER -> Pair(
                    com.google.android.material.R.attr.colorErrorContainer,
                    com.google.android.material.R.attr.colorOnErrorContainer
                )
                ReportType.VIEW_BLOCKER    -> Pair(
                    com.google.android.material.R.attr.colorSecondaryContainer,
                    com.google.android.material.R.attr.colorOnSecondaryContainer
                )
                ReportType.REEL_TRACKER   -> Pair(
                    com.google.android.material.R.attr.colorPrimaryContainer,
                    com.google.android.material.R.attr.colorOnPrimaryContainer
                )
                ReportType.FOCUS_MODE     -> Pair(
                    com.google.android.material.R.attr.colorSecondaryContainer,
                    com.google.android.material.R.attr.colorOnSecondaryContainer
                )
                ReportType.GENERAL        -> Pair(
                    com.google.android.material.R.attr.colorSurfaceVariant,
                    com.google.android.material.R.attr.colorOnSurfaceVariant
                )
            }
        }

        private fun calculateProgress(report: Report): Int {
            return when (report.type) {
                ReportType.APP_BLOCKER     -> minOf(100, (report.count * 100) / 20)
                ReportType.FOCUS_MODE      -> minOf(100, (report.count * 100) / 120)
                ReportType.KEYWORD_BLOCKER -> minOf(100, (report.count * 100) / 15)
                ReportType.VIEW_BLOCKER    -> minOf(100, (report.count * 100) / 25)
                ReportType.REEL_TRACKER   -> minOf(100, (report.count * 100) / 30)
                else -> -1
            }
        }

        private fun getProgressLabel(type: ReportType): String {
            return when (type) {
                ReportType.APP_BLOCKER     -> "Blocking Activity"
                ReportType.FOCUS_MODE      -> "Focus Goal (2h)"
                ReportType.KEYWORD_BLOCKER -> "Content Protection"
                ReportType.VIEW_BLOCKER    -> "Shorts Blocked"
                ReportType.REEL_TRACKER   -> "Daily Reel Usage"
                else -> "Progress"
            }
        }

        private fun getProgressColor(type: ReportType, progress: Int): Int {
            val context = itemView.context
            return when (type) {
                ReportType.FOCUS_MODE ->
                    // More focus = greener
                    if (progress >= 50) ContextCompat.getColor(context, R.color.md_theme_primary)
                    else ContextCompat.getColor(context, android.R.color.holo_orange_dark)
                ReportType.REEL_TRACKER ->
                    // High reel usage = more orange/red warning
                    if (progress < 50) ContextCompat.getColor(context, R.color.md_theme_primary)
                    else ContextCompat.getColor(context, android.R.color.holo_orange_dark)
                else -> ContextCompat.getColor(context, R.color.md_theme_primary)
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
