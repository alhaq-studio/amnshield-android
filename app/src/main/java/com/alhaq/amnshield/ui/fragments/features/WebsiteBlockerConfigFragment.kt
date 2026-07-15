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
import com.alhaq.amnshield.ui.fragments.ManageBlockSchedulesFragment
import com.alhaq.amnshield.data.blockers.UnifiedFeatureScheduleRule
import com.alhaq.amnshield.utils.ScheduleUtils
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

        // Setup prefilled website blocking toggles
        setupWebsiteToggles()

        // Schedule Blocker Button
        binding.btnScheduleWebBlocker.setOnClickListener {
            val intent = Intent(requireContext(), com.alhaq.amnshield.ui.activity.FragmentActivity::class.java).apply {
                putExtra("fragment", ManageBlockSchedulesFragment.FRAGMENT_ID)
                putExtra("prefill_target", "WEBSITE_BLOCKER")
            }
            startActivity(intent, activityOptions.toBundle())
        }

        // Add website button
        binding.btnAddWebsite.setOnClickListener {
            val rawUrl = binding.inputWebsite.text?.toString()?.trim()
            if (rawUrl.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Please enter a domain or URL", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            var cleanUrl = rawUrl.lowercase(Locale.ROOT)
            // Strip protocols and www
            cleanUrl = cleanUrl.replace("https://", "")
                .replace("http://", "")
                .replace("www.", "")

            val currentWebsites = savedPreferencesLoader.loadBlockedWebsites().toMutableSet()
            if (currentWebsites.contains(cleanUrl)) {
                Toast.makeText(requireContext(), "Website already blocked", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            currentWebsites.add(cleanUrl)
            savedPreferencesLoader.saveBlockedWebsites(currentWebsites)
            sendRefreshRequest(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_APP_BLOCKER)
            binding.inputWebsite.text?.clear()
            refreshWebsitesList()
        }

        refreshWebsitesList()
    }

    private fun setupWebsiteToggles() {
        val currentWebsites = savedPreferencesLoader.loadBlockedWebsites()

        // 1. Meta (Facebook / Instagram)
        binding.switchBlockMeta.isChecked = currentWebsites.contains("instagram.com") && currentWebsites.contains("facebook.com")
        binding.switchBlockMeta.setOnCheckedChangeListener { _, isChecked ->
            val updatedWebsites = savedPreferencesLoader.loadBlockedWebsites().toMutableSet()
            val domains = listOf("instagram.com", "facebook.com", "m.instagram.com", "m.facebook.com")
            if (isChecked) {
                updatedWebsites.addAll(domains)
            } else {
                updatedWebsites.removeAll(domains)
            }
            savedPreferencesLoader.saveBlockedWebsites(updatedWebsites)
            sendRefreshRequest(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_APP_BLOCKER)
            refreshWebsitesList()
        }

        // 2. TikTok
        binding.switchBlockTiktok.isChecked = currentWebsites.contains("tiktok.com")
        binding.switchBlockTiktok.setOnCheckedChangeListener { _, isChecked ->
            val updatedWebsites = savedPreferencesLoader.loadBlockedWebsites().toMutableSet()
            val domains = listOf("tiktok.com", "m.tiktok.com")
            if (isChecked) {
                updatedWebsites.addAll(domains)
            } else {
                updatedWebsites.removeAll(domains)
            }
            savedPreferencesLoader.saveBlockedWebsites(updatedWebsites)
            sendRefreshRequest(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_APP_BLOCKER)
            refreshWebsitesList()
        }

        // 3. X / Twitter
        binding.switchBlockTwitter.isChecked = currentWebsites.contains("twitter.com") && currentWebsites.contains("x.com")
        binding.switchBlockTwitter.setOnCheckedChangeListener { _, isChecked ->
            val updatedWebsites = savedPreferencesLoader.loadBlockedWebsites().toMutableSet()
            val domains = listOf("twitter.com", "x.com", "mobile.twitter.com")
            if (isChecked) {
                updatedWebsites.addAll(domains)
            } else {
                updatedWebsites.removeAll(domains)
            }
            savedPreferencesLoader.saveBlockedWebsites(updatedWebsites)
            sendRefreshRequest(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_APP_BLOCKER)
            refreshWebsitesList()
        }

        // 4. YouTube
        binding.switchBlockYoutube.isChecked = currentWebsites.contains("youtube.com")
        binding.switchBlockYoutube.setOnCheckedChangeListener { _, isChecked ->
            val updatedWebsites = savedPreferencesLoader.loadBlockedWebsites().toMutableSet()
            val domains = listOf("youtube.com", "m.youtube.com", "youtu.be")
            if (isChecked) {
                updatedWebsites.addAll(domains)
            } else {
                updatedWebsites.removeAll(domains)
            }
            savedPreferencesLoader.saveBlockedWebsites(updatedWebsites)
            sendRefreshRequest(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_APP_BLOCKER)
            refreshWebsitesList()
        }

        // 5. Snapchat
        binding.switchBlockSnapchat.isChecked = currentWebsites.contains("snapchat.com")
        binding.switchBlockSnapchat.setOnCheckedChangeListener { _, isChecked ->
            val updatedWebsites = savedPreferencesLoader.loadBlockedWebsites().toMutableSet()
            val domains = listOf("snapchat.com")
            if (isChecked) {
                updatedWebsites.addAll(domains)
            } else {
                updatedWebsites.removeAll(domains)
            }
            savedPreferencesLoader.saveBlockedWebsites(updatedWebsites)
            sendRefreshRequest(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_APP_BLOCKER)
            refreshWebsitesList()
        }
    }

    private fun refreshWebsitesList() {
        binding.layoutBlockedWebsitesList.removeAllViews()
        val blockedWebsites = savedPreferencesLoader.loadBlockedWebsites()

        // Sync switch states based on current loaded list (so manual additions/deletions update toggles)
        binding.switchBlockMeta.isChecked = blockedWebsites.contains("instagram.com") && blockedWebsites.contains("facebook.com")
        binding.switchBlockTiktok.isChecked = blockedWebsites.contains("tiktok.com")
        binding.switchBlockTwitter.isChecked = blockedWebsites.contains("twitter.com") && blockedWebsites.contains("x.com")
        binding.switchBlockYoutube.isChecked = blockedWebsites.contains("youtube.com")
        binding.switchBlockSnapchat.isChecked = blockedWebsites.contains("snapchat.com")

        for (website in blockedWebsites) {
            val itemBinding = ItemSocialBlockCardBinding.inflate(layoutInflater, binding.layoutBlockedWebsitesList, false)
            itemBinding.txtTitle.text = website
            itemBinding.btnDelete.setOnClickListener {
                val updatedWebsites = savedPreferencesLoader.loadBlockedWebsites().toMutableSet()
                updatedWebsites.remove(website)
                savedPreferencesLoader.saveBlockedWebsites(updatedWebsites)
                sendRefreshRequest(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_APP_BLOCKER)
                refreshWebsitesList()
            }

            binding.layoutBlockedWebsitesList.addView(itemBinding.root)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupWebsiteBlockerSwitch() {
        val isFeatureEnabled = savedPreferencesLoader.isWebsiteBlockerEnabled() || 
            savedPreferencesLoader.loadUnifiedFeatureScheduleRules().any { it.targets.contains(UnifiedFeatureScheduleRule.FeatureTarget.WEBSITE_BLOCKER) && it.isEnabled == true }
        
        binding.switchWebsiteBlocker.setOnCheckedChangeListener(null)
        binding.switchWebsiteBlocker.isChecked = isFeatureEnabled

        binding.switchWebsiteBlocker.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                val hasSchedules = savedPreferencesLoader.loadUnifiedFeatureScheduleRules().any { 
                    it.targets.contains(UnifiedFeatureScheduleRule.FeatureTarget.WEBSITE_BLOCKER) && it.isEnabled == true 
                }
                if (!hasSchedules) {
                    buttonView.setOnCheckedChangeListener(null)
                    buttonView.isChecked = false
                    setupWebsiteBlockerSwitch()
                    
                    showFeatureScheduleDialog("Website Blocker", { start, end, days ->
                        ScheduleUtils.autoResolveAndSaveFeatureSchedule(
                            requireContext(),
                            savedPreferencesLoader,
                            "Website Blocker",
                            start,
                            end,
                            days
                        )
                        setupWebsiteBlockerSwitch()
                        sendRefreshRequest(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_APP_BLOCKER)
                        sendRefreshRequest(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_UNIFIED_FEATURE_SCHEDULES)
                    }, {
                        setupWebsiteBlockerSwitch()
                    })
                } else {
                    savedPreferencesLoader.setWebsiteBlockerEnabled(true, updateManual = true)
                    sendRefreshRequest(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_APP_BLOCKER)
                    sendRefreshRequest(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_UNIFIED_FEATURE_SCHEDULES)
                }
            } else {
                ScheduleUtils.disableFeatureSchedules(savedPreferencesLoader, "Website Blocker")
                sendRefreshRequest(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_APP_BLOCKER)
                sendRefreshRequest(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_UNIFIED_FEATURE_SCHEDULES)
            }
        }
    }

    companion object {
        const val FRAGMENT_ID = "website_blocker"
    }
}
