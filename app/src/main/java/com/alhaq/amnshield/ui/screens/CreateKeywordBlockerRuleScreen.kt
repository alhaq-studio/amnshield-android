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

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun CreateKeywordBlockerRuleScreen(
    state: AmnShieldState,
    viewModel: AmnShieldViewModel,
    initialType: String = "Block Schedule",
    editingRule: ScheduleRule? = null,
    onSaveRule: (ScheduleRule) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val initialName = remember(editingRule) {
        editingRule?.name ?: "Keyword Blocker Rule"
    }

    var ruleName by remember { mutableStateOf(initialName) }

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
                        text = if (editingRule != null) "Edit Keyword Rule" else "Create Keyword Rule",
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
                            "Configure Keywords",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.clickable {
                                val intent = Intent(context, com.alhaq.amnshield.ui.activity.FragmentActivity::class.java).apply {
                                    putExtra("feature_type", "keyword_blocker")
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                    OutlinedTextField(
                        value = ruleName,
                        onValueChange = { ruleName = it },
                        placeholder = { Text("e.g. Study Time Keywords") },
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
                                    Icon(Icons.Outlined.Shield, contentDescription = null, tint = Color(0xFF10B981))
                                    Column {
                                        Text("Always Block (24/7)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text("Block keywords all day, every day", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                        Icon(Icons.Outlined.Lock, contentDescription = null, tint = Color(0xFF10B981))
                                        Column {
                                            Text("Block Schedule", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Text("Filter keywords during these hours", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                                        .background(if (isSelected) Color(0xFF10B981) else MaterialTheme.colorScheme.surfaceVariant)
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
                }

                // 3. Blocked Keywords List
                val loader = remember { SavedPreferencesLoader(context) }
                val blockedKeywords = remember {
                    mutableStateListOf<String>().apply {
                        addAll(loader.loadBlockedKeywords())
                    }
                }
                var newKeyword by remember { mutableStateOf("") }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Block, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("3. Manage Blacklisted Keywords", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newKeyword,
                                onValueChange = { newKeyword = it },
                                placeholder = { Text("Add new keyword...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            IconButton(
                                onClick = {
                                    val kw = newKeyword.trim().lowercase()
                                    if (kw.isNotEmpty() && !blockedKeywords.contains(kw)) {
                                        blockedKeywords.add(kw)
                                        loader.saveBlockedKeywords(blockedKeywords.toSet())
                                        val intent = Intent(com.alhaq.amnshield.services.AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_BLOCKED_KEYWORD_LIST)
                                        context.sendBroadcast(intent.setPackage(context.packageName))
                                        newKeyword = ""
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add")
                            }
                        }

                        if (blockedKeywords.isEmpty()) {
                            Text(
                                "No keywords blocked yet. Add keywords above to apply this rule.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            androidx.compose.foundation.layout.FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                blockedKeywords.forEach { keyword ->
                                    InputChip(
                                        selected = false,
                                        onClick = { /* No-op */ },
                                        label = { Text(keyword) },
                                        trailingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Delete",
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clickable {
                                                        blockedKeywords.remove(keyword)
                                                        loader.saveBlockedKeywords(blockedKeywords.toSet())
                                                        val intent = Intent(com.alhaq.amnshield.services.AmnShieldAccessibilityService.INTENT_ACTION_REFRESH_BLOCKED_KEYWORD_LIST)
                                                        context.sendBroadcast(intent.setPackage(context.packageName))
                                                    }
                                            )
                                        },
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

                    val saveEnabled = ruleName.isNotBlank() && (isAlwaysBlockEnabled || isScheduleEnabled || isCheatEnabled)
                    Button(
                        onClick = {
                            if (saveEnabled) {
                                val newRule = ScheduleRule(
                                    id = editingRule?.id ?: UUID.randomUUID().toString(),
                                    name = ruleName,
                                    appOrCategory = "Keywords Blocker",
                                    restrictionType = "Keyword Blocker",
                                    startTime = scheduleStartTime,
                                    endTime = scheduleEndTime,
                                    days = scheduleDays.toList(),
                                    limitValue = 0,
                                    isActive = true,
                                    periods = emptyList(),
                                    targetBlockerType = "Keyword Blocker",
                                    selectedApps = emptyList(),
                                    selectedKeywords = emptyList(),
                                    selectedWebsites = emptyList(),
                                    selectedPlatforms = emptyList(),
                                    selectedBlockers = listOf("Keyword Blocker"),
                                    
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
