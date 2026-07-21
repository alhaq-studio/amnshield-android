package com.alhaq.amnshield.ui.fragments.features

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.alhaq.amnshield.R
import com.alhaq.amnshield.databinding.FragmentWebsiteBlockerConfigBinding
import com.alhaq.amnshield.databinding.ItemSocialBlockCardBinding
import com.alhaq.amnshield.services.AmnShieldAccessibilityService
import com.alhaq.amnshield.premium.PremiumManager
import java.util.Locale

/**
 * Configuration screen for managing blocked websites/URLs.
 */
class WebsiteBlockerConfigFragment : BaseFeatureFragment() {

    private var _binding: FragmentWebsiteBlockerConfigBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWebsiteBlockerConfigBinding.inflate(inflater, container, false)

        val premiumManager = PremiumManager.getInstance(requireContext().applicationContext)
        if (!premiumManager.isPremium()) {
            binding.configContainer.visibility = View.GONE
            binding.statusCard.visibility = View.VISIBLE
            binding.statusMessage.text = getString(R.string.premium_required_message)
            binding.btnStatusAction.text = getString(R.string.premium_view_plans)
            binding.btnStatusAction.setOnClickListener {
                val intent = Intent(requireContext(), com.alhaq.amnshield.ui.activity.FragmentActivity::class.java)
                intent.putExtra("feature_type", "premium_features")
                startActivity(intent)
            }
            return binding.root
        }

        if (!isAccessibilityServiceEnabled(AmnShieldAccessibilityService::class.java)) {
            binding.configContainer.visibility = View.GONE
            binding.statusCard.visibility = View.VISIBLE
            binding.statusMessage.text = "Please enable the main AmnShield Accessibility Service"
            binding.btnStatusAction.text = "Enable Service"
            binding.btnStatusAction.setOnClickListener {
                showAccessibilityInfoDialog("AmnShield Accessibility Service", AmnShieldAccessibilityService::class.java)
            }
            return binding.root
        }

        binding.statusCard.visibility = View.GONE
        binding.configContainer.visibility = View.VISIBLE

        setupUI()

        return binding.root
    }

    private fun setupUI() {
        // Back arrow
        binding.btnBackArrow.setOnClickListener {
            if (!parentFragmentManager.popBackStackImmediate()) {
                activity?.finish()
            }
        }

        // Enable switch
        setupWebsiteBlockerSwitch()

        binding.cardScheduleWebBlocker.visibility = View.GONE

        binding.btnWarningScreen.setOnClickListener {
            com.alhaq.amnshield.ui.dialogs.TweakAppBlockerWarning(savedPreferencesLoader).show(
                childFragmentManager,
                "tweak_website_blocker_warning"
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupWebsiteBlockerSwitch() {
        val isFeatureEnabled = savedPreferencesLoader.isWebsiteBlockerEnabled()
        
        binding.switchWebsiteBlocker.setOnCheckedChangeListener(null)
        binding.switchWebsiteBlocker.isChecked = isFeatureEnabled

        binding.switchWebsiteBlocker.setOnCheckedChangeListener { buttonView, isChecked ->
            savedPreferencesLoader.setWebsiteBlockerEnabled(isChecked, updateManual = true)
            sendRefreshRequest(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_APP_BLOCKER)
            sendRefreshRequest(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_UNIFIED_FEATURE_SCHEDULES)
        }
    }

    companion object {
        const val FRAGMENT_ID = "website_blocker"
    }
}
