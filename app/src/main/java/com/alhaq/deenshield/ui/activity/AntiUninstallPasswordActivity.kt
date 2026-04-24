package com.alhaq.deenshield.ui.activity

import android.content.Context
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

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme before super.onCreate
        val sharedPreferences = getSharedPreferences("com.alhaq.deenshield_preferences", MODE_PRIVATE)
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

        binding.btnVerify.setOnClickListener {
            val enteredPassword = binding.passwordInput.text.toString()
            
            if (enteredPassword == savedPassword) {
                // Password correct - notify accessibility service to start 5-minute cooldown
                val intent = android.content.Intent(com.alhaq.deenshield.services.DeenShieldAccessibilityService.INTENT_ACTION_PASSWORD_VERIFIED)
                sendBroadcast(intent)
                
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
    }
}
