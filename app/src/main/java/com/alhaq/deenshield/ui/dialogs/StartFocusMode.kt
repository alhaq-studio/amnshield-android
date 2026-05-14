package com.alhaq.deenshield.ui.dialogs

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.alhaq.deenshield.Constants
import com.alhaq.deenshield.R
import com.alhaq.deenshield.blockers.FocusModeBlocker
import com.alhaq.deenshield.databinding.DialogFocusModeBinding
import com.alhaq.deenshield.services.DeenShieldAccessibilityService
import com.alhaq.deenshield.utils.NotificationTimerManager
import com.alhaq.deenshield.utils.SavedPreferencesLoader

class StartFocusMode(
    savedPreferencesLoader: SavedPreferencesLoader,
    private val onPositiveButtonPressed: () -> Unit
) : BaseDialog(savedPreferencesLoader) {

    private var pendingFocusDuration: Long? = null
    private var pendingFocusMode: Int? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Inflate the custom dialog layout
        val dialogFocusModeBinding = DialogFocusModeBinding.inflate(layoutInflater)
        val previousData = savedPreferencesLoader?.getFocusModeData()
        dialogFocusModeBinding.focusModeMinsPicker.setValue(3)
        dialogFocusModeBinding.focusModeMinsPicker.minValue = 2

        var selectedMode = previousData?.modeType ?: Constants.FOCUS_MODE_BLOCK_SELECTED
        if (previousData != null) {
            when (previousData.modeType) {
                Constants.FOCUS_MODE_BLOCK_SELECTED -> dialogFocusModeBinding.blockSelected.isChecked =
                    true

                Constants.FOCUS_MODE_BLOCK_ALL_EX_SELECTED -> dialogFocusModeBinding.blockAll.isChecked =
                    true
            }
        }

        dialogFocusModeBinding.modeType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                dialogFocusModeBinding.blockAll.id -> selectedMode =
                    Constants.FOCUS_MODE_BLOCK_ALL_EX_SELECTED

                dialogFocusModeBinding.blockSelected.id -> selectedMode =
                    Constants.FOCUS_MODE_BLOCK_SELECTED
            }
        }
        return MaterialAlertDialogBuilder(requireContext())
            .setView(dialogFocusModeBinding.root)
            .setPositiveButton(getString(R.string.start)) { _, _ ->
                val totalMillis = dialogFocusModeBinding.focusModeMinsPicker.getValue() * 60000L
                val mode = selectedMode
                if (mode != null && ensureNotificationPermission(totalMillis, mode)) {
                    startFocusModeSession(totalMillis, mode)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            val granted = grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (granted) {
                val duration = pendingFocusDuration
                val mode = pendingFocusMode
                if (duration != null && mode != null) {
                    startFocusModeSession(duration, mode)
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.notification_permission_required),
                    Toast.LENGTH_SHORT
                ).show()
            }
            pendingFocusDuration = null
            pendingFocusMode = null
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun ensureNotificationPermission(duration: Long, mode: Int): Boolean {
        if (hasNotificationPermission()) {
            return true
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pendingFocusDuration = duration
            pendingFocusMode = mode
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
        } else {
            showNotificationPermissionDialog()
        }
        return false
    }

    private fun hasNotificationPermission(): Boolean {
        val notificationsEnabled = NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return notificationsEnabled
        }
        val postNotificationsGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        return notificationsEnabled && postNotificationsGranted
    }

    private fun showNotificationPermissionDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.notification_permission_title)
            .setMessage(R.string.notification_permission_rationale)
            .setPositiveButton(R.string.open_settings) { _, _ ->
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
                }
                startActivity(intent)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun startFocusModeSession(duration: Long, mode: Int) {
        val startTime = System.currentTimeMillis()
        val endTime = startTime + duration
        
        savedPreferencesLoader?.saveFocusModeData(
            FocusModeBlocker.FocusModeData(
                true,
                endTime,
                mode
            )
        )
        
        // Save focus session start time to track later
        savedPreferencesLoader?.saveFocusSessionStartTime(startTime, endTime)
        
        sendRefreshRequest(DeenShieldAccessibilityService.INTENT_ACTION_REFRESH_FOCUS_MODE)
        val timer = NotificationTimerManager(requireContext())
        timer.startTimer(duration)
        onPositiveButtonPressed()
    }

    private companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }

}
