package com.alhaq.amnshield.ui.fragments.features

import android.accessibilityservice.AccessibilityService
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.alhaq.amnshield.R
import com.alhaq.amnshield.databinding.DialogPermissionInfoBinding
import com.alhaq.amnshield.receivers.AdminReceiver
import com.alhaq.amnshield.ui.activity.FragmentActivity
import com.alhaq.amnshield.ui.fragments.installation.AccessibilityGuide
import com.alhaq.amnshield.utils.SavedPreferencesLoader
import com.alhaq.amnshield.utils.ScheduleUtils

/**
 * Base feature fragment that provides shared helpers for the individual feature screens.
 */
abstract class BaseFeatureFragment : Fragment() {

    protected val savedPreferencesLoader: SavedPreferencesLoader by lazy {
        SavedPreferencesLoader(requireContext())
    }

    protected val activityOptions: ActivityOptionsCompat by lazy {
        ActivityOptionsCompat.makeCustomAnimation(
            requireContext(),
            R.anim.fade_in,
            R.anim.fade_out
        )
    }

    protected fun isAccessibilityServiceEnabled(serviceClass: Class<out AccessibilityService>): Boolean {
        val componentName = ComponentName(requireContext(), serviceClass)
        val enabledServices = Settings.Secure.getString(
            requireContext().contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        val accessibilityEnabled = Settings.Secure.getInt(
            requireContext().contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0
        )
        if (accessibilityEnabled != 1) return false

        // OEM ROMs may serialize enabled services with short/full class names.
        // Normalize and compare against both full and short component forms.
        val expected = setOf(
            componentName.flattenToString().lowercase(),
            componentName.flattenToShortString().lowercase(),
            "${componentName.packageName}/${componentName.className}".lowercase(),
            "${componentName.packageName}/${componentName.shortClassName}".lowercase()
        )

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        for (entry in splitter) {
            val normalized = entry.trim().lowercase()
            if (normalized in expected) return true
        }

        return false
    }

    protected fun sendRefreshRequest(action: String) {
        // Restrict broadcast to our own process so a non-exported receiver still
        // receives it on API 33+ and external apps cannot intercept refresh signals.
        val ctx = requireContext()
        ctx.sendBroadcast(Intent(action).setPackage(ctx.packageName))
    }

    protected fun showAccessibilityInfoDialog(title: String, serviceClass: Class<*>) {
        val dialogBinding = DialogPermissionInfoBinding.inflate(layoutInflater)
    dialogBinding.title.text = getString(R.string.enable_2, title)
    dialogBinding.desc.text = getString(R.string.accessibility_perm_desc)
    dialogBinding.point1.text = getString(R.string.perform_actions_like_a_back_press)
    dialogBinding.point2.text = getString(R.string.read_content_on_screen)
    dialogBinding.point3.visibility = View.GONE
    dialogBinding.point4.visibility = View.GONE
        dialogBinding.btnGuide.visibility = View.VISIBLE

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnReject.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnAccept.setOnClickListener {
            Toast.makeText(
                requireContext(),
                getString(R.string.find_amnshield_and_press_enable),
                Toast.LENGTH_LONG
            ).show()
            openAccessibilityServiceScreen(serviceClass)
            dialog.dismiss()
        }
        dialogBinding.btnGuide.setOnClickListener {
            val intent = Intent(requireContext(), FragmentActivity::class.java).apply {
                putExtra("fragment", AccessibilityGuide.FRAGMENT_ID)
            }
            startActivity(intent, activityOptions.toBundle())
            dialog.dismiss()
        }

        dialog.show()
    }

    protected fun showDrawOverOtherAppsDialog(onAccepted: () -> Unit = {}) {
        val dialogBinding = DialogPermissionInfoBinding.inflate(layoutInflater)
    dialogBinding.title.text = getString(R.string.enable_2, getString(R.string.display_time_elapsed))
        dialogBinding.desc.text = getString(R.string.device_perm_draw_over_other_apps)
        dialogBinding.point1.text = getString(R.string.show_time_elapsed_on_phone)
    dialogBinding.point2.text = getString(R.string.calculate_how_many_reels_tiktok_short_videos_you_scroll_per_day)
    dialogBinding.point3.visibility = View.GONE
    dialogBinding.point4.text = getString(R.string.plan_a_robbery)
        dialogBinding.btnGuide.visibility = View.GONE

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnReject.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnAccept.setOnClickListener {
            Toast.makeText(
                requireContext(),
                getString(R.string.find_amnshield_and_press_enable),
                Toast.LENGTH_LONG
            ).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${requireContext().packageName}")
            )
            startActivity(intent, activityOptions.toBundle())
            dialog.dismiss()
            onAccepted()
        }

        dialog.show()
    }

