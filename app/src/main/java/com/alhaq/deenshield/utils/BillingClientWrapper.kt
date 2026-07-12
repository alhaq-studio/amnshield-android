package com.alhaq.deenshield.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*

class BillingClientWrapper(private val context: Context) : PurchasesUpdatedListener {

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private var onPurchaseFinished: ((BillingResult) -> Unit)? = null

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

    fun queryProducts(productIds: List<String>, onProductsQueried: (List<ProductDetails>) -> Unit) {
        val productList = productIds.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        billingClient.queryProductDetailsAsync(params) { billingResult, queryResult ->
            val productDetailsList = queryResult.productDetailsList
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                onProductsQueried(productDetailsList)
            } else {
                Log.w(TAG, "Failed to query products: ${billingResult.debugMessage}")
                onProductsQueried(emptyList())
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails, onPurchaseFinished: (BillingResult) -> Unit) {
        this.onPurchaseFinished = onPurchaseFinished
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
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
    fun queryPurchases(onPurchasesQueried: (List<Purchase>) -> Unit) {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Found ${purchases.size} existing purchase(s)")
                onPurchasesQueried(purchases)
            } else {
                Log.w(TAG, "Failed to query purchases: ${billingResult.debugMessage}")
                onPurchasesQueried(emptyList())
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        Log.d(TAG, "onPurchasesUpdated - Result code: ${billingResult.responseCode}, Debug message: ${billingResult.debugMessage}")
        
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            Log.d(TAG, "Processing ${purchases.size} purchase(s)")
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "User canceled the purchase")
            onPurchaseFinished?.invoke(billingResult)
        } else {
            Log.w(TAG, "Purchase failed or canceled: ${billingResult.debugMessage}")
            onPurchaseFinished?.invoke(billingResult)
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
                    onPurchaseFinished?.invoke(billingResult)
                }
            } else {
                // Already acknowledged, invoke callback immediately
                onPurchaseFinished?.invoke(BillingResult.newBuilder()
                    .setResponseCode(BillingClient.BillingResponseCode.OK)
                    .build())
            }
        } else {
            Log.d(TAG, "Purchase not in PURCHASED state: ${purchase.purchaseState}")
        }
    }
}
