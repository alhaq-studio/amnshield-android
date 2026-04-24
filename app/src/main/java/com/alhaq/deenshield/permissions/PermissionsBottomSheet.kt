package com.alhaq.deenshield.permissions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.alhaq.deenshield.R
import com.alhaq.deenshield.databinding.BottomSheetPermissionsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PermissionsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetPermissionsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: PermissionsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetPermissionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = PermissionsViewModel(requireContext())

        viewModel.permissionsState.observe(viewLifecycleOwner) { state ->
            binding.accessibilityPermission.isGranted = state.isAccessibilityEnabled
            binding.deviceAdminPermission.isGranted = state.isDeviceAdminEnabled
            binding.overlayPermission.isGranted = state.isDrawOverOtherAppsEnabled
            binding.usageStatsPermission.isGranted = state.isUsageStatsEnabled
            binding.notificationPermission.isGranted = state.areNotificationsEnabled
        }

        binding.accessibilityPermission.setOnClickListener {
            startActivity(viewModel.permissionsManager.getAccessibilityServiceIntent())
        }
        binding.deviceAdminPermission.setOnClickListener {
            startActivity(viewModel.permissionsManager.getDeviceAdminIntent())
        }
        binding.overlayPermission.setOnClickListener {
            startActivity(viewModel.permissionsManager.getDrawOverOtherAppsIntent())
        }
        binding.usageStatsPermission.setOnClickListener {
            startActivity(viewModel.permissionsManager.getUsageStatsIntent())
        }
        binding.notificationPermission.setOnClickListener {
            startActivity(viewModel.permissionsManager.getNotificationPermissionIntent())
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermissions()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "PermissionsBottomSheet"
    }
}