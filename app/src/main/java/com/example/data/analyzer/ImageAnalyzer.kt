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
}
