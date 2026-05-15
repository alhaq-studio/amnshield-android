package com.alhaq.deenshield.ui.fragments.usage

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.alhaq.deenshield.R
import com.alhaq.deenshield.databinding.AppUsageItemBinding
import com.alhaq.deenshield.databinding.DialogPermissionInfoBinding
import com.alhaq.deenshield.databinding.FragmentAllAppUsageBinding
import com.alhaq.deenshield.ui.activity.FragmentActivity
import com.alhaq.deenshield.ui.activity.SelectAppsActivity
import com.alhaq.deenshield.utils.SavedPreferencesLoader
import com.alhaq.deenshield.utils.TimeTools
import com.alhaq.deenshield.utils.UsageStatsHelper
import com.alhaq.deenshield.utils.getDefaultLauncherPackageName
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

class AllAppsUsageFragment : Fragment() {

    companion object {
        const val FRAGMENT_ID = "all_app_usage"
    }

    private var selectedDate:Long = System.currentTimeMillis()
    private var currentDate:Long = selectedDate
    private var earliestDate:Long = selectedDate

    private var _binding: FragmentAllAppUsageBinding? = null
    private val binding get() = _binding!!

    private var ignoredPackages: MutableSet<String> = mutableSetOf()
    private lateinit var savedPreferencesLoader: SavedPreferencesLoader

    val selectIgnoredAppsLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedApps = result.data?.getStringArrayListExtra("SELECTED_APPS")
            selectedApps?.let {
                savedPreferencesLoader.saveIgnoredAppUsageTracker(it.toSet())
                reloadIgnoredPackages()
                lifecycleScope.launch(Dispatchers.IO) {
                    val localDate = Instant.ofEpochMilli(selectedDate)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    setUsageStats(localDate)
                }
            }
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAllAppUsageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        savedPreferencesLoader = SavedPreferencesLoader(requireContext())

        if (!hasUsageStatsPermission(requireContext())) {
            makeUsageStatsPermissoinDialog()
        }

        val adapter = AppUsageAdapter(emptyList())
        binding.appUsageRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.appUsageRecyclerView.adapter = adapter

