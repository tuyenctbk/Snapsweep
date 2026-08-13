package com.example.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.data.local.AppDatabase
import com.example.data.scanner.MediaScannerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeeklyCleanupWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "WeeklyCleanupWorker"
        const val CHANNEL_ID = "weekly_cleanup_channel"
        const val NOTIFICATION_ID = 2002
        const val KEY_ITEMS_FOUND = "items_found"
        /** Minimum clutter count required before the weekly notification is sent. */
        private const val CLUTTER_NOTIFY_THRESHOLD = 5
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val database = AppDatabase.getInstance(appContext)
            val repository = MediaScannerRepository(appContext, database.cleanupDao())

            val summaries = repository.scanMedia { }
            val screenshotCount = summaries.firstOrNull { it.category.name == "OLD_SCREENSHOTS" }?.count ?: 0
            val blurryCount = summaries.firstOrNull { it.category.name == "BLURRY_PHOTOS" }?.count ?: 0

            val totalClutterCount = screenshotCount + blurryCount

            val shouldNotify = totalClutterCount >= CLUTTER_NOTIFY_THRESHOLD

            if (shouldNotify) {
                showWeeklyNotification(totalClutterCount, screenshotCount, blurryCount)
            }

            Result.success(workDataOf(KEY_ITEMS_FOUND to totalClutterCount))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun showWeeklyNotification(total: Int, screenshots: Int, blurry: Int) {
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Weekly Cleanup Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Weekly reminders when over 100 screenshots and blurry photos accumulate"
            }
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("🧹 Weekly SnapSweep Cleanup")
            .setContentText("You have accumulated $total clutter items ($screenshots screenshots, $blurry blurry photos). Clean now to free space!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            manager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
