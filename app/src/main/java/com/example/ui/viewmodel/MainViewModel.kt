package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.CleanupRecordEntity
import com.example.data.local.KeptMediaEntity
import com.example.data.local.UserStatsEntity
import com.example.data.model.CategorySummary
import com.example.data.model.MediaCategory
import com.example.data.model.MediaItem
import com.example.data.model.StorageStats
import com.example.data.scanner.MediaScannerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import com.example.data.firebase.FirebaseManager
import com.example.ui.theme.ThemeMode

import com.example.ui.screens.SmartSortOption

sealed interface SwipeAction {
    data class Trashed(val item: MediaItem) : SwipeAction
    data class Kept(val item: MediaItem) : SwipeAction
}

data class MainUiState(
    val isScanning: Boolean = false,
    val scanProgress: Float = 0f,
    val categories: List<CategorySummary> = emptyList(),
    val pendingTrash: List<MediaItem> = emptyList(),
    val activeCategory: MediaCategory? = null,
    val activeQueue: List<MediaItem> = emptyList(),
    val activeIndex: Int = 0,
    val activeSortOption: SmartSortOption = SmartSortOption.HIGHEST_CLUTTER,
    val swipeHistory: List<SwipeAction> = emptyList(),
    val stats: StorageStats = StorageStats(),
    val fullscreenItem: MediaItem? = null,
    val showTrashSheet: Boolean = false,
    val userNotification: String? = null,
    val pendingDeleteIntentSender: android.content.IntentSender? = null,
    val pendingDeleteItems: List<MediaItem> = emptyList(),
    val hasImagesPermission: Boolean = false,
    val hasVideosPermission: Boolean = false,
    val taggedMediaCount: Int = 0,
    val isBackgroundWorkerRunning: Boolean = false,
    val bgWorkerMessage: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val showRateDialog: Boolean = false,
    val showThemeDialog: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val showPrivacyInfoDialog: Boolean = false,
    val isPowerSavingMode: Boolean = false,
    val showMonthlyReportDialog: Boolean = false,
    val hasUserRatedApp: Boolean = false,
    val hasCompletedOnboarding: Boolean = false,
    val showOnboarding: Boolean = true,
    val lastTrashedItem: MediaItem? = null,
    val showBulkCleanDialog: Boolean = false,
    val isAutoPurgeEnabled: Boolean = true,
    val showGestureGuideOverlay: Boolean = false,
    val hasSeenGestureGuide: Boolean = false,
    val weeklyWorkerMessage: String? = null,
    val cleanupStreakDays: Int = 3,
    val lastCleanupDateMillis: Long = System.currentTimeMillis()
)

