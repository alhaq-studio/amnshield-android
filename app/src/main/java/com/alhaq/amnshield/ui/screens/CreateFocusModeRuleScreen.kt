package com.alhaq.amnshield.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alhaq.amnshield.Constants
import com.alhaq.amnshield.ui.activity.SelectAppsActivity
import com.alhaq.amnshield.ui.state.AmnShieldState
import com.alhaq.amnshield.ui.state.ScheduleRule
import com.alhaq.amnshield.utils.SavedPreferencesLoader
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFocusModeRuleScreen(
    state: AmnShieldState,
    onSaveRule: (ScheduleRule) -> Unit,
    onBack: () -> Unit,
    editingRule: ScheduleRule? = null
) {
    val context = LocalContext.current

    val initialName = remember(editingRule) {
        editingRule?.name ?: "Auto Focus Schedule"
    }

    var ruleName by remember { mutableStateOf(initialName) }

    val selectedApps = remember(editingRule) {
        mutableStateListOf<String>().apply {
            if (editingRule != null) {
                addAll(editingRule.selectedApps)
            } else {
                val loader = SavedPreferencesLoader(context)
                addAll(loader.getFocusModeSelectedApps())
            }
        }
    }

    // Focus Mode Strategy (Block All Except Whitelist vs Block Selected Blacklist)
    var focusModeType by remember(editingRule) {
        val loader = SavedPreferencesLoader(context)
        val defaultType = loader.getFocusModeData().modeType
        mutableStateOf(if (defaultType == Constants.FOCUS_MODE_BLOCK_ALL_EX_SELECTED) 1 else 0)
    }

    // Time Window & Days Schedule
    var scheduleStartTime by remember(editingRule) {
        mutableStateOf(editingRule?.startTime ?: "09:00")
    }
    var scheduleEndTime by remember(editingRule) {
        mutableStateOf(editingRule?.endTime ?: "17:00")
    }
    val scheduleDays = remember(editingRule) {
        mutableStateListOf<String>().apply {
            if (editingRule != null) {
                addAll(editingRule.days)
            } else {
                addAll(listOf("Mon", "Tue", "Wed", "Thu", "Fri"))
            }
        }
    }

    // Time picker dialog state
    var showTimePicker by remember { mutableStateOf(false) }
    var activeTimePicker by remember { mutableStateOf("start") } // "start", "end"

    // App Picker Activity Launcher
    val selectAppsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val apps = result.data?.getStringArrayListExtra("SELECTED_APPS")
            if (apps != null) {
                selectedApps.clear()
                selectedApps.addAll(apps)
            }
        }
    }

    val saveEnabled = ruleName.isNotBlank() && scheduleDays.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (editingRule != null) "Edit Auto Focus Schedule" else "Create Auto Focus Schedule",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Auto Focus Window",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Automatically activates Focus Mode during configured hours and days.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Rule Name Field
            OutlinedTextField(
                value = ruleName,
                onValueChange = { ruleName = it },
                label = { Text("Schedule Name") },
                placeholder = { Text("e.g., Deep Work Hours, Study Window") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Focus Mode Strategy
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Focus Mode Strategy",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = focusModeType == 1,
                            onClick = { focusModeType = 1 },
                            label = { Text("Block All Except Whitelist") },
                            leadingIcon = if (focusModeType == 1) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = focusModeType == 0,
                            onClick = { focusModeType = 0 },
                            label = { Text("Block Selected Only") },
                            leadingIcon = if (focusModeType == 0) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Schedule Hours Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Active Schedule Window",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                activeTimePicker = "start"
                                showTimePicker = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Start Time", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(scheduleStartTime, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                activeTimePicker = "end"
                                showTimePicker = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("End Time", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(scheduleEndTime, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Active Days",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val weekDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        weekDays.forEach { day ->
                            val isSelected = scheduleDays.contains(day)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) scheduleDays.remove(day) else scheduleDays.add(day)
                                },
                                label = { Text(day.take(1), fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape
                            )
                        }
                    }
                }
            }

            // Target Apps Selector Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                if (focusModeType == 1) "Allowed Whitelist Apps" else "Blocked Blacklist Apps",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "${selectedApps.size} apps configured",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = {
                                val intent = Intent(context, SelectAppsActivity::class.java).apply {
                                    putStringArrayListExtra("PRE_SELECTED_APPS", ArrayList(selectedApps))
                                }
                                selectAppsLauncher.launch(intent)
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Select Apps")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        val ruleId = editingRule?.id ?: UUID.randomUUID().toString()
                        val newRule = ScheduleRule(
                            id = ruleId,
                            name = ruleName,
                            appOrCategory = "Focus Mode",
                            restrictionType = "Focus Mode",
                            startTime = scheduleStartTime,
                            endTime = scheduleEndTime,
                            days = scheduleDays.toList(),
                            targetBlockerType = "Focus Mode",
                            selectedApps = selectedApps.toList(),
                            selectedBlockers = listOf("Focus Mode"),
                            isScheduleEnabled = true,
                            isActive = editingRule?.isActive ?: true
                        )

                        val loader = SavedPreferencesLoader(context)
                        val currentData = loader.getFocusModeData()
                        val selectedMode = if (focusModeType == 1) Constants.FOCUS_MODE_BLOCK_ALL_EX_SELECTED else Constants.FOCUS_MODE_BLOCK_SELECTED
                        loader.saveFocusModeData(
                            com.alhaq.amnshield.blockers.FocusModeBlocker.FocusModeData(
                                isTurnedOn = currentData.isTurnedOn,
                                endTime = currentData.endTime,
                                modeType = selectedMode,
                                selectedApps = HashSet(selectedApps)
                            )
                        )
                        loader.saveFocusModeSelectedApps(selectedApps.toList())

                        onSaveRule(newRule)
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp)
                        .testTag("save_focus_rule_button"),
                    shape = RoundedCornerShape(12.dp),
                    enabled = saveEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Save & Apply Schedule", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Time picker dialog logic
    if (showTimePicker) {
        val initialTime = if (activeTimePicker == "start") scheduleStartTime else scheduleEndTime
        val parts = initialTime.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0

        CreateRuleTimePickerDialog(
            title = if (activeTimePicker == "start") "Select Auto Focus Start Time" else "Select Auto Focus End Time",
            initialHour = h,
            initialMinute = m,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour: Int, minute: Int ->
                val formatted = String.format("%02d:%02d", hour, minute)
                if (activeTimePicker == "start") {
                    scheduleStartTime = formatted
                } else {
                    scheduleEndTime = formatted
                }
                showTimePicker = false
            }
        )
    }
}
