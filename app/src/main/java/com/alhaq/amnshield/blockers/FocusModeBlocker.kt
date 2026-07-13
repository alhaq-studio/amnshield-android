package com.alhaq.amnshield.blockers

import com.alhaq.amnshield.Constants

class FocusModeBlocker : BaseBlocker() {

    companion object {
        // Essential system apps that should NEVER be blocked to prevent system instability
        val ESSENTIAL_SYSTEM_APPS = setOf(
            "com.android.settings",
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
            "com.alhaq.amnshield" // AmnShield itself
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
     * @return
     */
    fun doesAppNeedToBeBlocked(packageName: String, defaultLauncher: String? = null): FocusModeResult {
        // NEVER block essential system apps to prevent system instability
        if (ESSENTIAL_SYSTEM_APPS.contains(packageName) || (defaultLauncher != null && packageName == defaultLauncher)) {
            return FocusModeResult(isBlocked = false)
        }

        // responsible for checking if manual focus mode is turned on
        if (focusModeData.isTurnedOn) {
            if (focusModeData.endTime < System.currentTimeMillis()) {
                focusModeData.isTurnedOn = false
                return FocusModeResult(isBlocked = false, isRequestingToUpdateSPData = true)
            }
            when (focusModeData.modeType) {
                Constants.FOCUS_MODE_BLOCK_SELECTED -> {
                    if (focusModeData.selectedApps.contains(packageName)) {
                        return FocusModeResult(
                            isBlocked = true,
                            focusModeEndTime = focusModeData.endTime
                        )
                    }
                }

                Constants.FOCUS_MODE_BLOCK_ALL_EX_SELECTED -> {
                    if (!focusModeData.selectedApps.contains(packageName)) {
                        return FocusModeResult(
                            isBlocked = true,
                            focusModeEndTime = focusModeData.endTime
                        )
                    }
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


}