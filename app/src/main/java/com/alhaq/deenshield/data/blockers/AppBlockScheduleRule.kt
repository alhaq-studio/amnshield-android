package com.alhaq.deenshield.data.blockers

data class AppBlockScheduleRule(
    val id: String,
    val title: String,
    val packageName: String,
    val type: RuleType,
    val recurrence: Recurrence,
    val startMinute: Int = 0,
    val endMinute: Int = 0,
    val selectedDays: Set<Int> = emptySet(),
    val durationHours: Int = 0,
    val activeUntilMillis: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
) {
    enum class RuleType {
        BLOCK,
        CHEAT
    }

    enum class Recurrence {
        HOURLY,
        DAILY,
        WEEKLY,
        ALWAYS
    }
}
