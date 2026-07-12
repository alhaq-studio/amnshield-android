package com.alhaq.deenshield.data.blockers

import java.util.Calendar

/**
 * Data model for per-app launch/opens limiting rules.
 * Tracks how many times an app can be launched within a specific time period.
 * When the limit is reached, the app is blocked by AppBlocker enforcement logic.
 *
 * @param id Unique identifier (UUID)
 * @param packageName Target app's package name
 * @param maxLaunches Maximum number of launches allowed per period
 * @param timePeriod Time period for limit reset (HOURLY, DAILY, WEEKLY)
 * @param dayOfWeek For WEEKLY: Calendar.MONDAY (2) through SUNDAY (1), ignored for others
 * @param createdAt Timestamp when this rule was created
 */
data class AppLaunchLimitRule(
    val id: String,
    val packageName: String,
    val maxLaunches: Int,
    val timePeriod: TimePeriod,
    val dayOfWeek: Int = Calendar.getInstance().get(Calendar.DAY_OF_WEEK),
    val createdAt: Long = System.currentTimeMillis()
) {
    enum class TimePeriod {
        HOURLY,
        DAILY,
        WEEKLY
    }

    /**
     * Get a human-readable description of this limit rule.
     * Example: "5 launches per day" or "10 launches per week"
     */
    fun getDescription(): String {
        val periodText = when (timePeriod) {
            TimePeriod.HOURLY -> "hour"
            TimePeriod.DAILY -> "day"
            TimePeriod.WEEKLY -> "week"
        }
        return "$maxLaunches launch${if (maxLaunches != 1) "es" else ""} per $periodText"
    }
}
