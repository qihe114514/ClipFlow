package com.douyin.downloaderqh.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.douyin.downloaderqh.api.DouyinApiClient
import com.douyin.downloaderqh.data.SettingsDataStore
import com.douyin.downloaderqh.download.DownloadManager
import com.douyin.downloaderqh.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import kotlinx.serialization.Serializable

@Serializable
data class HistoryEntry(
    val url: String,
    val title: String,
    val type: String,
    val timestamp: Long
)

data class MainUiState(
    val shareUrl: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val parsedData: VideoData? = null,
    val downloadItems: List<DownloadItem> = emptyList(),
    val downloadStatus: Map<Int, DownloadStatus> = emptyMap(),
    val downloadProgress: Map<Int, Float> = emptyMap(),
    val downloadSpeed: Map<Int, String> = emptyMap(),
    // 设置
    val savePath: String = "",
    val bgWallpaperUri: String = "",
    val bgWallpaperType: String = "none",
    val bgBlurRadius: Float = 0f,
    val bgOpacity: Float = 0.5f,
    val videoSoundEnabled: Boolean = false,
    // 历史记录
    val parseHistory: List<HistoryEntry> = emptyList()
)

sealed class DownloadStatus {
    data object Idle : DownloadStatus()
    data object Downloading : DownloadStatus()
    data class Success(val path: String) : DownloadStatus()
    data class Error(val message: String) : DownloadStatus()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val apiClient = DouyinApiClient()
    val downloadManager = DownloadManager(application)
    private val settingsStore = SettingsDataStore(application)
    private val appContext = application

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsStore.savePath.collect { path ->
                _uiState.update { it.copy(savePath = path) }
            }
        }
        viewModelScope.launch {
            settingsStore.bgWallpaperUri.collect { uri ->
                _uiState.update { it.copy(bgWallpaperUri = uri) }
            }
        }
        viewModelScope.launch {
            settingsStore.bgWallpaperType.collect { type ->
                _uiState.update { it.copy(bgWallpaperType = type) }
            }
        }
        viewModelScope.launch {
            settingsStore.bgBlurRadius.collect { radius ->
                _uiState.update { it.copy(bgBlurRadius = radius) }
            }
        }
        viewModelScope.launch {
            settingsStore.bgOpacity.collect { opacity ->
                _uiState.update { it.copy(bgOpacity = opacity) }
            }
        }
        viewModelScope.launch {
            settingsStore.videoSoundEnabled.collect { enabled ->
                _uiState.update { it.copy(videoSoundEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            settingsStore.parseHistory.collect { history ->
                _uiState.update { it.copy(parseHistory = history) }
            }
        }
    }

    fun updateShareUrl(url: String) {
        _uiState.update { it.copy(shareUrl = url, error = null) }
    }

    fun parseVideo() {
        val url = _uiState.value.shareUrl.trim()
        if (url.isEmpty()) {
            _uiState.update { it.copy(error = "请输入视频链接") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, parsedData = null, downloadItems = emptyList(), downloadStatus = emptyMap(), downloadProgress = emptyMap(), downloadSpeed = emptyMap()) }

            val result = apiClient.parseVideo(url)

            result.fold(
                onSuccess = { response ->
                    val data = response.data
                    if (data != null) {
                        val items = data.getAllVideoUrls()
                        settingsStore.addParseHistory(
                            HistoryEntry(
                                url = url,
                                title = data.title.ifEmpty { "未知视频" },
                                type = data.type,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                parsedData = data,
                                downloadItems = items,
                                error = null
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "API返回了空数据"
                            )
                        }
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "解析失败，请检查链接后重试"
                        )
                    }
                }
            )
        }
    }

    fun downloadItem(index: Int, item: DownloadItem) {
        val statusMap = _uiState.value.downloadStatus.toMutableMap()
        if (statusMap[index] is DownloadStatus.Downloading) return

        statusMap[index] = DownloadStatus.Downloading
        _uiState.update { it.copy(downloadStatus = statusMap) }

        triggerLightHaptic()

        viewModelScope.launch {
            try {
                val savePath = if (_uiState.value.savePath.isNotBlank()) _uiState.value.savePath else null
                downloadManager.downloadWithProgress(item, savePath).collect { pair ->
                    if (pair == null) {
                        // 下载完成
                        val map = _uiState.value.downloadStatus.toMutableMap()
                        map[index] = DownloadStatus.Success("")
                        _uiState.update { it.copy(downloadStatus = map) }
                    } else {
                        val (progress, speed) = pair
                        val progressMap = _uiState.value.downloadProgress.toMutableMap()
                        val speedMap = _uiState.value.downloadSpeed.toMutableMap()
                        progressMap[index] = progress
                        speedMap[index] = speed
                        _uiState.update {
                            it.copy(downloadProgress = progressMap, downloadSpeed = speedMap)
                        }
                    }
                }
            } catch (e: Exception) {
                val map = _uiState.value.downloadStatus.toMutableMap()
                map[index] = DownloadStatus.Error(e.message ?: "下载失败")
                _uiState.update { it.copy(downloadStatus = map) }
            }
        }
    }

    private fun triggerLightHaptic() {
        try {
            val ctx = appContext
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = manager.defaultVibrator
                vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (_: Exception) {}
    }

    fun setSavePath(uri: Uri) {
        viewModelScope.launch {
            settingsStore.setSavePath(uri.toString())
        }
    }

    fun setBgWallpaper(uri: Uri, type: String) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            settingsStore.setBgWallpaper(uri.toString(), type)
        }
    }

    fun setBgBlurRadius(radius: Float) {
        viewModelScope.launch {
            settingsStore.setBgBlurRadius(radius)
        }
    }

    fun setBgOpacity(opacity: Float) {
        viewModelScope.launch {
            settingsStore.setBgOpacity(opacity)
        }
    }

    fun setVideoSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setVideoSoundEnabled(enabled)
        }
    }

    fun clearBgWallpaper() {
        viewModelScope.launch {
            val curUri = _uiState.value.bgWallpaperUri
            if (curUri.isNotBlank()) {
                try {
                    val context = getApplication<Application>()
                    context.contentResolver.releasePersistableUriPermission(
                        Uri.parse(curUri),
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}
            }
            settingsStore.setBgWallpaper("", "none")
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            settingsStore.clearParseHistory()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}