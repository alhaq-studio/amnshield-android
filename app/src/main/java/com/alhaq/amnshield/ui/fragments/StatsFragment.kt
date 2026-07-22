package com.alhaq.amnshield.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.alhaq.amnshield.R
import com.alhaq.amnshield.premium.PremiumManager
import com.alhaq.amnshield.ui.activity.FragmentActivity
import com.alhaq.amnshield.ui.activity.MainActivity
import com.alhaq.amnshield.ui.fragments.usage.AllAppsUsageFragment
import com.alhaq.amnshield.ui.screens.AppUsageItem
import com.alhaq.amnshield.ui.screens.StatsScreen
import com.alhaq.amnshield.ui.theme.AmnShieldTheme
import com.alhaq.amnshield.utils.BlockingStatsManager
import com.alhaq.amnshield.utils.UsageStatsHelper
import com.alhaq.amnshield.utils.SavedPreferencesLoader
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

import androidx.lifecycle.ViewModelProvider
import com.alhaq.amnshield.ui.viewmodel.AmnShieldViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

class StatsFragment : Fragment() {

    private val premiumManager by lazy { PremiumManager.getInstance(requireContext()) }
    private val savedPreferencesLoader by lazy { SavedPreferencesLoader(requireContext()) }
    private lateinit var viewModel: AmnShieldViewModel

