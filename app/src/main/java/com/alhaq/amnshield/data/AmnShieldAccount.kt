package com.alhaq.amnshield.data

import android.net.Uri

data class AmnShieldAccount(
    val displayName: String?,
    val email: String?,
    val photoUrl: Uri? = null,
    val idToken: String? = null
)
