package com.alhaq.amnshield.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import com.alhaq.amnshield.data.AmnShieldProductDetails
import com.alhaq.amnshield.premium.PremiumProducts
import com.android.billingclient.api.*

class BillingClientWrapper(private val context: Context) : PurchasesUpdatedListener {

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private var onPurchaseFinished: ((Boolean, String) -> Unit)? = null
    private val cachedProductDetails = mutableMapOf<String, ProductDetails>()

    companion object {
        private const val TAG = "BillingClientWrapper"
    }

    fun startConnection(onConnected: () -> Unit) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    onConnected()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        })
    }

    fun queryProducts(productIds: List<String>, onProductsQueried: (List<AmnShieldProductDetails>) -> Unit) {
        val inAppProductList = productIds.filter { it in PremiumProducts.allInAppProducts }.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }

        val subProductList = productIds.filter { it in PremiumProducts.allSubscriptionProducts }.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }

        val results = mutableListOf<AmnShieldProductDetails>()
        var pendingQueries = 0
        if (inAppProductList.isNotEmpty()) pendingQueries++
        if (subProductList.isNotEmpty()) pendingQueries++

        if (pendingQueries == 0) {
            onProductsQueried(emptyList())
            return
        }

        val checkAndCallback = {
            pendingQueries--
            if (pendingQueries == 0) {
                onProductsQueried(results)
            }
        }

        if (inAppProductList.isNotEmpty()) {
            val params = QueryProductDetailsParams.newBuilder().setProductList(inAppProductList).build()
            billingClient.queryProductDetailsAsync(params) { billingResult, queryResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val productDetailsList = queryResult.productDetailsList
                    val mapped = productDetailsList.map { details ->
                        cachedProductDetails[details.productId] = details
                        val price = details.oneTimePurchaseOfferDetails?.formattedPrice
                            ?: details.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                            ?: ""
                        AmnShieldProductDetails(details.productId, price)
                    }
                    synchronized(results) { results.addAll(mapped) }
                } else {
                    Log.w(TAG, "Failed to query INAPP products: ${billingResult.debugMessage}")
                }
                checkAndCallback()
            }
        }

        if (subProductList.isNotEmpty()) {
            val params = QueryProductDetailsParams.newBuilder().setProductList(subProductList).build()
            billingClient.queryProductDetailsAsync(params) { billingResult, queryResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val productDetailsList = queryResult.productDetailsList
                    val mapped = productDetailsList.map { details ->
                        cachedProductDetails[details.productId] = details
                        val price = details.oneTimePurchaseOfferDetails?.formattedPrice
                            ?: details.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                            ?: ""
                        AmnShieldProductDetails(details.productId, price)
                    }
                    synchronized(results) { results.addAll(mapped) }
                } else {
                    Log.w(TAG, "Failed to query SUBS products: ${billingResult.debugMessage}")
                }
                checkAndCallback()
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, productDetails: AmnShieldProductDetails, onPurchaseFinished: (Boolean, String) -> Unit) {
        this.onPurchaseFinished = onPurchaseFinished
        val realDetails = cachedProductDetails[productDetails.productId]
        if (realDetails == null) {
            onPurchaseFinished(false, "Product details not found or cached")
            return
        }
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(realDetails)
                .build()
        )
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    /**
     * Query existing purchases to restore premium status.
     * This works for both real users and License Test accounts.
     * Test accounts will see their test purchases here without being charged.
     */
    fun queryPurchases(onPurchasesQueried: (List<String>) -> Unit) {
        val activePurchases = mutableListOf<String>()
        var pendingQueries = 2

        val checkAndCallback = {
            pendingQueries--
            if (pendingQueries == 0) {
                onPurchasesQueried(activePurchases)
            }
        }

        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Found ${purchases.size} active INAPP purchase(s)")
                val mapped = purchases.mapNotNull { it.products.firstOrNull() }
                synchronized(activePurchases) { activePurchases.addAll(mapped) }
            } else {
                Log.w(TAG, "Failed to query INAPP purchases: ${billingResult.debugMessage}")
            }
            checkAndCallback()
        }

        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Found ${purchases.size} active SUBS purchase(s)")
                val mapped = purchases.mapNotNull { it.products.firstOrNull() }
                synchronized(activePurchases) { activePurchases.addAll(mapped) }
            } else {
                Log.w(TAG, "Failed to query SUBS purchases: ${billingResult.debugMessage}")
            }
            checkAndCallback()
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        Log.d(TAG, "onPurchasesUpdated - Result code: ${billingResult.responseCode}, Debug message: ${billingResult.debugMessage}")
        
        val isSuccess = billingResult.responseCode == BillingClient.BillingResponseCode.OK
        if (isSuccess && purchases != null) {
            Log.d(TAG, "Processing ${purchases.size} purchase(s)")
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "User canceled the purchase")
            onPurchaseFinished?.invoke(false, "User canceled the purchase")
        } else {
            Log.w(TAG, "Purchase failed or canceled: ${billingResult.debugMessage}")
            onPurchaseFinished?.invoke(false, billingResult.debugMessage)
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        // Log purchase for debugging (helps identify test purchases)
        Log.d(TAG, "Processing purchase: ${purchase.products}, state: ${purchase.purchaseState}")
        
        // Google Play automatically handles test purchases for License Test accounts
        // Test purchases will have purchaseState == PURCHASED but won't charge real money
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            Log.d(TAG, "Purchase successful. Acknowledged: ${purchase.isAcknowledged}")
            
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    Log.d(TAG, "Acknowledgment result: ${billingResult.responseCode}")
                    val isAckSuccess = billingResult.responseCode == BillingClient.BillingResponseCode.OK
                    onPurchaseFinished?.invoke(isAckSuccess, billingResult.debugMessage)
                }
            } else {
                // Already acknowledged, invoke callback immediately
                onPurchaseFinished?.invoke(true, "Already acknowledged")
            }
        } else {
            Log.d(TAG, "Purchase not in PURCHASED state: ${purchase.purchaseState}")
            onPurchaseFinished?.invoke(false, "Purchase state is: ${purchase.purchaseState}")
        }
    }
}
