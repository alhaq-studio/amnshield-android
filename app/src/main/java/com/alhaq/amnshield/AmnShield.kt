package com.alhaq.amnshield

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.alhaq.amnshield.utils.NotificationManager

class AmnShield: Application(), Application.ActivityLifecycleCallbacks {
    private var startedActivitiesCount = 0

    companion object {
        var isAppUnlocked = false
        var isBypassUnlocked = false
    }

    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler(CrashLogger(this))
        NotificationManager(this).createNotificationChannels()
        registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {
        startedActivitiesCount++
    }
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {
        startedActivitiesCount--
        if (startedActivitiesCount == 0) {
            isAppUnlocked = false
            isBypassUnlocked = false
        }
    }
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