    protected fun showDeviceAdminDialog(onAccepted: () -> Unit) {
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
            .create()

        dialogBinding.btnReject.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnAccept.setOnClickListener {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                val component = ComponentName(requireContext(), AdminReceiver::class.java)
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    getString(R.string.device_admin_perm)
                )
            }
            startActivity(intent, activityOptions.toBundle())
            dialog.dismiss()
            onAccepted()
        }

        dialog.show()
    }

    private fun openAccessibilityServiceScreen(cls: Class<*>) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                val componentName = ComponentName(requireContext(), cls)
                putExtra(":settings:fragment_args_key", componentName.flattenToString())
                val bundle = Bundle().apply {
                    putString(":settings:fragment_args_key", componentName.flattenToString())
                }
                putExtra(":settings:show_fragment_args", bundle)
            }
            startActivity(intent, activityOptions.toBundle())
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    protected fun showFeatureScheduleDialog(
        featureName: String,
        onConfirm: (startTime: String, endTime: String, days: List<String>) -> Unit,
        onCancel: () -> Unit
    ) {
        var startTime = "09:00"
        var endTime = "17:00"
        val selectedDays = mutableSetOf("Mon", "Tue", "Wed", "Thu", "Fri")

        val context = requireContext()
        val dialogView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 36, 48, 36)
        }

        val titleText = TextView(context).apply {
            text = "Set Blocker Time Window"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 24)
        }
        dialogView.addView(titleText)

        val subtitleText = TextView(context).apply {
            text = "This blocker will only apply during the selected times."
            textSize = 14f
            setPadding(0, 0, 0, 36)
        }
        dialogView.addView(subtitleText)

        val startTimeRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 24)
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        val startTimeLabel = TextView(context).apply {
            text = "Start Time: "
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val startTimeBtn = MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = startTime
            setOnClickListener {
                val parts = startTime.split(":")
                val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 9
                val initialMin = parts.getOrNull(1)?.toIntOrNull() ?: 0
                val picker = MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(initialHour)
                    .setMinute(initialMin)
                    .setTitleText("Select Start Time")
                    .build()
                picker.addOnPositiveButtonClickListener {
                    startTime = String.format("%02d:%02d", picker.hour, picker.minute)
                    text = startTime
                }
                picker.show(childFragmentManager, "start_time_picker")
            }
        }
        startTimeRow.addView(startTimeLabel)
        startTimeRow.addView(startTimeBtn)
        dialogView.addView(startTimeRow)

        val endTimeRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 36)
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        val endTimeLabel = TextView(context).apply {
            text = "End Time: "
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val endTimeBtn = MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = endTime
            setOnClickListener {
                val parts = endTime.split(":")
                val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 17
                val initialMin = parts.getOrNull(1)?.toIntOrNull() ?: 0
                val picker = MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(initialHour)
                    .setMinute(initialMin)
                    .setTitleText("Select End Time")
                    .build()
                picker.addOnPositiveButtonClickListener {
                    endTime = String.format("%02d:%02d", picker.hour, picker.minute)
                    text = endTime
                }
                picker.show(childFragmentManager, "end_time_picker")
            }
        }
        endTimeRow.addView(endTimeLabel)
        endTimeRow.addView(endTimeBtn)
        dialogView.addView(endTimeRow)

        val daysLabel = TextView(context).apply {
            text = "Select Days:"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 16)
        }
        dialogView.addView(daysLabel)

        val daysContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 24)
        }
        val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        daysOfWeek.forEach { day ->
            val cb = MaterialCheckBox(context).apply {
                text = day
                isChecked = selectedDays.contains(day)
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) selectedDays.add(day) else selectedDays.remove(day)
                }
            }
            daysContainer.addView(cb)
        }
        dialogView.addView(daysContainer)

        val dialog = MaterialAlertDialogBuilder(context)
            .setView(dialogView)
            .setPositiveButton("Apply") { _, _ ->
                if (selectedDays.isEmpty()) {
                    Toast.makeText(context, "Please select at least one day", Toast.LENGTH_SHORT).show()
                    onCancel()
                } else {
                    onConfirm(startTime, endTime, selectedDays.toList())
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                onCancel()
            }
            .setOnCancelListener {
                onCancel()
            }
            .create()

        dialog.show()
    }
}
