package com.example.ui.components

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LocalPhotoItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val dateTakenMillis: Long,
    val width: Int = 0,
    val height: Int = 0
) {
    val formattedSize: String
        get() {
            val mb = sizeBytes / (1024f * 1024f)
            return if (mb >= 1024) {
                String.format("%.2f GB", mb / 1024f)
            } else if (mb >= 1) {
                String.format("%.1f MB", mb)
            } else {
                val kb = sizeBytes / 1024f
                String.format("%.0f KB", kb)
            }
        }
}

/**
 * A responsive Photo Gallery composable that queries local images using [ContentResolver]
 * and displays them in a LazyVerticalGrid layout with full-screen preview capability.
 */
@Composable
fun PhotoGallery(
    modifier: Modifier = Modifier,
    columnsCount: Int = 3,
    onPhotoClick: ((LocalPhotoItem) -> Unit)? = null
) {
    val context = LocalContext.current
    var photos by remember { mutableStateOf<List<LocalPhotoItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedPhoto by remember { mutableStateOf<LocalPhotoItem?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        photos = fetchLocalPhotos(context)
        isLoading = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("photo_gallery_container")
    ) {
        when {
            isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = CyanPrimary,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("photo_gallery_loading")
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading photos from gallery...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
            photos.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(DarkSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Collections,
                            contentDescription = "Empty Gallery",
                            tint = TextMuted,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No photos found in gallery",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Make sure gallery access permission is granted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnsCount),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("photo_gallery_grid"),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = photos,
                        key = { it.id }
                    ) { photo ->
                        PhotoGridTile(
                            photo = photo,
                            onClick = {
                                if (onPhotoClick != null) {
                                    onPhotoClick(photo)
                                } else {
                                    selectedPhoto = photo
                                }
                            }
                        )
                    }
                }
            }
        }

        // Fullscreen Lightbox Modal when a photo is clicked
        selectedPhoto?.let { photo ->
            PhotoLightboxDialog(
                photo = photo,
                onDismiss = { selectedPhoto = null }
            )
        }
    }
}

@Composable
private fun PhotoGridTile(
    photo: LocalPhotoItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("photo_gallery_item_${photo.id}"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkBorder)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(photo.uri)
                    .crossfade(true)
                    .size(256)
                    .build(),
                contentDescription = photo.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Bottom Gradient & Size Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = photo.formattedSize,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PhotoLightboxDialog(
    photo: LocalPhotoItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("photo_gallery_lightbox")
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(photo.uri)
                    .crossfade(true)
                    .build(),
                contentDescription = photo.displayName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            // Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.65f)
                ) {
                    Text(
                        text = photo.displayName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close preview",
                        tint = Color.White
                    )
                }
            }

            // Bottom Info Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                color = Color.Black.copy(alpha = 0.8f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Size: ${photo.formattedSize}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = TextPrimary
                        )
                        if (photo.width > 0 && photo.height > 0) {
                            Text(
                                text = "Dimensions: ${photo.width} x ${photo.height} px",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Helper function that queries [ContentResolver] for images stored in [MediaStore.Images.Media.EXTERNAL_CONTENT_URI].
 */
suspend fun fetchLocalPhotos(context: Context): List<LocalPhotoItem> = withContext(Dispatchers.IO) {
    val photos = mutableListOf<LocalPhotoItem>()
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.SIZE,
        MediaStore.Images.Media.DATE_TAKEN,
        MediaStore.Images.Media.WIDTH,
        MediaStore.Images.Media.HEIGHT
    )
    val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

    try {
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val widthColumn = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
            val heightColumn = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: "Photo_$id"
                val size = cursor.getLong(sizeColumn)
                val date = cursor.getLong(dateColumn)
                val width = if (widthColumn >= 0) cursor.getInt(widthColumn) else 0
                val height = if (heightColumn >= 0) cursor.getInt(heightColumn) else 0
                val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                photos.add(
                    LocalPhotoItem(
                        id = id,
                        uri = contentUri,
                        displayName = name,
                        sizeBytes = size,
                        dateTakenMillis = date,
                        width = width,
                        height = height
                    )
                )
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return@withContext photos
}
