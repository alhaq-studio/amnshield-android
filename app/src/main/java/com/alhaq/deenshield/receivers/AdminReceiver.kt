package com.alhaq.deenshield.receivers

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.alhaq.deenshield.Constants
import com.alhaq.deenshield.R
import java.util.Calendar

class AdminReceiver : DeviceAdminReceiver() {
    
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        val antiUninstallInfo = context.getSharedPreferences("anti_uninstall", Context.MODE_PRIVATE)
        val isAntiUninstallOn = antiUninstallInfo.getBoolean("is_anti_uninstall_on", false)
        
        if (isAntiUninstallOn) {
            val mode = antiUninstallInfo.getInt("mode", -1)
            
            return when (mode) {
                Constants.ANTI_UNINSTALL_PASSWORD_MODE -> {
                    context.getString(R.string.anti_uninstall_password_mode_warning)
                }
                Constants.ANTI_UNINSTALL_TIMED_MODE -> {
                    val unlockAtMillis = antiUninstallInfo.getLong("unlock_at_millis", 0L)
                    if (unlockAtMillis > 0L) {
                        val remainingMillis = unlockAtMillis - System.currentTimeMillis()
                        if (remainingMillis > 0L) {
                            val daysDiff = kotlin.math.ceil(
                                remainingMillis / (1000.0 * 60.0 * 60.0 * 24.0)
                            ).toInt().coerceAtLeast(1)
                            context.getString(R.string.anti_uninstall_timed_mode_warning, daysDiff)
                        } else {
                            antiUninstallInfo.edit().putBoolean("is_anti_uninstall_on", false).apply()
                            context.getString(R.string.anti_uninstall_timed_mode_expired)
                        }
                    } else {
                        val dateString = antiUninstallInfo.getString("date", null)
                        if (dateString != null) {
                        try {
                            val parts = dateString.split("/")
                            val selectedDate = Calendar.getInstance()
                            selectedDate.set(
                                parts[2].toInt(),
                                parts[0].toInt() - 1,
                                parts[1].toInt(),
                                0, 0, 0
                            )
                            selectedDate.set(Calendar.MILLISECOND, 0)
                            
                            val today = Calendar.getInstance()
                            today.set(Calendar.HOUR_OF_DAY, 0)
                            today.set(Calendar.MINUTE, 0)
                            today.set(Calendar.SECOND, 0)
                            today.set(Calendar.MILLISECOND, 0)
                            
                            val daysDiff = ((selectedDate.timeInMillis - today.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
                            
                            if (daysDiff > 0) {
                                context.getString(R.string.anti_uninstall_timed_mode_warning, daysDiff)
                            } else {
                                // Auto-disable if expired
                                antiUninstallInfo.edit().putBoolean("is_anti_uninstall_on", false).apply()
                                context.getString(R.string.anti_uninstall_timed_mode_expired)
                            }
                        } catch (e: Exception) {
                            context.getString(R.string.anti_uninstall_is_active)
                        }
                        } else {
                            context.getString(R.string.anti_uninstall_is_active)
                        }
                    }
                }
                else -> {
                    context.getString(R.string.anti_uninstall_is_active)
                }
            }
        }
        
        // Return default message if anti-uninstall is not enabled
        return super.onDisableRequested(context, intent) ?: ""
    }
    
    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        
        // If device admin was disabled, turn off anti-uninstall
        val antiUninstallInfo = context.getSharedPreferences("anti_uninstall", Context.MODE_PRIVATE)
        val isAntiUninstallOn = antiUninstallInfo.getBoolean("is_anti_uninstall_on", false)
        
        if (isAntiUninstallOn) {
            antiUninstallInfo.edit().putBoolean("is_anti_uninstall_on", false).apply()
            Toast.makeText(context, R.string.anti_uninstall_removed, Toast.LENGTH_SHORT).show()
        }
    }
}