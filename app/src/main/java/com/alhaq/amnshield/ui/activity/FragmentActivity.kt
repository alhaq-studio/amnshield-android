package com.alhaq.amnshield.ui.activity

import android.content.Context
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.alhaq.amnshield.R
import com.alhaq.amnshield.ui.fragments.anti_uninstall.ChooseModeFragment
import com.alhaq.amnshield.ui.fragments.installation.AccessibilityGuide

import com.alhaq.amnshield.ui.fragments.installation.WelcomeFragment
import com.alhaq.amnshield.ui.fragments.installation.PermissionsFragment
import com.alhaq.amnshield.ui.fragments.features.PremiumFeaturesFragment
import com.alhaq.amnshield.ui.fragments.usage.AllAppsUsageFragment
import com.alhaq.amnshield.ui.fragments.BlocksManagerFragment
import com.alhaq.amnshield.ui.fragments.ManageLaunchLimitsFragment
import com.alhaq.amnshield.ui.fragments.ProfileFragment

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.alhaq.amnshield.AmnShield
import com.alhaq.amnshield.utils.SavedPreferencesLoader
import com.alhaq.amnshield.ui.theme.AmnShieldTheme
import com.alhaq.amnshield.ui.state.AppTheme
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.compose.ui.platform.ViewCompositionStrategy

class FragmentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        com.alhaq.amnshield.utils.ThemeUtils.applyTheme(this)

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
                "focus_mode" -> com.alhaq.amnshield.ui.fragments.FocusFragment()
                "app_blocker" -> com.alhaq.amnshield.ui.fragments.features.AppBlockerConfigFragment()
                // "view_blocker" was consolidated into Reel Blocker; route removed to
                // avoid resurrecting the deprecated config screen via legacy intents.
                "reel_blocker" -> com.alhaq.amnshield.ui.fragments.features.ReelBlockerConfigFragment()
                "website_blocker", "social_media_blocker" -> com.alhaq.amnshield.ui.fragments.features.WebsiteBlockerConfigFragment()
                "usage_tracker" -> com.alhaq.amnshield.ui.fragments.features.UsageTrackerConfigFragment()
                "keyword_blocker" -> com.alhaq.amnshield.ui.fragments.features.KeywordBlockerConfigFragment()
                "anti_uninstall" -> ChooseModeFragment()
                "setup_password_mode" -> com.alhaq.amnshield.ui.fragments.anti_uninstall.SetupPasswordModeFragment()
                "setup_timed_mode" -> com.alhaq.amnshield.ui.fragments.anti_uninstall.SetupTimedModeFragment()
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
                BlocksManagerFragment.FRAGMENT_ID -> {
                    fragment = BlocksManagerFragment()
                }
                ManageLaunchLimitsFragment.FRAGMENT_ID -> {
                    fragment = ManageLaunchLimitsFragment()
                }
                WelcomeFragment.FRAGMENT_ID -> {
                    fragment = WelcomeFragment()
                }

                AccessibilityGuide.FRAGMENT_ID ->
                    fragment = AccessibilityGuide()
                ProfileFragment.FRAGMENT_ID ->
                    fragment = ProfileFragment()
            }
            
            fragment?.arguments = Bundle().apply {
                intent.extras?.let { putAll(it) }
            }
        }
        
        if (fragment != null) {
            // Check if Bypass PIN Lock is enabled and needs verification
            val isBlockerConfig = when (featureType) {
                "focus_mode", "app_blocker", "reel_blocker", "social_media_blocker",
                "usage_tracker", "keyword_blocker", "anti_uninstall", "setup_password_mode",
                "setup_timed_mode" -> true
                else -> {
                    val fragId = intent.getStringExtra("fragment")
                    fragId == ChooseModeFragment.FRAGMENT_ID ||
                    fragId == BlocksManagerFragment.FRAGMENT_ID ||
                    fragId == ManageLaunchLimitsFragment.FRAGMENT_ID
                }
            }

            val loader = SavedPreferencesLoader(this)
            val pinEnabled = loader.isPinSecurityEnabled()
            val bypassEnabled = loader.isBypassPinLockEnabled()
            val antiUninstallPrefs = getSharedPreferences("anti_uninstall", Context.MODE_PRIVATE)
            val isAntiUninstallOn = antiUninstallPrefs.getBoolean("is_anti_uninstall_on", false)
            val pinCode = loader.getPinCode()

            val needsPin = isBlockerConfig && pinEnabled && bypassEnabled && isAntiUninstallOn && pinCode.isNotEmpty() && !AmnShield.isBypassUnlocked

            if (needsPin) {
                showBypassPinDialog(pinCode) {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_holder, fragment)
                        .commit()
                }
            } else {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_holder, fragment)
                    .commit()
            }
        }
    }

    private fun showBypassPinDialog(correctPinCode: String, onSuccess: () -> Unit) {
        val dialog = android.app.Dialog(this, android.R.style.Theme_Material_NoActionBar_Fullscreen)
        
        dialog.window?.let { window ->
            window.decorView.setViewTreeLifecycleOwner(this)
            window.decorView.setViewTreeViewModelStoreOwner(this)
            window.decorView.setViewTreeSavedStateRegistryOwner(this)
        }

        val composeView = androidx.compose.ui.platform.ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }
        
        composeView.setContent {
            var pinText by remember { mutableStateOf("") }
            var errorText by remember { mutableStateOf("") }
            
            AmnShieldTheme(appTheme = com.alhaq.amnshield.utils.ThemeUtils.resolveAppTheme(this@FragmentActivity)) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Settings Locked",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Enter your 4-digit PIN to modify blocker settings",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            repeat(4) { index ->
                                val hasChar = index < pinText.length
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (hasChar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                        )
                                )
                            }
                        }
                        
                        if (errorText.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = errorText,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(48.dp))
                        
                        val buttons = listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9"),
                            listOf("Cancel", "0", "Delete")
                        )
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            buttons.forEach { row ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                                ) {
                                    row.forEach { char ->
                                        Box(
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (char == "Cancel" || char == "Delete") Color.Transparent
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                )
                                                .clickable {
                                                    when (char) {
                                                        "Cancel" -> {
                                                            dialog.dismiss()
                                                            finish()
                                                        }
                                                        "Delete" -> {
                                                            if (pinText.isNotEmpty()) {
                                                                pinText = pinText.substring(0, pinText.length - 1)
                                                            }
                                                        }
                                                        else -> {
                                                            if (pinText.length < 4) {
                                                                pinText += char
                                                                errorText = ""
                                                                if (pinText.length == 4) {
                                                                    if (pinText == correctPinCode) {
                                                                        AmnShield.isBypassUnlocked = true
                                                                        dialog.dismiss()
                                                                        onSuccess()
                                                                    } else {
                                                                        pinText = ""
                                                                        errorText = "Incorrect PIN code"
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = char,
                                                style = if (char == "Cancel" || char == "Delete") MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = if (char == "Cancel") MaterialTheme.colorScheme.error else if (char == "Delete") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        dialog.setContentView(composeView)
        dialog.setCancelable(false)
        dialog.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK) {
                dialog.dismiss()
                finish()
                true
            } else {
                false
            }
        }
        dialog.show()
    }
}