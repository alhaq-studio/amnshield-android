package com.alhaq.amnshield.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
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
import com.alhaq.amnshield.ui.state.ScheduleRule
import com.alhaq.amnshield.ui.viewmodel.AmnShieldViewModel
import com.alhaq.amnshield.ui.components.bounceClick

@Composable
fun BlocksScreen(
    state: AmnShieldState,
    viewModel: AmnShieldViewModel,
    onNavigateToAppBlocker: () -> Unit = {},
    onNavigateToKeywordBlocker: () -> Unit = {},
    onNavigateToWebBlocker: () -> Unit = {},
    onNavigateToFocusMode: () -> Unit = {},
    onNavigateToCheatHours: () -> Unit = {},
    onNavigateToSchedules: () -> Unit = {},
    onNavigateToLaunchLimits: () -> Unit = {},
    onNavigateToAntiUninstall: () -> Unit = {},
    onNavigateToUsageTracker: () -> Unit = {},
    onNavigateToReelsBlocker: () -> Unit = {},
    onNavigateToPremium: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        // Active protection summary card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (state.isMainServiceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (state.isMainServiceEnabled) Icons.Default.Shield else Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = if (state.isMainServiceEnabled) "Protection Active" else "Protection Disabled",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (state.isMainServiceEnabled) "All offline blocking shields are scanning." else "Please enable Accessibility Service.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (state.isAdvancedMode) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "QUICK ACTIONS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.8.sp
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        QuickActionChip(
                            icon = Icons.Outlined.Lock,
                            label = "App Blocker",
                            color = Color(0xFF6366F1),
                            onClick = onNavigateToAppBlocker
                        )
                        QuickActionChip(
                            icon = Icons.Outlined.Label,
                            label = "Keywords",
                            color = Color(0xFFEF4444),
                            onClick = onNavigateToKeywordBlocker
                        )
                        QuickActionChip(
                            icon = Icons.Outlined.PublicOff,
                            label = "Websites",
                            color = Color(0xFFEC4899),
                            onClick = onNavigateToWebBlocker
                        )
                        QuickActionChip(
                            icon = Icons.Outlined.VideoLibrary,
                            label = "Reels",
                            color = Color(0xFFF43F5E),
                            onClick = onNavigateToReelsBlocker
                        )
                    }
                }
            }
        }

        // Section 1: Active Feature Blockers
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "BLOCKERS & SHIELDS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.8.sp
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column {
                        BlockItemRow(
                            icon = Icons.Outlined.Lock,
                            title = "App Blocker",
                            summary = "Block distracting apps & games",
                            statusText = if (state.isAppBlockerEnabled) "ON" else "OFF",
                            onChecked = onNavigateToAppBlocker,
                            iconColor = Color(0xFF6366F1)
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        BlockItemRow(
                            icon = Icons.Outlined.Label,
                            title = "Keyword Blocker",
                            summary = "Filter web searches & content keywords",
                            statusText = if (state.isKeywordBlockerEnabled) "ON" else "OFF",
                            onChecked = onNavigateToKeywordBlocker,
                            iconColor = Color(0xFFEF4444)
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        BlockItemRow(
                            icon = Icons.Outlined.PublicOff,
                            title = "Website Blocker",
                            summary = "Restrict explicit & adult websites",
                            statusText = if (state.isWebFilterEnabled) "ON" else "OFF",
                            onChecked = onNavigateToWebBlocker,
                            iconColor = Color(0xFFEC4899)
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        BlockItemRow(
                            icon = Icons.Outlined.VideoLibrary,
                            title = "Reels & Shorts Blocker",
                            summary = "Stop addictive short video feeds",
                            statusText = if (state.isReelsBlockerEnabled) "ON" else "OFF",
                            onChecked = onNavigateToReelsBlocker,
                            iconColor = Color(0xFFF43F5E)
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        BlockItemRow(
                            icon = Icons.Outlined.CenterFocusStrong,
                            title = "Focus Space",
                            summary = "Deep work sessions & custom app rules",
                            statusText = if (state.isFocusModeActive) "ACTIVE" else "OFF",
                            onChecked = onNavigateToFocusMode,
                            iconColor = Color(0xFF10B981)
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        BlockItemRow(
                            icon = Icons.Outlined.Security,
                            title = "Anti-Uninstall Safeguard",
                            summary = "Prevent unauthorized app removal",
                            statusText = if (state.isAntiUninstallEnabled) "ON" else "OFF",
                            onChecked = onNavigateToAntiUninstall,
                            iconColor = Color(0xFFF59E0B)
                        )
                    }
                }
            }
        }

        // Section 2: Schedules & Rules Overview
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "SCHEDULES & MANAGEMENT",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.8.sp
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column {
                        val rulesCount = state.scheduleRules.size
                        BlockItemRow(
                            icon = Icons.Outlined.Schedule,
                            title = "Block Schedules & Rules",
                            summary = "Manage 24/7, daily & weekly block rules",
                            statusText = if (rulesCount > 0) "$rulesCount rules" else "0 rules",
                            onChecked = onNavigateToSchedules,
                            iconColor = Color(0xFF8B5CF6)
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        BlockItemRow(
                            icon = Icons.Outlined.HourglassTop,
                            title = "Launch & Usage Limits",
                            summary = "Set daily app open caps & duration limits",
                            statusText = if (state.isUsageLimitEnabled) "ACTIVE" else "OFF",
                            onChecked = onNavigateToLaunchLimits,
                            iconColor = Color(0xFF3B82F6)
                        )
                    }
                }
            }
        }

        // Section 3: Summary of Created Rules (if any)
        if (state.scheduleRules.isNotEmpty()) {
            item {
                Text(
                    text = "ACTIVE SCHEDULE RULES (${state.scheduleRules.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(state.scheduleRules, key = { it.id }) { rule ->
                BlocksSummaryRuleCard(
                    rule = rule,
                    onClick = onNavigateToSchedules
                )
            }
        }
    }
}

@Composable
fun QuickActionChip(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.08f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.4f)), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
fun BlockItemRow(
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
            .bounceClick { onChecked() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
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

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (statusText.contains("ON") || statusText.contains("ACTIVE") || (statusText.any { it.isDigit() && it != '0' } && !statusText.contains("0 rules"))) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (statusText.contains("ON") || statusText.contains("ACTIVE") || (statusText.any { it.isDigit() && it != '0' } && !statusText.contains("0 rules"))) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun BlocksSummaryRuleCard(
    rule: ScheduleRule,
    onClick: () -> Unit
) {
    val blockerColor = when (rule.targetBlockerType) {
        "Keyword Blocker" -> Color(0xFF10B981)
        "Website Blocker" -> Color(0xFF3B82F6)
        "Reels Blocker" -> Color(0xFFEC4899)
        else -> Color(0xFF8B5CF6)
    }

    val ruleName = rule.name.ifBlank { "Unnamed Blocker" }
    val categoryText = rule.appOrCategory.ifBlank { "Apps" }
    val blockerType = rule.targetBlockerType.ifBlank { "App Blocker" }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(blockerColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (blockerType) {
                            "Keyword Blocker" -> Icons.Outlined.Search
                            "Website Blocker" -> Icons.Outlined.Language
                            "Reels Blocker" -> Icons.Outlined.PlayCircleOutline
                            else -> Icons.Outlined.AppShortcut
                        },
                        contentDescription = null,
                        tint = blockerColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = ruleName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$categoryText • $blockerType",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (rule.isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = if (rule.isActive) "ACTIVE" else "OFF",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (rule.isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
