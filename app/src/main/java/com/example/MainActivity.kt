package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.ui.components.FullscreenViewer
import com.example.ui.components.PendingTrashSheet
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SwipeCleanerScreen
import com.example.ui.theme.SnapSweepTheme
import com.example.ui.viewmodel.MainViewModel

import com.example.ui.components.RateAppDialog
import com.example.ui.components.ThemeSelectorDialog
import com.example.ui.components.SettingsDialog

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)

        setContent {
            val viewModel: MainViewModel = viewModel(
                factory = MainViewModel.factory(database)
            )
            val uiState by viewModel.uiState.collectAsState()
            val context = LocalContext.current

            SnapSweepTheme(themeMode = uiState.themeMode) {

                // Permissions Launcher
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    viewModel.checkPermissions(context)
                    viewModel.scan(context)
                }

                val requestGalleryPermissions = {
                    val permissionsNeeded = mutableListOf<String>()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                            permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES)
                        }
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                            permissionsNeeded.add(Manifest.permission.READ_MEDIA_VIDEO)
                        }
                    } else {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                    }

                    if (permissionsNeeded.isNotEmpty()) {
                        permissionLauncher.launch(permissionsNeeded.toTypedArray())
                    } else {
                        viewModel.checkPermissions(context)
                        viewModel.scan(context)
                    }
                }

                LaunchedEffect(uiState.showOnboarding) {
                    viewModel.checkPermissions(context)
                    if (!uiState.showOnboarding) {
                        val permissionsNeeded = mutableListOf<String>()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES)
                            }
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                                permissionsNeeded.add(Manifest.permission.READ_MEDIA_VIDEO)
                            }
                        } else {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                        }

                        if (permissionsNeeded.isNotEmpty()) {
                            permissionLauncher.launch(permissionsNeeded.toTypedArray())
                        } else {
                            viewModel.scan(context)
                        }
                    }
                }

                // Activity Result Launcher for secure Media Store deletion
                val deleteRequestLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartIntentSenderForResult()
                ) { result ->
                    if (result.resultCode == RESULT_OK) {
                        viewModel.onDeleteRequestCompleted(context, uiState.pendingDeleteItems)
                    } else {
                        viewModel.clearPendingDeleteRequest()
                    }
                }

                LaunchedEffect(uiState.pendingDeleteIntentSender) {
                    uiState.pendingDeleteIntentSender?.let { sender ->
                        try {
                            deleteRequestLauncher.launch(
                                IntentSenderRequest.Builder(sender).build()
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                            viewModel.clearPendingDeleteRequest()
                        }
                    }
                }

                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                if (uiState.showOnboarding) {
                    OnboardingScreen(
                        hasImagesPermission = uiState.hasImagesPermission,
                        hasVideosPermission = uiState.hasVideosPermission,
                        onRequestPermissions = { requestGalleryPermissions() },
                        onFinishOnboarding = { viewModel.finishOnboarding() }
                    )
                } else if (uiState.activeCategory != null) {
                    SwipeCleanerScreen(
                        category = uiState.activeCategory!!,
                        queue = uiState.activeQueue,
                        currentIndex = uiState.activeIndex,
                        pendingTrashCount = uiState.pendingTrash.size,
                        canUndo = uiState.swipeHistory.isNotEmpty(),
                        onBack = { viewModel.closeCategoryCleaner() },
                        onSwipeLeft = { item -> viewModel.swipeLeft(item) },
                        onSwipeRight = { item -> viewModel.swipeRight(item) },
                        onUndo = { viewModel.undoLastSwipe() },
                        onZoom = { item -> viewModel.setFullscreenItem(item) },
                        onOpenTrashSheet = { viewModel.setShowTrashSheet(true) }
                    )
                } else {
                    DashboardScreen(
                        stats = uiState.stats,
                        categories = uiState.categories,
                        pendingTrash = uiState.pendingTrash,
                        isScanning = uiState.isScanning,
                        scanProgress = uiState.scanProgress,
                        userNotification = uiState.userNotification,
                        hasImagesPermission = uiState.hasImagesPermission,
                        hasVideosPermission = uiState.hasVideosPermission,
                        isWorkerRunning = uiState.isBackgroundWorkerRunning,
                        taggedCount = uiState.taggedMediaCount,
                        bgWorkerMessage = uiState.bgWorkerMessage,
                        onRunBackgroundWorker = { viewModel.triggerBackgroundScan(context) },
                        onRequestPermission = { requestGalleryPermissions() },
                        onRefreshScan = { viewModel.scan(context) },
                        onOpenCategory = { category -> viewModel.openCategoryCleaner(category) },
                        onOpenTrashSheet = { viewModel.setShowTrashSheet(true) },
                        onClearNotification = { viewModel.clearNotification() },
                        onOpenSettings = { viewModel.setShowSettingsDialog(true) }
                    )
                }

                // Pending Trash Sheet Dialog
                if (uiState.showTrashSheet) {
                    PendingTrashSheet(
                        pendingTrash = uiState.pendingTrash,
                        onRemoveFromTrash = { item -> viewModel.removeFromTrash(item) },
                        onConfirmDeleteAll = {
                            viewModel.confirmAndPurgeTrash(context)
                        },
                        onDismiss = { viewModel.setShowTrashSheet(false) },
                        sheetState = sheetState
                    )
                }

                // Settings & Customization Dialog
                if (uiState.showSettingsDialog) {
                    SettingsDialog(
                        currentThemeMode = uiState.themeMode,
                        onThemeSelected = { mode -> viewModel.setThemeMode(mode) },
                        isWorkerRunning = uiState.isBackgroundWorkerRunning,
                        taggedCount = uiState.taggedMediaCount,
                        bgWorkerMessage = uiState.bgWorkerMessage,
                        onRunBackgroundWorker = { viewModel.triggerBackgroundScan(context) },
                        onReplayOnboarding = { viewModel.reopenOnboarding() },
                        onShowRateApp = { viewModel.setShowRateDialog(true) },
                        onDismiss = { viewModel.setShowSettingsDialog(false) }
                    )
                }

                // Smart 5-Star Rate App Dialog
                if (uiState.showRateDialog) {
                    RateAppDialog(
                        onDismiss = { viewModel.setShowRateDialog(false) },
                        onRateSubmitted = { stars -> viewModel.onUserRatedApp(stars) }
                    )
                }

                // Fullscreen Zoom Dialog
                uiState.fullscreenItem?.let { item ->
                    FullscreenViewer(
                        item = item,
                        onDismiss = { viewModel.setFullscreenItem(null) },
                        onSwipeLeft = { itemToTrash -> viewModel.swipeLeft(itemToTrash) },
                        onSwipeRight = { itemToKeep -> viewModel.swipeRight(itemToKeep) }
                    )
                }
            }
        }
    }
}
