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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alhaq.amnshield.ui.components.AmnShieldToggleButton

@Composable
fun HomeScreen(
    isMainServiceEnabled: Boolean,
    isPremiumUser: Boolean,
    isFocusModeActive: Boolean,
    isAppBlockerEnabled: Boolean,
    isKeywordBlockerEnabled: Boolean,
    isUsageTrackerEnabled: Boolean,
    isReelBlockerEnabled: Boolean,
    isAntiUninstallEnabled: Boolean,
    onFeatureClicked: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
    ) {
        // Active protection summary card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isMainServiceEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
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
                                if (isMainServiceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isMainServiceEnabled) Icons.Default.Shield else Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = if (isMainServiceEnabled) "Protection Active" else "Protection Disabled",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isMainServiceEnabled) "All offline blocking shields are scanning." else "Please enable Accessibility Service.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Features quick actions label
        item {
            Text(
                text = "FEATURES",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.08.sp
            )
        }

        // Features grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FeatureGridCard(
                        modifier = Modifier.weight(1f),
                        title = "App Block",
                        isActive = isAppBlockerEnabled,
                        onClick = { onFeatureClicked("app_blocker") }
                    )
                    FeatureGridCard(
                        modifier = Modifier.weight(1f),
                        title = "Web Filter",
                        isActive = isKeywordBlockerEnabled,
                        onClick = { onFeatureClicked("keyword_blocker") }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FeatureGridCard(
                        modifier = Modifier.weight(1f),
                        title = "Reels Block",
                        isActive = isReelBlockerEnabled,
                        onClick = { onFeatureClicked("reel_blocker") }
                    )
                    FeatureGridCard(
                        modifier = Modifier.weight(1f),
                        title = "Focus Mode",
                        isActive = isFocusModeActive,
                        onClick = { onFeatureClicked("focus_mode") }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FeatureGridCard(
                        modifier = Modifier.weight(1f),
                        title = "Anti-Uninstall",
                        isActive = isAntiUninstallEnabled,
                        onClick = { onFeatureClicked("anti_uninstall") }
                    )
                    FeatureGridCard(
                        modifier = Modifier.weight(1f),
                        title = "Usage Track",
                        isActive = isUsageTrackerEnabled,
                        onClick = { onFeatureClicked("usage_tracker") }
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FeatureGridCard(
                        modifier = Modifier.weight(1f),
                        title = if (isPremiumUser) "Premium Active" else "Get Premium",
                        isActive = isPremiumUser,
                        onClick = { onFeatureClicked("premium_features") }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun FeatureGridCard(
    title: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline
                            )
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
