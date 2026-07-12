package com.alhaq.amnshield.utils

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId

/**
 * Manager class for tracking and persisting blocking statistics.
 * Tracks app blocks, keyword blocks, view/reel blocks, and focus sessions.
 */
class BlockingStatsManager private constructor(private val context: Context) {

    private val prefs: SharedPreferences = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Data classes for different types of block events
    data class BlockEvent(
        val timestamp: Long,
        val type: BlockType,
        val packageName: String? = null,
        val keyword: String? = null,
        val viewId: String? = null,
        val details: String? = null
    )

    data class FocusSession(
        val startTime: Long,
        val endTime: Long,
        val durationMinutes: Long
    )

    enum class BlockType {
        APP_BLOCK,
        KEYWORD_BLOCK,
        VIEW_REEL_BLOCK,
        FOCUS_SESSION
    }

    /**
     * Record an app block event
     */
    fun recordAppBlock(packageName: String, details: String? = null) {
        val event = BlockEvent(
            timestamp = System.currentTimeMillis(),
            type = BlockType.APP_BLOCK,
            packageName = packageName,
            details = details
        )
        saveBlockEvent(event)
        
        // Check for achievements
        checkAndNotifyAchievements()
    }

    /**
     * Record a keyword block event
     */
    fun recordKeywordBlock(keyword: String, packageName: String? = null) {
        val event = BlockEvent(
            timestamp = System.currentTimeMillis(),
            type = BlockType.KEYWORD_BLOCK,
            keyword = keyword,
            packageName = packageName
        )
        saveBlockEvent(event)
        
        // Check for achievements
        checkAndNotifyAchievements()
    }

    /**
     * Record a view/reel block event
     */
    fun recordViewBlock(packageName: String, viewId: String? = null) {
        val event = BlockEvent(
            timestamp = System.currentTimeMillis(),
            type = BlockType.VIEW_REEL_BLOCK,
            packageName = packageName,
            viewId = viewId
        )
        saveBlockEvent(event)
        
        // Check for achievements
        checkAndNotifyAchievements()
    }

    /**
     * Record a focus session
     */
    fun recordFocusSession(startTime: Long, endTime: Long) {
        val durationMinutes = (endTime - startTime) / (60 * 1000)
        val session = FocusSession(startTime, endTime, durationMinutes)
        saveFocusSession(session)
        
        // Check for achievements
        checkAndNotifyAchievements()
    }
    
    /**
     * Check current stats and notify achievements if enabled
     */
    private fun checkAndNotifyAchievements() {
        val reminderPrefs = context.getSharedPreferences("reminder_settings", Context.MODE_PRIVATE)
        val achievementsEnabled = reminderPrefs.getBoolean("achievement_enabled", true)
        
        if (achievementsEnabled) {
            val notificationHelper = NotificationHelper(context)
            notificationHelper.checkAndNotifyAchievements()
        }
    }

