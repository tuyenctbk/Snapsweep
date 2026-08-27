package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StorageStats
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.CyanPrimaryDark
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldKeep
import com.example.ui.theme.LightBackground
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

@Composable
fun StorageHeader(
    stats: StorageStats,
    isScanning: Boolean,
    scanProgress: Float,
    modifier: Modifier = Modifier
) {
    // Space Saved Target Calculation
    val targetBytes = remember(stats.totalBytesFreed) {
        when {
            stats.totalBytesFreed < 100 * 1024 * 1024L -> 100 * 1024 * 1024L // 100 MB Target
            stats.totalBytesFreed < 500 * 1024 * 1024L -> 500 * 1024 * 1024L // 500 MB Target
            stats.totalBytesFreed < 2 * 1024 * 1024 * 1024L -> 2 * 1024 * 1024 * 1024L // 2 GB Target
            else -> 10 * 1024 * 1024 * 1024L // 10 GB Target
        }
    }

    val goalProgress = (stats.totalBytesFreed.toFloat() / targetBytes.toFloat()).coerceIn(0f, 1f)
    val formattedTarget = remember(targetBytes) {
        val gb = targetBytes / (1024f * 1024f * 1024f)
        if (gb >= 1.0f) String.format("%.0f GB Goal", gb) else String.format("%.0f MB Goal", targetBytes / (1024f * 1024f))
    }

    val rankTitle = remember(stats.totalBytesFreed) {
        when {
            stats.totalBytesFreed < 50 * 1024 * 1024L -> "🌱 Storage Starter"
            stats.totalBytesFreed < 200 * 1024 * 1024L -> "🚀 Space Cadet"
            stats.totalBytesFreed < 1000 * 1024 * 1024L -> "🏆 Storage Hero"
            else -> "👑 Storage Master"
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("storage_header")
    ) {
        // Space Saved Dashboard Hero Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    if (MaterialTheme.colorScheme.background == LightBackground) {
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFF1F5F9),
                                Color(0xFFE2E8F0),
                                Color(0xFFEDE9FE)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1E293B),
                                Color(0xFF0F172A),
                                Color(0xFF1E1B4B)
                            )
                        )
                    }
                )
                .border(1.dp, Brush.horizontalGradient(listOf(CyanPrimary.copy(alpha = 0.5f), DarkBorder)), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(CyanPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CleaningServices,
                                contentDescription = "Cleaner",
                                tint = CyanPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "SPACE SAVED DASHBOARD",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.2.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = TextMuted
                            )
                            Text(
                                text = stats.formattedFreedStorage,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Black
                                ),
                                color = TextPrimary
                            )
                        }
                    }

                    // Rank Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(EmeraldKeep.copy(alpha = 0.15f))
                            .border(1.dp, EmeraldKeep.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = rankTitle,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldKeep
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Storage Reclaimed Goal Progress Bar
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Reclamation Progress (${(goalProgress * 100).toInt()}%)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = CyanPrimary
                        )
                        Text(
                            text = "${stats.totalItemsCleaned} Items Cleaned • $formattedTarget",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { goalProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = CyanPrimary,
                        trackColor = DarkSurfaceVariant
                    )
                }

                // Scanning Progress bar if active
                if (isScanning) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val animatedProgress by animateFloatAsState(
                        targetValue = scanProgress,
                        animationSpec = tween(durationMillis = 300),
                        label = "progress"
                    )
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                val lottieComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(com.example.R.raw.clean_sweep))
                                val lottieProgress by animateLottieCompositionAsState(
                                    composition = lottieComposition,
                                    iterations = 100
                                )
                                LottieAnimation(
                                    composition = lottieComposition,
                                    progress = { lottieProgress },
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Scanning MediaStore...",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = CyanPrimary
                                    )
                                    Text(
                                        text = "Analyzing metadata & finding duplicates",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                            }
                            Text(
                                text = "${(animatedProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = CyanPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = CyanPrimary,
                            trackColor = DarkSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Privacy Trust Badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurface.copy(alpha = 0.6f))
                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Privacy",
                tint = EmeraldKeep,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "100% On-Device & Offline • Zero Cloud • $0 Ads",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = TextSecondary
            )
        }
    }
}
