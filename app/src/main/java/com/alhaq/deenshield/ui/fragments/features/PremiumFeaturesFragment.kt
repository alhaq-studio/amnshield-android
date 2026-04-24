package com.alhaq.deenshield.ui.fragments.features

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.alhaq.deenshield.R
import com.alhaq.deenshield.databinding.FragmentPremiumFeaturesBinding
import com.alhaq.deenshield.premium.PremiumManager
import com.alhaq.deenshield.premium.PremiumProducts
import com.alhaq.deenshield.utils.BillingClientWrapper

class PremiumFeaturesFragment : Fragment() {

    private var _binding: FragmentPremiumFeaturesBinding? = null
    private val binding get() = _binding!!
    private val premiumManager by lazy { PremiumManager.getInstance(requireContext().applicationContext) }
    private lateinit var billingClientWrapper: BillingClientWrapper
    private val products = mutableMapOf<String, ProductDetails>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPremiumFeaturesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        updatePremiumState()

        billingClientWrapper = BillingClientWrapper(requireContext())
        billingClientWrapper.startConnection {
            billingClientWrapper.queryProducts(PremiumProducts.allInAppProducts + PremiumProducts.allSubscriptionProducts) { productDetailsList ->
                productDetailsList.forEach { products[it.productId] = it }
                updateProductDetails()
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun setupClickListeners() {
        binding.btnBuyLifetime.setOnClickListener {
            launchPurchase(PremiumProducts.PRODUCT_LIFETIME)
        }
        binding.btnBuyMonthly.setOnClickListener {
            launchPurchase(PremiumProducts.PRODUCT_MONTHLY)
        }
        binding.btnBuyYearly.setOnClickListener {
            launchPurchase(PremiumProducts.PRODUCT_YEARLY)
        }
        binding.btnRestore.setOnClickListener {
            restorePurchases()
        }
    }

    private fun launchPurchase(productId: String) {
        if (premiumManager.isPremium()) {
            Toast.makeText(requireContext(), R.string.premium_already_active, Toast.LENGTH_SHORT).show()
            return
        }
        val activity = activity ?: return
        products[productId]?.let {
            billingClientWrapper.launchPurchaseFlow(activity, it) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    premiumManager.updatePremiumStatus(true)
                    updatePremiumState()
                    Toast.makeText(requireContext(), R.string.premium_purchase_success, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(), "Purchase failed: ${billingResult.debugMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Restore previous purchases. Works for:
     * - Real users who reinstalled the app
     * - License Test accounts (testers won't be charged)
     * - Users switching devices
     */
    private fun restorePurchases() {
        Toast.makeText(requireContext(), R.string.premium_restore_in_progress, Toast.LENGTH_SHORT).show()
        
        billingClientWrapper.queryPurchases { purchases ->
            if (purchases.isNotEmpty()) {
                // Found existing purchases (test or real)
                premiumManager.updatePremiumStatus(true)
                updatePremiumState()
                Toast.makeText(requireContext(), R.string.premium_purchase_success, Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), "No previous purchases found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updatePremiumState() {
        val isPremium = premiumManager.isPremium()
        binding.premiumActiveContainer.visibility = if (isPremium) View.VISIBLE else View.GONE
        binding.premiumUpsellContainer.visibility = if (isPremium) View.GONE else View.VISIBLE
        binding.btnBuyLifetime.isEnabled = !isPremium
        binding.btnBuyMonthly.isEnabled = !isPremium
        binding.btnBuyYearly.isEnabled = !isPremium
        binding.btnRestore.visibility = if (isPremium) View.GONE else View.VISIBLE
    }

    private fun updateProductDetails() {
        activity?.runOnUiThread {
            products[PremiumProducts.PRODUCT_LIFETIME]?.oneTimePurchaseOfferDetails?.let { offer ->
                binding.txtLifetimePrice.text = offer.formattedPrice
            }
            products[PremiumProducts.PRODUCT_MONTHLY]?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.let { phase ->
                binding.txtMonthlyPrice.text = phase.formattedPrice
            }
            products[PremiumProducts.PRODUCT_YEARLY]?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.let { phase ->
                binding.txtYearlyPrice.text = phase.formattedPrice
            }
        }
    }
}
