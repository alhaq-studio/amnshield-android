package com.alhaq.deenshield.ui.fragments.features

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
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.alhaq.deenshield.R
import com.alhaq.deenshield.databinding.DialogPermissionInfoBinding
import com.alhaq.deenshield.receivers.AdminReceiver
import com.alhaq.deenshield.ui.activity.FragmentActivity
import com.alhaq.deenshield.ui.fragments.installation.AccessibilityGuide
import com.alhaq.deenshield.utils.SavedPreferencesLoader

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
                getString(R.string.find_deenshield_and_press_enable),
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
                getString(R.string.find_deenshield_and_press_enable),
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
}
