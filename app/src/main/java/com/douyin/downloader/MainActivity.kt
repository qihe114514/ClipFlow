package com.douyin.downloader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.douyin.downloader.ui.MainViewModel
import com.douyin.downloader.ui.screens.HistoryScreen
import com.douyin.downloader.ui.screens.MainScreen
import com.douyin.downloader.ui.screens.SettingsScreen
import com.douyin.downloader.ui.theme.DouyinDownloaderTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DouyinDownloaderTheme {
                val uiState by viewModel.uiState.collectAsState()
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "main",
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable("main") {
                        MainScreen(
                            uiState = uiState,
                            onUrlChange = { viewModel.updateShareUrl(it) },
                            onParseClick = { viewModel.parseVideo() },
                            onDownloadClick = { index, item -> viewModel.downloadItem(index, item) },
                            onSettingsClick = { navController.navigate("settings") },
                            onHistoryClick = { navController.navigate("history") },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            savePath = uiState.savePath,
                            bgWallpaperUri = uiState.bgWallpaperUri,
                            bgWallpaperType = uiState.bgWallpaperType,
                            bgBlurRadius = uiState.bgBlurRadius,
                            bgOpacity = uiState.bgOpacity,
                            videoSoundEnabled = uiState.videoSoundEnabled,
                            onSetSavePath = { viewModel.setSavePath(it) },
                            onSetBgWallpaper = { uri, type -> viewModel.setBgWallpaper(uri, type) },
                            onSetBgBlurRadius = { viewModel.setBgBlurRadius(it) },
                            onSetBgOpacity = { viewModel.setBgOpacity(it) },
                            onSetVideoSoundEnabled = { viewModel.setVideoSoundEnabled(it) },
                            onClearBgWallpaper = { viewModel.clearBgWallpaper() },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("history") {
                        HistoryScreen(
                            historyList = uiState.parseHistory,
                            onItemClick = { entry ->
                                viewModel.updateShareUrl(entry.url)
                                viewModel.parseVideo()
                                navController.popBackStack()
                            },
                            onClearHistory = { viewModel.clearHistory() },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
