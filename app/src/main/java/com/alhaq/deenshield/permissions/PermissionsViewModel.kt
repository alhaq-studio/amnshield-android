package com.alhaq.deenshield.permissions

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PermissionsViewModel(context: Context) : ViewModel() {

    val permissionsManager = PermissionsManager(context)

    private val _permissionsState = MutableLiveData<PermissionsState>()
    val permissionsState: LiveData<PermissionsState> = _permissionsState

    fun checkPermissions() {
        _permissionsState.value = PermissionsState(
            isAccessibilityEnabled = permissionsManager.isAccessibilityServiceEnabled(),
            isDeviceAdminEnabled = permissionsManager.isDeviceAdminEnabled(),
            isDrawOverOtherAppsEnabled = permissionsManager.isDrawOverOtherAppsEnabled(),
            isUsageStatsEnabled = permissionsManager.isUsageStatsPermissionGranted(),
            areNotificationsEnabled = permissionsManager.areNotificationsEnabled()
        )
    }
}

data class PermissionsState(
    val isAccessibilityEnabled: Boolean = false,
    val isDeviceAdminEnabled: Boolean = false,
    val isDrawOverOtherAppsEnabled: Boolean = false,
    val isUsageStatsEnabled: Boolean = false,
    val areNotificationsEnabled: Boolean = false
)