package com.alhaq.amnshield.ui.fragments.anti_uninstall

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
import com.alhaq.amnshield.Constants
import com.alhaq.amnshield.R
import com.alhaq.amnshield.databinding.FragmentSetupPasswordModeBinding
import com.alhaq.amnshield.databinding.DialogPermissionInfoBinding
import com.alhaq.amnshield.receivers.AdminReceiver
import com.alhaq.amnshield.services.AmnShieldAccessibilityService

class SetupPasswordModeFragment : Fragment() {

    private var _binding: FragmentSetupPasswordModeBinding? = null
    private val binding get() = _binding!!  // Safe getter for binding
    
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
            proceedWithSetup()
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
        _binding = FragmentSetupPasswordModeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnNextPass.setOnClickListener {
            // Validate password first
            val password = binding.password.text.toString()
            if (password.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.please_enter_a_password), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 4) {
                Toast.makeText(requireContext(), getString(R.string.password_must_be_at_least_4_characters), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Show warning dialog
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.alert))
                .setMessage(getString(R.string.are_you_sure_you_want_to_turn_on_anti_uninstall_there_is_no_turning_back))
                .setPositiveButton(getString(R.string.i_understand)) { _, _ ->
                    // Check if device admin is already active
                    if (isDeviceAdminActive()) {
                        proceedWithSetup()
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
    
    private fun proceedWithSetup() {
        setupPasswordMode()
    }

    private fun setupPasswordMode() {
        // Store the password as a salted SHA-256 hash via PasswordHasher (v2 format).
        // The raw password never touches disk; rooted/backup inspection cannot recover it.
        val hashed = com.alhaq.amnshield.utils.PasswordHasher.hash(
            binding.password.text.toString()
        )
        val editor =
            activity?.getSharedPreferences("anti_uninstall", Context.MODE_PRIVATE)?.edit()
        editor?.apply() {
            putBoolean("is_anti_uninstall_on", true)
            putString("password", hashed)
            putInt("mode", Constants.ANTI_UNINSTALL_PASSWORD_MODE)
            commit()
        }

        val intent = Intent(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_ANTI_UNINSTALL)
            .setPackage(activity?.packageName)
        activity?.sendBroadcast(intent)

        activity?.finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}