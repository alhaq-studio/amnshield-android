package com.alhaq.amnshield.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alhaq.amnshield.utils.ReelsStatsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelsMetricsScreen(
    summary: ReelsStatsManager.ReelsMetricsSummary,
    dailyLimit: Int,
    isBlockerEnabled: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onConfigureRules: () -> Unit
) {
    val totalScrolled = summary.totalScrolledToday
    val watchTimeSec = summary.totalWatchTimeTodaySeconds
    val watchMins = (watchTimeSec / 60).toInt()
    val watchRemainingSec = (watchTimeSec % 60).toInt()

    val formattedWatchTime = if (watchMins > 0) "${watchMins}m ${watchRemainingSec}s" else "${watchRemainingSec}s"
    val avgWatchSec = summary.avgWatchTimePerReelSeconds
    val avgFormatted = if (avgWatchSec >= 60) "${avgWatchSec / 60}m ${avgWatchSec % 60}s" else "${avgWatchSec}s/video"

    val limitPct = if (dailyLimit > 0) ((totalScrolled.toFloat() / dailyLimit.toFloat())).coerceIn(0f, 1f) else 0f
    val limitPctText = "${(limitPct * 100).toInt()}%"

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Column {
                        Text(
                            "Reels & Shorts Metrics",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            "Short-Form Video Consumption & Scroll Analytics",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
        ) {
            // Hero Today Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Today's Scroll Total",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "$totalScrolled Reels Scrolled",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // Active / Paused Badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isBlockerEnabled) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                border = BorderStroke(1.dp, if (isBlockerEnabled) Color(0xFF10B981) else MaterialTheme.colorScheme.error)
                            ) {
                                Text(
                                    text = if (isBlockerEnabled) "BLOCKER ACTIVE 🟢" else "PAUSED 🔴",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isBlockerEnabled) Color(0xFF059669) else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Stats Summary Row (Watch Time & Avg Speed)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Icon(Icons.Outlined.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = formattedWatchTime, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(text = "Watch Duration", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Icon(Icons.Outlined.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = avgFormatted, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(text = "Avg per Video", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        if (dailyLimit > 0) {
                            Spacer(modifier = Modifier.height(16.dp))

                            // Daily Limit Progress Bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Daily Limit Usage", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$totalScrolled / $dailyLimit ($limitPctText)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (limitPct >= 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { limitPct },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                color = if (limitPct >= 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }

            // 7-Day Scroll Trend Chart Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "7-DAY SCROLL TREND",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.05.sp
                                )
                                Text(
                                    text = "Peak: ${summary.peakScrollDayLabel} (${summary.peakScrollCount} reels)",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Outlined.BarChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        val maxRecordVal = (summary.dailyRecords.maxOfOrNull { it.totalScrolled } ?: 1).coerceAtLeast(1)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            summary.dailyRecords.forEach { record ->
                                val barHeightRatio = (record.totalScrolled.toFloat() / maxRecordVal.toFloat()).coerceIn(0.05f, 1f)
                                val isPeak = record.totalScrolled == maxRecordVal && maxRecordVal > 0

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (record.totalScrolled > 0) {
                                        Text(
                                            text = record.totalScrolled.toString(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPeak) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }

                                    Box(
                                        modifier = Modifier
                                            .width(20.dp)
                                            .fillMaxHeight(barHeightRatio)
                                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                            .background(
                                                if (isPeak) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                            )
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = record.dayLabel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Platform Breakdown Section Header
            item {
                Text(
                    text = "PLATFORM BREAKDOWN",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.08.sp
                )
            }

            // Platform Breakdown Items
            items(summary.platformBreakdownToday) { platform ->
                val platformPct = if (totalScrolled > 0) (platform.scrolledCount.toFloat() / totalScrolled.toFloat()).coerceIn(0f, 1f) else 0f
                val platformPctText = "${(platformPct * 100).toInt()}%"

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(getPlatformColor(platform.platformKey).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getPlatformIcon(platform.platformKey),
                                contentDescription = null,
                                tint = getPlatformColor(platform.platformKey),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = platform.displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${platform.scrolledCount} reels ($platformPctText)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = getPlatformColor(platform.platformKey)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = { platformPct },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                color = getPlatformColor(platform.platformKey),
                                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }

            // Quick Configure Button
            item {
                Button(
                    onClick = onConfigureRules,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Configure Reels Blocker Limits & Rules", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun getPlatformIcon(platformKey: String): ImageVector {
    return when (platformKey) {
        "instagram" -> Icons.Default.CameraAlt
        "youtube" -> Icons.Default.PlayArrow
        "tiktok" -> Icons.Default.MusicNote
        "facebook" -> Icons.Default.ThumbUp
        "snapchat" -> Icons.Default.FlashOn
        "browser" -> Icons.Default.Language
        else -> Icons.Default.VideoLibrary
    }
}

private fun getPlatformColor(platformKey: String): Color {
    return when (platformKey) {
        "instagram" -> Color(0xFFE1306C)
        "youtube" -> Color(0xFFFF0000)
        "tiktok" -> Color(0xFF00F2FE)
        "facebook" -> Color(0xFF1877F2)
        "snapchat" -> Color(0xFFEAB308)
        "browser" -> Color(0xFF3B82F6)
        else -> Color(0xFF10B981)
    }
}
