package com.alhaq.amnshield.ui.screens

import android.content.Intent
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID
import com.alhaq.amnshield.ui.state.AmnShieldState
import com.alhaq.amnshield.ui.state.ScheduleRule
import com.alhaq.amnshield.ui.state.SchedulePeriod
import com.alhaq.amnshield.ui.viewmodel.AmnShieldViewModel
import com.alhaq.amnshield.utils.SavedPreferencesLoader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReelsBlockerRuleScreen(
    state: AmnShieldState,
    viewModel: AmnShieldViewModel,
    initialType: String = "Block Schedule",
    editingRule: ScheduleRule? = null,
    onSaveRule: (ScheduleRule) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val initialName = remember(editingRule) {
        editingRule?.name ?: "Reels Blocker Rule"
    }

    var ruleName by remember { mutableStateOf(initialName) }

    val loader = remember { SavedPreferencesLoader(context) }

    // 1. Always Block vs Block Schedule
    var isAlwaysBlockEnabled by remember(editingRule) {
        mutableStateOf(editingRule?.isAlwaysBlockEnabled ?: (editingRule == null))
    }
    var isScheduleEnabled by remember(editingRule) {
        mutableStateOf(editingRule?.isScheduleEnabled ?: false)
    }

    // Reels Limit Mode (scrolled limit)
    var isLimitByReelsScrolled by remember {
        mutableStateOf(loader.getReelBlockerMode(2) == 1) // 1 = MODE_BLOCK_AFTER_DAILY_COUNT, 2 = MODE_BLOCK_ALL
    }
    var reelsLimitStr by remember {
        mutableStateOf(loader.getReelBlockerDailyLimit(200).toString())
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

    // Time Pickers Dialog State
    var activeTimePicker by remember { mutableStateOf("") } // "start", "end", "cheat_start", "cheat_end"
    var showTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        text = if (editingRule != null) "Edit Reels Rule" else "Create Reels Rule",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                // Rule Name
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "1. Rule Name",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Configure Reels",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.clickable {
                                val intent = Intent(context, com.alhaq.amnshield.ui.activity.FragmentActivity::class.java).apply {
                                    putExtra("feature_type", "reel_blocker")
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                    OutlinedTextField(
                        value = ruleName,
                        onValueChange = { ruleName = it },
                        placeholder = { Text("e.g. Study Time Reels") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Protection Settings (Block Schedule & Cheat Hours side-by-side)
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "2. Protection Settings",
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
                                    Icon(Icons.Outlined.Shield, contentDescription = null, tint = Color(0xFFEC4899))
                                    Column {
                                        Text("Always Block (24/7)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text("Block Reels all day, every day", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                        Icon(Icons.Outlined.Lock, contentDescription = null, tint = Color(0xFFEC4899))
                                        Column {
                                            Text("Block Schedule", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Text("Block Reels during these hours", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                                        .background(if (isSelected) Color(0xFFEC4899) else MaterialTheme.colorScheme.surfaceVariant)
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

                    // C) Reels Limit Mode (Block All Reels immediately, or Limit by Reels Scrolled)
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
                                    Icon(Icons.Outlined.HourglassEmpty, contentDescription = null, tint = Color(0xFFEC4899))
                                    Column {
                                        Text("Limit by Reels Scrolled", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text("Block Reels only after scrolling a set number", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Switch(
                                    checked = isLimitByReelsScrolled,
                                    onCheckedChange = { isLimitByReelsScrolled = it }
                                )
                            }

                            AnimatedVisibility(visible = isLimitByReelsScrolled) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Daily Reels Limit", style = MaterialTheme.typography.labelSmall)
                                    OutlinedTextField(
                                        value = reelsLimitStr,
                                        onValueChange = { reelsLimitStr = it.filter { char -> char.isDigit() } },
                                        placeholder = { Text("e.g. 50") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                        )
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

                    val saveEnabled = ruleName.isNotBlank() && (isAlwaysBlockEnabled || isScheduleEnabled || isCheatEnabled)
                    Button(
                        onClick = {
                            if (saveEnabled) {
                                // Save Reels scrolled limit mode config
                                val selectedMode = if (isLimitByReelsScrolled) 1 else 2 // 1 = MODE_BLOCK_AFTER_DAILY_COUNT, 2 = MODE_BLOCK_ALL
                                loader.setReelBlockerMode(selectedMode)
                                val parsedLimit = reelsLimitStr.toIntOrNull() ?: 200
                                loader.setReelBlockerDailyLimit(parsedLimit)
                                
                                // Trigger refresh
                                val refreshIntent = Intent(com.alhaq.amnshield.services.AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_REEL_BLOCKER)
                                context.sendBroadcast(refreshIntent.setPackage(context.packageName))

                                val newRule = ScheduleRule(
                                    id = editingRule?.id ?: UUID.randomUUID().toString(),
                                    name = ruleName,
                                    appOrCategory = "Reels Blocker",
                                    restrictionType = "Reels Blocker",
                                    startTime = scheduleStartTime,
                                    endTime = scheduleEndTime,
                                    days = scheduleDays.toList(),
                                    limitValue = 0,
                                    isActive = true,
                                    periods = emptyList(),
                                    targetBlockerType = "Reels Blocker",
                                    selectedApps = emptyList(),
                                    selectedKeywords = emptyList(),
                                    selectedWebsites = emptyList(),
                                    selectedPlatforms = emptyList(),
                                    selectedBlockers = listOf("Reels Blocker"),
                                    
                                    isAlwaysBlockEnabled = isAlwaysBlockEnabled,
                                    isScheduleEnabled = isScheduleEnabled,
                                    isCheatEnabled = isCheatEnabled,
                                    cheatStartTime = cheatStartTime,
                                    cheatEndTime = cheatEndTime,
                                    cheatDays = cheatDays.toList()
                                )
                                onSaveRule(newRule)
                            }
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp),
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