        lifecycleScope.launch(Dispatchers.IO) {

            reloadIgnoredPackages()

            setUsageStats()

            findDataAvailabilityRange()
        }
        binding.openMenu.setOnClickListener {
            val popupMenu = PopupMenu(requireContext(), binding.openMenu)
            popupMenu.menuInflater.inflate(R.menu.usage_tracker_options, popupMenu.menu)

            // Handle menu item clicks
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.select_ignored -> {

                        val intent = Intent(requireContext(), SelectAppsActivity::class.java)
                        intent.putStringArrayListExtra(
                            "PRE_SELECTED_APPS",
                            ArrayList(savedPreferencesLoader.loadIgnoredAppUsageTracker())
                        )
                        selectIgnoredAppsLauncher.launch(
                            intent,
                            ActivityOptionsCompat.makeCustomAnimation(
                                requireContext(),
                                R.anim.fade_in,
                                R.anim.fade_out
                            )
                        )
                        true
                    }

                    R.id.view_recommendations -> {
                        binding.main.smoothScrollTo(0, binding.recommendationCard.top)
                        true
                    }

                    R.id.add_shortcut_usage_tracker -> {

                        val intent = Intent(requireContext(), FragmentActivity::class.java).apply {
                            action = Intent.ACTION_CREATE_SHORTCUT
                        }

                        intent.putExtra("fragment", FRAGMENT_ID)
                        val shortcutInfo =
                            ShortcutInfoCompat.Builder(requireContext(), "deenshield_usage_tracker")
                                .setShortLabel("Usage Stats")
                                .setLongLabel("Usage Stats")
                                .setIntent(intent)
                                .setIcon(
                                    IconCompat.createWithResource(
                                        requireContext(),
                                        R.drawable.baseline_query_stats_24
                                    )
                                )
                                .build()


                        val supported =
                            ShortcutManagerCompat.isRequestPinShortcutSupported(requireContext())
                        val dynamicShortcuts =
                            ShortcutManagerCompat.getDynamicShortcuts(requireContext())

                        if (supported) {
                            if (dynamicShortcuts.contains(shortcutInfo)) {
                                return@setOnMenuItemClickListener false
                            }
                        }
                        val pinnedShortcutCallbackIntent =
                            Intent("example.intent.action.SHORTCUT_CREATED")

                        val successCallback = PendingIntent.getBroadcast(
                            requireContext(),
                            3000,
                            pinnedShortcutCallbackIntent,
                            FLAG_IMMUTABLE
                        )

                        ShortcutManagerCompat.requestPinShortcut(
                            requireContext(),
                            shortcutInfo,
                            successCallback.intentSender
                        )

                        true
                    }

                    else -> false
                }
            }

            popupMenu.show()

        }
        binding.selectDate.setOnClickListener {
            showDatePickerDialog(selectedDate, earliestDate, currentDate) { newDate ->
                selectedDate = newDate
                binding.selectDate.text = TimeTools.formatDate(newDate)
                val localDate = Instant.ofEpochMilli(newDate)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()

                lifecycleScope.launch(Dispatchers.IO) {
                    setUsageStats(localDate)
                }

            }
        }

    }

    fun findDataAvailabilityRange() {

        val usageStatsManager = requireContext().getSystemService(UsageStatsManager::class.java)
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            0, System.currentTimeMillis()
        )

        // Calculate earliest available date
        earliestDate = stats.minByOrNull { it.firstTimeStamp }?.firstTimeStamp ?: System.currentTimeMillis()
        currentDate = System.currentTimeMillis()
        selectedDate = currentDate.coerceAtLeast(earliestDate) // Ensure valid range

    }

    private fun reloadIgnoredPackages() {
        ignoredPackages.clear()
        getDefaultLauncherPackageName(requireContext().packageManager)?.let {
            ignoredPackages.add(it)
        }
        ignoredPackages.addAll(savedPreferencesLoader.loadIgnoredAppUsageTracker())
    }

    override fun onResume() {
        super.onResume()

        lifecycleScope.launch(Dispatchers.IO) {
            val localDate = Instant.ofEpochMilli(selectedDate)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

            setUsageStats(localDate)
            findDataAvailabilityRange()
        }
    }
    private fun makeUsageStatsPermissoinDialog() {
        val dialogBinding =
            DialogPermissionInfoBinding.inflate(layoutInflater)
        dialogBinding.title.text =
            getString(R.string.enable_2, "Device Usage Access")

        dialogBinding.desc.text =
            "DeenShield requires device usage access to monitor apps, helping you manage screen time effectively and stay focused on your goals. Rest assured, all data stays securely on your device and is never shared with anyone, ensuring your privacy is fully protected."

        dialogBinding.point1.text = "Track what apps you use"
        dialogBinding.point2.visibility = View.GONE
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(false)
            .show()

        dialogBinding.btnReject.setOnClickListener {
            dialog.dismiss()
            activity?.finish()
        }
        dialogBinding.btnAccept.setOnClickListener {
            Toast.makeText(requireContext(), "Find 'DeenShield' and press enable", Toast.LENGTH_LONG)
                .show()
            requestUsageStatsPermission(requireContext())
            dialog.dismiss()
        }
    }

    private suspend fun setUsageStats(date : LocalDate = LocalDate.now()) {
        val usageStatsHelper = UsageStatsHelper(requireContext())
        val list = usageStatsHelper.getForegroundStatsByDay(date).filter {
            it.totalTime >= 180_000 && it.packageName !in ignoredPackages
        }
        val totalTime = TimeTools.formatTime(calculateTotalScreenTimeInHours(list),false)

        withContext(Dispatchers.Main) {
            try {
                val adapter = binding.appUsageRecyclerView.adapter as AppUsageAdapter
                if(list.isEmpty()){
                    Toast.makeText(requireContext(),"No data available",Toast.LENGTH_SHORT).show()
                }
                updatePieChart(list)
                updateRecommendations(list)
                binding.totalUsage.text = totalTime

                adapter.updateData(list)
            } catch (e: Exception) {
                Log.e("AppUsageFragment", "Error updating UI with stats", e)
            }
        }
    }

    private fun calculateTotalScreenTimeInHours(stats: List<Stat>): Long {
        val totalTimeInMillis = stats.sumOf { it.totalTime }

        return totalTimeInMillis
    }

    private fun updateRecommendations(statsList: List<Stat>) {
        val topApps = statsList
            .sortedByDescending { it.totalTime }
            .take(3)

        if (topApps.isEmpty()) {
            binding.recommendationSubtitle.text = "No usage data available for recommendations on this date."
            setRecommendationRow(0, null)
            setRecommendationRow(1, null)
            setRecommendationRow(2, null)
            return
        }

        binding.recommendationSubtitle.text = "Based on your highest usage apps for this day"
        setRecommendationRow(0, topApps.getOrNull(0))
        setRecommendationRow(1, topApps.getOrNull(1))
        setRecommendationRow(2, topApps.getOrNull(2))
    }

    private fun setRecommendationRow(index: Int, stat: Stat?) {
        val row = when (index) {
            0 -> binding.recommendationItem1
            1 -> binding.recommendationItem2
            else -> binding.recommendationItem3
        }
        val iconView: ImageView = when (index) {
            0 -> binding.recommendationIcon1
            1 -> binding.recommendationIcon2
            else -> binding.recommendationIcon3
        }
        val textView: TextView = when (index) {
            0 -> binding.recommendationText1
            1 -> binding.recommendationText2
            else -> binding.recommendationText3
        }

        if (stat == null) {
            row.visibility = View.GONE
            return
        }

        val pm = requireContext().packageManager
        val appName = try {
            val appInfo = pm.getApplicationInfo(stat.packageName, 0)
            iconView.setImageDrawable(pm.getApplicationIcon(appInfo))
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            iconView.setImageResource(R.drawable.baseline_android_24)
            stat.packageName.substringAfterLast('.')
        }

        val minutes = (stat.totalTime / 60000L).toInt()
        val riskText = when {
            minutes >= 240 || stat.sessions.size >= 35 -> "High"
            minutes >= 120 || stat.sessions.size >= 20 -> "Moderate"
            else -> "Elevated"
        }

        textView.text = "$appName • ${TimeTools.formatTime(stat.totalTime, false)} • $riskText risk"
        row.visibility = View.VISIBLE
    }

    private fun showDatePickerDialog(
        selectedDate: Long,
        startDate: Long,
        endDate: Long,
        onDateSelected: (Long) -> Unit
    ) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = selectedDate

        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val pickedCalendar = Calendar.getInstance()
                pickedCalendar.set(year, month, dayOfMonth)
                onDateSelected(pickedCalendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Restrict the selectable date range
        datePicker.datePicker.minDate = startDate
        datePicker.datePicker.maxDate = endDate
        datePicker.show()
    }

    private fun updatePieChart(statsList: List<Stat>) {
        val sortedStats = statsList.sortedByDescending { it.totalTime }
        val topApps = sortedStats.take(3)
        val othersTime = sortedStats.drop(3).sumOf { it.totalTime }

        val entries = mutableListOf<PieEntry>()
        val pm = requireContext().packageManager
        topApps.forEach { stats ->
            val usageTime = stats.totalTime
            val label = try {
                val appInfo = pm.getApplicationInfo(stats.packageName, 0)
                appInfo.loadLabel(pm).toString()
            } catch (e: Exception) {
                stats.packageName.substringAfterLast('.')
            }
            entries.add(PieEntry(usageTime.toFloat(), label))
        }

        if (othersTime > 0) {
            entries.add(PieEntry(othersTime.toFloat(), getString(R.string.app_usage_others_label)))
        }

        val pieDataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                Color.parseColor("#2196F3"),
                Color.parseColor("#F44336"),
                Color.parseColor("#4CAF50"),
                requireContext().getColor(R.color.md_theme_inverseSurface)
            )
            sliceSpace = 3f
            selectionShift = 10f
            yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            valueLinePart1Length = 0.5f
            valueLinePart2Length = 0.4f
            valueLineColor = MaterialColors.getColor(
                requireContext(),
                com.google.android.material.R.attr.colorOutline,
                Color.GRAY
            )
            valueTextColor = MaterialColors.getColor(
                requireContext(),
                com.google.android.material.R.attr.colorOnSurface,
                Color.BLACK
            )
            valueTextSize = 12f
        }

        val pieData = PieData(pieDataSet)
        val totalValue = pieData.yValueSum
        pieData.setDrawValues(true)
        pieData.setValueFormatter(object : ValueFormatter() {
            override fun getPieLabel(value: Float, pieEntry: PieEntry?): String {
                if (pieEntry == null || totalValue == 0f) {
                    return ""
                }
                val percentage = (value / totalValue) * 100f
                val formattedPercentage = String.format(Locale.getDefault(), "%.1f%%", percentage)
                val entryLabel = pieEntry.label ?: ""
                return if (entryLabel.isNotBlank()) {
                    "$entryLabel $formattedPercentage"
                } else {
                    formattedPercentage
                }
            }
        })
        pieData.setValueTextColor(
            MaterialColors.getColor(
                requireContext(),
                com.google.android.material.R.attr.colorOnSurface,
                Color.BLACK
            )
        )
        pieData.setValueTextSize(12f)

        binding.pieChart.apply {
            data = pieData
            description.isEnabled = false
            isRotationEnabled = true
            isDrawHoleEnabled = true
            holeRadius = 85f
            transparentCircleRadius = 0f
            setHoleColor(
                MaterialColors.getColor(
                    context,
                    com.google.android.material.R.attr.colorSurface,
                    Color.WHITE
                )
            )
            legend.isEnabled = false
            setDrawEntryLabels(false)
            setUsePercentValues(false)
            setExtraOffsets(12f, 12f, 12f, 12f)
            animateY(1200, Easing.EaseInOutQuart)
            invalidate()
        }
    }

    inner class AppUsageViewHolder(private val binding: AppUsageItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(stats: Stat, packageManager: PackageManager) {
            binding.root.setOnClickListener{
                activity?.supportFragmentManager?.beginTransaction()
                    ?.setCustomAnimations(R.anim.fade_in,R.anim.fade_out)
                    ?.replace(R.id.fragment_holder, AppUsageBreakdown(stats))
                    ?.addToBackStack(null)
                    ?.commit()
            }
            binding.root.setOnLongClickListener {

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Add to ignored packages?")
                    .setMessage("This action will cause the tracker to not display any stats from this app.")
                    .setCancelable(true)
                    .setPositiveButton("Okay") { _, _ ->
                        val savedPreferencesLoader = SavedPreferencesLoader(requireContext())
                        val ignoredAppsSP =
                            savedPreferencesLoader.loadIgnoredAppUsageTracker().toMutableSet()
                        ignoredAppsSP.add(stats.packageName)
                        ignoredPackages.addAll(ignoredAppsSP)
                        savedPreferencesLoader.saveIgnoredAppUsageTracker(ignoredAppsSP)

                        lifecycleScope.launch(Dispatchers.IO) {
                            val localDate = Instant.ofEpochMilli(selectedDate)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()

                            setUsageStats(localDate)
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                true
            }

            // Load app icon and label on the main thread
            val appInfo = try {
                packageManager.getApplicationInfo(stats.packageName, 0)
            } catch (e: Exception) {
                null
            }
            binding.appIcon.setImageDrawable(appInfo?.loadIcon(packageManager))
            binding.appName.text = appInfo?.loadLabel(packageManager)
                ?: stats.packageName.substringAfterLast('.')
            binding.appUsage.text = TimeTools.formatTime(stats.totalTime)
        }
    }

    inner class AppUsageAdapter(
        private var appUsageStats: List<Stat>
    ) : RecyclerView.Adapter<AppUsageViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppUsageViewHolder {
            val binding = AppUsageItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return AppUsageViewHolder(binding)
        }

        override fun onBindViewHolder(holder: AppUsageViewHolder, position: Int) {
            holder.bind(appUsageStats[position], holder.itemView.context.packageManager)
        }

        @SuppressLint("NotifyDataSetChanged")
        fun updateData(newAppUsageStats: List<Stat>) {
            appUsageStats = newAppUsageStats

            notifyDataSetChanged()
        }

        override fun getItemCount(): Int = appUsageStats.size
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOpsManager.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun requestUsageStatsPermission(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    data class UsageSession(
        val startTime: ZonedDateTime,
        val durationMillis: Long
    )

    class Stat(
        val packageName: String,
        val totalTime: Long,
        val sessions: List<UsageSession>
    ) {
        val startTimes: List<ZonedDateTime>
            get() = sessions.map { it.startTime }
    }

}