    // Compose States
    private val totalScreenTimeState = mutableStateOf("0h 0m")
    private val distractionsBlockedState = mutableStateOf(0)
    private val focusTimeState = mutableStateOf("0m")
    private val totalReelsWatchedState = mutableStateOf(0)
    private val averageWatchSecondsState = mutableStateOf(0)
    private val topAppsState = mutableStateListOf<AppUsageItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[AmnShieldViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val activeTheme = com.alhaq.amnshield.utils.ThemeUtils.resolveAppTheme(requireContext())
                viewModel.updateTheme(activeTheme)
                val state by viewModel.state.collectAsState()
                AmnShieldTheme(appTheme = activeTheme) {
                    StatsScreen(
                        totalScreenTime = totalScreenTimeState.value,
                        distractionsBlocked = distractionsBlockedState.value,
                        focusTime = focusTimeState.value,
                        totalReelsWatched = totalReelsWatchedState.value,
                        averageWatchSeconds = averageWatchSecondsState.value,
                        topApps = topAppsState,
                        onRefresh = { loadStats() },
                        onViewDetailedUsage = {
                            val intent = Intent(requireContext(), FragmentActivity::class.java)
                            intent.putExtra("fragment", AllAppsUsageFragment.FRAGMENT_ID)
                            val options = ActivityOptionsCompat.makeCustomAnimation(
                                requireContext(),
                                R.anim.fade_in,
                                R.anim.fade_out
                            )
                            startActivity(intent, options.toBundle())
                        },
                        onViewReelsMetrics = {
                            (activity as? MainActivity)?.selectTab(R.id.navigation_blocks)
                        },
                        onAppClick = { packageName ->
                            val intent = Intent(requireContext(), FragmentActivity::class.java)
                            intent.putExtra("fragment", AllAppsUsageFragment.FRAGMENT_ID)
                            intent.putExtra("target_package_name", packageName)
                            val options = ActivityOptionsCompat.makeCustomAnimation(
                                requireContext(),
                                R.anim.fade_in,
                                R.anim.fade_out
                            )
                            startActivity(intent, options.toBundle())
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadStats()
    }

    override fun onResume() {
        super.onResume()
        loadStats()
    }

    private fun showPremiumUpsell() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.premium_required_title)
            .setMessage(getString(R.string.premium_required_message))
            .setPositiveButton(R.string.premium_view_plans) { _, _ ->
                val intent = Intent(requireContext(), FragmentActivity::class.java).apply {
                    putExtra("feature_type", "premium_features")
                }
                startActivity(intent)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun loadStats() {
        lifecycleScope.launch {
            context?.let { ctx ->
                try {
                    val usageStatsHelper = UsageStatsHelper(ctx)

                    withContext(Dispatchers.IO) {
                        val calendar = Calendar.getInstance()
                        calendar.set(Calendar.HOUR_OF_DAY, 0)
                        calendar.set(Calendar.MINUTE, 0)
                        calendar.set(Calendar.SECOND, 0)
                        calendar.set(Calendar.MILLISECOND, 0)
                        val startTime = calendar.timeInMillis
                        val endTime = System.currentTimeMillis()

                        val statsList = usageStatsHelper.getForegroundStatsByTimestamps(startTime, endTime)
                        val deviceTotalTime = statsList.sumOf { it.totalTime }

                        // 1. Gather all launcher packages
                        val launcherIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
                        val resolveInfos = ctx.packageManager.queryIntentActivities(launcherIntent, 0)
                        val launcherPackages = resolveInfos.map { it.activityInfo.packageName }.toSet()

                        val systemPackages = setOf(
                            "android",
                            "com.android.systemui",
                            "com.android.settings",
                            "com.google.android.gms",
                            "com.google.android.inputmethod.latin",
                            "com.sec.android.inputmethod",
                            ctx.packageName
                        )

                        // 2. Filter stats: ignore system and launcher apps, and require a launch intent (user-facing)
                        val filteredStats = statsList.filter { stat ->
                            val pkg = stat.packageName
                            !systemPackages.contains(pkg) &&
                            !launcherPackages.contains(pkg) &&
                            pkg.isNotBlank() &&
                            ctx.packageManager.getLaunchIntentForPackage(pkg) != null
                        }

                        val sortedApps = filteredStats.sortedByDescending { it.totalTime }.take(5)

                        // 3. Resolve app labels and load icons on IO thread
                        val resolvedApps = sortedApps.map { appStat ->
                            val appName = try {
                                val appInfo = ctx.packageManager.getApplicationInfo(appStat.packageName, 0)
                                ctx.packageManager.getApplicationLabel(appInfo).toString()
                            } catch (e: Exception) {
                                appStat.packageName
                            }
                            val appTime = appStat.totalTime / (1000 * 60)
                            val appTimeFormatted = if (appTime >= 60) "${appTime / 60}h ${appTime % 60}m" else "${appTime}m"
                            val progress = (appTime.toFloat() / 120f).coerceIn(0f, 1f)
                            
                            val iconBitmap = getAppIconBitmap(ctx, appStat.packageName)
                            
                            AppUsageItem(appName, appStat.packageName, appTimeFormatted, progress, iconBitmap)
                        }

                        withContext(Dispatchers.Main) {
                            // Update screen time
                            val hours = deviceTotalTime / (1000 * 60 * 60)
                            val minutes = (deviceTotalTime % (1000 * 60 * 60)) / (1000 * 60)
                            totalScreenTimeState.value = "${hours}h ${minutes}m"

                            // Update blocking stats
                            val blockStats = BlockingStatsManager.getInstance(ctx).getTodayStats()
                            val totalBlocks = blockStats.appBlocksCount + blockStats.keywordBlocksCount + blockStats.viewBlocksCount
                            distractionsBlockedState.value = totalBlocks
                            focusTimeState.value = formatMinutes(blockStats.totalFocusMinutes)

                            // Update reels stats
                            val reelsScrolled = savedPreferencesLoader.getReelsScrolledToday()
                            val reelsWatchTime = savedPreferencesLoader.getReelsWatchTimeSeconds()
                            val avgWatch = if (reelsScrolled > 0) (reelsWatchTime / reelsScrolled).toInt() else 0

                            totalReelsWatchedState.value = reelsScrolled
                            averageWatchSecondsState.value = avgWatch

                            // Update top apps list
                            topAppsState.clear()
                            topAppsState.addAll(resolvedApps)
                        }
                    }
                } catch (t: Throwable) {
                    totalScreenTimeState.value = "0h 0m"
                    distractionsBlockedState.value = 0
                    focusTimeState.value = "0m"
                    topAppsState.clear()
                    android.util.Log.e("StatsFragment", "Failed to load stats", t)
                }
            }
        }
    }

    private fun getAppIconBitmap(context: android.content.Context, packageName: String): android.graphics.Bitmap? {
        return try {
            val pm = context.packageManager
            val iconDrawable = pm.getApplicationIcon(packageName)
            val size = 120
            val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            iconDrawable.setBounds(0, 0, size, size)
            iconDrawable.draw(canvas)
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun formatMinutes(totalMinutes: Long): String {
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return when {
            hours > 0 && mins > 0 -> String.format(Locale.getDefault(), "%dh %dm", hours, mins)
            hours > 0 -> String.format(Locale.getDefault(), "%dh", hours)
            else -> String.format(Locale.getDefault(), "%dm", mins)
        }
    }
}
