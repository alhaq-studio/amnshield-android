package com.alhaq.amnshield.ui.dialogs

import android.annotation.SuppressLint
import android.content.Intent
import android.view.MotionEvent
import android.widget.ScrollView
import androidx.fragment.app.DialogFragment
import com.alhaq.amnshield.utils.SavedPreferencesLoader
import nl.joery.timerangepicker.TimeRangePicker

open class BaseDialog(val savedPreferencesLoader: SavedPreferencesLoader? = null) :
    DialogFragment() {
    fun sendRefreshRequest(action: String) {
        val ctx = context ?: return
        val intent = Intent(action).setPackage(ctx.packageName)
        ctx.sendBroadcast(intent)
    }


    @SuppressLint("ClickableViewAccessibility")
    fun fixPickerInterceptBug(scrollview: ScrollView, picker: TimeRangePicker) {
        picker.setOnTouchListener { v, event ->
            // Disable ScrollView's touch interception when interacting with the picker
            when (event.action) {
                MotionEvent.ACTION_DOWN -> scrollview.requestDisallowInterceptTouchEvent(true)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> scrollview.requestDisallowInterceptTouchEvent(
                    false
                )
            }
            v.onTouchEvent(event) // Pass the event to the picker
        }
    }
}