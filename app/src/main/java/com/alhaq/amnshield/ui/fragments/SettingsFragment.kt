package com.alhaq.amnshield.ui.fragments

import android.content.ActivityNotFoundException
import android.content.Context
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
import androidx.lifecycle.ViewModelProvider
import androidx.compose.ui.platform.ComposeView
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.alhaq.amnshield.Constants
import com.alhaq.amnshield.R
import com.alhaq.amnshield.ui.activity.MainActivity
import com.alhaq.amnshield.ui.activity.FragmentActivity
import com.alhaq.amnshield.ui.activity.RemindersActivity
import com.alhaq.amnshield.services.AmnShieldAccessibilityService
import com.alhaq.amnshield.premium.PremiumManager
import com.alhaq.amnshield.utils.SavedPreferencesLoader
import com.alhaq.amnshield.utils.ZipUtils
import com.alhaq.amnshield.utils.GoogleSignInHelper
import com.alhaq.amnshield.ui.screens.SettingsScreen
import com.alhaq.amnshield.ui.theme.AmnShieldTheme
import com.alhaq.amnshield.ui.viewmodel.AmnShieldViewModel
import java.util.Locale

class SettingsFragment : Fragment() {

    private lateinit var viewModel: AmnShieldViewModel
    private val premiumManager by lazy { PremiumManager.getInstance(requireContext().applicationContext) }
    private val savedPreferencesLoader by lazy { SavedPreferencesLoader(requireContext().applicationContext) }
    private lateinit var googleSignInHelper: GoogleSignInHelper

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[AmnShieldViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val state by viewModel.state.collectAsState()
                AmnShieldTheme(appTheme = state.currentTheme) {
                    SettingsScreen(
                        state = state,
                        viewModel = viewModel,
                        onNavigateToProfile = {
                            val containerId = if (requireActivity().findViewById<View>(R.id.nav_host_fragment) != null) {
                                R.id.nav_host_fragment
                            } else {
                                R.id.fragment_holder
                            }
                            val transaction = parentFragmentManager.beginTransaction()
                            transaction.replace(containerId, ProfileFragment())
                            transaction.addToBackStack(null)
                            transaction.commit()
                        },
                        onBackupRestore = { showBackupRestoreDialog() },
                        onReminders = {
                            val intent = Intent(requireContext(), RemindersActivity::class.java)
                            val options = ActivityOptionsCompat.makeCustomAnimation(
                                requireContext(),
                                R.anim.fade_in,
                                R.anim.fade_out
                            )
                            startActivity(intent, options.toBundle())
                        },
                        onShareCrashLogs = { shareCrashLogs() },
                        onHelpFAQ = { showFAQDialog() },
                        onAbout = { showAboutDialog() },
                        onLanguage = { showLanguageDialog() },
                        onSignOut = { showSignOutDialog() },
                        onToggleWebFilter = { enabled ->
                            savedPreferencesLoader.setWebsiteBlockerEnabled(enabled)
                            sendRefreshRequest(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_APP_BLOCKER)
                            viewModel.loadState(viewModel.state.value.copy(isWebFilterEnabled = enabled))
                        },
                        onToggleUsageLimit = { enabled ->
                            savedPreferencesLoader.setUsageTrackerFeatureEnabled(enabled)
                            viewModel.loadState(viewModel.state.value.copy(isUsageLimitEnabled = enabled))
                        },
                        onBack = {
                            if (!parentFragmentManager.popBackStackImmediate()) {
                                requireActivity().finish()
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        googleSignInHelper = GoogleSignInHelper(requireContext())
        loadSettingsState()
    }

    private fun loadSettingsState() {
        val webFilterEnabled = savedPreferencesLoader.isWebsiteBlockerEnabled()
        val usageTrackerEnabled = savedPreferencesLoader.isUsageTrackerFeatureEnabled()
        val account = googleSignInHelper.getLastSignedInAccount()
        val name = account?.displayName ?: "Alhaq DST"
        val email = account?.email ?: "alhaq.dst@gmail.com"
        
        val isAdvanced = true
        
        viewModel.loadState(
            viewModel.state.value.copy(
                isWebFilterEnabled = webFilterEnabled,
                isUsageLimitEnabled = usageTrackerEnabled,
                userName = name,
                userEmail = email,
                isAdvancedMode = isAdvanced
            )
        )
    }

    private fun sendRefreshRequest(action: String) {
        val ctx = requireContext()
        ctx.sendBroadcast(Intent(action).setPackage(ctx.packageName))
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
        val errorManager = com.alhaq.amnshield.utils.ErrorReportManager.getInstance(ctx)
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
            putExtra(Intent.EXTRA_SUBJECT, "AmnShield Crash Log")
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
                A: No. AmnShield works completely offline. Google Sign-In is optional and only used for premium purchases.
                
                Q: Which permissions does AmnShield need?
                A: AmnShield requires Accessibility Service permission to detect and block apps/content. All processing happens on your device - no data is collected.
                
                Q: Is my data safe?
                A: Yes! AmnShield doesn't collect, store, or transmit any of your data. All AI processing happens locally on your device.
                
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
                A: Anti-uninstall prevents you from removing AmnShield protection. Choose password mode (requires password) or timed mode (blocks until a future date). Premium feature.
                
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
                A: Open Google Play Store → Profile → Payments & subscriptions → Subscriptions → AmnShield → Cancel. You keep premium until the end of your billing period.
                
                Q: Can I switch from monthly to lifetime?
                A: Yes! Purchase the lifetime plan and your monthly subscription will be canceled automatically.
                
                Q: What happens if I reinstall the app?
                A: Use "Restore Purchases" in Premium screen to restore your premium status.
                
                ═══════════════════════════
                🛠️ TROUBLESHOOTING
                ═══════════════════════════
                
                Q: Features not working?
                A: 1) Enable Accessibility Service in Settings → Accessibility → AmnShield
                   2) Enable Device Admin in Settings → Security → Device Admin Apps
                   3) Restart the app
                
                Q: Settings screen keeps closing?
                A: This is App Protection working. Enter your password to access Settings, or use the 5-minute recovery fallback on the verification screen if you forgot it.
                
                Q: Keyword blocker blocking system apps?
                A: Go to Keyword Blocker → Configure → Add system apps like Settings, Launcher to "Ignored Apps" list.
                
                Q: Battery drain?
                A: AmnShield uses minimal battery. Check Settings → Battery → Background restriction is OFF for AmnShield.
                
                Q: How to backup my settings?
                A: Settings → Data Management → Backup to save all your configurations.
                
                ═══════════════════════════
                🔐 PRIVACY & SECURITY
                ═══════════════════════════
                
                Q: Does AmnShield track my activity?
                A: No. All statistics are stored locally on your device only.
                
                Q: Does AmnShield require internet?
                A: No. All features work offline. Internet is only needed for premium purchases.
                
                Q: Is my browsing history collected?
                A: Never. AmnShield only detects keywords and content in real-time, nothing is stored or transmitted.
                
                Q: Is AmnShield open source?
                A: No. AmnShield's core protection engine is closed-source. We focus on clear privacy documentation, on-device processing, and selective transparency where appropriate.
                
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
                
                AmnShield is an Islamic digital wellbeing app that helps Muslims maintain a halal digital lifestyle through practical protection and healthy habits.
                
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
                .setTitle(getString(R.string.about_amnshield))
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

    private fun showSignOutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.sign_out))
            .setMessage(getString(R.string.sign_out_confirmation))
            .setPositiveButton(getString(R.string.sign_out)) { _, _ ->
                googleSignInHelper.signOut {
                    Toast.makeText(requireContext(), getString(R.string.signed_out_successfully), Toast.LENGTH_SHORT).show()
                    loadSettingsState()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
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
        loadSettingsState()
        
        Toast.makeText(requireContext(), R.string.restart_required, Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        loadSettingsState()
    }
}
