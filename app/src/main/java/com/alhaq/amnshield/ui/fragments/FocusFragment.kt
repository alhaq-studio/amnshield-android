package com.alhaq.amnshield.ui.fragments

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.ComposeView
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.alhaq.amnshield.premium.PremiumManager
import com.alhaq.amnshield.services.AmnShieldAccessibilityService
import com.alhaq.amnshield.ui.activity.FragmentActivity
import com.alhaq.amnshield.ui.activity.SelectAppsActivity
import com.alhaq.amnshield.ui.fragments.features.BaseFeatureFragment
import com.alhaq.amnshield.ui.dialogs.StartFocusMode
import com.alhaq.amnshield.ui.screens.FocusScreen
import com.alhaq.amnshield.ui.screens.FocusAppItem
import com.alhaq.amnshield.ui.theme.AmnShieldTheme
import com.alhaq.amnshield.ui.state.AppTheme
import com.alhaq.amnshield.utils.SavedPreferencesLoader
import com.alhaq.amnshield.blockers.FocusModeBlocker
import com.alhaq.amnshield.utils.NotificationTimerManager
import androidx.lifecycle.ViewModelProvider
import com.alhaq.amnshield.ui.viewmodel.AmnShieldViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FocusFragment : BaseFeatureFragment() {

    private val premiumManager by lazy { PremiumManager.getInstance(requireContext().applicationContext) }
    private val loader by lazy { SavedPreferencesLoader(requireContext()) }
    private lateinit var viewModel: AmnShieldViewModel

    private val isServiceEnabled = mutableStateOf(false)
    private val isFocusModeActive = mutableStateOf(false)
    private val focusModeEndTime = mutableStateOf(0L)
    private val allowedAppsCount = mutableStateOf(0)

    private val installedAppsList = mutableStateListOf<FocusAppItem>()
    private val preSelectedApps = mutableStateOf<Set<String>>(emptySet())
    private val defaultMode = mutableStateOf(1)

    private data class PendingSession(
        val durationMinutes: Int,
        val mode: Int,
        val selectedApps: Set<String>
    )
    private var pendingSession: PendingSession? = null

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingSession?.let {
                startFocusSession(it.durationMinutes, it.mode, it.selectedApps)
            }
        } else {
            Toast.makeText(requireContext(), "Notification permission is required for focus timers.", Toast.LENGTH_SHORT).show()
        }
        pendingSession = null
    }

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
                        installedApps = installedAppsList,
                        preSelectedApps = preSelectedApps.value,
                        defaultMode = defaultMode.value,
                        onStartFocusSession = { durationMinutes, mode, selectedApps ->
                            checkNotificationPermissionAndStart(durationMinutes, mode, selectedApps)
                        },
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
        loadInstalledApps()
    }

    private fun refreshFocusState() {
        isServiceEnabled.value = isAccessibilityServiceEnabled(AmnShieldAccessibilityService::class.java)
        val focusData = loader.getFocusModeData()
        isFocusModeActive.value = focusData.isTurnedOn
        focusModeEndTime.value = focusData.endTime
        allowedAppsCount.value = loader.getFocusModeSelectedApps().size
        preSelectedApps.value = loader.getFocusModeSelectedApps().toSet()
        defaultMode.value = focusData.modeType
    }

    private fun checkNotificationPermissionAndStart(durationMinutes: Int, mode: Int, selectedApps: Set<String>) {
        if (!premiumManager.isPremium()) {
            Toast.makeText(requireContext(), "Focus Mode is a Premium Feature", Toast.LENGTH_SHORT).show()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                pendingSession = PendingSession(durationMinutes, mode, selectedApps)
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        startFocusSession(durationMinutes, mode, selectedApps)
    }

    private fun startFocusSession(durationMinutes: Int, mode: Int, selectedApps: Set<String>) {
        val durationMillis = durationMinutes * 60_000L
        val startTime = System.currentTimeMillis()
        val endTime = startTime + durationMillis
        
        loader.saveFocusModeSelectedApps(selectedApps.toList())
        loader.saveFocusModeData(
            FocusModeBlocker.FocusModeData(
                isTurnedOn = true,
                endTime = endTime,
                modeType = mode,
                selectedApps = HashSet(selectedApps)
            )
        )
        loader.saveFocusSessionStartTime(startTime, endTime)
        
        val intent = Intent(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_FOCUS_MODE).apply {
            setPackage(requireContext().packageName)
        }
        requireContext().sendBroadcast(intent)
        
        val timer = NotificationTimerManager(requireContext())
        timer.startTimer(durationMillis)
        
        refreshFocusState()
    }

    private fun loadInstalledApps() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val pm = requireContext().packageManager
                val launcherApps = requireContext().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                val profiles = launcherApps.profiles
                val appList = mutableListOf<FocusAppItem>()
                val installedPackages = mutableSetOf<String>()

                for (profile in profiles) {
                    val apps = launcherApps.getActivityList(null, profile)
                        .map { it.applicationInfo }
                        .filter { it.packageName != requireContext().packageName }
                    
                    apps.forEach { appInfo ->
                        if (!installedPackages.contains(appInfo.packageName)) {
                            installedPackages.add(appInfo.packageName)
                            val appLabel = appInfo.loadLabel(pm).toString()
                            val bitmap = getAppIconBitmap(requireContext(), appInfo.packageName)
                            appList.add(FocusAppItem(appInfo.packageName, appLabel, bitmap))
                        }
                    }
                }
                appList.sortBy { it.label.lowercase() }
                withContext(Dispatchers.Main) {
                    installedAppsList.clear()
                    installedAppsList.addAll(appList)
                }
            } catch (e: Exception) {
                android.util.Log.e("FocusFragment", "Error loading installed apps", e)
            }
        }
    }

    private fun getAppIconBitmap(context: Context, packageName: String): android.graphics.Bitmap? {
        return try {
            val pm = context.packageManager
            val iconDrawable = pm.getApplicationIcon(packageName)
            val size = 96
            val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            iconDrawable.setBounds(0, 0, size, size)
            iconDrawable.draw(canvas)
            bitmap
        } catch (e: Exception) {
            null
        }
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
