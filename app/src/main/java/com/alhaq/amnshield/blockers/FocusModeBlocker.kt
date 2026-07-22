package com.alhaq.amnshield.blockers

import com.alhaq.amnshield.Constants
import com.alhaq.amnshield.utils.SavedPreferencesLoader

class FocusModeBlocker : BaseBlocker() {

    companion object {
        // Essential system apps that should NEVER be blocked to prevent system instability
        val ESSENTIAL_SYSTEM_APPS = setOf(
            "com.android.settings",
            "com.google.android.settings",
            "com.sec.android.app.sbrowser", // Allow browser so focus mode settings/warning can load helper pages if needed, wait, let's keep it same
            "com.sec.android.app.launcher",
            "com.samsung.android.settings",
            "com.coloros.settings",
            "com.android.systemui",
            "com.google.android.apps.nexuslauncher",
            "com.android.launcher",
            "com.android.launcher3",
            "com.sec.android.app.launcher",
            "com.huawei.android.launcher",
            "com.miui.mihome2",
            "com.mi.android.globallauncher",
            "com.android.dialer",
            "com.android.phone",
            "com.android.contacts",
            "com.android.mms",
            "com.android.messaging",
            "com.google.android.dialer",
            "com.google.android.contacts",
            "com.samsung.android.dialer",
            "com.samsung.android.contacts",
            "com.alhaq.amnshield", // AmnShield itself
            
            // OEM Security/Phone Managers
            "com.huawei.systemmanager",
            "com.miui.securitycenter",
            "com.iqoo.secure",
            "com.oppo.safe",
            "com.oneplus.security",
            "com.vivo.permissionmanager",
            "com.samsung.android.lool",
            "com.samsung.android.sm",
            "com.samsung.android.sm_cn",
            "com.coloros.safecenter",
            "com.coloros.securityguard",

            // System Keyboards (required to type)
            "com.google.android.inputmethod.latin",
            "com.samsung.android.honeyboard",
            "com.android.inputmethod.latin",
            "com.huawei.android.inputmethod",
            "com.miui.miinput",

            // Clocks & Alarms
            "com.google.android.deskclock",
            "com.sec.android.app.clockpackage",
            "com.android.deskclock",
            "com.huawei.deskclock",
            "com.miui.clock",
            "com.coloros.alarm",
            "com.oppo.alarm",

            // Calendars
            "com.google.android.calendar",
            "com.android.calendar",
            "com.samsung.android.calendar",
            "com.miui.calendar",

            // Calculators
            "com.google.android.calculator",
            "com.sec.android.app.popupcalculator",
            "com.android.calculator2",
            "com.miui.calculator",
            "com.coloros.calculator",

            // File Managers / Documents Provider
            "com.google.android.apps.nbu.files",
            "com.sec.android.app.myfiles",
            "com.android.documentsui",
            "com.mi.android.globalFileexplorer",
            "com.coloros.filemanager",

            // System Package Installer & Google Play Services
            "com.google.android.packageinstaller",
            "com.android.packageinstaller",
            "com.google.android.permissioncontroller",
            "com.android.vending",
            "com.google.android.gms"
        )
    }

    var focusModeData = FocusModeData()

    fun update(data: FocusModeData) {
        this.focusModeData = data
    }

    /**
     * Check if app app needs to blocked for reasons related to focus mode
     *
     * @param packageName
     * @param savedPreferencesLoader
     * @param defaultLauncher
     * @return
     */
    fun doesAppNeedToBeBlocked(
        context: android.content.Context,
        packageName: String,
        savedPreferencesLoader: SavedPreferencesLoader,
        defaultLauncher: String? = null
    ): FocusModeResult {
        // NEVER block essential system apps to prevent system instability
        if (ESSENTIAL_SYSTEM_APPS.contains(packageName) ||
            packageName.equals("com.alhaq.amnshield", ignoreCase = true) ||
            packageName.equals("com.alhaq.deenshield", ignoreCase = true) ||
            packageName.startsWith("com.alhaq.deenshield.", ignoreCase = true) ||
            (defaultLauncher != null && packageName == defaultLauncher)
        ) {
            return FocusModeResult(isBlocked = false)
        }

        // 1. Check if manual focus mode is turned on
        if (focusModeData.isTurnedOn) {
            if (focusModeData.endTime in 1 until System.currentTimeMillis()) {
                focusModeData.isTurnedOn = false
                return FocusModeResult(isBlocked = false, isRequestingToUpdateSPData = true)
            }
            return evaluateBlocking(packageName, focusModeData.modeType, focusModeData.selectedApps, focusModeData.endTime)
        }

        // 2. AutoFocus schedule active evaluation
        val configuredFocusData = savedPreferencesLoader.getFocusModeData()
        return evaluateBlocking(packageName, configuredFocusData.modeType, configuredFocusData.selectedApps, -1)
    }

    private fun evaluateBlocking(packageName: String, modeType: Int, selectedApps: Set<String>, endTime: Long): FocusModeResult {
        when (modeType) {
            Constants.FOCUS_MODE_BLOCK_SELECTED -> {
                if (selectedApps.contains(packageName)) {
                    return FocusModeResult(
                        isBlocked = true,
                        focusModeEndTime = endTime
                    )
                }
            }
            Constants.FOCUS_MODE_BLOCK_ALL_EX_SELECTED -> {
                if (!selectedApps.contains(packageName)) {
                    return FocusModeResult(
                        isBlocked = true,
                        focusModeEndTime = endTime
                    )
                }
            }
        }
        return FocusModeResult(isBlocked = false)
    }

    /**
     * Stores information related to manual focus mode
     *
     * @property isTurnedOn
     * @property endTime specifies when manual focus hours ends. -1 if not under manual focus hours
     * @property modeType Can either be of type [Constants.FOCUS_MODE_BLOCK_ALL_EX_SELECTED] or [Constants.FOCUS_MODE_BLOCK_SELECTED].
     * @property selectedApps
     */

    data class FocusModeData(
        var isTurnedOn: Boolean = false,
        val endTime: Long = -1,
        val modeType: Int = Constants.FOCUS_MODE_BLOCK_ALL_EX_SELECTED,
        var selectedApps: HashSet<String> = hashSetOf()
    )

    /**
     * Focus mode blocker check result
     *
     * @property isBlocked
     * @property focusModeEndTime specifies when focus mode ends. returns -1 if not in focus mode.
     * @property isRequestingToUpdateSPData returns true if focusModeData in shared preference needs to be updated because focus mode has ended
     */
    data class FocusModeResult(
        val isBlocked: Boolean,
        val focusModeEndTime: Long = -1,
        val isRequestingToUpdateSPData: Boolean = false
    )
    private fun isSystemApp(context: android.content.Context, packageName: String): Boolean {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (e: Exception) {
            false
        }
    }

}