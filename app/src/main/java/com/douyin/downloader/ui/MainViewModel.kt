package com.douyin.downloader.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.douyin.downloader.api.DouyinApiClient
import com.douyin.downloader.data.SettingsDataStore
import com.douyin.downloader.download.DownloadManager
import com.douyin.downloader.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MainUiState(
    val shareUrl: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val parsedData: VideoData? = null,
    val downloadItems: List<DownloadItem> = emptyList(),
    val downloadStatus: Map<Int, DownloadStatus> = emptyMap(),
    // 设置
    val savePath: String = "",
    val bgWallpaperUri: String = "",
    val bgWallpaperType: String = "none",
    val bgBlurRadius: Float = 0f,
    val bgOpacity: Float = 0.5f,
    val showSettings: Boolean = false
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

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        // 加载设置
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
    }

    fun updateShareUrl(url: String) {
        _uiState.update { it.copy(shareUrl = url, error = null) }
    }

    fun toggleSettings(show: Boolean) {
        _uiState.update { it.copy(showSettings = show) }
    }

    fun parseVideo() {
        val url = _uiState.value.shareUrl.trim()
        if (url.isEmpty()) {
            _uiState.update { it.copy(error = "请输入视频链接") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, parsedData = null, downloadItems = emptyList()) }

            val result = apiClient.parseVideo(url)

            result.fold(
                onSuccess = { response ->
                    val data = response.data
                    if (data != null) {
                        val items = data.getAllVideoUrls()
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
        if (statusMap[index] is DownloadStatus.Downloading) {
            return // 已经在下载中
        }

        statusMap[index] = DownloadStatus.Downloading
        _uiState.update { it.copy(downloadStatus = statusMap) }

        viewModelScope.launch {
            val savePath = if (_uiState.value.savePath.isNotBlank()) _uiState.value.savePath else null
            val result = downloadManager.downloadToGallery(item, savePath)

            result.fold(
                onSuccess = { path ->
                    val map = _uiState.value.downloadStatus.toMutableMap()
                    map[index] = DownloadStatus.Success(path)
                    _uiState.update { it.copy(downloadStatus = map) }
                },
                onFailure = { e ->
                    val map = _uiState.value.downloadStatus.toMutableMap()
                    map[index] = DownloadStatus.Error(e.message ?: "下载失败")
                    _uiState.update { it.copy(downloadStatus = map) }
                }
            )
        }
    }

    // 设置操作
    fun setSavePath(uri: Uri) {
        viewModelScope.launch {
            settingsStore.setSavePath(uri.toString())
        }
    }

    fun setBgWallpaper(uri: Uri, type: String) {
        viewModelScope.launch {
            // 获取持久化权限
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

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
