# Error Reporting & Feedback System Implementation Guide

## Overview
A comprehensive error/crash logging and user feedback collection system for AmnShield that respects user privacy by storing all data locally by default.

For end-to-end verification steps, see `TESTING_GUIDE.md` and run **Test 17: Error Reporting & Crash Recovery**.

---

## ✅ What's Been Implemented

### 1. **ErrorReportManager** (Core Manager)
**Location:** `app/src/main/java/com/alhaq/amnshield/utils/ErrorReportManager.kt`

**Features:**
- Centralized error/crash logging
- Device diagnostics collection (manufacturer, model, Android version, memory info)
- User feedback collection with optional email contact
- All data stored locally in `context.filesDir/error_reports/`
- User consent management (SharedPreferences)
- Export functionality for email/sharing

**Key Methods:**
```kotlin
// Logging
errorManager.logCrash(thread, throwable)
errorManager.logNonFatalError("TAG", "message", exception)

// User Preferences
errorManager.setErrorReportingEnabled(enabled)
errorManager.setFeedbackCollectionEnabled(enabled)

// Data Management
errorManager.saveFeedback(UserFeedback(...))
errorManager.getCrashLogContent() // Get all crash logs
errorManager.getAllFeedbackAsText() // Get all feedback
errorManager.exportReportsAsText() // Full export for sharing
errorManager.clearAllReports() // Privacy cleanup
```

### 2. **Enhanced CrashLogger**
**Location:** `app/src/main/java/com/alhaq/amnshield/CrashLogger.kt`

- Updated to use `ErrorReportManager` for all logging
- Handles uncaught exceptions gracefully
- Logs both fatal crashes and non-fatal errors

### 3. **CrashRecoveryActivity**
**Location:** `app/src/main/java/com/alhaq/amnshield/ui/activity/CrashRecoveryActivity.kt`

**Purpose:** Shows after app crashes (when user reopens app)

**Features:**
- Error summary display
- Feedback collection form (optional)
- User email collection (for follow-up)
- Privacy notice
- Graceful return to main app

### 4. **ErrorReportingSettingsFragment**
**Location:** `app/src/main/java/com/alhaq/amnshield/ui/fragments/settings/ErrorReportingSettingsFragment.kt`

**Features:**
- Toggle crash reporting on/off
- Toggle feedback collection on/off
- View saved crash logs
- View submitted feedback
- Export all data via email/sharing
- Clear all error data (privacy cleanup)
- Privacy information display

---

## 📍 Where Data Goes

### Storage Locations
```
app's private files directory/
└── error_reports/
    ├── crash_log.txt          # All crash logs (appended)
    ├── diagnostics.json       # Device info snapshot
    └── feedback/
        ├── feedback_<timestamp>.json
        ├── feedback_<timestamp>.json
        └── ...
```

### No Network Transmission (Privacy-First)
- **Default:** All data stored locally only
- **User Choice:** User can manually export and share via:
  - Email
  - Cloud storage (Google Drive, etc.)
  - Other apps (messaging, etc.)
- **Never automatic:** No background sending without explicit user action

---

## 🔌 Integration Points

### 1. **Add to Settings UI**
In `SettingsFragment.kt`, add this to your navigation or settings menu:

```kotlin
// In setupClickListeners() or similar
binding.btnErrorReporting.setOnClickListener {
    val fragment = ErrorReportingSettingsFragment()
    childFragmentManager.beginTransaction()
        .replace(R.id.settings_container, fragment)
        .addToBackStack(null)
        .commit()
}
```

Add button to settings layout:
```xml
<com.google.android.material.button.MaterialButton
    android:id="@+id/btn_error_reporting"
    style="@style/Widget.Material3.Button.TextButton"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="Error Reporting"
    android:gravity="start"
    app:icon="@drawable/baseline_warning_24" />
```

### 2. **Log Non-Fatal Errors Anywhere**
When you catch exceptions that shouldn't crash the app:

```kotlin
try {
    // Risky operation
} catch (e: Exception) {
    val errorManager = ErrorReportManager.getInstance(context)
    errorManager.logNonFatalError("MyFeature", "Something went wrong", e)
    // Show user a friendly message
}
```

### 3. **Optionally Trigger Crash Recovery**
If you want to show the crash recovery UI manually:

```kotlin
val intent = Intent(context, CrashRecoveryActivity::class.java)
intent.putExtra("crash_message", "App encountered an error")
intent.putExtra("crash_stacktrace", exceptionStackTrace)
startActivity(intent)
```

---

## 📊 Data Collected

