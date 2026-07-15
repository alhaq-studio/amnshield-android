package com.alhaq.amnshield

import android.app.Application
import com.alhaq.amnshield.utils.NotificationManager

class AmnShield: Application() {
  override fun onCreate() {
    super.onCreate()
    Thread.setDefaultUncaughtExceptionHandler(CrashLogger(this))
    NotificationManager(this).createNotificationChannels()
  }
}
