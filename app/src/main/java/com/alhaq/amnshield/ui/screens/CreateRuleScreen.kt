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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID
import com.alhaq.amnshield.ui.state.AmnShieldState
import com.alhaq.amnshield.ui.state.ScheduleRule
import com.alhaq.amnshield.ui.state.SchedulePeriod
import com.alhaq.amnshield.ui.viewmodel.AmnShieldViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRuleScreen(
    state: AmnShieldState,
    viewModel: AmnShieldViewModel,
    initialType: String = "Block Schedule",
    onSaveRule: (ScheduleRule) -> Unit,
    onBack: () -> Unit
) {
    var ruleName by remember { mutableStateOf("") }
    var targetType by remember { mutableStateOf(initialType) } // "Block Schedule", "Launch Limit", "Cheat Window"
    var targetBlockerType by remember { mutableStateOf("App Blocker") } // "App Blocker", "Keyword Blocker", "Website Blocker", "Reels Blocker", "Notification Shielder"
    
    // Feature configurations
    val selectedApps = remember { mutableStateListOf<String>("com.instagram.android", "com.google.android.youtube") }
    val selectedKeywords = remember { mutableStateListOf<String>() }
    val selectedWebsites = remember { mutableStateListOf<String>() }
    val selectedPlatforms = remember { mutableStateListOf<String>("Instagram", "TikTok") }
    
    var customAppInput by remember { mutableStateOf("") }
    var customKeywordInput by remember { mutableStateOf("") }
    var customWebsiteInput by remember { mutableStateOf("") }
    
    // Multiple periods
    val periodsList = remember { mutableStateListOf<SchedulePeriod>() }
    
    // New period builder fields
    var newStartTime by remember { mutableStateOf("09:00") }
    var newEndTime by remember { mutableStateOf("17:00") }
    val newSelectedDays = remember { mutableStateListOf("Mon", "Tue", "Wed", "Thu", "Fri") }
    
    var limitValueStr by remember { mutableStateOf("5") }

    // On initial launch, pre-populate one default period
    LaunchedEffect(Unit) {
        if (periodsList.isEmpty()) {
            periodsList.add(SchedulePeriod("09:00", "17:00", listOf("Mon", "Tue", "Wed", "Thu", "Fri")))
        }
        // Initialize from global state lists if empty
        if (selectedKeywords.isEmpty() && state.keywords.isNotEmpty()) {
            selectedKeywords.addAll(state.keywords.take(2))
        }
        if (selectedWebsites.isEmpty() && state.customBlockedWebsites.isNotEmpty()) {
            selectedWebsites.addAll(state.customBlockedWebsites.take(2))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Create Protection Rule",
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
                // Intro Header
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

                // Rule Name Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "1. Give This Rule a Name",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = ruleName,
                        onValueChange = { ruleName = it },
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

                // Protection Feature Selection
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
                    
                    // Horizontal scrollable card list for selecting the blocker feature
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        features.forEach { (typeKey, label, icon) ->
                            val isSelected = targetBlockerType == typeKey
                            Card(
                                modifier = Modifier
                                    .width(135.dp)
                                    .clickable { targetBlockerType = typeKey }
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

                // Dynamic Feature Configuration Area
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
                        when (targetBlockerType) {
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
                                
                                // Show checkable app list
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    defaultAppsList.forEach { (appName, pkg) ->
                                        val isChecked = selectedApps.contains(pkg)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    if (isChecked) selectedApps.remove(pkg) else selectedApps.add(pkg)
                                                }
                                                .padding(vertical = 6.dp, horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { checked ->
                                                    if (checked) selectedApps.add(pkg) else selectedApps.remove(pkg)
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
                                
                                // Custom app entry
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = customAppInput,
                                        onValueChange = { customAppInput = it },
                                        placeholder = { Text("Add custom package (e.g. com.reddit)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        textStyle = MaterialTheme.typography.bodySmall
                                    )
                                    Button(
                                        onClick = {
                                            if (customAppInput.isNotBlank()) {
                                                if (!selectedApps.contains(customAppInput)) {
                                                    selectedApps.add(customAppInput)
                                                }
                                                customAppInput = ""
                                            }
                                        },
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

                                // Flow-like row of selected keyword chips
                                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        selectedKeywords.forEach { keyword ->
                                            InputChip(
                                                selected = true,
                                                onClick = { selectedKeywords.remove(keyword) },
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
                                
                                // Add Custom Keyword
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = customKeywordInput,
                                        onValueChange = { customKeywordInput = it },
                                        placeholder = { Text("e.g. gambling, shopping, bet") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    Button(
                                        onClick = {
                                            if (customKeywordInput.isNotBlank()) {
                                                if (!selectedKeywords.contains(customKeywordInput)) {
                                                    selectedKeywords.add(customKeywordInput)
                                                }
                                                customKeywordInput = ""
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Add")
                                    }
                                }
                                
                                // Quick suggestion tags
                                Text("Suggested Filters", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val presets = listOf("gaming", "crypto", "dating", "casino", "tiktok")
                                    presets.forEach { preset ->
                                        AssistChip(
                                            onClick = {
                                                if (!selectedKeywords.contains(preset)) selectedKeywords.add(preset)
                                            },
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

                                // Current list
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    selectedWebsites.forEach { domain ->
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
                                            IconButton(onClick = { selectedWebsites.remove(domain) }, modifier = Modifier.size(24.dp)) {
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
                                        value = customWebsiteInput,
                                        onValueChange = { customWebsiteInput = it },
                                        placeholder = { Text("e.g. distraction.com") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    Button(
                                        onClick = {
                                            if (customWebsiteInput.isNotBlank()) {
                                                if (!selectedWebsites.contains(customWebsiteInput)) {
                                                    selectedWebsites.add(customWebsiteInput)
                                                }
                                                customWebsiteInput = ""
                                            }
                                        },
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
                                        val isChecked = selectedPlatforms.contains(platform)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    if (isChecked) selectedPlatforms.remove(platform) else selectedPlatforms.add(platform)
                                                }
                                                .padding(vertical = 8.dp, horizontal = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { checked ->
                                                    if (checked) selectedPlatforms.add(platform) else selectedPlatforms.remove(platform)
                                                }
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

                // Restriction Mode Selection
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
                                    .clickable { targetType = typeKey }
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

                    // Dynamically show parameter for Launch Limit or Cheat Window
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
                                    onValueChange = { limitValueStr = it },
                                    label = { Text(if (targetType == "Launch Limit") "Launches" else "Minutes") },
                                    singleLine = true,
                                    modifier = Modifier.width(100.dp),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }

                // Configured Time Windows (Multiple Schedules)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "4. Active Time Windows",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
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
                                            onClick = { periodsList.removeAt(index) },
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

                    // Conflict Detection notification banner
                    val hasSelfConflict = remember(periodsList.toList()) {
                        var conflict = false
                        for (i in 0 until periodsList.size) {
                            for (j in i + 1 until periodsList.size) {
                                if (periodsOverlap(periodsList[i], periodsList[j])) {
                                    conflict = true
                                    break
                                }
                            }
                        }
                        conflict
                    }

                    val existingConflictingRules = remember(periodsList.toList(), targetBlockerType, targetType) {
                        state.scheduleRules.filter {
                            it.isActive && it.targetBlockerType == targetBlockerType && it.restrictionType == targetType
                        }
                    }

                    val hasExternalConflict = remember(existingConflictingRules, periodsList.toList()) {
                        var conflict = false
                        for (rule in existingConflictingRules) {
                            val rulePeriods = if (rule.periods.isNotEmpty()) rule.periods else listOf(SchedulePeriod(rule.startTime, rule.endTime, rule.days))
                            for (p1 in rulePeriods) {
                                for (p2 in periodsList) {
                                    if (periodsOverlap(p1, p2)) {
                                        conflict = true
                                        break
                                    }
                                }
                            }
                        }
                        conflict
                    }

                    if (hasSelfConflict || hasExternalConflict) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Conflict Warning",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Conflict Detected",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                val explanation = buildString {
                                    if (hasSelfConflict) {
                                        append("Some time windows in this rule overlap. ")
                                    }
                                    if (hasExternalConflict) {
                                        append("This overlaps with an existing rule for $targetBlockerType. ")
                                    }
                                    append("We will automatically resolve and correct these overlaps on save to prevent any breaks!")
                                }
                                Text(
                                    text = explanation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                                )
                                
                                Button(
                                    onClick = {
                                        val currentPeriods = periodsList.toList()
                                        val internalsMerged = mergeSchedules(currentPeriods)
                                        val allToMerge = mutableListOf<SchedulePeriod>()
                                        allToMerge.addAll(internalsMerged)
                                        for (rule in existingConflictingRules) {
                                            val rulePeriods = if (rule.periods.isNotEmpty()) rule.periods else listOf(SchedulePeriod(rule.startTime, rule.endTime, rule.days))
                                            allToMerge.addAll(rulePeriods)
                                        }
                                        
                                        val fullyResolved = mergeSchedules(allToMerge)
                                        periodsList.clear()
                                        periodsList.addAll(fullyResolved)
                                        for (rule in existingConflictingRules) {
                                            viewModel.deleteScheduleRule(rule.id)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(38.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Auto-Correct & Consolidate Now", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "All Clear",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Schedules are perfectly clean!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Builder card to design and add a new window to the rule
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
                                    onValueChange = { newStartTime = it },
                                    label = { Text("Start Time") },
                                    placeholder = { Text("09:00") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                OutlinedTextField(
                                    value = newEndTime,
                                    onValueChange = { newEndTime = it },
                                    label = { Text("End Time") },
                                    placeholder = { Text("17:00") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                            
                            // Day of Week selector buttons
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
                                            .clickable {
                                                if (isSelected) {
                                                    newSelectedDays.remove(day)
                                                } else {
                                                    newSelectedDays.add(day)
                                                }
                                            },
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
                                onClick = {
                                    if (newStartTime.isNotBlank() && newEndTime.isNotBlank() && newSelectedDays.isNotEmpty()) {
                                        val newPeriod = SchedulePeriod(
                                            startTime = newStartTime,
                                            endTime = newEndTime,
                                            days = newSelectedDays.toList()
                                        )
                                        periodsList.add(newPeriod)
                                    }
                                },
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

                    val saveEnabled = periodsList.isNotEmpty() && ruleName.isNotBlank()
                    Button(
                        onClick = {
                            if (saveEnabled) {
                                val finalPeriods = mergeSchedules(periodsList.toList())
                                val allToMerge = mutableListOf<SchedulePeriod>()
                                allToMerge.addAll(finalPeriods)
                                
                                val existingConflictingRulesOnSave = state.scheduleRules.filter {
                                    it.isActive && it.targetBlockerType == targetBlockerType && it.restrictionType == targetType
                                }
                                
                                for (rule in existingConflictingRulesOnSave) {
                                    val rulePeriods = if (rule.periods.isNotEmpty()) rule.periods else listOf(SchedulePeriod(rule.startTime, rule.endTime, rule.days))
                                    allToMerge.addAll(rulePeriods)
                                    viewModel.deleteScheduleRule(rule.id)
                                }
                                
                                val fullyMergedPeriods = mergeSchedules(allToMerge)
                                val firstPeriod = fullyMergedPeriods.firstOrNull() ?: SchedulePeriod("09:00", "17:00", listOf("Mon", "Tue", "Wed", "Thu", "Fri"))
                                
                                val limitValue = limitValueStr.toIntOrNull() ?: 5
                                
                                val appOrCategoryDisplay = when (targetBlockerType) {
                                    "App Blocker" -> if (selectedApps.isNotEmpty()) "${selectedApps.size} Apps" else "All Apps"
                                    "Keyword Blocker" -> if (selectedKeywords.isNotEmpty()) "${selectedKeywords.size} Keywords" else "Keywords Active"
                                    "Website Blocker" -> if (selectedWebsites.isNotEmpty()) "${selectedWebsites.size} Domains" else "Websites Active"
                                    "Reels Blocker" -> if (selectedPlatforms.isNotEmpty()) selectedPlatforms.joinToString(", ") else "All Reels"
                                    "Notification Shielder" -> "Mute Distractions"
                                    else -> targetBlockerType
                                }

                                val newRule = ScheduleRule(
                                    id = UUID.randomUUID().toString(),
                                    name = ruleName,
                                    appOrCategory = appOrCategoryDisplay,
                                    restrictionType = targetType,
                                    startTime = firstPeriod.startTime,
                                    endTime = firstPeriod.endTime,
                                    days = firstPeriod.days,
                                    limitValue = limitValue,
                                    isActive = true,
                                    periods = fullyMergedPeriods,
                                    targetBlockerType = targetBlockerType,
                                    selectedApps = selectedApps.toList(),
                                    selectedKeywords = selectedKeywords.toList(),
                                    selectedWebsites = selectedWebsites.toList(),
                                    selectedPlatforms = selectedPlatforms.toList()
                                )
                                
                                onSaveRule(newRule)
                                onBack()
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
}
