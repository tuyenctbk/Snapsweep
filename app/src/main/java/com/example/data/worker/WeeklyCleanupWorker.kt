package com.example.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.scanner.MediaScannerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Background WorkManager task that runs weekly to scan for duplicate screenshots
 * and out-of-focus clutter to keep device storage optimized automatically.
 */
class WeeklyCleanupWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "WeeklyCleanupWorker"
        const val CHANNEL_ID = "weekly_cleanup_channel"
        const val NOTIFICATION_ID = 2002
        const val KEY_ITEMS_FOUND = "items_found"
        const val KEY_SCREENSHOTS_FOUND = "screenshots_found"
        const val KEY_BYTES_RECLAIMABLE = "bytes_reclaimable"
        /** Minimum clutter count required before the weekly notification is sent. */
        private const val CLUTTER_NOTIFY_THRESHOLD = 5

        /**
         * Schedules the periodic weekly WorkManager scan with battery constraints.
         */
        fun scheduleWeeklyScan(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<WeeklyCleanupWorker>(
                7, TimeUnit.DAYS,
                12, TimeUnit.HOURS // flex interval
            )
                .setConstraints(constraints)
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val database = AppDatabase.getInstance(appContext)
            val repository = MediaScannerRepository(appContext, database.cleanupDao())

            val summaries = repository.scanMedia { }
            val screenshotSummary = summaries.firstOrNull { it.category.name == "OLD_SCREENSHOTS" }
            val duplicateSummary = summaries.firstOrNull { it.category.name == "SIMILAR_BURSTS" }
            val blurrySummary = summaries.firstOrNull { it.category.name == "BLURRY_PHOTOS" }

            val screenshotCount = screenshotSummary?.count ?: 0
            val duplicateCount = duplicateSummary?.count ?: 0
            val blurryCount = blurrySummary?.count ?: 0

            val totalClutterCount = screenshotCount + duplicateCount + blurryCount
            val totalReclaimableBytes = (screenshotSummary?.totalSizeBytes ?: 0L) +
                    (duplicateSummary?.totalSizeBytes ?: 0L) +
                    (blurrySummary?.totalSizeBytes ?: 0L)

            // Trigger notification if duplicate screenshots or clutter accumulate
            if (totalClutterCount > 0) {
                showWeeklyNotification(totalClutterCount, screenshotCount + duplicateCount, blurryCount)
            }

            Result.success(
                workDataOf(
                    KEY_ITEMS_FOUND to totalClutterCount,
                    KEY_SCREENSHOTS_FOUND to (screenshotCount + duplicateCount),
                    KEY_BYTES_RECLAIMABLE to totalReclaimableBytes
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun showWeeklyNotification(total: Int, screenshots: Int, blurry: Int) {
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = appContext.getString(R.string.weekly_scan_title)
            val channelDesc = appContext.getString(R.string.weekly_scan_desc)
            val channel = NotificationChannel(
                CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = channelDesc
            }
            manager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notificationTitle = appContext.getString(R.string.weekly_scan_notification_title)
        val notificationText = appContext.getString(R.string.weekly_scan_notification_body, total, screenshots, blurry)

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
            .setContentIntent(pendingIntent)
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
