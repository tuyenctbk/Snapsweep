package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.data.model.CategorySummary
import com.example.data.model.MediaCategory
import com.example.data.model.MediaItem
import com.example.data.model.StorageStats
import com.example.ui.components.CategoryCard
import com.example.ui.components.GalleryPermissionCard
import com.example.ui.components.PhotoGallery
import com.example.ui.components.StorageHeader
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldKeep
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.RoseTrash
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

import com.example.ui.components.ShareAppCard
import androidx.compose.material.icons.filled.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    stats: StorageStats,
    categories: List<CategorySummary>,
    pendingTrash: List<MediaItem>,
    isScanning: Boolean,
    scanProgress: Float,
    userNotification: String?,
    hasImagesPermission: Boolean = true,
    hasVideosPermission: Boolean = true,
    isWorkerRunning: Boolean = false,
    taggedCount: Int = 0,
    bgWorkerMessage: String? = null,
    onRunBackgroundWorker: () -> Unit = {},
    onRequestPermission: () -> Unit = {},
    onRefreshScan: () -> Unit,
    onOpenCategory: (MediaCategory) -> Unit,
    onOpenTrashSheet: () -> Unit,
    onClearNotification: () -> Unit,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userNotification) {
        userNotification?.let {
            snackbarHostState.showSnackbar(it)
            onClearNotification()
        }
    }

    val onThisDaySummary = categories.firstOrNull { it.category == MediaCategory.ON_THIS_DAY }
    val standardCategories = categories.filter { it.category != MediaCategory.ON_THIS_DAY }

    val totalPendingTrashBytes = remember(pendingTrash) { pendingTrash.sumOf { it.sizeBytes } }
    val formattedTrashSize = rememberFormattedBytes(totalPendingTrashBytes)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CyanPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CleaningServices,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "SnapSweep",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            ),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyanPrimary.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "FREE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = CyanPrimary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = CyanPrimary
                        )
                    }

                    IconButton(
                        onClick = onRefreshScan,
                        modifier = Modifier.testTag("refresh_scan_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Scan",
                            tint = CyanPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            // Floating Pending Trash Bar
            if (pendingTrash.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("floating_trash_bar")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(RoseTrash, Color(0xFFBE123C))
                                )
                            )
                            .clickable { onOpenTrashSheet() }
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Pending Trash",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "PENDING TRASH QUEUE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        ),
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = "${pendingTrash.size} items • $formattedTrashSize to free",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "PURGE",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                    color = RoseTrash
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWideScreen = maxWidth > 600.dp
            if (isWideScreen) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left Pane: Core stats & status
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StorageHeader(
                            stats = stats,
                            isScanning = isScanning,
                            scanProgress = scanProgress
                        )

                        GalleryPermissionCard(
                            hasImagesPermission = hasImagesPermission,
                            hasVideosPermission = hasVideosPermission,
                            onRequestPermission = onRequestPermission
                        )

                        if (onThisDaySummary != null && onThisDaySummary.count > 0) {
                            OnThisDayCard(
                                summary = onThisDaySummary,
                                onCleanClick = { onOpenCategory(MediaCategory.ON_THIS_DAY) }
                            )
                        }

                        ShareAppCard()
                    }

                    // Right Pane: Active cleanup & Local gallery preview
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SMART CLEANUP QUEUES",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                ),
                                color = TextMuted
                            )
                            Text(
                                text = "100% Offline AI",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanPrimary
                            )
                        }

                        standardCategories.forEach { summary ->
                            CategoryCard(
                                summary = summary,
                                onCleanClick = onOpenCategory
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LOCAL GALLERY PREVIEW",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                ),
                                color = TextMuted
                            )
                            Text(
                                text = "ContentResolver Grid",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanPrimary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                        ) {
                            PhotoGallery(columnsCount = 4)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("dashboard_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Storage Header
                    item {
                        StorageHeader(
                            stats = stats,
                            isScanning = isScanning,
                            scanProgress = scanProgress
                        )
                    }

                    // Gallery Permission Status / Request Card
                    item {
                        GalleryPermissionCard(
                            hasImagesPermission = hasImagesPermission,
                            hasVideosPermission = hasVideosPermission,
                            onRequestPermission = onRequestPermission
                        )
                    }

                    // "On This Day" Micro-Cleanup Feature
                    if (onThisDaySummary != null && onThisDaySummary.count > 0) {
                        item {
                            OnThisDayCard(
                                summary = onThisDaySummary,
                                onCleanClick = { onOpenCategory(MediaCategory.ON_THIS_DAY) }
                            )
                        }
                    }

                    // Smart Categories Header Title
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SMART CLEANUP QUEUES",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                ),
                                color = TextMuted
                            )

                            Text(
                                text = "100% Offline AI",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanPrimary
                            )
                        }
                    }

                    // List of Category Cards
                    items(standardCategories, key = { it.category.name }) { summary ->
                        CategoryCard(
                            summary = summary,
                            onCleanClick = onOpenCategory
                        )
                    }

                    // Local Photo Gallery Title & Component
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LOCAL GALLERY PREVIEW",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                ),
                                color = TextMuted
                            )

                            Text(
                                text = "ContentResolver Grid",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanPrimary
                            )
                        }
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(340.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                        ) {
                            PhotoGallery(columnsCount = 3)
                        }
                    }

                    // Smart Share App Growth Card
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        ShareAppCard()
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun OnThisDayCard(
    summary: CategorySummary,
    onCleanClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF831843),
                        Color(0xFF4C0519),
                        Color(0xFF1E1B4B)
                    )
                )
            )
            .border(1.dp, PurpleAccent.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .clickable { onCleanClick() }
            .padding(18.dp)
            .testTag("on_this_day_card")
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Calendar",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "ON THIS DAY MICRO-CLEANUP",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = summary.formattedTotalSize,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "You captured ${summary.count} photos on this day in past years. Clean up memories you no longer need!",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onCleanClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF831843)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(38.dp)
            ) {
                Text(
                    text = "REVIEW ${summary.count} MEMORIES",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black)
                )
            }
        }
    }
}

@Composable
private fun rememberFormattedBytes(bytes: Long): String {
    return androidx.compose.runtime.remember(bytes) {
        val mb = bytes / (1024f * 1024f)
        if (mb >= 1024) {
            String.format("%.2f GB", mb / 1024f)
        } else {
            String.format("%.1f MB", mb)
        }
    }
}
