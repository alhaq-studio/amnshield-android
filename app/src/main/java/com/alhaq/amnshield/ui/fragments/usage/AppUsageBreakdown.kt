package com.alhaq.amnshield.ui.fragments.usage

import android.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder

import com.alhaq.amnshield.data.blockers.AppBlockScheduleRule
import com.alhaq.amnshield.databinding.FragmentAppUsageBreakdownBinding
import com.alhaq.amnshield.services.AmnShieldAccessibilityService
import com.alhaq.amnshield.utils.SavedPreferencesLoader
import com.alhaq.amnshield.utils.TimeTools
import java.time.Duration
import java.util.UUID

class AppUsageBreakdown(private val stat: AllAppsUsageFragment.Stat) : Fragment() {


    private var _binding: FragmentAppUsageBreakdownBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentAppUsageBreakdownBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupLineChart(binding.lineChart)
        plotUsageData()

        try {
            val appInfo = requireContext().packageManager.getApplicationInfo(stat.packageName, 0)
            binding.appName.text = appInfo.loadLabel(requireContext().packageManager)

            binding.appIcon.setImageDrawable(appInfo.loadIcon(requireContext().packageManager))
        } catch (_: Exception) {
        }
        binding.screentime.text = TimeTools.formatTime(stat.totalTime, false)
        binding.sessions.text = stat.sessions.size.toString()

        // Setup launch limit card
        binding.appOpensCard.setOnClickListener {
            showSetLaunchLimitDialog()
        }

