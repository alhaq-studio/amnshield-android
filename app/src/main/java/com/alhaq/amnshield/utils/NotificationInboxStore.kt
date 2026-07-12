package com.alhaq.amnshield.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.concurrent.TimeUnit

/**
 * Lightweight on-device store for in-app notifications shown by DeenShield
 * (daily reports, focus reminders, achievements, block alerts, etc.).
 *
 * The toolbar bell icon opens a screen backed by this store so the user sees
 * an actual history of notifications instead of the reminder settings screen.
 *
 * All data lives in SharedPreferences as a JSON list and never leaves the device,
 * matching the rest of the app's privacy-first architecture.
 *
 * Tracking and de-duplication features:
 *  - Items are de-duplicated within a short window so a runaway caller can't
 *    flood the inbox with identical entries (see [DEDUPE_WINDOW_MS]).
 *  - The list is auto-pruned of items older than [RETENTION_DAYS] every time
 *    it is read.
 *  - Per-category aggregate counters (received / read) and last-seen / last-read
 *    timestamps are persisted for lightweight in-app analytics
 *    (see [Stats], [getStats]). These never leave the device.
 *  - Individual items can be marked read on tap or removed on swipe.
 */
object NotificationInboxStore {

    private const val PREFS = "notification_inbox"
    private const val KEY_ITEMS = "items"
    private const val KEY_STATS = "stats"
    private const val MAX_ITEMS = 100

    /** Drop a duplicate add() of the same title+message+category within this window. */
    private val DEDUPE_WINDOW_MS = TimeUnit.SECONDS.toMillis(60)

    /** History older than this is pruned automatically on read. */
    private const val RETENTION_DAYS = 30L
    private val RETENTION_MS = TimeUnit.DAYS.toMillis(RETENTION_DAYS)

    enum class Category {
        DAILY_REPORT,
        REMINDER,
        ACHIEVEMENT,
        BLOCK_ALERT,
        SYSTEM
    }

    data class InboxNotification(
        val id: Long = System.currentTimeMillis(),
        val title: String,
        val message: String,
        val category: Category = Category.SYSTEM,
        val timestamp: Long = System.currentTimeMillis(),
        var read: Boolean = false,
        /** When the user actually opened/tapped this item, if ever. */
        var readAt: Long? = null
    )

    /**
     * Aggregate counters per category. Lifetime totals are kept across prunes
     * so analytics aren't lost when items expire.
     */
    data class CategoryStats(
        var received: Long = 0,
        var read: Long = 0,
        var lastSeen: Long = 0,
        var lastRead: Long = 0,
        /** Sum of (readAt - timestamp) for reads, used to compute avg latency. */
        var totalReadLatencyMs: Long = 0
    ) {
        val avgReadLatencyMs: Long
            get() = if (read > 0) totalReadLatencyMs / read else 0L
    }

    data class Stats(
        val perCategory: Map<Category, CategoryStats>,
        val totalReceived: Long,
        val totalRead: Long
    )

    private val gson = Gson()
    private val listType = object : TypeToken<MutableList<InboxNotification>>() {}.type
    private val statsType =
        object : TypeToken<MutableMap<String, CategoryStats>>() {}.type

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * Add a new inbox item.
     *
     * Returns true if the item was inserted, false if it was suppressed by the
     * deduplication window. Aggregate counters are still updated on suppression
     * so callers know the event happened.
     */
    @Synchronized
    fun add(
        context: Context,
        title: String,
        message: String,
        category: Category
    ): Boolean {
        val list = readListRaw(context).toMutableList()
        val now = System.currentTimeMillis()

        val duplicate = list.firstOrNull {
            it.category == category &&
                it.title == title &&
                it.message == message &&
                (now - it.timestamp) <= DEDUPE_WINDOW_MS
        }

        // Always update aggregate stats.
        updateStats(context) { all ->
            val s = all.getOrPut(category.name) { CategoryStats() }
            s.received += 1
            s.lastSeen = now
        }

        if (duplicate != null) return false

        val newItem = InboxNotification(
            id = now,
            title = title,
            message = message,
            category = category,
            timestamp = now
        )
        list.add(0, newItem)

        if (list.size > MAX_ITEMS) {
            list.subList(MAX_ITEMS, list.size).clear()
        }
        writeList(context, list)
        return true
    }

