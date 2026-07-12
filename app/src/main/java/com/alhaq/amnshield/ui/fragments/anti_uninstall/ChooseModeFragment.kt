package com.alhaq.amnshield.ui.fragments.anti_uninstall

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.alhaq.amnshield.R
import com.alhaq.amnshield.databinding.FragmentChoseAntiUninstallModeBinding

class ChooseModeFragment : Fragment() {

    companion object {
        const val FRAGMENT_ID = "choose_anti_uninstall_mode"
        const val ARG_BLOCK_CHANGES_DEFAULT = "arg_block_changes_default"
    }
    private var _binding: FragmentChoseAntiUninstallModeBinding? = null
    private val binding get() = _binding!!  // Safe getter for binding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChoseAntiUninstallModeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cbBlockChangesMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.enable_block_changes_guard))
                    .setMessage(getString(R.string.if_you_enable_this_you_won_t_be_able_to_change_configurations_such_as_adding_blocked_apps_keywords_and_more))
                    .setPositiveButton(getString(R.string.i_understand), null)
                    .show()
            }
        }

        binding.btnNext.setOnClickListener {
            val defaultBlockChanges = binding.cbBlockChangesMode.isChecked
            when (binding.radioGroup.checkedRadioButtonId) {
                binding.passMode.id -> {
                    val fragment = SetupPasswordModeFragment().apply {
                        arguments = Bundle().apply {
                            putBoolean(ARG_BLOCK_CHANGES_DEFAULT, defaultBlockChanges)
                        }
                    }
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.fragment_holder,
                            fragment
                        ) // Replace with FragmentB
                        .addToBackStack(null)
                        .commit()
                }

                binding.timedMode.id -> {
                    val fragment = SetupTimedModeFragment().apply {
                        arguments = Bundle().apply {
                            putBoolean(ARG_BLOCK_CHANGES_DEFAULT, defaultBlockChanges)
                        }
                    }
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.fragment_holder,
                            fragment
                        ) // Replace with FragmentB
                        .addToBackStack(null)
                        .commit()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
