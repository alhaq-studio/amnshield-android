package com.alhaq.amnshield.ui.state

import androidx.compose.ui.graphics.Color

enum class AppTheme {
    SUNSET_GLOW,
    EMERALD_CALM,
    COSMIC_NIGHT
}

data class BlockedApp(
    val packageName: String,
    val name: String,
    val iconUrl: String? = null,
    val usageMinutes: Int = 0,
    val maxLaunches: Int = 0,
    val currentLaunches: Int = 0,
    val isCategoryBlocked: Boolean = false
)

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val timeAgo: String,
    val isToday: Boolean,
    val iconType: String // "error", "chart", "warning", "info"
)

data class ScreenTimeDay(
    val dayOfWeek: String, // "Mon", "Tue", etc.
    val minutes: Int
)

data class ReelsScrollHour(
    val hour: Int, // 6, 12, 18, 24
    val value: Int
)

data class AmnShieldState(
    // Onboarding
    val isSetupComplete: Boolean = false,
    val permissionAccessibilityDone: Boolean = false,
    val permissionBackgroundDone: Boolean = false,
    val permissionNotificationsDone: Boolean = false,

    // Active theme
    val currentTheme: AppTheme = AppTheme.SUNSET_GLOW,

    // Core Protection Feature Switches
    val isAppBlockerEnabled: Boolean = true,
    val isReelsBlockerEnabled: Boolean = true,
    val isKeywordBlockerEnabled: Boolean = true,
    val isWebFilterEnabled: Boolean = true,
    val isScheduleEnabled: Boolean = false,
    val isUsageLimitEnabled: Boolean = true,
    val isFocusModeActive: Boolean = false,

    // Stats
    val distractionsBlocked: Int = 42,
    val focusTimeMinutes: Int = 165, // 2h 45m
    val totalReelsWatched: Int = 2303,
    val averageWatchSeconds: Int = 13, // 13 seconds/video

    // App & Website Usage Log list
    val appsUsage: List<BlockedApp> = listOf(
        BlockedApp("com.activision.callofduty", "Call of Duty", usageMinutes = 10, currentLaunches = 6, maxLaunches = 10),
        BlockedApp("com.google.android", "Google", usageMinutes = 5, currentLaunches = 4, maxLaunches = 5),
        BlockedApp("com.google.android.youtube", "YouTube", usageMinutes = 120, currentLaunches = 12, maxLaunches = 15),
        BlockedApp("com.instagram.android", "Instagram", usageMinutes = 45, currentLaunches = 8, maxLaunches = 15),
        BlockedApp("com.twitter.android", "X", usageMinutes = 15, currentLaunches = 3, maxLaunches = 10)
    ),

    val customBlockedWebsites: List<String> = listOf(
        "socialmedia.com",
        "distractor.org",
        "timewaster.net"
    ),

    // Keywords Blocker
    val keywords: List<String> = listOf("porn*", "*.xxx", "nsfw", "gambling", "betting"),

    // Screen Time history (Mon - Sun)
    val weeklyScreenTime: List<ScreenTimeDay> = listOf(
        ScreenTimeDay("Mon", 45),
        ScreenTimeDay("Tue", 90),
        ScreenTimeDay("Wed", 60),
        ScreenTimeDay("Thu", 130),
        ScreenTimeDay("Fri", 85),
        ScreenTimeDay("Sat", 40),
        ScreenTimeDay("Sun", 15)
    ),

    // Reels daily scroll hour distribution
    val peakReelsScrollHours: List<ReelsScrollHour> = listOf(
        ReelsScrollHour(6, 40),
        ReelsScrollHour(12, 30),
        ReelsScrollHour(18, 75),
        ReelsScrollHour(24, 100)
    ),

    // Notifications list
    val notifications: List<NotificationItem> = listOf(
        NotificationItem("1", "App Blocked", "TikTok was blocked during your Focus Session.", "2m ago", true, "error"),
        NotificationItem("2", "Weekly Report", "Your screen time is down 12% this week!", "1h ago", true, "chart"),
        NotificationItem("3", "Accessibility Status", "Please re-enable service for accurate tracking.", "1d ago", false, "warning"),
        NotificationItem("4", "Digital Wellbeing", "You achieved today's focus milestone of 2 hours.", "2d ago", false, "info")
    ),

    // Modals
    val isShowingForgotPasswordModal: Boolean = false,
    val isShowingAddKeywordModal: Boolean = false,
    val isShowingAddAppModal: Boolean = false,

    // Profile settings
    val userName: String = "Alhaq DST",
    val userEmail: String = "alhaq.dst@gmail.com",
    val userBio: String = "Digital Wellbeing Guardian • Staying mindful & focused.",
    val userGoalMinutes: Int = 120,
    val focusProfileType: String = "Deep Focus",
    val isPinProtectionEnabled: Boolean = false,
    val profilePin: String = ""
)
