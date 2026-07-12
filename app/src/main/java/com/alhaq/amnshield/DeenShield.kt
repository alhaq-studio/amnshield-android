package com.alhaq.amnshield

import android.app.Application
import com.alhaq.amnshield.utils.NotificationManager
import com.google.android.material.color.DynamicColors

class DeenShield: Application() {
  override fun onCreate() {
    super.onCreate()
    DynamicColors.applyToActivitiesIfAvailable(this)
    Thread.setDefaultUncaughtExceptionHandler(CrashLogger(this))
    NotificationManager(this).createNotificationChannels()
  }
}
