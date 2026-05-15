package com.alhaq.deenshield.ui.activity

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.documentfile.provider.DocumentFile
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.alhaq.deenshield.Constants
import com.alhaq.deenshield.R
import com.alhaq.deenshield.databinding.ActivityMainBinding
import com.alhaq.deenshield.databinding.DialogPermissionInfoBinding
import com.alhaq.deenshield.databinding.DialogRemoveAntiUninstallBinding
import com.alhaq.deenshield.receivers.AdminReceiver
import com.alhaq.deenshield.services.DeenShieldAccessibilityService
import com.alhaq.deenshield.ui.fragments.HomeFragment
import com.alhaq.deenshield.ui.fragments.BlocksFragment
import com.alhaq.deenshield.ui.fragments.StatsFragment
import com.alhaq.deenshield.ui.fragments.SettingsFragment
import com.alhaq.deenshield.ui.activity.FragmentActivity
import com.alhaq.deenshield.ui.fragments.installation.AccessibilityGuide
import com.alhaq.deenshield.ui.fragments.installation.WelcomeFragment
import com.alhaq.deenshield.utils.ErrorReportManager
import com.alhaq.deenshield.utils.SavedPreferencesLoader
import com.alhaq.deenshield.utils.PermissionGuideHelper
import com.alhaq.deenshield.utils.GoogleSignInHelper
import com.alhaq.deenshield.utils.ThemeUtils
import com.alhaq.deenshield.utils.UserFeedback
import com.alhaq.deenshield.utils.ZipUtils
import com.alhaq.deenshield.utils.BillingClientWrapper
import com.alhaq.deenshield.premium.PremiumManager
import com.alhaq.deenshield.permissions.PermissionsBottomSheet
import com.alhaq.deenshield.permissions.PermissionsManager
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Calendar


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var googleSignInHelper: GoogleSignInHelper
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private var brandLogoBitmap: Bitmap? = null
    private var drawerBannerDrawable: Drawable? = null
    
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val account = googleSignInHelper.handleSignInResult(result.data)
            if (account != null) {
                updateNavigationHeader(account)
                Toast.makeText(this, getString(R.string.signed_in_as, account.email), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Sign in failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private lateinit var selectPinnedAppsLauncher: ActivityResultLauncher<Intent>

    private lateinit var selectBlockedAppsLauncher: ActivityResultLauncher<Intent>

    private lateinit var selectFocusModeUnblockedAppsLauncher: ActivityResultLauncher<Intent>

    private lateinit var selectOverlayAppsLauncher: ActivityResultLauncher<Intent>

    private lateinit var selectBlockedKeywords: ActivityResultLauncher<Intent>

    private lateinit var addCheatHoursActivity: ActivityResultLauncher<Intent>

    private lateinit var addAutoFocusHoursActivity: ActivityResultLauncher<Intent>

    private lateinit var directoryPicker: ActivityResultLauncher<Intent>


    private val savedPreferencesLoader = SavedPreferencesLoader(this)
    private val premiumManager by lazy { PremiumManager.getInstance(this) }
    private lateinit var options: ActivityOptionsCompat

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // Permission granted, show notifications
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()

//                makeStartFocusModeDialog()
            } else {
                // Permission denied
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()

            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this)

        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        // Proper edge-to-edge inset handling for Android 15+
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            
            // Apply padding to prevent content from being hidden by system bars
            v.setPadding(
                systemBars.left,
                systemBars.top, // Add top padding to account for status bar
                systemBars.right,
                maxOf(systemBars.bottom, imeInsets.bottom) // Handle keyboard
            )
            
            // Consume insets to prevent further propagation
            WindowInsetsCompat.CONSUMED
        }

        // Initialize helpers
        googleSignInHelper = GoogleSignInHelper(this)
        drawerLayout = binding.drawerLayout

        // Modern back-press handling: prefer OnBackPressedDispatcher over the
        // deprecated Activity.onBackPressed override. Closes the navigation
        // drawer when open, otherwise falls back to the system default.
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // Initialize notification channels
        initializeNotificationChannels()
        
        // Restore premium purchases automatically on app start
        restorePremiumPurchases()
        
        // Schedule daily reports for premium users
        scheduleDailyReportsIfPremium()

        // Setup navigation drawer
        setupNavigationDrawer()

        options = ActivityOptionsCompat.makeCustomAnimation(this, R.anim.fade_in, R.anim.fade_out)
        setupActivityLaunchers()
        setupFragmentNavigation(savedInstanceState)

        if (!isFirstLaunchComplete()) {
            val intent = Intent(this, FragmentActivity::class.java)
            intent.putExtra("fragment", WelcomeFragment.FRAGMENT_ID)
            startActivity(intent, options.toBundle())
            // Show permissions bottom sheet on first launch
            val permissionsBottomSheet = PermissionsBottomSheet()
            permissionsBottomSheet.show(supportFragmentManager, PermissionsBottomSheet.TAG)
            setFirstLaunchComplete(true)
        }
        showDonationDialog()
    }
    
    private fun setupFragmentNavigation(savedInstanceState: Bundle?) {
        // Load HomeFragment by default
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, HomeFragment())
                .commit()
            binding.bottomNavigation.selectedItemId = R.id.navigation_home
        }
        
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment, HomeFragment())
                        .commit()
                    true
                }
                R.id.navigation_stats -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment, StatsFragment())
                        .commit()
                    true
                }
                R.id.navigation_reports -> {
                    val intent = Intent(this, ReportsActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.navigation_blocks -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment, BlocksFragment())
                        .commit()
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
    
    override fun onResume() {
        super.onResume()
        // Permissions will now be handled by PermissionsBottomSheet on first launch, 
        // or user can access them through settings if needed.
        // checkPermissions() // Removed old permission check
        maybeShowPremiumReminder()
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (::drawerToggle.isInitialized && drawerToggle.onOptionsItemSelected(item)) {
            return true
        }
        return when (item.itemId) {
            R.id.action_notifications -> {
                // Bell icon opens the in-app notification inbox.
                // Reminder/notification preferences live under Settings → Reminders.
                val intent = Intent(this, NotificationsActivity::class.java)
                val options = ActivityOptionsCompat.makeCustomAnimation(
                    this,
                    R.anim.fade_in,
                    R.anim.fade_out
                )
                startActivity(intent, options.toBundle())
                true
            }
            R.id.action_settings -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, SettingsFragment())
                    .commit()
                binding.bottomNavigation.selectedItemId = R.id.navigation_blocks
                true
            }
            R.id.action_about -> {
                showAboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupActivityLaunchers() {

        selectPinnedAppsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val selectedApps = result.data?.getStringArrayListExtra("SELECTED_APPS")
                selectedApps?.let {
                    savedPreferencesLoader.savePinned(it.toSet())
                }
            }
        }

        selectBlockedAppsLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val selectedApps = result.data?.getStringArrayListExtra("SELECTED_APPS")
                    selectedApps?.let {
                        savedPreferencesLoader.saveBlockedApps(it.toSet())
                        sendRefreshRequest(DeenShieldAccessibilityService.INTENT_ACTION_REFRESH_APP_BLOCKER)
                    }
                }
            }


        selectFocusModeUnblockedAppsLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val selectedApps = result.data?.getStringArrayListExtra("SELECTED_APPS")
                    selectedApps?.let {
                        savedPreferencesLoader.saveFocusModeSelectedApps(selectedApps)
                    }
                }
            }

        selectOverlayAppsLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val selectedApps = result.data?.getStringArrayListExtra("SELECTED_APPS")
                    selectedApps?.let {
                        savedPreferencesLoader.setOverlayApps(it.toSet())
                    }
                }
            }

        selectBlockedKeywords =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val blockedKeywords = result.data?.getStringArrayListExtra("SELECTED_KEYWORDS")
                    blockedKeywords?.let {
                        savedPreferencesLoader.saveBlockedKeywords(it.toSet())
                        sendRefreshRequest(DeenShieldAccessibilityService.INTENT_ACTION_REFRESH_BLOCKED_KEYWORD_LIST)
                    }
                }
            }

        addCheatHoursActivity =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
                sendRefreshRequest(DeenShieldAccessibilityService.INTENT_ACTION_REFRESH_APP_BLOCKER)
            }

        addAutoFocusHoursActivity =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
                sendRefreshRequest(DeenShieldAccessibilityService.INTENT_ACTION_REFRESH_FOCUS_MODE)
            }
        // Register the directory picker
        directoryPicker = ZipUtils.registerDirectoryPicker(this) { directoryUri ->
            // Create the zip file in the selected directory
            val filename = ZipUtils.createZipFileName()
            val zipUri = createFileInDirectory(directoryUri, filename)
            zipUri?.let {
                ZipUtils.zipSharedPreferencesToUri(this, it)
            }
        }
    }

    // OLD: setupClickListeners removed - UI moved to fragments
    /*
    private fun setupClickListeners() {
        // click listeners for configuration options
        binding.selectPinnedApps.setOnClickListener {
            val intent = Intent(this, SelectAppsActivity::class.java)
            intent.putStringArrayListExtra(
                "PRE_SELECTED_APPS",
                ArrayList(savedPreferencesLoader.loadPinnedApps())
            )

            selectPinnedAppsLauncher.launch(intent, options)

        }
        binding.selectBlockedApps.setOnClickListener {
            val intent = Intent(this, SelectAppsActivity::class.java)
            intent.putStringArrayListExtra(
                "PRE_SELECTED_APPS",
                ArrayList(savedPreferencesLoader.loadBlockedApps())
            )
            selectBlockedAppsLauncher.launch(intent, options)
        }
        binding.selectBlockedKeywords.setOnClickListener {
            val intent = Intent(this, ManageKeywordsActivity::class.java)
            intent.putStringArrayListExtra(
                "PRE_SAVED_KEYWORDS",
                ArrayList(savedPreferencesLoader.loadBlockedKeywords())
            )
            selectBlockedKeywords.launch(intent, options)
        }


        binding.appBlockerSelectCheatHours.setOnClickListener {
            val intent = Intent(this, TimedActionActivity::class.java)
            intent.putExtra("selected_mode", TimedActionActivity.MODE_APP_BLOCKER_CHEAT_HOURS)
            addCheatHoursActivity.launch(intent, options)
        }
        binding.btnConfigAppblockerWarning.setOnClickListener {
            TweakAppBlockerWarning(savedPreferencesLoader).show(
                supportFragmentManager,
                "tweak_app_blocker_warning"
            )
        }
        binding.btnConfigViewblockerWarning.setOnClickListener {
            TweakViewBlockerWarning(savedPreferencesLoader).show(
                supportFragmentManager,
                "tweak_view_blocker_warning"
            )
        }
        binding.btnConfigViewblockerCheatHours.setOnClickListener {
            TweakViewBlockerCheatHours(savedPreferencesLoader).show(
                supportFragmentManager,
                "tweak_view_blocker_cheat_hours"
            )
        }
        binding.btnConfigTracker.setOnClickListener{
            TweakUsageTracker(savedPreferencesLoader).show(
                supportFragmentManager,
                "tweak_usage_tracker"
            )
        }
        binding.btnUnlockAntiUninstall.setOnClickListener {
            makeRemoveAntiUninstallDialog()
        }
        binding.btnManagePreinstalledKeywords.setOnClickListener {
            TweakKeywordPack().show(supportFragmentManager, "tweak_keyword_pack")
        }
        binding.btnManageKeywordBlocker.setOnClickListener {
            TweakKeywordBlocker(savedPreferencesLoader).show(
                supportFragmentManager,
                "tweak_keyword_blocker"
            )
        }
        binding.selectAppUsageStats.setOnClickListener {
            val intent = Intent(this, FragmentActivity::class.java)
            intent.putExtra("fragment", AllAppsUsageFragment.FRAGMENT_ID)
            startActivity(intent, options.toBundle())
        }

        binding.selectReelUsageStats.setOnClickListener {
            val intent = Intent(this, UsageMetricsActivity::class.java)
            startActivity(intent, options.toBundle())
        }
        binding.btnSelectAppsToShowOverlay.setOnClickListener {
            val intent = Intent(this, SelectAppsActivity::class.java)
            intent.putStringArrayListExtra(
                "PRE_SELECTED_APPS",
                ArrayList(savedPreferencesLoader.getOverlayApps())
            )
            selectOverlayAppsLauncher.launch(intent, options)
        }
        binding.selectFocusBlockedApps.setOnClickListener {
            val intent = Intent(this, SelectAppsActivity::class.java)
            intent.putStringArrayListExtra(
                "PRE_SELECTED_APPS",
                ArrayList(savedPreferencesLoader.getFocusModeSelectedApps())
            )
            selectFocusModeUnblockedAppsLauncher.launch(intent, options)
        }
        binding.autoFocus.setOnClickListener {
            val intent = Intent(this, TimedActionActivity::class.java)
            intent.putExtra("selected_mode", TimedActionActivity.MODE_AUTO_FOCUS)
            addAutoFocusHoursActivity.launch(intent, options)
        }


        binding.startFocusMode.setOnClickListener {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(
                        this, Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS,options)
                    return@setOnClickListener
                }
            }


            createFocusModeShortcut()

            StartFocusMode(savedPreferencesLoader, onPositiveButtonPressed = {
                binding.selectFocusBlockedApps.isEnabled = false
                binding.startFocusMode.isEnabled = false

            }).show(
                supportFragmentManager,
                "start_focus_mode"
            )

        }

        // listeners for turn on/ off buttons
        binding.antiUninstallCardChip.setOnClickListener {
            if (!isDeviceAdminOn) {
                makeDeviceAdminPermissionDialog()
            } else {
                if (binding.antiUninstallWarning.visibility == View.GONE) {
                    val intent = Intent(this, FragmentActivity::class.java)
                    intent.putExtra("fragment", ChooseModeFragment.FRAGMENT_ID)
                    startActivity(intent, options.toBundle())
                } else {
                    makeAccessibilityInfoDialog(
                        "DeenShield",
                        DeenShieldAccessibilityService::class.java
                    )
                }
            }
        }

        // Monochrome feature removed - Shizuku dependency removed

        binding.keywordBlockerStatusChip.setOnClickListener {
            makeAccessibilityInfoDialog("Keyword Blocker", KeywordBlockerService::class.java)
        }
        binding.focusModeStatusChip.setOnClickListener {
            makeAccessibilityInfoDialog("App Blocker", AppBlockerService::class.java)
        }
        binding.appBlockerStatusChip.setOnClickListener {
            makeAccessibilityInfoDialog("App Blocker", AppBlockerService::class.java)
        }
        binding.viewBlockerStatusChip.setOnClickListener {
            makeAccessibilityInfoDialog("View Blocker", ViewBlockerService::class.java)
        }
        binding.usageTrackerStatusChip.setOnClickListener {
            if (!isDisplayOverOtherAppsOn) {
                makeDrawOverOtherAppsDialog()
            } else {
                makeAccessibilityInfoDialog("Usage Tracker", UsageTrackingService::class.java)
            }
        }

        // socials click listeners
        binding.btnDiscord.setOnClickListener {
            openUrl("https://discord.gg/zXz7pGVJY")
        }

        binding.btnTelegram.setOnClickListener {
            openUrl("https://t.me/deenshield")
        }
        binding.btnGithub.setOnClickListener {
            openUrl(Constants.AMNSHIELD_WEBSITE_URL)
        }
        binding.btnInstagram.setOnClickListener {
            openUrl("https://www.instagram.com/alhaqinitiative")
        }
        binding.btnDonate.setOnClickListener {
            openUrl("https://alhaq-initiative.org/donate.html")
        }

        binding.btnCredits.setOnClickListener {
            openUrl("https://alhaq-initiative.org/credits.html")
        }
        binding.btnBackup.setOnClickListener {
            ZipUtils.showDirectoryPicker(directoryPicker)
        }
        binding.helpReelBlocker.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.about_view_blocker))
                .setMessage(getString(R.string.this_option_has_the_ability_to_block_youtube_shorts_and_instagram_reels_while_allowing_access_to_other_app_features))
                .setPositiveButton(getString(R.string.ok), null)
                .show()
        }

        binding.btnBackup.setOnClickListener {
            ZipUtils.showDirectoryPicker(directoryPicker)
        }
        binding.btnShareErrors.setOnClickListener {
            shareCrashLog(this)
        }

        // Card click listeners - make feature cards expandable/collapsible
        binding.focusModeCard.setOnClickListener {
            // Toggle visibility of focus mode configuration buttons
            val buttonsVisible = binding.selectFocusBlockedApps.visibility == View.VISIBLE
            binding.selectFocusBlockedApps.visibility = if (buttonsVisible) View.GONE else View.VISIBLE
            binding.autoFocus.visibility = if (buttonsVisible) View.GONE else View.VISIBLE
            binding.startFocusMode.visibility = if (buttonsVisible) View.GONE else View.VISIBLE
        }

        binding.appBlockerCard.setOnClickListener {
            // Toggle visibility of app blocker configuration buttons
            val buttonsVisible = binding.selectBlockedApps.visibility == View.VISIBLE
            binding.selectBlockedApps.visibility = if (buttonsVisible) View.GONE else View.VISIBLE
            binding.appBlockerSelectCheatHours.visibility = if (buttonsVisible) View.GONE else View.VISIBLE
            binding.btnConfigAppblockerWarning.visibility = if (buttonsVisible) View.GONE else View.VISIBLE
        }

        binding.viewBlockerCard.setOnClickListener {
            // Toggle visibility of view blocker configuration buttons
            val buttonsVisible = binding.btnConfigViewblockerWarning.visibility == View.VISIBLE
            binding.btnConfigViewblockerWarning.visibility = if (buttonsVisible) View.GONE else View.VISIBLE
            binding.btnConfigViewblockerCheatHours.visibility = if (buttonsVisible) View.GONE else View.VISIBLE
        }

        binding.keywordBlockerCard.setOnClickListener {
            // Toggle visibility of keyword blocker configuration buttons
            val buttonsVisible = binding.selectBlockedKeywords.visibility == View.VISIBLE
            binding.selectBlockedKeywords.visibility = if (buttonsVisible) View.GONE else View.VISIBLE
            binding.btnManagePreinstalledKeywords.visibility = if (buttonsVisible) View.GONE else View.VISIBLE
            binding.btnManageKeywordBlocker.visibility = if (buttonsVisible) View.GONE else View.VISIBLE
        }

        binding.usageTrackerCard.setOnClickListener {
            // Navigate to AllAppsUsageFragment which shows usage stats with diagram
            val intent = Intent(this, FragmentActivity::class.java)
            intent.putExtra("fragment", "all_app_usage")
            val options = ActivityOptionsCompat.makeCustomAnimation(
                this,
                R.anim.fade_in,
                R.anim.fade_out
            )
            startActivity(intent, options.toBundle())
        }

        binding.antiUninstallCard.setOnClickListener {
            // Show/hide anti-uninstall unlock button
            val buttonVisible = binding.btnUnlockAntiUninstall.visibility == View.VISIBLE
            binding.btnUnlockAntiUninstall.visibility = if (buttonVisible) View.GONE else View.VISIBLE
        }
    }
    */ // END setupClickListeners - commented out

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            startActivity(intent, options.toBundle())
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No application found to open the link", Toast.LENGTH_SHORT).show()
        }
    }

