package com.alhaq.deenshield.ui.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.alhaq.deenshield.utils.NotificationHelper

/**
 * BroadcastReceiver that triggers daily report notifications
 * at the scheduled time set by the user in RemindersActivity.
 */
class DailyReportReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        // Check if daily reports are still enabled
        val sharedPreferences = context.getSharedPreferences("reminder_settings", Context.MODE_PRIVATE)
        val isEnabled = sharedPreferences.getBoolean("daily_report_enabled", false)
        
        if (isEnabled) {
            // Show the daily report notification
            val notificationHelper = NotificationHelper(context)
            notificationHelper.showDailyReportNotification()
        }
    }
}
