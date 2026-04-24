package com.alhaq.deenshield.ui.dto

data class Report(
    val title: String,
    val summary: String,
    val count: Int = 0,
    val type: ReportType = ReportType.GENERAL,
    val detailedStats: List<ReportDetail> = emptyList(),
    val additionalInfo: String? = null
)

data class ReportDetail(
    val label: String,
    val value: String
)

enum class ReportType {
    APP_BLOCKER,
    KEYWORD_BLOCKER,
    VIEW_BLOCKER,
    FOCUS_MODE,
    GENERAL
}
