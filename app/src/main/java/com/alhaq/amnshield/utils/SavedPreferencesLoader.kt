package com.alhaq.amnshield.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.alhaq.amnshield.blockers.ReelBlocker
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.alhaq.amnshield.blockers.FocusModeBlocker
import com.alhaq.amnshield.data.blockers.AppBlockScheduleRule
import com.alhaq.amnshield.data.blockers.UnifiedFeatureScheduleRule
import com.alhaq.amnshield.ui.activity.MainActivity
import java.util.Calendar
import java.util.UUID
import java.util.ArrayList

class SavedPreferencesLoader(private val context: Context) {

    fun loadPinnedApps(): Set<String> {
        val sharedPreferences =
            context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        return sharedPreferences.getStringSet("pinned_apps", emptySet()) ?: emptySet()
    }


    fun loadIgnoredAppUsageTracker(): Set<String> {
        val sharedPreferences =
            context.getSharedPreferences("app_usage_tracker", Context.MODE_PRIVATE)
        return sharedPreferences.getStringSet("ignored_apps", emptySet()) ?: emptySet()
    }

    fun loadBlockedApps(): Set<String> {
        val sharedPreferences =
            context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        return sharedPreferences.getStringSet("blocked_apps", emptySet()) ?: emptySet()
    }

    fun saveAppBlockerCooldownData(cooldowns: Map<String, Long>) {
        val sharedPreferences =
            context.getSharedPreferences("app_blocker", Context.MODE_PRIVATE)
        val json = Gson().toJson(cooldowns)
        // apply() is asynchronous; cooldown data is non-critical and called frequently
        // from the accessibility hot path so we avoid synchronous disk writes here.
        sharedPreferences.edit().putString("cooldown_data", json).apply()
    }

    fun loadAppBlockerCooldownData(): MutableMap<String, Long> {
        val sharedPreferences =
            context.getSharedPreferences("app_blocker", Context.MODE_PRIVATE)
        val json = sharedPreferences.getString("cooldown_data", null)
        if (json.isNullOrEmpty()) return mutableMapOf()

        val type = object : TypeToken<MutableMap<String, Long>>() {}.type
        return Gson().fromJson(json, type)
    }

    fun saveViewBlockerCooldownData(cooldowns: Map<String, Long>) {
        val sharedPreferences =
            context.getSharedPreferences("view_blocker", Context.MODE_PRIVATE)
        val json = Gson().toJson(cooldowns)
        sharedPreferences.edit().putString("cooldown_data", json).apply()
    }

    fun loadViewBlockerCooldownData(): MutableMap<String, Long> {
        val sharedPreferences =
            context.getSharedPreferences("view_blocker", Context.MODE_PRIVATE)
        val json = sharedPreferences.getString("cooldown_data", null)
        if (json.isNullOrEmpty()) return mutableMapOf()

        val type = object : TypeToken<MutableMap<String, Long>>() {}.type
        return Gson().fromJson(json, type)
    }

    fun saveReelBlockerCooldownData(cooldowns: Map<String, Long>) {
        val sharedPreferences =
            context.getSharedPreferences("reel_blocker", Context.MODE_PRIVATE)
        val json = Gson().toJson(cooldowns)
        sharedPreferences.edit().putString("cooldown_data", json).apply()
    }

    fun loadReelBlockerCooldownData(): MutableMap<String, Long> {
        val sharedPreferences =
            context.getSharedPreferences("reel_blocker", Context.MODE_PRIVATE)
        val json = sharedPreferences.getString("cooldown_data", null)
        if (json.isNullOrEmpty()) return mutableMapOf()

        val type = object : TypeToken<MutableMap<String, Long>>() {}.type
        return Gson().fromJson(json, type)
    }

