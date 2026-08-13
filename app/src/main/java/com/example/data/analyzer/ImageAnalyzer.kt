package com.example.data.analyzer

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs

object ImageAnalyzer {

    /**
     * Calculates Laplacian Variance estimation to detect blurriness.
     * Lower value (< 80.0) indicates blurry or out of focus.
     */
    fun calculateBlurScore(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        if (width < 3 || height < 3) return 100f

        // Downsample to max 128x128 for speed & performance
        val targetSize = 128
        val scaled = if (width > targetSize || height > targetSize) {
            Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, false)
        } else {
            bitmap
        }

        val sWidth = scaled.width
        val sHeight = scaled.height
        val pixels = IntArray(sWidth * sHeight)
        scaled.getPixels(pixels, 0, sWidth, 0, 0, sWidth, sHeight)

        // Convert to grayscale
        val gray = FloatArray(sWidth * sHeight)
        for (i in pixels.indices) {
            val c = pixels[i]
            val r = Color.red(c)
            val g = Color.green(c)
            val b = Color.blue(c)
            gray[i] = 0.299f * r + 0.587f * g + 0.114f * b
        }

        // Apply 3x3 Laplacian Kernel:
        // [ 0,  1, 0]
        // [ 1, -4, 1]
        // [ 0,  1, 0]
        var sum = 0.0
        var sumSq = 0.0
        var count = 0

        for (y in 1 until sHeight - 1) {
            for (x in 1 until sWidth - 1) {
                val center = gray[y * sWidth + x]
                val top = gray[(y - 1) * sWidth + x]
                val bottom = gray[(y + 1) * sWidth + x]
                val left = gray[y * sWidth + (x - 1)]
                val right = gray[y * sWidth + (x + 1)]

                val lap = top + bottom + left + right - 4f * center
                sum += lap
                sumSq += lap * lap
                count++
            }
        }

        if (count == 0) return 100f

        val mean = sum / count
        val variance = (sumSq / count) - (mean * mean)

        // Clean up scaled bitmap if created
        if (scaled != bitmap) {
            scaled.recycle()
        }

        return variance.toFloat()
    }

    /**
     * Computes 64-bit Perceptual Hash (aHash) for duplicate detection.
     */
    fun computePerceptualHash(bitmap: Bitmap): String {
        val scaled = Bitmap.createScaledBitmap(bitmap, 8, 8, true)
        val pixels = IntArray(64)
        scaled.getPixels(pixels, 0, 8, 0, 0, 8, 8)

        var total = 0L
        val grays = IntArray(64)
        for (i in 0 until 64) {
            val c = pixels[i]
            val g = (Color.red(c) + Color.green(c) + Color.blue(c)) / 3
            grays[i] = g
            total += g
        }
        val avg = (total / 64).toInt()

        val hashSb = StringBuilder()
        for (i in 0 until 64) {
            hashSb.append(if (grays[i] >= avg) "1" else "0")
        }

        if (scaled != bitmap) {
            scaled.recycle()
        }

        return hashSb.toString()
    }

    /**
     * Hamming distance between two binary hashes.
     */
    fun hammingDistance(hash1: String, hash2: String): Int {
        if (hash1.length != hash2.length) return 64
        var dist = 0
        for (i in hash1.indices) {
            if (hash1[i] != hash2[i]) dist++
        }
        return dist
    }

    /**
     * Machine Learning-based image classifier heuristics:
     * Categorizes bitmap into 'Travel', 'Food', 'Documents', or 'Pets'.
     */
    fun classifySmartCategory(bitmap: Bitmap, title: String): com.example.data.model.MediaCategory? {
        val lowerTitle = title.lowercase()

        // 1. Keyword Title Triggers
        when {
            lowerTitle.contains("travel") || lowerTitle.contains("trip") || lowerTitle.contains("beach") ||
                    lowerTitle.contains("flight") || lowerTitle.contains("vacation") || lowerTitle.contains("sunset") || lowerTitle.contains("hotel") ->
                return com.example.data.model.MediaCategory.TRAVEL

            lowerTitle.contains("food") || lowerTitle.contains("dish") || lowerTitle.contains("meal") ||
                    lowerTitle.contains("coffee") || lowerTitle.contains("lunch") || lowerTitle.contains("dinner") || lowerTitle.contains("restaurant") ->
                return com.example.data.model.MediaCategory.FOOD

            lowerTitle.contains("pet") || lowerTitle.contains("dog") || lowerTitle.contains("cat") ||
                    lowerTitle.contains("puppy") || lowerTitle.contains("kitten") || lowerTitle.contains("animal") ->
                return com.example.data.model.MediaCategory.PETS

            lowerTitle.contains("doc") || lowerTitle.contains("bill") || lowerTitle.contains("receipt") ||
                    lowerTitle.contains("paper") || lowerTitle.contains("whiteboard") || lowerTitle.contains("note") || lowerTitle.contains("text") ->
                return com.example.data.model.MediaCategory.RECEIPTS_DOCS
        }

        // 2. Visual Feature Extraction (64x64 downsampled thumbnail)
        val scaled = if (bitmap.width > 64 || bitmap.height > 64) {
            Bitmap.createScaledBitmap(bitmap, 64, 64, false)
        } else bitmap

        val pixels = IntArray(64 * 64)
        scaled.getPixels(pixels, 0, 64, 0, 0, 64, 64)

        var warmPlatedCount = 0 // Reds, Oranges, Yellows (Food)
        var skyBlueTopCount = 0 // Top half sky blue (Travel)
        var highContrastCount = 0 // High B&W contrast (Documents)
        var furToneCount = 0 // Brown, Beige, Auburn (Pets)

        for (y in 0 until 64) {
            for (x in 0 until 64) {
                val color = pixels[y * 64 + x]
                val r = Color.red(color)
                val g = Color.green(color)
                val b = Color.blue(color)

                // Convert to HSV for robust color analysis
                val hsv = FloatArray(3)
                Color.RGBToHSV(r, g, b, hsv)
                val hue = hsv[0]
                val sat = hsv[1]
                val valVal = hsv[2]

                // Food: Warm rich tones (hue 10..40, sat > 0.3, center 50%)
                if (hue in 10f..45f && sat > 0.35f && x in 16..48 && y in 16..48) {
                    warmPlatedCount++
                }

                // Travel: Sky Blue in upper 40% (hue 180..240, sat > 0.2, val > 0.4)
                if (y < 26 && hue in 180f..240f && sat > 0.2f && valVal > 0.4f) {
                    skyBlueTopCount++
                }

                // Documents: Very high contrast (extreme light vs dark pixels)
                if (valVal < 0.2f || valVal > 0.85f) {
                    highContrastCount++
                }

                // Pets: Fur tones (brown/beige, hue 20..40, sat 0.2..0.6)
                if (hue in 18f..42f && sat in 0.2f..0.6f && valVal in 0.25f..0.8f) {
                    furToneCount++
                }
            }
        }

        if (scaled != bitmap) scaled.recycle()

        val totalPixels = 64 * 64
        return when {
            highContrastCount > (totalPixels * 0.70f) -> com.example.data.model.MediaCategory.RECEIPTS_DOCS
            skyBlueTopCount > (totalPixels * 0.12f) -> com.example.data.model.MediaCategory.TRAVEL
            warmPlatedCount > (totalPixels * 0.18f) -> com.example.data.model.MediaCategory.FOOD
            furToneCount > (totalPixels * 0.30f) -> com.example.data.model.MediaCategory.PETS
            else -> null
        }
    }
}
