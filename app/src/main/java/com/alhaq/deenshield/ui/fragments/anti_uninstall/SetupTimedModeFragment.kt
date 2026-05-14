package com.alhaq.deenshield.ui.fragments.anti_uninstall

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.alhaq.deenshield.Constants
import com.alhaq.deenshield.R
import com.alhaq.deenshield.databinding.FragmentSetupTimedModeBinding
import com.alhaq.deenshield.databinding.DialogPermissionInfoBinding
import com.alhaq.deenshield.receivers.AdminReceiver
import com.alhaq.deenshield.services.DeenShieldAccessibilityService

class SetupTimedModeFragment : Fragment() {

    private var _binding: FragmentSetupTimedModeBinding? = null
    private val binding get() = _binding!!  // Safe getter for binding
    private var selectedDate: String? = null
    private var selectedUnlockAtMillis: Long? = null
    
    private val devicePolicyManager by lazy {
        requireContext().getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }
    
    private val adminComponent by lazy {
        ComponentName(requireContext(), AdminReceiver::class.java)
    }
    
    // Launcher for device admin permission request
    private val deviceAdminLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (isDeviceAdminActive()) {
            selectedDate?.let { date ->
                selectedUnlockAtMillis?.let { unlockAt ->
                    proceedWithSetup(date, unlockAt)
                }
            }
        } else {
            Toast.makeText(
                requireContext(),
                getString(R.string.device_admin_required),
                Toast.LENGTH_LONG
            ).show()
            requireActivity().finish()
        }
    }
    
    private fun isDeviceAdminActive(): Boolean {
        return devicePolicyManager.isAdminActive(adminComponent)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetupTimedModeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val defaultBlockChanges = arguments?.getBoolean(ChooseModeFragment.ARG_BLOCK_CHANGES_DEFAULT, false) == true
        binding.blockChanges.isChecked = defaultBlockChanges
        binding.calendarView.minDate = binding.calendarView.date
        
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate = "${month + 1}/$dayOfMonth/$year"
            val lockUntil = java.util.Calendar.getInstance().apply {
                set(year, month, dayOfMonth, 23, 59, 59)
                set(java.util.Calendar.MILLISECOND, 999)
            }
            selectedUnlockAtMillis = lockUntil.timeInMillis
        }
        
        binding.turnOnTimed.setOnClickListener {
            // Validate date selection
            if (selectedDate == null || selectedUnlockAtMillis == null) {
                Toast.makeText(requireContext(), getString(R.string.please_select_a_date), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.alert))
                .setMessage(getString(R.string.are_you_sure_you_want_to_turn_on_anti_uninstall_there_is_no_turning_back))
                .setPositiveButton(getString(R.string.i_understand)) { _, _ ->
                    // Check if device admin is already active
                    if (isDeviceAdminActive()) {
                        selectedDate?.let { date ->
                            selectedUnlockAtMillis?.let { unlockAt ->
                                proceedWithSetup(date, unlockAt)
                            }
                        }
                    } else {
                        // Show device admin request dialog
                        showDeviceAdminDialog()
                    }
                }
                .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                    requireActivity().finish()
                }
                .show()
        }
        binding.blockChanges.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.alert))
                    .setMessage(getString(R.string.if_you_enable_this_you_won_t_be_able_to_change_configurations_such_as_adding_blocked_apps_keywords_and_more))
                    .setPositiveButton(getString(R.string.i_understand), null)
                    .show()
            }
        }

    }

    private fun showDeviceAdminDialog() {
        val dialogBinding = DialogPermissionInfoBinding.inflate(layoutInflater)
        dialogBinding.title.text = getString(R.string.enable_2, getString(R.string.anti_uninstall))
        dialogBinding.desc.text = getString(R.string.device_admin_perm)
        dialogBinding.point1.text = getString(R.string.prevent_uninstallation_attempts_until_a_set_condition_is_met)
        dialogBinding.point2.visibility = View.GONE
        dialogBinding.point3.visibility = View.GONE
        dialogBinding.point4.visibility = View.GONE
        dialogBinding.btnGuide.visibility = View.GONE

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        dialogBinding.btnReject.setOnClickListener {
            dialog.dismiss()
            requireActivity().finish()
        }
        
        dialogBinding.btnAccept.setOnClickListener {
            dialog.dismiss()
            requestDeviceAdmin()
        }

        dialog.show()
    }
    
    private fun requestDeviceAdmin() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                getString(R.string.device_admin_perm)
            )
        }
        deviceAdminLauncher.launch(intent)
    }
    
    private fun proceedWithSetup(date: String, unlockAtMillis: Long) {
        turnOnTimedMode(date, unlockAtMillis)
    }

    private fun turnOnTimedMode(selectedDate: String, unlockAtMillis: Long) {

        val editor =
            activity?.getSharedPreferences("anti_uninstall", Context.MODE_PRIVATE)?.edit()
        editor?.apply() {
            putBoolean("is_anti_uninstall_on", true)
            putString("date", selectedDate)
            putLong("unlock_at_millis", unlockAtMillis)
            putBoolean("is_configuring_blocked", binding.blockChanges.isChecked)
            putInt("mode", Constants.ANTI_UNINSTALL_TIMED_MODE)
            commit()
        }

        val intent = Intent(DeenShieldAccessibilityService.INTENT_ACTION_REFRESH_ANTI_UNINSTALL)
            .setPackage(activity?.packageName)
        activity?.sendBroadcast(intent)

        activity?.finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}