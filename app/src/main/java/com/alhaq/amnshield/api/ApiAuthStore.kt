package com.alhaq.amnshield.api

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Stores the AmnShield API state: whether the API is enabled, and which apps the user has allowed.
 *
 * Identity is the calling app's package. Kept in the shared "app_preferences" file.
 */
object ApiAuthStore {
    private const val PREFS = "app_preferences"
    private const val KEY_ENABLED = "apiEnabled"
    private const val KEY_GRANTS = "apiAuthorizedPackages"

    private val gson = Gson()
    private val mapType = object : TypeToken<MutableMap<String, Long>>() {}.type

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isApiEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    fun setApiEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /** Package -> time it was allowed, in millis. */
    fun grants(context: Context): Map<String, Long> {
        val raw = prefs(context).getString(KEY_GRANTS, null) ?: return emptyMap()
        return try {
            gson.fromJson(raw, mapType) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun grantedPackages(context: Context): Set<String> = grants(context).keys

    /** An app is allowed only when the API is on and one of its packages has been granted. */
    fun isAnyGranted(context: Context, packageNames: Array<String>?): Boolean {
        if (packageNames == null || !isApiEnabled(context)) return false
        val granted = grants(context).keys
        return packageNames.any { it in granted }
    }

    fun grant(context: Context, packageName: String) {
        val updated = grants(context).toMutableMap()
        updated[packageName] = System.currentTimeMillis()
        save(context, updated)
    }

    fun revoke(context: Context, packageName: String) {
        val updated = grants(context).toMutableMap()
        updated.remove(packageName)
        save(context, updated)
    }

    private fun save(context: Context, map: Map<String, Long>) {
        prefs(context).edit().putString(KEY_GRANTS, gson.toJson(map)).apply()
    }
}
