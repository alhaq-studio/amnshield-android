package com.alhaq.amnshield.premium

import android.content.Context
import com.alhaq.amnshield.BuildConfig
import com.alhaq.amnshield.utils.SavedPreferencesLoader

class PremiumManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val preferencesLoader = SavedPreferencesLoader(appContext)

    enum class UserType {
        FREE,
        COMPASSIONATE,
        PREMIUM,
        SPECIAL
    }

    /**
     * Check if user has premium or special access
     */
    fun isPremium(): Boolean {
        val hasLocalPremium = if (BuildConfig.IS_PLAYSTORE) {
            preferencesLoader.isPremiumUser()
        } else {
            isLicenseKeyValid()
        }
        return hasLocalPremium || isSpecialUser() || isCompassionateAccessActive()
    }

    fun isCompassionateAccessActive(): Boolean {
        return preferencesLoader.getCompassionateAccessExpiry() > System.currentTimeMillis()
    }

    /**
     * Check if user is a special user with the special access ID
     */
    fun isSpecialUser(): Boolean {
        val specialId = preferencesLoader.getSpecialAccessId()
        return specialId == SPECIAL_ACCESS_ID
    }

    /**
     * Check if the offline license key is valid
     */
    fun isLicenseKeyValid(): Boolean {
        val key = preferencesLoader.getLicenseKey() ?: return false
        val email = preferencesLoader.getLicenseEmail() ?: return false
        val payload = LicenseValidator.verifyLicense(key) ?: return false
        return payload.email == email && payload.expires > System.currentTimeMillis()
    }

    /**
     * Get the current user type
     */
    fun getUserType(): UserType {
        val isPremiumActive = if (BuildConfig.IS_PLAYSTORE) {
            preferencesLoader.isPremiumUser()
        } else {
            isLicenseKeyValid()
        }
        return when {
            isSpecialUser() -> UserType.SPECIAL
            isCompassionateAccessActive() -> UserType.COMPASSIONATE
            isPremiumActive -> UserType.PREMIUM
            else -> UserType.FREE
        }
    }

    /**
     * Get user type as display string
     */
    fun getUserTypeLabel(): String {
        return when (getUserType()) {
            UserType.FREE -> "Free"
            UserType.COMPASSIONATE -> "Compassionate Access"
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
     * Redeem offline license key
     */
    fun redeemLicenseKey(licenseString: String): Boolean {
        if (BuildConfig.IS_PLAYSTORE) return false
        val payload = LicenseValidator.verifyLicense(licenseString) ?: return false
        preferencesLoader.saveLicenseKey(payload.email, licenseString)
        return true
    }

    /**
     * Remove offline license key
     */
    fun removeLicenseKey() {
        preferencesLoader.clearLicenseKey()
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
