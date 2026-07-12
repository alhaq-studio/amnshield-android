package com.alhaq.amnshield.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.alhaq.amnshield.R
import com.alhaq.amnshield.ui.activity.MainActivity
import com.alhaq.amnshield.ui.activity.ReportsActivity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Manages notifications for daily reports, reminders, and achievements
 */
class NotificationHelper(private val context: Context) {

    private val reminderPrefs by lazy {
        context.getSharedPreferences("reminder_settings", Context.MODE_PRIVATE)
    }

    private val achievementPrefs by lazy {
        context.getSharedPreferences("achievement_notifications", Context.MODE_PRIVATE)
    }

    companion object {
        private const val CHANNEL_ID_REPORTS = "daily_reports"
        private const val CHANNEL_ID_REMINDERS = "reminders"
        private const val CHANNEL_ID_ACHIEVEMENTS = "achievements"
        
        private const val NOTIFICATION_ID_DAILY_REPORT = 1001
        private const val NOTIFICATION_ID_REMINDER = 2001
        private const val NOTIFICATION_ID_ACHIEVEMENT = 3001
        
        @Volatile
        private var instance: NotificationHelper? = null
        
        fun getInstance(context: Context): NotificationHelper {
            return instance ?: synchronized(this) {
                instance ?: NotificationHelper(context.applicationContext).also { instance = it }
            }
        }
    }
    
    init {
        createNotificationChannels()
    }
    
    /**
     * Create notification channels for Android O and above
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Daily Reports Channel
            val reportsChannel = NotificationChannel(
                CHANNEL_ID_REPORTS,
                "Daily Reports",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Receive daily summaries of your blocking activity"
                enableLights(true)
                enableVibration(false)
            }
            
            // Reminders Channel
            val remindersChannel = NotificationChannel(
                CHANNEL_ID_REMINDERS,
                "Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders to stay focused and maintain healthy digital habits"
                enableLights(true)
                enableVibration(true)
            }
            
            // Achievements Channel
            val achievementsChannel = NotificationChannel(
                CHANNEL_ID_ACHIEVEMENTS,
                "Achievements",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Celebrate your productivity milestones"
                enableLights(true)
                enableVibration(false)
            }
            
            notificationManager.createNotificationChannels(
                listOf(reportsChannel, remindersChannel, achievementsChannel)
            )
        }
    }
    
    /**
     * Show daily report notification
     */
    fun showDailyReportNotification(date: LocalDate = LocalDate.now()) {
        if (!reminderPrefs.getBoolean("daily_report_enabled", false)) return

        val reportGenerator = ReportGenerator(context)
        val summaryText = reportGenerator.generateDailySummaryText(date)
        val shortDate = date.format(DateTimeFormatter.ofPattern("MMM d"))
        
        val intent = Intent(context, ReportsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REPORTS)
            .setSmallIcon(R.drawable.ic_logo_mini)
            .setContentTitle("Daily DeenShield Report • $shortDate")
            .setContentText("Tap to review your blocking, focus, and reels summary")
            .setStyle(NotificationCompat.BigTextStyle().bigText(summaryText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        with(NotificationManagerCompat.from(context)) {
            try {
                notify(NOTIFICATION_ID_DAILY_REPORT, notification)
                NotificationInboxStore.add(
                    context,
                    title = "Daily DeenShield Report • $shortDate",
                    message = "Tap to review your blocking, focus, and reels summary",
                    category = NotificationInboxStore.Category.DAILY_REPORT
                )
            } catch (e: SecurityException) {
                // User has not granted notification permission
            }
        }
    }
    
    /**
     * Show focus reminder notification
     */
    fun showFocusReminder(title: String, message: String) {
        if (!reminderPrefs.getBoolean("focus_reminder_enabled", false)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
            .setSmallIcon(R.drawable.ic_logo_mini)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        
        with(NotificationManagerCompat.from(context)) {
            try {
                notify(NOTIFICATION_ID_REMINDER, notification)
                NotificationInboxStore.add(
                    context,
                    title = title,
                    message = message,
                    category = NotificationInboxStore.Category.REMINDER
                )
            } catch (e: SecurityException) {
                // User has not granted notification permission
            }
        }
    }
    
    /**
     * Show achievement notification
     */
    fun showAchievementNotification(achievement: String, description: String) {
        if (!reminderPrefs.getBoolean("achievement_enabled", true)) return

        val todayKey = LocalDate.now().toString()
        val notificationKey = "${achievement.lowercase()}_$todayKey"
        if (achievementPrefs.getBoolean(notificationKey, false)) return

        val intent = Intent(context, ReportsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ACHIEVEMENTS)
            .setSmallIcon(R.drawable.ic_logo_mini)
            .setContentTitle("🎉 Achievement Unlocked!")
            .setContentText(achievement)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$achievement\n\n$description"))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        with(NotificationManagerCompat.from(context)) {
            try {
                notify(NOTIFICATION_ID_ACHIEVEMENT, notification)
                achievementPrefs.edit().putBoolean(notificationKey, true).apply()
                NotificationInboxStore.add(
                    context,
                    title = "🎉 $achievement",
                    message = description,
                    category = NotificationInboxStore.Category.ACHIEVEMENT
                )
            } catch (e: SecurityException) {
                // User has not granted notification permission
            }
        }
    }
    
    /**
     * Check achievements based on blocking stats
     */
    fun checkAndNotifyAchievements() {
        val statsManager = BlockingStatsManager.getInstance(context)
        val summary = statsManager.getStatsSummaryForDate(LocalDate.now())
        
        // Check for milestones
        when {
            summary.appBlocksCount == 10 -> {
                showAchievementNotification(
                    "Productivity Warrior",
                    "You've resisted 10 app distractions today!"
                )
            }
            summary.totalFocusMinutes >= 60 -> {
                showAchievementNotification(
                    "Focus Master",
                    "You've completed 1 hour of focused work today!"
                )
            }
            summary.totalFocusMinutes >= 120 -> {
                showAchievementNotification(
                    "Deep Work Champion",
                    "You've achieved 2 hours of deep focus today!"
                )
            }
            summary.keywordBlocksCount >= 20 -> {
                showAchievementNotification(
                    "Content Guardian",
                    "You've blocked 20+ harmful keywords today!"
                )
            }
            summary.viewBlocksCount >= 30 -> {
                showAchievementNotification(
                    "Time Saver",
                    "You've avoided 30+ time-wasting reels/views today!"
                )
            }
        }
    }
    
    /**
     * Show custom reminder
     */
    fun showCustomReminder(title: String, message: String, iconResId: Int = R.drawable.ic_logo_mini) {
        showFocusReminder(title, message)
    }
}
