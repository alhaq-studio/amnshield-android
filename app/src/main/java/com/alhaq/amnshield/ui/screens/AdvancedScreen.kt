package com.alhaq.amnshield.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alhaq.amnshield.ui.state.AmnShieldState

@Composable
fun AdvancedScreen(
    state: AmnShieldState,
    onNavigateToCheatHours: () -> Unit,
    onNavigateToSchedules: () -> Unit,
    onNavigateToLaunchLimits: () -> Unit,
    onNavigateToAntiUninstall: () -> Unit,
    onNavigateToUsageTracker: () -> Unit,
    onNavigateToPremium: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        // Section: Advanced Rules
        item {
            Text(
                text = "ADVANCED RULES",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.8.sp
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column {
                    AdvancedItemRow(
                        icon = Icons.Outlined.HourglassEmpty,
                        title = "Manage Cheat Hours",
                        summary = "Temporary access windows",
                        statusText = "Setup",
                        onChecked = onNavigateToCheatHours,
                        iconColor = Color(0xFFF59E0B)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    AdvancedItemRow(
                        icon = Icons.Outlined.CalendarToday,
                        title = "Manage Schedules",
                        summary = "Recurring block time windows",
                        statusText = if (state.isScheduleEnabled) "ON" else "0 rules",
                        onChecked = onNavigateToSchedules,
                        iconColor = Color(0xFF3B82F6)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    AdvancedItemRow(
                        icon = Icons.Outlined.Launch,
                        title = "Manage Launch Limits",
                        summary = "Limit app openings per time period",
                        statusText = if (state.isUsageLimitEnabled) "ON" else "0 limits",
                        onChecked = onNavigateToLaunchLimits,
                        iconColor = Color(0xFF8B5CF6)
                    )
                }
            }
        }

        // Section: System Settings
        item {
            Text(
                text = "SYSTEM SETTINGS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.8.sp
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column {
                    AdvancedItemRow(
                        icon = Icons.Outlined.Security,
                        title = "Anti-Uninstall",
                        summary = "Prevent app uninstallation",
                        statusText = if (state.isAntiUninstallEnabled) "ON" else "OFF",
                        onChecked = onNavigateToAntiUninstall,
                        iconColor = Color(0xFF10B981)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    AdvancedItemRow(
                        icon = Icons.Outlined.Analytics,
                        title = "Usage Tracker",
                        summary = "Track detailed screen time data",
                        statusText = if (state.isUsageTrackerEnabled) "ON" else "OFF",
                        onChecked = onNavigateToUsageTracker,
                        iconColor = Color(0xFF0EA5E9)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    AdvancedItemRow(
                        icon = Icons.Outlined.Star,
                        title = "Premium Features",
                        summary = if (state.isPremiumUser) "Premium Active" else "Upgrade to Premium",
                        statusText = if (state.isPremiumUser) "ACTIVE" else "GET",
                        onChecked = onNavigateToPremium,
                        iconColor = Color(0xFFF59E0B)
                    )
                }
            }
        }
    }
}

@Composable
fun AdvancedItemRow(
    icon: ImageVector,
    title: String,
    summary: String,
    statusText: String,
    onChecked: () -> Unit,
    iconColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChecked() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
