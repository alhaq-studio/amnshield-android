package com.alhaq.amnshield.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.alhaq.amnshield.ui.viewmodel.AmnShieldViewModel

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
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        // Active protection summary card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.isMainServiceEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
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

        item {
            Text(
                text = "CORE PROTECTION",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.8.sp
            )
        }

        // Core protection list cards grouped inside a single Card Container
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
                    BlockItemRow(
                        icon = Icons.Outlined.Lock,
                        title = "App Blocker",
                        summary = "Blocked apps and categories",
                        statusText = if (state.isAppBlockerEnabled) "ON" else "OFF",
                        onChecked = onNavigateToAppBlocker,
                        iconColor = Color(0xFF6366F1)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    BlockItemRow(
                        icon = Icons.Outlined.Label,
                        title = "Keyword Blocker",
                        summary = "Keywords and adult content packs",
                        statusText = "${state.keywords.size} keywords",
                        onChecked = onNavigateToKeywordBlocker,
                        iconColor = Color(0xFFEF4444)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    BlockItemRow(
                        icon = Icons.Outlined.PublicOff,
                        title = "Website/URL Blocker",
                        summary = "Block websites and custom URLs",
                        statusText = if (state.isWebFilterEnabled) "ON" else "OFF",
                        onChecked = onNavigateToWebBlocker,
                        iconColor = Color(0xFFEC4899)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    BlockItemRow(
                        icon = Icons.Outlined.VideoLibrary,
                        title = "Reels Blocker",
                        summary = "Block short-form video algorithms",
                        statusText = if (state.isReelsBlockerEnabled) "ON" else "OFF",
                        onChecked = onNavigateToReelsBlocker,
                        iconColor = Color(0xFFF43F5E)
                    )
                }
            }
        }


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
            .clickable { onChecked() }
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
                color = if (statusText.contains("ON") || (statusText.any { it.isDigit() && it != '0' } && !statusText.contains("0 rules"))) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (statusText.contains("ON") || (statusText.any { it.isDigit() && it != '0' } && !statusText.contains("0 rules"))) {
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
