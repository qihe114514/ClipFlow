package com.qihe.clipflow.ui.bilibili

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.qihe.clipflow.data.local.AppDatabase
import com.qihe.clipflow.data.repository.BilibiliPlatformParser
import com.qihe.clipflow.data.repository.HistoryRepository
import com.qihe.clipflow.data.repository.HistorySaver
import com.qihe.clipflow.data.repository.BilibiliVideoDetails
import com.qihe.clipflow.data.repository.ParseResult
import com.qihe.clipflow.ui.components.DownloadPillState
import com.qihe.clipflow.util.BilibiliDownloadManager
import com.qihe.clipflow.util.DownloadForegroundService
import com.qihe.clipflow.util.DownloadNotifier
import com.qihe.clipflow.util.DownloadSessionTracker
import com.qihe.clipflow.util.DownloadState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BilibiliUiState(
    val input: String = "",
    val loading: Boolean = false,
    val result: ParseResult? = null,
    val selectedPart: Int = 0,
    val error: String? = null,
    val download: DownloadState? = null,
    val showDownloadDialog: Boolean = false,
    /** 本视频已成功保存过的清晰度（按列表 index），用于显示“已保存”。 */
    val completedQualityIds: Set<Int> = emptySet()
)

class BilibiliViewModel(application: Application) : AndroidViewModel(application) {
    private val parser = BilibiliPlatformParser()
    private val downloads = BilibiliDownloadManager(application)
    private val historySaver = HistorySaver(
        HistoryRepository(AppDatabase.getInstance(application).historyDao())
    )
    private val _state = MutableStateFlow(BilibiliUiState())
    val state: StateFlow<BilibiliUiState> = _state

    fun setInput(value: String) = _state.update { it.copy(input = value, error = null) }
    fun parse() = viewModelScope.launch {
        val input = _state.value.input
        _state.update { it.copy(loading = true, error = null, result = null) }
        val sourceUrl = parser.normalizeInput(input)
        parser.parse(sourceUrl).fold(
            onSuccess = { result ->
                _state.update { it.copy(loading = false, result = result, completedQualityIds = emptySet()) }
                historySaver.save(
                    platform = "bilibili",
                    sourceUrl = sourceUrl,
                    result = result,
                    defaultTitle = "B 站视频",
                    fallbackContentType = "bilibili"
                )
            },
            onFailure = { error -> _state.update { it.copy(loading = false, error = error.message ?: "解析失败") } }
        )
    }
    fun selectPart(index: Int) {
        val bvid = _state.value.result?.bilibili?.bvid ?: return
        _state.update { it.copy(selectedPart = index, input = "https://www.bilibili.com/video/$bvid?p=${index + 1}") }
        parse()
    }
    private var downloadJob: Job? = null
    private var activeNotificationKey: String? = null

    fun download(qualityIndex: Int) {
        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            val state = _state.value
            val details: BilibiliVideoDetails = state.result?.bilibili ?: return@launch
            val quality = details.qualities.getOrNull(qualityIndex) ?: return@launch
            if (quality.audioUrls.isEmpty()) {
                _state.update { it.copy(error = "当前清晰度缺少音频，无法合成 MP4") }
                return@launch
            }
            DownloadPillState.hide()
            _state.update { it.copy(download = null, showDownloadDialog = true, error = null) }
            val notificationKey = "bilibili_${System.currentTimeMillis()}"
            activeNotificationKey = notificationKey
            val notificationTitle = state.result?.title.orEmpty().ifBlank { "B 站视频" }
            DownloadSessionTracker.begin(notificationKey)
            DownloadForegroundService.start(getApplication())
            downloads.download(quality.streamUrls, quality.audioUrls, state.result?.title.orEmpty()) { progress ->
                _state.update { it.copy(download = progress) }
                if (progress.isDownloading) {
                    DownloadSessionTracker.update(progress.progress, progress.speedText)
                }
                if (progress.isComplete) {
                    DownloadSessionTracker.finish(notificationKey)
                    DownloadNotifier.complete(getApplication(), notificationKey, notificationTitle)
                    _state.update { it.copy(completedQualityIds = it.completedQualityIds + qualityIndex) }
                    activeNotificationKey = null
                }
                if (progress.error != null) {
                    DownloadSessionTracker.finish(notificationKey)
                    DownloadNotifier.cancel(getApplication(), notificationKey)
                    activeNotificationKey = null
                }
                if (!_state.value.showDownloadDialog && progress.isDownloading) {
                    DownloadPillState.update(progress.progress, progress.speedText)
                }
                if (!progress.isDownloading) {
                    DownloadPillState.hide()
                }
            }.onFailure { error ->
                DownloadSessionTracker.finish(notificationKey)
                DownloadNotifier.cancel(getApplication(), notificationKey)
                activeNotificationKey = null
                DownloadPillState.hide()
                _state.update { it.copy(showDownloadDialog = true, error = error.message ?: "下载或合成失败，请稍后重试") }
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        activeNotificationKey?.let {
            DownloadSessionTracker.finish(it)
            DownloadNotifier.cancel(getApplication(), it)
        }
        activeNotificationKey = null
        DownloadPillState.hide()
        _state.update { it.copy(download = null, showDownloadDialog = false) }
    }

    fun backgroundDownload() {
        val active = _state.value.download?.takeIf { it.isDownloading } ?: return
        _state.update { it.copy(showDownloadDialog = false) }
        DownloadPillState.sourceRoute = "bilibili"
        DownloadPillState.show(active.progress, active.speedText) {
            DownloadPillState.hide()
            _state.update { it.copy(showDownloadDialog = true) }
        }
    }

    fun dismissDownload() {
        DownloadPillState.hide()
        _state.update { it.copy(download = null, showDownloadDialog = false) }
    }
}
