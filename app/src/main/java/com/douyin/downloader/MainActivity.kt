package com.douyin.downloader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.douyin.downloader.ui.MainViewModel
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

                if (uiState.showSettings) {
                    SettingsScreen(
                        savePath = uiState.savePath,
                        bgWallpaperUri = uiState.bgWallpaperUri,
                        bgWallpaperType = uiState.bgWallpaperType,
                        bgBlurRadius = uiState.bgBlurRadius,
                        bgOpacity = uiState.bgOpacity,
                        onSetSavePath = { viewModel.setSavePath(it) },
                        onSetBgWallpaper = { uri, type -> viewModel.setBgWallpaper(uri, type) },
                        onSetBgBlurRadius = { viewModel.setBgBlurRadius(it) },
                        onSetBgOpacity = { viewModel.setBgOpacity(it) },
                        onClearBgWallpaper = { viewModel.clearBgWallpaper() },
                        onBack = { viewModel.toggleSettings(false) }
                    )
                } else {
                    MainScreen(
                        uiState = uiState,
                        onUrlChange = { viewModel.updateShareUrl(it) },
                        onParseClick = { viewModel.parseVideo() },
                        onDownloadClick = { index, item -> viewModel.downloadItem(index, item) },
                        onSettingsClick = { viewModel.toggleSettings(true) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
