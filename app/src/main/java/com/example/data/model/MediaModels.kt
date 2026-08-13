package com.example.data.model

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhonelinkSetup
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.graphics.vector.ImageVector

enum class MediaCategory(
    val titleResId: Int,
    val descriptionResId: Int,
    val icon: ImageVector,
    val badgeColorHex: Long
) {
    OLD_SCREENSHOTS(
        titleResId = com.example.R.string.category_old_screenshots_title,
        descriptionResId = com.example.R.string.category_old_screenshots_desc,
        icon = Icons.Default.PhonelinkSetup,
        badgeColorHex = 0xFF0284C7 // Cyan
    ),
    BLURRY_PHOTOS(
        titleResId = com.example.R.string.category_blurry_photos_title,
        descriptionResId = com.example.R.string.category_blurry_photos_desc,
        icon = Icons.Default.BlurOn,
        badgeColorHex = 0xFFF59E0B // Amber
    ),
    SIMILAR_BURSTS(
        titleResId = com.example.R.string.category_similar_bursts_title,
        descriptionResId = com.example.R.string.category_similar_bursts_desc,
        icon = Icons.Default.ContentCopy,
        badgeColorHex = 0xFF8B5CF6 // Purple
    ),
    RECEIPTS_DOCS(
        titleResId = com.example.R.string.category_receipts_docs_title,
        descriptionResId = com.example.R.string.category_receipts_docs_desc,
        icon = Icons.Default.Description,
        badgeColorHex = 0xFF10B981 // Emerald
    ),
    TRAVEL(
        titleResId = com.example.R.string.category_travel_title,
        descriptionResId = com.example.R.string.category_travel_desc,
        icon = Icons.Default.FlightTakeoff,
        badgeColorHex = 0xFF3B82F6 // Blue
    ),
    FOOD(
        titleResId = com.example.R.string.category_food_title,
        descriptionResId = com.example.R.string.category_food_desc,
        icon = Icons.Default.Restaurant,
        badgeColorHex = 0xFFF97316 // Orange
    ),
    PETS(
        titleResId = com.example.R.string.category_pets_title,
        descriptionResId = com.example.R.string.category_pets_desc,
        icon = Icons.Default.Pets,
        badgeColorHex = 0xFFA855F7 // Purple
    ),
    HEAVY_MEDIA(
        titleResId = com.example.R.string.category_heavy_media_title,
        descriptionResId = com.example.R.string.category_heavy_media_desc,
        icon = Icons.Default.Videocam,
        badgeColorHex = 0xFFEF4444 // Rose
    ),
    ON_THIS_DAY(
        titleResId = com.example.R.string.category_on_this_day_title,
        descriptionResId = com.example.R.string.category_on_this_day_desc,
        icon = Icons.Default.CalendarToday,
        badgeColorHex = 0xFFEC4899 // Pink
    )
}

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val title: String,
    val sizeBytes: Long,
    val dateTakenMillis: Long,
    val category: MediaCategory,
    val width: Int = 0,
    val height: Int = 0,
    val blurScore: Float = 0f,
    val hash: String = "",
    val isVideo: Boolean = false,
    val isPendingTrash: Boolean = false,
    val isKept: Boolean = false
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

data class CategorySummary(
    val category: MediaCategory,
    val count: Int,
    val totalSizeBytes: Long,
    val items: List<MediaItem>
) {
    val formattedTotalSize: String
        get() {
            val mb = totalSizeBytes / (1024f * 1024f)
            return if (mb >= 1024) {
                String.format("%.2f GB", mb / 1024f)
            } else {
                String.format("%.1f MB", mb)
            }
        }
}

data class StorageStats(
    val totalBytesFreed: Long = 0L,
    val totalItemsCleaned: Int = 0,
    val lastCleanupMillis: Long = 0L
) {
    val formattedFreedStorage: String
        get() {
            val gb = totalBytesFreed / (1024f * 1024f * 1024f)
            return if (gb >= 1.0f) {
                String.format("%.2f GB", gb)
            } else {
                val mb = totalBytesFreed / (1024f * 1024f)
                String.format("%.1f MB", mb)
            }
        }
}
