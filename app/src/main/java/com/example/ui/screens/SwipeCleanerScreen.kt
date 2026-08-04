package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeCleanerScreen(
    category: MediaCategory,
    queue: List<MediaItem>,
    currentIndex: Int,
    pendingTrashCount: Int,
    canUndo: Boolean,
    onBack: () -> Unit,
    onSwipeLeft: (MediaItem) -> Unit,
    onSwipeRight: (MediaItem) -> Unit,
    onUndo: () -> Unit,
    onZoom: (MediaItem) -> Unit,
    onOpenTrashSheet: () -> Unit,
    modifier: Modifier = Modifier
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
                                    text = category.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                if (totalCount > 0 && currentIndex < totalCount) {
                                    Text(
                                        text = "Item $currentStep of $totalCount",
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
                canUndo = canUndo
            )
        }
    }
}
