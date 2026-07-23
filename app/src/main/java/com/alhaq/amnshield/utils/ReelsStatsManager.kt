package com.alhaq.amnshield.utils

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Manager for tracking, storing, and aggregating Reels & Shorts metrics:
 * - Scrolled counts per platform and per date
 * - Total watch duration per platform and per date
 * - Historical weekly/monthly trend metrics
 */
class ReelsStatsManager private constructor(private val context: Context) {

    private val prefs: SharedPreferences = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    data class PlatformReelsStat(
        val platformKey: String,
        val displayName: String,
        val scrolledCount: Int = 0,
        val watchTimeSeconds: Long = 0L
    )

    data class DailyReelsRecord(
        val dateStr: String, // YYYY-MM-DD
        val dayLabel: String, // Mon, Tue, etc.
        val totalScrolled: Int = 0,
        val totalWatchTimeSeconds: Long = 0L,
        val platformStats: Map<String, PlatformReelsStat> = emptyMap()
    )

    data class ReelsMetricsSummary(
        val totalScrolledToday: Int = 0,
        val totalWatchTimeTodaySeconds: Long = 0L,
        val avgWatchTimePerReelSeconds: Int = 0,
        val peakScrollDayLabel: String = "N/A",
        val peakScrollCount: Int = 0,
        val topPlatformName: String = "Instagram Reels",
        val dailyRecords: List<DailyReelsRecord> = emptyList(),
        val platformBreakdownToday: List<PlatformReelsStat> = emptyList()
    )

    /**
     * Map package name to standard platform key & display name
     */
    fun resolvePlatform(packageName: String?): Pair<String, String> {
        val pkg = packageName?.lowercase(Locale.ROOT) ?: ""
        return when {
            pkg.contains("instagram") -> "instagram" to "Instagram Reels"
            pkg.contains("youtube") -> "youtube" to "YouTube Shorts"
            pkg.contains("tiktok") || pkg.contains("musically") -> "tiktok" to "TikTok"
            pkg.contains("facebook") -> "facebook" to "Facebook Reels"
            pkg.contains("snapchat") -> "snapchat" to "Snapchat Spotlight"
            pkg.contains("chrome") || pkg.contains("firefox") || pkg.contains("browser") || pkg.contains("opera") -> "browser" to "Web Short Videos"
            else -> "other" to "Short Video Apps"
        }
    }

    /**
     * Record a scroll event for a given app package
     */
    @Synchronized
    fun recordReelScroll(packageName: String?) {
        val today = LocalDate.now()
        val dateStr = today.toString()
        val (platformKey, displayName) = resolvePlatform(packageName)

        val record = loadDailyRecord(dateStr)
        val currentScrolled = record.totalScrolled + 1
        val platformMap = record.platformStats.toMutableMap()
        val currentPlatform = platformMap[platformKey] ?: PlatformReelsStat(platformKey, displayName)
        platformMap[platformKey] = currentPlatform.copy(
            scrolledCount = currentPlatform.scrolledCount + 1
        )

        val updatedRecord = record.copy(
            totalScrolled = currentScrolled,
            platformStats = platformMap
        )
        saveDailyRecord(updatedRecord)

        // Also update SavedPreferencesLoader legacy keys for backward compatibility
        val legacyPrefs = context.getSharedPreferences("reel_blocker", Context.MODE_PRIVATE)
        legacyPrefs.edit()
            .putString("reels_stats_date", dateStr)
            .putInt("reels_scrolled_today", currentScrolled)
            .apply()
    }

    /**
     * Record watch duration seconds for a given app package
     */
    @Synchronized
    fun recordReelWatchTime(packageName: String?, seconds: Long) {
        if (seconds <= 0) return
        val today = LocalDate.now()
        val dateStr = today.toString()
        val (platformKey, displayName) = resolvePlatform(packageName)

        val record = loadDailyRecord(dateStr)
        val updatedWatchTime = record.totalWatchTimeSeconds + seconds
        val platformMap = record.platformStats.toMutableMap()
        val currentPlatform = platformMap[platformKey] ?: PlatformReelsStat(platformKey, displayName)
        platformMap[platformKey] = currentPlatform.copy(
            watchTimeSeconds = currentPlatform.watchTimeSeconds + seconds
        )

        val updatedRecord = record.copy(
            totalWatchTimeSeconds = updatedWatchTime,
            platformStats = platformMap
        )
        saveDailyRecord(updatedRecord)

        // Legacy compat
        val legacyPrefs = context.getSharedPreferences("reel_blocker", Context.MODE_PRIVATE)
        legacyPrefs.edit()
            .putString("reels_stats_date", dateStr)
            .putLong("reels_watch_time_seconds_today", updatedWatchTime)
            .apply()
    }

