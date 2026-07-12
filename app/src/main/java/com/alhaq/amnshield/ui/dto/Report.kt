package com.alhaq.amnshield.ui.dto

import com.alhaq.amnshield.R

data class Report(
    val title: String,
    val summary: String,
    val count: Int = 0,
    val yesterdayCount: Int = 0,
    val type: ReportType = ReportType.GENERAL,
    val detailedStats: List<ReportDetail> = emptyList(),
    val additionalInfo: String? = null
)

data class ReportDetail(
    val label: String,
    val value: String
)

enum class ReportType(val iconRes: Int) {
    APP_BLOCKER(R.drawable.baseline_app_shortcut_24),
    KEYWORD_BLOCKER(R.drawable.baseline_lock_24),
    VIEW_BLOCKER(R.drawable.baseline_stop_24),
    REEL_TRACKER(R.drawable.baseline_query_stats_24),
    FOCUS_MODE(R.drawable.ic_focus_mode),
    GENERAL(R.drawable.baseline_info_24)
}
