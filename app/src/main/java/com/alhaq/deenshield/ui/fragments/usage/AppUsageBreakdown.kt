package com.alhaq.deenshield.ui.fragments.usage

import android.R
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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

import com.alhaq.deenshield.data.blockers.AppBlockScheduleRule
import com.alhaq.deenshield.databinding.FragmentAppUsageBreakdownBinding
import com.alhaq.deenshield.services.DeenShieldAccessibilityService
import com.alhaq.deenshield.utils.SavedPreferencesLoader
import com.alhaq.deenshield.utils.TimeTools
import java.time.Duration
import java.util.Calendar
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
            showCreateOrEditRuleFlow(null)
        }

        binding.btnManageSchedules.setOnClickListener {
            showManageSchedulesDialog()
        }
    }

    private fun showBlockListActionDialog() {
        val actions = arrayOf(
            "Add to current blocked apps",
            "Add to existing named blocklist",
            "Create new blocklist and add"
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Block this app")
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> addToCurrentBlockedApps()
                    1 -> addToExistingBlockList()
                    2 -> createNewBlockListAndAdd()
                }
            }
            .show()
    }

    private fun addToCurrentBlockedApps() {
        val loader = SavedPreferencesLoader(requireContext())
        val blocked = loader.loadBlockedApps().toMutableSet()
        blocked.add(stat.packageName)
        loader.saveBlockedApps(blocked)
        sendAppBlockerRefresh()
        Toast.makeText(requireContext(), "Added to blocked apps", Toast.LENGTH_SHORT).show()
    }

    private fun addToExistingBlockList() {
        val loader = SavedPreferencesLoader(requireContext())
        val lists = loader.loadAppBlockLists()
        if (lists.isEmpty()) {
            createNewBlockListAndAdd()
            return
        }

        val names = lists.keys.sorted().toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select blocklist")
            .setItems(names) { _, index ->
                loader.addPackageToBlockList(names[index], stat.packageName)
                sendAppBlockerRefresh()
                Toast.makeText(requireContext(), "Added to ${names[index]}", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun createNewBlockListAndAdd() {
        val input = EditText(requireContext())
        input.hint = "e.g. High Risk Apps"

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Create blocklist")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text?.toString()?.trim().orEmpty()
                if (name.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter a blocklist name", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val loader = SavedPreferencesLoader(requireContext())
                loader.addPackageToBlockList(name, stat.packageName)
                sendAppBlockerRefresh()
                Toast.makeText(requireContext(), "Created $name and added app", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCreateOrEditRuleFlow(existingRule: AppBlockScheduleRule?) {
        val types = arrayOf("Block Hours", "Cheat Hours")
        val selectedType = when (existingRule?.type) {
            AppBlockScheduleRule.RuleType.CHEAT -> 1
            else -> 0
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Choose rule type")
            .setSingleChoiceItems(types, selectedType) { dialog, which ->
                val type = if (which == 1) AppBlockScheduleRule.RuleType.CHEAT else AppBlockScheduleRule.RuleType.BLOCK
                dialog.dismiss()
                showRecurrenceDialog(existingRule, type)
            }
            .show()
    }

    private fun showRecurrenceDialog(existingRule: AppBlockScheduleRule?, type: AppBlockScheduleRule.RuleType) {
        val options = arrayOf("Hourly", "Daily", "Weekly", "Always")
        val selected = when (existingRule?.recurrence) {
            AppBlockScheduleRule.Recurrence.HOURLY -> 0
            AppBlockScheduleRule.Recurrence.DAILY -> 1
            AppBlockScheduleRule.Recurrence.WEEKLY -> 2
            AppBlockScheduleRule.Recurrence.ALWAYS -> 3
            null -> 1
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Recurrence")
            .setSingleChoiceItems(options, selected) { dialog, which ->
                dialog.dismiss()
                when (which) {
                    0 -> configureHourlyRule(existingRule, type)
                    1 -> configureDailyRule(existingRule, type)
                    2 -> configureWeeklyRule(existingRule, type)
                    3 -> saveRule(
                        existingRule,
                        type,
                        AppBlockScheduleRule.Recurrence.ALWAYS,
                        startMinute = 0,
                        endMinute = 0,
                        selectedDays = emptySet(),
                        durationHours = 0,
                        activeUntilMillis = 0L
                    )
                }
            }
            .show()
    }

    private fun configureHourlyRule(existingRule: AppBlockScheduleRule?, type: AppBlockScheduleRule.RuleType) {
        val input = EditText(requireContext())
        input.hint = "Duration in hours"
        input.setText((existingRule?.durationHours ?: 2).toString())

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Hourly rule")
            .setMessage("Applies from now for N hours")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val hours = input.text?.toString()?.toIntOrNull()?.coerceIn(1, 168) ?: 2
                val end = System.currentTimeMillis() + (hours * 60L * 60L * 1000L)
                saveRule(
                    existingRule,
                    type,
                    AppBlockScheduleRule.Recurrence.HOURLY,
                    startMinute = 0,
                    endMinute = 0,
                    selectedDays = emptySet(),
                    durationHours = hours,
                    activeUntilMillis = end
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun configureDailyRule(existingRule: AppBlockScheduleRule?, type: AppBlockScheduleRule.RuleType) {
        pickTimeMinutes(existingRule?.startMinute ?: 9 * 60) { start ->
            pickTimeMinutes(existingRule?.endMinute ?: 18 * 60) { end ->
                saveRule(
                    existingRule,
                    type,
                    AppBlockScheduleRule.Recurrence.DAILY,
                    startMinute = start,
                    endMinute = end,
                    selectedDays = emptySet(),
                    durationHours = 0,
                    activeUntilMillis = 0L
                )
            }
        }
    }

    private fun configureWeeklyRule(existingRule: AppBlockScheduleRule?, type: AppBlockScheduleRule.RuleType) {
        val dayLabels = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val dayIds = arrayOf(
            Calendar.SUNDAY,
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY,
            Calendar.SATURDAY
        )

        val selected = BooleanArray(dayLabels.size) { idx ->
            existingRule?.selectedDays?.contains(dayIds[idx]) == true
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select days")
            .setMultiChoiceItems(dayLabels, selected) { _, which, isChecked ->
                selected[which] = isChecked
            }
            .setPositiveButton("Next") { _, _ ->
                val selectedDays = mutableSetOf<Int>()
                selected.forEachIndexed { index, checked ->
                    if (checked) selectedDays.add(dayIds[index])
                }
                if (selectedDays.isEmpty()) {
                    Toast.makeText(requireContext(), "Select at least one day", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                pickTimeMinutes(existingRule?.startMinute ?: 9 * 60) { start ->
                    pickTimeMinutes(existingRule?.endMinute ?: 18 * 60) { end ->
                        saveRule(
                            existingRule,
                            type,
                            AppBlockScheduleRule.Recurrence.WEEKLY,
                            startMinute = start,
                            endMinute = end,
                            selectedDays = selectedDays,
                            durationHours = 0,
                            activeUntilMillis = 0L
                        )
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun pickTimeMinutes(initialMinutes: Int, onPicked: (Int) -> Unit) {
        val hour = (initialMinutes / 60).coerceIn(0, 23)
        val minute = (initialMinutes % 60).coerceIn(0, 59)
        TimePickerDialog(requireContext(), { _, h, m ->
            onPicked(h * 60 + m)
        }, hour, minute, true).show()
    }

    private fun saveRule(
        existingRule: AppBlockScheduleRule?,
        type: AppBlockScheduleRule.RuleType,
        recurrence: AppBlockScheduleRule.Recurrence,
        startMinute: Int,
        endMinute: Int,
        selectedDays: Set<Int>,
        durationHours: Int,
        activeUntilMillis: Long
    ) {
        val loader = SavedPreferencesLoader(requireContext())
        val appName = binding.appName.text?.toString().orEmpty().ifBlank { stat.packageName }
        val title = "$appName • ${type.name} • ${recurrence.name}"

        val rule = AppBlockScheduleRule(
            id = existingRule?.id ?: UUID.randomUUID().toString(),
            title = title,
            packageName = stat.packageName,
            type = type,
            recurrence = recurrence,
            startMinute = startMinute,
            endMinute = endMinute,
            selectedDays = selectedDays,
            durationHours = durationHours,
            activeUntilMillis = activeUntilMillis,
            createdAt = existingRule?.createdAt ?: System.currentTimeMillis()
        )

        // Ensure app is in blocked set so schedule rules can govern it.
        val blocked = loader.loadBlockedApps().toMutableSet()
        blocked.add(stat.packageName)
        loader.saveBlockedApps(blocked)

        loader.upsertAppBlockerScheduleRule(rule)
        sendAppBlockerRefresh()
        Toast.makeText(requireContext(), "Schedule saved", Toast.LENGTH_SHORT).show()
    }

    private fun showManageSchedulesDialog() {
        val loader = SavedPreferencesLoader(requireContext())
        val rules = loader.loadAppBlockerScheduleRules().filter { it.packageName == stat.packageName }
        if (rules.isEmpty()) {
            Toast.makeText(requireContext(), "No schedules for this app", Toast.LENGTH_SHORT).show()
            return
        }

        val labels = rules.map { rule ->
            val days = if (rule.recurrence == AppBlockScheduleRule.Recurrence.WEEKLY && rule.selectedDays.isNotEmpty()) {
                " [${rule.selectedDays.sorted().joinToString(",")}]"
            } else ""
            "${rule.type.name} • ${rule.recurrence.name}$days"
        }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit schedules")
            .setItems(labels) { _, which ->
                val selectedRule = rules[which]
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Rule action")
                    .setItems(arrayOf("Edit", "Delete")) { _, action ->
                        if (action == 0) {
                            showCreateOrEditRuleFlow(selectedRule)
                        } else {
                            loader.removeAppBlockerScheduleRule(selectedRule.id)
                            sendAppBlockerRefresh()
                            Toast.makeText(requireContext(), "Rule deleted", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .show()
            }
            .show()
    }

    private fun sendAppBlockerRefresh() {
        requireContext().sendBroadcast(
            android.content.Intent(DeenShieldAccessibilityService.INTENT_ACTION_REFRESH_APP_BLOCKER)
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
                com.alhaq.deenshield.R.color.text_color
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

            val dialog = com.alhaq.deenshield.ui.dialogs.SetLaunchLimitDialog(
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
