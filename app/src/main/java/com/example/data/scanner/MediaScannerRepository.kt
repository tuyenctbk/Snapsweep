package com.example.data.scanner

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.data.analyzer.ImageAnalyzer
import com.example.data.local.CleanupDao
import com.example.data.model.CategorySummary
import com.example.data.model.MediaCategory
import com.example.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

class MediaScannerRepository(
    private val context: Context,
    private val cleanupDao: CleanupDao
) {

    suspend fun scanMedia(onProgress: (Float) -> Unit): List<CategorySummary> = withContext(Dispatchers.IO) {
        val keptIds = cleanupDao.getAllKeptMediaIds().toSet()

        val rawItems = mutableListOf<MediaItem>()
        
        onProgress(0.1f)
        val realItems = queryMediaStore(keptIds)
        onProgress(0.5f)

        if (realItems.isNotEmpty()) {
            rawItems.addAll(realItems)
        } else {
            // Provide realistic offline sample items if MediaStore is empty (e.g. clean emulator)
            rawItems.addAll(generateSampleItems(context, keptIds))
        }

        onProgress(0.8f)
        
        // Group items into categories
        val categoryMap = MediaCategory.values().associateWith { mutableListOf<MediaItem>() }

        for (item in rawItems) {
            categoryMap[item.category]?.add(item)
        }

        val summaries = categoryMap.map { (cat, items) ->
            CategorySummary(
                category = cat,
                count = items.size,
                totalSizeBytes = items.sumOf { it.sizeBytes },
                items = items.sortedByDescending { it.dateTakenMillis }
            )
        }

        onProgress(1.0f)
        summaries
    }

    private fun queryMediaStore(keptIds: Set<Long>): List<MediaItem> {
        val mediaList = mutableListOf<MediaItem>()

        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT
        )

        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        // Query Images
        val imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        context.contentResolver.query(
            imageUri,
            projection,
            null,
            null,
            "${MediaStore.MediaColumns.DATE_TAKEN} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)

            val previousHashes = mutableListOf<Pair<String, Long>>() // hash to timestamp

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                if (keptIds.contains(id)) continue

                val name = cursor.getString(nameColumn) ?: "Photo"
                val size = cursor.getLong(sizeColumn)
                var dateTaken = cursor.getLong(dateColumn)
                if (dateTaken == 0L) dateTaken = System.currentTimeMillis()
                val mime = cursor.getString(mimeColumn) ?: "image/jpeg"
                val width = cursor.getInt(widthColumn)
                val height = cursor.getInt(heightColumn)

                val contentUri = ContentUris.withAppendedId(imageUri, id)

                // Categorization logic
                val isScreenshot = name.contains("screenshot", ignoreCase = true) ||
                        name.contains("ScreenShot", ignoreCase = true) ||
                        name.startsWith("Screenshot_") ||
                        (mime.contains("png", ignoreCase = true) && dateTaken < thirtyDaysAgo)

                val itemCalendar = Calendar.getInstance().apply { timeInMillis = dateTaken }
                val itemYear = itemCalendar.get(Calendar.YEAR)
                val itemMonth = itemCalendar.get(Calendar.MONTH)
                val itemDay = itemCalendar.get(Calendar.DAY_OF_MONTH)

                val isOnThisDay = (itemMonth == currentMonth && itemDay == currentDay && itemYear < currentYear)

                var finalBlurScore = 150f
                var isSimilar = false
                var mlCategory: MediaCategory? = null

                val category = when {
                    isOnThisDay -> MediaCategory.ON_THIS_DAY
                    isScreenshot && dateTaken < thirtyDaysAgo -> MediaCategory.OLD_SCREENSHOTS
                    isScreenshot -> null // Skip recent screenshots from being flagged for cleanup
                    size > 50 * 1024 * 1024 -> MediaCategory.HEAVY_MEDIA
                    else -> {
                        // Perform real-time local image analysis & ML classification
                        val thumb = loadThumbnail(context, contentUri)
                        if (thumb != null) {
                            finalBlurScore = ImageAnalyzer.calculateBlurScore(thumb)
                            val hash = ImageAnalyzer.computePerceptualHash(thumb)
                            mlCategory = ImageAnalyzer.classifySmartCategory(thumb, name)

                            // Check duplicates (taken within 15 seconds, low Hamming distance)
                            val match = previousHashes.firstOrNull { (prevHash, prevTime) ->
                                Math.abs(dateTaken - prevTime) < 15000L && ImageAnalyzer.hammingDistance(hash, prevHash) <= 8
                            }
                            if (match != null) {
                                isSimilar = true
                            }
                            if (hash.isNotEmpty()) {
                                previousHashes.add(hash to dateTaken)
                                if (previousHashes.size > 25) {
                                    previousHashes.removeAt(0)
                                }
                            }
                        }
                        
                        when {
                            isSimilar -> MediaCategory.SIMILAR_BURSTS
                            finalBlurScore < 40f -> MediaCategory.BLURRY_PHOTOS
                            mlCategory != null -> mlCategory
                            name.contains("receipt", ignoreCase = true) || name.contains("doc", ignoreCase = true) -> MediaCategory.RECEIPTS_DOCS
                            else -> null
                        }
                    }
                }

                if (category != null) {
                    mediaList.add(
                        MediaItem(
                            id = id,
                            uri = contentUri,
                            title = name,
                            sizeBytes = size,
                            dateTakenMillis = dateTaken,
                            category = category,
                            width = width,
                            height = height,
                            blurScore = finalBlurScore,
                            isVideo = false
                        )
                    )
                }
            }
        }

        // Query Videos > 50MB
        val videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        context.contentResolver.query(
            videoUri,
            projection,
            "${MediaStore.MediaColumns.SIZE} > ?",
            arrayOf("${50 * 1024 * 1024}"),
            "${MediaStore.MediaColumns.DATE_TAKEN} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn) + 1_000_000L
                if (keptIds.contains(id)) continue

                val name = cursor.getString(nameColumn) ?: "Video"
                val size = cursor.getLong(sizeColumn)
                var dateTaken = cursor.getLong(dateColumn)
                if (dateTaken == 0L) dateTaken = System.currentTimeMillis()
                val mime = cursor.getString(mimeColumn) ?: "video/mp4"

                val contentUri = ContentUris.withAppendedId(videoUri, id - 1_000_000L)

                mediaList.add(
                    MediaItem(
                        id = id,
                        uri = contentUri,
                        title = name,
                        sizeBytes = size,
                        dateTakenMillis = dateTaken,
                        category = MediaCategory.HEAVY_MEDIA,
                        isVideo = true
                    )
                )
            }
        }

        return mediaList
    }

    private fun generateSampleItems(context: Context, keptIds: Set<Long>): List<MediaItem> {
        val samples = mutableListOf<MediaItem>()
        val sampleDir = File(context.cacheDir, "sample_photos").apply { mkdirs() }

        val now = System.currentTimeMillis()
        val fortyDaysAgo = now - (40L * 24 * 60 * 60 * 1000)
        val oneYearAgo = Calendar.getInstance().apply {
            add(Calendar.YEAR, -1)
        }.timeInMillis

        var sampleIdCounter = 1001L

        // Helper to draw sample image file
        fun createSampleFile(fileName: String, label: String, bgColor: Int, accentColor: Int): Uri {
            val file = File(sampleDir, fileName)
            if (!file.exists()) {
                val bitmap = Bitmap.createBitmap(800, 1000, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(bgColor)

                val paint = Paint().apply {
                    color = accentColor
                    textSize = 48f
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(label, 400f, 480f, paint)

                val subPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 32f
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("SnapSweep Sample Photo", 400f, 550f, subPaint)

                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                bitmap.recycle()
            }
            return Uri.fromFile(file)
        }

        val sampleConfigs = listOf(
            // Old Screenshots
            Triple("screenshot_1.jpg", "📱 Screenshot 2024-03-15", MediaCategory.OLD_SCREENSHOTS),
            Triple("screenshot_2.jpg", "📱 Order Confirmation #8942", MediaCategory.OLD_SCREENSHOTS),
            Triple("screenshot_3.jpg", "📱 Text Message Thread", MediaCategory.OLD_SCREENSHOTS),
            Triple("screenshot_4.jpg", "📱 Map Directions Screen", MediaCategory.OLD_SCREENSHOTS),
            
            // Blurry
            Triple("blurry_1.jpg", "🌁 Out of Focus Motion Shot", MediaCategory.BLURRY_PHOTOS),
            Triple("blurry_2.jpg", "🌁 Low Light Night Scene", MediaCategory.BLURRY_PHOTOS),
            Triple("blurry_3.jpg", "🌁 Shaky Moving Subject", MediaCategory.BLURRY_PHOTOS),

            // Similar / Bursts
            Triple("burst_1.jpg", "👯 Burst Shot #1 (Best)", MediaCategory.SIMILAR_BURSTS),
            Triple("burst_2.jpg", "👯 Burst Shot #2 (Duplicate)", MediaCategory.SIMILAR_BURSTS),
            Triple("burst_3.jpg", "👯 Burst Shot #3 (Duplicate)", MediaCategory.SIMILAR_BURSTS),
            Triple("burst_4.jpg", "👯 Group Selfie #2", MediaCategory.SIMILAR_BURSTS),

            // Receipts & Documents
            Triple("receipt_1.jpg", "🧾 Target Grocery Receipt", MediaCategory.RECEIPTS_DOCS),
            Triple("receipt_2.jpg", "🧾 Office Whiteboard Notes", MediaCategory.RECEIPTS_DOCS),
            Triple("receipt_3.jpg", "🧾 Parking Ticket Stub", MediaCategory.RECEIPTS_DOCS),

            // ML Travel
            Triple("travel_1.jpg", "✈️ Beach Resort Sunset Shot", MediaCategory.TRAVEL),
            Triple("travel_2.jpg", "✈️ Mountain Hiking Landmark", MediaCategory.TRAVEL),

            // ML Food
            Triple("food_1.jpg", "🍕 Plated Artisan Pizza & Coffee", MediaCategory.FOOD),
            Triple("food_2.jpg", "🍔 Burger & Fries Lunch", MediaCategory.FOOD),

            // ML Pets
            Triple("pet_1.jpg", "🐶 Golden Retriever Park Portrait", MediaCategory.PETS),
            Triple("pet_2.jpg", "🐱 Sleeping Tabby Cat", MediaCategory.PETS),

            // Heavy Media
            Triple("heavy_video_1.jpg", "🎬 4K Beach Sunset Video (180MB)", MediaCategory.HEAVY_MEDIA),
            Triple("heavy_video_2.jpg", "🎬 Screen Recording 20Mins (240MB)", MediaCategory.HEAVY_MEDIA),

            // On This Day
            Triple("on_this_day_1.jpg", "📅 Last Year Today Concert", MediaCategory.ON_THIS_DAY),
            Triple("on_this_day_2.jpg", "📅 Last Year Today Lunch", MediaCategory.ON_THIS_DAY)
        )

        val colors = listOf(
            Color.parseColor("#1E293B") to Color.parseColor("#38BDF8"),
            Color.parseColor("#0F172A") to Color.parseColor("#F59E0B"),
            Color.parseColor("#172554") to Color.parseColor("#8B5CF6"),
            Color.parseColor("#064E3B") to Color.parseColor("#10B981"),
            Color.parseColor("#4C0519") to Color.parseColor("#EF4444"),
            Color.parseColor("#831843") to Color.parseColor("#EC4899")
        )

        for ((index, config) in sampleConfigs.withIndex()) {
            val id = sampleIdCounter++
            if (keptIds.contains(id)) continue

            val (fileName, title, category) = config
            val colorPair = colors[index % colors.size]
            val uri = createSampleFile(fileName, title, colorPair.first, colorPair.second)

            val size = when (category) {
                MediaCategory.HEAVY_MEDIA -> (120L + (index * 60L)) * 1024 * 1024
                MediaCategory.OLD_SCREENSHOTS -> (2L + index) * 1024 * 1024
                MediaCategory.RECEIPTS_DOCS -> (1L + index) * 1024 * 512
                else -> (3L + index) * 1024 * 1024
            }

            val dateTaken = when (category) {
                MediaCategory.OLD_SCREENSHOTS -> fortyDaysAgo - (index * 86400000L)
                MediaCategory.ON_THIS_DAY -> oneYearAgo
                else -> now - (index * 3600000L)
            }

            samples.add(
                MediaItem(
                    id = id,
                    uri = uri,
                    title = title,
                    sizeBytes = size,
                    dateTakenMillis = dateTaken,
                    category = category,
                    width = 800,
                    height = 1000,
                    blurScore = if (category == MediaCategory.BLURRY_PHOTOS) 24.5f else 150.0f,
                    isVideo = category == MediaCategory.HEAVY_MEDIA,
                    isSample = true
                )
            )
        }

        return samples
    }

    private fun loadThumbnail(context: Context, uri: Uri): Bitmap? {
        return try {
            val contentResolver = context.contentResolver
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentResolver.loadThumbnail(uri, android.util.Size(128, 128), null)
            } else {
                // Pre-Q loading with inSampleSize scaling
                var inputStream = contentResolver.openInputStream(uri) ?: return null
                val options = android.graphics.BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                android.graphics.BitmapFactory.decodeStream(inputStream, null, options)
                inputStream.close()

                val targetSize = 128
                var scale = 1
                while (options.outWidth / scale / 2 >= targetSize && options.outHeight / scale / 2 >= targetSize) {
                    scale *= 2
                }

                val decodeOptions = android.graphics.BitmapFactory.Options().apply {
                    inSampleSize = scale
                }
                inputStream = contentResolver.openInputStream(uri) ?: return null
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream, null, decodeOptions)
                inputStream.close()
                bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
