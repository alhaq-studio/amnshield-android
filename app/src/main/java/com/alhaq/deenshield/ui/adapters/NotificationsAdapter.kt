package com.alhaq.deenshield.ui.adapters

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alhaq.deenshield.R
import com.alhaq.deenshield.utils.NotificationInboxStore.Category
import com.alhaq.deenshield.utils.NotificationInboxStore.InboxNotification

class NotificationsAdapter(
    private var items: List<InboxNotification>
) : RecyclerView.Adapter<NotificationsAdapter.ViewHolder>() {

    fun updateData(newItems: List<InboxNotification>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.notification_title)
        private val message: TextView = itemView.findViewById(R.id.notification_message)
        private val meta: TextView = itemView.findViewById(R.id.notification_meta)
        private val unreadDot: View = itemView.findViewById(R.id.unread_dot)

        fun bind(item: InboxNotification) {
            title.text = item.title
            message.text = item.message

            val relative = DateUtils.getRelativeTimeSpanString(
                item.timestamp,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            )
            meta.text = "${categoryLabel(item.category)} · $relative"

            unreadDot.visibility = if (item.read) View.INVISIBLE else View.VISIBLE
            itemView.alpha = if (item.read) 0.78f else 1f
        }

        private fun categoryLabel(category: Category): String = when (category) {
            Category.DAILY_REPORT -> "Daily report"
            Category.REMINDER -> "Reminder"
            Category.ACHIEVEMENT -> "Achievement"
            Category.BLOCK_ALERT -> "Block alert"
            Category.SYSTEM -> "DeenShield"
        }
    }
}