class MainViewModel(
    private val database: AppDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val cleanupDao = database.cleanupDao()

    init {
        observeStats()
    }

    private fun observeStats() {
        viewModelScope.launch {
            cleanupDao.getUserStatsFlow().collect { entity ->
                if (entity != null) {
                    _uiState.update {
                        it.copy(
                            stats = StorageStats(
                                totalBytesFreed = entity.totalBytesFreed,
                                totalItemsCleaned = entity.totalItemsCleaned,
                                lastCleanupMillis = entity.lastCleanupMillis
                            )
                        )
                    }
                }
            }
        }
    }

    fun updatePermissionState(hasImages: Boolean, hasVideos: Boolean) {
        _uiState.update {
            it.copy(
                hasImagesPermission = hasImages,
                hasVideosPermission = hasVideos
            )
        }
    }

    fun checkPermissions(context: Context) {
        val hasImages: Boolean
        val hasVideos: Boolean
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            hasImages = androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.READ_MEDIA_IMAGES
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            hasVideos = androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.READ_MEDIA_VIDEO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            val hasStorage = androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            hasImages = hasStorage
            hasVideos = hasStorage
        }
        updatePermissionState(hasImages, hasVideos)
    }

    fun scan(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, scanProgress = 0.05f) }
            val repository = MediaScannerRepository(context, cleanupDao)

            val summaries = repository.scanMedia { progress ->
                _uiState.update { it.copy(scanProgress = progress) }
            }

            val taggedCount = cleanupDao.getAllTaggedMedia().size

            _uiState.update {
                it.copy(
                    isScanning = false,
                    scanProgress = 1.0f,
                    categories = summaries,
                    taggedMediaCount = taggedCount
                )
            }
        }
    }

    fun triggerBackgroundScan(context: Context) {
        _uiState.update {
            it.copy(
                isBackgroundWorkerRunning = true,
                bgWorkerMessage = "Background scan worker initiated..."
            )
        }

        val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.data.worker.MediaScanWorker>()
            .build()

        val workManager = androidx.work.WorkManager.getInstance(context)
        workManager.enqueueUniqueWork(
            com.example.data.worker.MediaScanWorker.WORK_NAME,
            androidx.work.ExistingWorkPolicy.REPLACE,
            workRequest
        )

        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(workRequest.id).collect { workInfo ->
                if (workInfo != null) {
                    when (workInfo.state) {
                        androidx.work.WorkInfo.State.SUCCEEDED -> {
                            val dups = workInfo.outputData.getInt(com.example.data.worker.MediaScanWorker.KEY_DUPLICATES_COUNT, 0)
                            val screenshots = workInfo.outputData.getInt(com.example.data.worker.MediaScanWorker.KEY_SCREENSHOTS_COUNT, 0)
                            val total = workInfo.outputData.getInt(com.example.data.worker.MediaScanWorker.KEY_TOTAL_TAGGED, 0)
                            
                            val taggedCount = cleanupDao.getAllTaggedMedia().size
                            _uiState.update {
                                it.copy(
                                    isBackgroundWorkerRunning = false,
                                    taggedMediaCount = taggedCount,
                                    bgWorkerMessage = "Tagged $dups duplicates & $screenshots screenshots via background hashing & aspect ratio scan.",
                                    userNotification = "Background scan complete! $total items tagged for potential deletion."
                                )
                            }
                            scan(context)
                        }
                        androidx.work.WorkInfo.State.FAILED -> {
                            _uiState.update {
                                it.copy(
                                    isBackgroundWorkerRunning = false,
                                    bgWorkerMessage = "Background scan worker encountered an error."
                                )
                            }
                        }
                        androidx.work.WorkInfo.State.RUNNING -> {
                            _uiState.update {
                                it.copy(
                                    isBackgroundWorkerRunning = true,
                                    bgWorkerMessage = "Worker running: Hashing image files & matching screen aspect ratios..."
                                )
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun sortQueue(items: List<MediaItem>, sortOption: SmartSortOption): List<MediaItem> {
        return when (sortOption) {
            SmartSortOption.HIGHEST_CLUTTER -> items.sortedByDescending {
                if (it.category == MediaCategory.BLURRY_PHOTOS) 100f - it.blurScore else it.sizeBytes.toFloat()
            }
            SmartSortOption.LARGEST_FIRST -> items.sortedByDescending { it.sizeBytes }
            SmartSortOption.NEWEST_FIRST -> items.sortedByDescending { it.dateTakenMillis }
            SmartSortOption.OLDEST_FIRST -> items.sortedBy { it.dateTakenMillis }
        }
    }

    fun setSortOption(sortOption: SmartSortOption) {
        _uiState.update { state ->
            val sorted = sortQueue(state.activeQueue, sortOption)
            state.copy(
                activeSortOption = sortOption,
                activeQueue = sorted,
                activeIndex = 0
            )
        }
    }

    fun setShowMonthlyReportDialog(show: Boolean) {
        _uiState.update { it.copy(showMonthlyReportDialog = show) }
    }

    private fun updateStreak() {
        val now = System.currentTimeMillis()
        val lastClean = _uiState.value.lastCleanupDateMillis
        val currentStreak = _uiState.value.cleanupStreakDays

        if (lastClean == 0L) {
            _uiState.update { it.copy(cleanupStreakDays = 1, lastCleanupDateMillis = now) }
            return
        }

        val diffMs = now - lastClean
        val diffHours = diffMs / (1000 * 60 * 60)

        if (diffHours < 24) {
            _uiState.update { it.copy(lastCleanupDateMillis = now) }
        } else if (diffHours < 48) {
            _uiState.update { it.copy(cleanupStreakDays = currentStreak + 1, lastCleanupDateMillis = now) }
        } else {
            _uiState.update { it.copy(cleanupStreakDays = 1, lastCleanupDateMillis = now) }
        }
    }

    fun openCategoryCleaner(
        category: MediaCategory,
        sortOption: SmartSortOption = _uiState.value.activeSortOption
    ) {
        val summary = _uiState.value.categories.firstOrNull { it.category == category }
        val pendingIds = _uiState.value.pendingTrash.map { it.id }.toSet()
        val rawItems = summary?.items?.filter { !pendingIds.contains(it.id) } ?: emptyList()
        val sortedItems = sortQueue(rawItems, sortOption)

        val showGuide = !_uiState.value.hasSeenGestureGuide

        _uiState.update {
            it.copy(
                activeCategory = category,
                activeQueue = sortedItems,
                activeSortOption = sortOption,
                activeIndex = 0,
                swipeHistory = emptyList(),
                showGestureGuideOverlay = showGuide,
                hasSeenGestureGuide = true
            )
        }
    }

    fun closeCategoryCleaner() {
        _uiState.update {
            it.copy(
                activeCategory = null,
                activeQueue = emptyList(),
                activeIndex = 0,
                swipeHistory = emptyList()
            )
        }
    }

    fun swipeLeft(item: MediaItem) {
        updateStreak()
        val updatedTrash = _uiState.value.pendingTrash.toMutableList().apply {
            if (none { it.id == item.id }) add(item)
        }
        val history = _uiState.value.swipeHistory + SwipeAction.Trashed(item)

        _uiState.update { state ->
            val nextIndex = state.activeIndex + 1
            state.copy(
                pendingTrash = updatedTrash,
                activeIndex = nextIndex,
                swipeHistory = history,
                lastTrashedItem = item
            )
        }
    }

    fun dismissLastTrashedItem() {
        _uiState.update { it.copy(lastTrashedItem = null) }
    }

    fun setShowBulkCleanDialog(show: Boolean) {
        _uiState.update { it.copy(showBulkCleanDialog = show) }
    }

    fun bulkCleanCategories(
        targetCategories: List<MediaCategory> = listOf(MediaCategory.OLD_SCREENSHOTS, MediaCategory.BLURRY_PHOTOS)
    ) {
        val allMatchingItems = _uiState.value.categories
            .filter { targetCategories.contains(it.category) }
            .flatMap { it.items }

        if (allMatchingItems.isEmpty()) return

        val updatedTrash = _uiState.value.pendingTrash.toMutableList()
        var addedCount = 0
        for (item in allMatchingItems) {
            if (updatedTrash.none { it.id == item.id }) {
                updatedTrash.add(item)
                addedCount++
            }
        }

        _uiState.update { state ->
            state.copy(
                pendingTrash = updatedTrash,
                showBulkCleanDialog = false,
                showTrashSheet = true,
                userNotification = "Added $addedCount items to pending trash queue!"
            )
        }
    }

    fun swipeRight(item: MediaItem) {
        viewModelScope.launch {
            cleanupDao.insertKeptMedia(KeptMediaEntity(item.id, System.currentTimeMillis()))
        }
        val history = _uiState.value.swipeHistory + SwipeAction.Kept(item)

        _uiState.update { state ->
            val nextIndex = state.activeIndex + 1
            state.copy(
                activeIndex = nextIndex,
                swipeHistory = history
            )
        }
    }

    fun undoLastSwipe() {
        val history = _uiState.value.swipeHistory
        if (history.isEmpty()) return

        val lastAction = history.last()
        val remainingHistory = history.dropLast(1)

        _uiState.update { state ->
            val newIndex = (state.activeIndex - 1).coerceAtLeast(0)
            val updatedTrash = state.pendingTrash.toMutableList()

            when (lastAction) {
                is SwipeAction.Trashed -> {
                    updatedTrash.removeAll { it.id == lastAction.item.id }
                }
                is SwipeAction.Kept -> {
                    viewModelScope.launch {
                        cleanupDao.deleteKeptMedia(lastAction.item.id)
                    }
                }
            }

            state.copy(
                activeIndex = newIndex,
                pendingTrash = updatedTrash,
                swipeHistory = remainingHistory
            )
        }
    }

    fun removeFromTrash(item: MediaItem) {
        _uiState.update { state ->
            val updated = state.pendingTrash.filter { it.id != item.id }
            state.copy(pendingTrash = updated)
        }
    }

    fun confirmAndPurgeTrash(context: Context, onComplete: () -> Unit = {}) {
        val itemsToDelete = _uiState.value.pendingTrash
        if (itemsToDelete.isEmpty()) return

        viewModelScope.launch {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                // Android 11+: Use MediaStore.createDeleteRequest to securely prompt user for system permission
                try {
                    val uris = itemsToDelete.map { it.uri }
                    val pendingIntent = android.provider.MediaStore.createDeleteRequest(context.contentResolver, uris)
                    _uiState.update { state ->
                        state.copy(
                            pendingDeleteIntentSender = pendingIntent.intentSender,
                            pendingDeleteItems = itemsToDelete
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Fallback delete
                    performDirectDeletion(context, itemsToDelete)
                }
            } else {
                // Pre-Android 11: delete directly with ContentResolver
                performDirectDeletion(context, itemsToDelete)
            }
            onComplete()
        }
    }

    private suspend fun performDirectDeletion(context: Context, realItems: List<MediaItem>) {
        var freedBytes = 0L
        val records = mutableListOf<CleanupRecordEntity>()

        for (item in realItems) {
            try {
                val deleted = context.contentResolver.delete(item.uri, null, null)
                if (deleted > 0) {
                    freedBytes += item.sizeBytes
                    records.add(
                        CleanupRecordEntity(
                            mediaId = item.id,
                            categoryName = item.category.name,
                            sizeBytes = item.sizeBytes,
                            cleanedAtMillis = System.currentTimeMillis()
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (records.isNotEmpty()) {
            val currentStats = cleanupDao.getUserStats() ?: UserStatsEntity()
            val newStats = UserStatsEntity(
                id = 1,
                totalBytesFreed = currentStats.totalBytesFreed + freedBytes,
                totalItemsCleaned = currentStats.totalItemsCleaned + records.size,
                lastCleanupMillis = System.currentTimeMillis()
            )

            cleanupDao.insertCleanupRecords(records)
            cleanupDao.insertOrUpdateUserStats(newStats)
        }

        _uiState.update { state ->
            val remainingTrash = state.pendingTrash.filter { !realItems.contains(it) }
            state.copy(
                pendingTrash = remainingTrash,
                showTrashSheet = false,
                userNotification = "Cleaned ${records.size} items",
                activeCategory = null,
                activeQueue = emptyList()
            )
        }
        scan(context)
    }

    fun onDeleteRequestCompleted(context: Context, deletedItems: List<MediaItem>) {
        viewModelScope.launch {
            var freedBytes = 0L
            val records = mutableListOf<CleanupRecordEntity>()

            for (item in deletedItems) {
                freedBytes += item.sizeBytes
                records.add(
                    CleanupRecordEntity(
                        mediaId = item.id,
                        categoryName = item.category.name,
                        sizeBytes = item.sizeBytes,
                        cleanedAtMillis = System.currentTimeMillis()
                    )
                )
            }

            val currentStats = cleanupDao.getUserStats() ?: UserStatsEntity()
            val newStats = UserStatsEntity(
                id = 1,
                totalBytesFreed = currentStats.totalBytesFreed + freedBytes,
                totalItemsCleaned = currentStats.totalItemsCleaned + deletedItems.size,
                lastCleanupMillis = System.currentTimeMillis()
            )

            cleanupDao.insertCleanupRecords(records)
            cleanupDao.insertOrUpdateUserStats(newStats)

            val msg = "Cleaned ${deletedItems.size} items • Saved ${String.format("%.1f MB", freedBytes / (1024f * 1024f))}"

            _uiState.update { state ->
                val remainingTrash = state.pendingTrash.filter { !deletedItems.contains(it) }
                state.copy(
                    pendingTrash = remainingTrash,
                    pendingDeleteIntentSender = null,
                    pendingDeleteItems = emptyList(),
                    showTrashSheet = false,
                    userNotification = msg,
                    activeCategory = null,
                    activeQueue = emptyList()
                )
            }

            FirebaseManager.logStorageCleaned(freedBytes, "batch_media")

            scan(context)
            checkForRatingMilestone()
        }
    }

    fun clearPendingDeleteRequest() {
        _uiState.update {
            it.copy(
                pendingDeleteIntentSender = null,
                pendingDeleteItems = emptyList()
            )
        }
    }

    fun setFullscreenItem(item: MediaItem?) {
        _uiState.update { it.copy(fullscreenItem = item) }
    }

    fun setShowTrashSheet(show: Boolean) {
        _uiState.update { it.copy(showTrashSheet = show) }
    }

    fun clearNotification() {
        _uiState.update { it.copy(userNotification = null) }
    }

    fun finishOnboarding() {
        _uiState.update {
            it.copy(
                hasCompletedOnboarding = true,
                showOnboarding = false
            )
        }
    }

    fun reopenOnboarding() {
        _uiState.update { it.copy(showOnboarding = true) }
    }

    fun setThemeMode(mode: ThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
        FirebaseManager.logEvent("theme_changed", mapOf("mode" to mode.name))
    }

    fun setShowThemeDialog(show: Boolean) {
        _uiState.update { it.copy(showThemeDialog = show) }
    }

    fun setShowSettingsDialog(show: Boolean) {
        _uiState.update { it.copy(showSettingsDialog = show) }
    }

    fun setShowPrivacyInfoDialog(show: Boolean) {
        _uiState.update { it.copy(showPrivacyInfoDialog = show) }
    }

    fun checkPowerSavingMode(context: Context) {
        val batteryStatus: android.content.Intent? = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        val level: Int = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct: Float = if (level >= 0 && scale > 0) level * 100 / scale.toFloat() else 100f

        val status: Int = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging: Boolean = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                status == android.os.BatteryManager.BATTERY_STATUS_FULL

        val isLowBattery = batteryPct < 20f && !isCharging
        _uiState.update { it.copy(isPowerSavingMode = isLowBattery) }
    }

    fun setShowRateDialog(show: Boolean) {
        _uiState.update { it.copy(showRateDialog = show) }
    }

    fun onUserRatedApp(stars: Int) {
        _uiState.update {
            it.copy(
                hasUserRatedApp = true,
                showRateDialog = false,
                userNotification = "Thank you for rating SnapSweep!"
            )
        }
    }

    fun checkForRatingMilestone() {
        val currentState = _uiState.value
        if (!currentState.hasUserRatedApp && currentState.stats.totalItemsCleaned >= 2) {
            _uiState.update { it.copy(showRateDialog = true) }
        }
    }

    fun setAutoPurgeEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isAutoPurgeEnabled = enabled) }
    }

    fun setShowGestureGuideOverlay(show: Boolean) {
        _uiState.update { it.copy(showGestureGuideOverlay = show, hasSeenGestureGuide = true) }
    }

    fun scheduleWeeklyCleanupWorker(context: Context) {
        val periodicRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.data.worker.WeeklyCleanupWorker>(
            7, java.util.concurrent.TimeUnit.DAYS
        ).build()

        androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            com.example.data.worker.WeeklyCleanupWorker.WORK_NAME,
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
    }

    fun triggerWeeklyWorkerNow(context: Context) {
        val oneTimeRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.data.worker.WeeklyCleanupWorker>()
            .build()

        val workManager = androidx.work.WorkManager.getInstance(context)
        workManager.enqueueUniqueWork(
            "WeeklyCleanupTestWork",
            androidx.work.ExistingWorkPolicy.REPLACE,
            oneTimeRequest
        )

        _uiState.update { it.copy(weeklyWorkerMessage = "Weekly cleanup worker enqueued!") }

        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(oneTimeRequest.id).collect { workInfo ->
                if (workInfo != null && workInfo.state == androidx.work.WorkInfo.State.SUCCEEDED) {
                    val itemsFound = workInfo.outputData.getInt(com.example.data.worker.WeeklyCleanupWorker.KEY_ITEMS_FOUND, 0)
                    _uiState.update {
                        it.copy(
                            weeklyWorkerMessage = "Weekly scanner checked gallery: Found $itemsFound clutter items. Notification dispatched!",
                            userNotification = "Weekly cleanup check complete! Found $itemsFound clutter items."
                        )
                    }
                }
            }
        }
    }

    fun checkAndExecute30DayAutoPurge(context: Context) {
        if (!_uiState.value.isAutoPurgeEnabled) return

        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val expiredTrashItems = _uiState.value.pendingTrash.filter { it.dateTakenMillis < thirtyDaysAgo }

        if (expiredTrashItems.isNotEmpty()) {
            val freedBytes = expiredTrashItems.sumOf { it.sizeBytes }
            _uiState.update { state ->
                val remainingTrash = state.pendingTrash.filter { !expiredTrashItems.contains(it) }
                state.copy(
                    pendingTrash = remainingTrash,
                    userNotification = "Auto-purged ${expiredTrashItems.size} items older than 30 days from Recently Deleted trash!"
                )
            }
        }
    }

    companion object {
        fun factory(database: AppDatabase) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(database) as T
            }
        }
    }
}
