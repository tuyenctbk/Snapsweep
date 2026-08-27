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

import com.example.ui.components.BulkCleanDialog
import com.example.ui.components.ShareAppCard
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Surface

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
    showBulkCleanDialog: Boolean = false,
    cleanupStreakDays: Int = 3,
    onRunBackgroundWorker: () -> Unit = {},
    onRequestPermission: () -> Unit = {},
    onRefreshScan: () -> Unit,
    onOpenCategory: (MediaCategory) -> Unit,
    onOpenTrashSheet: () -> Unit,
    onClearNotification: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenMonthlyReport: () -> Unit = {},
    onOpenBulkCleanDialog: () -> Unit = {},
    onConfirmBulkClean: () -> Unit = {},
    onDismissBulkCleanDialog: () -> Unit = {},
    onOpenScanResults: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userNotification) {
        userNotification?.let {
            snackbarHostState.showSnackbar(it)
            onClearNotification()
        }
    }

    val screenshotsSummary = categories.firstOrNull { it.category == MediaCategory.OLD_SCREENSHOTS }
    val blurrySummary = categories.firstOrNull { it.category == MediaCategory.BLURRY_PHOTOS }

    val bulkCleanItemCount = (screenshotsSummary?.count ?: 0) + (blurrySummary?.count ?: 0)
    val bulkCleanBytes = (screenshotsSummary?.totalSizeBytes ?: 0L) + (blurrySummary?.totalSizeBytes ?: 0L)

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

                        ScanResultsSummaryCard(
                            onViewResults = onOpenScanResults,
                            screenshotsCount = screenshotsSummary?.count ?: 0,
                            duplicatesCount = (categories.firstOrNull { it.category == MediaCategory.SIMILAR_BURSTS }?.count ?: 0)
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

                    // Cleanup Streak Tracker Card
                    item {
                        CleanupStreakCard(streakDays = cleanupStreakDays)
                    }

                    // Monthly Report Card
                    item {
                        MonthlyReportCard(onOpenReport = onOpenMonthlyReport)
                    }

                    // Scan Results Card
                    item {
                        ScanResultsSummaryCard(
                            onViewResults = onOpenScanResults,
                            screenshotsCount = screenshotsSummary?.count ?: 0,
                            duplicatesCount = (categories.firstOrNull { it.category == MediaCategory.SIMILAR_BURSTS }?.count ?: 0)
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

                    // Bulk Clean Quick Action Card if items exist
                    if (bulkCleanItemCount > 0) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFF881337),
                                                Color(0xFF4C0519),
                                                Color(0xFF0F172A)
                                            )
                                        )
                                    )
                                    .border(1.dp, RoseTrash.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                    .clickable { onOpenBulkCleanDialog() }
                                    .padding(18.dp)
                                    .testTag("bulk_clean_quick_action")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(RoseTrash.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteSweep,
                                                contentDescription = "Bulk Clean",
                                                tint = RoseTrash,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "ONE-TAP BULK CLEAN",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.sp
                                                ),
                                                color = RoseTrash
                                            )
                                            Text(
                                                text = "Bulk Delete $bulkCleanItemCount Junk Photos",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = Color.White
                                            )
                                            Text(
                                                text = "Screenshots & blurry photos detected",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.LightGray
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = onOpenBulkCleanDialog,
                                        colors = ButtonDefaults.buttonColors(containerColor = RoseTrash),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "CLEAN NOW",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
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

        // Render Bulk Clean Confirmation Dialog if triggered
        if (showBulkCleanDialog) {
            BulkCleanDialog(
                screenshotsCount = screenshotsSummary?.count ?: 0,
                screenshotsSize = screenshotsSummary?.totalSizeBytes ?: 0L,
                blurryCount = blurrySummary?.count ?: 0,
                blurrySize = blurrySummary?.totalSizeBytes ?: 0L,
                onConfirmBulkClean = onConfirmBulkClean,
                onDismiss = onDismissBulkCleanDialog
            )
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
fun CleanupStreakCard(
    streakDays: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                colors = listOf(Color(0xFFF97316), CyanPrimary)
            )
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("cleanup_streak_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF97316).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Streak Fire",
                        tint = Color(0xFFF97316),
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "$streakDays DAY CLEANUP STREAK",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        ),
                        color = Color(0xFFF97316)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (streakDays > 1) "Awesome! Keep swiping daily to maintain gallery health." else "Clean today to start your daily streak!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val activeDots = streakDays.coerceAtMost(5)
                for (i in 1..5) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (i <= activeDots) Color(0xFFF97316) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun MonthlyReportCard(
    onOpenReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                colors = listOf(CyanPrimary, Color(0xFF3B82F6))
            )
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenReport() }
            .testTag("monthly_report_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CyanPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Monthly Report",
                        tint = CyanPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "MONTHLY CLEANUP REPORT",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "View storage saved & share your progress card",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onOpenReport,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanPrimary,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = "VIEW",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun ScanResultsSummaryCard(
    onViewResults: () -> Unit,
    screenshotsCount: Int,
    duplicatesCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                colors = listOf(CyanPrimary, Color(0xFF8B5CF6))
            )
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onViewResults() }
            .testTag("scan_results_summary_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF8B5CF6).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Scan Summary",
                        tint = Color(0xFF8B5CF6),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "DETAILED SCAN RESULTS",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Grouped: $screenshotsCount Screenshots • $duplicatesCount Duplicates",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onViewResults,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8B5CF6),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(36.dp)
                    .testTag("view_detailed_scan_results_button")
            ) {
                Text(
                    text = "ANALYZE",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
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
