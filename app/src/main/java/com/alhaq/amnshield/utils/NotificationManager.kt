package com.alhaq.amnshield.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class NotificationManager(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val dailyReportChannel = NotificationChannel(
                DAILY_REPORT_CHANNEL_ID,
                "Daily Reports",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily report reminders"
            }
            notificationManager.createNotificationChannel(dailyReportChannel)

            val blockIssuesChannel = NotificationChannel(
                BLOCK_ISSUES_CHANNEL_ID,
                "Block Issues",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Block issues reminders"
            }
            notificationManager.createNotificationChannel(blockIssuesChannel)

            val subscriptionChannel = NotificationChannel(
                SUBSCRIPTION_CHANNEL_ID,
                "Subscription",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Subscription reminders"
            }
            notificationManager.createNotificationChannel(subscriptionChannel)
        }
    }

    companion object {
        const val DAILY_REPORT_CHANNEL_ID = "daily_report_channel"
        const val BLOCK_ISSUES_CHANNEL_ID = "block_issues_channel"
        const val SUBSCRIPTION_CHANNEL_ID = "subscription_channel"
    }
}