package com.alhaq.amnshield.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alhaq.amnshield.ui.state.SchedulePeriod
import com.alhaq.amnshield.ui.viewmodel.CreateRuleViewModel

@Composable
fun BoundaryHeader() {
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
                    text = "Apply tailored time windows to specific blocker features. Consolidate schedules seamlessly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RuleNameSection(
    ruleName: String,
    onRuleNameChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "1. Give This Rule a Name",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        OutlinedTextField(
            value = ruleName,
            onValueChange = onRuleNameChange,
            placeholder = { Text("e.g. Work Social Blocker, Bedtime Rest") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("rule_name_input"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
    }
}

@Composable
fun FeatureSelection(
    selectedFeature: String,
    onFeatureSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "2. Select Blocker Feature to Apply Window",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        val features = listOf(
            Triple("App Blocker", "App Blocker", Icons.Default.Apps),
            Triple("Keyword Blocker", "Keyword Blocker", Icons.Default.Key),
            Triple("Website Blocker", "Website Blocker", Icons.Default.Language),
            Triple("Reels Blocker", "Reels Blocker", Icons.Default.MovieFilter),
            Triple("Notification Shielder", "Notification Shielder", Icons.Default.NotificationsOff)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            features.forEach { (typeKey, label, icon) ->
                val isSelected = selectedFeature == typeKey
                Card(
                    modifier = Modifier
                        .width(135.dp)
                        .clickable { onFeatureSelected(typeKey) }
                        .testTag("feature_tab_${typeKey.replace(" ", "_")}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConfigArea(
    viewModel: CreateRuleViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (viewModel.targetBlockerType) {
                "App Blocker" -> {
                    Text(
                        "App Selection Picker",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Select which applications will be restricted during this active schedule window.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    val defaultAppsList = listOf(
                        "Instagram" to "com.instagram.android",
                        "YouTube" to "com.google.android.youtube",
                        "X (Twitter)" to "com.twitter.android",
                        "TikTok" to "com.zhiliaoapp.musically",
                        "Snapchat" to "com.snapchat.android",
                        "Facebook" to "com.facebook.katana",
                        "Call of Duty" to "com.activision.callofduty"
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        defaultAppsList.forEach { (appName, pkg) ->
                            val isChecked = viewModel.selectedApps.contains(pkg)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (isChecked) viewModel.removeSelectedApp(pkg) else viewModel.addSelectedApp(pkg)
                                    }
                                    .padding(vertical = 6.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked) viewModel.addSelectedApp(pkg) else viewModel.removeSelectedApp(pkg)
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(appName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(pkg, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = viewModel.customAppInput,
                            onValueChange = { viewModel.customAppInput = it },
                            placeholder = { Text("Add custom package (e.g. com.reddit)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            onClick = { viewModel.addCustomApp() },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("Add")
                        }
                    }
                }
                
                "Keyword Blocker" -> {
                    Text(
                        "Keyword Block Rules",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "All content containing these keywords will be filtered and blocked instantly under this active window.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            viewModel.selectedKeywords.forEach { keyword ->
                                InputChip(
                                    selected = true,
                                    onClick = { viewModel.removeSelectedKeyword(keyword) },
                                    label = { Text(keyword) },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = viewModel.customKeywordInput,
                            onValueChange = { viewModel.customKeywordInput = it },
                            placeholder = { Text("e.g. gambling, shopping, bet") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Button(
                            onClick = { viewModel.addCustomKeyword() },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Add")
                        }
                    }
                    
                    Text("Suggested Filters", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val presets = listOf("gaming", "crypto", "dating", "casino", "tiktok")
                        presets.forEach { preset ->
                            AssistChip(
                                onClick = { viewModel.addSelectedKeyword(preset) },
                                label = { Text(preset) }
                            )
                        }
                    }
                }
                
                "Website Blocker" -> {
                    Text(
                        "Restricted Website Domains",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Specify websites that should be completely blocked in any browser during this schedule window.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        viewModel.selectedWebsites.forEach { domain ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Outlined.Language, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(domain, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                }
                                IconButton(onClick = { viewModel.removeSelectedWebsite(domain) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Delete", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = viewModel.customWebsiteInput,
                            onValueChange = { viewModel.customWebsiteInput = it },
                            placeholder = { Text("e.g. distraction.com") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Button(
                            onClick = { viewModel.addCustomWebsite() },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Add")
                        }
                    }
                }
                
                "Reels Blocker" -> {
                    Text(
                        "Target Short-Video Platforms",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "We will aggressively interrupt and exit full-screen Reels scrolling on checked apps.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val platforms = listOf("Instagram Reels", "YouTube Shorts", "TikTok", "Facebook Reels")
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        platforms.forEach { platform ->
                            val isChecked = viewModel.selectedPlatforms.contains(platform)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.togglePlatform(platform) }
                                    .padding(vertical = 8.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { viewModel.togglePlatform(platform) }
                                )
                                Text(platform, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                
                "Notification Shielder" -> {
                    Text(
                        "Notification Distraction Shielder",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Dodge the temptation loop. Shield notification banners and hide notifications during this active window.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                "Auto-Mute Distractions Active",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Incoming notifications are silenced, hidden from drawer, and logged in wellbeing report.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RestrictionModeSelection(
    targetType: String,
    onTypeSelected: (String) -> Unit,
    limitValueStr: String,
    onLimitValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "3. Select Restriction Mode",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val types = listOf(
                Triple("Block Schedule", "Block", Icons.Outlined.Lock),
                Triple("Launch Limit", "Limit", Icons.Outlined.Launch),
                Triple("Cheat Window", "Cheat", Icons.Outlined.HourglassEmpty)
            )
            types.forEach { (typeKey, displayLabel, icon) ->
                val isSelected = targetType == typeKey
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTypeSelected(typeKey) }
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = displayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        AnimatedVisibility(visible = targetType == "Launch Limit" || targetType == "Cheat Window") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (targetType == "Launch Limit") "Maximum Launches" else "Allowance Period",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (targetType == "Launch Limit") "Allow up to N launches per app daily." else "Allow unblocked apps for specified minutes.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedTextField(
                        value = limitValueStr,
                        onValueChange = onLimitValueChange,
                        label = { Text(if (targetType == "Launch Limit") "Launches" else "Minutes") },
                        singleLine = true,
                        modifier = Modifier.width(100.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveTimeWindows(
    periodsList: List<SchedulePeriod>,
    onRemovePeriod: (Int) -> Unit
) {
    if (periodsList.isEmpty()) {
        Text(
            "Please add at least one time window below to apply this rule.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Medium
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            periodsList.forEachIndexed { index, period ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AccessTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${period.startTime} - ${period.endTime}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = period.days.joinToString(" • "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        IconButton(
                            onClick = { onRemovePeriod(index) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove window",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddNewTimeWindow(
    newStartTime: String,
    onStartTimeChange: (String) -> Unit,
    newEndTime: String,
    onEndTimeChange: (String) -> Unit,
    newSelectedDays: List<String>,
    onToggleDay: (String) -> Unit,
    onAddPeriod: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Add Another Time Window",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newStartTime,
                    onValueChange = onStartTimeChange,
                    label = { Text("Start Time") },
                    placeholder = { Text("09:00") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = newEndTime,
                    onValueChange = onEndTimeChange,
                    label = { Text("End Time") },
                    placeholder = { Text("17:00") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )
            }
            
            Text(
                "Days",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val allDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                allDays.forEach { day ->
                    val isSelected = newSelectedDays.contains(day)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .clickable { onToggleDay(day) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.take(1),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Button(
                onClick = onAddPeriod,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Window", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
