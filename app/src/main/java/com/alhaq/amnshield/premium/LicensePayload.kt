package com.alhaq.amnshield.premium

data class LicensePayload(
    val email: String,
    val type: String,
    val expires: Long
)
