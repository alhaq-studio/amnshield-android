package com.alhaq.amnshield.ui.activity

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.alhaq.amnshield.Constants
import com.alhaq.amnshield.R
import com.alhaq.amnshield.databinding.DialogWarningOverlayBinding
import com.alhaq.amnshield.services.AmnShieldAccessibilityService
import com.alhaq.amnshield.utils.SavedPreferencesLoader


class WarningActivity : AppCompatActivity() {

    private var proceedTimer: CountDownTimer? = null
    private var dialog: AlertDialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        com.alhaq.amnshield.utils.ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        val savedPreferencesLoader = SavedPreferencesLoader(this)


        val mode = intent.getIntExtra("mode", 0)
        
        val warningScreenConfig = if (mode == Constants.WARNING_SCREEN_MODE_VIEW_BLOCKER) {
            savedPreferencesLoader.loadViewBlockerWarningInfo()
        } else {
            savedPreferencesLoader.loadAppBlockerWarningInfo()

        }
        val binding = DialogWarningOverlayBinding.inflate(layoutInflater)
        val isHomePressRequested = intent.getBooleanExtra("is_press_home", false)
        val isReelBlockerWarning = intent.getBooleanExtra("is_reel_blocker", false)
        val isAppBlockerMode = mode == Constants.WARNING_SCREEN_MODE_APP_BLOCKER

        binding.warningTitle.text = if (isAppBlockerMode) {
            getString(R.string.warning_title_app_blocker)
        } else {
            getString(R.string.warning_title_reels_blocker)
        }

        binding.minsPicker.setValue(3)
        binding.minsPicker.minValue = 2
        val isDialogCancelable = !isAppBlockerMode || isHomePressRequested

        if (warningScreenConfig.isProceedDisabled) {
            binding.btnProceed.visibility = View.GONE
            binding.proceedSeconds.visibility = View.GONE

        } else {
            proceedTimer =
                object : CountDownTimer(warningScreenConfig.proceedDelayInSecs * 1000L, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    binding.proceedSeconds.text =
                        getString(R.string.proceed_in, millisUntilFinished / 1000)
                }

                override fun onFinish() {
                    binding.btnProceed.let { button ->
                        button.isEnabled = true
                        if (warningScreenConfig.isDynamicIntervalSettingAllowed) {
                            binding.minsPicker.visibility = View.VISIBLE
                        }
                        button.setText(R.string.proceed)
                    }
                    binding.proceedSeconds.visibility = View.GONE
                }
            }.start()
        }

        dialog = MaterialAlertDialogBuilder(this)
            .setView(binding.root)
            .setCancelable(isDialogCancelable)
            .setOnCancelListener {
                finish()
            }
            .show()

        val fallbackMessage = if (isAppBlockerMode) {
            getString(R.string.warning_default_message_app)
        } else {
            getString(R.string.warning_default_message_reels)
        }
        val configuredMessage = warningScreenConfig.message.trim()
        binding.warningMsg.text = if (configuredMessage.isNotEmpty()) configuredMessage else fallbackMessage

        binding.minsPicker.setValue(warningScreenConfig.timeInterval / 60000)
        binding.btnCancel.text = if (isAppBlockerMode || isHomePressRequested) {
            getString(R.string.warning_cancel_go_home)
        } else {
            getString(R.string.warning_cancel_stay_safe)
        }

        binding.btnCancel.setOnClickListener {
            if (isAppBlockerMode || isHomePressRequested) {
                val intent = Intent(Intent.ACTION_MAIN)
                intent.addCategory(Intent.CATEGORY_HOME)
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            dialog?.dismiss()
            finish()
        }
        binding.btnProceed.setOnClickListener {
            if (mode == Constants.WARNING_SCREEN_MODE_VIEW_BLOCKER) {
                intent.getStringExtra("result_id")
                    ?.let { it1 ->
                        val refreshAction = if (isReelBlockerWarning) {
                            AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_REEL_BLOCKER_COOLDOWN
                        } else {
                            AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_VIEW_BLOCKER_COOLDOWN
                        }
                        sendRefreshRequest(
                            it1,
                            refreshAction,
                            binding.minsPicker.getValue()
                        )
                    }
            }

            if (mode == Constants.WARNING_SCREEN_MODE_APP_BLOCKER) {
                intent.getStringExtra("result_id")
                    ?.let { it1 ->
                        sendRefreshRequest(
                            it1,
                            AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_APP_BLOCKER_COOLDOWN,
                            binding.minsPicker.getValue()
                        )
                        val intent = packageManager.getLaunchIntentForPackage(it1)
                        if (intent != null) {
                            startActivity(intent)
                        }
                    }
            }

            dialog?.dismiss()
            finishAffinity()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        proceedTimer?.cancel()
        dialog?.dismiss()  // Ensure dialog is dismissed before activity is destroyed


    }

    private fun sendRefreshRequest(id: String, action: String, time: Int) {
        val intent = Intent(action).setPackage(packageName)
        intent.putExtra("result_id", id)
        intent.putExtra("selected_time", time * 60_000)
        sendBroadcast(intent)
    }
}