package com.alhaq.amnshield.data.blockers

/**
 * Unified schedule rule that controls blocker feature toggles (on/off) in a recurring window.
 */
data class UnifiedFeatureScheduleRule(
    val id: String,
    val title: String,
    val type: RuleType,
    val recurrence: Recurrence,
    val targets: Set<FeatureTarget>,
    val startMinute: Int = 0,
    val endMinute: Int = 0,
    val selectedDays: Set<Int> = emptySet(),
    val durationHours: Int = 0,
    val activeUntilMillis: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val groupId: String? = null,
    val groupTitle: String? = null,
    val isEnabled: Boolean? = true
) {
    val isRuleEnabled: Boolean
        get() = isEnabled ?: true

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

    enum class FeatureTarget {
        APP_BLOCKER,
        KEYWORD_BLOCKER,
        REEL_BLOCKER,
        FOCUS_MODE,
        WEBSITE_BLOCKER
    }
}
