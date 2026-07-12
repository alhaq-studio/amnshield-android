package com.alhaq.amnshield.ui.fragments.features

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.alhaq.amnshield.data.AmnShieldProductDetails
import com.alhaq.amnshield.R
import com.alhaq.amnshield.databinding.FragmentPremiumFeaturesBinding
import com.alhaq.amnshield.premium.PremiumManager
import com.alhaq.amnshield.premium.PremiumProducts
import com.alhaq.amnshield.utils.BillingClientWrapper
import com.alhaq.amnshield.utils.SavedPreferencesLoader
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.DateFormat
import java.util.Date

class PremiumFeaturesFragment : Fragment() {

    private var _binding: FragmentPremiumFeaturesBinding? = null
    private val binding get() = _binding!!
    private val premiumManager by lazy { PremiumManager.getInstance(requireContext().applicationContext) }
    private val preferencesLoader by lazy { SavedPreferencesLoader(requireContext().applicationContext) }
    private lateinit var billingClientWrapper: BillingClientWrapper
    private val products = mutableMapOf<String, AmnShieldProductDetails>()

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
        binding.btnCompassionateAccess.setOnClickListener {
            showCompassionateAccessDialog()
        }
        binding.btnSpecialAccess.setOnClickListener {
            showSpecialAccessDialog()
        }
    }

    private fun launchPurchase(productId: String) {
        if (premiumManager.isPremium()) {
            Toast.makeText(requireContext(), R.string.premium_already_active, Toast.LENGTH_SHORT).show()
            return
        }
        val activity = activity ?: return
        products[productId]?.let {
            billingClientWrapper.launchPurchaseFlow(activity, it) { isSuccess, debugMessage ->
                if (isSuccess) {
                    premiumManager.updatePremiumStatus(true)
                    updatePremiumState()
                    Toast.makeText(requireContext(), R.string.premium_purchase_success, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(), "Purchase failed: $debugMessage", Toast.LENGTH_LONG).show()
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
                Toast.makeText(requireContext(), R.string.premium_no_previous_purchases, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updatePremiumState() {
        val userType = premiumManager.getUserType()
        val isPremium = userType != PremiumManager.UserType.FREE
        binding.premiumActiveContainer.visibility = if (isPremium) View.VISIBLE else View.GONE
        binding.premiumUpsellContainer.visibility = if (isPremium) View.GONE else View.VISIBLE
        binding.btnBuyLifetime.isEnabled = !isPremium
        binding.btnBuyMonthly.isEnabled = !isPremium
        binding.btnBuyYearly.isEnabled = !isPremium
        binding.btnRestore.visibility = if (isPremium) View.GONE else View.VISIBLE

        val activeMessage = when (userType) {
            PremiumManager.UserType.PREMIUM -> getString(R.string.premium_active_message)
            PremiumManager.UserType.SPECIAL -> getString(R.string.special_access_active_description)
            PremiumManager.UserType.COMPASSIONATE -> {
                val expiry = preferencesLoader.getCompassionateAccessExpiry()
                if (expiry > 0L) {
                    getString(
                        R.string.compassionate_access_active_until,
                        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(expiry))
                    )
                } else {
                    getString(R.string.compassionate_access_active_description)
                }
            }
            PremiumManager.UserType.FREE -> getString(R.string.premium_active_message)
        }
        binding.txtPremiumActiveMessage.text = activeMessage
    }

    private fun updateProductDetails() {
        activity?.runOnUiThread {
            products[PremiumProducts.PRODUCT_LIFETIME]?.let { product ->
                binding.txtLifetimePrice.text = product.priceText
            }
            products[PremiumProducts.PRODUCT_MONTHLY]?.let { product ->
                binding.txtMonthlyPrice.text = product.priceText
            }
            products[PremiumProducts.PRODUCT_YEARLY]?.let { product ->
                binding.txtYearlyPrice.text = product.priceText
            }
        }
    }

    private fun showCompassionateAccessDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.compassionate_access_title)
            .setMessage(getString(R.string.compassionate_access_intro_message))
            .setPositiveButton(R.string.compassionate_access_intro_positive) { _, _ ->
                showCompassionateAccessForm()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showCompassionateAccessForm() {
        val context = requireContext()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val horizontalPadding = resources.getDimensionPixelSize(R.dimen.padding_normal)
            setPadding(horizontalPadding, 0, horizontalPadding, 0)
        }

        val nameInput = EditText(context).apply {
            hint = getString(R.string.compassionate_access_name_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        }

        val emailInput = EditText(context).apply {
            hint = getString(R.string.compassionate_access_email_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }

        container.addView(nameInput)
        container.addView(emailInput)

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.compassionate_access_form_title)
            .setMessage(getString(R.string.compassionate_access_form_message))
            .setView(container)
            .setPositiveButton(R.string.compassionate_access_proceed, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val name = nameInput.text?.toString()?.trim().orEmpty()
                        val email = emailInput.text?.toString()?.trim().orEmpty()

                        if (name.isEmpty()) {
                            nameInput.error = getString(R.string.compassionate_access_name_required)
                            return@setOnClickListener
                        }

                        if (email.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                            emailInput.error = getString(R.string.compassionate_access_email_invalid)
                            return@setOnClickListener
                        }

                        dialog.dismiss()
                        grantCompassionateAccess(name, email.ifBlank { null })
                    }
                }
            }
            .show()
    }

    private fun grantCompassionateAccess(userName: String, email: String?) {
        val grantedAt = System.currentTimeMillis()
        val appId = "CAP-$grantedAt-${(10000..99999).random()}"
        val expiresAt = grantedAt + (365L * 24 * 60 * 60 * 1000)

        try {
            preferencesLoader.saveCompassionateAccessGrant(
                appId = appId,
                userName = userName,
                email = email,
                grantedAt = grantedAt,
                expiresAt = expiresAt
            )
            premiumManager.resetReminderWindow()
            updatePremiumState()

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.compassionate_access_success_title)
                .setMessage(
                    getString(
                        R.string.compassionate_access_success_message,
                        appId,
                        email ?: getString(R.string.compassionate_access_no_email_value)
                    )
                )
                .setPositiveButton(android.R.string.ok, null)
                .show()
        } catch (_: Exception) {
            Toast.makeText(
                requireContext(),
                R.string.compassionate_access_error,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showSpecialAccessDialog() {
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.special_access_hint)
            inputType = InputType.TYPE_CLASS_TEXT
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.special_access_title)
            .setMessage(getString(R.string.special_access_message))
            .setView(input)
            .setPositiveButton(R.string.special_access_activate) { _, _ ->
                val accessId = input.text?.toString().orEmpty()
                if (premiumManager.setSpecialAccessId(accessId)) {
                    updatePremiumState()
                    Toast.makeText(requireContext(), R.string.special_access_success, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(), R.string.special_access_invalid, Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
