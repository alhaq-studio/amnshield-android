package com.alhaq.amnshield.ui.fragments.installation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity.POWER_SERVICE
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.alhaq.amnshield.R
import com.alhaq.amnshield.databinding.FragmentPermissionsBinding
import com.alhaq.amnshield.utils.PermissionGuideHelper
import com.alhaq.amnshield.utils.ZipUtils
import com.alhaq.amnshield.utils.ZipUtils.unzipSharedPreferencesFromUri

class PermissionsFragment : Fragment() {

    companion object {
        const val FRAGMENT_ID = "permission_fragment"
    }

    private var _binding: FragmentPermissionsBinding? = null
    private val binding get() = _binding!!  // Safe getter for binding

    private lateinit var permissionGuideHelper: PermissionGuideHelper

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            setPermissionIconState(isGranted, binding.notifPermIcon)
            updateNextButtonState()
        }

    private val batteryOptimizationLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            setPermissionIconState(isBackgroundPermissionGiven(), binding.bgPermIcon)
            updateNextButtonState()
        }

    private val accessibilityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            setPermissionIconState(isAccessibilityPermissionGiven(), binding.accessPermIcon)
            updateNextButtonState()
        }

    private val restorePicker: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            result.data?.data?.let { uri ->
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                activity?.contentResolver?.takePersistableUriPermission(uri, takeFlags)
                unzipSharedPreferencesFromUri(requireContext(), uri)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPermissionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("BatteryLife")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        permissionGuideHelper = PermissionGuideHelper(requireActivity())

        binding.btnNext.setOnClickListener {
            val sharedPreferences =
                requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
            sharedPreferences.edit().putBoolean("isFirstLaunchComplete", true).apply()

            requireActivity().supportFragmentManager.beginTransaction()
                .replace(
                    R.id.fragment_holder,
                    AccessibilityGuide()
                )
                .addToBackStack(null)
                .commit()
        }

        binding.notifPermRoot.setOnClickListener {
            if (isNotificationPermissionGiven()) return@setOnClickListener
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        binding.bgPermRoot.setOnClickListener {
            if (isBackgroundPermissionGiven()) return@setOnClickListener
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${requireContext().packageName}")
            }
            batteryOptimizationLauncher.launch(intent)
        }

        binding.accessPermRoot.setOnClickListener {
            if (isAccessibilityPermissionGiven()) return@setOnClickListener
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                accessibilityLauncher.launch(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            }
        }

        binding.restoreRoot.setOnClickListener {
            ZipUtils.showRestorePicker(restorePicker)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun refreshPermissions() {
        val isBgOk = isBackgroundPermissionGiven()
        val isNotifOk = isNotificationPermissionGiven()
        val isAccessOk = isAccessibilityPermissionGiven()

        setPermissionIconState(isBgOk, binding.bgPermIcon)
        setPermissionIconState(isNotifOk, binding.notifPermIcon)
        setPermissionIconState(isAccessOk, binding.accessPermIcon)

        updateNextButtonState()
    }

    private fun updateNextButtonState() {
        val isBgOk = isBackgroundPermissionGiven()
        val isAccessOk = isAccessibilityPermissionGiven()
        // Enabled next button if background and accessibility service permissions are granted
        binding.btnNext.isEnabled = isBgOk && isAccessOk
    }

    private fun setPermissionIconState(isEnabled: Boolean, icon: ImageView) {
        if (isEnabled) {
            icon.setImageResource(R.drawable.baseline_done_24)
            icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface))
        } else {
            icon.setImageResource(R.drawable.baseline_close_24)
            icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.error_color))
        }
    }

    private fun isBackgroundPermissionGiven(): Boolean {
        val powerManager =
            requireContext().getSystemService(POWER_SERVICE) as PowerManager
        val packageName = requireContext().packageName
        return powerManager.isIgnoringBatteryOptimizations(packageName)
    }

    private fun isNotificationPermissionGiven(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private fun isAccessibilityPermissionGiven(): Boolean {
        if (!::permissionGuideHelper.isInitialized) {
            permissionGuideHelper = PermissionGuideHelper(requireActivity())
        }
        return permissionGuideHelper.isAccessibilityEnabled(com.alhaq.amnshield.services.AmnShieldAccessibilityService::class.java)
    }
}