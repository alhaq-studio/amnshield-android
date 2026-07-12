package com.alhaq.amnshield.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.alhaq.amnshield.R
import com.alhaq.amnshield.databinding.FragmentStatsBinding
import com.alhaq.amnshield.premium.PremiumManager
import com.alhaq.amnshield.ui.activity.FragmentActivity
import com.alhaq.amnshield.ui.activity.ReportsActivity
import com.alhaq.amnshield.ui.fragments.usage.AllAppsUsageFragment
import com.alhaq.amnshield.utils.BlockingStatsManager
import com.alhaq.amnshield.utils.UsageStatsHelper
import com.alhaq.amnshield.utils.SavedPreferencesLoader
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!
    private val premiumManager by lazy { PremiumManager.getInstance(requireContext()) }
    private val savedPreferencesLoader by lazy { SavedPreferencesLoader(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        loadStats()
    }

    override fun onResume() {
        super.onResume()
        loadStats()
    }

    private fun setupClickListeners() {
        binding.btnRefreshStats.setOnClickListener {
            loadStats()
        }

        binding.btnViewDetails.setOnClickListener {
            val intent = Intent(requireContext(), FragmentActivity::class.java)
            intent.putExtra("fragment", AllAppsUsageFragment.FRAGMENT_ID)
            val options = ActivityOptionsCompat.makeCustomAnimation(
                requireContext(),
                com.alhaq.amnshield.R.anim.fade_in,
                com.alhaq.amnshield.R.anim.fade_out
            )
            startActivity(intent, options.toBundle())
        }

        binding.btnViewReports.setOnClickListener {
            val intent = Intent(requireContext(), ReportsActivity::class.java)
            val options = ActivityOptionsCompat.makeCustomAnimation(
                requireContext(),
                com.alhaq.amnshield.R.anim.fade_in,
                com.alhaq.amnshield.R.anim.fade_out
            )
            startActivity(intent, options.toBundle())
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

                        // Get usage stats using getForegroundStatsByTimestamps
                        val statsList = usageStatsHelper.getForegroundStatsByTimestamps(startTime, endTime)
                        val totalTime = statsList.sumOf { it.totalTime }

                        // Get top 3 apps
                        val sortedApps = statsList.sortedByDescending { it.totalTime }.take(3)

                        withContext(Dispatchers.Main) {
                            // Update screen time
                            val hours = totalTime / (1000 * 60 * 60)
                            val minutes = (totalTime % (1000 * 60 * 60)) / (1000 * 60)
                            binding.txtScreenTime.text = "${hours}h ${minutes}m"

                            // Update daily report summary with live blocking stats
                            val blockStats = BlockingStatsManager.getInstance(ctx).getTodayStats()
                            val totalBlocks = blockStats.appBlocksCount + blockStats.keywordBlocksCount + blockStats.viewBlocksCount
                            val summaryParts = mutableListOf<String>()
                            if (totalBlocks > 0) summaryParts.add("$totalBlocks blocks")
                            if (blockStats.focusSessionsCount > 0) summaryParts.add("${blockStats.focusSessionsCount} focus sessions")
                            if (blockStats.totalFocusMinutes > 0) summaryParts.add("${formatMinutes(blockStats.totalFocusMinutes)} focus time")
                            binding.dailyReportSummary.text = if (summaryParts.isNotEmpty()) {
                                summaryParts.joinToString(" · ")
                            } else {
                                "No activity recorded yet today."
                            }

                            // Update top apps
                            binding.txtTopApp1.text = "1. No usage yet"
                            binding.txtTopApp2.text = "2. No usage yet"
                            binding.txtTopApp3.text = "3. No usage yet"

                            if (sortedApps.isNotEmpty()) {
                                val app1 = sortedApps.getOrNull(0)
                                if (app1 != null) {
                                    val appName = try {
                                        val appInfo = ctx.packageManager.getApplicationInfo(app1.packageName, 0)
                                        ctx.packageManager.getApplicationLabel(appInfo).toString()
                                    } catch (e: Exception) {
                                        app1.packageName
                                    }
                                    val appTime = app1.totalTime / (1000 * 60)
                                    binding.txtTopApp1.text = "1. $appName - ${appTime}m"
                                }

                                val app2 = sortedApps.getOrNull(1)
                                if (app2 != null) {
                                    val appName = try {
                                        val appInfo = ctx.packageManager.getApplicationInfo(app2.packageName, 0)
                                        ctx.packageManager.getApplicationLabel(appInfo).toString()
                                    } catch (e: Exception) {
                                        app2.packageName
                                    }
                                    val appTime = app2.totalTime / (1000 * 60)
                                    binding.txtTopApp2.text = "2. $appName - ${appTime}m"
                                }

                                val app3 = sortedApps.getOrNull(2)
                                if (app3 != null) {
                                    val appName = try {
                                        val appInfo = ctx.packageManager.getApplicationInfo(app3.packageName, 0)
                                        ctx.packageManager.getApplicationLabel(appInfo).toString()
                                    } catch (e: Exception) {
                                        app3.packageName
                                    }
                                    val appTime = app3.totalTime / (1000 * 60)
                                    binding.txtTopApp3.text = "3. $appName - ${appTime}m"
                                }
                            }
                        }
                    }
                } catch (t: Throwable) {
                    binding.txtScreenTime.text = "0h 0m"
                    binding.dailyReportSummary.text = "Stats unavailable. Tap Refresh below."
                    binding.txtTopApp1.text = "1. No usage yet"
                    binding.txtTopApp2.text = "2. No usage yet"
                    binding.txtTopApp3.text = "3. No usage yet"
                    android.util.Log.e("StatsFragment", "Failed to load stats", t)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
