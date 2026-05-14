package com.alhaq.deenshield.ui.activity

import android.content.Context
import android.os.CountDownTimer
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.alhaq.deenshield.Constants
import com.alhaq.deenshield.R
import com.alhaq.deenshield.databinding.ActivityAntiUninstallPasswordBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Activity shown when user attempts to access protected Settings screens (Device Admin, Accessibility)
 * Requires password verification to proceed or cancels and returns home
 */
class AntiUninstallPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAntiUninstallPasswordBinding
    private var savedPassword: String? = null
    private var antiUninstallMode: Int = -1
    private var recoveryTimer: CountDownTimer? = null

    companion object {
        private const val RECOVERY_WAIT_MILLIS = 5 * 60 * 1000L
        private const val KEY_RECOVERY_UNLOCK_AT = "recovery_unlock_at"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme before super.onCreate. Read from the shared "theme_prefs"
        // file (the one Settings writes to) so the gradient theme is honored.
        val sharedPreferences = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        val themeStyle = sharedPreferences.getString("theme_style", "default")
        if (themeStyle == "gradient") {
            setTheme(R.style.Theme_DeenShield_Gradient)
        }
        super.onCreate(savedInstanceState)
        binding = ActivityAntiUninstallPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle back button to close activity and return to Settings
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        // Load anti-uninstall settings
        val prefs = getSharedPreferences("anti_uninstall", Context.MODE_PRIVATE)
        savedPassword = prefs.getString("password", null)
        antiUninstallMode = prefs.getInt("mode", -1)

        setupUI()
    }

    private fun setupUI() {
        when (antiUninstallMode) {
            Constants.ANTI_UNINSTALL_PASSWORD_MODE -> {
                setupPasswordMode()
            }
            Constants.ANTI_UNINSTALL_TIMED_MODE -> {
                setupTimedMode()
            }
            else -> {
                // Unknown mode, just close
                finish()
            }
        }
    }

    private fun setupPasswordMode() {
        binding.txtTitle.text = getString(R.string.anti_uninstall_password_required)
        binding.txtMessage.text = getString(R.string.enter_password_to_proceed_settings)
        binding.passwordInputLayout.visibility = android.view.View.VISIBLE
        binding.btnCancel.visibility = android.view.View.VISIBLE
        binding.btnVerify.visibility = android.view.View.VISIBLE
        binding.btnForgotPassword.visibility = android.view.View.VISIBLE

        binding.btnVerify.setOnClickListener {
            val enteredPassword = binding.passwordInput.text.toString()

            if (com.alhaq.deenshield.utils.PasswordHasher.verify(enteredPassword, savedPassword)) {
                // If the stored value was the legacy plaintext format, silently upgrade it
                // to the salted hash now that we know the password matched.
                if (com.alhaq.deenshield.utils.PasswordHasher.isPlainText(savedPassword)) {
                    val upgraded = com.alhaq.deenshield.utils.PasswordHasher.hash(enteredPassword)
                    getSharedPreferences("anti_uninstall", Context.MODE_PRIVATE)
                        .edit()
                        .putString("password", upgraded)
                        .apply()
                    savedPassword = upgraded
                }

                // Password correct - notify accessibility service to start 5-minute cooldown.
                // Scope the broadcast to our own package to prevent third-party apps from
                // sending the same action to bypass anti-uninstall.
                clearRecoveryTimerState()
                sendPasswordVerifiedBroadcast()

                // Close activity to allow Settings access - user can continue where they were
                finish()
            } else {
                // Password incorrect - show error
                binding.passwordInputLayout.error = getString(R.string.incorrect_password)
                binding.passwordInput.text?.clear()
            }
        }

        binding.btnCancel.setOnClickListener {
            // User cancelled - close activity to return to previous Settings screen
            finish()
        }

        binding.btnForgotPassword.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.start_recovery)
                .setMessage(R.string.recovery_started)
                .setPositiveButton(R.string.start_recovery) { _, _ ->
                    val unlockAt = System.currentTimeMillis() + RECOVERY_WAIT_MILLIS
                    getSharedPreferences("anti_uninstall", Context.MODE_PRIVATE)
                        .edit()
                        .putLong(KEY_RECOVERY_UNLOCK_AT, unlockAt)
                        .apply()
                    beginRecoveryCountdown(unlockAt)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        maybeResumeRecoveryCountdown()
    }

    private fun setupTimedMode() {
        binding.txtTitle.text = getString(R.string.anti_uninstall_timed_mode_active)
        
        // Calculate remaining days
        val savedDate = getSharedPreferences("anti_uninstall", Context.MODE_PRIVATE)
            .getString("date", null)
        
        val message = if (savedDate != null) {
            try {
                val parts = savedDate.split("/")
                val selectedDate = java.util.Calendar.getInstance()
                selectedDate.set(
                    parts[2].toInt(),
                    parts[0].toInt() - 1,
                    parts[1].toInt()
                )
                
                val today = java.util.Calendar.getInstance()
                val daysDiff = ((selectedDate.timeInMillis - today.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
                
                getString(R.string.anti_uninstall_timed_mode_days_remaining, daysDiff)
            } catch (e: Exception) {
                getString(R.string.anti_uninstall_is_active_cannot_remove)
            }
        } else {
            getString(R.string.anti_uninstall_is_active_cannot_remove)
        }
        
        binding.txtMessage.text = message
        binding.passwordInputLayout.visibility = android.view.View.GONE
        binding.btnVerify.visibility = android.view.View.GONE
        binding.btnForgotPassword.visibility = android.view.View.GONE
        binding.txtRecoveryStatus.visibility = android.view.View.GONE
        binding.btnCancel.text = getString(R.string.ok)
        binding.btnCancel.visibility = android.view.View.VISIBLE

        binding.btnCancel.setOnClickListener {
            // Close activity to return to previous Settings screen
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear password input to prevent memory leaks
        binding.passwordInput.text?.clear()
        recoveryTimer?.cancel()
        recoveryTimer = null
    }

    private fun maybeResumeRecoveryCountdown() {
        val unlockAt = getSharedPreferences("anti_uninstall", Context.MODE_PRIVATE)
            .getLong(KEY_RECOVERY_UNLOCK_AT, 0L)
        if (unlockAt > 0L) {
            beginRecoveryCountdown(unlockAt)
        }
    }

    private fun beginRecoveryCountdown(unlockAt: Long) {
        recoveryTimer?.cancel()

        val remaining = unlockAt - System.currentTimeMillis()
        if (remaining <= 0L) {
            onRecoveryReady()
            return
        }

        binding.btnForgotPassword.isEnabled = false
        binding.txtRecoveryStatus.visibility = android.view.View.VISIBLE

        recoveryTimer = object : CountDownTimer(remaining, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val totalSeconds = millisUntilFinished / 1000L
                val minutes = (totalSeconds / 60).toInt()
                val seconds = (totalSeconds % 60).toInt()
                binding.txtRecoveryStatus.text = getString(
                    R.string.recovery_countdown_message,
                    minutes,
                    seconds
                )
            }

            override fun onFinish() {
                onRecoveryReady()
            }
        }.start()
    }

    private fun onRecoveryReady() {
        clearRecoveryTimerState()
        Toast.makeText(this, getString(R.string.recovery_ready), Toast.LENGTH_LONG).show()
        sendPasswordVerifiedBroadcast()
        finish()
    }

    private fun clearRecoveryTimerState() {
        recoveryTimer?.cancel()
        recoveryTimer = null
        getSharedPreferences("anti_uninstall", Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_RECOVERY_UNLOCK_AT)
            .apply()
    }

    private fun sendPasswordVerifiedBroadcast() {
        val intent = android.content.Intent(
            com.alhaq.deenshield.services.DeenShieldAccessibilityService.INTENT_ACTION_PASSWORD_VERIFIED
        ).setPackage(packageName)
        sendBroadcast(intent)
    }
}
