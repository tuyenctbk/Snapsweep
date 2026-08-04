package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.MediaCategory
import com.example.data.model.MediaItem
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldKeep
import com.example.ui.theme.RoseTrash
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SwipeCardStack(
    items: List<MediaItem>,
    currentIndex: Int,
    onSwipeLeft: (MediaItem) -> Unit,
    onSwipeRight: (MediaItem) -> Unit,
    onUndo: () -> Unit,
    onZoom: (MediaItem) -> Unit,
    canUndo: Boolean,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    if (currentIndex >= items.size) {
        // Queue Completed State
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(EmeraldKeep.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Complete",
                        tint = EmeraldKeep,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Category Queue Swept!",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "All photos in this category have been reviewed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                if (canUndo) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceVariant)
                            .clickable { onUndo() }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = CyanPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Undo Last Swipe",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = CyanPrimary
                        )
                    }
                }
            }
        }
        return
    }

    val currentItem = items[currentIndex]
    val nextItem = items.getOrNull(currentIndex + 1)

    val offsetX = remember(currentIndex) { Animatable(0f) }
    val offsetY = remember(currentIndex) { Animatable(0f) }

    val rotation = (offsetX.value / 20f).coerceIn(-25f, 25f)
    val swipeThreshold = 300f

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("swipe_card_stack"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Stack Box
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background Next Card
            if (nextItem != null) {
                MediaCardView(
                    item = nextItem,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(DarkSurfaceVariant)
                )
            }

            // Foreground Active Card
            MediaCardView(
                item = currentItem,
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                    .rotate(rotation)
                    .pointerInput(currentIndex) {
                        detectDragGestures(
                            onDragEnd = {
                                if (offsetX.value > swipeThreshold) {
                                    // Swipe Right (Keep)
                                    coroutineScope.launch {
                                        offsetX.animateTo(1000f, tween(200))
                                        onSwipeRight(currentItem)
                                    }
                                } else if (offsetX.value < -swipeThreshold) {
                                    // Swipe Left (Trash)
                                    coroutineScope.launch {
                                        offsetX.animateTo(-1000f, tween(200))
                                        onSwipeLeft(currentItem)
                                    }
                                } else {
                                    // Reset to center
                                    coroutineScope.launch {
                                        offsetX.animateTo(0f, spring())
                                        offsetY.animateTo(0f, spring())
                                    }
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                coroutineScope.launch {
                                    offsetX.snapTo(offsetX.value + dragAmount.x)
                                    offsetY.snapTo(offsetY.value + dragAmount.y)
                                }
                            }
                        )
                    }
            )

            // Swipe Overlay Indicators
            if (offsetX.value > 80f) {
                // Keep Indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(28.dp)
                        .rotate(-15f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(EmeraldKeep)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "KEEP 💚",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    )
                }
            } else if (offsetX.value < -80f) {
                // Trash Indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(28.dp)
                        .rotate(15f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(RoseTrash)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "TRASH 🗑️",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Undo Button
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(if (canUndo) DarkSurfaceVariant else DarkSurface)
                    .border(1.dp, if (canUndo) CyanPrimary else DarkBorder, CircleShape)
                    .clickable(enabled = canUndo) { onUndo() }
                    .testTag("undo_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.RotateLeft,
                    contentDescription = "Undo",
                    tint = if (canUndo) CyanPrimary else TextMuted,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Trash Button (Swipe Left)
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(RoseTrash)
                    .clickable {
                        coroutineScope.launch {
                            offsetX.animateTo(-1000f, tween(200))
                            onSwipeLeft(currentItem)
                        }
                    }
                    .testTag("trash_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Swipe Trash",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }

            // Fullscreen Zoom Button
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkBorder, CircleShape)
                    .clickable { onZoom(currentItem) }
                    .testTag("zoom_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Fullscreen,
                    contentDescription = "Zoom",
                    tint = TextPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Keep Button (Swipe Right)
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(EmeraldKeep)
                    .clickable {
                        coroutineScope.launch {
                            offsetX.animateTo(1000f, tween(200))
                            onSwipeRight(currentItem)
                        }
                    }
                    .testTag("keep_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Swipe Keep",
                    tint = Color.Black,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

@Composable
fun MediaCardView(
    item: MediaItem,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dateStr = remember(item.dateTakenMillis) {
        val sdf = SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault())
        sdf.format(Date(item.dateTakenMillis))
    }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Photo Image
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.uri)
                    .crossfade(true)
                    .build(),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Top Category Tag Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(item.category.badgeColorHex))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = item.category.title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = item.formattedSize,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = CyanPrimary
                            )
                        )
                    }
                }
            }

            // Bottom Details Overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )

                    if (item.blurScore > 0f) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = RoseTrash,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Blur Metric: ${String.format("%.1f", item.blurScore)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = RoseTrash
                            )
                        }
                    }
                }
            }
        }
    }
}
