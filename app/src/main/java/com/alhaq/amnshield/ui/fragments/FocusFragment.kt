package com.alhaq.amnshield.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.ComposeView
import androidx.compose.runtime.*
import com.alhaq.amnshield.premium.PremiumManager
import com.alhaq.amnshield.services.AmnShieldAccessibilityService
import com.alhaq.amnshield.ui.activity.FragmentActivity
import com.alhaq.amnshield.ui.activity.SelectAppsActivity
import com.alhaq.amnshield.ui.fragments.features.BaseFeatureFragment
import com.alhaq.amnshield.ui.dialogs.StartFocusMode
import com.alhaq.amnshield.ui.screens.FocusScreen
import com.alhaq.amnshield.ui.theme.AmnShieldTheme
import com.alhaq.amnshield.ui.state.AppTheme
import com.alhaq.amnshield.utils.SavedPreferencesLoader

import androidx.lifecycle.ViewModelProvider
import com.alhaq.amnshield.ui.viewmodel.AmnShieldViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

class FocusFragment : BaseFeatureFragment() {

    private val premiumManager by lazy { PremiumManager.getInstance(requireContext().applicationContext) }
    private val loader by lazy { SavedPreferencesLoader(requireContext()) }
    private lateinit var viewModel: AmnShieldViewModel

    private val isServiceEnabled = mutableStateOf(false)
    private val isFocusModeActive = mutableStateOf(false)
    private val focusModeEndTime = mutableStateOf(0L)
    private val allowedAppsCount = mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[AmnShieldViewModel::class.java]
    }

    private val selectAppsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val selectedApps = result.data?.getStringArrayListExtra("SELECTED_APPS")
            selectedApps?.let {
                loader.saveFocusModeSelectedApps(it)
                refreshFocusState()
                val intent = Intent(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_FOCUS_MODE).apply {
                    setPackage(requireContext().packageName)
                }
                requireContext().sendBroadcast(intent)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val state by viewModel.state.collectAsState()
                AmnShieldTheme(appTheme = state.currentTheme) {
                    FocusScreen(
                        isServiceEnabled = isServiceEnabled.value,
                        isFocusModeActive = isFocusModeActive.value,
                        focusModeEndTime = focusModeEndTime.value,
                        selectedAppsCount = allowedAppsCount.value,
                        onStartFocusSession = { startFocusSession() },
                        onConfigureApps = { configureApps() },
                        onConfigureSchedules = { configureSchedules() },
                        onEnableService = { enableService() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshFocusState()
    }

    private fun refreshFocusState() {
        isServiceEnabled.value = isAccessibilityServiceEnabled(AmnShieldAccessibilityService::class.java)
        val focusData = loader.getFocusModeData()
        isFocusModeActive.value = focusData.isTurnedOn
        focusModeEndTime.value = focusData.endTime
        allowedAppsCount.value = loader.getFocusModeSelectedApps().size
    }

    private fun startFocusSession() {
        if (!premiumManager.isPremium()) {
            Toast.makeText(requireContext(), "Focus Mode is a Premium Feature", Toast.LENGTH_SHORT).show()
            return
        }
        StartFocusMode(loader) {
            refreshFocusState()
        }.show(childFragmentManager, "start_focus_mode")
    }

    private fun configureApps() {
        if (!premiumManager.isPremium()) {
            Toast.makeText(requireContext(), "Focus Mode is a Premium Feature", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(requireContext(), SelectAppsActivity::class.java).apply {
            putStringArrayListExtra("PRE_SELECTED_APPS", ArrayList(loader.getFocusModeSelectedApps()))
        }
        selectAppsLauncher.launch(intent, activityOptions)
    }

    private fun configureSchedules() {
        if (!premiumManager.isPremium()) {
            Toast.makeText(requireContext(), "Focus Mode is a Premium Feature", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(requireContext(), FragmentActivity::class.java).apply {
            putExtra("fragment", ManageBlockSchedulesFragment.FRAGMENT_ID)
            putExtra("prefill_target", "FOCUS_MODE")
        }
        startActivity(intent, activityOptions.toBundle())
    }

    private fun enableService() {
        showAccessibilityInfoDialog("AmnShield Accessibility Service", AmnShieldAccessibilityService::class.java)
    }
}
