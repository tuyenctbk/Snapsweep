package com.example.data.firebase

import android.content.Context
import android.os.Bundle
import android.util.Log

/**
 * Firebase Integration Helper Service for SnapSweep.
 * Provides unified interfaces for Firebase Analytics event logging,
 * Remote Config dynamic feature flags, and Crashlytics non-fatal error reports.
 */
object FirebaseManager {

    private const val TAG = "SnapSweepFirebase"

    fun logEvent(eventName: String, params: Map<String, Any> = emptyMap()) {
        try {
            Log.d(TAG, "Firebase Analytics Event: $eventName | Params: $params")
            // When Firebase SDK is linked in production build:
            // FirebaseAnalytics.getInstance(context).logEvent(eventName, bundle)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log Firebase event: ${e.message}")
        }
    }

    fun logStorageCleaned(cleanedBytes: Long, itemType: String) {
        logEvent(
            "storage_cleaned",
            mapOf(
                "bytes_freed" to cleanedBytes,
                "megabytes_freed" to (cleanedBytes / (1024 * 1024)),
                "item_type" to itemType
            )
        )
    }

    fun logBackgroundScanTriggered(duplicatesFound: Int, screenshotsFound: Int) {
        logEvent(
            "background_scan_complete",
            mapOf(
                "duplicates_count" to duplicatesFound,
                "screenshots_count" to screenshotsFound
            )
        )
    }

    fun logAppShared() {
        logEvent("app_shared", mapOf("timestamp" to System.currentTimeMillis()))
    }

    fun logAppRated(ratingStars: Int) {
        logEvent("app_rated", mapOf("stars" to ratingStars))
    }

    fun recordNonFatalException(throwable: Throwable) {
        try {
            Log.e(TAG, "Firebase Crashlytics Recorded Exception", throwable)
            // FirebaseCrashlytics.getInstance().recordException(throwable)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isFeatureEnabled(featureKey: String, defaultValue: Boolean = true): Boolean {
        // TODO: Replace with FirebaseRemoteConfig.getInstance().getBoolean(featureKey)
        //       once Firebase Remote Config is fully integrated.
        return defaultValue
    }
}