        setupRecommendations()
    }

    private fun setupRecommendations() {
        val usageMinutes = (stat.totalTime / (1000L * 60L)).toInt()
        val sessionsCount = stat.sessions.size

        val recommendation = when {
            usageMinutes >= 240 || sessionsCount >= 35 ->
                "High uncontrolled usage detected. Consider enabling Always Block or a strict weekly block schedule."
            usageMinutes >= 120 || sessionsCount >= 20 ->
                "Moderate risk usage pattern. A daily block window with limited cheat hours is recommended."
            usageMinutes >= 60 || sessionsCount >= 12 ->
                "Usage is rising. Set soft time windows now to prevent escalation."
            else ->
                "Usage looks controlled. You can still pre-configure schedules for consistency."
        }

        binding.recommendationBody.text = recommendation

        binding.btnBlockThisApp.setOnClickListener {
            showBlockListActionDialog()
        }

        binding.btnConfigureSchedule.setOnClickListener {
            openCreateRuleForThisApp()
        }

        binding.btnManageSchedules.setOnClickListener {
            openManageSchedulesForApp()
        }
    }

    private fun showBlockListActionDialog() {
        val actions = arrayOf(
            "Instant Quick-Block / Unblock",
            "Add to Existing Block Schedule",
            "Create New Schedule Rule for App"
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Block options for ${getAppName()}")
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> toggleInstantBlock()
                    1 -> addToExistingScheduleRule()
                    2 -> openCreateRuleForThisApp()
                }
            }
            .show()
    }

    private fun toggleInstantBlock() {
        val loader = SavedPreferencesLoader(requireContext())
        val blocked = loader.loadBlockedApps().toMutableSet()
        val isCurrentlyBlocked = blocked.contains(stat.packageName)

        if (isCurrentlyBlocked) {
            blocked.remove(stat.packageName)
            loader.saveBlockedApps(blocked)
            sendAppBlockerRefresh()
            Toast.makeText(requireContext(), "${getAppName()} removed from blocked apps", Toast.LENGTH_SHORT).show()
        } else {
            blocked.add(stat.packageName)
            loader.saveBlockedApps(blocked)
            sendAppBlockerRefresh()
            Toast.makeText(requireContext(), "${getAppName()} added to blocked apps", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addToExistingScheduleRule() {
        val loader = SavedPreferencesLoader(requireContext())
        val rules = loader.loadAppBlockerScheduleRules()
            .filter { it.packageName != "keyword_blocker" && it.packageName != "website_blocker" && it.packageName != "reel_blocker" && it.packageName != "FOCUS_MODE" }

        if (rules.isEmpty()) {
            Toast.makeText(requireContext(), "No existing rules found. Create a new rule below.", Toast.LENGTH_SHORT).show()
            openCreateRuleForThisApp()
            return
        }

        val distinctGroups = rules.groupBy { it.groupId ?: it.id }
        val groupTitles = distinctGroups.map { (_, ruleList) ->
            ruleList.firstOrNull()?.groupTitle ?: ruleList.firstOrNull()?.title ?: "Schedule Rule"
        }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select schedule rule")
            .setItems(groupTitles) { _, index ->
                val selectedGroupId = distinctGroups.keys.elementAt(index)
                val targetRule = distinctGroups[selectedGroupId]?.firstOrNull()

                if (targetRule != null) {
                    val newRule = targetRule.copy(
                        id = UUID.randomUUID().toString(),
                        packageName = stat.packageName
                    )
                    loader.upsertAppBlockerScheduleRule(newRule)

                    val blocked = loader.loadBlockedApps().toMutableSet()
                    blocked.add(stat.packageName)
                    loader.saveBlockedApps(blocked)

                    sendAppBlockerRefresh()
                    Toast.makeText(requireContext(), "Added ${getAppName()} to ${groupTitles[index]}", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun openCreateRuleForThisApp() {
        val intent = android.content.Intent(requireContext(), com.alhaq.amnshield.ui.activity.FragmentActivity::class.java).apply {
            putExtra("fragment", com.alhaq.amnshield.ui.fragments.BlocksManagerFragment.FRAGMENT_ID)
            putExtra("action", "create")
            putExtra("prefill_target", "APP_BLOCKER")
            putExtra("prefill_app", stat.packageName)
        }
        startActivity(intent)
    }

    private fun openManageSchedulesForApp() {
        val intent = android.content.Intent(requireContext(), com.alhaq.amnshield.ui.activity.FragmentActivity::class.java).apply {
            putExtra("fragment", com.alhaq.amnshield.ui.fragments.BlocksManagerFragment.FRAGMENT_ID)
            putExtra("filter_type", "App Blocker")
        }
        startActivity(intent)
    }

    private fun getAppName(): String {
        return try {
            val appInfo = requireContext().packageManager.getApplicationInfo(stat.packageName, 0)
            appInfo.loadLabel(requireContext().packageManager).toString()
        } catch (_: Exception) {
            stat.packageName
        }
    }

    private fun sendAppBlockerRefresh() {
        requireContext().sendBroadcast(
            android.content.Intent(AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_APP_BLOCKER)
                .setPackage(requireContext().packageName)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupLineChart(lineChart: LineChart) {
        lineChart.apply {
            description.isEnabled = false
            legend.isEnabled = true

            // Enable touch gestures
            setTouchEnabled(true)
            setPinchZoom(true)

            // Configure X axis
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                labelRotationAngle = 45f
                valueFormatter = HourAxisFormatter()
            }

            // Configure Y axis
            axisLeft.apply {
                valueFormatter = MinutesAxisFormatter()
                axisMinimum = 0f
            }
            axisRight.isEnabled = false

            // Animate chart
            animateX(1000)
        }
    }

    private fun plotUsageData() {
        val hourlyUsage = MutableList(24) { 0L }

        stat.sessions.forEach { session ->
            var cursor = session.startTime
            var remainingMillis = session.durationMillis.coerceAtLeast(0L)

            while (remainingMillis > 0) {
                val nextHour = cursor
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0)
                    .plusHours(1)
                val millisUntilNextHour = Duration.between(cursor, nextHour)
                    .toMillis()
                    .coerceAtLeast(1L)
                val chunkMillis = minOf(remainingMillis, millisUntilNextHour)
                hourlyUsage[cursor.hour] = hourlyUsage[cursor.hour] + chunkMillis
                cursor = cursor.plusNanos(chunkMillis * 1_000_000)
                remainingMillis -= chunkMillis
            }
        }

        val entries = hourlyUsage.mapIndexed { hour, millis ->
            Entry(hour.toFloat(), millis / (1000f * 60f))
        }

        // Create and configure the dataset
        val dataSet = LineDataSet(entries, "Usage (minutes)")

        setupChartUI(binding.lineChart,dataSet)
    }



    private fun setupChartUI(
        chart: LineChart,
        lineDataSet: LineDataSet
    ) {

        val primaryColor = MaterialColors.getColor(
            requireContext(), R.attr.colorPrimary, ContextCompat.getColor(
                requireContext(),
                com.alhaq.amnshield.R.color.text_color
            )
        )
        lineDataSet.apply {
            color = primaryColor
            valueTextColor = primaryColor
            lineWidth = 3f
            setDrawCircles(false)

            setDrawValues(false)

            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f
        }

        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            labelCount = 5
            setDrawGridLines(false) // Disable vertical grid lines
            textColor = primaryColor
        }

        chart.axisLeft.apply {
            isEnabled = false
            setDrawGridLines(false)
            textColor = primaryColor
        }

        chart.apply {
            axisRight.isEnabled = false
            legend.isEnabled = false
            description.isEnabled = false
            animateY(800, Easing.EaseInCubic)

            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)

            setPinchZoom(false)

            data = LineData(lineDataSet)

        }
        chart.invalidate()
    }

    private class HourAxisFormatter : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            val hour = value.toInt()
            return String.format("%02d:00", hour)
        }
    }

    private class MinutesAxisFormatter : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return "${value.toInt()} min"
        }
    }

    private class MinutesValueFormatter : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            if (value == 0f) return ""
            return "${value.toInt()}m"
        }
    }

    private fun showSetLaunchLimitDialog() {
        try {
            val appInfo = requireContext().packageManager.getApplicationInfo(stat.packageName, 0)
            val appName = appInfo.loadLabel(requireContext().packageManager).toString()

            val dialog = com.alhaq.amnshield.ui.dialogs.SetLaunchLimitDialog(
                packageName = stat.packageName,
                appName = appName,
                onSave = { rule ->
                    if (rule != null) {
                        SavedPreferencesLoader(requireContext()).addAppLaunchLimitRule(rule)
                        Toast.makeText(
                            requireContext(),
                            "Launch limit set: ${rule.getDescription()}",
                            Toast.LENGTH_SHORT
                        ).show()
                        // Send refresh to accessibility service so it can track launches
                        sendAppBlockerRefresh()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Launch limit removed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
            dialog.show(childFragmentManager, "set_launch_limit_${stat.packageName}")
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error opening launch limit dialog", Toast.LENGTH_SHORT).show()
        }
    }
}
