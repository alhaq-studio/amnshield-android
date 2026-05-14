package com.alhaq.deenshield.ui.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.alhaq.deenshield.R
import com.alhaq.deenshield.databinding.DialogKeywordPackageBinding
import com.alhaq.deenshield.services.DeenShieldAccessibilityService
import com.alhaq.deenshield.utils.SavedPreferencesLoader

class TweakKeywordPack : BaseDialog() {

    private lateinit var sharedPreferences: SharedPreferences

    @SuppressLint("ApplySharedPref")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogManageKeywordPacks = DialogKeywordPackageBinding.inflate(layoutInflater)

        // Initialize SharedPreferences
        sharedPreferences =
            requireContext().getSharedPreferences("keyword_packs", Context.MODE_PRIVATE)
        
        // Get SavedPreferencesLoader from parent class or create new one
        val loader = savedPreferencesLoader ?: SavedPreferencesLoader(requireContext())

        // Load current preferences into dialog
        dialogManageKeywordPacks.cbAdultKeywords.isChecked =
            sharedPreferences.getBoolean("adult_blocker", false)

        // Build and show the dialog
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.manage_keyword_blockers))
            .setView(dialogManageKeywordPacks.root)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val wasAdultPackEnabled = sharedPreferences.getBoolean("adult_blocker", false)
                val isAdultPackEnabled = dialogManageKeywordPacks.cbAdultKeywords.isChecked
                
                // Get current custom keywords
                val currentKeywords = loader.loadBlockedKeywords().toMutableSet()
                
                if (isAdultPackEnabled && !wasAdultPackEnabled) {
                    // Pack was just enabled - add adult keywords to custom list
                    val adultKeywords = resources.getStringArray(R.array.adult_keywords)
                    currentKeywords.addAll(adultKeywords.map { it.trim().lowercase() })
                    loader.saveBlockedKeywords(currentKeywords)
                } else if (!isAdultPackEnabled && wasAdultPackEnabled) {
                    // Pack was just disabled - remove adult keywords from custom list
                    val adultKeywords = resources.getStringArray(R.array.adult_keywords)
                        .map { it.trim().lowercase() }
                        .toSet()
                    currentKeywords.removeAll(adultKeywords)
                    loader.saveBlockedKeywords(currentKeywords)
                }
                
                // Save pack state
                with(sharedPreferences.edit()) {
                    putBoolean("adult_blocker", isAdultPackEnabled)
                    commit()
                }

                // Send broadcast to refresh the KeywordBlockerService
                sendRefreshRequest(DeenShieldAccessibilityService.INTENT_ACTION_REFRESH_BLOCKED_KEYWORD_LIST)
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
    }

}