    fun getAll(context: Context): List<InboxNotification> {
        val list = readListRaw(context).toMutableList()
        // Auto-prune anything past the retention window so the inbox stays
        // relevant without forcing the user to clear it.
        val cutoff = System.currentTimeMillis() - RETENTION_MS
        val before = list.size
        list.removeAll { it.timestamp < cutoff }
        if (list.size != before) writeList(context, list)
        return list
    }

    fun getByCategory(context: Context, category: Category): List<InboxNotification> =
        getAll(context).filter { it.category == category }

    fun unreadCount(context: Context): Int = getAll(context).count { !it.read }

    /**
     * Mark a single item as read and record read latency for analytics.
     * Safe to call repeatedly; only the first transition increments stats.
     */
    @Synchronized
    fun markRead(context: Context, id: Long) {
        val list = readListRaw(context).toMutableList()
        val idx = list.indexOfFirst { it.id == id }
        if (idx < 0) return
        val item = list[idx]
        if (item.read) return
        val now = System.currentTimeMillis()
        item.read = true
        item.readAt = now
        writeList(context, list)
        updateStats(context) { all ->
            val s = all.getOrPut(item.category.name) { CategoryStats() }
            s.read += 1
            s.lastRead = now
            s.totalReadLatencyMs += (now - item.timestamp).coerceAtLeast(0L)
        }
    }

    @Synchronized
    fun markAllRead(context: Context) {
        val list = readListRaw(context).toMutableList()
        if (list.isEmpty()) return
        val now = System.currentTimeMillis()
        var changed = false
        list.forEach { item ->
            if (!item.read) {
                item.read = true
                item.readAt = now
                changed = true
                updateStats(context) { all ->
                    val s = all.getOrPut(item.category.name) { CategoryStats() }
                    s.read += 1
                    s.lastRead = now
                    s.totalReadLatencyMs += (now - item.timestamp).coerceAtLeast(0L)
                }
            }
        }
        if (changed) writeList(context, list)
    }

    @Synchronized
    fun remove(context: Context, id: Long) {
        val list = readListRaw(context).toMutableList()
        if (list.removeAll { it.id == id }) writeList(context, list)
    }

    @Synchronized
    fun clearAll(context: Context) {
        prefs(context).edit().remove(KEY_ITEMS).apply()
    }

    /** Returns lifetime aggregate statistics. Counters survive prune/clear. */
    fun getStats(context: Context): Stats {
        val map = readStatsRaw(context)
        val perCategory = Category.values().associateWith {
            map[it.name] ?: CategoryStats()
        }
        val totalReceived = perCategory.values.sumOf { it.received }
        val totalRead = perCategory.values.sumOf { it.read }
        return Stats(perCategory, totalReceived, totalRead)
    }

    // region internals

    private fun readListRaw(context: Context): List<InboxNotification> {
        val raw = prefs(context).getString(KEY_ITEMS, null) ?: return emptyList()
        return try {
            gson.fromJson<MutableList<InboxNotification>>(raw, listType) ?: emptyList()
        } catch (_: Throwable) {
            // Corrupt JSON shouldn't crash the inbox screen.
            emptyList()
        }
    }

    private fun writeList(context: Context, list: List<InboxNotification>) {
        prefs(context).edit().putString(KEY_ITEMS, gson.toJson(list)).apply()
    }

    private fun readStatsRaw(context: Context): MutableMap<String, CategoryStats> {
        val raw = prefs(context).getString(KEY_STATS, null) ?: return mutableMapOf()
        return try {
            gson.fromJson<MutableMap<String, CategoryStats>>(raw, statsType)
                ?: mutableMapOf()
        } catch (_: Throwable) {
            mutableMapOf()
        }
    }

    private fun updateStats(
        context: Context,
        block: (MutableMap<String, CategoryStats>) -> Unit
    ) {
        val map = readStatsRaw(context)
        block(map)
        prefs(context).edit().putString(KEY_STATS, gson.toJson(map)).apply()
    }

    // endregion
}
