package com.alhaq.amnshield.ui.activity

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.alhaq.amnshield.R
import com.alhaq.amnshield.databinding.ActivityCrashRecoveryBinding
import com.alhaq.amnshield.utils.ErrorReportManager
import com.alhaq.amnshield.utils.UserFeedback
import android.util.Log
import android.os.Build

/**
 * Activity shown after app crash recovery. Allows users to submit feedback
 * about what they were doing when the crash occurred.
 */
class CrashRecoveryActivity : AppCompatActivity() {

    companion object {
        private val SUPPORT_CC_ADDRESSES = arrayOf(
            "support@alhaq-initiative.org",
            "alhaq.dst@gmail.com"
        )
    }

    private lateinit var binding: ActivityCrashRecoveryBinding
    private lateinit var errorManager: ErrorReportManager
    private var crashStackTrace: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        com.alhaq.amnshield.utils.ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityCrashRecoveryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        errorManager = ErrorReportManager.getInstance(this)
        
        // Get the crash info from intent
        crashStackTrace = intent.getStringExtra("crash_stacktrace") ?: ""
        val crashMessage = intent.getStringExtra("crash_message") ?: "Unknown error"

        setupUI(crashMessage)
    }

    private fun setupUI(crashMessage: String) {
        // Title and description
        binding.crashTitle.text = "Oops! We encountered an issue"
        binding.crashDescription.text = "AmnShield crashed unexpectedly. We've saved diagnostic information."

        // Show last error if available
        if (crashMessage.isNotEmpty()) {
            binding.errorSummary.text = "Error: $crashMessage"
        }

        // Feedback section
        binding.feedbackLabel.text = "Help us improve"
        binding.feedbackHint.text = "Tell us what you were doing when this happened (optional)"

        // Category spinner setup (simplified for now)
        binding.feedbackCategory.text = "Bug Report"
        binding.shareFeedbackWithLogsCheckbox.text =
            "Also open share screen with this feedback and the saved crash logs"
        binding.shareFeedbackWithLogsCheckbox.isChecked = true

        // Buttons
        binding.btnSendFeedback.text = "Send Feedback"
        binding.btnContinue.text = "Continue without sending"

        binding.btnSendFeedback.setOnClickListener {
            submitFeedback()
        }

        binding.btnContinue.setOnClickListener {
            returnToMainActivity()
        }

        binding.tvDataCollectionNotice.text =
            "Your crash data is stored locally. We never send it without your permission."
    }

    private fun submitFeedback() {
        val feedbackText = binding.feedbackInput.text.toString().trim()
        val senderName = binding.userNameInput.text.toString().trim()
        val userEmail = binding.userEmailInput.text.toString().trim()
        val shouldShareWithLogs = binding.shareFeedbackWithLogsCheckbox.isChecked

        if (feedbackText.isEmpty()) {
            Toast.makeText(this, "Please provide some feedback", Toast.LENGTH_SHORT).show()
            return
        }

        // Create feedback object
        val feedback = UserFeedback(
            category = "Crash",
            message = feedbackText,
            rating = 2, // Crash = low rating by default
            email = if (userEmail.isNotEmpty()) userEmail else null,
            stackTrace = crashStackTrace,
            deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL} / Android ${Build.VERSION.RELEASE}"
        )

        // Save feedback
        if (errorManager.saveFeedback(feedback)) {
            Log.d("CrashRecovery", "Feedback submitted: $feedbackText")

            if (shouldShareWithLogs) {
                openShareChooser(
                    feedbackText = feedbackText,
                    senderName = senderName,
                    deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
                )
                Toast.makeText(
                    this,
                    "Feedback saved. Review and send the bundled report if you want to share it.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "Thank you for your feedback. It will help us improve AmnShield.",
                    Toast.LENGTH_LONG
                ).show()
            }

            returnToMainActivity()
        } else {
            Toast.makeText(this, "Failed to save feedback. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openShareChooser(
        feedbackText: String,
        senderName: String,
        deviceModel: String
    ) {
        val emailBody = buildString {
            append("From: ")
            append(senderName.ifBlank { "Anonymous" })
            append("\n")
            append("Device type or model: ")
            append(deviceModel)
            append("\n")
            append("Issue or Feedback:\n")
            append(feedbackText)
        }

        val attachmentFile = errorManager.createBundledReportFile(prefixText = emailBody)
            ?: run {
                Toast.makeText(this, "Failed to prepare bundled report", Toast.LENGTH_SHORT).show()
                return
            }
        val attachmentUri: Uri = FileProvider.getUriForFile(
            this,
            "$packageName.provider",
            attachmentFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "AmnShield Error Report")
            putExtra(Intent.EXTRA_TEXT, emailBody)
            putExtra(Intent.EXTRA_CC, SUPPORT_CC_ADDRESSES.filter { !TextUtils.isEmpty(it) }.toTypedArray())
            putExtra(Intent.EXTRA_STREAM, attachmentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            selector = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
        }

        try {
            startActivity(Intent.createChooser(shareIntent, "Share crash report via..."))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No app available to share", Toast.LENGTH_SHORT).show()
        }
    }

    private fun returnToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}
