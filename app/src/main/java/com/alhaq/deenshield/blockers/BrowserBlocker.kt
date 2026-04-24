package com.alhaq.deenshield.blockers

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

class BrowserBlocker(private val service: AccessibilityService) : BaseBlocker() {

    // Cache for packages confirmed as browsers.
    private val cacheBlockedBrowserApps: HashSet<String> = hashSetOf()

    // Cache for packages confirmed as non-browsers.
    private val cacheNotBlockedBrowserApps: HashSet<String> = hashSetOf()

    var isTurnedOn = false

    fun isAppBrowser(event: android.view.accessibility.AccessibilityEvent): Boolean {
        if (!isTurnedOn) return false
        val packageName = event.packageName?.toString() ?: return false

        if (cacheBlockedBrowserApps.contains(packageName)) return true
        if (cacheNotBlockedBrowserApps.contains(packageName)) return false

        val isBrowser = resolveIsBrowser(service, packageName) &&
            !KeywordBlocker.URL_BAR_ID_LIST.containsKey(packageName)

        if (isBrowser) {
            cacheBlockedBrowserApps.add(packageName)
        } else {
            cacheNotBlockedBrowserApps.add(packageName)
        }

        return isBrowser
    }

    private fun resolveIsBrowser(context: Context, packageName: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse("http://www.deenshield.app")
            `package` = packageName
        }

        val activities = context.packageManager.queryIntentActivities(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        return activities.isNotEmpty()
    }
}