### Crash Log Includes:
```
- Timestamp
- Device manufacturer, model, brand, hardware
- Android version and API level
- App version and package name
- Memory information (total, free, used, max)
- Thread name and ID
- Exception type and message
- Full stack trace with cause chain
```

### User Feedback Structure:
```json
{
  "timestamp": "2026-04-25 10:15:30",
  "category": "Crash|Bug|Feature|Performance|General",
  "message": "User's description of issue",
  "rating": 3,
  "email": "user@example.com (optional)",
  "stackTrace": "Auto-attached error (optional)",
  "deviceInfo": "Device model / Android version (optional)"
}
```

---

## 🔐 Privacy & User Control

✅ **Privacy-First Design:**
- All data stored on device locally
- No network transmission by default
- User controls when/if to share
- Users can view exactly what's stored
- One-tap deletion of all data

✅ **User Consent:**
- Settings toggle for crash reporting
- Settings toggle for feedback collection
- Clear messaging about data usage
- Privacy notice in crash recovery UI

---

## 🧪 Testing

### Test Crash Logging:
```kotlin
// Force a crash to test logging
throw RuntimeException("Test crash")
```

### Check Logs:
1. Go to Settings → Error Reporting
2. Tap "View Crash Logs"
3. Should see your test crash with full details

### Test Feedback:
1. Manually trigger: `Intent(context, CrashRecoveryActivity::class.java)`
2. Submit feedback with test message
3. Go to Settings → Error Reporting → View Feedback

---

## 🚀 Future Enhancements

### Optional: Add Backend Integration
If you want to add optional cloud backup/analysis:

```kotlin
// Option 1: Firebase Crashlytics (optional, user-opted)
if (errorManager.isErrorReportingEnabled() && userHasOptedInToCloud) {
    FirebaseCrashlytics.getInstance().recordException(exception)
}

// Option 2: Custom backend API
// POST to your server only when user explicitly chooses "Share"
// Never automatic, always with user consent
```

### Production Email & Intent Export Integration
The logs and feedback export flows are fully implemented using the Android Sharesheet.
- **Primary Support Recipient**: Pre-populated via `Intent.EXTRA_EMAIL` to automatically set the "To" address to `support@alhaq-initiative.org`.
- **Secondary Support Recipient**: Pre-populated via `Intent.EXTRA_CC` for `alhaq.dst@gmail.com`.
- **Decoupled Share Crash Logs**: Implemented locally within `SettingsFragment` via `ErrorReportManager.createBundledReportFile` and `FileProvider.getUriForFile`. This ensures the share functionality behaves perfectly regardless of whether the settings fragment is loaded inside `MainActivity` or a standalone `FragmentActivity`.

---

## 📋 Checklist for Production

- [x] Test crash logging with forced crashes
- [x] Test feedback submission
- [x] Verify all data stored locally (no network leaks)
- [x] Test export functionality
- [x] Test clear/delete functionality
- [x] Add settings menu item for Error Reporting
- [x] Update privacy policy mentioning local error logs
- [x] Test on multiple Android versions (target API range)
- [x] Verify UI/UX alignment with Material 3
- [x] Verify safety wrappers (`safeStartActivity`) on settings intents to prevent ActivityNotFoundException

---

## 📞 Support

**Architecture Questions:**
- ErrorReportManager is a singleton for app-wide access
- CrashLogger is set as default exception handler in `AmnShield` application class
- All storage is app-private (not accessible to other apps)

**User FAQ:**
- "Where are my crash logs stored?" → On your device only, in private app storage
- "Will you send my data?" → Never without your permission
- "How do I delete logs?" → Settings → Error Reporting → Clear All (permanent)
- "Can I see what's being logged?" → Yes, view crash logs and feedback in settings

---

## File Structure Summary

```
New Files Created:
├── ErrorReportManager.kt (Core manager)
├── CrashRecoveryActivity.kt (Post-crash UI)
├── ErrorReportingSettingsFragment.kt (Settings UI)
├── activity_crash_recovery.xml (Layout)
└── fragment_error_reporting_settings.xml (Layout)

Updated Files:
├── CrashLogger.kt (Enhanced with ErrorReportManager)
├── AmnShieldAccessibilityService.kt (Fixed logging calls, settings protection leaks, and OEM setting packages)
├── AndroidManifest.xml (Registered CrashRecoveryActivity & exported AppInstallReceiver)
├── strings.xml (Added error reporting strings)
├── SettingsFragment.kt (Decoupled Share Crash Logs)
├── PermissionsBottomSheet.kt (Added safety wrappers for settings intents)
```

---

**Ready to deploy! All components are production-ready and fully tested.**
