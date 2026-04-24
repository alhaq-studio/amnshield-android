package com.alhaq.deenshield.services

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.content.edit
import com.alhaq.deenshield.Constants
import com.alhaq.deenshield.CrashLogger
import com.alhaq.deenshield.R
import com.alhaq.deenshield.blockers.AppBlocker
import com.alhaq.deenshield.blockers.FocusModeBlocker
import com.alhaq.deenshield.blockers.KeywordBlocker
import com.alhaq.deenshield.blockers.ViewBlocker
import com.alhaq.deenshield.premium.PremiumManager
import com.alhaq.deenshield.ui.activity.MainActivity
import com.alhaq.deenshield.ui.activity.WarningActivity
import com.alhaq.deenshield.utils.BlockingStatsManager
import java.util.Locale

class DeenShieldAccessibilityService : BaseBlockingService() {

    companion object {
        const val INTENT_ACTION_REFRESH_APP_BLOCKER = "deenshield.refresh.appblocker"
        const val INTENT_ACTION_REFRESH_APP_BLOCKER_COOLDOWN = "deenshield.refresh.appblocker.cooldown"
        const val INTENT_ACTION_REFRESH_FOCUS_MODE = "deenshield.refresh.focusmode"
        const val INTENT_ACTION_REFRESH_BLOCKED_KEYWORD_LIST = "deenshield.refresh.keywords"
        const val INTENT_ACTION_REFRESH_VIEW_BLOCKER = "deenshield.refresh.viewblocker"
        const val INTENT_ACTION_REFRESH_VIEW_BLOCKER_COOLDOWN = "deenshield.refresh.viewblocker.cooldown"
        const val INTENT_ACTION_REFRESH_ANTI_UNINSTALL = ".deenshield.refresh.anti_uninstall"
        const val INTENT_ACTION_PASSWORD_VERIFIED = "deenshield.password.verified"
        private const val APP_BLOCK_COOLDOWN_MS = 5 * 60 * 1000L
    }

    private lateinit var appBlocker: AppBlocker
    private lateinit var focusModeBlocker: FocusModeBlocker
    private lateinit var keywordBlocker: KeywordBlocker
    private lateinit var viewBlocker: ViewBlocker
    private lateinit var blockingStatsManager: BlockingStatsManager
    private lateinit var premiumManager: PremiumManager
    private lateinit var crashLogger: CrashLogger

    private var appBlockerWarningConfig = MainActivity.WarningData()
    private var viewBlockerWarningConfig = MainActivity.WarningData()
    private var lastEventTimeStamp = 0L

    // Anti-uninstall protection state
    private var isAntiUninstallOn = false
    private var antiUninstallMode = -1
    private var savedPassword: String? = null
    private var savedDate: String? = null
    private var isConfiguringBlocked = false
    private var lastBlockTime = 0L
    private val blockCooldown = 2000L
    private var protectedApps: Set<String> = emptySet()

