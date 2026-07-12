package com.alhaq.amnshield.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alhaq.amnshield.R
import com.alhaq.amnshield.ui.adapters.NotificationsAdapter
import com.alhaq.amnshield.utils.NotificationInboxStore
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Inbox screen for in-app AmnShield notifications.
 *
 * The toolbar bell icon in [MainActivity] now opens this screen so users see
 * their actual notification history. Reminder/notification preferences live
 * inside the Settings screen ("Reminders") and are reachable from the
 * overflow menu here as well.
 */
class NotificationsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: View
    private lateinit var adapter: NotificationsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        com.alhaq.amnshield.utils.ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.notifications)

        try {
            recyclerView = findViewById(R.id.notifications_recycler_view)
            emptyState = findViewById(R.id.empty_state)
            adapter = NotificationsAdapter(emptyList())
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = adapter

            findViewById<View>(R.id.btn_open_reminder_settings).setOnClickListener {
                openReminderSettings()
            }
        } catch (t: Throwable) {
            android.util.Log.e("NotificationsActivity", "onCreate failed", t)
            Toast.makeText(
                this,
                "Notifications could not be opened.",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isFinishing || !::adapter.isInitialized) return
        refresh()
        // Mark items as read once the user actually views them so the unread
        // badge clears on next visit.
        NotificationInboxStore.markAllRead(this)
    }

    private fun refresh() {
        try {
            val items = NotificationInboxStore.getAll(this)
            adapter.updateData(items)
            val empty = items.isEmpty()
            emptyState.visibility = if (empty) View.VISIBLE else View.GONE
            recyclerView.visibility = if (empty) View.GONE else View.VISIBLE
        } catch (t: Throwable) {
            android.util.Log.e("NotificationsActivity", "refresh failed", t)
            adapter.updateData(emptyList())
            emptyState.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_notifications, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_mark_all_read -> {
                NotificationInboxStore.markAllRead(this)
                refresh()
                true
            }
            R.id.action_clear_all -> {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.notifications_clear_all)
                    .setMessage(R.string.notifications_clear_all_confirm)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.notifications_clear_all) { _, _ ->
                        NotificationInboxStore.clearAll(this)
                        refresh()
                    }
                    .show()
                true
            }
            R.id.action_reminder_settings -> {
                openReminderSettings()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun openReminderSettings() {
        startActivity(Intent(this, RemindersActivity::class.java))
    }
}