    fun isReelBlockerEnabled(defaultValue: Boolean = false): Boolean {
        val sharedPreferences =
            context.getSharedPreferences("reel_blocker", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("is_enabled", defaultValue)
    }

    fun setReelBlockerEnabled(enabled: Boolean) {
        val sharedPreferences =
            context.getSharedPreferences("reel_blocker", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("is_enabled", enabled).apply()
    }

    fun getReelBlockerMode(defaultMode: Int = ReelBlocker.MODE_BLOCK_ALL): Int {
        val sharedPreferences =
            context.getSharedPreferences("reel_blocker", Context.MODE_PRIVATE)
        return sharedPreferences.getInt("mode_type", defaultMode)
    }

    fun setReelBlockerMode(mode: Int) {
        val sharedPreferences =
            context.getSharedPreferences("reel_blocker", Context.MODE_PRIVATE)
        sharedPreferences.edit().putInt("mode_type", mode).apply()
    }

    fun getReelBlockerDailyLimit(defaultLimit: Int = 200): Int {
        val sharedPreferences =
            context.getSharedPreferences("reel_blocker", Context.MODE_PRIVATE)
        return sharedPreferences.getInt("daily_limit", defaultLimit)
    }

    fun setReelBlockerDailyLimit(limit: Int) {
        val sharedPreferences =
            context.getSharedPreferences("reel_blocker", Context.MODE_PRIVATE)
        sharedPreferences.edit().putInt("daily_limit", limit).apply()
    }

    // -- Reel Blocker per-platform / browser toggles ---------------------------
    // Each toggle defaults to `true` for native platforms and `false` for browsers
    // so existing users keep current behavior, and the browser feature stays opt-in.

    fun isReelBlockerYoutubeEnabled(default: Boolean = true): Boolean =
        context.getSharedPreferences("reel_blocker", Context.MODE_PRIVATE)
            .getBoolean("is_youtube_enabled", default)

    fun setReelBlockerYoutubeEnabled(enabled: Boolean) {
        context.getSharedPreferences("reel_blocker", Context.MODE_PRIVATE)
            .edit().putBoolean("is_youtube_enabled", enabled).apply()
    }

    fun isReelBlockerInstagramEnabled(default: Boolean = true): Boolean =
        context.getSharedPreferences("reel_blocker", Context.MODE_PRIVATE)
            .getBoolean("is_instagram_enabled", default)

    fun setReelBlockerInstagramEnabled(enabled: Boolean) {
        context.getSharedPreferences("reel_blocker", Context.MODE_PRIVATE)
            .edit().putBoolean("is_instagram_enabled", enabled).apply()
    }

    fun isReelBlockerTiktokEnabled(default: Boolean = true): Boolean =
        context.getSharedPreferences("reel_blocker", Context.MODE_PRIVATE)
            .getBoolean("is_tiktok_enabled", default)

    fun setReelBlockerTiktokEnabled(enabled: Boolean) {
        context.getSharedPreferences("reel_blocker", Context.MODE_PRIVATE)
            .edit().putBoolean("is_tiktok_enabled", enabled).apply()
    }

    fun isReelBlockerBrowserEnabled(default: Boolean = false): Boolean =
        context.getSharedPreferences("reel_blocker", Context.MODE_PRIVATE)
            .getBoolean("is_browser_enabled", default)

    fun setReelBlockerBrowserEnabled(enabled: Boolean) {
        context.getSharedPreferences("reel_blocker", Context.MODE_PRIVATE)
            .edit().putBoolean("is_browser_enabled", enabled).apply()
    }

    fun loadBlockedKeywords(): Set<String> {
        val sharedPreferences =
            context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        return sharedPreferences.getStringSet("blocked_keywords", emptySet()) ?: emptySet()
    }

    fun savePinned(pinnedApps: Set<String>) {
        val sharedPreferences =
            context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().putStringSet("pinned_apps", pinnedApps).apply()
    }


    fun saveBlockedApps(pinnedApps: Set<String>) {
        val sharedPreferences =
            context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().putStringSet("blocked_apps", pinnedApps).apply()
    }


    fun saveIgnoredAppUsageTracker(ignoredApps: Set<String>) {
        val sharedPreferences =
            context.getSharedPreferences("app_usage_tracker", Context.MODE_PRIVATE)
        sharedPreferences.edit().putStringSet("ignored_apps", ignoredApps).apply()
    }


    fun saveBlockedKeywords(pinnedApps: Set<String>) {
        val sharedPreferences =
            context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().putStringSet("blocked_keywords", pinnedApps).apply()
    }
    private data class LegacyAutoTimedActionItem(
        val title: String = "",
        val startTimeInMins: Int = 0,
        val endTimeInMins: Int = 0,
        val packages: ArrayList<String> = ArrayList()
    )

    fun migrateLegacySchedulesIfNeeded() {
        val migrationPrefs = context.getSharedPreferences("schedules_migration", Context.MODE_PRIVATE)
        if (migrationPrefs.getBoolean("migrated_v2", false)) {
            return
        }

        // 1. Migrate App Blocker Cheat Hours
        val cheatHoursPrefs = context.getSharedPreferences("cheat_hours", Context.MODE_PRIVATE)
        val legacyCheatJson = cheatHoursPrefs.getString("cheatHoursList", null)
        if (!legacyCheatJson.isNullOrEmpty()) {
            try {
                val legacyType = object : TypeToken<List<LegacyAutoTimedActionItem>>() {}.type
                val legacyList = Gson().fromJson<List<LegacyAutoTimedActionItem>>(legacyCheatJson, legacyType) ?: emptyList()
                val currentRules = loadAppBlockerScheduleRules()
                legacyList.forEach { item ->
                    item.packages.forEach { pkg ->
                        val rule = AppBlockScheduleRule(
                            id = UUID.randomUUID().toString(),
                            title = item.title,
                            packageName = pkg,
                            type = AppBlockScheduleRule.RuleType.CHEAT,
                            recurrence = AppBlockScheduleRule.Recurrence.DAILY,
                            startMinute = item.startTimeInMins,
                            endMinute = item.endTimeInMins
                        )
                        currentRules.add(rule)
                    }
                }
                saveAppBlockerScheduleRules(currentRules)
            } catch (e: Exception) {
                Log.e("SavedPreferencesLoader", "Error migrating app blocker cheat hours", e)
            }
        }

        // 2. Migrate Focus Mode Auto-Focus Hours
        val focusHoursPrefs = context.getSharedPreferences("auto_focus_hours", Context.MODE_PRIVATE)
        val legacyFocusJson = focusHoursPrefs.getString("auto_focus_list", null)
        if (!legacyFocusJson.isNullOrEmpty()) {
            try {
                val legacyType = object : TypeToken<List<LegacyAutoTimedActionItem>>() {}.type
                val legacyList = Gson().fromJson<List<LegacyAutoTimedActionItem>>(legacyFocusJson, legacyType) ?: emptyList()
                val currentUnifiedRules = loadUnifiedFeatureScheduleRules()
                legacyList.forEach { item ->
                    val rule = UnifiedFeatureScheduleRule(
                        id = UUID.randomUUID().toString(),
                        title = item.title,
                        type = UnifiedFeatureScheduleRule.RuleType.BLOCK,
                        recurrence = UnifiedFeatureScheduleRule.Recurrence.DAILY,
                        targets = setOf(UnifiedFeatureScheduleRule.FeatureTarget.FOCUS_MODE),
                        startMinute = item.startTimeInMins,
                        endMinute = item.endTimeInMins
                    )
                    currentUnifiedRules.add(rule)
                }
                saveUnifiedFeatureScheduleRules(currentUnifiedRules)
            } catch (e: Exception) {
                Log.e("SavedPreferencesLoader", "Error migrating focus mode auto-focus hours", e)
            }
        }

        // 3. Migrate Reel Blocker Cheat Hours
        val reelBlockerStartTime = cheatHoursPrefs.getInt("view_blocker_start_time", -1)
        val reelBlockerEndTime = cheatHoursPrefs.getInt("view_blocker_end_time", -1)
        if (reelBlockerStartTime != -1 && reelBlockerEndTime != -1) {
            try {
                val currentUnifiedRules = loadUnifiedFeatureScheduleRules()
                val rule = UnifiedFeatureScheduleRule(
                    id = UUID.randomUUID().toString(),
                    title = "Reel Blocker Cheat Hours",
                    type = UnifiedFeatureScheduleRule.RuleType.CHEAT,
                    recurrence = UnifiedFeatureScheduleRule.Recurrence.DAILY,
                    targets = setOf(UnifiedFeatureScheduleRule.FeatureTarget.REEL_BLOCKER),
                    startMinute = reelBlockerStartTime,
                    endMinute = reelBlockerEndTime
                )
                currentUnifiedRules.add(rule)
                saveUnifiedFeatureScheduleRules(currentUnifiedRules)
            } catch (e: Exception) {
                Log.e("SavedPreferencesLoader", "Error migrating reel blocker cheat hours", e)
            }
        }

        // 4. Mark migration as complete
        migrationPrefs.edit().putBoolean("migrated_v2", true).apply()

        // 5. Clean up old preferences keys/files
        cheatHoursPrefs.edit().remove("cheatHoursList").remove("view_blocker_start_time").remove("view_blocker_end_time").apply()
        focusHoursPrefs.edit().remove("auto_focus_list").apply()
    }

    fun saveAppBlockerWarningInfo(warningData: MainActivity.WarningData) {
        val sharedPreferences = context.getSharedPreferences("warning_data", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()

        val json = gson.toJson(warningData)

        editor.putString("app_blocker", json)
        editor.apply()
    }

    fun loadAppBlockerWarningInfo(): MainActivity.WarningData {
        val sharedPreferences = context.getSharedPreferences("warning_data", Context.MODE_PRIVATE)
        val gson = Gson()

        val json = sharedPreferences.getString("app_blocker", null)

        if (json.isNullOrEmpty()) return MainActivity.WarningData()

        val type = object : TypeToken<MainActivity.WarningData>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveViewBlockerWarningInfo(warningData: MainActivity.WarningData) {
        val sharedPreferences = context.getSharedPreferences("warning_data", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()

        val json = gson.toJson(warningData)

        editor.putString("view_blocker", json)
        editor.apply()
    }

    fun saveCheatHoursForViewBlocker(startTime: Int, endTime: Int) {
        val sharedPreferences = context.getSharedPreferences("cheat_hours", Context.MODE_PRIVATE)
        val edit = sharedPreferences.edit()
        edit.putInt("view_blocker_start_time", startTime)
        edit.putInt("view_blocker_end_time", endTime)
        edit.apply()
    }

    fun loadViewBlockerWarningInfo(): MainActivity.WarningData {
        val sharedPreferences = context.getSharedPreferences("warning_data", Context.MODE_PRIVATE)
        val gson = Gson()

        val json = sharedPreferences.getString("view_blocker", null)

        if (json.isNullOrEmpty()) return MainActivity.WarningData()

        val type = object : TypeToken<MainActivity.WarningData>() {}.type
        return gson.fromJson(json, type)
    }



    fun saveFocusModeData(focusModeData: FocusModeBlocker.FocusModeData) {
        val sharedPreferences =
            context.getSharedPreferences("focus_mode", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()

        val json = gson.toJson(focusModeData)

        editor.putString("focus_mode", json)
        editor.apply()
    }


    fun getFocusModeData(): FocusModeBlocker.FocusModeData {

        val sharedPreferences =
            context.getSharedPreferences("focus_mode", Context.MODE_PRIVATE)
        val gson = Gson()

        val json = sharedPreferences.getString("focus_mode", null)

        val data = if (json.isNullOrEmpty()) {
            FocusModeBlocker.FocusModeData()
        } else {
            val type = object : TypeToken<FocusModeBlocker.FocusModeData>() {}.type
            try {
                gson.fromJson<FocusModeBlocker.FocusModeData>(json, type) ?: FocusModeBlocker.FocusModeData()
            } catch (e: Exception) {
                FocusModeBlocker.FocusModeData()
            }
        }
        
        data.selectedApps = getFocusModeSelectedApps().toHashSet()
        return data
    }
    
    fun saveFocusSessionStartTime(startTime: Long, endTime: Long) {
        val sharedPreferences = context.getSharedPreferences("focus_mode", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putLong("focus_session_start", startTime)
            .putLong("focus_session_end", endTime)
            .apply()
    }
    
    fun completeFocusSession() {
        val sharedPreferences = context.getSharedPreferences("focus_mode", Context.MODE_PRIVATE)
        val startTime = sharedPreferences.getLong("focus_session_start", -1)
        val endTime = sharedPreferences.getLong("focus_session_end", -1)
        
        if (startTime != -1L && endTime != -1L) {
            // Record the completed focus session
            BlockingStatsManager.getInstance(context).recordFocusSession(startTime, endTime)
            
            // Clear the session data
            sharedPreferences.edit()
                .remove("focus_session_start")
                .remove("focus_session_end")
                .apply()
        }
    }

    fun saveFocusModeSelectedApps(appList: List<String>) {
        val sharedPreferences =
            context.getSharedPreferences("focus_mode", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()

        val json = gson.toJson(appList)

        editor.putString("selected_apps", json)
        editor.apply()
    }

    fun getFocusModeSelectedApps(): List<String> {
        val sharedPreferences =
            context.getSharedPreferences("focus_mode", Context.MODE_PRIVATE)
        val gson = Gson()

        val json = sharedPreferences.getString("selected_apps", null)

        if (json.isNullOrEmpty()) return listOf()

        val type =
            object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveKeywordBlockerIgnoredApps(appList: List<String>) {
        val sharedPreferences =
            context.getSharedPreferences("Keyword_blocker_ignored_apps", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()

        val json = gson.toJson(appList)

        editor.putString("selected_apps", json)
        editor.apply()
    }

    fun getKeywordBlockerIgnoredApps(): List<String> {
        val sharedPreferences =
            context.getSharedPreferences("Keyword_blocker_ignored_apps", Context.MODE_PRIVATE)
        val gson = Gson()

        val json = sharedPreferences.getString("selected_apps", null)

        if (json.isNullOrEmpty()) return listOf()

        val type =
            object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type)
    }


    fun setOverlayApps(selectedApps: Set<String>) {
        val sharedPreferences =
            context.getSharedPreferences("overlay_apps", Context.MODE_PRIVATE)
        sharedPreferences.edit().putStringSet("apps", selectedApps).apply()
    }

    fun getOverlayApps(): Set<String> {
        val sharedPreferences =
            context.getSharedPreferences("overlay_apps", Context.MODE_PRIVATE)
        return sharedPreferences.getStringSet("apps", emptySet()) ?: emptySet()
    }

    fun loadGrayScaleApps(): Set<String> {
        val sharedPreferences =
            context.getSharedPreferences("grayscale", Context.MODE_PRIVATE)
        return sharedPreferences.getStringSet("apps", emptySet()) ?: emptySet()
    }

    fun saveGrayScaleApps(apps: Set<String>) {
        val sharedPreferences =
            context.getSharedPreferences("grayscale", Context.MODE_PRIVATE)
        sharedPreferences.edit().putStringSet("apps", apps).apply()
    }

    // Auto-block categories for newly installed apps
    fun getAutoBlockCategories(): Set<String> {
        val sharedPreferences = context.getSharedPreferences("app_blocker", Context.MODE_PRIVATE)
        return sharedPreferences.getStringSet("auto_block_categories", emptySet()) ?: emptySet()
    }

    fun setAutoBlockCategories(categories: Set<String>) {
        val sharedPreferences = context.getSharedPreferences("app_blocker", Context.MODE_PRIVATE)
        sharedPreferences.edit().putStringSet("auto_block_categories", categories).apply()
    }

    fun isAutoBlockEnabled(): Boolean {
        val sharedPreferences = context.getSharedPreferences("app_blocker", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("auto_block_enabled", false)
    }

    fun setAutoBlockEnabled(enabled: Boolean) {
        val sharedPreferences = context.getSharedPreferences("app_blocker", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("auto_block_enabled", enabled).apply()
    }

    fun loadAppBlockLists(): MutableMap<String, MutableSet<String>> {
        val sharedPreferences = context.getSharedPreferences("app_blocker", Context.MODE_PRIVATE)
        val json = sharedPreferences.getString("named_block_lists", null)
        if (json.isNullOrEmpty()) return mutableMapOf()

        val type = object : TypeToken<MutableMap<String, MutableSet<String>>>() {}.type
        return runCatching {
            Gson().fromJson<MutableMap<String, MutableSet<String>>>(json, type) ?: mutableMapOf()
        }.getOrElse {
            Log.e("SavedPreferencesLoader", "Failed to load named block lists", it)
            mutableMapOf()
        }
    }

    fun saveAppBlockLists(lists: MutableMap<String, MutableSet<String>>) {
        val sharedPreferences = context.getSharedPreferences("app_blocker", Context.MODE_PRIVATE)
        val json = Gson().toJson(lists)
        sharedPreferences.edit().putString("named_block_lists", json).apply()

        // Keep legacy blocked_apps in sync as a union for compatibility.
        val merged = lists.values.flatten().toSet()
        saveBlockedApps(merged)
    }

    fun addPackageToBlockList(listName: String, packageName: String) {
        val lists = loadAppBlockLists()
        val selectedList = lists.getOrPut(listName) { mutableSetOf() }
        selectedList.add(packageName)
        saveAppBlockLists(lists)
    }

    fun loadAppBlockerScheduleRules(): MutableList<AppBlockScheduleRule> {
        val sharedPreferences = context.getSharedPreferences("app_blocker", Context.MODE_PRIVATE)
        val json = sharedPreferences.getString("schedule_rules", null)
        if (json.isNullOrEmpty()) return mutableListOf()

        val type = object : TypeToken<MutableList<AppBlockScheduleRule>>() {}.type
        return runCatching {
            Gson().fromJson<MutableList<AppBlockScheduleRule>>(json, type) ?: mutableListOf()
        }.getOrElse {
            Log.e("SavedPreferencesLoader", "Failed to load app blocker schedule rules", it)
            mutableListOf()
        }
    }

    fun saveAppBlockerScheduleRules(rules: MutableList<AppBlockScheduleRule>) {
        val sharedPreferences = context.getSharedPreferences("app_blocker", Context.MODE_PRIVATE)
        val json = Gson().toJson(rules)
        sharedPreferences.edit().putString("schedule_rules", json).apply()
    }

    @Synchronized
    fun upsertAppBlockerScheduleRule(rule: AppBlockScheduleRule) {
        val rules = loadAppBlockerScheduleRules()
        val existing = rules.indexOfFirst { it.id == rule.id }
        if (existing >= 0) {
            rules[existing] = rule
        } else {
            rules.add(rule)
        }
        saveAppBlockerScheduleRules(rules)
    }

    @Synchronized
    fun removeAppBlockerScheduleRule(ruleId: String) {
        val rules = loadAppBlockerScheduleRules()
        rules.removeAll { it.id == ruleId }
        saveAppBlockerScheduleRules(rules)
    }

    @Synchronized
    fun removeAppBlockerScheduleGroup(groupId: String): Int {
        val rules = loadAppBlockerScheduleRules()
        val before = rules.size
        rules.removeAll { it.groupId == groupId }
        if (rules.size != before) {
            saveAppBlockerScheduleRules(rules)
        }
        return before - rules.size
    }

    fun loadUnifiedFeatureScheduleRules(): MutableList<UnifiedFeatureScheduleRule> {
        val sharedPreferences = context.getSharedPreferences("app_blocker", Context.MODE_PRIVATE)
        val json = sharedPreferences.getString("unified_feature_schedule_rules", null)
        if (json.isNullOrEmpty()) return mutableListOf()

        val type = object : TypeToken<MutableList<UnifiedFeatureScheduleRule>>() {}.type
        return runCatching {
            Gson().fromJson<MutableList<UnifiedFeatureScheduleRule>>(json, type) ?: mutableListOf()
        }.getOrElse {
            Log.e("SavedPreferencesLoader", "Failed to load unified feature schedule rules", it)
            mutableListOf()
        }
    }

    fun saveUnifiedFeatureScheduleRules(rules: MutableList<UnifiedFeatureScheduleRule>) {
        val sharedPreferences = context.getSharedPreferences("app_blocker", Context.MODE_PRIVATE)
        val json = Gson().toJson(rules)
        sharedPreferences.edit().putString("unified_feature_schedule_rules", json).apply()
    }

    @Synchronized
    fun upsertUnifiedFeatureScheduleRule(rule: UnifiedFeatureScheduleRule) {
        val rules = loadUnifiedFeatureScheduleRules()
        val existing = rules.indexOfFirst { it.id == rule.id }
        if (existing >= 0) {
            rules[existing] = rule
        } else {
            rules.add(rule)
        }
        saveUnifiedFeatureScheduleRules(rules)
    }

    @Synchronized
    fun removeUnifiedFeatureScheduleRule(ruleId: String) {
        val rules = loadUnifiedFeatureScheduleRules()
        rules.removeAll { it.id == ruleId }
        saveUnifiedFeatureScheduleRules(rules)
    }

    @Synchronized
    fun removeUnifiedFeatureScheduleGroup(groupId: String): Int {
        val rules = loadUnifiedFeatureScheduleRules()
        val before = rules.size
        rules.removeAll { it.groupId == groupId }
        if (rules.size != before) {
            saveUnifiedFeatureScheduleRules(rules)
        }
        return before - rules.size
    }

    private fun getPremiumPrefs(): android.content.SharedPreferences {
        return context.getSharedPreferences("premium_state", Context.MODE_PRIVATE)
    }

    fun isPremiumUser(): Boolean {
        return getPremiumPrefs().getBoolean("is_premium", false)
    }

    fun setPremiumUser(enabled: Boolean) {
        getPremiumPrefs().edit().putBoolean("is_premium", enabled).apply()
    }

    fun getLicenseKey(): String? {
        return getPremiumPrefs().getString("license_key", null)
    }

    fun getLicenseEmail(): String? {
        return getPremiumPrefs().getString("license_email", null)
    }

    fun saveLicenseKey(email: String, licenseKey: String) {
        getPremiumPrefs().edit()
            .putString("license_email", email)
            .putString("license_key", licenseKey)
            .apply()
    }

    fun clearLicenseKey() {
        getPremiumPrefs().edit()
            .remove("license_email")
            .remove("license_key")
            .apply()
    }

    fun getLastPremiumReminder(): Long {
        return getPremiumPrefs().getLong("last_premium_reminder", 0L)
    }

    fun setLastPremiumReminder(timestamp: Long) {
        getPremiumPrefs().edit().putLong("last_premium_reminder", timestamp).apply()
    }

    fun getSpecialAccessId(): String {
        return getPremiumPrefs().getString("special_access_id", "") ?: ""
    }

    fun setSpecialAccessId(accessId: String) {
        getPremiumPrefs().edit().putString("special_access_id", accessId).apply()
    }

    private fun getCompassionateAccessPrefs(): android.content.SharedPreferences {
        return context.getSharedPreferences("compassionate_access", Context.MODE_PRIVATE)
    }

    fun saveCompassionateAccessGrant(
        appId: String,
        userName: String,
        email: String?,
        grantedAt: Long,
        expiresAt: Long
    ) {
        getCompassionateAccessPrefs().edit()
            .putString("app_id", appId)
            .putString("user_name", userName)
            .putString("email", email.orEmpty())
            .putLong("granted_at", grantedAt)
            .putLong("expires_at", expiresAt)
            .apply()
    }

    fun getCompassionateAccessAppId(): String {
        return getCompassionateAccessPrefs().getString("app_id", "") ?: ""
    }

    fun getCompassionateAccessExpiry(): Long {
        return getCompassionateAccessPrefs().getLong("expires_at", 0L)
    }

    fun clearCompassionateAccessGrant() {
        getCompassionateAccessPrefs().edit().clear().apply()
    }

    private fun getHomePrefs(): android.content.SharedPreferences {
        return context.getSharedPreferences("home_dashboard", Context.MODE_PRIVATE)
    }

    fun isHomeWelcomeCardVisible(): Boolean {
        return getHomePrefs().getBoolean("show_welcome_card", true)
    }

    fun setHomeWelcomeCardVisible(visible: Boolean) {
        getHomePrefs().edit().putBoolean("show_welcome_card", visible).apply()
    }

    private fun getFeatureTogglesPrefs(): android.content.SharedPreferences {
        return context.getSharedPreferences("feature_toggles", Context.MODE_PRIVATE)
    }

    fun isAppBlockerFeatureEnabled(default: Boolean = true): Boolean {
        return getFeatureTogglesPrefs().getBoolean("app_blocker_enabled", default)
    }

    fun setAppBlockerFeatureEnabled(enabled: Boolean) {
        getFeatureTogglesPrefs().edit().putBoolean("app_blocker_enabled", enabled).apply()
    }

    fun isKeywordBlockerFeatureEnabled(default: Boolean = true): Boolean {
        return getFeatureTogglesPrefs().getBoolean("keyword_blocker_enabled", default)
    }

    fun setKeywordBlockerFeatureEnabled(enabled: Boolean) {
        getFeatureTogglesPrefs().edit().putBoolean("keyword_blocker_enabled", enabled).apply()
    }

    fun isUsageTrackerFeatureEnabled(default: Boolean = true): Boolean {
        return getFeatureTogglesPrefs().getBoolean("usage_tracker_enabled", default)
    }

    fun setUsageTrackerFeatureEnabled(enabled: Boolean) {
        getFeatureTogglesPrefs().edit().putBoolean("usage_tracker_enabled", enabled).apply()
    }

    fun isFocusModeFeatureEnabled(default: Boolean = true): Boolean {
        return getFeatureTogglesPrefs().getBoolean("focus_mode_enabled", default)
    }

    fun setFocusModeFeatureEnabled(enabled: Boolean) {
        getFeatureTogglesPrefs().edit().putBoolean("focus_mode_enabled", enabled).apply()
    }

    // ==================== App Launch Limit Rules ====================

    fun loadAppLaunchLimitRules(): List<com.alhaq.amnshield.data.blockers.AppLaunchLimitRule> {
        val sharedPreferences =
            context.getSharedPreferences("app_blocker", Context.MODE_PRIVATE)
        val json = sharedPreferences.getString("launch_limit_rules", null)
        if (json.isNullOrEmpty()) return emptyList()

        return try {
            val type = object : TypeToken<List<com.alhaq.amnshield.data.blockers.AppLaunchLimitRule>>() {}.type
            Gson().fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e("SavedPreferencesLoader", "Error loading launch limit rules", e)
            emptyList()
        }
    }

    fun saveAppLaunchLimitRules(rules: List<com.alhaq.amnshield.data.blockers.AppLaunchLimitRule>) {
        val sharedPreferences =
            context.getSharedPreferences("app_blocker", Context.MODE_PRIVATE)
        val json = Gson().toJson(rules)
        sharedPreferences.edit().putString("launch_limit_rules", json).apply()
    }

    @Synchronized
    fun addAppLaunchLimitRule(rule: com.alhaq.amnshield.data.blockers.AppLaunchLimitRule) {
        val rules = loadAppLaunchLimitRules().toMutableList()
        rules.removeAll { it.packageName == rule.packageName }
        rules.add(rule)
        saveAppLaunchLimitRules(rules)
    }

    @Synchronized
    fun removeAppLaunchLimitRule(packageName: String) {
        val rules = loadAppLaunchLimitRules().toMutableList()
        rules.removeAll { it.packageName == packageName }
        saveAppLaunchLimitRules(rules)
    }

    fun getAppLaunchLimitRule(packageName: String): com.alhaq.amnshield.data.blockers.AppLaunchLimitRule? {
        return loadAppLaunchLimitRules().firstOrNull { it.packageName == packageName }
    }

    // ==================== Launch Count Tracking (Per Period) ====================
    // Tracks current launch counts with period information to reset when period changes

    fun trackAppLaunch(packageName: String) {
        val rule = getAppLaunchLimitRule(packageName) ?: return
        val now = System.currentTimeMillis()
        val launchData = loadLaunchCountMap().toMutableMap()
        val current = launchData[packageName]

        launchData[packageName] = if (current == null || isLaunchDataExpired(current, rule, now)) {
            LaunchCountData(count = 1, firstLaunchTime = now, period = rule.timePeriod.name)
        } else {
            current.copy(count = current.count + 1, period = rule.timePeriod.name)
        }

        saveLaunchCountMap(launchData)
    }

    fun getCurrentLaunchCount(
        packageName: String,
        rule: com.alhaq.amnshield.data.blockers.AppLaunchLimitRule? = getAppLaunchLimitRule(packageName)
    ): Int {
        if (rule == null) {
            resetLaunchCount(packageName)
            return 0
        }

        val launchData = loadLaunchCountMap()
        val current = launchData[packageName] ?: return 0

        if (isLaunchDataExpired(current, rule, System.currentTimeMillis())) {
            resetLaunchCount(packageName)
            return 0
        }

        return current.count
    }

    fun resetLaunchCount(packageName: String) {
        val launchData = loadLaunchCountMap().toMutableMap()
        if (launchData.remove(packageName) != null) {
            saveLaunchCountMap(launchData)
        }
    }

    private fun loadLaunchCountMap(): Map<String, LaunchCountData> {
        val sharedPreferences = context.getSharedPreferences("app_launch_tracking", Context.MODE_PRIVATE)
        val json = sharedPreferences.getString("launch_counts", null)
        if (json.isNullOrEmpty()) return emptyMap()

        return try {
            val type = object : TypeToken<Map<String, LaunchCountData>>() {}.type
            Gson().fromJson<Map<String, LaunchCountData>>(json, type) ?: emptyMap()
        } catch (e: Exception) {
            Log.e("SavedPreferencesLoader", "Error loading launch data", e)
            emptyMap()
        }
    }

    private fun saveLaunchCountMap(data: Map<String, LaunchCountData>) {
        val sharedPreferences = context.getSharedPreferences("app_launch_tracking", Context.MODE_PRIVATE)
        val newJson = Gson().toJson(data)
        sharedPreferences.edit().putString("launch_counts", newJson).apply()
    }

    private fun isLaunchDataExpired(
        launchData: LaunchCountData,
        rule: com.alhaq.amnshield.data.blockers.AppLaunchLimitRule,
        nowMillis: Long
    ): Boolean {
        if (launchData.period != rule.timePeriod.name) return true

        return when (rule.timePeriod) {
            com.alhaq.amnshield.data.blockers.AppLaunchLimitRule.TimePeriod.HOURLY -> {
                nowMillis - launchData.firstLaunchTime >= 60L * 60L * 1000L
            }

            com.alhaq.amnshield.data.blockers.AppLaunchLimitRule.TimePeriod.DAILY -> {
                val first = Calendar.getInstance().apply { timeInMillis = launchData.firstLaunchTime }
                val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
                first.get(Calendar.YEAR) != now.get(Calendar.YEAR) ||
                    first.get(Calendar.DAY_OF_YEAR) != now.get(Calendar.DAY_OF_YEAR)
            }

            com.alhaq.amnshield.data.blockers.AppLaunchLimitRule.TimePeriod.WEEKLY -> {
                launchData.firstLaunchTime < getWeeklyWindowStartMillis(nowMillis, rule.dayOfWeek)
            }
        }
    }

    private fun getWeeklyWindowStartMillis(nowMillis: Long, weekStartDay: Int): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        while (calendar.get(Calendar.DAY_OF_WEEK) != weekStartDay) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        return calendar.timeInMillis
    }

    fun isKeywordBlockerAdultPackEnabled(): Boolean {
        val sharedPreferences = context.getSharedPreferences("keyword_blocker_packs", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("adult_blocker", false)
    }

    fun setKeywordBlockerAdultPackEnabled(enabled: Boolean) {
        val sharedPreferences = context.getSharedPreferences("keyword_blocker_packs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("adult_blocker", enabled).apply()
    }

    fun isSocialMediaBlockerEnabled(defaultValue: Boolean = false): Boolean {
        val sharedPreferences = context.getSharedPreferences("social_media_blocker", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("is_enabled", defaultValue)
    }

    fun setSocialMediaBlockerEnabled(enabled: Boolean) {
        val sharedPreferences = context.getSharedPreferences("social_media_blocker", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("is_enabled", enabled).apply()
    }

    fun loadBlockedSocialApps(): Set<String> {
        val sharedPreferences = context.getSharedPreferences("social_media_blocker", Context.MODE_PRIVATE)
        val hasKey = sharedPreferences.contains("blocked_apps")
        if (!hasKey) {
            val defaults = setOf("com.instagram.android", "com.sec.android.app.sbrowser")
            sharedPreferences.edit().putStringSet("blocked_apps", defaults).apply()
            return defaults
        }
        return sharedPreferences.getStringSet("blocked_apps", emptySet()) ?: emptySet()
    }

    fun saveBlockedSocialApps(apps: Set<String>) {
        val sharedPreferences = context.getSharedPreferences("social_media_blocker", Context.MODE_PRIVATE)
        sharedPreferences.edit().putStringSet("blocked_apps", apps).apply()
    }

    fun loadBlockedSocialWebsites(): Set<String> {
        val sharedPreferences = context.getSharedPreferences("social_media_blocker", Context.MODE_PRIVATE)
        val hasKey = sharedPreferences.contains("blocked_websites")
        if (!hasKey) {
            val defaults = setOf("facebook.com", "fb.com", "fb.watch")
            sharedPreferences.edit().putStringSet("blocked_websites", defaults).apply()
            return defaults
        }
        return sharedPreferences.getStringSet("blocked_websites", emptySet()) ?: emptySet()
    }

    fun saveBlockedSocialWebsites(websites: Set<String>) {
        val sharedPreferences = context.getSharedPreferences("social_media_blocker", Context.MODE_PRIVATE)
        sharedPreferences.edit().putStringSet("blocked_websites", websites).apply()
    }

    /**
     * Data class to track launch count with timestamp for period-based reset logic
     */
    data class LaunchCountData(
        val count: Int,
        val firstLaunchTime: Long,
        val period: String // "HOURLY", "DAILY", "WEEKLY"
    )

    private fun checkAndResetReelsStatsDaily(sharedPreferences: SharedPreferences) {
        val today = com.alhaq.amnshield.utils.TimeTools.getCurrentDate()
        val savedDate = sharedPreferences.getString("reels_stats_date", "")
        if (savedDate != today) {
            val editor = sharedPreferences.edit()
            editor.putString("reels_stats_date", today)
            editor.putInt("reels_scrolled_today", 0)
            editor.putLong("reels_watch_time_seconds_today", 0L)
            editor.apply()
        }
    }

    fun getReelsScrolledToday(): Int {
        val sharedPreferences = context.getSharedPreferences("reel_blocker", Context.MODE_PRIVATE)
        checkAndResetReelsStatsDaily(sharedPreferences)
        return sharedPreferences.getInt("reels_scrolled_today", 0)
    }

    fun incrementReelsScrolled() {
        val sharedPreferences = context.getSharedPreferences("reel_blocker", Context.MODE_PRIVATE)
        checkAndResetReelsStatsDaily(sharedPreferences)
        val current = sharedPreferences.getInt("reels_scrolled_today", 0)
        sharedPreferences.edit().putInt("reels_scrolled_today", current + 1).apply()
    }

    fun getReelsWatchTimeSeconds(): Long {
        val sharedPreferences = context.getSharedPreferences("reel_blocker", Context.MODE_PRIVATE)
        checkAndResetReelsStatsDaily(sharedPreferences)
        return sharedPreferences.getLong("reels_watch_time_seconds_today", 0L)
    }

    fun addReelsWatchTime(seconds: Long) {
        if (seconds <= 0) return
        val sharedPreferences = context.getSharedPreferences("reel_blocker", Context.MODE_PRIVATE)
        checkAndResetReelsStatsDaily(sharedPreferences)
        val current = sharedPreferences.getLong("reels_watch_time_seconds_today", 0L)
        sharedPreferences.edit().putLong("reels_watch_time_seconds_today", current + seconds).apply()
    }
}
