package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldKeep
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.AmberWarning

@Composable
fun SettingsDialog(
    currentThemeMode: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    isWorkerRunning: Boolean,
    taggedCount: Int,
    bgWorkerMessage: String?,
    onRunBackgroundWorker: () -> Unit,
    onReplayOnboarding: () -> Unit,
    onShowRateApp: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .padding(vertical = 16.dp)
                .testTag("settings_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(CyanPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Section 1: Appearance (Theme)
                SettingsSectionHeader(title = "Appearance", icon = Icons.Default.Palette)
                Spacer(modifier = Modifier.height(10.dp))

                ThemeOptionItem(
                    title = "Dark Theme",
                    subtitle = "Midnight slate tones, easy on the eyes",
                    icon = Icons.Default.DarkMode,
                    isSelected = currentThemeMode == ThemeMode.DARK,
                    onClick = { onThemeSelected(ThemeMode.DARK) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                ThemeOptionItem(
                    title = "Light Theme",
                    subtitle = "Clean white design with high contrast",
                    icon = Icons.Default.LightMode,
                    isSelected = currentThemeMode == ThemeMode.LIGHT,
                    onClick = { onThemeSelected(ThemeMode.LIGHT) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                ThemeOptionItem(
                    title = "System Automatic",
                    subtitle = "Matches your Android system settings",
                    icon = Icons.Default.SystemUpdateAlt,
                    isSelected = currentThemeMode == ThemeMode.SYSTEM,
                    onClick = { onThemeSelected(ThemeMode.SYSTEM) }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Section 2: Hashing & Metadata Scan Worker
                SettingsSectionHeader(title = "Background Engine", icon = Icons.Default.AutoAwesome)
                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "File Hashing & Aspect Ratio",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Performs deep offline scans using MD5 stream hashing to tag screen captures & heavy media files.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Status badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isWorkerRunning) CyanPrimary else MaterialTheme.colorScheme.outline)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isWorkerRunning) "Status: Scanning background files..." else "Status: Idle",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isWorkerRunning) CyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (taggedCount > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tagged $taggedCount duplicate or low-quality items",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = EmeraldKeep
                            )
                        }

                        bgWorkerMessage?.let {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = onRunBackgroundWorker,
                            enabled = !isWorkerRunning,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanPrimary,
                                contentColor = Color.Black,
                                disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("run_background_worker_settings")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isWorkerRunning) "SCANNING..." else "START BACKGROUND SCAN",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Section 3: Help & Guide
                SettingsSectionHeader(title = "App Guide & Feedback", icon = Icons.Default.Info)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onReplayOnboarding()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("replay_onboarding_button")
                    ) {
                        Text(
                            text = "Reset Guide",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            onShowRateApp()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberWarning,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("rate_app_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Rate App",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Section 4: Privacy Seal
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = EmeraldKeep.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, EmeraldKeep.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Security",
                            tint = EmeraldKeep,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "100% Secure Offline AI",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = EmeraldKeep
                            )
                            Text(
                                text = "All media analysis, blurring detection, and hashing are strictly performed local-only.",
                                style = MaterialTheme.typography.bodySmall,
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
private fun SettingsSectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CyanPrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = CyanPrimary
        )
    }
}

@Composable
private fun ThemeOptionItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) CyanPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) CyanPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) CyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (isSelected) "Selected" else "Not Selected",
                tint = if (isSelected) CyanPrimary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
