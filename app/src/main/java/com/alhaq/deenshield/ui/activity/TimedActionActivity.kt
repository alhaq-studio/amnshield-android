package com.alhaq.deenshield.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.alhaq.deenshield.R
import com.alhaq.deenshield.databinding.ActivityAddTimedActionActivityBinding
import com.alhaq.deenshield.databinding.CheatHourItemBinding
import com.alhaq.deenshield.databinding.DialogAddTimedActionBinding
import com.alhaq.deenshield.services.DeenShieldAccessibilityService
import com.alhaq.deenshield.utils.SavedPreferencesLoader
import com.alhaq.deenshield.utils.TimeTools
import nl.joery.timerangepicker.TimeRangePicker

class TimedActionActivity : AppCompatActivity() {

    companion object {
        const val MODE_APP_BLOCKER_CHEAT_HOURS = 1
        const val MODE_AUTO_FOCUS = 2
    }

    private lateinit var binding: ActivityAddTimedActionActivityBinding
    private val savedPreferencesLoader = SavedPreferencesLoader(this)
    private var timedActionList: MutableList<AutoTimedActionItem> = mutableListOf()

    private lateinit var selectUnblockedAppsLauncher: ActivityResultLauncher<Intent>
    private var selectedUnblockedApps: ArrayList<String>? = arrayListOf()

    private lateinit var dialogAddToTimedActionBinding: DialogAddTimedActionBinding

