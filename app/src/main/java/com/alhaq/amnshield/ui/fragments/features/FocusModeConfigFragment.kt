package com.alhaq.amnshield.ui.fragments.features

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import com.alhaq.amnshield.R
import com.alhaq.amnshield.databinding.FragmentFocusModeConfigBinding
import com.alhaq.amnshield.services.AmnShieldAccessibilityService
import com.alhaq.amnshield.ui.activity.SelectAppsActivity
import com.alhaq.amnshield.ui.activity.FragmentActivity
import com.alhaq.amnshield.ui.fragments.BlocksManagerFragment
import com.alhaq.amnshield.ui.dialogs.StartFocusMode
import com.alhaq.amnshield.utils.SavedPreferencesLoader
import com.alhaq.amnshield.premium.PremiumManager

/**
 * Comprehensive Focus Mode configuration screen with all options:
 * - Select apps to block during focus mode
 * - Configure auto-focus schedules
 * - Start focus mode session
 */
class FocusModeConfigFragment : BaseFeatureFragment() {

    private var _binding: FragmentFocusModeConfigBinding? = null
    private val binding get() = _binding!!
    private val premiumManager by lazy { PremiumManager.getInstance(requireContext().applicationContext) }

    private val selectAppsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val selectedApps = result.data?.getStringArrayListExtra("SELECTED_APPS")
            selectedApps?.let {
                savedPreferencesLoader.saveFocusModeSelectedApps(it)
                updateSelectedAppsCount(it.size)
                val intent = Intent(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_FOCUS_MODE)
                intent.setPackage(requireContext().packageName)
                requireContext().sendBroadcast(intent)
            }
        }
    }



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFocusModeConfigBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!premiumManager.isPremium()) {
            showPremiumRequiredState()
            return
        }
        
        checkServiceStatus()
        setupClickListeners()
        loadConfiguration()
    }

    private fun checkServiceStatus() {
        if (!isAccessibilityServiceEnabled(AmnShieldAccessibilityService::class.java)) {
            binding.statusCard.visibility = View.VISIBLE
            binding.configContainer.visibility = View.GONE
            binding.statusMessage.text = "Please enable AmnShield accessibility service to continue."
            binding.btnStatusAction.text = getString(R.string.open_accessibility_settings)
            binding.btnStatusAction.setOnClickListener {
                showAccessibilityInfoDialog("AmnShield Accessibility Service", AmnShieldAccessibilityService::class.java)
            }
        } else {
            binding.statusCard.visibility = View.GONE
            binding.configContainer.visibility = View.VISIBLE
        }
    }

    private fun setupClickListeners() {
        binding.btnSelectApps.setOnClickListener {
            val intent = Intent(requireContext(), SelectAppsActivity::class.java)
            intent.putStringArrayListExtra(
                "PRE_SELECTED_APPS",
                ArrayList(savedPreferencesLoader.getFocusModeSelectedApps())
            )
            selectAppsLauncher.launch(intent, activityOptions)
        }

        binding.btnAutoFocus.setOnClickListener {
            val intent = Intent(requireContext(), FragmentActivity::class.java).apply {
                putExtra("fragment", BlocksManagerFragment.FRAGMENT_ID)
                putExtra("prefill_target", "FOCUS_MODE")
            }
            startActivity(intent)
        }

        binding.btnStartFocusMode.setOnClickListener {
            StartFocusMode(savedPreferencesLoader) {
                // Focus mode started
                binding.btnSelectApps.isEnabled = false
                binding.btnStartFocusMode.isEnabled = false
            }.show(childFragmentManager, "start_focus_mode")
        }
    }

    private fun showPremiumRequiredState() {
        binding.statusCard.visibility = View.VISIBLE
        binding.configContainer.visibility = View.GONE
        binding.statusMessage.text = getString(R.string.premium_required_message)
        binding.btnStatusAction.text = getString(R.string.premium_view_plans)
        binding.btnStatusAction.setOnClickListener { openPremiumScreen() }
    }

    private fun openPremiumScreen() {
        val intent = Intent(requireContext(), com.alhaq.amnshield.ui.activity.FragmentActivity::class.java)
        intent.putExtra("feature_type", "premium_features")
        startActivity(intent, activityOptions.toBundle())
    }

    private fun loadConfiguration() {
        val selectedApps = savedPreferencesLoader.getFocusModeSelectedApps()
        updateSelectedAppsCount(selectedApps.size)
    }

    private fun updateSelectedAppsCount(count: Int) {
        binding.txtSelectedAppsCount.text = getString(R.string.app_s_selected, count)
    }

    private fun loadAutoFocusSchedules() {
        // No-op - auto focus schedules are unified under schedules
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val FRAGMENT_ID = "focus_mode_config"
    }
}
