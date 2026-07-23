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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alhaq.amnshield.ui.activity.SelectAppsActivity
import com.alhaq.amnshield.ui.state.AmnShieldState
import com.alhaq.amnshield.ui.state.ScheduleRule
import com.alhaq.amnshield.utils.SavedPreferencesLoader
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRuleScreen(
    state: AmnShieldState,
    onSaveRule: (ScheduleRule) -> Unit,
    onBack: () -> Unit,
    editingRule: ScheduleRule? = null,
    prefillTarget: String = "APP_BLOCKER",
    prefillApp: String? = null
) {
    val context = LocalContext.current

    val initialName = remember(editingRule, prefillApp) {
        if (editingRule != null) {
            editingRule.name
        } else if (!prefillApp.isNullOrEmpty()) {
            val appLabel = try {
                context.packageManager.getApplicationLabel(
                    context.packageManager.getApplicationInfo(prefillApp, 0)
                ).toString()
            } catch (_: Exception) {
                prefillApp
            }
            "Block: $appLabel"
        } else {
            "App Blocker Rule"
        }
    }

    var ruleName by remember { mutableStateOf(initialName) }

    val selectedApps = remember(editingRule, prefillApp) {
        mutableStateListOf<String>().apply {
            if (editingRule != null) {
                addAll(editingRule.selectedApps)
            } else if (!prefillApp.isNullOrEmpty()) {
                add(prefillApp)
            } else {
                val loader = SavedPreferencesLoader(context)
                addAll(loader.loadBlockedApps())
            }
        }
    }

    // 1. Always Block vs Block Schedule
    var isAlwaysBlockEnabled by remember(editingRule) {
        mutableStateOf(editingRule?.isAlwaysBlockEnabled ?: (editingRule == null))
    }
    var isScheduleEnabled by remember(editingRule) {
        mutableStateOf(editingRule?.isScheduleEnabled ?: false)
    }
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

    // 2. Cheat Hours
    var isCheatEnabled by remember(editingRule) {
        mutableStateOf(editingRule?.isCheatEnabled ?: false)
    }
    var cheatStartTime by remember(editingRule) {
        mutableStateOf(editingRule?.cheatStartTime ?: "12:00")
    }
    var cheatEndTime by remember(editingRule) {
        mutableStateOf(editingRule?.cheatEndTime ?: "13:00")
    }
    val cheatDays = remember(editingRule) {
        mutableStateListOf<String>().apply {
            if (editingRule != null) {
                addAll(editingRule.cheatDays)
            } else {
                addAll(listOf("Mon", "Tue", "Wed", "Thu", "Fri"))
            }
        }
    }

    // 3. Usage & Launch Limits
    var isUsageLimitEnabled by remember(editingRule) {
        mutableStateOf(editingRule?.isUsageLimitEnabled ?: false)
    }
    var usageHoursStr by remember(editingRule) {
        mutableStateOf(editingRule?.usageLimitHours?.toString() ?: "1")
    }

    var isLaunchLimitEnabled by remember(editingRule) {
        mutableStateOf(editingRule?.isLaunchLimitEnabled ?: false)
    }
    var launchCountStr by remember(editingRule) {
        mutableStateOf(editingRule?.launchLimitCount?.toString() ?: "5")
    }

    // Time picker dialog state
    var showTimePicker by remember { mutableStateOf(false) }
    var activeTimePicker by remember { mutableStateOf("start") } // "start", "end", "cheat_start", "cheat_end"

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

    val saveEnabled = ruleName.isNotBlank() && selectedApps.isNotEmpty()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        if (editingRule != null) "Edit App Blocker Rule" else "Create App Blocker Rule",
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
                        Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "App Blocker Rule",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Configure schedules, cheat hours, and usage limits on target apps.",
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
                label = { Text("Rule Name") },
                placeholder = { Text("e.g., Social Media Limit, Gaming Block") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Select Target Apps
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
                            Text("Target Apps", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("${selectedApps.size} apps selected", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

            // 1. Blocking Mode (Always Block vs Schedule)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Blocking Mode", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = isAlwaysBlockEnabled,
                            onClick = {
                                isAlwaysBlockEnabled = true
                                isScheduleEnabled = false
                            },
                            label = { Text("Always Block (24/7)") },
                            leadingIcon = if (isAlwaysBlockEnabled) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = isScheduleEnabled,
                            onClick = {
                                isAlwaysBlockEnabled = false
                                isScheduleEnabled = true
                            },
                            label = { Text("Custom Schedule") },
                            leadingIcon = if (isScheduleEnabled) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (isScheduleEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))

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

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Active Days", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))
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
            }

            // 2. Cheat Hours Section
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
                            Text("Cheat Hours (Bypass Window)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Temporary daily window when blocking is bypassed", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isCheatEnabled,
                            onCheckedChange = { isCheatEnabled = it }
                        )
                    }

                    if (isCheatEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    activeTimePicker = "cheat_start"
                                    showTimePicker = true
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Cheat Start", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(cheatStartTime, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    activeTimePicker = "cheat_end"
                                    showTimePicker = true
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Cheat End", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(cheatEndTime, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 3. Usage & Launch Limits Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Usage & Launch Limits", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    // Daily Usage Duration Limit
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Daily Screen Time Limit", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Max total hours allowed per day", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isUsageLimitEnabled,
                            onCheckedChange = { isUsageLimitEnabled = it }
                        )
                    }

                    if (isUsageLimitEnabled) {
                        OutlinedTextField(
                            value = usageHoursStr,
                            onValueChange = { usageHoursStr = it },
                            label = { Text("Max Daily Hours") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Divider()

                    // Daily Launch Limit
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Daily Launch Count Limit", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Max app opens allowed per day", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isLaunchLimitEnabled,
                            onCheckedChange = { isLaunchLimitEnabled = it }
                        )
                    }

                    if (isLaunchLimitEnabled) {
                        OutlinedTextField(
                            value = launchCountStr,
                            onValueChange = { launchCountStr = it },
                            label = { Text("Max App Launches") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save Buttons
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
                        val usageHours = usageHoursStr.toIntOrNull() ?: 1
                        val launchCount = launchCountStr.toIntOrNull() ?: 5

                        val newRule = ScheduleRule(
                            id = editingRule?.id ?: UUID.randomUUID().toString(),
                            name = ruleName,
                            appOrCategory = "App Blocker",
                            restrictionType = "App Blocker",
                            startTime = scheduleStartTime,
                            endTime = scheduleEndTime,
                            days = scheduleDays.toList(),
                            limitValue = usageHours,
                            isActive = true,
                            periods = emptyList(),
                            targetBlockerType = "App Blocker",
                            selectedApps = selectedApps.toList(),
                            selectedKeywords = emptyList(),
                            selectedWebsites = emptyList(),
                            selectedPlatforms = emptyList(),
                            selectedBlockers = listOf("App Blocker"),
                            isAlwaysBlockEnabled = isAlwaysBlockEnabled,
                            isScheduleEnabled = isScheduleEnabled,
                            isCheatEnabled = isCheatEnabled,
                            cheatStartTime = cheatStartTime,
                            cheatEndTime = cheatEndTime,
                            cheatDays = cheatDays.toList(),
                            isUsageLimitEnabled = isUsageLimitEnabled,
                            usageLimitHours = usageHours,
                            isLaunchLimitEnabled = isLaunchLimitEnabled,
                            launchLimitCount = launchCount
                        )
                        onSaveRule(newRule)
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp)
                        .testTag("save_rule_button"),
                    shape = RoundedCornerShape(12.dp),
                    enabled = saveEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Save & Apply Rule", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Time picker dialog logic
    if (showTimePicker) {
        val initialTime = when (activeTimePicker) {
            "start" -> scheduleStartTime
            "end" -> scheduleEndTime
            "cheat_start" -> cheatStartTime
            else -> cheatEndTime
        }
        val parts = initialTime.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0

        CreateRuleTimePickerDialog(
            title = when (activeTimePicker) {
                "start" -> "Select Start Time"
                "end" -> "Select End Time"
                "cheat_start" -> "Select Cheat Start Time"
                else -> "Select Cheat End Time"
            },
            initialHour = h,
            initialMinute = m,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour: Int, minute: Int ->
                val formatted = String.format("%02d:%02d", hour, minute)
                when (activeTimePicker) {
                    "start" -> scheduleStartTime = formatted
                    "end" -> scheduleEndTime = formatted
                    "cheat_start" -> cheatStartTime = formatted
                    "cheat_end" -> cheatEndTime = formatted
                }
                showTimePicker = false
            }
        )
    }
}