    /**
     * Get all block events for a specific date
     */
    fun getBlockEventsForDate(date: LocalDate): List<BlockEvent> {
        val key = getDateKey(date)
        val json = prefs.getString(key, null) ?: return emptyList()
        
        return try {
            val jsonArray = JSONArray(json)
            val events = mutableListOf<BlockEvent>()
            
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                events.add(BlockEvent(
                    timestamp = obj.getLong("timestamp"),
                    type = BlockType.valueOf(obj.getString("type")),
                    packageName = obj.optString("packageName").takeIf { it.isNotEmpty() },
                    keyword = obj.optString("keyword").takeIf { it.isNotEmpty() },
                    viewId = obj.optString("viewId").takeIf { it.isNotEmpty() },
                    details = obj.optString("details").takeIf { it.isNotEmpty() }
                ))
            }
            
            events
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get focus sessions for a specific date
     */
    fun getFocusSessionsForDate(date: LocalDate): List<FocusSession> {
        val key = getFocusSessionKey(date)
        val json = prefs.getString(key, null) ?: return emptyList()
        
        return try {
            val jsonArray = JSONArray(json)
            val sessions = mutableListOf<FocusSession>()
            
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                sessions.add(FocusSession(
                    startTime = obj.getLong("startTime"),
                    endTime = obj.getLong("endTime"),
                    durationMinutes = obj.getLong("durationMinutes")
                ))
            }
            
            sessions
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get statistics summary for a specific date
     */
    fun getStatsSummaryForDate(date: LocalDate): StatsSummary {
        val events = getBlockEventsForDate(date)
        val focusSessions = getFocusSessionsForDate(date)
        
        return StatsSummary(
            appBlocksCount = events.count { it.type == BlockType.APP_BLOCK },
            keywordBlocksCount = events.count { it.type == BlockType.KEYWORD_BLOCK },
            viewBlocksCount = events.count { it.type == BlockType.VIEW_REEL_BLOCK },
            focusSessionsCount = focusSessions.size,
            totalFocusMinutes = focusSessions.sumOf { it.durationMinutes }
        )
    }

    /**
     * Get statistics for today
     */
    fun getTodayStats(): StatsSummary {
        return getStatsSummaryForDate(LocalDate.now())
    }

    /**
     * Clear old data (older than specified days)
     */
    fun clearOldData(daysToKeep: Int = 30) {
        val editor = prefs.edit()
        val cutoffDate = LocalDate.now().minusDays(daysToKeep.toLong())
        
        prefs.all.keys.forEach { key ->
            if (key.startsWith(DATE_PREFIX) || key.startsWith(FOCUS_PREFIX)) {
                try {
                    val dateStr = key.substringAfter("_")
                    val date = LocalDate.parse(dateStr)
                    if (date.isBefore(cutoffDate)) {
                        editor.remove(key)
                    }
                } catch (e: Exception) {
                    // Skip invalid keys
                }
            }
        }
        
        editor.apply()
    }

    // Private helper methods

    private fun saveBlockEvent(event: BlockEvent) {
        val date = LocalDate.ofInstant(
            java.time.Instant.ofEpochMilli(event.timestamp),
            ZoneId.systemDefault()
        )
        val key = getDateKey(date)
        
        val existingEvents = getBlockEventsForDate(date).toMutableList()
        existingEvents.add(event)
        
        val jsonArray = JSONArray()
        existingEvents.forEach { e ->
            val obj = JSONObject().apply {
                put("timestamp", e.timestamp)
                put("type", e.type.name)
                e.packageName?.let { put("packageName", it) }
                e.keyword?.let { put("keyword", it) }
                e.viewId?.let { put("viewId", it) }
                e.details?.let { put("details", it) }
            }
            jsonArray.put(obj)
        }
        
        prefs.edit().putString(key, jsonArray.toString()).apply()
    }

    private fun saveFocusSession(session: FocusSession) {
        val date = LocalDate.ofInstant(
            java.time.Instant.ofEpochMilli(session.startTime),
            ZoneId.systemDefault()
        )
        val key = getFocusSessionKey(date)
        
        val existingSessions = getFocusSessionsForDate(date).toMutableList()
        existingSessions.add(session)
        
        val jsonArray = JSONArray()
        existingSessions.forEach { s ->
            val obj = JSONObject().apply {
                put("startTime", s.startTime)
                put("endTime", s.endTime)
                put("durationMinutes", s.durationMinutes)
            }
            jsonArray.put(obj)
        }
        
        prefs.edit().putString(key, jsonArray.toString()).apply()
    }

    private fun getDateKey(date: LocalDate): String {
        return "${DATE_PREFIX}${date}"
    }

    private fun getFocusSessionKey(date: LocalDate): String {
        return "${FOCUS_PREFIX}${date}"
    }

    data class StatsSummary(
        val appBlocksCount: Int,
        val keywordBlocksCount: Int,
        val viewBlocksCount: Int,
        val focusSessionsCount: Int,
        val totalFocusMinutes: Long
    )

    companion object {
        private const val PREFS_NAME = "blocking_stats"
        private const val DATE_PREFIX = "events_"
        private const val FOCUS_PREFIX = "focus_"

        @Volatile
        private var instance: BlockingStatsManager? = null

        fun getInstance(context: Context): BlockingStatsManager {
            return instance ?: synchronized(this) {
                instance ?: BlockingStatsManager(context).also { instance = it }
            }
        }
    }
}
