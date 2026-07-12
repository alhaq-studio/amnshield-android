package com.alhaq.deenshield.ui.dialogs

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.alhaq.deenshield.R
import com.alhaq.deenshield.data.blockers.AppLaunchLimitRule
import com.alhaq.deenshield.databinding.DialogSetLaunchLimitBinding
import com.alhaq.deenshield.utils.SavedPreferencesLoader
import java.util.UUID

/**
 * Dialog for setting per-app launch/opens limits.
 * User selects:
 * - Max launches (1-50)
 * - Time period (hourly, daily, weekly)
 * 
 * When saved, creates an AppLaunchLimitRule and optionally triggers auto-block when limit is hit.
 */
class SetLaunchLimitDialog(
    private val packageName: String,
    private val appName: String,
    private val onSave: (rule: AppLaunchLimitRule?) -> Unit
) : DialogFragment() {

    private var _binding: DialogSetLaunchLimitBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogSetLaunchLimitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val savedPrefs = SavedPreferencesLoader(requireContext())
        val existingLimit = savedPrefs.getAppLaunchLimitRule(packageName)

        // Set title
        binding.dialogTitle.text = "Set Launch Limit: $appName"

        // Setup max launches slider (1-50)
        binding.maxLaunchesSlider.min = 1
        binding.maxLaunchesSlider.max = 50
        binding.maxLaunchesSlider.progress = existingLimit?.maxLaunches ?: 10

        updateLaunchCountDisplay()
        binding.maxLaunchesSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateLaunchCountDisplay()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Setup time period radio buttons
        val period = existingLimit?.timePeriod ?: AppLaunchLimitRule.TimePeriod.DAILY
        when (period) {
            AppLaunchLimitRule.TimePeriod.HOURLY -> binding.radioPeriodHourly.isChecked = true
            AppLaunchLimitRule.TimePeriod.DAILY -> binding.radioPeriodDaily.isChecked = true
            AppLaunchLimitRule.TimePeriod.WEEKLY -> binding.radioPeriodWeekly.isChecked = true
        }

        // Setup action buttons
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            val maxLaunches = binding.maxLaunchesSlider.progress
            val period = when (binding.periodGroup.checkedRadioButtonId) {
                binding.radioPeriodHourly.id -> AppLaunchLimitRule.TimePeriod.HOURLY
                binding.radioPeriodDaily.id -> AppLaunchLimitRule.TimePeriod.DAILY
                binding.radioPeriodWeekly.id -> AppLaunchLimitRule.TimePeriod.WEEKLY
                else -> AppLaunchLimitRule.TimePeriod.DAILY
            }

            val rule = AppLaunchLimitRule(
                id = existingLimit?.id ?: UUID.randomUUID().toString(),
                packageName = packageName,
                maxLaunches = maxLaunches,
                timePeriod = period,
                createdAt = existingLimit?.createdAt ?: System.currentTimeMillis()
            )

            onSave(rule)
            dismiss()
        }

        binding.btnDelete.visibility = if (existingLimit != null) View.VISIBLE else View.GONE
        binding.btnDelete.setOnClickListener {
            savedPrefs.removeAppLaunchLimitRule(packageName)
            onSave(null)
            dismiss()
        }
    }

    private fun updateLaunchCountDisplay() {
        val count = binding.maxLaunchesSlider.progress
        binding.launchCountDisplay.text = "$count launch${if (count != 1) "es" else ""}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
