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
import com.alhaq.amnshield.ui.state.ScheduleRule
import com.alhaq.amnshield.ui.state.SchedulePeriod
import com.alhaq.amnshield.utils.SavedPreferencesLoader
import com.alhaq.amnshield.ui.activity.SelectAppsActivity
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRuleScreen(
    state: com.alhaq.amnshield.ui.state.AmnShieldState,
    onSaveRule: (ScheduleRule) -> Unit,
    onBack: () -> Unit,
    editingRule: ScheduleRule? = null,
    prefillTarget: String = "APP_BLOCKER"
) {
    val context = LocalContext.current

    val isFocusModeTarget = prefillTarget == "FOCUS_MODE" || editingRule?.targetBlockerType == "Focus Mode"

    val initialName = remember(editingRule) {
        editingRule?.name ?: if (isFocusModeTarget) "Auto Focus Schedule" else "App Blocker Rule"
    }

    var ruleName by remember { mutableStateOf(initialName) }

    val selectedApps = remember(editingRule) {
        mutableStateListOf<String>().apply {
            if (editingRule != null) {
                addAll(editingRule.selectedApps)
            } else if (isFocusModeTarget) {
                val loader = SavedPreferencesLoader(context)
                addAll(loader.getFocusModeSelectedApps())
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
                addAll(listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"))
            }
        }
    }

    // 3. Usage Limit
    var isUsageLimitEnabled by remember(editingRule) {
        mutableStateOf(editingRule?.isUsageLimitEnabled ?: false)
    }
    var usageLimitHoursStr by remember(editingRule) {
        mutableStateOf(editingRule?.usageLimitHours?.toString() ?: "2")
    }

    // 4. Launch Limit
    var isLaunchLimitEnabled by remember(editingRule) {
        mutableStateOf(editingRule?.isLaunchLimitEnabled ?: false)
    }
    var launchLimitCountStr by remember(editingRule) {
        mutableStateOf(editingRule?.launchLimitCount?.toString() ?: "10")
    }

    // Time Pickers Dialog State
    var activeTimePicker by remember { mutableStateOf("") } // "start", "end", "cheat_start", "cheat_end"
    var showTimePicker by remember { mutableStateOf(false) }

    val selectAppsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selected = result.data?.getStringArrayListExtra("SELECTED_APPS")
            if (selected != null) {
                selectedApps.clear()
                selectedApps.addAll(selected)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val screenTitle = when {
                        editingRule != null && isFocusModeTarget -> "Edit Auto Focus Schedule"
                        editingRule != null -> "Edit App Blocker Rule"
                        isFocusModeTarget -> "Create Auto Focus Schedule"
                        else -> "Create App Blocker Rule"
                    }
                    Text(
                        text = screenTitle,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Define Your Boundary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Configure block schedules, cheat hours, and limits together on a single rule.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 1. Rule Name
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "1. Rule Name",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = ruleName,
                        onValueChange = { ruleName = it },
                        placeholder = { Text("e.g. Focus Hours, Work Block") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("rule_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // 2. Apps Selection
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "2. Target Apps",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Configure App Blocker",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.clickable {
                                val intent = Intent(context, com.alhaq.amnshield.ui.activity.FragmentActivity::class.java).apply {
                                    putExtra("feature_type", "app_blocker")
                                }
                                context.startActivity(intent)
                            }
                        )
                    }

                    Button(
                        onClick = {
                            val intent = Intent(context, SelectAppsActivity::class.java).apply {
                                putStringArrayListExtra("PRE_SELECTED_APPS", ArrayList(selectedApps))
                            }
                            selectAppsLauncher.launch(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(imageVector = Icons.Outlined.AppShortcut, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedApps.isEmpty()) "Select Apps to Block" else "Edit App Selection (${selectedApps.size} Selected)",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (selectedApps.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            selectedApps.forEach { pkg ->
                                val appLabel = remember(pkg) {
                                    try {
                                        val appInfo = context.packageManager.getApplicationInfo(pkg, 0)
                                        context.packageManager.getApplicationLabel(appInfo).toString()
                                    } catch (e: Exception) {
                                        pkg.substringAfterLast(".")
                                    }
                                }
                                FilterChip(
                                    selected = true,
                                    onClick = { selectedApps.remove(pkg) },
                                    label = { Text(appLabel, style = MaterialTheme.typography.bodySmall) },
                                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(12.dp)) }
                                )
                            }
                        }
                    }
                }

                // 3. Consolidated Rule Settings (Block, Cheat, Usage, Launch Limits side-by-side)
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "3. Protection Settings",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // A) Always Block (24/7)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Shield, contentDescription = null, tint = Color(0xFFEF4444))
                                    Column {
                                        Text("Always Block (24/7)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text("Block apps all day, every day", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Switch(
                                    checked = isAlwaysBlockEnabled,
                                    onCheckedChange = {
                                        isAlwaysBlockEnabled = it
                                        if (it) {
                                            isScheduleEnabled = false
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // B) Block Schedule
                    AnimatedVisibility(visible = !isAlwaysBlockEnabled) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.Lock, contentDescription = null, tint = Color(0xFFEF4444))
                                        Column {
                                            Text("Block Schedule", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Text("Block apps during these hours", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Switch(
                                        checked = isScheduleEnabled,
                                        onCheckedChange = {
                                            isScheduleEnabled = it
                                            if (it) {
                                                isAlwaysBlockEnabled = false
                                            }
                                        }
                                    )
                                }

                                AnimatedVisibility(visible = isScheduleEnabled) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text("Start Time", style = MaterialTheme.typography.labelSmall)
                                                Button(
                                                    onClick = {
                                                        activeTimePicker = "start"
                                                        showTimePicker = true
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                                                ) {
                                                    Text(scheduleStartTime, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text("End Time", style = MaterialTheme.typography.labelSmall)
                                                Button(
                                                    onClick = {
                                                        activeTimePicker = "end"
                                                        showTimePicker = true
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                                                ) {
                                                    Text(scheduleEndTime, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }

                                        // Active Days
                                        Text("Active Days", style = MaterialTheme.typography.labelSmall)
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            val daysList = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                                            daysList.forEach { day ->
                                                val isSelected = scheduleDays.contains(day)
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSelected) Color(0xFFEF4444) else MaterialTheme.colorScheme.surfaceVariant)
                                                        .clickable {
                                                            if (isSelected) scheduleDays.remove(day) else scheduleDays.add(day)
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(day.take(1), color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // B) Cheat Hours
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.HourglassEmpty, contentDescription = null, tint = Color(0xFFF59E0B))
                                    Column {
                                        Text("Cheat Hours (Bypass)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text("Bypass active block during these hours", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Switch(checked = isCheatEnabled, onCheckedChange = { isCheatEnabled = it })
                            }

                            AnimatedVisibility(visible = isCheatEnabled) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("Start Time", style = MaterialTheme.typography.labelSmall)
                                            Button(
                                                onClick = {
                                                    activeTimePicker = "cheat_start"
                                                    showTimePicker = true
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                                            ) {
                                                Text(cheatStartTime, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("End Time", style = MaterialTheme.typography.labelSmall)
                                            Button(
                                                onClick = {
                                                    activeTimePicker = "cheat_end"
                                                    showTimePicker = true
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                                            ) {
                                                Text(cheatEndTime, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    // Cheat Days
                                    Text("Cheat Days", style = MaterialTheme.typography.labelSmall)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        val daysList = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                                        daysList.forEach { day ->
                                            val isSelected = cheatDays.contains(day)
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) Color(0xFFF59E0B) else MaterialTheme.colorScheme.surfaceVariant)
                                                    .clickable {
                                                        if (isSelected) cheatDays.remove(day) else cheatDays.add(day)
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(day.take(1), color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // C) Usage Limit
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Timer, contentDescription = null, tint = Color(0xFF3B82F6))
                                    Column {
                                        Text("Daily Usage Limit", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text("Max usage hours allowed per day", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Switch(checked = isUsageLimitEnabled, onCheckedChange = { isUsageLimitEnabled = it })
                            }

                            AnimatedVisibility(visible = isUsageLimitEnabled) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Usage Limit", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    OutlinedTextField(
                                        value = usageLimitHoursStr,
                                        onValueChange = { usageLimitHoursStr = it },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        suffix = { Text("hours") },
                                        singleLine = true,
                                        modifier = Modifier.width(130.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // D) Launch Limit
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Launch, contentDescription = null, tint = Color(0xFF8B5CF6))
                                    Column {
                                        Text("Daily Launch Limit", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text("Max opens allowed per app daily", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Switch(checked = isLaunchLimitEnabled, onCheckedChange = { isLaunchLimitEnabled = it })
                            }

                            AnimatedVisibility(visible = isLaunchLimitEnabled) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Launch Limit", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    OutlinedTextField(
                                        value = launchLimitCountStr,
                                        onValueChange = { launchLimitCountStr = it },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        suffix = { Text("opens") },
                                        singleLine = true,
                                        modifier = Modifier.width(130.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Actions (Save and Cancel)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }

                    val saveEnabled = ruleName.isNotBlank() && selectedApps.isNotEmpty() && (isAlwaysBlockEnabled || isScheduleEnabled || isCheatEnabled || isUsageLimitEnabled || isLaunchLimitEnabled)
                    Button(
                        onClick = {
                            if (saveEnabled) {
                                val finalApps = selectedApps.toList()
                                val appOrCategoryDisplay = if (finalApps.size == 1) {
                                    try {
                                        context.packageManager.getApplicationLabel(
                                            context.packageManager.getApplicationInfo(finalApps.first(), 0)
                                        ).toString()
                                    } catch (e: Exception) {
                                        finalApps.first()
                                    }
                                } else {
                                    "${finalApps.size} Apps"
                                }

                                val usageHours = usageLimitHoursStr.toIntOrNull() ?: 2
                                val launchCount = launchLimitCountStr.toIntOrNull() ?: 10

                                val newRule = ScheduleRule(
                                    id = editingRule?.id ?: UUID.randomUUID().toString(),
                                    name = ruleName,
                                    appOrCategory = appOrCategoryDisplay,
                                    restrictionType = if (isFocusModeTarget) "Focus Mode" else "App Blocker",
                                    startTime = scheduleStartTime,
                                    endTime = scheduleEndTime,
                                    days = scheduleDays.toList(),
                                    limitValue = if (isUsageLimitEnabled) usageHours else launchCount,
                                    isActive = true,
                                    periods = emptyList(),
                                    targetBlockerType = if (isFocusModeTarget) "Focus Mode" else "App Blocker",
                                    selectedApps = finalApps,
                                    selectedKeywords = emptyList(),
                                    selectedWebsites = emptyList(),
                                    selectedPlatforms = emptyList(),
                                    selectedBlockers = listOf(if (isFocusModeTarget) "Focus Mode" else "App Blocker"),
                                    
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
                            }
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
            onConfirm = { hour, minute ->
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRuleTimePickerDialog(
    title: String,
    initialHour: Int = 9,
    initialMinute: Int = 0,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = state)
            }
        }
    )
}
