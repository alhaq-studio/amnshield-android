package com.alhaq.deenshield.ui.activity

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alhaq.deenshield.R
import com.alhaq.deenshield.ui.adapters.NotificationsAdapter
import com.alhaq.deenshield.utils.NotificationInboxStore
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

/**
 * Inbox screen for in-app DeenShield notifications.
 *
 * The toolbar bell icon in [MainActivity] opens this screen so users see
 * their actual notification history. Reminder/notification preferences live
 * inside Settings ("Reminders") and are reachable from the overflow menu here.
 *
 * Interactions:
 *  - Tap an item: marks it read individually (read latency is recorded).
 *  - Swipe an item: removes it; an undo snackbar restores it within the
 *    timeout window.
 *  - Items older than the retention window are pruned automatically by the
 *    store on every read.
 */
class NotificationsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: View
    private lateinit var adapter: NotificationsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        val themeStyle = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
            .getString("theme_style", "default")
        if (themeStyle == "gradient") {
            setTheme(R.style.Theme_DeenShield_Gradient)
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notifications)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.notifications)

        try {
            recyclerView = findViewById(R.id.notifications_recycler_view)
            emptyState = findViewById(R.id.empty_state)
            adapter = NotificationsAdapter(emptyList()) { item ->
                NotificationInboxStore.markRead(this, item.id)
                refresh()
            }
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = adapter
            attachSwipeToDismiss()

            findViewById<View>(R.id.btn_open_reminder_settings).setOnClickListener {
                openReminderSettings()
            }
        } catch (t: Throwable) {
            android.util.Log.e("NotificationsActivity", "onCreate failed", t)
            Toast.makeText(this, "Notifications could not be opened.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isFinishing || !::adapter.isInitialized) return
        refresh()
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

    private fun attachSwipeToDismiss() {
        val background = ColorDrawable(Color.parseColor("#33000000"))
        val callback = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val item = adapter.getItemAt(vh.bindingAdapterPosition) ?: run {
                    refresh(); return
                }
                NotificationInboxStore.remove(this@NotificationsActivity, item.id)
                refresh()
                Snackbar.make(
                    recyclerView,
                    R.string.notifications_item_removed,
                    Snackbar.LENGTH_LONG
                ).setAction(R.string.notifications_undo) {
                    NotificationInboxStore.add(
                        this@NotificationsActivity,
                        item.title,
                        item.message,
                        item.category
                    )
                    refresh()
                }.show()
            }

            override fun onChildDraw(
                c: Canvas,
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val view = vh.itemView
                background.setBounds(view.left, view.top, view.right, view.bottom)
                background.draw(c)
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(recyclerView)
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
