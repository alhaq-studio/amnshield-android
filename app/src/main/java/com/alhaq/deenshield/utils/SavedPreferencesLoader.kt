package com.alhaq.deenshield.utils

import android.content.Context
import android.util.Log
import com.alhaq.deenshield.blockers.ReelBlocker
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.alhaq.deenshield.blockers.FocusModeBlocker
import com.alhaq.deenshield.data.AttentionSpanVideoItem
import com.alhaq.deenshield.ui.activity.MainActivity
import com.alhaq.deenshield.ui.activity.TimedActionActivity

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
        sharedPreferences.edit().putStringSet("pinned_apps", pinnedApps).commit()
    }


    fun saveBlockedApps(pinnedApps: Set<String>) {
        val sharedPreferences =
            context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().putStringSet("blocked_apps", pinnedApps).commit()
    }


    fun saveIgnoredAppUsageTracker(ignoredApps: Set<String>) {
        val sharedPreferences =
            context.getSharedPreferences("app_usage_tracker", Context.MODE_PRIVATE)
        sharedPreferences.edit().putStringSet("ignored_apps", ignoredApps).commit()
    }


    fun saveBlockedKeywords(pinnedApps: Set<String>) {
        val sharedPreferences =
            context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().putStringSet("blocked_keywords", pinnedApps).commit()
    }
    fun saveAppBlockerCheatHoursList(cheatHoursList: MutableList<TimedActionActivity.AutoTimedActionItem>) {
        val sharedPreferences = context.getSharedPreferences("cheat_hours", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()

        val json = gson.toJson(cheatHoursList)

        editor.putString("cheatHoursList", json)
        editor.commit()
    }

    fun loadAppBlockerCheatHoursList(): MutableList<TimedActionActivity.AutoTimedActionItem> {
        val sharedPreferences = context.getSharedPreferences("cheat_hours", Context.MODE_PRIVATE)
        val gson = Gson()

        val json = sharedPreferences.getString("cheatHoursList", null)

        if (json.isNullOrEmpty()) return mutableListOf()

        val type =
            object : TypeToken<MutableList<TimedActionActivity.AutoTimedActionItem>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveAutoFocusHoursList(cheatHoursList: MutableList<TimedActionActivity.AutoTimedActionItem>) {
        val sharedPreferences =
            context.getSharedPreferences("auto_focus_hours", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()

        val json = gson.toJson(cheatHoursList)

        editor.putString("auto_focus_list", json)
        editor.commit()
    }

    fun loadAutoFocusHoursList(): MutableList<TimedActionActivity.AutoTimedActionItem> {
        val sharedPreferences =
            context.getSharedPreferences("auto_focus_hours", Context.MODE_PRIVATE)
        val gson = Gson()

        val json = sharedPreferences.getString("auto_focus_list", null)

        if (json.isNullOrEmpty()) return mutableListOf()

        val type =
            object : TypeToken<MutableList<TimedActionActivity.AutoTimedActionItem>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveAppBlockerWarningInfo(warningData: MainActivity.WarningData) {
        val sharedPreferences = context.getSharedPreferences("warning_data", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()

        val json = gson.toJson(warningData)

        editor.putString("app_blocker", json)
        editor.commit()
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
        editor.commit()
    }

    fun saveCheatHoursForViewBlocker(startTime: Int, endTime: Int) {
        val sharedPreferences = context.getSharedPreferences("cheat_hours", Context.MODE_PRIVATE)
        val edit = sharedPreferences.edit()
        edit.putInt("view_blocker_start_time", startTime)
        edit.putInt("view_blocker_end_time", endTime)
        edit.commit()
    }

    fun loadViewBlockerWarningInfo(): MainActivity.WarningData {
        val sharedPreferences = context.getSharedPreferences("warning_data", Context.MODE_PRIVATE)
        val gson = Gson()

        val json = sharedPreferences.getString("view_blocker", null)

        if (json.isNullOrEmpty()) return MainActivity.WarningData()

        val type = object : TypeToken<MainActivity.WarningData>() {}.type
        return gson.fromJson(json, type)
    }


    fun saveUsageHoursAttentionSpanData(attentionSpanListData: MutableMap<String, MutableList<AttentionSpanVideoItem>>) {
        val sharedPreferences =
            context.getSharedPreferences("attention_span_data", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()

        val json = gson.toJson(attentionSpanListData)

        editor.putString("attention_data", json)
        editor.commit()
    }

    fun loadUsageHoursAttentionSpanData(): MutableMap<String, MutableList<AttentionSpanVideoItem>> {
        val sharedPreferences =
            context.getSharedPreferences("attention_span_data", Context.MODE_PRIVATE)
        val gson = Gson()

        val json = sharedPreferences.getString("attention_data", null)

        if (json.isNullOrEmpty()) return mutableMapOf()

        val type =
            object : TypeToken<MutableMap<String, MutableList<AttentionSpanVideoItem>>>() {}.type
        return runCatching {
            gson.fromJson<MutableMap<String, MutableList<AttentionSpanVideoItem>>>(json, type)
                ?: mutableMapOf()
        }.getOrElse {
            Log.e("SavedPreferencesLoader", "Failed to load attention span data", it)
            mutableMapOf()
        }
    }

    fun saveReelsScrolled(reelsData: MutableMap<String, Int>) {
        val sharedPreferences =
            context.getSharedPreferences("attention_span_data", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()

        val json = gson.toJson(reelsData)

        editor.putString("reels_data", json)
        editor.commit()
    }

    fun getReelsScrolled(): MutableMap<String, Int> {
        val sharedPreferences =
            context.getSharedPreferences("attention_span_data", Context.MODE_PRIVATE)
        val gson = Gson()

        val json = sharedPreferences.getString("reels_data", null)

        if (json.isNullOrEmpty()) return mutableMapOf()

        val type =
            object : TypeToken<MutableMap<String, Int>>() {}.type
        return runCatching {
            gson.fromJson<MutableMap<String, Int>>(json, type) ?: mutableMapOf()
        }.getOrElse {
            Log.e("SavedPreferencesLoader", "Failed to load reels scrolled data", it)
            mutableMapOf()
        }
    }

    fun saveFocusModeData(focusModeData: FocusModeBlocker.FocusModeData) {
        val sharedPreferences =
            context.getSharedPreferences("focus_mode", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()

        val json = gson.toJson(focusModeData)

        editor.putString("focus_mode", json)
        editor.commit()
    }


    fun getFocusModeData(): FocusModeBlocker.FocusModeData {

        val sharedPreferences =
            context.getSharedPreferences("focus_mode", Context.MODE_PRIVATE)
        val gson = Gson()

        val json = sharedPreferences.getString("focus_mode", null)

        if (json.isNullOrEmpty()) return FocusModeBlocker.FocusModeData()

        val type =
            object : TypeToken<FocusModeBlocker.FocusModeData>() {}.type
        return gson.fromJson(json, type)
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
        editor.commit()
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
        editor.commit()
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
        sharedPreferences.edit().putStringSet("apps", selectedApps).commit()
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
        sharedPreferences.edit().putStringSet("apps", apps).commit()
    }

    // Auto-block categories for newly installed apps
    fun getAutoBlockCategories(): Set<String> {
        val sharedPreferences = context.getSharedPreferences("app_blocker", Context.MODE_PRIVATE)
        return sharedPreferences.getStringSet("auto_block_categories", emptySet()) ?: emptySet()
    }

    fun setAutoBlockCategories(categories: Set<String>) {
        val sharedPreferences = context.getSharedPreferences("app_blocker", Context.MODE_PRIVATE)
        sharedPreferences.edit().putStringSet("auto_block_categories", categories).commit()
    }

    fun isAutoBlockEnabled(): Boolean {
        val sharedPreferences = context.getSharedPreferences("app_blocker", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("auto_block_enabled", false)
    }

    fun setAutoBlockEnabled(enabled: Boolean) {
        val sharedPreferences = context.getSharedPreferences("app_blocker", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("auto_block_enabled", enabled).commit()
    }

    private fun getPremiumPrefs(): android.content.SharedPreferences {
        return context.getSharedPreferences("premium_state", Context.MODE_PRIVATE)
    }

    fun isPremiumUser(): Boolean {
        return getPremiumPrefs().getBoolean("is_premium", false)
    }

    fun setPremiumUser(enabled: Boolean) {
        getPremiumPrefs().edit().putBoolean("is_premium", enabled).commit()
    }

    fun getLastPremiumReminder(): Long {
        return getPremiumPrefs().getLong("last_premium_reminder", 0L)
    }

    fun setLastPremiumReminder(timestamp: Long) {
        getPremiumPrefs().edit().putLong("last_premium_reminder", timestamp).commit()
    }

    fun getSpecialAccessId(): String {
        return getPremiumPrefs().getString("special_access_id", "") ?: ""
    }

    fun setSpecialAccessId(accessId: String) {
        getPremiumPrefs().edit().putString("special_access_id", accessId).commit()
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
            .commit()
    }

    fun getCompassionateAccessAppId(): String {
        return getCompassionateAccessPrefs().getString("app_id", "") ?: ""
    }

    fun getCompassionateAccessExpiry(): Long {
        return getCompassionateAccessPrefs().getLong("expires_at", 0L)
    }

    fun clearCompassionateAccessGrant() {
        getCompassionateAccessPrefs().edit().clear().commit()
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

}
