package com.alhaq.amnshield.services

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.content.edit
import com.alhaq.amnshield.Constants
import com.alhaq.amnshield.CrashLogger
import com.alhaq.amnshield.R
import com.alhaq.amnshield.blockers.AppBlocker
import com.alhaq.amnshield.blockers.FocusModeBlocker
import com.alhaq.amnshield.blockers.KeywordBlocker
import com.alhaq.amnshield.blockers.ReelBlocker
import com.alhaq.amnshield.blockers.ViewBlocker
import com.alhaq.amnshield.data.blockers.UnifiedFeatureScheduleRule
import com.alhaq.amnshield.premium.PremiumManager
import com.alhaq.amnshield.ui.activity.MainActivity
import com.alhaq.amnshield.ui.activity.WarningActivity
import com.alhaq.amnshield.utils.BlockingStatsManager
import com.alhaq.amnshield.utils.ScheduleUtils
import com.alhaq.amnshield.utils.TimeTools
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class DeenShieldAccessibilityService : BaseBlockingService() {

    companion object {
        const val INTENT_ACTION_REFRESH_APP_BLOCKER = "deenshield.refresh.appblocker"
        const val INTENT_ACTION_REFRESH_APP_BLOCKER_COOLDOWN = "deenshield.refresh.appblocker.cooldown"
        const val INTENT_ACTION_REFRESH_FOCUS_MODE = "deenshield.refresh.focusmode"
        const val INTENT_ACTION_REFRESH_BLOCKED_KEYWORD_LIST = "deenshield.refresh.keywords"
        const val INTENT_ACTION_REFRESH_VIEW_BLOCKER = "deenshield.refresh.viewblocker"
        const val INTENT_ACTION_REFRESH_VIEW_BLOCKER_COOLDOWN = "deenshield.refresh.viewblocker.cooldown"
        const val INTENT_ACTION_REFRESH_REEL_BLOCKER = "deenshield.refresh.reelblocker"
        const val INTENT_ACTION_REFRESH_REEL_BLOCKER_COOLDOWN = "deenshield.refresh.reelblocker.cooldown"
        const val INTENT_ACTION_REFRESH_UNIFIED_FEATURE_SCHEDULES = "deenshield.refresh.unified.feature.schedules"
        const val INTENT_ACTION_REFRESH_ANTI_UNINSTALL = ".deenshield.refresh.anti_uninstall"
        const val INTENT_ACTION_PASSWORD_VERIFIED = "deenshield.password.verified"

        private const val REEL_TRACKER_SCROLL_DEBOUNCE_MS = 800L
        private const val REEL_TRACKER_DUPLICATE_WINDOW_MS = 2_500L
        private const val REEL_TRACKER_SURFACE_REENTRY_MS = 10 * 60 * 1000L
        private const val UNIFIED_SCHEDULE_EVAL_INTERVAL_MS = 15_000L
    }

    private lateinit var appBlocker: AppBlocker
    private lateinit var focusModeBlocker: FocusModeBlocker
    private lateinit var keywordBlocker: KeywordBlocker
    private lateinit var reelBlocker: ReelBlocker
    private lateinit var viewBlocker: ViewBlocker
    private lateinit var blockingStatsManager: BlockingStatsManager
    private lateinit var premiumManager: PremiumManager
    private lateinit var crashLogger: CrashLogger

    private var appBlockerWarningConfig = MainActivity.WarningData()
    private var viewBlockerWarningConfig = MainActivity.WarningData()

    // Anti-uninstall protection state
    private var isAntiUninstallOn = false
    private var antiUninstallMode = -1
    private var savedPassword: String? = null
    private var savedDate: String? = null
    private var savedUnlockAtMillis: Long = 0L
    private var isConfiguringBlocked = false
    private var lastBlockTime = 0L
    private val blockCooldown = 2000L
    private var protectedApps: Set<String> = emptySet()

    // Password verification session management
    private var lastPasswordVerificationTime = 0L
    private val PASSWORD_VERIFICATION_COOLDOWN = 5 * 60 * 1000L
    private var isPasswordVerified = false
    private var lastReelCountRefreshTime = 0L
    private var lastUnifiedScheduleEvalTime = 0L
    private var cachedReelsScrolledToday = 0
    private var reelTrackerDate = TimeTools.getCurrentDate()
    private val lastReelTrackerHitTimes = mutableMapOf<String, Long>()
    private val lastReelTrackerSignatures = mutableMapOf<String, String>()
    private var cachedDefaultLauncher: String? = null

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val eventChannel = Channel<AccessibilityEvent>(Channel.CONFLATED) { droppedEvent ->
        droppedEvent.recycle()
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onServiceConnected() {
        super.onServiceConnected()

        appBlocker = AppBlocker()
        focusModeBlocker = FocusModeBlocker()
        keywordBlocker = KeywordBlocker(this)
        reelBlocker = ReelBlocker()
        viewBlocker = ViewBlocker()
        blockingStatsManager = BlockingStatsManager.getInstance(this)
        premiumManager = PremiumManager.getInstance(this)
        crashLogger = CrashLogger(this)

        setupAppBlocker()
        setupFocusMode()
        setupKeywordBlocker()
        setupReelBlocker()
        setupViewBlocker()
        setupAntiUninstall()
        cachedDefaultLauncher = getDefaultLauncherPackage()

        val filter = IntentFilter().apply {
            addAction(INTENT_ACTION_REFRESH_APP_BLOCKER)
            addAction(INTENT_ACTION_REFRESH_BLOCKED_KEYWORD_LIST)
            addAction(INTENT_ACTION_REFRESH_VIEW_BLOCKER)
            addAction(INTENT_ACTION_REFRESH_VIEW_BLOCKER_COOLDOWN)
            addAction(INTENT_ACTION_REFRESH_REEL_BLOCKER)
            addAction(INTENT_ACTION_REFRESH_REEL_BLOCKER_COOLDOWN)
            addAction(INTENT_ACTION_REFRESH_UNIFIED_FEATURE_SCHEDULES)
            addAction(INTENT_ACTION_REFRESH_APP_BLOCKER_COOLDOWN)
            addAction(INTENT_ACTION_REFRESH_FOCUS_MODE)
            addAction(INTENT_ACTION_REFRESH_ANTI_UNINSTALL)
            addAction(INTENT_ACTION_PASSWORD_VERIFIED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(refreshReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(refreshReceiver, filter)
        }

        evaluateUnifiedFeatureSchedulesIfNeeded(force = true)

        startBackgroundWorker()
    }

    private fun isSettingsApp(packageName: String?): Boolean {
        packageName ?: return false
        return packageName == "com.android.settings" ||
                packageName == "com.google.android.settings" ||
                packageName == "com.samsung.android.settings" ||
                packageName.endsWith(".settings")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val packageName = event.packageName?.toString() ?: return

        var rootNode: AccessibilityNodeInfo? = null
        try {
            if (packageName.equals("com.alhaq.amnshield", ignoreCase = true)) {
                return
            }

            if (packageName.equals("com.android.systemui", ignoreCase = true)) {
                return
            }

            evaluateUnifiedFeatureSchedulesIfNeeded()

            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                trackAppLaunch(packageName)
                cachedDefaultLauncher = getDefaultLauncherPackage()
            }

            rootNode = rootInActiveWindow
            val rootPackage = rootNode?.packageName?.toString() ?: packageName

            if (rootPackage.equals("com.alhaq.amnshield", ignoreCase = true) ||
                rootPackage.equals("com.android.systemui", ignoreCase = true)
            ) {
                return
            }

            if (isAntiUninstallOn && isSettingsApp(packageName) && rootNode != null) {
                checkAndBlockDangerousSettingsScreens(rootNode)
            }

            if (isAntiUninstallOn && rootNode != null) {
                checkAndBlockLauncherUninstall(rootNode)
            }

            val isPremiumUser = premiumManager.isPremium()

            if (isPremiumUser && savedPreferencesLoader.isFocusModeFeatureEnabled()) {
                val focusModeResult = focusModeBlocker.doesAppNeedToBeBlocked(packageName, cachedDefaultLauncher)
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

            val isFocusModeActive = isPremiumUser && savedPreferencesLoader.isFocusModeFeatureEnabled() && focusModeBlocker.focusModeData.isTurnedOn
            val isFocusBlockAllExSelectedActive = isFocusModeActive && focusModeBlocker.focusModeData.modeType == Constants.FOCUS_MODE_BLOCK_ALL_EX_SELECTED

            if (!isFocusBlockAllExSelectedActive) {
                if (isPremiumUser && savedPreferencesLoader.isAppBlockerFeatureEnabled()) {
                    val appBlockerResult = appBlocker.doesAppNeedToBeBlocked(packageName, savedPreferencesLoader)
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
            }

            val eventCopy = AccessibilityEvent.obtain(event)
            val sendResult = eventChannel.trySend(eventCopy)
            if (sendResult.isFailure) {
                eventCopy.recycle()
            }
        } catch (t: Throwable) {
            android.util.Log.e("DeenShield", "Accessibility pipeline error", t)
            crashLogger.logNonFatalError("AccessibilityService", "Pipeline error", Exception(t))
        } finally {
            rootNode?.recycle()
        }
    }

    private fun startBackgroundWorker() {
        serviceScope.launch {
            for (event in eventChannel) {
                try {
                    processDeferredChecks(event)
                } catch (t: Throwable) {
                    if (t is CancellationException) throw t
                    android.util.Log.e("DeenShield", "Deferred blocker worker error", t)
                    crashLogger.logNonFatalError("AccessibilityService", "Deferred blocker error", Exception(t))
                } finally {
                    event.recycle()
                }
            }
        }
    }

    private fun processDeferredChecks(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        val rootNode = rootInActiveWindow ?: return
        try {
            val rootPackage = rootNode.packageName?.toString() ?: packageName

            if (packageName.equals("com.alhaq.amnshield", ignoreCase = true) ||
                rootPackage.equals("com.alhaq.amnshield", ignoreCase = true) ||
                packageName.equals("com.android.systemui", ignoreCase = true) ||
                rootPackage.equals("com.android.systemui", ignoreCase = true)
            ) {
                return
            }

            val hasCoreKeywords = savedPreferencesLoader.isKeywordBlockerFeatureEnabled() &&
                savedPreferencesLoader.loadBlockedKeywords().isNotEmpty()
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

            if (premiumManager.isPremium()) {
                try {
                    refreshReelCountCacheIfNeeded()
                    reelBlocker.reelsScrolledToday = cachedReelsScrolledToday

                    val detectedReelSurface = reelBlocker.detectReelSurfaceId(rootNode, packageName)
                    if (detectedReelSurface != null) {
                        trackReelExposure(packageName, detectedReelSurface, event)
                    }

                    val reelBlockerResult = reelBlocker.doesReelNeedToBeBlocked(rootNode, packageName)
                    if (reelBlockerResult != null && reelBlockerResult.isBlocked) {
                        blockingStatsManager.recordViewBlock(packageName, reelBlockerResult.viewId)
                        handleReelBlockerResult(reelBlockerResult)
                        return
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DeenShield", "Reel blocker error", e)
                }
            }
        } finally {
            rootNode.recycle()
        }
    }

    private fun handleReelBlockerResult(result: ReelBlocker.ReelBlockerResult?) {
        if (result == null || !result.isBlocked) return

        pressBack()

        if (viewBlockerWarningConfig.isWarningDialogHidden) return
        val dialogIntent = Intent(this, WarningActivity::class.java)
        dialogIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        dialogIntent.putExtra("mode", Constants.WARNING_SCREEN_MODE_VIEW_BLOCKER)
        dialogIntent.putExtra("result_id", result.viewId)
        dialogIntent.putExtra("is_press_home", result.requestHomePressInstead)
        dialogIntent.putExtra("is_reel_blocker", true)
        startActivity(dialogIntent)
    }

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            when (intent.action) {
                INTENT_ACTION_REFRESH_APP_BLOCKER -> setupAppBlocker()
                INTENT_ACTION_REFRESH_BLOCKED_KEYWORD_LIST -> setupKeywordBlocker()
                INTENT_ACTION_REFRESH_VIEW_BLOCKER -> setupViewBlocker()
                INTENT_ACTION_REFRESH_REEL_BLOCKER -> setupReelBlocker()
                INTENT_ACTION_REFRESH_FOCUS_MODE -> setupFocusMode()
                INTENT_ACTION_REFRESH_UNIFIED_FEATURE_SCHEDULES -> evaluateUnifiedFeatureSchedulesIfNeeded(force = true)
                INTENT_ACTION_REFRESH_ANTI_UNINSTALL -> setupAntiUninstall()
                INTENT_ACTION_REFRESH_VIEW_BLOCKER_COOLDOWN -> {
                    val interval = intent.getIntExtra("selected_time", viewBlockerWarningConfig.timeInterval)
                    val endTime = System.currentTimeMillis() + interval
                    viewBlocker.applyCooldown(
                        intent.getStringExtra("result_id") ?: "xxxxxxxxxxxxxx",
                        endTime
                    )
                    savedPreferencesLoader.saveViewBlockerCooldownData(viewBlocker.getCooldownSnapshot())
                }
                INTENT_ACTION_REFRESH_REEL_BLOCKER_COOLDOWN -> {
                    val interval = intent.getIntExtra("selected_time", viewBlockerWarningConfig.timeInterval)
                    val endTime = System.currentTimeMillis() + interval
                    reelBlocker.applyCooldown(
                        intent.getStringExtra("result_id") ?: "xxxxxxxxxxxxxx",
                        endTime
                    )
                    savedPreferencesLoader.saveReelBlockerCooldownData(reelBlocker.getCooldownSnapshot())
                }

                INTENT_ACTION_REFRESH_APP_BLOCKER_COOLDOWN -> {
                    val interval = intent.getIntExtra("selected_time", appBlockerWarningConfig.timeInterval)
                    val endTime = System.currentTimeMillis() + interval
                    appBlocker.putCooldownTo(
                        intent.getStringExtra("result_id") ?: "xxxxxxxxxxxxxx",
                        endTime
                    )
                    savedPreferencesLoader.saveAppBlockerCooldownData(appBlocker.getCooldownSnapshot())
                }

                INTENT_ACTION_PASSWORD_VERIFIED -> {
                    isPasswordVerified = true
                    lastPasswordVerificationTime = System.currentTimeMillis()
                }
            }
        }
    }

    private fun setupAppBlocker() {
        appBlockerWarningConfig = savedPreferencesLoader.loadAppBlockerWarningInfo()
        appBlocker.blockedApps = savedPreferencesLoader.loadBlockedApps().toHashSet()
        appBlocker.restoreCooldowns(savedPreferencesLoader.loadAppBlockerCooldownData())
        appBlocker.refreshCheatHoursData(savedPreferencesLoader.loadAppBlockerCheatHoursList())
        appBlocker.refreshScheduleRules(savedPreferencesLoader.loadAppBlockerScheduleRules())
        savedPreferencesLoader.saveAppBlockerCooldownData(appBlocker.getCooldownSnapshot())
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

        userIgnoredPackages.add("com.alhaq.amnshield")
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
        viewBlocker.restoreCooldowns(savedPreferencesLoader.loadViewBlockerCooldownData())
        savedPreferencesLoader.saveViewBlockerCooldownData(viewBlocker.getCooldownSnapshot())
    }

    private fun setupReelBlocker() {
        val viewBlockerPrefs = getSharedPreferences("view_blocker", Context.MODE_PRIVATE)
        val reelBlockerPrefs = getSharedPreferences("reel_blocker", Context.MODE_PRIVATE)
        val configReelsPrefs = getSharedPreferences("config_reels", Context.MODE_PRIVATE)
        val cheatHoursPrefs = getSharedPreferences("cheat_hours", Context.MODE_PRIVATE)

        reelBlocker.isEnabled = savedPreferencesLoader.isReelBlockerEnabled(
            viewBlockerPrefs.getBoolean("is_enabled", false)
        )
        reelBlocker.isIGInboxReelAllowed = configReelsPrefs.getBoolean("is_reel_inbox", false)
        reelBlocker.isFirstReelInFeedAllowed = configReelsPrefs.getBoolean("is_reel_first", false)
        reelBlocker.modeType = savedPreferencesLoader.getReelBlockerMode(ReelBlocker.MODE_BLOCK_ALL)
        reelBlocker.dailyReelLimit = savedPreferencesLoader.getReelBlockerDailyLimit(200)
        reelBlocker.cheatMinuteStartTime = cheatHoursPrefs.getInt("view_blocker_start_time", -1)
        reelBlocker.cheatMinutesEndTime = cheatHoursPrefs.getInt("view_blocker_end_time", -1)

        reelBlocker.isYoutubeEnabled = reelBlockerPrefs.getBoolean("is_youtube_enabled", true)
        reelBlocker.isInstagramEnabled = reelBlockerPrefs.getBoolean("is_instagram_enabled", true)
        reelBlocker.isTiktokEnabled = reelBlockerPrefs.getBoolean("is_tiktok_enabled", true)
        reelBlocker.isBrowserShortsEnabled = reelBlockerPrefs.getBoolean("is_browser_enabled", false)

        reelBlocker.restoreCooldowns(savedPreferencesLoader.loadReelBlockerCooldownData())
        savedPreferencesLoader.saveReelBlockerCooldownData(reelBlocker.getCooldownSnapshot())
        refreshReelCountCacheIfNeeded(force = true)
        reelBlocker.reelsScrolledToday = cachedReelsScrolledToday
    }

    private fun refreshReelCountCacheIfNeeded(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && (now - lastReelCountRefreshTime) < 15_000) {
            return
        }

        val todayKey = TimeTools.getCurrentDate()
        cachedReelsScrolledToday = savedPreferencesLoader.getReelsScrolled()[todayKey] ?: 0
        lastReelCountRefreshTime = now
    }

    private fun trackReelExposure(packageName: String, surfaceId: String, event: AccessibilityEvent) {
        if (!savedPreferencesLoader.isUsageTrackerFeatureEnabled()) {
            return
        }

        val trackerPrefs = getSharedPreferences("config_tracker", Context.MODE_PRIVATE)
        if (!trackerPrefs.getBoolean("is_reel_counter", true)) {
            return
        }

        val today = TimeTools.getCurrentDate()
        if (reelTrackerDate != today) {
            reelTrackerDate = today
            lastReelTrackerHitTimes.clear()
            lastReelTrackerSignatures.clear()
        }

        val key = "$packageName|$surfaceId"
        val now = System.currentTimeMillis()
        if (!shouldTrackReelExposure(event, key, now)) {
            return
        }
        lastReelTrackerHitTimes[key] = now

        val reelsData = savedPreferencesLoader.getReelsScrolled()
        val currentCount = reelsData[today] ?: 0
        val updatedCount = currentCount + 1
        reelsData[today] = updatedCount
        savedPreferencesLoader.saveReelsScrolled(reelsData)

        cachedReelsScrolledToday = updatedCount
        lastReelCountRefreshTime = now

        val legacyMetricsPrefs = getSharedPreferences("usage_metrics", Context.MODE_PRIVATE)
        legacyMetricsPrefs.edit { putInt("total_reels", updatedCount) }
    }

    private fun trackAppLaunch(packageName: String) {
        try {
            savedPreferencesLoader.trackAppLaunch(packageName)
        } catch (e: Exception) {
            android.util.Log.e("DeenShield", "Error tracking app launch", e)
        }
    }

    private fun getDefaultLauncherPackage(): String? {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            resolveInfo?.activityInfo?.packageName
        } catch (e: Exception) {
            null
        }
    }

    private fun shouldTrackReelExposure(event: AccessibilityEvent, key: String, now: Long): Boolean {
        val lastSeen = lastReelTrackerHitTimes[key]
        if (lastSeen == null) {
            lastReelTrackerSignatures[key] = buildReelTrackerSignature(event)
            return true
        }

        return when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                val elapsed = now - lastSeen
                if (elapsed < REEL_TRACKER_SCROLL_DEBOUNCE_MS) return false

                val signature = buildReelTrackerSignature(event)
                val lastSignature = lastReelTrackerSignatures[key]
                if (signature == lastSignature && elapsed < REEL_TRACKER_DUPLICATE_WINDOW_MS) {
                    return false
                }

                lastReelTrackerSignatures[key] = signature
                true
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                (now - lastSeen) >= REEL_TRACKER_SURFACE_REENTRY_MS
            }
            else -> false
        }
    }

    private fun buildReelTrackerSignature(event: AccessibilityEvent): String {
        return "${event.eventType}|${event.fromIndex}|${event.toIndex}|${event.itemCount}|" +
            "${event.scrollX}|${event.scrollY}|${event.maxScrollX}|${event.maxScrollY}|${event.contentChangeTypes}"
    }

    private fun setupFocusMode() {
        val focusModeData = savedPreferencesLoader.getFocusModeData()
        focusModeBlocker.update(focusModeData)
    }

    private fun evaluateUnifiedFeatureSchedulesIfNeeded(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && (now - lastUnifiedScheduleEvalTime) < UNIFIED_SCHEDULE_EVAL_INTERVAL_MS) {
            return
        }
        lastUnifiedScheduleEvalTime = now

        val rules = savedPreferencesLoader.loadUnifiedFeatureScheduleRules()
        if (rules.isEmpty()) return

        UnifiedFeatureScheduleRule.FeatureTarget.entries.forEach { target ->
            val targetRules = rules.filter { it.targets.contains(target) }
            if (targetRules.isEmpty()) return@forEach

            val activeCheat = targetRules.firstOrNull {
                it.type == UnifiedFeatureScheduleRule.RuleType.CHEAT && isUnifiedRuleActive(it, now)
            }
            val activeBlock = targetRules.firstOrNull {
                it.type == UnifiedFeatureScheduleRule.RuleType.BLOCK && isUnifiedRuleActive(it, now)
            }

            // Priority: Active Cheat > Active Block > Manual User Setting
            when {
                activeCheat != null -> applyUnifiedFeatureState(target, false)
                activeBlock != null -> applyUnifiedFeatureState(target, true)
                // If no schedule is active, we don't change the state.
                // This lets the user's manual toggle in the UI remain in control.
            }
        }
    }

    private fun applyUnifiedFeatureState(
        target: UnifiedFeatureScheduleRule.FeatureTarget,
        enabled: Boolean
    ) {
        when (target) {
            UnifiedFeatureScheduleRule.FeatureTarget.APP_BLOCKER -> {
                if (savedPreferencesLoader.isAppBlockerFeatureEnabled() != enabled) {
                    savedPreferencesLoader.setAppBlockerFeatureEnabled(enabled)
                    setupAppBlocker()
                }
            }

            UnifiedFeatureScheduleRule.FeatureTarget.KEYWORD_BLOCKER -> {
                if (savedPreferencesLoader.isKeywordBlockerFeatureEnabled() != enabled) {
                    savedPreferencesLoader.setKeywordBlockerFeatureEnabled(enabled)
                    setupKeywordBlocker()
                }
            }

            UnifiedFeatureScheduleRule.FeatureTarget.REEL_BLOCKER -> {
                if (savedPreferencesLoader.isReelBlockerEnabled() != enabled) {
                    savedPreferencesLoader.setReelBlockerEnabled(enabled)
                    setupReelBlocker()
                }
            }

            UnifiedFeatureScheduleRule.FeatureTarget.FOCUS_MODE -> {
                if (savedPreferencesLoader.isFocusModeFeatureEnabled() != enabled) {
                    savedPreferencesLoader.setFocusModeFeatureEnabled(enabled)
                    setupFocusMode()
                }
            }
        }
    }

    private fun isUnifiedRuleActive(rule: UnifiedFeatureScheduleRule, nowMillis: Long): Boolean {
        return when (rule.recurrence) {
            UnifiedFeatureScheduleRule.Recurrence.ALWAYS -> true
            UnifiedFeatureScheduleRule.Recurrence.HOURLY -> rule.activeUntilMillis > nowMillis
            UnifiedFeatureScheduleRule.Recurrence.DAILY -> {
                ScheduleUtils.isDailyWindowActive(rule.startMinute, rule.endMinute, nowMillis)
            }
            UnifiedFeatureScheduleRule.Recurrence.WEEKLY -> {
                ScheduleUtils.isWeeklyWindowActive(rule.startMinute, rule.endMinute, rule.selectedDays, nowMillis)
            }
        }
    }

    private fun setupAntiUninstall() {
        val info = getSharedPreferences("anti_uninstall", Context.MODE_PRIVATE)
        isAntiUninstallOn = info.getBoolean("is_anti_uninstall_on", false)
        antiUninstallMode = info.getInt("mode", -1)
        savedPassword = info.getString("password", null)
        savedDate = info.getString("date", null)
        savedUnlockAtMillis = info.getLong("unlock_at_millis", 0L)
        isConfiguringBlocked = info.getBoolean("is_configuring_blocked", false)

        protectedApps = if (isAntiUninstallOn) setOf("com.alhaq.amnshield") else emptySet()
    }

    private fun checkAndBlockDangerousSettingsScreens(node: AccessibilityNodeInfo?) {
        if (node == null) return

        var isDangerousScreen = false
        traverseAndDetectScreen(node, node) { detectedType, appPackage ->
            when (detectedType) {
                "device_admin" -> if (isAntiUninstallOn && isConfiguringBlocked) isDangerousScreen = true
                "accessibility_deenshield" -> if (isAntiUninstallOn && isConfiguringBlocked) isDangerousScreen = true
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
            val child = node.getChild(i)
            if (child != null) {
                scanNodeForUninstallDialog(child, onAppDetected)
                child.recycle()
            }
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

    private fun traverseAndDetectScreen(
        node: AccessibilityNodeInfo?, 
        windowRoot: AccessibilityNodeInfo?, 
        onScreenDetected: (String, String?) -> Unit
    ) {
        if (node == null || windowRoot == null) return

        if (node.className != null && node.className == "android.widget.TextView") {
            val nodeText = node.text?.toString()?.lowercase(Locale.ROOT) ?: ""

            if ((nodeText.contains("device admin") || nodeText.contains("device administrators")) &&
                checkForDeenShieldMention(windowRoot)
            ) {
                if (checkForActionButton(windowRoot)) {
                    onScreenDetected("device_admin", null)
                    return
                }
            }

            if (nodeText.contains("deenshield") && nodeText.contains("accessibility")) {
                if (checkForToggleContext(windowRoot)) {
                    onScreenDetected("accessibility_deenshield", null)
                    return
                }
            }

            if (nodeText.contains("uninstall") || nodeText.contains("remove")) {
                for (packageName in protectedApps) {
                    val appName = packageName.substringAfterLast(".").lowercase(Locale.ROOT)
                    if (checkForAppMention(windowRoot, appName, packageName)) {
                        onScreenDetected("app_uninstall", packageName)
                        return
                    }
                }
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                traverseAndDetectScreen(child, windowRoot, onScreenDetected)
                child.recycle()
            }
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
            val child = node.getChild(i)
            if (child != null) {
                val found = checkForActionButton(child)
                child.recycle()
                if (found) return true
            }
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
            val child = node.getChild(i)
            if (child != null) {
                val found = checkForToggleContext(child)
                child.recycle()
                if (found) return true
            }
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
            val child = node.getChild(i)
            if (child != null) {
                val found = checkForAppMention(child, appName, packageName)
                child.recycle()
                if (found) return true
            }
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
            val child = node.getChild(i)
            if (child != null) {
                val found = checkForDeenShieldMention(child)
                child.recycle()
                if (found) return true
            }
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
        if (savedUnlockAtMillis > 0L) {
            val now = System.currentTimeMillis()
            if (now > savedUnlockAtMillis) {
                val info = getSharedPreferences("anti_uninstall", Context.MODE_PRIVATE)
                info.edit {
                    putBoolean("is_anti_uninstall_on", false)
                    remove("unlock_at_millis")
                }
                isAntiUninstallOn = false

                Handler(Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(
                        this,
                        getString(R.string.anti_uninstall_timed_mode_expired),
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
                return false
            }
            return true
        }

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
        val intent = Intent(this, com.alhaq.amnshield.ui.activity.AntiUninstallPasswordActivity::class.java).apply {
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
        eventChannel.close()
        serviceScope.cancel()
    }
}
