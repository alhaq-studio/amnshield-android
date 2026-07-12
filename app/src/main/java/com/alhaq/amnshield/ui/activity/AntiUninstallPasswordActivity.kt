package com.alhaq.amnshield.ui.activity

import android.content.Context
import android.os.CountDownTimer
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.alhaq.amnshield.Constants
import com.alhaq.amnshield.R
import com.alhaq.amnshield.databinding.ActivityAntiUninstallPasswordBinding
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
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKOUT_UNTIL = "lockout_until"
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_MILLIS = 2 * 60 * 1000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        com.alhaq.amnshield.utils.ThemeUtils.applyTheme(this)
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
            val lockoutRemaining = getLockoutRemainingMillis()
            if (lockoutRemaining > 0L) {
                val remainingSeconds = (lockoutRemaining / 1000L).toInt().coerceAtLeast(1)
                Toast.makeText(
                    this,
                    getString(R.string.anti_uninstall_too_many_attempts, remainingSeconds),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val enteredPassword = binding.passwordInput.text.toString()

            if (com.alhaq.amnshield.utils.PasswordHasher.verify(enteredPassword, savedPassword)) {
                // If the stored value was the legacy plaintext format, silently upgrade it
                // to the salted hash now that we know the password matched.
                if (com.alhaq.amnshield.utils.PasswordHasher.isPlainText(savedPassword)) {
                    val upgraded = com.alhaq.amnshield.utils.PasswordHasher.hash(enteredPassword)
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
                clearAttemptState()
                sendPasswordVerifiedBroadcast()

                // Close activity to allow Settings access - user can continue where they were
                finish()
            } else {
                // Password incorrect - count attempt and apply lockout if threshold reached.
                val attemptsLeft = registerFailedAttempt()
                binding.passwordInputLayout.error = if (attemptsLeft > 0) {
                    getString(R.string.anti_uninstall_attempts_remaining, attemptsLeft)
                } else {
                    getString(R.string.incorrect_password)
                }
                binding.passwordInput.text?.clear()
            }
        }

        binding.btnCancel.setOnClickListener {
            // User cancelled - close activity to return to previous Settings screen
            finish()
        }

        binding.btnForgotPassword.setOnClickListener {
            val lockoutRemaining = getLockoutRemainingMillis()
            if (lockoutRemaining > 0L) {
                val remainingSeconds = (lockoutRemaining / 1000L).toInt().coerceAtLeast(1)
                Toast.makeText(
                    this,
                    getString(R.string.anti_uninstall_too_many_attempts, remainingSeconds),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

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

            updateLockoutUiState()
        maybeResumeRecoveryCountdown()
    }

    private fun setupTimedMode() {
        binding.txtTitle.text = getString(R.string.anti_uninstall_timed_mode_active)
        
        val antiPrefs = getSharedPreferences("anti_uninstall", Context.MODE_PRIVATE)
        val unlockAtMillis = antiPrefs.getLong("unlock_at_millis", 0L)

        val message = if (unlockAtMillis > 0L) {
            val remainingMillis = (unlockAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
            val daysDiff = kotlin.math.ceil(
                remainingMillis / (1000.0 * 60.0 * 60.0 * 24.0)
            ).toInt().coerceAtLeast(0)
            getString(R.string.anti_uninstall_timed_mode_days_remaining, daysDiff)
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
        clearAttemptState()
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
            com.alhaq.amnshield.services.DeenShieldAccessibilityService.INTENT_ACTION_PASSWORD_VERIFIED
        ).setPackage(packageName)
        sendBroadcast(intent)
    }

    private fun getLockoutRemainingMillis(): Long {
        val lockoutUntil = getSharedPreferences("anti_uninstall", Context.MODE_PRIVATE)
            .getLong(KEY_LOCKOUT_UNTIL, 0L)
        return (lockoutUntil - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    private fun registerFailedAttempt(): Int {
        val prefs = getSharedPreferences("anti_uninstall", Context.MODE_PRIVATE)
        val failed = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1

        if (failed >= MAX_FAILED_ATTEMPTS) {
            prefs.edit()
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_LOCKOUT_UNTIL, System.currentTimeMillis() + LOCKOUT_MILLIS)
                .apply()
            updateLockoutUiState()
            return 0
        }

        prefs.edit().putInt(KEY_FAILED_ATTEMPTS, failed).apply()
        return (MAX_FAILED_ATTEMPTS - failed).coerceAtLeast(0)
    }

    private fun clearAttemptState() {
        getSharedPreferences("anti_uninstall", Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_FAILED_ATTEMPTS)
            .remove(KEY_LOCKOUT_UNTIL)
            .apply()
        updateLockoutUiState()
    }

    private fun updateLockoutUiState() {
        val isLocked = getLockoutRemainingMillis() > 0L
        binding.btnVerify.isEnabled = !isLocked
        binding.btnForgotPassword.isEnabled = !isLocked
    }
}
