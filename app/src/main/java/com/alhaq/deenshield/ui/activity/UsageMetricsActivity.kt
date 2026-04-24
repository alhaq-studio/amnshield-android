package com.alhaq.deenshield.ui.activity

import com.alhaq.deenshield.R
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.alhaq.deenshield.databinding.ActivityUsageMetricsBinding
import com.alhaq.deenshield.data.AttentionSpanVideoItem
import com.alhaq.deenshield.utils.SavedPreferencesLoader
import com.alhaq.deenshield.utils.TimeTools
import com.alhaq.deenshield.views.CustomMarkerView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.properties.Delegates


class UsageMetricsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsageMetricsBinding
    private var savedPreferencesLoader = SavedPreferencesLoader(this)


    var primaryColor by Delegates.notNull<Int>()

    lateinit var totalReels: Map<String,Int>
    lateinit var reelsAttentionSpanData: MutableMap<String, MutableList<AttentionSpanVideoItem>>

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme before super.onCreate
        val sharedPreferences = getSharedPreferences("com.alhaq.deenshield_preferences", MODE_PRIVATE)
        val themeStyle = sharedPreferences.getString("theme_style", "default")
        if (themeStyle == "gradient") {
            setTheme(R.style.Theme_DeenShield_Gradient)
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityUsageMetricsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(com.alhaq.deenshield.R.id.main)) { v, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            WindowInsetsCompat.CONSUMED
        }
        primaryColor = ContextCompat.getColor(
            this,
            com.alhaq.deenshield.R.color.md_theme_primary
        )

        totalReels = savedPreferencesLoader.getReelsScrolled()
        reelsAttentionSpanData = savedPreferencesLoader.loadUsageHoursAttentionSpanData()

        if (reelsAttentionSpanData.isEmpty() && totalReels.isEmpty()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Experimental Usage Tracker")
                .setMessage("This feature is not yet available on all devices. To check if your device is supported, open YouTube Shorts or Instagram Reels or Tiktok and, start scrolling, and then return here. If the stats remain empty, your device is currently unsupported. We are working to expand compatibility to more devices soon.")
                .setPositiveButton("Okay", null)
                .show()
        }

        binding.btnDigiWelbeing.setOnClickListener {
            val packageName = "com.google.android.apps.wellbeing"
            val intent = packageManager.getLaunchIntentForPackage(packageName)

            if (intent != null) {
                startActivity(intent)
            } else {
                Snackbar.make(
                    binding.root,
                    getString(com.alhaq.deenshield.R.string.failed_to_launch_system_digital_wellbeing),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }


        val markerViewReel = CustomMarkerView(this, com.alhaq.deenshield.R.layout.custom_marker_view)
        markerViewReel.apply {
            chartView = binding.reelsStats
            showDecimal = false
        }
        binding.reelsStats.marker = markerViewReel


        val markerViewAttention = CustomMarkerView(this, com.alhaq.deenshield.R.layout.custom_marker_view)
        markerViewAttention.apply {
            chartView = binding.avgAttentionStats
            showDecimal = true
        }
        binding.avgAttentionStats.marker = markerViewAttention

        binding.shareStats.setOnClickListener {
            binding.btnDigiWelbeing.text = "Tracked Using DeenShield"

            markerViewReel.visibility = View.GONE
            val screenshotFile = captureScreenshot(binding.linearSharePic)
            if (screenshotFile != null) {
                // Open the BottomSheet to share the screenshot
                openShareBottomSheet(screenshotFile)
            } else {
                Toast.makeText(this, "Failed to capture screenshot", Toast.LENGTH_SHORT).show()
            }

            markerViewReel.visibility = View.VISIBLE
            binding.btnDigiWelbeing.text = getString(com.alhaq.deenshield.R.string.view_more)
        }


    }

    override fun onResume() {
        super.onResume()

        lifecycleScope.launch {
            // Get the current date and update UI elements

            totalReels = savedPreferencesLoader.getReelsScrolled()
            reelsAttentionSpanData = savedPreferencesLoader.loadUsageHoursAttentionSpanData()
            makeReelCountStatsChart()
            makeAverageReelAttentionSpanChart()

            val date = TimeTools.getCurrentDate()
            binding.statsTodayReels.text =
                getString(com.alhaq.deenshield.R.string.you_scrolled_reels, totalReels.getOrDefault(date, 0))

//            if (totalReels.getOrDefault(date, 0) == 0) {
//                binding.statsAttentionSpanToday.visibility = View.GONE
//            } else {
//                binding.statsAttentionSpanToday.visibility = View.VISIBLE
//            }
            var average = withContext(Dispatchers.Default) {
                reelsAttentionSpanData[date]?.let { calculateAverageAttentionSpan(it, date) }
            }
            if (average == null) {
                average = 0.0
            }

            binding.statsAttentionSpanToday.text =
                getString(com.alhaq.deenshield.R.string.you_had_an_attention_span_of_seconds_video, average)
        }
    }


    private fun setupChartUI(
        chart: LineChart,
        labels: List<String>,
        lineDataSet: LineDataSet
    ) {

        lineDataSet.apply {
            color = primaryColor
            valueTextColor = primaryColor
            lineWidth = 3f
            setDrawCircles(true)
            setDrawCircleHole(true)
            circleHoleRadius = 4f
            circleRadius = 8f
            setCircleColor(primaryColor)

            setDrawValues(false)

            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f
        }

        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            labelCount = 5
            setDrawGridLines(false) // Disable vertical grid lines
            valueFormatter = IndexAxisValueFormatter(labels)
            textColor = primaryColor
        }

        chart.axisLeft.apply {
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


    private suspend fun makeReelCountStatsChart() {
        withContext(Dispatchers.Default) {
            val entries = mutableListOf<Entry>()
            val labels = mutableListOf<String>() // Store labels for X-axis

            var index = 0f // Keep track of index for the x-axis
            for ((date, value) in totalReels) {
                entries.add(Entry(index, value.toFloat()))
                labels.add(TimeTools.shortenDate(date))
                index += 1f
            }

            val lineDataSet = LineDataSet(entries, getString(com.alhaq.deenshield.R.string.reel_count))
            // Switch back to the main thread to update the UI
            withContext(Dispatchers.Main) {
                setupChartUI(
                    binding.reelsStats, labels, lineDataSet
                )

            }
        }
    }

    private suspend fun makeAverageReelAttentionSpanChart() {
        withContext(Dispatchers.Default) {
            val entries = mutableListOf<Entry>()
            val labels = mutableListOf<String>() // Store labels for X-axis
            var index = 0f // Keep track of index for the x-axis

            for ((date, value) in reelsAttentionSpanData) {
                val average = calculateAverageAttentionSpan(value, date)
                entries.add(Entry(index, average.toFloat()))
                labels.add(TimeTools.shortenDate(date))
                index += 1f
            }

            val lineDataSet = LineDataSet(entries, getString(com.alhaq.deenshield.R.string.average_attention_span))
            // Switch back to the main thread to update the UI
            withContext(Dispatchers.Main) {
                setupChartUI(
                    binding.avgAttentionStats, labels, lineDataSet
                )
            }
        }
    }
    private fun openShareBottomSheet(file: File) {
        val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Share Stats"))
    }

    private fun captureScreenshot(rootView: View): File? {
        // Create a Bitmap of the root layout based on its actual size
        val bitmap = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        rootView.draw(canvas)


        // Save the resized bitmap to a file in the cache directory
        val file = File(cacheDir, "screenshot_${System.currentTimeMillis()}.png")
        try {
            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            return file
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            // Recycle bitmaps to free memory
            bitmap.recycle()
        }
        return null
    }


    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun calculateAverageAttentionSpan(
        value: MutableList<AttentionSpanVideoItem>,
        date: String
    ): Double {
        val totalElapsedTime = value.sumOf { item -> item.elapsedTime.toDouble() }
        val reelsCount = totalReels[date]?.toDouble() ?: 0.0
        return if (reelsCount > 0) {
            totalElapsedTime / reelsCount
        } else {
            0.0
        }
    }

}