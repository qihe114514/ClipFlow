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
import com.qihe.clipflow.util.DownloadState
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
    val showDownloadDialog: Boolean = false
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
                _state.update { it.copy(loading = false, result = result) }
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
    fun download(qualityIndex: Int) = viewModelScope.launch {
        val state = _state.value
        val details: BilibiliVideoDetails = state.result?.bilibili ?: return@launch
        val quality = details.qualities.getOrNull(qualityIndex) ?: return@launch
        if (quality.audioUrls.isEmpty()) {
            _state.update { it.copy(error = "当前清晰度缺少音频，无法合成 MP4") }
            return@launch
        }
        DownloadPillState.hide()
        _state.update { it.copy(download = null, showDownloadDialog = true, error = null) }
        downloads.download(quality.streamUrls, quality.audioUrls, state.result?.title.orEmpty()) { progress ->
            _state.update { it.copy(download = progress) }
            if (!_state.value.showDownloadDialog && progress.isDownloading) {
                DownloadPillState.update(progress.progress, progress.speedText)
            }
            if (!progress.isDownloading) {
                DownloadPillState.hide()
            }
        }.onFailure { error ->
            DownloadPillState.hide()
            _state.update { it.copy(showDownloadDialog = true, error = error.message ?: "下载或合成失败，请稍后重试") }
        }
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
