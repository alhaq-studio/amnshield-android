package com.alhaq.amnshield

import android.content.Context
import android.util.Log
import com.alhaq.amnshield.utils.ErrorReportManager

/**
 * Global uncaught exception handler that logs crashes using ErrorReportManager.
 * Catches all uncaught exceptions and ensures they are logged before system handles it.
 */
class CrashLogger(private val context: Context) : Thread.UncaughtExceptionHandler {

    private val errorManager = ErrorReportManager.getInstance(context)
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            errorManager.logCrash(thread, throwable)
            Log.e("CrashLogger", "Uncaught exception logged", throwable)
        } catch (e: Exception) {
            // Last resort: ensure logging doesn't prevent crash handling
            Log.e("CrashLogger", "Failed to log crash", e)
        }

        // Allow the system to process the crash
        defaultHandler?.uncaughtException(thread, throwable)
    }

    /**
     * Log a non-fatal error that was caught and handled
     */
    fun logNonFatalError(tag: String, message: String, exception: Exception? = null) {
        errorManager.logNonFatalError(tag, message, exception)
    }
}
