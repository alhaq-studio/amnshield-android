package com.alhaq.deenshield.ui.fragments.usage

import android.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

import com.alhaq.deenshield.databinding.FragmentAppUsageBreakdownBinding
import com.alhaq.deenshield.utils.TimeTools
import java.time.Duration

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
}