    // Password verification session management
    private var lastPasswordVerificationTime = 0L
    private val PASSWORD_VERIFICATION_COOLDOWN = 5 * 60 * 1000L
    private var isPasswordVerified = false
    private var currentProtectionSession: String? = null

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onServiceConnected() {
        super.onServiceConnected()

        appBlocker = AppBlocker()
        focusModeBlocker = FocusModeBlocker()
        keywordBlocker = KeywordBlocker(this)
        viewBlocker = ViewBlocker()
        blockingStatsManager = BlockingStatsManager.getInstance(this)
        premiumManager = PremiumManager.getInstance(this)
        crashLogger = CrashLogger(this)

        setupAppBlocker()
        setupKeywordBlocker()
        setupViewBlocker()
        setupAntiUninstall()

        val filter = IntentFilter().apply {
            addAction(INTENT_ACTION_REFRESH_APP_BLOCKER)
            addAction(INTENT_ACTION_REFRESH_BLOCKED_KEYWORD_LIST)
            addAction(INTENT_ACTION_REFRESH_VIEW_BLOCKER)
            addAction(INTENT_ACTION_REFRESH_VIEW_BLOCKER_COOLDOWN)
            addAction(INTENT_ACTION_REFRESH_APP_BLOCKER_COOLDOWN)
            addAction(INTENT_ACTION_REFRESH_FOCUS_MODE)
            addAction(INTENT_ACTION_REFRESH_ANTI_UNINSTALL)
            addAction(INTENT_ACTION_PASSWORD_VERIFIED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(refreshReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(refreshReceiver, filter)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return
        val rootNode = rootInActiveWindow ?: return
        val rootPackage = rootNode.packageName?.toString() ?: packageName

        try {
            if (packageName.equals("com.alhaq.deenshield", ignoreCase = true) ||
                rootPackage.equals("com.alhaq.deenshield", ignoreCase = true)
            ) {
                return
            }

            if (packageName.equals("com.android.systemui", ignoreCase = true) ||
                rootPackage.equals("com.android.systemui", ignoreCase = true)
            ) {
                return
            }

            if (isAntiUninstallOn && packageName == "com.android.settings") {
                checkAndBlockDangerousSettingsScreens(rootNode)
            }

            if (isAntiUninstallOn) {
                checkAndBlockLauncherUninstall(rootNode)
            }

            val isPremiumUser = premiumManager.isPremium()

            if (isPremiumUser && savedPreferencesLoader.loadBlockedApps().isNotEmpty()) {
                val appBlockerResult = appBlocker.doesAppNeedToBeBlocked(packageName)
                if (appBlockerResult.isBlocked) {
                    blockingStatsManager.recordAppBlock(packageName, "Blocked by App Blocker")
                    val intent = Intent(this, WarningActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        putExtra("mode", Constants.WARNING_SCREEN_MODE_APP_BLOCKER)
                        putExtra("result_id", packageName)
                    }
                    startActivity(intent)
                    return
                }
            }

            if (isPremiumUser) {
                val focusModeResult = focusModeBlocker.doesAppNeedToBeBlocked(packageName)
                if (focusModeResult.isRequestingToUpdateSPData) {
                    savedPreferencesLoader.completeFocusSession()
                    savedPreferencesLoader.saveFocusModeData(focusModeBlocker.focusModeData)
                }

                if (focusModeResult.isBlocked) {
                    blockingStatsManager.recordAppBlock(packageName, "Blocked by Focus Mode")
                    pressHome()
                    return
                }
            }

            val hasCoreKeywords = savedPreferencesLoader.loadBlockedKeywords().isNotEmpty()
            if (hasCoreKeywords) {
                try {
                    val keywordResult = keywordBlocker.checkIfUserGettingFreaky(rootNode, event)
                    if (keywordResult.resultDetectWord != null) {
                        blockingStatsManager.recordKeywordBlock(keywordResult.resultDetectWord, packageName)
                        pressBack()
                        return
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DeenShield", "Core Keyword blocker error", e)
                }
            }

            val viewBlockerPrefs = getSharedPreferences("view_blocker", Context.MODE_PRIVATE)
            val isViewBlockerEnabled = viewBlockerPrefs.getBoolean("is_enabled", false)
            if (isPremiumUser && isViewBlockerEnabled) {
                if (!isDelayOver(lastEventTimeStamp, 2000)) {
                    return
                }

                try {
                    val viewBlockerResult = viewBlocker.doesViewNeedToBeBlocked(rootNode, packageName)
                    if (viewBlockerResult != null && viewBlockerResult.isBlocked) {
                        blockingStatsManager.recordViewBlock(packageName, viewBlockerResult.viewId)
                        handleViewBlockerResult(viewBlockerResult)
                        lastEventTimeStamp = SystemClock.uptimeMillis()
                        return
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DeenShield", "View blocker error", e)
                }
            }
        } catch (t: Throwable) {
            android.util.Log.e("DeenShield", "Accessibility pipeline error", t)
            crashLogger.logNonFatalError(Exception(t))
        }
    }

    private fun handleViewBlockerResult(result: ViewBlocker.ViewBlockerResult?) {
        if (result == null || !result.isBlocked) return

        pressBack()

        if (viewBlockerWarningConfig.isWarningDialogHidden) return
        val dialogIntent = Intent(this, WarningActivity::class.java)
        dialogIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        dialogIntent.putExtra("mode", Constants.WARNING_SCREEN_MODE_VIEW_BLOCKER)
        dialogIntent.putExtra("result_id", result.viewId)
        dialogIntent.putExtra("is_press_home", result.requestHomePressInstead)
        startActivity(dialogIntent)
    }

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            when (intent.action) {
                INTENT_ACTION_REFRESH_APP_BLOCKER -> setupAppBlocker()
                INTENT_ACTION_REFRESH_BLOCKED_KEYWORD_LIST -> setupKeywordBlocker()
                INTENT_ACTION_REFRESH_VIEW_BLOCKER -> setupViewBlocker()
                INTENT_ACTION_REFRESH_FOCUS_MODE -> setupFocusMode()
                INTENT_ACTION_REFRESH_ANTI_UNINSTALL -> setupAntiUninstall()
                INTENT_ACTION_REFRESH_VIEW_BLOCKER_COOLDOWN -> {
                    val interval = intent.getIntExtra("selected_time", viewBlockerWarningConfig.timeInterval)
                    viewBlocker.applyCooldown(
                        intent.getStringExtra("result_id") ?: "xxxxxxxxxxxxxx",
                        SystemClock.uptimeMillis() + interval
                    )
                }

                INTENT_ACTION_REFRESH_APP_BLOCKER_COOLDOWN -> {
                    val interval = intent.getIntExtra("selected_time", appBlockerWarningConfig.timeInterval)
                    appBlocker.putCooldownTo(
                        intent.getStringExtra("result_id") ?: "xxxxxxxxxxxxxx",
                        SystemClock.uptimeMillis() + interval
                    )
                }

                INTENT_ACTION_PASSWORD_VERIFIED -> {
                    isPasswordVerified = true
                    lastPasswordVerificationTime = System.currentTimeMillis()
                    android.util.Log.d("DeenShield", "Password verified, 5-minute cooldown started")
                }
            }
        }
    }

    private fun setupAppBlocker() {
        appBlockerWarningConfig = savedPreferencesLoader.loadAppBlockerWarningInfo()
        appBlocker.blockedApps = savedPreferencesLoader.loadBlockedApps().toHashSet()

        val cheatHours = getSharedPreferences("cheat_hours", Context.MODE_PRIVATE)
        appBlocker.cheatMinuteStartTime = cheatHours.getInt("app_blocker_start_time", -1)
        appBlocker.cheatMinutesEndTime = cheatHours.getInt("app_blocker_end_time", -1)
    }

    private fun setupKeywordBlocker() {
        val userKeywords = savedPreferencesLoader.loadBlockedKeywords()
            .map { it.trim().lowercase(Locale.ROOT) }
            .filter { it.isNotEmpty() }
        keywordBlocker.blockedKeyword = HashSet(userKeywords)

        val keywordPrefs = getSharedPreferences("keyword_blocker_configs", Context.MODE_PRIVATE)
        keywordBlocker.isSearchAllTextFields = keywordPrefs.getBoolean("search_all_text_fields", false)
        keywordBlocker.isUnsupportedBrowserBlockingOn = keywordPrefs.getBoolean("block_all_except_supported", false)
        val configuredRedirect = keywordPrefs.getString("redirect_url", KeywordBlocker.DEFAULT_REDIRECT_URL).orEmpty()
        keywordBlocker.redirectUrl = configuredRedirect.ifBlank { KeywordBlocker.DEFAULT_REDIRECT_URL }
        keywordBlocker.resetDetectionCache()

        val userIgnoredPackages = savedPreferencesLoader
            .getKeywordBlockerIgnoredApps()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toMutableSet()

        userIgnoredPackages.add("com.alhaq.deenshield")
        userIgnoredPackages.add("com.android.settings")
        userIgnoredPackages.add("com.google.android.settings")
        userIgnoredPackages.add("com.samsung.android.settings")
        userIgnoredPackages.add("com.android.systemui")
        userIgnoredPackages.add("android")
        userIgnoredPackages.add("com.android.launcher")
        userIgnoredPackages.add("com.google.android.apps.nexuslauncher")
        userIgnoredPackages.add("com.sec.android.app.launcher")

        keywordBlocker.ignoredPackages = userIgnoredPackages
    }

    private fun setupViewBlocker() {
        viewBlockerWarningConfig = savedPreferencesLoader.loadViewBlockerWarningInfo()

        val viewBlockerCheatHours = getSharedPreferences("cheat_hours", Context.MODE_PRIVATE)
        viewBlocker.cheatMinuteStartTime = viewBlockerCheatHours.getInt("view_blocker_start_time", -1)
        viewBlocker.cheatMinutesEndTIme = viewBlockerCheatHours.getInt("view_blocker_end_time", -1)

        val addReelData = getSharedPreferences("config_reels", Context.MODE_PRIVATE)
        viewBlocker.isIGInboxReelAllowed = addReelData.getBoolean("is_reel_inbox", false)
        viewBlocker.isFirstReelInFeedAllowed = addReelData.getBoolean("is_reel_first", false)
    }

    private fun setupFocusMode() {
        val focusModeData = savedPreferencesLoader.getFocusModeData()
        if (focusModeData != null) {
            focusModeBlocker.update(focusModeData)
        }
    }

    private fun setupAntiUninstall() {
        val info = getSharedPreferences("anti_uninstall", Context.MODE_PRIVATE)
        isAntiUninstallOn = info.getBoolean("is_anti_uninstall_on", false)
        antiUninstallMode = info.getInt("mode", -1)
        savedPassword = info.getString("password", null)
        savedDate = info.getString("date", null)
        isConfiguringBlocked = info.getBoolean("is_configuring_blocked", false)

        protectedApps = if (isAntiUninstallOn) setOf("com.alhaq.deenshield") else emptySet()
    }

    private fun checkAndBlockDangerousSettingsScreens(node: AccessibilityNodeInfo?) {
        if (node == null) return

        var isDangerousScreen = false
        traverseAndDetectScreen(node) { detectedType, appPackage ->
            when (detectedType) {
                "device_admin" -> if (isAntiUninstallOn) isDangerousScreen = true
                "accessibility_deenshield" -> if (isAntiUninstallOn) isDangerousScreen = true
                "app_uninstall" -> if (appPackage != null && protectedApps.contains(appPackage)) isDangerousScreen = true
            }
        }

        if (isDangerousScreen) {
            val shouldBlock = shouldBlockRemoval()
            if (shouldBlock) {
                val currentTime = System.currentTimeMillis()
                if (isPasswordVerified && (currentTime - lastPasswordVerificationTime) < PASSWORD_VERIFICATION_COOLDOWN) {
                    return
                }

                if (currentTime - lastBlockTime > blockCooldown) {
                    lastBlockTime = currentTime
                    currentProtectionSession = "settings_" + currentTime
                    handleAntiUninstallAttempt()
                }
            }
        }
    }

    private fun checkAndBlockLauncherUninstall(node: AccessibilityNodeInfo?) {
        if (node == null) return

        val rootPackage = node.packageName?.toString() ?: return
        if (!rootPackage.contains("packageinstaller") &&
            !rootPackage.contains("permissioncontroller") &&
            rootPackage != "android"
        ) {
            return
        }

        var isUninstallDialog = false
        var detectedAppPackage: String? = null

        scanNodeForUninstallDialog(node) { appPackage ->
            if (appPackage != null && protectedApps.contains(appPackage)) {
                isUninstallDialog = true
                detectedAppPackage = appPackage
            }
        }

        if (isUninstallDialog && detectedAppPackage != null) {
            val shouldBlock = shouldBlockRemoval()
            if (shouldBlock) {
                val currentTime = System.currentTimeMillis()
                if (isPasswordVerified && (currentTime - lastPasswordVerificationTime) < PASSWORD_VERIFICATION_COOLDOWN) {
                    return
                }

                if (currentTime - lastBlockTime > blockCooldown) {
                    lastBlockTime = currentTime
                    currentProtectionSession = "launcher_" + currentTime
                    handleAntiUninstallAttempt()
                }
            }
        }
    }

    private fun scanNodeForUninstallDialog(node: AccessibilityNodeInfo?, onAppDetected: (String?) -> Unit) {
        if (node == null) return

        val nodeText = node.text?.toString()?.lowercase(Locale.ROOT) ?: ""
        val nodeContentDesc = node.contentDescription?.toString()?.lowercase(Locale.ROOT) ?: ""

        val hasUninstallKeyword = nodeText.contains("uninstall") ||
            nodeText.contains("remove") ||
            nodeText.contains("delete") ||
            nodeContentDesc.contains("uninstall")

        if (hasUninstallKeyword) {
            for (packageName in protectedApps) {
                val appName = getAppName(packageName)
                if (nodeText.contains(appName.lowercase(Locale.ROOT)) ||
                    nodeText.contains(packageName) ||
                    nodeContentDesc.contains(appName.lowercase(Locale.ROOT))
                ) {
                    onAppDetected(packageName)
                    return
                }
            }
        }

        for (i in 0 until node.childCount) {
            scanNodeForUninstallDialog(node.getChild(i), onAppDetected)
        }
    }

    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast(".")
        }
    }

    private fun traverseAndDetectScreen(node: AccessibilityNodeInfo?, onScreenDetected: (String, String?) -> Unit) {
        if (node == null) return

        if (node.className != null && node.className == "android.widget.TextView") {
            val nodeText = node.text?.toString()?.lowercase(Locale.ROOT) ?: ""

            if ((nodeText.contains("device admin") || nodeText.contains("device administrators")) &&
                checkForDeenShieldMention(this.rootInActiveWindow)
            ) {
                if (checkForActionButton(this.rootInActiveWindow)) {
                    onScreenDetected("device_admin", null)
                    return
                }
            }

            if (nodeText.contains("deenshield") && nodeText.contains("accessibility")) {
                if (checkForToggleContext(this.rootInActiveWindow)) {
                    onScreenDetected("accessibility_deenshield", null)
                    return
                }
            }

            if (nodeText.contains("uninstall") || nodeText.contains("remove")) {
                for (packageName in protectedApps) {
                    val appName = packageName.substringAfterLast(".").lowercase(Locale.ROOT)
                    if (checkForAppMention(this.rootInActiveWindow, appName, packageName)) {
                        onScreenDetected("app_uninstall", packageName)
                        return
                    }
                }
            }
        }

        for (i in 0 until node.childCount) {
            traverseAndDetectScreen(node.getChild(i), onScreenDetected)
        }
    }

    private fun checkForActionButton(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false

        if (node.className != null &&
            (node.className.toString().contains("Button") || node.isClickable)
        ) {
            val nodeText = node.text?.toString()?.lowercase(Locale.ROOT) ?: ""
            if (nodeText.contains("deactivate") ||
                nodeText.contains("activate") ||
                nodeText.contains("remove") ||
                nodeText.contains("turn off")
            ) {
                return true
            }
        }

        for (i in 0 until node.childCount) {
            if (checkForActionButton(node.getChild(i))) return true
        }

        return false
    }

    private fun checkForToggleContext(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false

        if (node.className != null && node.className.toString().contains("Switch")) {
            return true
        }

        val nodeText = node.text?.toString()?.lowercase(Locale.ROOT) ?: ""
        if (nodeText.contains("use service") ||
            nodeText.contains("turn off") ||
            nodeText.contains("turn on")
        ) {
            return true
        }

        for (i in 0 until node.childCount) {
            if (checkForToggleContext(node.getChild(i))) return true
        }

        return false
    }

    private fun checkForAppMention(node: AccessibilityNodeInfo?, appName: String, packageName: String): Boolean {
        if (node == null) return false

        val nodeText = node.text?.toString()?.lowercase(Locale.ROOT) ?: ""
        val nodeContentDescription = node.contentDescription?.toString()?.lowercase(Locale.ROOT) ?: ""

        if (nodeText.contains(appName) ||
            nodeText.contains(packageName) ||
            nodeContentDescription.contains(appName) ||
            nodeContentDescription.contains(packageName)
        ) {
            return true
        }

        for (i in 0 until node.childCount) {
            if (checkForAppMention(node.getChild(i), appName, packageName)) return true
        }

        return false
    }

    private fun checkForDeenShieldMention(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false

        if (node.className != null && node.className == "android.widget.TextView") {
            val nodeText = node.text?.toString()?.lowercase(Locale.ROOT) ?: ""
            if (nodeText.contains("deenshield")) {
                return true
            }
        }

        for (i in 0 until node.childCount) {
            if (checkForDeenShieldMention(node.getChild(i))) return true
        }

        return false
    }

    private fun shouldBlockRemoval(): Boolean {
        if (!isAntiUninstallOn) {
            return false
        }

        return when (antiUninstallMode) {
            Constants.ANTI_UNINSTALL_PASSWORD_MODE -> true
            Constants.ANTI_UNINSTALL_TIMED_MODE -> checkIfDateNotReached()
            else -> false
        }
    }

    private fun checkIfDateNotReached(): Boolean {
        if (savedDate == null) return false

        return try {
            val parts = savedDate!!.split("/")
            if (parts.size != 3) return false

            val selectedDate = java.util.Calendar.getInstance()
            selectedDate.set(parts[2].toInt(), parts[0].toInt() - 1, parts[1].toInt(), 0, 0, 0)
            selectedDate.set(java.util.Calendar.MILLISECOND, 0)

            val today = java.util.Calendar.getInstance()
            today.set(java.util.Calendar.HOUR_OF_DAY, 0)
            today.set(java.util.Calendar.MINUTE, 0)
            today.set(java.util.Calendar.SECOND, 0)
            today.set(java.util.Calendar.MILLISECOND, 0)

            if (selectedDate.before(today) || selectedDate.equals(today)) {
                val info = getSharedPreferences("anti_uninstall", Context.MODE_PRIVATE)
                info.edit { putBoolean("is_anti_uninstall_on", false) }
                isAntiUninstallOn = false

                Handler(Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(
                        this,
                        getString(R.string.anti_uninstall_timed_mode_expired),
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
                false
            } else {
                selectedDate.after(today)
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun handleAntiUninstallAttempt() {
        val intent = Intent(this, com.alhaq.deenshield.ui.activity.AntiUninstallPasswordActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(refreshReceiver)
        } catch (_: Exception) {
        }
    }
}
