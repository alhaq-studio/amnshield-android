package com.alhaq.amnshield.utils

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.gson.Gson
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Centralized manager for error/crash logs and user feedback collection.
 * Stores locally by default; users can opt-in to share via email.
 */
class ErrorReportManager(private val context: Context) {

    private val errorDir = File(context.filesDir, "error_reports")
    private val crashLogFile = File(errorDir, "crash_log.txt")
    private val feedbackDir = File(errorDir, "feedback")
    private val diagnosticsFile = File(errorDir, "diagnostics.json")
    private val prefs = context.getSharedPreferences("error_reporting", Context.MODE_PRIVATE)
    private val gson = Gson()

    init {
        errorDir.mkdirs()
        feedbackDir.mkdirs()
    }

    /**
     * User preferences for error reporting
     */
    fun setErrorReportingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("crash_reporting_enabled", enabled).apply()
    }

    fun isErrorReportingEnabled(): Boolean {
        return prefs.getBoolean("crash_reporting_enabled", false)
    }

    fun setFeedbackCollectionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("feedback_collection_enabled", enabled).apply()
    }

    fun isFeedbackCollectionEnabled(): Boolean {
        return prefs.getBoolean("feedback_collection_enabled", false)
    }

    /**
     * Log a fatal crash with full diagnostic information
     */
    fun logCrash(thread: Thread, throwable: Throwable) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
            val crashEntry = StringBuilder()
            
            // Header
            crashEntry.append("\n\n")
            crashEntry.append("═".repeat(80)).append("\n")
            crashEntry.append("CRASH REPORT\n")
            crashEntry.append("═".repeat(80)).append("\n")
            crashEntry.append("Timestamp: $timestamp\n")
            
            // Device info
            crashEntry.append("\n--- DEVICE INFORMATION ---\n")
            crashEntry.append("Manufacturer: ${Build.MANUFACTURER}\n")
            crashEntry.append("Model: ${Build.MODEL}\n")
            crashEntry.append("Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
            crashEntry.append("Device Brand: ${Build.BRAND}\n")
            crashEntry.append("Hardware: ${Build.HARDWARE}\n")
            
            // App info
            crashEntry.append("\n--- APP INFORMATION ---\n")
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            crashEntry.append("App Version: ${packageInfo.versionName} (${packageInfo.longVersionCode})\n")
            crashEntry.append("Package: ${context.packageName}\n")
            
            // Memory info
            crashEntry.append("\n--- MEMORY INFORMATION ---\n")
            val runtime = Runtime.getRuntime()
            val totalMemory = runtime.totalMemory() / 1024 / 1024
            val freeMemory = runtime.freeMemory() / 1024 / 1024
            val maxMemory = runtime.maxMemory() / 1024 / 1024
            crashEntry.append("Total Memory: ${totalMemory}MB\n")
            crashEntry.append("Free Memory: ${freeMemory}MB\n")
            crashEntry.append("Max Memory: ${maxMemory}MB\n")
            crashEntry.append("Used Memory: ${totalMemory - freeMemory}MB\n")
            
            // Thread info
            crashEntry.append("\n--- CRASH DETAILS ---\n")
            crashEntry.append("Thread: ${thread.name} (ID: ${thread.id})\n")
            crashEntry.append("Exception: ${throwable.javaClass.simpleName}\n")
            crashEntry.append("Message: ${throwable.message ?: "No message"}\n")
            
            // Stack trace
            crashEntry.append("\n--- STACK TRACE ---\n")
            crashEntry.append(throwable.stackTraceToString()).append("\n")
            
            // Cause chain
            if (throwable.cause != null) {
                crashEntry.append("\n--- CAUSED BY ---\n")
                crashEntry.append(throwable.cause!!.stackTraceToString()).append("\n")
            }
            
            // Footer
            crashEntry.append("═".repeat(80)).append("\n")
            
            // Write to file
            crashLogFile.appendText(crashEntry.toString())
            
            Log.e("ErrorReportManager", "Crash logged to ${crashLogFile.absolutePath}")
        } catch (e: Exception) {
            Log.e("ErrorReportManager", "Failed to log crash", e)
        }
    }

    /**
     * Log a non-fatal error
     */
    fun logNonFatalError(tag: String, message: String, exception: Exception? = null) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
            val errorEntry = StringBuilder()
            
            errorEntry.append("\n--- NON-FATAL ERROR at $timestamp ---\n")
            errorEntry.append("Tag: $tag\n")
            errorEntry.append("Message: $message\n")
            
            if (exception != null) {
                errorEntry.append("Exception: ${exception.javaClass.simpleName}\n")
                errorEntry.append(exception.stackTraceToString()).append("\n")
            }
            
            crashLogFile.appendText(errorEntry.toString())
            Log.w(tag, message, exception)
        } catch (e: Exception) {
            Log.e("ErrorReportManager", "Failed to log error", e)
        }
    }

    /**
     * Save user feedback with optional email for response
     */
    fun saveFeedback(feedback: UserFeedback): Boolean {
        return try {
            val filename = "feedback_${System.currentTimeMillis()}.json"
            val feedbackFile = File(feedbackDir, filename)
            val json = gson.toJson(feedback)
            feedbackFile.writeText(json)
            Log.d("ErrorReportManager", "Feedback saved: $filename")
            true
        } catch (e: Exception) {
            Log.e("ErrorReportManager", "Failed to save feedback", e)
            false
        }
    }

    /**
     * Collect all diagnostic data for reports
     */
    fun collectDiagnostics(): String {
        return try {
            val diagnostics = mapOf(
                "timestamp" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                "device" to "${Build.MANUFACTURER} ${Build.MODEL}",
                "android_version" to Build.VERSION.RELEASE,
                "api_level" to Build.VERSION.SDK_INT,
                "crash_log_exists" to crashLogFile.exists(),
                "feedback_count" to (feedbackDir.listFiles()?.size ?: 0),
                "crash_log_size_kb" to (crashLogFile.length() / 1024)
            )
            gson.toJson(diagnostics)
        } catch (e: Exception) {
            Log.e("ErrorReportManager", "Failed to collect diagnostics", e)
            "{}"
        }
    }

    /**
     * Get all crash logs as a single string (for sharing)
     */
    fun getCrashLogContent(): String {
        return try {
            if (crashLogFile.exists()) crashLogFile.readText() else "No crash logs found."
        } catch (e: Exception) {
            "Error reading crash logs: ${e.message}"
        }
    }

    /**
     * Get all feedback as formatted text (for review)
     */
    fun getAllFeedbackAsText(): String {
        return try {
            val feedbacks = feedbackDir.listFiles()?.mapNotNull { file ->
                try {
                    val json = file.readText()
                    gson.fromJson(json, UserFeedback::class.java)
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()

            if (feedbacks.isEmpty()) return "No feedback submitted."

            feedbacks.joinToString("\n${"─".repeat(60)}\n") { feedback ->
                buildString {
                    append("Date: ${feedback.timestamp}\n")
                    append("Category: ${feedback.category}\n")
                    append("Rating: ${feedback.rating}/5\n")
                    append("Message:\n${feedback.message}\n")
                    if (!feedback.email.isNullOrEmpty()) {
                        append("User Email: ${feedback.email}\n")
                    }
                    if (!feedback.stackTrace.isNullOrEmpty()) {
                        append("Stack Trace:\n${feedback.stackTrace}\n")
                    }
                }
            }
        } catch (e: Exception) {
            "Error reading feedback: ${e.message}"
        }
    }

    /**
     * Clear all logs and feedback (privacy cleanup)
     */
    fun clearAllReports() {
        try {
            errorDir.deleteRecursively()
            errorDir.mkdirs()
            feedbackDir.mkdirs()
            Log.d("ErrorReportManager", "All error reports cleared")
        } catch (e: Exception) {
            Log.e("ErrorReportManager", "Failed to clear reports", e)
        }
    }

    /**
     * Export logs for email sharing
     */
    fun exportReportsAsText(): String {
        return buildString {
            append("AmnShield Error Report Export\n")
            append("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
            append("\n${"═".repeat(60)}\n\n")
            
            append("DIAGNOSTICS\n")
            append("─".repeat(60)).append("\n")
            append(collectDiagnostics()).append("\n\n")
            
            append("CRASH LOGS\n")
            append("─".repeat(60)).append("\n")
            append(getCrashLogContent()).append("\n\n")
            
            append("USER FEEDBACK\n")
            append("─".repeat(60)).append("\n")
            append(getAllFeedbackAsText()).append("\n")
        }
    }

    /**
     * Create a single shareable report file that can be attached to an email.
     */
    fun createBundledReportFile(prefixText: String? = null): File? {
        return try {
            val shareDir = File(context.cacheDir, "shared_reports").apply { mkdirs() }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val reportFile = File(shareDir, "amnshield_error_report_$timestamp.txt")

            val content = buildString {
                if (!prefixText.isNullOrBlank()) {
                    append(prefixText.trim())
                    append("\n\n")
                }
                append(exportReportsAsText())
            }

            reportFile.writeText(content)
            reportFile
        } catch (e: Exception) {
            Log.e("ErrorReportManager", "Failed to create bundled report file", e)
            null
        }
    }

    companion object {
        @Volatile
        private var instance: ErrorReportManager? = null

        fun getInstance(context: Context): ErrorReportManager {
            return instance ?: synchronized(this) {
                instance ?: ErrorReportManager(context).also { instance = it }
            }
        }
    }
}

/**
 * Data class for user feedback
 */
data class UserFeedback(
    val timestamp: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
    val category: String = "General", // "Bug", "Crash", "Feature", "Performance", "General"
    val message: String,
    val rating: Int = 3, // 1-5 stars
    val email: String? = null, // Optional: user contact for follow-up
    val stackTrace: String? = null, // Optional: auto-attached stack trace
    val deviceInfo: String? = null // Optional: device/app version info
)
