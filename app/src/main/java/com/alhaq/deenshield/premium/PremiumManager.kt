package com.alhaq.deenshield.premium

import android.content.Context
import com.alhaq.deenshield.utils.SavedPreferencesLoader

class PremiumManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val preferencesLoader = SavedPreferencesLoader(appContext)

    enum class UserType {
        FREE,
        PREMIUM,
        SPECIAL
    }

    /**
     * Check if user has premium or special access
     */
    fun isPremium(): Boolean {
        return preferencesLoader.isPremiumUser() || isSpecialUser()
    }

    /**
     * Check if user is a special user with the special access ID
     */
    fun isSpecialUser(): Boolean {
        val specialId = preferencesLoader.getSpecialAccessId()
        return specialId == SPECIAL_ACCESS_ID
    }

    /**
     * Get the current user type
     */
    fun getUserType(): UserType {
        return when {
            isSpecialUser() -> UserType.SPECIAL
            preferencesLoader.isPremiumUser() -> UserType.PREMIUM
            else -> UserType.FREE
        }
    }

    /**
     * Get user type as display string
     */
    fun getUserTypeLabel(): String {
        return when (getUserType()) {
            UserType.FREE -> "Free"
            UserType.PREMIUM -> "Premium"
            UserType.SPECIAL -> "Special"
        }
    }

    /**
     * Update regular premium status (purchased)
     */
    fun updatePremiumStatus(active: Boolean) {
        preferencesLoader.setPremiumUser(active)
    }

    /**
     * Set special access ID
     * @param accessId The special access ID provided by company
     * @return true if the ID is valid and accepted
     */
    fun setSpecialAccessId(accessId: String): Boolean {
        val isValid = accessId.trim() == SPECIAL_ACCESS_ID
        if (isValid) {
            preferencesLoader.setSpecialAccessId(accessId.trim())
        }
        return isValid
    }

    /**
     * Remove special access
     */
    fun removeSpecialAccess() {
        preferencesLoader.setSpecialAccessId("")
    }

    fun shouldShowReminder(): Boolean {
        if (isPremium()) return false
        val now = System.currentTimeMillis()
        val last = preferencesLoader.getLastPremiumReminder()
        return now - last >= REMINDER_INTERVAL_MS
    }

    fun markReminderShown() {
        preferencesLoader.setLastPremiumReminder(System.currentTimeMillis())
    }

    fun resetReminderWindow() {
        preferencesLoader.setLastPremiumReminder(0L)
    }

    companion object {
        private const val REMINDER_INTERVAL_MS = 3L * 24 * 60 * 60 * 1000 // ~twice per week
        
        // Special access ID for users who contacted the company
        const val SPECIAL_ACCESS_ID = "Special-2025_free_premium_one_year_access_2416"

        @Volatile
        private var instance: PremiumManager? = null

        fun getInstance(context: Context): PremiumManager {
            return instance ?: synchronized(this) {
                instance ?: PremiumManager(context).also { instance = it }
            }
        }
    }
}
