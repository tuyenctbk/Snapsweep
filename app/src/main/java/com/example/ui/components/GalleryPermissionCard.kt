package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.EmeraldKeep
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun GalleryPermissionCard(
    hasImagesPermission: Boolean,
    hasVideosPermission: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allGranted = hasImagesPermission && hasVideosPermission

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("permission_request_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (allGranted) EmeraldKeep.copy(alpha = 0.5f) else CyanPrimary.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
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
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (allGranted) EmeraldKeep.copy(alpha = 0.15f)
                                else CyanPrimary.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (allGranted) Icons.Default.CheckCircle else Icons.Default.PhotoLibrary,
                            contentDescription = "Gallery Access",
                            tint = if (allGranted) EmeraldKeep else CyanPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (allGranted) "GALLERY ACCESS GRANTED" else "GALLERY PERMISSION REQUIRED",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = if (allGranted) EmeraldKeep else CyanPrimary
                        )
                        Text(
                            text = if (allGranted) "Ready to scan local photos & videos" else "Read Photos & Videos Access",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }
                }

                // 100% Privacy Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(PurpleAccent.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Privacy Shield",
                            tint = PurpleAccent,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "100% OFFLINE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp
                            ),
                            color = PurpleAccent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Explanation Text
            Text(
                text = if (allGranted) {
                    "SnapSweep has full access to read local media on your device to analyze blurry photos, screenshots, and duplicate bursts directly on-device."
                } else {
                    "SnapSweep needs permission to read photos and videos so it can scan your gallery for duplicate bursts, blurry photos, old screenshots, and large files. Your photos are analyzed 100% locally on your device and are never uploaded."
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Individual Permission Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PermissionBadge(
                    label = "Read Images",
                    isGranted = hasImagesPermission,
                    modifier = Modifier.weight(1f)
                )
                PermissionBadge(
                    label = "Read Videos",
                    isGranted = hasVideosPermission,
                    modifier = Modifier.weight(1f)
                )
            }

            if (!allGranted) {
                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onRequestPermission,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("grant_permission_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanPrimary,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GRANT GALLERY ACCESS",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        )
                    }

                    OutlinedButton(
                        onClick = { openAppSettings(context) },
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("open_settings_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionBadge(
    label: String,
    isGranted: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (isGranted) EmeraldKeep.copy(alpha = 0.12f) else DarkBorder.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isGranted) EmeraldKeep.copy(alpha = 0.3f) else DarkBorder
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Lock,
                contentDescription = null,
                tint = if (isGranted) EmeraldKeep else TextMuted,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (isGranted) EmeraldKeep else TextMuted
            )
        }
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