//    private fun checkPermissions() { // Removed old permission check
//        isDisplayOverOtherAppsOn = Settings.canDrawOverlays(this)
//        lifecycleScope.launch {
//            withContext(Dispatchers.IO) {
//                isGeneralSettingsOn = isAccessibilityServiceEnabled(DeenShieldAccessibilityService::class.java)
//            }
//
//            val devicePolicyManager =
//                getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
//            val componentName = ComponentName(applicationContext, AdminReceiver::class.java)
//            isDeviceAdminOn = devicePolicyManager.isAdminActive(componentName)
//
//            val antiUninstallInfo = getSharedPreferences("anti_uninstall", Context.MODE_PRIVATE)
//            isAntiUninstallOn = antiUninstallInfo.getBoolean("is_anti_uninstall_on", false)
//
//            withContext(Dispatchers.Main) {
//                notifyHomeFragment()
//            }
//        }
//    }

    private fun notifyHomeFragment() {
        val fragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        if (fragment is HomeFragment) {
            fragment.refreshStatus()
        }
    }


    // setupShizukuFeatures removed - Shizuku dependency removed

    private fun showDonationDialog() {
        val sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val firstDate = sharedPreferences.getString("first_date", null)
        if (firstDate == null) {
            // Store the current date as a string representation
            val currentDateString = LocalDate.now().toString()
            sharedPreferences.edit().putString("first_date", currentDateString).apply()
        }

        if (!(sharedPreferences.getBoolean("is_donation_alerted", false))) {
            // Parse the stored date string back to LocalDate
            val storedFirstDate = firstDate?.let { LocalDate.parse(it) } ?: LocalDate.now()
            val daysPassed = ChronoUnit.DAYS.between(storedFirstDate, LocalDate.now())

            if (daysPassed > 5L) {
                sharedPreferences.edit().putBoolean("is_donation_alerted", true).apply()
                MaterialAlertDialogBuilder(this)
                    .setTitle("Support DeenShield Development")
                    .setMessage(
                        "Thank you for using DeenShield! " +
                                "\n\nMy name is Habibur Rahman, and I am the founder of Al-Haq Initiative. I\'m a student with a passion for building Islamic wellbeing tools. " +
                                "I created DeenShield to help Muslims maintain a halal digital lifestyle. " +
                                "\n\nIf you find DeenShield beneficial, please consider supporting the project by:\n" +
                                "\u2022 Subscribing to Premium (unlocks advanced features)\n" +
                                "\u2022 Donating via Ko-fi or Stripe\n" +
                                "\n\nYour support helps keep DeenShield free and ad-free for everyone. JazakAllahu Khairan!"
                    )
                    .setNegativeButton("Close") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setPositiveButton("Support Now") { dialog, _ ->
                        showSupportOptionsDialog()
                        dialog.dismiss()
                    }
                    .setCancelable(false)
                    .show()
            }
        }
    }
    
    private fun showSupportOptionsDialog() {
        val options = arrayOf("Subscribe to Premium", "Donate via Ko-fi", "Donate via Stripe")
        MaterialAlertDialogBuilder(this)
            .setTitle("Support DeenShield")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> {
                        // Open Premium screen
                        val intent = Intent(this, FragmentActivity::class.java).apply {
                            putExtra("feature_type", "premium_features")
                        }
                        startActivity(intent)
                    }
                    1 -> {
                        // Open Ko-fi donation
                        openUrl("https://ko-fi.com/adsdonation")
                    }
                    2 -> {
                        // Open Stripe donation
                        openUrl("https://buy.stripe.com/28E3cwea897i8vkh2l14400")
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun isFirstLaunchComplete(): Boolean {
        val sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("isFirstLaunchComplete", false)
    }

    private fun setFirstLaunchComplete(complete: Boolean) {
        val sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("isFirstLaunchComplete", complete).apply()
    }

    fun shareCrashLog(context: Context) {
        val errorManager = ErrorReportManager.getInstance(context)
        val report = errorManager.exportReportsAsText()
        if (report.contains("No crash logs found.") && report.contains("No feedback submitted.")) {
            Toast.makeText(context, "No crash logs found", Toast.LENGTH_SHORT).show()
            return
        }

        val attachmentFile = errorManager.createBundledReportFile()
        if (attachmentFile == null) {
            Toast.makeText(context, "Failed to prepare bundled report", Toast.LENGTH_SHORT).show()
            return
        }
        val attachmentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            attachmentFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "DeenShield Crash Log")
            putExtra(Intent.EXTRA_TEXT, "Bundled crash report attached.")
            putExtra(Intent.EXTRA_CC, SUPPORT_CC_ADDRESSES)
            putExtra(Intent.EXTRA_STREAM, attachmentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            selector = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
        }

        context.startActivity(Intent.createChooser(intent, "Share Crash Log"))
    }
    private fun sendRefreshRequest(action: String) {
        val intent = Intent(action).setPackage(packageName)
        sendBroadcast(intent)
    }
//    private fun isAccessibilityServiceEnabled(serviceClass: Class<out AccessibilityService>): Boolean { // Removed old permission check
//        val serviceName = ComponentName(this, serviceClass).flattenToString()
//        val enabledServices = Settings.Secure.getString(
//            contentResolver,
//            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
//        ) ?: return false
//        val isAccessibilityEnabled = Settings.Secure.getInt(
//            contentResolver,
//            Settings.Secure.ACCESSIBILITY_ENABLED,
//            0
//        )
//        return isAccessibilityEnabled == 1 && enabledServices.contains(serviceName)
//    }

//    private fun makeDeviceAdminPermissionDialog() { // Removed old permission dialog
//        val dialogDeviceAdmin =
//            DialogPermissionInfoBinding.inflate(layoutInflater)
//        dialogDeviceAdmin.title.text = getString(R.string.enable_2, "Device Admin")
//        dialogDeviceAdmin.desc.text = getString(R.string.device_admin_perm)
//        dialogDeviceAdmin.point1.text =
//            getString(R.string.prevent_uninstallation_attempts_until_a_set_condition_is_met)
//        dialogDeviceAdmin.point2.visibility = View.GONE
//        val dialog = MaterialAlertDialogBuilder(this)
//            .setView(dialogDeviceAdmin.root)
//            .show()
//
//        dialogDeviceAdmin.btnReject.setOnClickListener {
//            dialog.dismiss()
//        }
//        dialogDeviceAdmin.btnAccept.setOnClickListener {
//            dialog.dismiss()
//            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
//            val componentName = ComponentName(this, AdminReceiver::class.java)
//            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
//            intent.putExtra(
//                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
//                "Enable admin to enable anti uninstall."
//            )
//            startActivity(intent, options.toBundle())
//
//        }
//    }

//    private fun makeDrawOverOtherAppsDialog() { // Removed old permission dialog
//        val dialogDisplayOverOtherApps =
//            DialogPermissionInfoBinding.inflate(layoutInflater)
//        dialogDisplayOverOtherApps.title.text =
//            getString(R.string.enable_2, "Display Over Other Apps")
//        dialogDisplayOverOtherApps.desc.text = getString(R.string.device_perm_draw_over_other_apps)
//        dialogDisplayOverOtherApps.point1.text = getString(R.string.show_time_elapsed_on_phone)
//        dialogDisplayOverOtherApps.point2.text =
//            getString(R.string.calculate_how_many_reels_tiktok_short_videos_you_scroll_per_day)
//        dialogDisplayOverOtherApps.point4.text = getString(R.string.plan_a_robbery)
//        val dialog = MaterialAlertDialogBuilder(this)
//            .setView(dialogDisplayOverOtherApps.root)
//            .show()
//
//        dialogDisplayOverOtherApps.btnReject.setOnClickListener {
//            dialog.dismiss()
//        }
//        dialogDisplayOverOtherApps.btnAccept.setOnClickListener {
//            dialog.dismiss()
//            Toast.makeText(
//                this,
//                getString(R.string.find_deenshield_and_press_enable), Toast.LENGTH_LONG
//            ).show()
//            val intent = Intent(
//                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
//                Uri.parse("package:$packageName")
//            )
//            startActivity(intent, options.toBundle())
//
//        }
//    }

    // makeShizukuInfoDialog removed - Shizuku dependency removed

//    private fun makeAccessibilityInfoDialog(title: String, cls: Class<*>) { // Removed old permission dialog
//        if (isAccessibilityServiceEnabled(DeenShieldAccessibilityService::class.java)) {
//            return
//        }
//        val dialogAccessibilityServiceInfoBinding =
//            DialogPermissionInfoBinding.inflate(layoutInflater)
//        dialogAccessibilityServiceInfoBinding.title.text = getString(R.string.enable_2, title)
//
//        val dialog = MaterialAlertDialogBuilder(this)
//            .setView(dialogAccessibilityServiceInfoBinding.root)
//            .show()
//
//        dialogAccessibilityServiceInfoBinding.btnReject.setOnClickListener {
//            dialog.dismiss()
//        }
//        dialogAccessibilityServiceInfoBinding.btnAccept.setOnClickListener {
//            Toast.makeText(this, "Find \'$title\' and press enable", Toast.LENGTH_LONG).show()
//            openAccessibilityServiceScreen(cls)
//            dialog.dismiss()
//        }
//        dialogAccessibilityServiceInfoBinding.btnGuide.visibility = View.VISIBLE
//        dialogAccessibilityServiceInfoBinding.btnGuide.setOnClickListener {
//            val intent = Intent(this, FragmentActivity::class.java)
//            intent.putExtra("fragment", AccessibilityGuide.FRAGMENT_ID)
//            startActivity(intent, options.toBundle())
//        }
//    }


    private fun createFocusModeShortcut() {

        val sp = getSharedPreferences("shortcuts",Context.MODE_PRIVATE)
        if(sp.getBoolean("focus_mode",false)){
            return
        }
        val intent = Intent(this, ShortcutActivity::class.java).apply {
            action = Intent.ACTION_CREATE_SHORTCUT
        }
        val shortcutInfo = ShortcutInfoCompat.Builder(this, "deenshield_focus_mode")
            .setShortLabel(getString(R.string.focus_mode))
            .setLongLabel(getString(R.string.focus_mode))
            .setIntent(intent)
            .setIcon(IconCompat.createWithResource(this, R.drawable.ic_focus_mode))
            .build()


        val supported = ShortcutManagerCompat.isRequestPinShortcutSupported(this)
        val dynamicShortcuts = ShortcutManagerCompat.getDynamicShortcuts(this)

        if(supported){
            if(dynamicShortcuts.contains(shortcutInfo)){
                return
            }
        }
        MaterialAlertDialogBuilder(this).apply {
            setTitle("Add Focus Mode to Home Screen")
            setMessage("Would you like to add Focus Mode to your home screen for quick access?")
            setPositiveButton("Ok") { dialog, _ ->
                sp.edit().putBoolean("focus_mode",true).apply()
                val pinnedShortcutCallbackIntent = Intent("example.intent.action.SHORTCUT_CREATED")

                val successCallback = PendingIntent.getBroadcast(
                    this@MainActivity,
                    1000,
                    pinnedShortcutCallbackIntent,
                    FLAG_IMMUTABLE
                )

                ShortcutManagerCompat.requestPinShortcut(
                    this@MainActivity,
                    shortcutInfo,
                    successCallback.intentSender
                )

            }
            setNegativeButton("Cancel", { _,_ ->
                sp.edit().putBoolean("focus_mode",false).apply()
            })
            show()
        }

    }

//    private fun openAccessibilityServiceScreen(cls: Class<*>) { // Removed old permission handling
//        try {
//            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
//            val componentName = ComponentName(this, cls)
//            intent.putExtra(":settings:fragment_args_key", componentName.flattenToString())
//            val bundle = Bundle()
//            bundle.putString(":settings:fragment_args_key", componentName.flattenToString())
//            intent.putExtra(":settings:show_fragment_args", bundle)
//            startActivity(intent, options.toBundle())
//        } catch (e: Exception) {
//            e.printStackTrace()
//            // Fallback to general Accessibility Settings
//            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
//        }
//    }

    @SuppressLint("ApplySharedPref")
    private fun makeRemoveAntiUninstallDialog() {
        val antiUninstallInfo = getSharedPreferences("anti_uninstall", Context.MODE_PRIVATE)
        val mode = antiUninstallInfo.getInt("mode", -1)
        when (mode) {

            Constants.ANTI_UNINSTALL_TIMED_MODE -> {
                val dateString = antiUninstallInfo.getString("date", null)
                val parts: List<String> = dateString!!.split("/")
                val selectedDate = Calendar.getInstance()
                selectedDate.set(
                    Integer.parseInt(parts[2]),  // Year
                    Integer.parseInt(parts[0]) - 1,  // Month (0-based)
                    Integer.parseInt(parts[1])  // Day
                )


                val today = Calendar.getInstance()

                val daysDiff =
                    (selectedDate.timeInMillis - today.timeInMillis) / (1000 * 60 * 60 * 24)
                if (selectedDate.before(today) || daysDiff.toInt() == 0) {
                    Snackbar.make(
                        binding.root,
                        getString(R.string.anti_uninstall_removed),
                        Snackbar.LENGTH_SHORT
                    )
                        .show()
                    antiUninstallInfo.edit().putBoolean("is_anti_uninstall_on", false).commit()
                    sendRefreshRequest(DeenShieldAccessibilityService.INTENT_ACTION_REFRESH_APP_BLOCKER)

                } else {

                    MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.failed))
                        .setMessage(getString(R.string.remaining_time_anti_uninstall, daysDiff))
                        .setPositiveButton("Ok", null)
                        .show()
                }

            }

            Constants.ANTI_UNINSTALL_PASSWORD_MODE -> {
                val dialogRemoveAntiUninstall =
                    DialogRemoveAntiUninstallBinding.inflate(layoutInflater)
                MaterialAlertDialogBuilder(this)
                    .setTitle(getString(R.string.remove_anti_uninstall))
                    .setView(dialogRemoveAntiUninstall.root)
                    .setPositiveButton(R.string.remove) { _, _ ->
                        val entered = dialogRemoveAntiUninstall.password.text.toString()
                        val stored = antiUninstallInfo.getString("password", null)
                        if (com.alhaq.deenshield.utils.PasswordHasher.verify(entered, stored)) {
                            // Upgrade legacy plaintext on the way out (defense in depth: even
                            // though we are removing protection, leave no plaintext behind).
                            if (com.alhaq.deenshield.utils.PasswordHasher.isPlainText(stored)) {
                                antiUninstallInfo.edit()
                                    .putString(
                                        "password",
                                        com.alhaq.deenshield.utils.PasswordHasher.hash(entered)
                                    )
                                    .apply()
                            }
                            antiUninstallInfo.edit().putBoolean("is_anti_uninstall_on", false)
                                .commit()
                            sendRefreshRequest(DeenShieldAccessibilityService.INTENT_ACTION_REFRESH_APP_BLOCKER)

                            Snackbar.make(
                                binding.root,
                                "Anti Uninstall removed",
                                Snackbar.LENGTH_SHORT
                            )
                                .show()

                            // checkPermissions() // Removed old permission check
                        } else {
                            Snackbar.make(
                                binding.root,
                                getString(R.string.incorrect_password_please_try_again),
                                Snackbar.LENGTH_SHORT
                            )
                                .setAction(getString(R.string.retry)) {
                                    makeRemoveAntiUninstallDialog()
                                }
                                .show()
                        }
                    }
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show()
            }
        }

    }
    private fun createFileInDirectory(directoryUri: Uri, filename: String): Uri? {
        return try {
            val docTree = DocumentFile.fromTreeUri(this, directoryUri)
            docTree?.createFile("application/zip", filename)?.uri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    data class WarningData(
        val message: String = "You can setup a custom message to appear here!",
        val timeInterval: Int = 120000, // default cooldown period
        val isDynamicIntervalSettingAllowed: Boolean = false,
        val isProceedDisabled: Boolean = false,
        val isWarningDialogHidden: Boolean = false, // perform back/home action directly without showing warning screen
        val proceedDelayInSecs: Int = 15
    )

    private fun showAboutDialog() {
        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) {
            "Unknown"
        }
        
        val aboutMessage = """
            DeenShield v$versionName
            
            A comprehensive Islamic digital wellbeing app designed to help you maintain focus, develop healthy digital habits, and protect yourself from distracting content.
            
            Features:
            • App Blocker - Block apps by category with smart controls
            • Reel Blocker - Limit endless scrolling on Reels, Shorts, and TikTok
            • Keyword Blocker - Detect and block inappropriate keywords
            • Focus Mode - Time-boxed app restrictions with tracking
            • Notifications Inbox - View your blocking activity notifications
            • Usage Tracker - Monitor your digital habits
            • Anti-Uninstall Protection - Secure your protection settings
            • Privacy Focused - All processing happens on your device
            
            Developed by Al-Haq Initiative
            
            No Ads • No Tracking • Islamic Values
        """.trimIndent()
        
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.about))
            .setMessage(aboutMessage)
            .setPositiveButton("OK", null)
            .setNeutralButton(getString(R.string.join_telegram)) { _, _ ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/deenshield"))
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(this, "Could not open link", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun setupNavigationDrawer() {
        val navigationView = binding.navView
        drawerToggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
        drawerToggle.drawerArrowDrawable.color =
            ContextCompat.getColor(this, R.color.md_theme_onSurface)
        
        // Update header with current sign-in status
        val account = googleSignInHelper.getLastSignedInAccount()
        updateNavigationHeader(account)
        
        // Set up navigation item selection
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_profile -> {
                    if (googleSignInHelper.isSignedIn()) {
                        openProfileScreen()
                    } else {
                        Toast.makeText(this, getString(R.string.sign_in_with_google), Toast.LENGTH_SHORT).show()
                    }
                }
                R.id.nav_sign_in -> {
                    signInWithGoogle()
                }
                R.id.nav_sign_out -> {
                    signOut()
                }
                R.id.nav_premium -> {
                    openPremiumScreen()
                }
                R.id.nav_donate -> {
                    showSupportOptionsDialog()
                }
                R.id.nav_reports -> {
                    val intent = Intent(this, ReportsActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_faq -> {
                    showFAQDialog()
                }
                R.id.nav_feedback -> {
                    showFeedbackDialog()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }
    
    private fun updateNavigationHeader(account: GoogleSignInAccount?) {
        val headerView = binding.navView.getHeaderView(0)
        val usernameView = headerView.findViewById<android.widget.TextView>(R.id.nav_header_username)
        val emailView = headerView.findViewById<android.widget.TextView>(R.id.nav_header_email)
        val profileImageView = headerView.findViewById<android.widget.ImageView>(R.id.nav_header_profile_image)

        ensureDrawerBranding(headerView, profileImageView, account)
        
        val menu = binding.navView.menu
        val signInItem = menu.findItem(R.id.nav_sign_in)
        val signOutItem = menu.findItem(R.id.nav_sign_out)
        val premiumItem = menu.findItem(R.id.nav_premium)
        
        if (account != null) {
            usernameView.text = account.displayName ?: account.email?.split("@")?.get(0) ?: getString(R.string.guest_user)
            emailView.text = account.email ?: getString(R.string.not_signed_in)
            // Could load profile photo here with Glide or similar
            signInItem.isVisible = false
            signOutItem.isVisible = true
            premiumItem.title = getString(if (premiumManager.isPremium()) R.string.premium_nav_manage else R.string.premium_nav_upgrade)
            if (profileImageView.drawable == null && brandLogoBitmap != null) {
                profileImageView.setImageBitmap(brandLogoBitmap)
            }
        } else {
            usernameView.text = getString(R.string.guest_user)
            emailView.text = getString(R.string.not_signed_in)
            if (brandLogoBitmap != null) {
                profileImageView.setImageBitmap(brandLogoBitmap)
            }
            signInItem.isVisible = true
            signOutItem.isVisible = false
            premiumItem.title = getString(R.string.premium_nav_upgrade)
        }
    }
    
    private fun ensureDrawerBranding(
        headerView: View,
        profileImageView: android.widget.ImageView,
        account: GoogleSignInAccount?
    ) {
        drawerBannerDrawable?.let { ViewCompat.setBackground(headerView, it) }
        if (account == null && brandLogoBitmap != null) {
            profileImageView.setImageBitmap(brandLogoBitmap)
        }

        val needsBanner = drawerBannerDrawable == null
        val needsLogo = brandLogoBitmap == null

        if (!needsBanner && (!needsLogo || account != null)) {
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val banner = if (needsBanner) loadDrawerBannerDrawable() else null
            val logo = if (needsLogo) loadBrandLogoBitmap() else null
            withContext(Dispatchers.Main) {
                banner?.let {
                    drawerBannerDrawable = it
                    ViewCompat.setBackground(headerView, it)
                }
                if (logo != null && brandLogoBitmap == null) {
                    brandLogoBitmap = logo
                }
                if (account == null) {
                    brandLogoBitmap?.let { profileImageView.setImageBitmap(it) }
                }
            }
        }
    }

    private fun loadBrandLogoBitmap(): Bitmap? {
        return runCatching {
            assets.open(BRAND_LOGO_ASSET_PATH).use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        }.getOrNull()
    }

    private fun loadDrawerBannerDrawable(): Drawable? {
        return runCatching {
            assets.open(BRAND_BANNER_ASSET_PATH).use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                BitmapDrawable(resources, bitmap)
            }
        }.getOrNull()
    }

    /**
     * Automatically restore premium purchases on app start.
     * This ensures premium status persists across:
     * - App restarts
     * - Device reboots  
     * - App reinstalls
     * - Device changes
     * Works for both real purchases and test purchases (License Test accounts)
     */
    private fun restorePremiumPurchases() {
        // Don't query if already premium
        if (premiumManager.isPremium()) {
            return
        }
        
        val billingWrapper = BillingClientWrapper(this)
        billingWrapper.startConnection {
            billingWrapper.queryPurchases { purchases: List<com.android.billingclient.api.Purchase> ->
                if (purchases.isNotEmpty()) {
                    // User has active purchases - restore premium status
                    premiumManager.updatePremiumStatus(true)
                    android.util.Log.d("MainActivity", "Premium status restored from purchases")
                }
            }
        }
    }

    private fun maybeShowPremiumReminder() {
        if (!premiumManager.isPremium() && premiumManager.shouldShowReminder()) {
            premiumManager.markReminderShown()
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.premium_reminder_title))
                .setMessage(getString(R.string.premium_reminder_message))
                .setPositiveButton(R.string.premium_view_plans) { _, _ ->
                    openPremiumScreen()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun openPremiumScreen() {
        val intent = Intent(this, FragmentActivity::class.java).apply {
            putExtra("feature_type", "premium_features")
        }
        startActivity(intent)
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInHelper.getSignInIntent()
        googleSignInLauncher.launch(signInIntent)
    }
    
    private fun signOut() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.sign_out))
            .setMessage(getString(R.string.sign_out_confirmation))
            .setPositiveButton(getString(R.string.sign_out)) { _, _ ->
                googleSignInHelper.signOut {
                    updateNavigationHeader(null)
                    Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun openProfileScreen() {
        val intent = Intent(this, FragmentActivity::class.java)
        intent.putExtra("fragment", "profile")
        startActivity(intent)
    }
    
    private fun showFAQDialog() {
        val faqItems = arrayOf(
            "How do I enable accessibility services?" to "Go to Settings → Accessibility → DeenShield, then enable the required services. This is needed for all blocking features to work.",
            "What is the Notifications bell icon?" to "The bell icon shows your notification inbox with blocking alerts, daily reports, reminders, and achievements. Tap it to view your notification history.",
            "How does Reel Blocker work?" to "Reel Blocker detects and blocks endless scrolling on Instagram Reels, YouTube Shorts, and TikTok videos, helping you maintain focus.",
            "Why do my blocked apps/keywords disappear?" to "Make sure accessibility services stay enabled. Some system optimizations may disable them. You can check status in Settings.",
            "Can I export my settings?" to "Yes! Go to Settings → Backup & Restore to export/import your configuration.",
            "What is Focus Mode?" to "Focus Mode lets you time-box app restrictions (e.g., block gaming apps for 2 hours). It tracks your focus sessions and shows productivity insights.",
            "How do I disable Anti-Uninstall protection?" to "Go to Settings → Anti-Uninstall, enter your password, and tap Disable. You can then uninstall DeenShield normally.",
            "Is DeenShield really privacy-focused?" to "Yes! All text analysis, keyword detection, and content blocking happens locally on your device. We never send your data to servers."
        )
        
        val questions = faqItems.map { it.first }.toTypedArray()
        
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.faq))
            .setItems(questions) { _, which ->
                MaterialAlertDialogBuilder(this)
                    .setTitle(faqItems[which].first)
                    .setMessage(faqItems[which].second)
                    .setPositiveButton(getString(R.string.ok), null)
                    .show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun showFeedbackDialog() {
        val input = android.widget.EditText(this).apply {
            hint = getString(R.string.feedback_hint)
            minLines = 4
            maxLines = 8
            setPadding(64, 32, 64, 32)
        }
        
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.send_feedback))
            .setView(input)
            .setPositiveButton(getString(R.string.send_feedback)) { _, _ ->
                val feedbackText = input.text.toString()
                if (feedbackText.isNotBlank()) {
                    sendFeedbackEmail(feedbackText)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun sendFeedbackEmail(feedback: String) {
        val account = googleSignInHelper.getLastSignedInAccount()
        val userEmail = account?.email
        val userName = account?.displayName ?: "Anonymous"
        val errorManager = ErrorReportManager.getInstance(this)

        errorManager.saveFeedback(
            UserFeedback(
                category = "General",
                message = feedback,
                rating = 3,
                email = userEmail,
                deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL} / Android ${Build.VERSION.RELEASE}"
            )
        )

        val emailBody = buildString {
            append("From: ")
            append(userName)
            append("\n")
            append("Device type or model: ")
            append("${Build.MANUFACTURER} ${Build.MODEL}")
            append("\n")
            append("Issue or Feedback:\n")
            append(feedback)
        }

        val attachmentFile = errorManager.createBundledReportFile(prefixText = emailBody)
        if (attachmentFile == null) {
            Toast.makeText(this, getString(R.string.feedback_error), Toast.LENGTH_SHORT).show()
            return
        }
        val attachmentUri = FileProvider.getUriForFile(
            this,
            "$packageName.provider",
            attachmentFile
        )

        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_subject))
            putExtra(Intent.EXTRA_TEXT, emailBody)
            putExtra(Intent.EXTRA_CC, SUPPORT_CC_ADDRESSES)
            putExtra(Intent.EXTRA_STREAM, attachmentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            selector = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
        }

        try {
            startActivity(Intent.createChooser(emailIntent, getString(R.string.send_feedback)))
            Toast.makeText(this, getString(R.string.feedback_sent), Toast.LENGTH_SHORT).show()
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, getString(R.string.feedback_error), Toast.LENGTH_SHORT).show()
        }
    }

    // onBackPressed override removed; handled by OnBackPressedDispatcher in onCreate.

    private fun initializeNotificationChannels() {
        val notificationManager = com.alhaq.deenshield.utils.NotificationManager(this)
        notificationManager.createNotificationChannels()
    }
    
    private fun scheduleDailyReportsIfPremium() {
        // Daily report notifications will be implemented in future update with WorkManager
        // For now, users can manually access reports from the Reports tab
    }

    private companion object {
        private val SUPPORT_CC_ADDRESSES = arrayOf(
            "support@alhaq-initiative.org",
            "alhaq.dst@gmail.com"
        )
        private const val BRAND_LOGO_ASSET_PATH = "icons/Deenshield_Transparent_bg.png"
        private const val BRAND_BANNER_ASSET_PATH = "icons/Blue and Pink Trendy Gradient Technology X-Frame Banner_20251028_204729_0000.png"
    }
}
