package com.alhaq.deenshield.utils

import android.accessibilityservice.AccessibilityService
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.alhaq.deenshield.receivers.AdminReceiver

/**
 * Helper class to guide users through granting permissions with automated navigation
 */
class PermissionGuideHelper(private val activity: Activity) {
    
    private val context: Context = activity.applicationContext
    private val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val componentName = ComponentName(context, AdminReceiver::class.java)
    
    // SharedPreferences for tracking permission states
    private val prefs = context.getSharedPreferences("permission_guide_prefs", Context.MODE_PRIVATE)
    
    /**
     * Check if accessibility service is enabled
     */
    fun isAccessibilityEnabled(serviceClass: Class<out AccessibilityService>): Boolean {
        val service = "${context.packageName}/${serviceClass.name}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(service) == true
    }
    
    /**
     * Check if usage stats permission is granted
     */
    fun isUsageStatsGranted(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }
    
    /**
     * Check if device admin permission is granted
     */
    fun isDeviceAdminGranted(): Boolean {
        return devicePolicyManager.isAdminActive(componentName)
    }
    
    /**
     * Check if overlay permission is granted
     */
    fun isOverlayGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }
    
    /**
     * Check if notification permission is granted (Android 13+)
     */
    fun isNotificationGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } else {
            true // Not required on older versions
        }
    }
    
    /**
     * Update all permission states in SharedPreferences
     */
    fun updatePermissionStates() {
        prefs.edit().apply {
            putBoolean("usage_stats", isUsageStatsGranted())
            putBoolean("device_admin", isDeviceAdminGranted())
            putBoolean("overlay", isOverlayGranted())
            putBoolean("notification", isNotificationGranted())
            apply()
        }
    }
    
    /**
     * Open app settings with guidance
     */
    fun openAppSettings() {
        Toast.makeText(
            context,
            "Opening app settings. Look for 'Permissions' in the menu.",
            Toast.LENGTH_LONG
        ).show()
        
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to general settings
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
    
    /**
     * Open accessibility settings with guidance for restricted settings
     */
    fun openAccessibilitySettings() {
        Toast.makeText(
            context,
            "Find 'DeenShield' in the list and enable it. If you don't see it, tap the 3-dot menu (⋮) at the top-right corner and enable 'Show restricted settings'",
            Toast.LENGTH_LONG
        ).show()
        
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            openAppSettings()
        }
    }
    
    /**
     * Open usage stats settings
     */
    fun openUsageStatsSettings() {
        Toast.makeText(
            context,
            "Find 'DeenShield' in the list and allow usage access",
            Toast.LENGTH_LONG
        ).show()
        
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            openAppSettings()
        }
    }
    
    /**
     * Open overlay permission settings
     */
    fun openOverlaySettings() {
        Toast.makeText(
            context,
            "Allow 'Display over other apps' permission for DeenShield",
            Toast.LENGTH_LONG
        ).show()
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            openAppSettings()
        }
    }
    
    /**
     * Request device admin permission
     */
    fun requestDeviceAdmin() {
        Toast.makeText(
            context,
            "Grant device admin permission to enable anti-uninstall protection",
            Toast.LENGTH_LONG
        ).show()
        
        try {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "DeenShield needs device admin permission to prevent accidental uninstallation during focus sessions"
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            openAppSettings()
        }
    }
    
    /**
     * Get list of missing permissions
     */
    fun getMissingPermissions(): List<String> {
        val missing = mutableListOf<String>()
        
        if (!isUsageStatsGranted()) {
            missing.add("Usage Stats")
        }
        if (!isDeviceAdminGranted()) {
            missing.add("Device Admin")
        }
        if (!isOverlayGranted()) {
            missing.add("Display Over Apps")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isNotificationGranted()) {
            missing.add("Notifications")
        }
        
        return missing
    }
    
    /**
     * Check if all required permissions are granted
     */
    fun areAllPermissionsGranted(): Boolean {
        return isUsageStatsGranted() &&
                isDeviceAdminGranted() &&
                isOverlayGranted() &&
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || isNotificationGranted())
    }
    
    /**
     * Guide user through complete permission setup
     */
    fun guidePermissionSetup() {
        val missing = getMissingPermissions()
        
        if (missing.isEmpty()) {
            Toast.makeText(context, "All permissions granted!", Toast.LENGTH_SHORT).show()
            return
        }
        
        val message = "Missing permissions: ${missing.joinToString(", ")}"
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        
        // Guide to the first missing permission
        when (missing.first()) {
            "Usage Stats" -> openUsageStatsSettings()
            "Device Admin" -> requestDeviceAdmin()
            "Display Over Apps" -> openOverlaySettings()
            "Notifications" -> openAppSettings()
        }
    }
}
