package com.alhaq.deenshield.utils

import android.app.Activity
import android.content.Context
import com.alhaq.deenshield.R

object ThemeUtils {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME_STYLE = "theme_style"

    fun applyTheme(activity: Activity) {
        activity.setTheme(resolveTheme(activity))
    }

    private fun resolveTheme(context: Context): Int {
        val themeStyle = context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME_STYLE, "default")

        return when (themeStyle) {
            "gradient" -> R.style.Theme_DeenShield_Gradient
            "emerald" -> R.style.Theme_DeenShield_Emerald
            "sunset" -> R.style.Theme_DeenShield_Sunset
            else -> R.style.Theme_DeenShield
        }
    }
}
