package com.alhaq.amnshield.utils

import android.app.Activity
import android.content.Context
import com.alhaq.amnshield.R

object ThemeUtils {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME_STYLE = "theme_style"

    fun applyTheme(activity: Activity) {
        activity.setTheme(resolveTheme(activity))
    }

    fun resolveTheme(context: Context): Int {
        val themeStyle = context
            .getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
            .getString(KEY_THEME_STYLE, "emerald")

        return when (themeStyle) {
            "gradient" -> R.style.Theme_AmnShield_Gradient
            "purple" -> R.style.Theme_AmnShield_Purple
            "emerald" -> R.style.Theme_AmnShield_Emerald
            "sunset" -> R.style.Theme_AmnShield_Sunset
            else -> R.style.Theme_AmnShield_Emerald
        }
    }

    fun resolveAppTheme(context: Context): com.alhaq.amnshield.ui.state.AppTheme {
        val themeStyle = context
            .getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
            .getString(KEY_THEME_STYLE, "emerald")

        return when (themeStyle) {
            "sunset" -> com.alhaq.amnshield.ui.state.AppTheme.SUNSET_GLOW
            "emerald" -> com.alhaq.amnshield.ui.state.AppTheme.EMERALD_CALM
            "purple" -> com.alhaq.amnshield.ui.state.AppTheme.COSMIC_NIGHT
            else -> com.alhaq.amnshield.ui.state.AppTheme.EMERALD_CALM
        }
    }
}
