package com.alhaq.amnshield.ui.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.alhaq.amnshield.R
import com.alhaq.amnshield.ui.activity.FragmentActivity

class BreathingWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "BreathingWidget"
        private const val ACTION_START_BREATHING = "com.alhaq.amnshield.breathing.START"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { widgetId ->
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_START_BREATHING) {
            val openIntent = Intent(context, FragmentActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("feature_type", "focus_mode")
                putExtra("start_breathing", true)
            }
            context.startActivity(openIntent)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int
    ) {
        try {
            val views = RemoteViews(context.packageName, R.layout.widget_breathing).apply {
                val openIntent = Intent(context, FragmentActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("feature_type", "focus_mode")
                    putExtra("start_breathing", true)
                }
                val pendingOpen = PendingIntent.getActivity(
                    context,
                    0,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                setOnClickPendingIntent(R.id.btn_start_breathing, pendingOpen)
            }

            appWidgetManager.updateAppWidget(widgetId, views)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating BreathingWidget", e)
        }
    }
}