    private var selectedMode = MODE_APP_BLOCKER_CHEAT_HOURS
    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme before super.onCreate
        val sharedPreferences = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        val themeStyle = sharedPreferences.getString("theme_style", "default")
        if (themeStyle == "gradient") {
            setTheme(R.style.Theme_DeenShield_Gradient)
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityAddTimedActionActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        if (intent.hasExtra("selected_mode")) {
            selectedMode = intent.getIntExtra("selected_mode", MODE_APP_BLOCKER_CHEAT_HOURS)
        }

        when (selectedMode) {
            MODE_APP_BLOCKER_CHEAT_HOURS -> {
                timedActionList = savedPreferencesLoader.loadAppBlockerCheatHoursList()
            }

            MODE_AUTO_FOCUS -> timedActionList = savedPreferencesLoader.loadAutoFocusHoursList()
        }
        selectUnblockedAppsLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val selectedApps = result.data?.getStringArrayListExtra("SELECTED_APPS")
                    selectedApps?.let {
                        selectedUnblockedApps = selectedApps
                        dialogAddToTimedActionBinding.btnSelectUnblockedApps.text =
                            getString(R.string.app_s_selected, selectedApps.size)
                    }
                }
            }

        binding.recyclerView2.layoutManager = LinearLayoutManager(this)
        binding.recyclerView2.adapter = CheatHourAdapter(timedActionList)

        binding.button.setOnClickListener {
            makeCheatHoursDialog()
        }
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun makeCheatHoursDialog() {


        dialogAddToTimedActionBinding = DialogAddTimedActionBinding.inflate(layoutInflater)

        val startTime = TimeRangePicker.Time(6, 30)
        val endTime = TimeRangePicker.Time(22, 0)

        var startTimeInMins: Int = startTime.totalMinutes
        var endTimeInMins: Int = endTime.totalMinutes

        dialogAddToTimedActionBinding.picker.hourFormat = TimeRangePicker.HourFormat.FORMAT_24

        dialogAddToTimedActionBinding.picker.startTimeMinutes = startTimeInMins
        dialogAddToTimedActionBinding.picker.endTimeMinutes = endTimeInMins
        dialogAddToTimedActionBinding.fromTime.text = startTime.toString()
        dialogAddToTimedActionBinding.endTime.text = endTime.toString()

        dialogAddToTimedActionBinding.picker.setOnTouchListener { v, event ->
            // Disable ScrollView's touch interception when interacting with the picker
            when (event.action) {
                MotionEvent.ACTION_DOWN -> dialogAddToTimedActionBinding.scrollview.requestDisallowInterceptTouchEvent(
                    true
                )

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> dialogAddToTimedActionBinding.scrollview.requestDisallowInterceptTouchEvent(
                    false
                )
            }
            v.onTouchEvent(event) // Pass the event to the picker
        }
        dialogAddToTimedActionBinding.picker.setOnTimeChangeListener(object :
            TimeRangePicker.OnTimeChangeListener {
            override fun onStartTimeChange(startTime: TimeRangePicker.Time) {
                dialogAddToTimedActionBinding.fromTime.text =
                    dialogAddToTimedActionBinding.picker.startTime.toString()
                startTimeInMins = dialogAddToTimedActionBinding.picker.startTimeMinutes
            }

            override fun onEndTimeChange(endTime: TimeRangePicker.Time) {
                dialogAddToTimedActionBinding.endTime.text =
                    dialogAddToTimedActionBinding.picker.endTime.toString()
                endTimeInMins = dialogAddToTimedActionBinding.picker.endTimeMinutes
            }

            override fun onDurationChange(duration: TimeRangePicker.TimeDuration) {
            }
        })

        when (selectedMode) {
            MODE_AUTO_FOCUS -> {
                dialogAddToTimedActionBinding.timedTitle.text = "Specify Auto-Focus Hours"
                dialogAddToTimedActionBinding.btnSelectUnblockedApps.text = "Select Apps to Block"
            }

            MODE_APP_BLOCKER_CHEAT_HOURS -> {
                dialogAddToTimedActionBinding.timedTitle.text = "Specify Cheat Hours"
                dialogAddToTimedActionBinding.timedTitle.text = "Specify Apps to Unblock"
            }
        }

        dialogAddToTimedActionBinding.btnSelectUnblockedApps.setOnClickListener {
            val intent = Intent(this, SelectAppsActivity::class.java)
            intent.putStringArrayListExtra(
                "PRE_SELECTED_APPS",
                selectedUnblockedApps
            )

            if (selectedMode == MODE_APP_BLOCKER_CHEAT_HOURS) {
                intent.putStringArrayListExtra(
                    "APP_LIST",
                    ArrayList(savedPreferencesLoader.loadBlockedApps())
                )
            }

            selectUnblockedAppsLauncher.launch(intent)
        }
        MaterialAlertDialogBuilder(this)
            .setView(dialogAddToTimedActionBinding.root)
            .setPositiveButton(getString(R.string.add)) { dialog, _ ->
                if (dialogAddToTimedActionBinding.cheatHourTitle.text?.isEmpty() == true) {
                    Toast.makeText(
                        this,
                        getString(R.string.please_type_a_title),
                        Toast.LENGTH_SHORT
                    ).show()
                } else if (selectedUnblockedApps?.isEmpty() == true) {
                    Toast.makeText(
                        this,
                        getString(R.string.please_select_a_few_apps), Toast.LENGTH_SHORT
                    )
                        .show()
                } else {
                    timedActionList.add(
                        AutoTimedActionItem(
                            dialogAddToTimedActionBinding.cheatHourTitle.text.toString(),
                            startTimeInMins!!,
                            endTimeInMins!!,
                            selectedUnblockedApps!!
                        )
                    )
                    binding.recyclerView2.adapter?.notifyItemInserted(timedActionList.size)
                    saveList()
                    dialog.dismiss()
                }

            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }


    inner class CheatHourAdapter(
        private val items: List<AutoTimedActionItem>
    ) : RecyclerView.Adapter<CheatHourAdapter.CheatHourViewHolder>() {

        inner class CheatHourViewHolder(private val binding: CheatHourItemBinding) :
            RecyclerView.ViewHolder(binding.root) {

            @SuppressLint("SetTextI18n")
            fun bind(item: AutoTimedActionItem) {
                binding.cheatHourTitle.text = item.title
                val convertedStartTime = TimeTools.convertMinutesTo24Hour(item.startTimeInMins)
                val convertedEndTIme = TimeTools.convertMinutesTo24Hour(item.endTimeInMins)

                binding.removeCheatHour.setOnClickListener {
                    timedActionList.removeAt(layoutPosition)
                    notifyItemRemoved(layoutPosition)
                    saveList()
                }

                binding.cheatTimings.text =
                    getString(
                        R.string.cheat_timings,
                        convertedStartTime.first,
                        convertedStartTime.second,
                        convertedEndTIme.first,
                        convertedEndTIme.second
                    )
                item.packages.forEach { packageName ->
                    binding.selectedApps.text =
                        binding.selectedApps.text.toString() + " " + packageName
                }

            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CheatHourViewHolder {
            val binding = CheatHourItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return CheatHourViewHolder(binding)
        }

        override fun onBindViewHolder(holder: CheatHourViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size
    }

    private fun saveList() {
        when (selectedMode) {
            MODE_APP_BLOCKER_CHEAT_HOURS -> {
                savedPreferencesLoader.saveAppBlockerCheatHoursList(timedActionList)
                sendBroadcast(
                    Intent(DeenShieldAccessibilityService.INTENT_ACTION_REFRESH_APP_BLOCKER)
                        .setPackage(packageName)
                )
            }

            MODE_AUTO_FOCUS -> {
                savedPreferencesLoader.saveAutoFocusHoursList(timedActionList)
                sendBroadcast(
                    Intent(DeenShieldAccessibilityService.INTENT_ACTION_REFRESH_FOCUS_MODE)
                        .setPackage(packageName)
                )
            }
        }
    }

    data class AutoTimedActionItem(
        val title: String,
        val startTimeInMins: Int,
        val endTimeInMins: Int,
        val packages: ArrayList<String>,
        val isProceedHidden: Boolean = false
    )


}