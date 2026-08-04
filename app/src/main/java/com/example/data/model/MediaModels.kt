package com.example.data.model

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PhonelinkSetup
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.graphics.vector.ImageVector

enum class MediaCategory(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val badgeColorHex: Long
) {
    OLD_SCREENSHOTS(
        title = "Old Screenshots",
        description = "Captured over 30 days ago",
        icon = Icons.Default.PhonelinkSetup,
        badgeColorHex = 0xFF0284C7 // Cyan
    ),
    BLURRY_PHOTOS(
        title = "Blurry & Out of Focus",
        description = "Low sharpness detected",
        icon = Icons.Default.BlurOn,
        badgeColorHex = 0xFFF59E0B // Amber
    ),
    SIMILAR_BURSTS(
        title = "Similar & Bursts",
        description = "Near-identical shots taken seconds apart",
        icon = Icons.Default.ContentCopy,
        badgeColorHex = 0xFF8B5CF6 // Purple
    ),
    RECEIPTS_DOCS(
        title = "Receipts & Documents",
        description = "Temporary papers, whiteboards & bills",
        icon = Icons.Default.Description,
        badgeColorHex = 0xFF10B981 // Emerald
    ),
    HEAVY_MEDIA(
        title = "Heavy Media",
        description = "Large videos and recordings > 50MB",
        icon = Icons.Default.Videocam,
        badgeColorHex = 0xFFEF4444 // Rose
    ),
    ON_THIS_DAY(
        title = "On This Day",
        description = "Memories captured on this date in past years",
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
    val isKept: Boolean = false,
    val isSample: Boolean = false
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
