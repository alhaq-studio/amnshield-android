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
import com.alhaq.amnshield.utils.SavedPreferencesLoader
import com.alhaq.amnshield.utils.ScheduleUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import android.content.Intent
import com.alhaq.amnshield.ui.activity.SelectAppsActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRuleScreen(
    state: AmnShieldState,
    viewModel: AmnShieldViewModel,
    initialType: String = "Block Schedule",
    prefillTarget: String? = null,
    editingRule: ScheduleRule? = null,
    onSaveRule: (ScheduleRule) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val initialBlockerType = remember(prefillTarget, editingRule) {
        if (editingRule != null) {
            editingRule.selectedBlockers.firstOrNull() ?: editingRule.targetBlockerType
        } else {
            when (prefillTarget) {
                "APP_BLOCKER" -> "App Blocker"
                "WEBSITE_BLOCKER" -> "Website Blocker"
                "REEL_BLOCKER" -> "Reels Blocker"
                "KEYWORD_BLOCKER" -> "Keyword Blocker"
                "FOCUS_MODE" -> "Focus Mode"
                else -> "App Blocker"
            }
        }
    }
    
    val defaultRuleName = remember(initialType, prefillTarget, editingRule) {
        if (editingRule != null) {
            editingRule.name
        } else {
            val targetName = when (prefillTarget) {
                "APP_BLOCKER" -> "App Blocker"
                "WEBSITE_BLOCKER" -> "Website Blocker"
                "REEL_BLOCKER" -> "Reels Blocker"
                "KEYWORD_BLOCKER" -> "Keyword Blocker"
                "FOCUS_MODE" -> "Focus Mode"
                else -> "Protection"
            }
            "$targetName Rule"
        }
    }

    var ruleName by remember(defaultRuleName) { mutableStateOf(defaultRuleName) }
    var targetType by remember(initialType, editingRule) {
        mutableStateOf(editingRule?.restrictionType ?: initialType)
    } // "Block Schedule", "Launch Limit", "Usage Limit", "Cheat Window"
    
    val selectedBlockerTypes = remember(editingRule) {
        mutableStateListOf<String>().apply {
            if (editingRule != null) {
                addAll(editingRule.selectedBlockers)
                if (isEmpty()) add(editingRule.targetBlockerType)
            } else {
                add(initialBlockerType)
            }
        }
    }
    
    // Feature configurations
    val prefilledApps = remember(prefillTarget) {
        val loader = SavedPreferencesLoader(context)
        val apps = if (prefillTarget == "APP_BLOCKER") {
            loader.loadBlockedApps()
        } else if (prefillTarget == "FOCUS_MODE") {
            loader.getFocusModeSelectedApps()
        } else {
            emptySet()
        }
        apps.toList()
    }
    val prefilledKeywords = remember(prefillTarget) {
        if (prefillTarget == "KEYWORD_BLOCKER") {
            SavedPreferencesLoader(context).loadBlockedKeywords().toList()
        } else {
            emptyList()
        }
    }
    val prefilledWebsites = remember(prefillTarget) {
        if (prefillTarget == "WEBSITE_BLOCKER") {
            SavedPreferencesLoader(context).loadBlockedWebsites().toList()
        } else {
            emptyList()
        }
    }
    val prefilledPlatforms = remember(prefillTarget) {
        if (prefillTarget == "REEL_BLOCKER") {
            val loader = SavedPreferencesLoader(context)
            val list = mutableListOf<String>()
            if (loader.isReelBlockerInstagramEnabled()) list.add("Instagram")
            if (loader.isReelBlockerTiktokEnabled()) list.add("TikTok")
            if (loader.isReelBlockerYoutubeEnabled()) list.add("YouTube")
            list
        } else {
            listOf("Instagram", "TikTok")
        }
    }

    val selectedApps = remember(editingRule) {
        mutableStateListOf<String>().apply {
            if (editingRule != null) {
                addAll(editingRule.selectedApps)
            } else {
                addAll(prefilledApps)
            }
        }
    }
    val selectedKeywords = remember(editingRule) {
        mutableStateListOf<String>().apply {
            if (editingRule != null) {
                addAll(editingRule.selectedKeywords)
            } else {
                addAll(prefilledKeywords)
            }
        }
    }
    val selectedWebsites = remember(editingRule) {
        mutableStateListOf<String>().apply {
            if (editingRule != null) {
                addAll(editingRule.selectedWebsites)
            } else {
                addAll(prefilledWebsites)
            }
        }
    }
    val selectedPlatforms = remember(editingRule) {
        mutableStateListOf<String>().apply {
            if (editingRule != null) {
                addAll(editingRule.selectedPlatforms)
            } else {
                addAll(prefilledPlatforms)
            }
        }
    }
    
    var customAppInput by remember { mutableStateOf("") }
    var customKeywordInput by remember { mutableStateOf("") }
    var customWebsiteInput by remember { mutableStateOf("") }
    
    // Multiple periods
    val periodsList = remember(editingRule) {
        mutableStateListOf<SchedulePeriod>().apply {
            if (editingRule != null && editingRule.periods.isNotEmpty()) {
                addAll(editingRule.periods)
            }
        }
    }
    
    // New period builder fields
    var newStartTime by remember { mutableStateOf("09:00") }
    var newEndTime by remember { mutableStateOf("17:00") }
    val newSelectedDays = remember { mutableStateListOf("Mon", "Tue", "Wed", "Thu", "Fri") }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    
    var limitValueStr by remember(editingRule) {
        mutableStateOf(editingRule?.limitValue?.toString() ?: "5")
    }

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
    
    // On initial launch, pre-populate period only if editing an existing rule
    LaunchedEffect(editingRule) {
        if (periodsList.isEmpty() && editingRule != null) {
            periodsList.add(SchedulePeriod(editingRule.startTime, editingRule.endTime, editingRule.days))
        }
        // Initialize from global state lists if empty and not editing
        if (editingRule == null) {
            if (selectedKeywords.isEmpty() && state.keywords.isNotEmpty()) {
                selectedKeywords.addAll(state.keywords.take(2))
            }
            if (selectedWebsites.isEmpty() && state.customBlockedWebsites.isNotEmpty()) {
                selectedWebsites.addAll(state.customBlockedWebsites.take(2))
            }
        }
    }

    val isAppBlockerSelected = selectedBlockerTypes.contains("App Blocker")
    LaunchedEffect(isAppBlockerSelected) {
        if (!isAppBlockerSelected && (targetType == "Launch Limit" || targetType == "Usage Limit")) {
            targetType = "Block Schedule"
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
                        "2. Select Blocker Feature(s) to Apply Window",
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
                            val isSelected = selectedBlockerTypes.contains(typeKey)
                            Card(
                                modifier = Modifier
                                    .width(135.dp)
                                    .clickable {
                                        if (isSelected) {
                                            if (selectedBlockerTypes.size > 1) {
                                                selectedBlockerTypes.remove(typeKey)
                                            }
                                        } else {
                                            selectedBlockerTypes.add(typeKey)
                                        }
                                    }
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
                        var separatorNeeded = false

                        if (selectedBlockerTypes.contains("App Blocker")) {
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
                                Icon(
                                    imageVector = Icons.Outlined.AppShortcut,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (selectedApps.isEmpty()) "Open App Picker" else "Edit App Selection (${selectedApps.size} Selected)",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            if (selectedApps.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
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
                                            trailingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove",
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                                selectedTrailingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        )
                                    }
                                }
                            }
                            separatorNeeded = true
                        }

                        if (selectedBlockerTypes.contains("Keyword Blocker")) {
                            if (separatorNeeded) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            }
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
                            separatorNeeded = true
                        }

                        if (selectedBlockerTypes.contains("Website Blocker")) {
                            if (separatorNeeded) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            }
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
                            separatorNeeded = true
                        }

                        if (selectedBlockerTypes.contains("Reels Blocker")) {
                            if (separatorNeeded) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            }
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
                            separatorNeeded = true
                        }

                        if (selectedBlockerTypes.contains("Notification Shielder")) {
                            if (separatorNeeded) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            }
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
                        val types = remember(isAppBlockerSelected) {
                            val baseList = mutableListOf(
                                Triple("Block Schedule", "Block", Icons.Outlined.Lock)
                            )
                            if (isAppBlockerSelected) {
                                baseList.add(Triple("Launch Limit", "Launch", Icons.Outlined.Launch))
                                baseList.add(Triple("Usage Limit", "Usage", Icons.Outlined.HourglassEmpty))
                            }
                            baseList.add(Triple("Cheat Window", "Cheat", Icons.Outlined.Star))
                            baseList
                        }
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
                                    .padding(vertical = 12.dp, horizontal = 2.dp),
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

                    // Dynamically show parameter for Launch Limit, Usage Limit, or Cheat Window
                    AnimatedVisibility(visible = targetType == "Launch Limit" || targetType == "Usage Limit" || targetType == "Cheat Window") {
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
                                        text = when (targetType) {
                                            "Launch Limit" -> "Maximum Launches"
                                            "Usage Limit" -> "Maximum Daily Usage"
                                            else -> "Allowance Period"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = when (targetType) {
                                            "Launch Limit" -> "Allow up to N launches per app daily."
                                            "Usage Limit" -> "Allow up to N hours of daily usage."
                                            else -> "Allow unblocked apps for specified minutes."
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedTextField(
                                    value = limitValueStr,
                                    onValueChange = { limitValueStr = it },
                                    label = { 
                                        Text(
                                            when (targetType) {
                                                "Launch Limit" -> "Launches"
                                                "Usage Limit" -> "Hours"
                                                else -> "Minutes"
                                            }
                                        )
                                    },
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
                                if (ScheduleUtils.periodsOverlap(periodsList[i], periodsList[j])) {
                                    conflict = true
                                    break
                                }
                            }
                        }
                        conflict
                    }

                    val existingConflictingRules = remember(periodsList.toList(), selectedBlockerTypes.toList(), targetType) {
                        state.scheduleRules.filter { rule ->
                            rule.isActive && rule.restrictionType == targetType &&
                            (rule.selectedBlockers.any { selectedBlockerTypes.contains(it) } ||
                             selectedBlockerTypes.contains(rule.targetBlockerType))
                        }
                    }

                    val hasExternalConflict = remember(existingConflictingRules, periodsList.toList()) {
                        var conflict = false
                        for (rule in existingConflictingRules) {
                            val rulePeriods = if (rule.periods.isNotEmpty()) rule.periods else listOf(SchedulePeriod(rule.startTime, rule.endTime, rule.days))
                            for (p1 in rulePeriods) {
                                for (p2 in periodsList) {
                                    if (ScheduleUtils.periodsOverlap(p1, p2)) {
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
                                        append("This overlaps with an existing rule for ${selectedBlockerTypes.joinToString(" / ")}. ")
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
                                        val internalsMerged = ScheduleUtils.mergeSchedules(currentPeriods)
                                        val allToMerge = mutableListOf<SchedulePeriod>()
                                        allToMerge.addAll(internalsMerged)
                                        for (rule in existingConflictingRules) {
                                            val rulePeriods = if (rule.periods.isNotEmpty()) rule.periods else listOf(SchedulePeriod(rule.startTime, rule.endTime, rule.days))
                                            allToMerge.addAll(rulePeriods)
                                        }
                                        
                                        val fullyResolved = ScheduleUtils.mergeSchedules(allToMerge)
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
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { showStartPicker = true }
                                ) {
                                    OutlinedTextField(
                                        value = newStartTime,
                                        onValueChange = {},
                                        label = { Text("Start Time") },
                                        readOnly = true,
                                        enabled = false,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                            disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { showEndPicker = true }
                                ) {
                                    OutlinedTextField(
                                        value = newEndTime,
                                        onValueChange = {},
                                        label = { Text("End Time") },
                                        readOnly = true,
                                        enabled = false,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                            disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }

                            if (showStartPicker) {
                                val parts = newStartTime.split(":")
                                val h = parts.getOrNull(0)?.toIntOrNull() ?: 9
                                val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                CreateRuleTimePickerDialog(
                                    title = "Select Start Time",
                                    initialHour = h,
                                    initialMinute = m,
                                    onDismiss = { showStartPicker = false },
                                    onConfirm = { hour, minute ->
                                        newStartTime = String.format("%02d:%02d", hour, minute)
                                        showStartPicker = false
                                    }
                                )
                            }

                            if (showEndPicker) {
                                val parts = newEndTime.split(":")
                                val h = parts.getOrNull(0)?.toIntOrNull() ?: 17
                                val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                CreateRuleTimePickerDialog(
                                    title = "Select End Time",
                                    initialHour = h,
                                    initialMinute = m,
                                    onDismiss = { showEndPicker = false },
                                    onConfirm = { hour, minute ->
                                        newEndTime = String.format("%02d:%02d", hour, minute)
                                        showEndPicker = false
                                    }
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
                            
                            val context = LocalContext.current
                            val timeRegex = remember { Regex("^(?:[01]\\d|2[0-3]):[0-5]\\d$") }
                            Button(
                                onClick = {
                                    if (newStartTime.isNotBlank() && newEndTime.isNotBlank() && newSelectedDays.isNotEmpty()) {
                                        if (!timeRegex.matches(newStartTime) || !timeRegex.matches(newEndTime)) {
                                            android.widget.Toast.makeText(context, "Invalid time format. Please use HH:mm", android.widget.Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        val newPeriod = SchedulePeriod(
                                            startTime = newStartTime,
                                            endTime = newEndTime,
                                            days = newSelectedDays.toList()
                                        )
                                        val hasOverlap = periodsList.any { ScheduleUtils.periodsOverlap(it, newPeriod) }
                                        if (hasOverlap) {
                                            android.widget.Toast.makeText(context, "This window overlaps with an existing window in this rule.", android.widget.Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        periodsList.add(newPeriod)
                                    } else {
                                        android.widget.Toast.makeText(context, "Please select times and at least one day.", android.widget.Toast.LENGTH_SHORT).show()
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

                    val saveEnabled = ruleName.isNotBlank()
                    Button(
                        onClick = {
                            if (saveEnabled) {
                                val finalPeriods = ScheduleUtils.mergeSchedules(periodsList.toList())
                                val allToMerge = mutableListOf<SchedulePeriod>()
                                allToMerge.addAll(finalPeriods)
                                
                                val existingConflictingRulesOnSave = state.scheduleRules.filter { rule ->
                                    rule.isActive && rule.restrictionType == targetType &&
                                    (rule.selectedBlockers.any { selectedBlockerTypes.contains(it) } ||
                                     selectedBlockerTypes.contains(rule.targetBlockerType))
                                }
                                
                                for (rule in existingConflictingRulesOnSave) {
                                    val rulePeriods = if (rule.periods.isNotEmpty()) rule.periods else listOf(SchedulePeriod(rule.startTime, rule.endTime, rule.days))
                                    allToMerge.addAll(rulePeriods)
                                    viewModel.deleteScheduleRule(rule.id)
                                }
                                
                                val fullyMergedPeriods = if (allToMerge.isEmpty()) {
                                    listOf(SchedulePeriod("00:00", "23:59", listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")))
                                } else {
                                    ScheduleUtils.mergeSchedules(allToMerge)
                                }
                                val firstPeriod = fullyMergedPeriods.first()
                                
                                val limitValue = limitValueStr.toIntOrNull() ?: 5
                                
                                val appOrCategoryDisplay = if (selectedBlockerTypes.size == 1) {
                                    when (selectedBlockerTypes.first()) {
                                        "App Blocker" -> if (selectedApps.isNotEmpty()) "${selectedApps.size} Apps" else "All Apps"
                                        "Keyword Blocker" -> if (selectedKeywords.isNotEmpty()) "${selectedKeywords.size} Keywords" else "Keywords Active"
                                        "Website Blocker" -> if (selectedWebsites.isNotEmpty()) "${selectedWebsites.size} Domains" else "Websites Active"
                                        "Reels Blocker" -> if (selectedPlatforms.isNotEmpty()) selectedPlatforms.joinToString(", ") else "All Reels"
                                        "Notification Shielder" -> "Mute Distractions"
                                        else -> selectedBlockerTypes.first()
                                    }
                                } else {
                                    selectedBlockerTypes.joinToString(" • ")
                                }

                                val newRule = ScheduleRule(
                                    id = editingRule?.id ?: UUID.randomUUID().toString(),
                                    name = ruleName,
                                    appOrCategory = appOrCategoryDisplay,
                                    restrictionType = targetType,
                                    startTime = firstPeriod.startTime,
                                    endTime = firstPeriod.endTime,
                                    days = firstPeriod.days,
                                    limitValue = limitValue,
                                    isActive = true,
                                    periods = fullyMergedPeriods,
                                    targetBlockerType = selectedBlockerTypes.firstOrNull() ?: "App Blocker",
                                    selectedApps = selectedApps.toList(),
                                    selectedKeywords = selectedKeywords.toList(),
                                    selectedWebsites = selectedWebsites.toList(),
                                    selectedPlatforms = selectedPlatforms.toList(),
                                    selectedBlockers = selectedBlockerTypes.toList()
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