    /**
     * Get record for a specific LocalDate
     */
    fun loadDailyRecord(dateStr: String): DailyReelsRecord {
        val jsonStr = prefs.getString(KEY_PREFIX_DATE + dateStr, null)
        val dayLabel = try {
            val date = LocalDate.parse(dateStr)
            date.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault()))
        } catch (e: Exception) {
            "Day"
        }

        if (jsonStr.isNullOrEmpty()) {
            return DailyReelsRecord(dateStr = dateStr, dayLabel = dayLabel)
        }

        return try {
            val obj = JSONObject(jsonStr)
            val totalScrolled = obj.optInt("totalScrolled", 0)
            val totalWatchTime = obj.optLong("totalWatchTimeSeconds", 0L)

            val platformMap = mutableMapOf<String, PlatformReelsStat>()
            val platformArray = obj.optJSONArray("platformStats")
            if (platformArray != null) {
                for (i in 0 until platformArray.length()) {
                    val pObj = platformArray.getJSONObject(i)
                    val pKey = pObj.getString("platformKey")
                    val pName = pObj.optString("displayName", pKey)
                    val scrolled = pObj.optInt("scrolledCount", 0)
                    val watchTime = pObj.optLong("watchTimeSeconds", 0L)
                    platformMap[pKey] = PlatformReelsStat(pKey, pName, scrolled, watchTime)
                }
            }

            DailyReelsRecord(
                dateStr = dateStr,
                dayLabel = dayLabel,
                totalScrolled = totalScrolled,
                totalWatchTimeSeconds = totalWatchTime,
                platformStats = platformMap
            )
        } catch (e: Exception) {
            DailyReelsRecord(dateStr = dateStr, dayLabel = dayLabel)
        }
    }

    private fun saveDailyRecord(record: DailyReelsRecord) {
        try {
            val obj = JSONObject()
            obj.put("dateStr", record.dateStr)
            obj.put("dayLabel", record.dayLabel)
            obj.put("totalScrolled", record.totalScrolled)
            obj.put("totalWatchTimeSeconds", record.totalWatchTimeSeconds)

            val platformArray = JSONArray()
            record.platformStats.values.forEach { p ->
                val pObj = JSONObject().apply {
                    put("platformKey", p.platformKey)
                    put("displayName", p.displayName)
                    put("scrolledCount", p.scrolledCount)
                    put("watchTimeSeconds", p.watchTimeSeconds)
                }
                platformArray.put(pObj)
            }
            obj.put("platformStats", platformArray)

            prefs.edit().putString(KEY_PREFIX_DATE + record.dateStr, obj.toString()).apply()
        } catch (e: Exception) {
            android.util.Log.e("ReelsStatsManager", "Error saving daily record", e)
        }
    }

    /**
     * Get records for the last N days (including today)
     */
    fun getRecordsForLastDays(daysCount: Int = 7): List<DailyReelsRecord> {
        val list = mutableListOf<DailyReelsRecord>()
        val today = LocalDate.now()
        for (i in (daysCount - 1) downTo 0) {
            val date = today.minusDays(i.toLong())
            list.add(loadDailyRecord(date.toString()))
        }
        return list
    }

    /**
     * Generate full summary metrics
     */
    fun getFullMetricsSummary(): ReelsMetricsSummary {
        val todayStr = LocalDate.now().toString()
        val todayRecord = loadDailyRecord(todayStr)

        val last7Days = getRecordsForLastDays(7)
        val peakDay = last7Days.maxByOrNull { it.totalScrolled }

        val avgSeconds = if (todayRecord.totalScrolled > 0) {
            (todayRecord.totalWatchTimeSeconds / todayRecord.totalScrolled).toInt()
        } else 0

        // Find top platform today
        val topPlatform = todayRecord.platformStats.values.maxByOrNull { it.scrolledCount }

        // Default platforms list if today's platforms are empty
        val defaultPlatforms = listOf(
            PlatformReelsStat("instagram", "Instagram Reels", todayRecord.platformStats["instagram"]?.scrolledCount ?: 0, todayRecord.platformStats["instagram"]?.watchTimeSeconds ?: 0L),
            PlatformReelsStat("youtube", "YouTube Shorts", todayRecord.platformStats["youtube"]?.scrolledCount ?: 0, todayRecord.platformStats["youtube"]?.watchTimeSeconds ?: 0L),
            PlatformReelsStat("tiktok", "TikTok", todayRecord.platformStats["tiktok"]?.scrolledCount ?: 0, todayRecord.platformStats["tiktok"]?.watchTimeSeconds ?: 0L),
            PlatformReelsStat("facebook", "Facebook Reels", todayRecord.platformStats["facebook"]?.scrolledCount ?: 0, todayRecord.platformStats["facebook"]?.watchTimeSeconds ?: 0L),
            PlatformReelsStat("browser", "Web Short Videos", todayRecord.platformStats["browser"]?.scrolledCount ?: 0, todayRecord.platformStats["browser"]?.watchTimeSeconds ?: 0L)
        )

        return ReelsMetricsSummary(
            totalScrolledToday = todayRecord.totalScrolled,
            totalWatchTimeTodaySeconds = todayRecord.totalWatchTimeSeconds,
            avgWatchTimePerReelSeconds = avgSeconds,
            peakScrollDayLabel = peakDay?.dayLabel ?: "N/A",
            peakScrollCount = peakDay?.totalScrolled ?: 0,
            topPlatformName = topPlatform?.displayName ?: "Instagram Reels",
            dailyRecords = last7Days,
            platformBreakdownToday = defaultPlatforms
        )
    }

    companion object {
        private const val PREFS_NAME = "reels_metrics_stats"
        private const val KEY_PREFIX_DATE = "record_"

        @Volatile
        private var instance: ReelsStatsManager? = null

        fun getInstance(context: Context): ReelsStatsManager {
            return instance ?: synchronized(this) {
                instance ?: ReelsStatsManager(context).also { instance = it }
            }
        }
    }
}
