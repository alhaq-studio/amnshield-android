package com.alhaq.amnshield.ui.screens

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alhaq.amnshield.ui.state.AmnShieldState
import com.alhaq.amnshield.ui.viewmodel.AmnShieldViewModel
import com.alhaq.amnshield.ui.state.AppTheme
import com.alhaq.amnshield.ui.components.AmnShieldToggleButton
import com.alhaq.amnshield.ui.components.bounceClick

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: AmnShieldState,
    viewModel: AmnShieldViewModel,
    onNavigateToProfile: () -> Unit,
    onBackupRestore: () -> Unit,
    onReminders: () -> Unit,
    onShareCrashLogs: () -> Unit,
    onHelpFAQ: () -> Unit,
    onAbout: () -> Unit,
    onLanguage: () -> Unit,
    onSignOut: () -> Unit,
    onToggleWebFilter: (Boolean) -> Unit,
    onToggleUsageLimit: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
        ) {
            // USER PROFILE & ACCOUNT
            item {
                Text(
                    text = "PROFILE & ACCOUNT",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.8.sp
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceClick { onNavigateToProfile() },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = state.userName.take(2).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.userName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = state.userEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Edit Profile",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // APPEARANCE
            item {
                Text(
                    text = "APPEARANCE",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.8.sp
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column {
                        // Custom Theme Selection row
                        var expandedThemeMenu by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bounceClick { expandedThemeMenu = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column {
                                    Text(
                                        "App Theme",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = when (state.currentTheme) {
                                            AppTheme.SUNSET_GLOW -> "Sunset Glow (Warm Sand)"
                                            AppTheme.EMERALD_CALM -> "Emerald Calm (Pearl Teal)"
                                            AppTheme.COSMIC_NIGHT -> "Cosmic Night (Deep Violet)"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Selection popup Menu drop-down option
                            Box {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                DropdownMenu(
                                    expanded = expandedThemeMenu,
                                    onDismissRequest = { expandedThemeMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Sunset Glow (Warm Sand)") },
                                        onClick = {
                                            viewModel.updateTheme(AppTheme.SUNSET_GLOW)
                                            val prefs = context.getSharedPreferences("theme_prefs", android.content.Context.MODE_PRIVATE)
                                            prefs.edit().putString("theme_style", "sunset").apply()
                                            expandedThemeMenu = false
                                            (context as? android.app.Activity)?.recreate()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Emerald Calm (Pearl Teal)") },
                                        onClick = {
                                            viewModel.updateTheme(AppTheme.EMERALD_CALM)
                                            val prefs = context.getSharedPreferences("theme_prefs", android.content.Context.MODE_PRIVATE)
                                            prefs.edit().putString("theme_style", "emerald").apply()
                                            expandedThemeMenu = false
                                            (context as? android.app.Activity)?.recreate()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Cosmic Night (Deep Violet)") },
                                        onClick = {
                                            viewModel.updateTheme(AppTheme.COSMIC_NIGHT)
                                            val prefs = context.getSharedPreferences("theme_prefs", android.content.Context.MODE_PRIVATE)
                                            prefs.edit().putString("theme_style", "purple").apply()
                                            expandedThemeMenu = false
                                            (context as? android.app.Activity)?.recreate()
                                        }
                                    )
                                }
                            }
                        }

                        Divider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bounceClick { onLanguage() }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Text(
                                    "Language",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "Change",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // FEATURES
            item {
                Text(
                    text = "FEATURES",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.8.sp
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column {
                        // Feature Toggle: Usage Tracker
                        SettingsToggleRow(
                            icon = Icons.Default.BarChart,
                            title = "Usage Tracker",
                            checked = state.isUsageLimitEnabled,
                            onCheckedChange = { onToggleUsageLimit(it) }
                        )
                    }
                }
            }

            // HOME SCREEN WIDGETS
            item {
                Text(
                    text = "HOME SCREEN WIDGETS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.8.sp
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        WidgetPinRow(
                            icon = "🛡️",
                            title = "Today's Overview Widget",
                            description = "Keep track of active shield status and distraction interventions in real-time.",
                            onPin = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    val appWidgetManager = AppWidgetManager.getInstance(context)
                                    val provider = ComponentName(context, "com.alhaq.amnshield.ui.widgets.ScreentimeWidgetProvider")
                                    if (appWidgetManager.isRequestPinAppWidgetSupported) {
                                        appWidgetManager.requestPinAppWidget(provider, null, null)
                                    } else {
                                        Toast.makeText(context, "Launcher does not support widget pinning", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Feature requires Android 8.0+", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }

            // GENERAL
            item {
                Text(
                    text = "GENERAL",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.8.sp
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column {
                        SettingsNavigationRow(icon = Icons.Outlined.CloudUpload, title = "Backup & Restore", onClick = onBackupRestore)
                        Divider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        SettingsNavigationRow(icon = Icons.Outlined.Notifications, title = "Reminders", onClick = onReminders)
                        Divider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        SettingsNavigationRow(icon = Icons.Outlined.BugReport, title = "Share Crash Logs", onClick = onShareCrashLogs)
                    }
                }
            }

            // ABOUT
            item {
                Text(
                    text = "ABOUT",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.8.sp
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column {
                        SettingsNavigationRow(icon = Icons.Outlined.HelpOutline, title = "Help & FAQ", onClick = onHelpFAQ)
                        Divider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        SettingsNavigationRow(icon = Icons.Outlined.Info, title = "About AmnShield", onClick = onAbout)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(onClick = onSignOut) {
                        Text(
                            "Sign Out",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        AmnShieldToggleButton(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsNavigationRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun WidgetPinRow(
    icon: String,
    title: String,
    description: String,
    onPin: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 22.sp)
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onPin,
                modifier = Modifier.height(36.dp),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add to Home Screen", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
