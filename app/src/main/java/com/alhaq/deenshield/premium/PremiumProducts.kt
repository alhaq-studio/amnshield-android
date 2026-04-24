package com.alhaq.deenshield.premium

object PremiumProducts {
    const val PRODUCT_LIFETIME = "deenshield_premium_lifetime"
    const val PRODUCT_MONTHLY = "deenshield_premium_monthly"
    const val PRODUCT_YEARLY = "deenshield_premium_yearly"

    val allInAppProducts = listOf(PRODUCT_LIFETIME)
    val allSubscriptionProducts = listOf(PRODUCT_MONTHLY, PRODUCT_YEARLY)
}
