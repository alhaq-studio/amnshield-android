package com.alhaq.deenshield.ui.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.alhaq.deenshield.R
import com.alhaq.deenshield.ui.fragments.anti_uninstall.ChooseModeFragment
import com.alhaq.deenshield.ui.fragments.installation.AccessibilityGuide
import com.alhaq.deenshield.ui.fragments.installation.WelcomeFragment
import com.alhaq.deenshield.ui.fragments.features.PremiumFeaturesFragment
import com.alhaq.deenshield.ui.fragments.usage.AllAppsUsageFragment
import com.alhaq.deenshield.ui.fragments.ProfileFragment

class FragmentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        com.alhaq.deenshield.utils.ThemeUtils.applyTheme(this)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_fragment)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        var fragment: Fragment? = null
        
        // Check for feature_type first (new navigation)
        val featureType = intent.getStringExtra("feature_type")
        if (featureType != null) {
            fragment = when (featureType) {
                "focus_mode" -> com.alhaq.deenshield.ui.fragments.features.FocusModeConfigFragment()
                "app_blocker" -> com.alhaq.deenshield.ui.fragments.features.AppBlockerConfigFragment()
                // "view_blocker" was consolidated into Reel Blocker; route removed to
                // avoid resurrecting the deprecated config screen via legacy intents.
                "reel_blocker" -> com.alhaq.deenshield.ui.fragments.features.ReelBlockerConfigFragment()
                "usage_tracker" -> com.alhaq.deenshield.ui.fragments.features.UsageTrackerConfigFragment()
                "keyword_blocker" -> com.alhaq.deenshield.ui.fragments.features.KeywordBlockerConfigFragment()
                "anti_uninstall" -> ChooseModeFragment()
                "setup_password_mode" -> com.alhaq.deenshield.ui.fragments.anti_uninstall.SetupPasswordModeFragment()
                "setup_timed_mode" -> com.alhaq.deenshield.ui.fragments.anti_uninstall.SetupTimedModeFragment()
                "additional_features" -> PremiumFeaturesFragment()
                "premium_features" -> PremiumFeaturesFragment()
                else -> null
            }
        }
        
        // Fallback to old fragment navigation
        if (fragment == null && intent.getStringExtra("fragment") != null) {
            when (intent.getStringExtra("fragment")) {
                ChooseModeFragment.FRAGMENT_ID -> {
                    fragment = ChooseModeFragment()
                }
                AllAppsUsageFragment.FRAGMENT_ID -> {
                    fragment = AllAppsUsageFragment()
                }
                WelcomeFragment.FRAGMENT_ID -> {
                    fragment = WelcomeFragment()
                }
                AccessibilityGuide.FRAGMENT_ID ->
                    fragment = AccessibilityGuide()
                ProfileFragment.FRAGMENT_ID ->
                    fragment = ProfileFragment()
            }
        }
        
        if (fragment != null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.fragment_holder,
                    fragment
                ) // Add or replace the fragment in the container
                .commit() // Commit the transaction
        }
    }
}