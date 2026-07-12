package com.alhaq.amnshield.ui.fragments.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.alhaq.amnshield.databinding.FragmentErrorReportingSettingsBinding
import com.alhaq.amnshield.utils.ErrorReportManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Settings fragment for error reporting and feedback preferences
 */
class ErrorReportingSettingsFragment : Fragment() {

    private var _binding: FragmentErrorReportingSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var errorManager: ErrorReportManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentErrorReportingSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        errorManager = ErrorReportManager.getInstance(requireContext())

        setupUI()
    }

    private fun setupUI() {
        // Crash reporting toggle
        binding.switchCrashReporting.isChecked = errorManager.isErrorReportingEnabled()
        binding.switchCrashReporting.setOnCheckedChangeListener { _, isChecked ->
            errorManager.setErrorReportingEnabled(isChecked)
            Toast.makeText(
                requireContext(),
                if (isChecked) "Crash reporting enabled" else "Crash reporting disabled",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Feedback collection toggle
        binding.switchFeedbackCollection.isChecked = errorManager.isFeedbackCollectionEnabled()
        binding.switchFeedbackCollection.setOnCheckedChangeListener { _, isChecked ->
            errorManager.setFeedbackCollectionEnabled(isChecked)
            Toast.makeText(
                requireContext(),
                if (isChecked) "Feedback collection enabled" else "Feedback collection disabled",
                Toast.LENGTH_SHORT
            ).show()
        }

        // View crash logs button
        binding.btnViewCrashLogs.setOnClickListener {
            showCrashLogs()
        }

        // View feedback button
        binding.btnViewFeedback.setOnClickListener {
            showFeedback()
        }

        // Export logs button
        binding.btnExportLogs.setOnClickListener {
            exportLogs()
        }

        // Clear all data button
        binding.btnClearAllData.setOnClickListener {
            showClearConfirmation()
        }
    }

    private fun showCrashLogs() {
        val logs = errorManager.getCrashLogContent()
        if (logs.isEmpty() || logs == "No crash logs found.") {
            Toast.makeText(requireContext(), "No crash logs available", Toast.LENGTH_SHORT).show()
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Crash Logs")
            .setMessage(logs)
            .setPositiveButton("Copy") { _, _ ->
                val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Crash Logs", logs)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(requireContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showFeedback() {
        val feedback = errorManager.getAllFeedbackAsText()
        if (feedback.isEmpty() || feedback == "No feedback submitted.") {
            Toast.makeText(requireContext(), "No feedback available", Toast.LENGTH_SHORT).show()
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("User Feedback")
            .setMessage(feedback)
            .setPositiveButton("Copy") { _, _ ->
                val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Feedback", feedback)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(requireContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun exportLogs() {
        val report = errorManager.exportReportsAsText()

        // Create intent to share via email or other apps
        // Pre-populate primary support email and CC secondary email
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_EMAIL, arrayOf("support@alhaq-initiative.org"))
            putExtra(Intent.EXTRA_SUBJECT, "AmnShield Error Report")
            putExtra(Intent.EXTRA_TEXT, report)
            // Pre-fill CC field with support emails (user can modify before sending)
            putExtra(Intent.EXTRA_CC, arrayOf("alhaq.dst@gmail.com"))
        }

        try {
            startActivity(Intent.createChooser(intent, "Export Report via..."))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "No app available to share", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showClearConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Clear All Error Data?")
            .setMessage("This will permanently delete all crash logs, error reports, and feedback. This action cannot be undone.")
            .setPositiveButton("Clear") { _, _ ->
                errorManager.clearAllReports()
                Toast.makeText(requireContext(), "All error data cleared", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
