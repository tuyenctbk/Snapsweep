package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldKeep
import com.example.ui.theme.RoseTrash

@Composable
fun SwipeGestureGuideOverlay(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.82f))
                .padding(24.dp)
                .testTag("gesture_guide_overlay"),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        colors = listOf(CyanPrimary, MaterialTheme.colorScheme.outline)
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Badge
                    Surface(
                        shape = CircleShape,
                        color = CyanPrimary.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gesture,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SWIPE CLEANUP GUIDE",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = CyanPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "How to Review & Clean",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Reviewing photos is effortless. Watch the gesture animation below:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Gesture Animation Sandbox Stage
                    GestureAnimationStage()

                    Spacer(modifier = Modifier.height(28.dp))

                    // Action Button
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanPrimary,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("dismiss_gesture_guide_button")
                    ) {
                        Text(
                            text = "GOT IT, START SWIPING! 🚀",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GestureAnimationStage() {
    val infiniteTransition = rememberInfiniteTransition(label = "gesture_animation")
    
    // Animate X translation offset from -110dp (left) to +110dp (right)
    val swipeOffsetDp by infiniteTransition.animateFloat(
        initialValue = -110f,
        targetValue = 110f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "swipe_offset"
    )

    // Calculate rotation angle matching swipe offset
    val rotationAngle = (swipeOffsetDp / 110f) * 12f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Left Action Indicator (DISCARD / TRASH)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(12.dp))
                .background(RoseTrash.copy(alpha = if (swipeOffsetDp < -20f) 0.25f else 0.08f))
                .border(
                    1.dp,
                    RoseTrash.copy(alpha = if (swipeOffsetDp < -20f) 0.8f else 0.2f),
                    RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Discard",
                    tint = RoseTrash,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null,
                        tint = RoseTrash,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "DISCARD",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        color = RoseTrash
                    )
                }
            }
        }

        // Right Action Indicator (KEEP / SAVE)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clip(RoundedCornerShape(12.dp))
                .background(EmeraldKeep.copy(alpha = if (swipeOffsetDp > 20f) 0.25f else 0.08f))
                .border(
                    1.dp,
                    EmeraldKeep.copy(alpha = if (swipeOffsetDp > 20f) 0.8f else 0.2f),
                    RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Keep",
                    tint = EmeraldKeep,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "KEEP",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        color = EmeraldKeep
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = EmeraldKeep,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        // Animated Card in center
        Box(
            modifier = Modifier
                .offset { IntOffset(swipeOffsetDp.dp.roundToPx(), 0) }
                .rotate(rotationAngle)
                .size(width = 110.dp, height = 140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            when {
                                swipeOffsetDp < -30f -> RoseTrash.copy(alpha = 0.8f)
                                swipeOffsetDp > 30f -> EmeraldKeep.copy(alpha = 0.8f)
                                else -> CyanPrimary.copy(alpha = 0.6f)
                            },
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .border(
                    2.dp,
                    when {
                        swipeOffsetDp < -30f -> RoseTrash
                        swipeOffsetDp > 30f -> EmeraldKeep
                        else -> CyanPrimary
                    },
                    RoundedCornerShape(16.dp)
                )
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Swipe,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when {
                        swipeOffsetDp < -30f -> "TRASH"
                        swipeOffsetDp > 30f -> "KEEP"
                        else -> "SWIPE ME"
                    },
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                    color = Color.White
                )
            }
        }

        // Animated Finger Pointer Icon
        Box(
            modifier = Modifier
                .offset { IntOffset((swipeOffsetDp + 15).dp.roundToPx(), 45.dp.roundToPx()) }
                .size(38.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.9f))
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.TouchApp,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
