package com.alhaq.amnshield.premium

object PremiumProducts {
    const val PRODUCT_LIFETIME = "amnshield_premium_lifetime"
    const val PRODUCT_MONTHLY = "amnshield_premium_monthly"
    const val PRODUCT_YEARLY = "amnshield_premium_yearly"

    val allInAppProducts = listOf(PRODUCT_LIFETIME)
    val allSubscriptionProducts = listOf(PRODUCT_MONTHLY, PRODUCT_YEARLY)
}
