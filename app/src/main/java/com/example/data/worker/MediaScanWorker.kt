package com.example.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.data.local.AppDatabase
import com.example.data.local.TaggedMediaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class MediaScanWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "MediaScanWorker"
        const val CHANNEL_ID = "media_scan_channel"
        const val NOTIFICATION_ID = 1001

        const val KEY_DUPLICATES_COUNT = "duplicates_count"
        const val KEY_SCREENSHOTS_COUNT = "screenshots_count"
        const val KEY_TOTAL_TAGGED = "total_tagged"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Check Power Saving Mode (< 20% battery and not charging)
            if (isLowBattery(appContext)) {
                showPowerSavingNotification()
                return@withContext Result.success(
                    workDataOf("power_saving_paused" to true)
                )
            }

            val cleanupDao = AppDatabase.getInstance(appContext).cleanupDao()
            val keptIds = cleanupDao.getAllKeptMediaIds().toSet()

            val mediaItems = queryAllImages(appContext, keptIds)
            val taggedList = mutableListOf<TaggedMediaEntity>()

            // 1. File Hashing for Duplicate Detection
            val hashMap = mutableMapOf<String, MutableList<ScannedImageInfo>>()
            for (item in mediaItems) {
                val hash = computeFileHash(appContext, item.uri, item.size)
                if (hash.isNotEmpty()) {
                    hashMap.getOrPut(hash) { mutableListOf() }.add(item)
                }
            }

            var duplicateCount = 0
            for ((hash, itemsWithSameHash) in hashMap) {
                if (itemsWithSameHash.size > 1) {
                    // Tag items after the first one as duplicates for potential deletion
                    itemsWithSameHash.drop(1).forEach { dupItem ->
                        duplicateCount++
                        taggedList.add(
                            TaggedMediaEntity(
                                mediaId = dupItem.id,
                                uriString = dupItem.uri.toString(),
                                tagType = "DUPLICATE",
                                fileHash = hash,
                                tagReason = "Exact duplicate file hash (MD5: ${hash.take(8)}...)",
                                taggedAtMillis = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }

            // 2. Screenshot Detection by Aspect Ratio & Metadata
            val displayMetrics = appContext.resources.displayMetrics
            val screenW = displayMetrics.widthPixels.toFloat()
            val screenH = displayMetrics.heightPixels.toFloat()
            val deviceScreenRatio = if (screenW > 0 && screenH > 0) {
                max(screenW, screenH) / min(screenW, screenH)
            } else {
                20f / 9f
            }

            // Standard mobile screenshot aspect ratios: 16:9, 18:9, 19.5:9, 20:9, 21:9
            val targetAspectRatios = listOf(16f / 9f, 18f / 9f, 19.5f / 9f, 20f / 9f, 21f / 9f, deviceScreenRatio)

            var screenshotCount = 0
            for (item in mediaItems) {
                if (item.width > 0 && item.height > 0) {
                    val w = item.width.toFloat()
                    val h = item.height.toFloat()
                    val itemRatio = max(w, h) / min(w, h)

                    val isMatchingScreenRatio = targetAspectRatios.any { targetRatio ->
                        abs(itemRatio - targetRatio) < 0.04f
                    }

                    val isPngOrScreenshotName = item.mimeType.contains("png", ignoreCase = true) ||
                            item.displayName.contains("screenshot", ignoreCase = true) ||
                            item.displayName.contains("screen", ignoreCase = true)

                    if (isMatchingScreenRatio && isPngOrScreenshotName) {
                        // Check if not already tagged as duplicate
                        if (taggedList.none { it.mediaId == item.id }) {
                            screenshotCount++
                            taggedList.add(
                                TaggedMediaEntity(
                                    mediaId = item.id,
                                    uriString = item.uri.toString(),
                                    tagType = "SCREENSHOT",
                                    fileHash = computeFileHash(appContext, item.uri, item.size),
                                    tagReason = "Aspect ratio (${item.width}x${item.height}, ${String.format("%.2f", itemRatio)}:1) matching screen metrics",
                                    taggedAtMillis = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
            }

            // Store tagged entities in Room database
            if (taggedList.isNotEmpty()) {
                cleanupDao.insertTaggedMedia(taggedList)
            }

            // Fire completion notification
            showNotification(duplicateCount, screenshotCount, taggedList.size)

            Result.success(
                workDataOf(
                    KEY_DUPLICATES_COUNT to duplicateCount,
                    KEY_SCREENSHOTS_COUNT to screenshotCount,
                    KEY_TOTAL_TAGGED to taggedList.size
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun queryAllImages(context: Context, keptIds: Set<Long>): List<ScannedImageInfo> {
        val list = mutableListOf<ScannedImageInfo>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )

        try {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_TAKEN} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
                val mimeCol = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
                val widthCol = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)

                while (cursor.moveToNext()) {
                    val id = if (idCol >= 0) cursor.getLong(idCol) else continue
                    if (keptIds.contains(id)) continue

                    val name = if (nameCol >= 0) cursor.getString(nameCol) ?: "Image" else "Image"
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "image/jpeg" else "image/jpeg"
                    val width = if (widthCol >= 0) cursor.getInt(widthCol) else 0
                    val height = if (heightCol >= 0) cursor.getInt(heightCol) else 0
                    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                    list.add(
                        ScannedImageInfo(
                            id = id,
                            uri = uri,
                            displayName = name,
                            size = size,
                            mimeType = mime,
                            width = width,
                            height = height
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun computeFileHash(context: Context, uri: Uri, size: Long): String {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)

            if (inputStream != null) {
                inputStream.use { stream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L
                    // Read up to first 2MB for fast background hashing
                    val maxBytesToHash = 2L * 1024 * 1024

                    while (stream.read(buffer).also { bytesRead = it } != -1 && totalRead < maxBytesToHash) {
                        digest.update(buffer, 0, bytesRead)
                        totalRead += bytesRead
                    }
                }
                // Convert to hex string
                digest.digest().joinToString("") { "%02x".format(it) }
            } else {
                // Fallback for file scheme URIs
                val file = uri.path?.let { File(it) }
                if (file != null && file.exists()) {
                    file.inputStream().use { stream ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (stream.read(buffer).also { bytesRead = it } != -1) {
                            digest.update(buffer, 0, bytesRead)
                        }
                    }
                    digest.digest().joinToString("") { "%02x".format(it) }
                } else {
                    "${uri.lastPathSegment}_$size"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "${uri.lastPathSegment}_$size"
        }
    }

    private fun showNotification(duplicates: Int, screenshots: Int, total: Int) {
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Media Scan Service",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for background gallery duplicate & screenshot analysis"
            }
            manager.createNotificationChannel(channel)
        }

        val contentText = if (total > 0) {
            "Found $duplicates duplicates and $screenshots screenshots ready for review"
        } else {
            "Gallery background scan complete. No duplicates found."
        }

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("SnapSweep Background Scan")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        try {
            manager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun isLowBattery(context: Context): Boolean {
        val batteryStatus: android.content.Intent? = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        val level: Int = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
        if (level < 0 || scale <= 0) return false
        val batteryPct: Float = level * 100 / scale.toFloat()

        val status: Int = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging: Boolean = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                status == android.os.BatteryManager.BATTERY_STATUS_FULL

        return batteryPct < 20f && !isCharging
    }

    private fun showPowerSavingNotification() {
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Media Scan Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("SnapSweep Power Saving")
            .setContentText("Background scan paused to conserve power (battery < 20%)")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        try {
            manager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}

private data class ScannedImageInfo(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val size: Long,
    val mimeType: String,
    val width: Int,
    val height: Int
)
