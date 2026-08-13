package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategorySummary
import com.example.data.model.MediaCategory
import com.example.data.model.MediaItem
import com.example.ui.components.SwipeCardStack
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.RoseTrash
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

import androidx.compose.ui.res.stringResource
import com.example.R

enum class SmartSortOption(val labelResId: Int) {
    HIGHEST_CLUTTER(R.string.highest_clutter),
    LARGEST_FIRST(R.string.largest_first),
    NEWEST_FIRST(R.string.newest_first),
    OLDEST_FIRST(R.string.oldest_first)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeCleanerScreen(
    category: MediaCategory,
    allCategories: List<CategorySummary> = emptyList(),
    activeSortOption: SmartSortOption = SmartSortOption.HIGHEST_CLUTTER,
    queue: List<MediaItem>,
    currentIndex: Int,
    pendingTrashCount: Int,
    canUndo: Boolean,
    onBack: () -> Unit,
    onSelectCategory: (MediaCategory) -> Unit = {},
    onSelectSortOption: (SmartSortOption) -> Unit = {},
    onSwipeLeft: (MediaItem) -> Unit,
    onSwipeRight: (MediaItem) -> Unit,
    onUndo: () -> Unit,
    onZoom: (MediaItem) -> Unit,
    onOpenTrashSheet: () -> Unit,
    modifier: Modifier = Modifier,
    onScanAgain: (() -> Unit)? = null
) {
    val totalCount = queue.size
    val currentStep = (currentIndex + 1).coerceAtMost(totalCount)
    val progress = if (totalCount > 0) currentStep.toFloat() / totalCount else 1.0f

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("swipe_cleaner_screen"),
        containerColor = DarkBackground,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(category.badgeColorHex).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = null,
                                    tint = Color(category.badgeColorHex),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = stringResource(category.titleResId),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                if (totalCount > 0 && currentIndex < totalCount) {
                                    Text(
                                        text = stringResource(R.string.item_of_count, currentStep, totalCount),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                    },
                    actions = {
                        if (pendingTrashCount > 0) {
                            IconButton(onClick = onOpenTrashSheet) {
                                Box {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = "Pending Trash",
                                        tint = RoseTrash
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(RoseTrash),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$pendingTrashCount",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
                )

                // 1. Horizontal Filter Chips for Quick Category Jumping
                if (allCategories.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        items(allCategories, key = { it.category.name }) { summary ->
                            val isSelected = summary.category == category
                            val categoryColor = Color(summary.category.badgeColorHex)

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) categoryColor.copy(alpha = 0.25f) else DarkSurface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) categoryColor else DarkSurfaceVariant
                                ),
                                modifier = Modifier
                                    .clickable { onSelectCategory(summary.category) }
                                    .testTag("filter_chip_${summary.category.name}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = summary.category.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) categoryColor else TextMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${stringResource(summary.category.titleResId)} (${summary.count})",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                        ),
                                        color = if (isSelected) Color.White else TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Horizontal Smart Sort Option Chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 4.dp, top = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.sort_label),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = CyanPrimary
                            )
                        }
                    }

                    items(SmartSortOption.values()) { option ->
                        val isSelected = option == activeSortOption

                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) CyanPrimary.copy(alpha = 0.2f) else DarkSurface.copy(alpha = 0.6f),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) CyanPrimary else DarkSurfaceVariant
                            ),
                            modifier = Modifier.clickable { onSelectSortOption(option) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (option) {
                                        SmartSortOption.HIGHEST_CLUTTER -> Icons.Default.AutoAwesome
                                        SmartSortOption.LARGEST_FIRST -> Icons.Default.VerticalAlignTop
                                        SmartSortOption.NEWEST_FIRST -> Icons.Default.ArrowUpward
                                        SmartSortOption.OLDEST_FIRST -> Icons.Default.ArrowDownward
                                    },
                                    contentDescription = null,
                                    tint = if (isSelected) CyanPrimary else TextMuted,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(option.labelResId),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) CyanPrimary else TextMuted
                                )
                            }
                        }
                    }
                }

                if (totalCount > 0) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = Color(category.badgeColorHex),
                        trackColor = DarkSurfaceVariant
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SwipeCardStack(
                items = queue,
                currentIndex = currentIndex,
                onSwipeLeft = onSwipeLeft,
                onSwipeRight = onSwipeRight,
                onUndo = onUndo,
                onZoom = onZoom,
                canUndo = canUndo,
                onScanAgain = onScanAgain
            )
        }
    }
}
