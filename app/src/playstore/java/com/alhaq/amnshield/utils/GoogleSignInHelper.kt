package com.alhaq.amnshield.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.alhaq.amnshield.R
import com.alhaq.amnshield.data.AmnShieldAccount
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class GoogleSignInHelper(private val context: Context) {

    private val googleSignInClient: GoogleSignInClient

    init {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    fun handleSignInResult(data: Intent?): AmnShieldAccount? {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        return try {
            val account = task.getResult(ApiException::class.java)
            account?.let {
                AmnShieldAccount(
                    displayName = it.displayName,
                    email = it.email,
                    photoUrl = it.photoUrl,
                    idToken = it.idToken
                )
            }
        } catch (e: ApiException) {
            null
        }
    }

    fun signOut(onComplete: () -> Unit) {
        googleSignInClient.signOut().addOnCompleteListener {
            onComplete()
        }
    }

    fun getLastSignedInAccount(): AmnShieldAccount? {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account?.let {
            AmnShieldAccount(
                displayName = it.displayName,
                email = it.email,
                photoUrl = it.photoUrl,
                idToken = it.idToken
            )
        }
    }

    fun isSignedIn(): Boolean {
        return GoogleSignIn.getLastSignedInAccount(context) != null
    }
}
