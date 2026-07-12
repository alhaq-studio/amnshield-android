package com.alhaq.amnshield.ui.activity

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.alhaq.amnshield.R
import com.alhaq.amnshield.databinding.ActivityRemindersBinding
import com.alhaq.amnshield.utils.NotificationHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.*

class RemindersActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityRemindersBinding
    private lateinit var notificationHelper: NotificationHelper
    private val sharedPreferences by lazy {
        getSharedPreferences("reminder_settings", Context.MODE_PRIVATE)
    }
    
    companion object {
        private const val PREF_DAILY_REPORT_ENABLED = "daily_report_enabled"
        private const val PREF_DAILY_REPORT_HOUR = "daily_report_hour"
        private const val PREF_DAILY_REPORT_MINUTE = "daily_report_minute"
        private const val PREF_FOCUS_REMINDER_ENABLED = "focus_reminder_enabled"
        private const val PREF_ACHIEVEMENT_ENABLED = "achievement_enabled"
        private const val DAILY_REPORT_REQUEST_CODE = 1001
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        com.alhaq.amnshield.utils.ThemeUtils.applyTheme(this)

        super.onCreate(savedInstanceState)
        binding = ActivityRemindersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Notifications & Reminders"
        
        notificationHelper = NotificationHelper(this)
        
        loadSettings()
        setupListeners()
    }
    
    private fun loadSettings() {
        // Load daily report settings
        val dailyReportEnabled = sharedPreferences.getBoolean(PREF_DAILY_REPORT_ENABLED, false)
        val hour = sharedPreferences.getInt(PREF_DAILY_REPORT_HOUR, 20)
        val minute = sharedPreferences.getInt(PREF_DAILY_REPORT_MINUTE, 0)
        
        binding.dailyReportSwitch.isChecked = dailyReportEnabled
        binding.reportTimeContainer.visibility = if (dailyReportEnabled) View.VISIBLE else View.GONE
        updateTimeButtonText(hour, minute)
        
        // Load other settings
        binding.focusReminderSwitch.isChecked = sharedPreferences.getBoolean(PREF_FOCUS_REMINDER_ENABLED, false)
        binding.achievementSwitch.isChecked = sharedPreferences.getBoolean(PREF_ACHIEVEMENT_ENABLED, true)
    }
    
    private fun setupListeners() {
        // Daily report switch
        binding.dailyReportSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !ensureNotificationsEnabled()) {
                binding.dailyReportSwitch.isChecked = false
                return@setOnCheckedChangeListener
            }

            sharedPreferences.edit().putBoolean(PREF_DAILY_REPORT_ENABLED, isChecked).apply()
            binding.reportTimeContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
            
            if (isChecked) {
                checkAlarmPermissionAndSchedule()
            } else {
                cancelDailyReportAlarm()
            }
        }
        
        // Time picker button
        binding.reportTimeButton.setOnClickListener {
            showTimePicker()
        }
        
        // Focus reminder switch
        binding.focusReminderSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !ensureNotificationsEnabled()) {
                binding.focusReminderSwitch.isChecked = false
                return@setOnCheckedChangeListener
            }

            sharedPreferences.edit().putBoolean(PREF_FOCUS_REMINDER_ENABLED, isChecked).apply()
            if (isChecked) {
                Toast.makeText(this, "Focus reminders will appear when you're using distracting apps", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Achievement switch
        binding.achievementSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !ensureNotificationsEnabled()) {
                binding.achievementSwitch.isChecked = false
                return@setOnCheckedChangeListener
            }

            sharedPreferences.edit().putBoolean(PREF_ACHIEVEMENT_ENABLED, isChecked).apply()
        }
        
        // Test notification button
        binding.testNotificationButton.setOnClickListener {
            if (!ensureNotificationsEnabled()) {
                return@setOnClickListener
            }
            testNotification()
        }
    }

    private fun ensureNotificationsEnabled(): Boolean {
        val enabled = NotificationManagerCompat.from(this).areNotificationsEnabled()
        if (enabled) return true

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.notification_permission_title)
            .setMessage(R.string.notification_permission_required)
            .setPositiveButton(R.string.open_settings) { _, _ ->
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                }
                startActivity(intent)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
        return false
    }
    
    private fun showTimePicker() {
        val hour = sharedPreferences.getInt(PREF_DAILY_REPORT_HOUR, 20)
        val minute = sharedPreferences.getInt(PREF_DAILY_REPORT_MINUTE, 0)
        
        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            sharedPreferences.edit()
                .putInt(PREF_DAILY_REPORT_HOUR, selectedHour)
                .putInt(PREF_DAILY_REPORT_MINUTE, selectedMinute)
                .apply()
            
            updateTimeButtonText(selectedHour, selectedMinute)
            scheduleDailyReportAlarm(selectedHour, selectedMinute)
            
            Toast.makeText(this, "Daily report scheduled for ${formatTime(selectedHour, selectedMinute)}", Toast.LENGTH_SHORT).show()
        }, hour, minute, false).show()
    }
    
    private fun updateTimeButtonText(hour: Int, minute: Int) {
        binding.reportTimeButton.text = formatTime(hour, minute)
    }
    
    private fun formatTime(hour: Int, minute: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        
        val hourOfDay = calendar.get(Calendar.HOUR)
        val displayHour = if (hourOfDay == 0) 12 else hourOfDay
        val amPm = if (calendar.get(Calendar.HOUR_OF_DAY) < 12) "AM" else "PM"
        
        return String.format("%02d:%02d (%d:%02d %s)", hour, minute, displayHour, minute, amPm)
    }
    
    private fun checkAlarmPermissionAndSchedule() {
        val hour = sharedPreferences.getInt(PREF_DAILY_REPORT_HOUR, 20)
        val minute = sharedPreferences.getInt(PREF_DAILY_REPORT_MINUTE, 0)
        scheduleDailyReportAlarm(hour, minute)
    }
    
    private fun scheduleDailyReportAlarm(hour: Int, minute: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val intent = Intent(this, DailyReportReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            DAILY_REPORT_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Schedule for next occurrence of the time
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            
            // If the time has passed today, schedule for tomorrow
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        
        // Schedule daily repeating alarm (inexact for battery efficiency)
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
    
    private fun cancelDailyReportAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, DailyReportReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            DAILY_REPORT_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
        Toast.makeText(this, "Daily report alarm cancelled", Toast.LENGTH_SHORT).show()
    }
    
    private fun testNotification() {
        notificationHelper.showDailyReportNotification()
        Toast.makeText(this, "Test notification sent! Check your notification panel.", Toast.LENGTH_LONG).show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}