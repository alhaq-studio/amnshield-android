package com.alhaq.amnshield.utils

import android.content.Context
import android.content.Intent
import com.alhaq.amnshield.data.AmnShieldAccount

/**
 * FOSS stub implementation of GoogleSignInHelper for F-Droid.
 * Bypasses GMS Sign-In dependencies entirely.
 */
class GoogleSignInHelper(private val context: Context) {

    fun getSignInIntent(): Intent {
        // Return dummy intent
        return Intent()
    }

    fun handleSignInResult(data: Intent?): AmnShieldAccount? {
        return null
    }

    fun signOut(onComplete: () -> Unit) {
        onComplete()
    }

    fun getLastSignedInAccount(): AmnShieldAccount? {
        return null
    }

    fun isSignedIn(): Boolean {
        return false
    }
}
