package com.alhaq.amnshield.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.alhaq.amnshield.data.blockers.PackageWand
import com.alhaq.amnshield.services.DeenShieldAccessibilityService
import com.alhaq.amnshield.utils.SavedPreferencesLoader

/**
 * Receives broadcast when new apps are installed and automatically blocks them
 * if they belong to selected categories (e.g., Games, Social Media, Dating, etc.)
 */
class AppInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        
        // Only process package additions
        if (intent.action != Intent.ACTION_PACKAGE_ADDED) return
        
        val prefsLoader = SavedPreferencesLoader(context)
        
        // Check if auto-block is enabled
        if (!prefsLoader.isAutoBlockEnabled()) return
        
        // Get the package name of the newly installed app
        val packageName = intent.data?.schemeSpecificPart ?: return
        
        // Don't auto-block DeenShield itself
        if (packageName == "com.alhaq.amnshield") return
        
        // Get enabled auto-block categories
        val autoBlockCategories = prefsLoader.getAutoBlockCategories()
        if (autoBlockCategories.isEmpty()) return
        
        // Get app info for enhanced category detection
        val appInfo = try {
            context.packageManager.getApplicationInfo(packageName, 0)
        } catch (e: Exception) {
            null
        }
        
        // Check if this app belongs to any selected category
        val appCategory = PackageWand.getCategoryForPackage(packageName, appInfo)
        
        if (appCategory != null && autoBlockCategories.contains(appCategory)) {
            // Add to blocked apps list
            val currentBlockedApps = prefsLoader.loadBlockedApps().toMutableSet()
            currentBlockedApps.add(packageName)
            prefsLoader.saveBlockedApps(currentBlockedApps)
            
            // Refresh the accessibility service (scoped to our package only)
            val refreshIntent = Intent(DeenShieldAccessibilityService.INTENT_ACTION_REFRESH_APP_BLOCKER)
                .setPackage(context.packageName)
            context.sendBroadcast(refreshIntent)
            
            Log.i("AppInstallReceiver", "Auto-blocked newly installed app: $packageName (category: $appCategory)")
        }
    }
}
