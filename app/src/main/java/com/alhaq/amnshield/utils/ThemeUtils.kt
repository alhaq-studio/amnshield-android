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

    private fun resolveTheme(context: Context): Int {
        val themeStyle = context
            .getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
            .getString(KEY_THEME_STYLE, "default")

        return when (themeStyle) {
            "gradient" -> R.style.Theme_AmnShield_Gradient
            "purple" -> R.style.Theme_AmnShield_Purple
            "emerald" -> R.style.Theme_AmnShield_Emerald
            "sunset" -> R.style.Theme_AmnShield_Sunset
            else -> R.style.Theme_AmnShield
        }
    }
}
