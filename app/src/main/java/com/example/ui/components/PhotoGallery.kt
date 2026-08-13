package com.example.ui.components

import androidx.compose.ui.res.stringResource
import com.example.R

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
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

enum class GalleryGroupMode {
    DATE_TAKEN, APP_ORIGIN, ALL
}

/**
 * A responsive Photo Gallery composable that queries local images using [ContentResolver]
 * and displays them in a LazyVerticalGrid layout with full-screen preview capability and grouping modes.
 */
@Composable
fun PhotoGallery(
    modifier: Modifier = Modifier,
    columnsCount: Int = 3,
    onPhotoClick: ((LocalPhotoItem) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var photos by remember { mutableStateOf<List<LocalPhotoItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedPhoto by remember { mutableStateOf<LocalPhotoItem?>(null) }
    var groupMode by remember { mutableStateOf(GalleryGroupMode.DATE_TAKEN) }

    // Multi-selection mode states
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedPhotos = remember { mutableStateListOf<LocalPhotoItem>() }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val deleteRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            selectedPhotos.clear()
            isSelectionMode = false
            isLoading = true
            coroutineScope.launch {
                photos = fetchLocalPhotos(context)
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        photos = fetchLocalPhotos(context)
        isLoading = false
    }

    // Compute grouped photos
    val groupedPhotos = remember(photos, groupMode) {
        when (groupMode) {
            GalleryGroupMode.DATE_TAKEN -> {
                photos.groupBy { photo ->
                    val now = System.currentTimeMillis()
                    val diffDays = (now - photo.dateTakenMillis) / (1000 * 60 * 60 * 24)
                    when {
                        diffDays <= 0 -> "📅 Today"
                        diffDays <= 1 -> "📅 Yesterday"
                        diffDays <= 7 -> "🗓️ This Week"
                        diffDays <= 30 -> "🗓️ This Month"
                        else -> "🗓️ Older Memories"
                    }
                }
            }
            GalleryGroupMode.APP_ORIGIN -> {
                photos.groupBy { photo ->
                    when {
                        photo.displayName.contains("screenshot", ignoreCase = true) -> "📱 Screenshots"
                        photo.displayName.contains("WA", ignoreCase = true) || photo.displayName.contains("WhatsApp", ignoreCase = true) -> "💬 WhatsApp Media"
                        photo.displayName.contains("IMG", ignoreCase = true) || photo.displayName.contains("PXL", ignoreCase = true) -> "📸 Camera Shots"
                        else -> "📁 Downloads & Others"
                    }
                }
            }
            GalleryGroupMode.ALL -> mapOf("🖼️ All Photos (${photos.size})" to photos)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("photo_gallery_container")
    ) {
        // Title and Selection Mode Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isSelectionMode) stringResource(R.string.batch_delete_mode) else stringResource(R.string.local_gallery),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = if (isSelectionMode) CyanPrimary else TextMuted
            )

            Text(
                text = if (isSelectionMode) stringResource(R.string.cancel) else stringResource(R.string.select_multiple),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = CyanPrimary,
                modifier = Modifier
                    .clickable {
                        isSelectionMode = !isSelectionMode
                        if (!isSelectionMode) {
                            selectedPhotos.clear()
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("gallery_select_mode_toggle")
            )
        }

        // Group Mode Chips Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.group_by),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = TextMuted
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (groupMode == GalleryGroupMode.DATE_TAKEN) CyanPrimary else DarkSurface)
                    .clickable { groupMode = GalleryGroupMode.DATE_TAKEN }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Date Taken",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (groupMode == GalleryGroupMode.DATE_TAKEN) Color.Black else TextSecondary
                    )
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (groupMode == GalleryGroupMode.APP_ORIGIN) CyanPrimary else DarkSurface)
                    .clickable { groupMode = GalleryGroupMode.APP_ORIGIN }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "App Origin",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (groupMode == GalleryGroupMode.APP_ORIGIN) Color.Black else TextSecondary
                    )
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (groupMode == GalleryGroupMode.ALL) CyanPrimary else DarkSurface)
                    .clickable { groupMode = GalleryGroupMode.ALL }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "All",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (groupMode == GalleryGroupMode.ALL) Color.Black else TextSecondary
                    )
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
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
                        groupedPhotos.forEach { (headerTitle, sectionPhotos) ->
                            item(span = { GridItemSpan(columnsCount) }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp, bottom = 4.dp)
                                ) {
                                    Text(
                                        text = "$headerTitle (${sectionPhotos.size})",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = CyanPrimary
                                    )
                                }
                            }

                            items(
                                items = sectionPhotos,
                                key = { photo -> "${headerTitle}_${photo.id}" }
                             ) { photo ->
                                val isSelected = selectedPhotos.any { it.id == photo.id }
                                PhotoGridTile(
                                    photo = photo,
                                    isSelected = isSelected,
                                    isSelectionMode = isSelectionMode,
                                    onClick = {
                                        if (isSelectionMode) {
                                            if (isSelected) {
                                                selectedPhotos.removeAll { it.id == photo.id }
                                            } else {
                                                selectedPhotos.add(photo)
                                            }
                                        } else {
                                            if (onPhotoClick != null) {
                                                onPhotoClick(photo)
                                            } else {
                                                selectedPhoto = photo
                                            }
                                        }
                                    }
                                )
                            }
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

            // Floating multi-delete bar
            androidx.compose.animation.AnimatedVisibility(
                visible = isSelectionMode && selectedPhotos.isNotEmpty(),
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.photos_selected, selectedPhotos.size),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            val totalSize = selectedPhotos.sumOf { it.sizeBytes }
                            val mb = totalSize / (1024f * 1024f)
                            val formatted = if (mb >= 1024) String.format("%.2f GB", mb / 1024f) else String.format("%.1f MB", mb)
                            Text(
                                text = stringResource(R.string.to_free, formatted),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                        Button(
                            onClick = { showDeleteConfirmation = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.delete_selected), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }

            // Delete Confirmation Dialog
            if (showDeleteConfirmation) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmation = false },
                    title = { Text(stringResource(R.string.delete_selected_title)) },
                    text = { Text(stringResource(R.string.delete_selected_confirm, selectedPhotos.size)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDeleteConfirmation = false
                                val uris = selectedPhotos.map { it.uri }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    try {
                                        val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, uris)
                                        deleteRequestLauncher.launch(
                                            IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                                        )
                                    } catch (e: Exception) {
                                        coroutineScope.launch {
                                            deleteLocalPhotosDirectly(context, selectedPhotos)
                                            selectedPhotos.clear()
                                            isSelectionMode = false
                                            photos = fetchLocalPhotos(context)
                                        }
                                    }
                                } else {
                                    coroutineScope.launch {
                                        deleteLocalPhotosDirectly(context, selectedPhotos)
                                        selectedPhotos.clear()
                                        isSelectionMode = false
                                        photos = fetchLocalPhotos(context)
                                    }
                                }
                            }
                        ) {
                            Text(stringResource(R.string.delete_button), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmation = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }
        }
    }
}

suspend fun deleteLocalPhotosDirectly(context: Context, items: List<LocalPhotoItem>): Int {
    var count = 0
    withContext(Dispatchers.IO) {
        for (item in items) {
            try {
                val rows = context.contentResolver.delete(item.uri, null, null)
                if (rows > 0) count++
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    return count
}

@Composable
private fun PhotoGridTile(
    photo: LocalPhotoItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
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
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 2.dp else 0.5.dp,
            if (isSelected) CyanPrimary else DarkBorder
        )
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

            // Selection overlay and checkbox
            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isSelected) CyanPrimary.copy(alpha = 0.25f) else Color.Transparent)
                )

                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) CyanPrimary else Color.Black.copy(alpha = 0.5f))
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Selection State",
                        tint = if (isSelected) Color.Black else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

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
