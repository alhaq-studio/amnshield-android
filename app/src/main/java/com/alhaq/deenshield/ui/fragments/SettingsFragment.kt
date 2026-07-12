package com.alhaq.deenshield.ui.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityOptionsCompat
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.alhaq.deenshield.Constants
import com.alhaq.deenshield.R
import com.alhaq.deenshield.databinding.FragmentSettingsBinding
import com.alhaq.deenshield.ui.activity.MainActivity
import com.alhaq.deenshield.ui.activity.FragmentActivity
import com.alhaq.deenshield.ui.activity.RemindersActivity
import com.alhaq.deenshield.services.DeenShieldAccessibilityService
import com.alhaq.deenshield.premium.PremiumManager
import com.alhaq.deenshield.utils.SavedPreferencesLoader
import com.alhaq.deenshield.utils.ZipUtils
import java.util.Locale

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val premiumManager by lazy { PremiumManager.getInstance(requireContext().applicationContext) }
    private val savedPreferencesLoader by lazy { SavedPreferencesLoader(requireContext().applicationContext) }
    private var suppressFeatureSwitchChange = false


    // Backup launcher
    private val backupDirLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.data?.let { uri ->
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                requireContext().contentResolver.takePersistableUriPermission(uri, takeFlags)
                
                // Create backup file in selected directory
                val fileName = ZipUtils.createZipFileName()
                val documentUri = requireContext().contentResolver.run {
                    val docUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(
                        uri,
                        android.provider.DocumentsContract.getTreeDocumentId(uri)!!
                    )
                    android.provider.DocumentsContract.createDocument(
                        this,
                        docUri,
                        "application/zip",
                        fileName
                    )
                }
                
                documentUri?.let { zipUri ->
                    ZipUtils.zipSharedPreferencesToUri(requireContext(), zipUri)
                    Toast.makeText(requireContext(), "Backup saved: $fileName", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Backup failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Restore launcher
    private val restoreFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.data?.let { uri ->
            try {
                ZipUtils.unzipSharedPreferencesFromUri(requireContext(), uri)
                Toast.makeText(requireContext(), "Restore complete! Restart app to apply changes.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Restore failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadPreferences()
        setupClickListeners()
    }

    private fun loadPreferences() {
        context?.let { ctx ->
            val prefs = ctx.getSharedPreferences("theme_prefs", android.content.Context.MODE_PRIVATE)
            val themeMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            val themeStyle = prefs.getString("theme_style", "default")

            binding.txtThemeStatus.text = when {
                themeStyle == "gradient" -> getString(R.string.modern_gradient)
                themeStyle == "purple" -> getString(R.string.purple_gradient)
                themeStyle == "emerald" -> getString(R.string.emerald_theme)
                themeStyle == "sunset" -> getString(R.string.sunset_glow)
                themeMode == AppCompatDelegate.MODE_NIGHT_NO -> getString(R.string.light_mode)
                themeMode == AppCompatDelegate.MODE_NIGHT_YES -> getString(R.string.dark_mode)
                else -> getString(R.string.system_default)
            }

            try {
                val versionName = ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName
                binding.txtVersion.text = "Version $versionName"
            } catch (e: Exception) {
                binding.txtVersion.text = getString(R.string.app_version)
            }

            suppressFeatureSwitchChange = true

            val viewBlockerPrefs = requireContext().getSharedPreferences("view_blocker", android.content.Context.MODE_PRIVATE)
            binding.switchReelBlockerQuick.isChecked = savedPreferencesLoader.isReelBlockerEnabled(
                viewBlockerPrefs.getBoolean("is_enabled", false)
            )
            binding.switchKeywordBlockerQuick.isChecked = savedPreferencesLoader.isKeywordBlockerFeatureEnabled()
            binding.switchUsageTrackerQuick.isChecked = savedPreferencesLoader.isUsageTrackerFeatureEnabled()

            suppressFeatureSwitchChange = false

            updateFeatureStatuses()

            // Load language preference
            updateLanguageStatus()
        }
    }

    private fun setupClickListeners() {
        // Theme selection
        binding.themeOption.setOnClickListener { showThemeSelectionDialog() }

        // Low-risk quick toggles
        binding.switchReelBlockerQuick.setOnCheckedChangeListener { _, isChecked ->
            if (suppressFeatureSwitchChange) return@setOnCheckedChangeListener
            if (!premiumManager.isPremium()) {
                revertSwitch(binding.switchReelBlockerQuick, false)
                showPremiumUpsell()
                return@setOnCheckedChangeListener
            }
            handleReelBlockerToggle(isChecked)
        }

        binding.switchKeywordBlockerQuick.setOnCheckedChangeListener { _, isChecked ->
            if (suppressFeatureSwitchChange) return@setOnCheckedChangeListener
            savedPreferencesLoader.setKeywordBlockerFeatureEnabled(isChecked)
            sendRefreshRequest(DeenShieldAccessibilityService.INTENT_ACTION_REFRESH_BLOCKED_KEYWORD_LIST)
            updateFeatureStatuses()
        }

        binding.switchUsageTrackerQuick.setOnCheckedChangeListener { _, isChecked ->
            if (suppressFeatureSwitchChange) return@setOnCheckedChangeListener
            savedPreferencesLoader.setUsageTrackerFeatureEnabled(isChecked)
            updateFeatureStatuses()
        }

        // High-risk status rows open full config screens
        binding.appBlockerOption.setOnClickListener {
            openFeatureConfig("app_blocker", requiresPremium = true)
        }

        binding.focusModeOption.setOnClickListener {
            openFeatureConfig("focus_mode", requiresPremium = true)
        }

        binding.antiUninstallOption.setOnClickListener {
            openFeatureConfig("anti_uninstall", requiresPremium = true)
        }

        binding.backupOption.setOnClickListener {
            showBackupRestoreDialog()
        }

        binding.remindersOption.setOnClickListener {
            val intent = Intent(requireContext(), RemindersActivity::class.java)
            val options = ActivityOptionsCompat.makeCustomAnimation(
                requireContext(),
                R.anim.fade_in,
                R.anim.fade_out
            )
            startActivity(intent, options.toBundle())
        }

        binding.shareErrorsOption.setOnClickListener {
            shareCrashLogs()
        }

        binding.helpFaqOption.setOnClickListener {
            showFAQDialog()
        }

        binding.aboutOption.setOnClickListener {
            showAboutDialog()
        }

        binding.languageOption.setOnClickListener {
            showLanguageDialog()
        }
    }

    private fun handleReelBlockerToggle(enabled: Boolean) {
        if (!premiumManager.isPremium()) {
            revertSwitch(binding.switchReelBlockerQuick, false)
            showPremiumUpsell()
            return
        }
        savedPreferencesLoader.setReelBlockerEnabled(enabled)
        sendRefreshRequest(DeenShieldAccessibilityService.INTENT_ACTION_REFRESH_REEL_BLOCKER)
        updateFeatureStatuses()
    }

    private fun updateFeatureStatuses() {
        val appEnabled = premiumManager.isPremium() &&
            savedPreferencesLoader.isAppBlockerFeatureEnabled() &&
            savedPreferencesLoader.loadBlockedApps().isNotEmpty()
        val focusEnabled = premiumManager.isPremium() && savedPreferencesLoader.getFocusModeData().isTurnedOn
        val antiEnabled = premiumManager.isPremium() &&
            requireContext().getSharedPreferences("anti_uninstall", android.content.Context.MODE_PRIVATE)
                .getBoolean("is_anti_uninstall_on", false)

        binding.txtAppBlockerStatus.text = if (appEnabled) getString(R.string.on) else getString(R.string.off)
        binding.txtFocusModeStatus.text = if (focusEnabled) getString(R.string.on) else getString(R.string.off)
        binding.txtAntiUninstallStatus.text = if (antiEnabled) getString(R.string.on) else getString(R.string.off)
    }

    private fun openFeatureConfig(featureType: String, requiresPremium: Boolean) {
        if (requiresPremium && !premiumManager.isPremium()) {
            showPremiumUpsell()
            return
        }

        val intent = Intent(requireContext(), FragmentActivity::class.java).apply {
            putExtra("feature_type", featureType)
        }
        startActivity(intent)
    }

    private fun sendRefreshRequest(action: String) {
        val ctx = requireContext()
        ctx.sendBroadcast(Intent(action).setPackage(ctx.packageName))
    }

    private fun handleDarkModeToggle(enabled: Boolean) {
        context?.let { ctx ->
            val prefs = ctx.getSharedPreferences("theme_prefs", android.content.Context.MODE_PRIVATE)
            val newTheme = if (enabled) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
            
            prefs.edit().putInt("theme_mode", newTheme).commit()
            AppCompatDelegate.setDefaultNightMode(newTheme)
            
            // Update status text
            binding.txtThemeStatus.text = if (enabled) getString(R.string.dark) else getString(R.string.light)
        }
    }

    private fun showBackupRestoreDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Backup & Restore")
            .setMessage("Choose an action:")
            .setPositiveButton("Backup") { _, _ ->
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                backupDirLauncher.launch(intent)
            }
            .setNegativeButton("Restore") { _, _ ->
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/zip"
                }
                restoreFileLauncher.launch(intent)
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun shareCrashLogs() {
        val ctx = requireContext()
        val errorManager = com.alhaq.deenshield.utils.ErrorReportManager.getInstance(ctx)
        val report = errorManager.exportReportsAsText()
        if (report.contains("No crash logs found.") && report.contains("No feedback submitted.")) {
            Toast.makeText(ctx, "No crash logs found", Toast.LENGTH_SHORT).show()
            return
        }

        val attachmentFile = errorManager.createBundledReportFile()
        if (attachmentFile == null) {
            Toast.makeText(ctx, "Failed to prepare bundled report", Toast.LENGTH_SHORT).show()
            return
        }
        val attachmentUri = androidx.core.content.FileProvider.getUriForFile(
            ctx,
            "${ctx.packageName}.provider",
            attachmentFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "DeenShield Crash Log")
            putExtra(Intent.EXTRA_TEXT, "Bundled crash report attached.")
            putExtra(Intent.EXTRA_CC, arrayOf("support@alhaq-initiative.org", "alhaq.dst@gmail.com"))
            putExtra(Intent.EXTRA_STREAM, attachmentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            selector = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
        }

        try {
            startActivity(Intent.createChooser(intent, "Share Crash Log"))
        } catch (e: Exception) {
            Toast.makeText(ctx, "No email client found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFAQDialog() {
        context?.let { ctx ->
            val faqs = """
                ❓ FREQUENTLY ASKED QUESTIONS
                
                ═══════════════════════════
                📱 GETTING STARTED
                ═══════════════════════════
                
                Q: Do I need to create an account?
                A: No. DeenShield works completely offline. Google Sign-In is optional and only used for premium purchases.
                
                Q: Which permissions does DeenShield need?
                A: DeenShield requires Accessibility Service permission to detect and block apps/content. All processing happens on your device - no data is collected.
                
                Q: Is my data safe?
                A: Yes! DeenShield doesn't collect, store, or transmit any of your data. All AI processing happens locally on your device.
                
                ═══════════════════════════
                🔒 CORE FEATURES (FREE)
                ═══════════════════════════
                
                Q: What is App Blocker?
                A: App Blocker prevents you from opening selected apps. You can block apps individually or auto-block entire categories (gaming, social media, etc.).
                
                Q: What is Keyword Blocker?
                A: Keyword Blocker detects and blocks specified keywords across apps. It works in search fields by default, or can scan all text when enabled.
                
                Q: What is Focus Mode?
                A: Focus Mode temporarily blocks selected apps for a set duration, helping you stay productive. Sessions are tracked for insights.
                
                Q: What is Anti-Uninstall Protection?
                A: Anti-uninstall prevents you from removing DeenShield protection. Choose password mode (requires password) or timed mode (blocks until a future date). Premium feature.
                
                ═══════════════════════════
                ⭐ PREMIUM FEATURES
                ═══════════════════════════
                
                Q: What is App Blocker?
                A: Block distracting apps by category (gaming, social media, etc.) with auto-block for new installs, cheat hours for temporary access, and custom warning screens. Premium feature.
                
                Q: What is Focus Mode?
                A: Create time-boxed app blocking sessions for focused work or study. Track your productivity with detailed session history. Premium feature.
                
                Q: What is View/Reel Blocker?
                A: Limits endless scrolling on Instagram Reels, YouTube Shorts, and TikTok. Set maximum views per day to break doomscrolling habits. Premium feature.
                
                Q: What free features are available?
                A: Keyword Blocker and Usage Statistics are free forever.
                
                ═══════════════════════════
                💎 PREMIUM & BILLING
                ═══════════════════════════
                
                Q: What premium plans are available?
                A: We offer Monthly (GBP 3.5/month), Yearly (GBP 2.5/month billed yearly), and Lifetime (GBP 13.5 one-time).
                
                Q: How do I cancel my subscription?
                A: Open Google Play Store → Profile → Payments & subscriptions → Subscriptions → DeenShield → Cancel. You keep premium until the end of your billing period.
                
                Q: Can I switch from monthly to lifetime?
                A: Yes! Purchase the lifetime plan and your monthly subscription will be canceled automatically.
                
                Q: What happens if I reinstall the app?
                A: Use "Restore Purchases" in Premium screen to restore your premium status.
                
                ═══════════════════════════
                🛠️ TROUBLESHOOTING
                ═══════════════════════════
                
                Q: Features not working?
                A: 1) Enable Accessibility Service in Settings → Accessibility → DeenShield
                   2) Enable Device Admin in Settings → Security → Device Admin Apps
                   3) Restart the app
                
                Q: Settings screen keeps closing?
                A: This is App Protection working. Enter your password to access Settings, or use the 5-minute recovery fallback on the verification screen if you forgot it.
                
                Q: Keyword blocker blocking system apps?
                A: Go to Keyword Blocker → Configure → Add system apps like Settings, Launcher to "Ignored Apps" list.
                
                Q: Battery drain?
                A: DeenShield uses minimal battery. Check Settings → Battery → Background restriction is OFF for DeenShield.
                
                Q: How to backup my settings?
                A: Settings → Data Management → Backup to save all your configurations.
                
                ═══════════════════════════
                🔐 PRIVACY & SECURITY
                ═══════════════════════════
                
                Q: Does DeenShield track my activity?
                A: No. All statistics are stored locally on your device only.
                
                Q: Does DeenShield require internet?
                A: No. All features work offline. Internet is only needed for premium purchases.
                
                Q: Is my browsing history collected?
                A: Never. DeenShield only detects keywords and content in real-time, nothing is stored or transmitted.
                
                Q: Is DeenShield open source?
                A: No. DeenShield's core protection engine is closed-source. We focus on clear privacy documentation, on-device processing, and selective transparency where appropriate.
                
                ═══════════════════════════
                📞 SUPPORT & CONTACT
                ═══════════════════════════
                
                Q: How do I report bugs or request features?
                A: Use the in-app feedback option, email support@alhaq-initiative.org, or contact us through the AmnShield support hub.
                
                Q: Is there a user guide?
                A: Yes. Check the official AmnShield docs for documentation, policies, and updates.
                
                ═══════════════════════════
            """.trimIndent()

            MaterialAlertDialogBuilder(ctx)
                .setTitle("Help & FAQ")
                .setMessage(faqs)
                .setPositiveButton("Email Support") { _, _ ->
                    openUrl("mailto:support@alhaq-initiative.org")
                }
                .setNeutralButton("Docs") { _, _ ->
                    openUrl(Constants.AMNSHIELD_DOCS_URL)
                }
                .setNegativeButton("Close", null)
                .show()
        }
    }

    private fun showAboutDialog() {
        context?.let { ctx ->
            val versionName = try {
                ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName
            } catch (e: Exception) {
                getString(R.string.app_version)
            }

            val message = """
                Version: $versionName
                
                DeenShield is an Islamic digital wellbeing app that helps Muslims maintain a halal digital lifestyle through practical protection and healthy habits.
                
                ✅ Free Features (Forever):
                • Keyword Blocker with custom lists & packs
                • Usage statistics and blocking reports
                • Device admin protection
                
                ⭐ Premium Features:
                • App Blocker with auto-block by category
                • Focus Mode with session tracking
                • View/Reel Blocker (limit doomscrolling)
                • Anti-Uninstall Protection (password/timed)
                • Priority support and updates
                
                🔒 Privacy First:
                • All detection is on-device
                • No data collection or tracking
                • No internet required for core features
                • Clear privacy documentation
                
                Developer: Al-Haq Initiative
                Contact: support@alhaq-initiative.org
                Website: amn.alhaq-initiative.org
            """.trimIndent()

            MaterialAlertDialogBuilder(ctx)
                .setTitle(getString(R.string.about_deenshield))
                .setMessage(message)
                .setPositiveButton("Website") { _, _ ->
                    openUrl(Constants.AMNSHIELD_WEBSITE_URL)
                }
                .setNeutralButton("Email") { _, _ ->
                    openUrl("mailto:support@alhaq-initiative.org")
                }
                .setNegativeButton("Close", null)
                .show()
        }
    }
    private fun revertSwitch(target: MaterialSwitch, checked: Boolean) {
        suppressFeatureSwitchChange = true
        target.isChecked = checked
        suppressFeatureSwitchChange = false
    }

    private fun showPremiumUpsell() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.premium_required_title)
            .setMessage(getString(R.string.premium_required_message))
            .setPositiveButton(R.string.premium_view_plans) { _, _ ->
                openPremiumScreen()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun openPremiumScreen() {
        val intent = Intent(requireContext(), FragmentActivity::class.java).apply {
            putExtra("feature_type", "premium_features")
        }
        startActivity(intent)
    }


    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            val options = ActivityOptionsCompat.makeCustomAnimation(
                requireContext(),
                R.anim.fade_in,
                R.anim.fade_out
            )
            startActivity(intent, options.toBundle())
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No application found to open the link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showThemeSelectionDialog() {
        val themes = arrayOf(
            getString(R.string.light_mode),
            getString(R.string.dark_mode),
            getString(R.string.system_default),
            getString(R.string.modern_gradient),
            getString(R.string.purple_gradient),
            getString(R.string.emerald_theme),
            getString(R.string.sunset_glow)
        )

        val prefs = requireContext().getSharedPreferences("theme_prefs", android.content.Context.MODE_PRIVATE)
        val currentMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        val currentStyle = prefs.getString("theme_style", "default")

        var checkedItem = when {
            currentStyle == "gradient" -> 3
            currentStyle == "purple" -> 4
            currentStyle == "emerald" -> 5
            currentStyle == "sunset" -> 6
            currentMode == AppCompatDelegate.MODE_NIGHT_NO -> 0
            currentMode == AppCompatDelegate.MODE_NIGHT_YES -> 1
            else -> 2
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.select_theme))
            .setSingleChoiceItems(themes, checkedItem) { dialog, which ->
                val editor = prefs.edit()
                when (which) {
                    0 -> { // Light
                        editor.putInt("theme_mode", AppCompatDelegate.MODE_NIGHT_NO)
                        editor.putString("theme_style", "default")
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    }
                    1 -> { // Dark
                        editor.putInt("theme_mode", AppCompatDelegate.MODE_NIGHT_YES)
                        editor.putString("theme_style", "default")
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    }
                    2 -> { // System
                        editor.putInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                        editor.putString("theme_style", "default")
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    }
                    3 -> { // Gradient
                        editor.putInt("theme_mode", AppCompatDelegate.MODE_NIGHT_YES) // Force dark for gradient
                        editor.putString("theme_style", "gradient")
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    }
                    4 -> { // Purple Gradient
                        editor.putInt("theme_mode", AppCompatDelegate.MODE_NIGHT_YES)
                        editor.putString("theme_style", "purple")
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    }
                    5 -> { // Emerald
                        editor.putInt("theme_mode", AppCompatDelegate.MODE_NIGHT_NO)
                        editor.putString("theme_style", "emerald")
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    }
                    6 -> { // Sunset Glow (flat warm, slightly dark-ish)
                        editor.putInt("theme_mode", AppCompatDelegate.MODE_NIGHT_NO)
                        editor.putString("theme_style", "sunset")
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    }
                }
                editor.apply()
                dialog.dismiss()
                
                // Restart activity to apply theme changes
                requireActivity().recreate()
            }
            .show()
    }

    private fun showLanguageDialog() {
        val languages = arrayOf(
            getString(R.string.language_system) to "",
            getString(R.string.language_english) to "en",
            getString(R.string.language_german) to "de",
            getString(R.string.language_persian) to "fa",
            getString(R.string.language_french) to "fr",
            getString(R.string.language_hindi) to "hi",
            getString(R.string.language_portuguese) to "pt",
            getString(R.string.language_turkish) to "tr",
            getString(R.string.language_chinese) to "zh"
        )

        val languageNames = languages.map { it.first }.toTypedArray()
        val currentLocale = AppCompatDelegate.getApplicationLocales()[0]?.language ?: ""
        val currentIndex = languages.indexOfFirst { it.second == currentLocale }.coerceAtLeast(0)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.choose_language)
            .setSingleChoiceItems(languageNames, currentIndex) { dialog, which ->
                val selectedLanguage = languages[which].second
                setAppLanguage(selectedLanguage)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun setAppLanguage(languageCode: String) {
        val localeList = if (languageCode.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageCode)
        }
        
        AppCompatDelegate.setApplicationLocales(localeList)
        updateLanguageStatus()
        
        Toast.makeText(requireContext(), R.string.restart_required, Toast.LENGTH_LONG).show()
    }

    private fun updateLanguageStatus() {
        val currentLocale = AppCompatDelegate.getApplicationLocales()[0]
        binding.txtLanguageStatus.text = when (currentLocale?.language) {
            "en" -> getString(R.string.language_english)
            "de" -> getString(R.string.language_german)
            "fa" -> getString(R.string.language_persian)
            "fr" -> getString(R.string.language_french)
            "hi" -> getString(R.string.language_hindi)
            "pt" -> getString(R.string.language_portuguese)
            "tr" -> getString(R.string.language_turkish)
            "zh" -> getString(R.string.language_chinese)
            else -> getString(R.string.language_system)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            loadPreferences()
        }
    }
}

