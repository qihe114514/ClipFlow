package com.qihe.clipflow.ui.parser

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.qihe.clipflow.data.api.model.ContentItem
import com.qihe.clipflow.data.api.model.ContentType
import com.qihe.clipflow.data.api.model.DouyinStatistics
import com.qihe.clipflow.data.api.model.MediaInfo
import com.qihe.clipflow.data.api.model.VideoBackupItem
import com.qihe.clipflow.data.repository.SupportedPlatform
import com.qihe.clipflow.util.DownloadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ParsePageUiState(
    val inputUrl: String = "",
    val isParsing: Boolean = false,
    val parseResult: List<ContentItem>? = null,
    val parseTitle: String = "",
    val parseDesc: String = "",
    val parseCover: String = "",
    val authorName: String = "",
    val authorAvatar: String = "",
    val contentType: String = "",
    val shareUrl: String = "",
    val stats: DouyinStatistics? = null,
    val videoBackups: List<VideoBackupItem> = emptyList(),
    val videoUrl: String = "",
    val isBackgroundDownload: Boolean = false,
    val error: String? = null,
    val downloadStates: Map<String, DownloadState> = emptyMap(),
    val showDownloadDialog: Boolean = false,
    val downloadingItemId: String? = null
)

open class PlatformParseViewModel(
    application: Application,
    platform: SupportedPlatform
) : AndroidViewModel(application) {

    private val parseSupport = ParseScreenSupport(
        application = application,
        platform = platform,
        scope = viewModelScope
    )

    private val _uiState = MutableStateFlow(ParsePageUiState())
    val uiState: StateFlow<ParsePageUiState> = _uiState

    init {
        viewModelScope.launch {
            parseSupport.downloadSession.collectLatest { session ->
                _uiState.update {
                    it.copy(
                        downloadStates = session.downloadStates,
                        showDownloadDialog = session.showDownloadDialog,
                        downloadingItemId = session.downloadingItemId,
                        isBackgroundDownload = session.isBackgroundDownload
                    )
                }
            }
        }
    }

    fun onUrlChange(url: String) {
        _uiState.update { it.copy(inputUrl = url, error = null) }
    }

    fun clearUrl() {
        _uiState.value = ParsePageUiState(
            downloadStates = _uiState.value.downloadStates,
            showDownloadDialog = _uiState.value.showDownloadDialog,
            downloadingItemId = _uiState.value.downloadingItemId,
            isBackgroundDownload = _uiState.value.isBackgroundDownload
        )
    }

    fun pasteFromClipboard() {
        parseSupport.readClipboardText().takeIf { it.isNotEmpty() }?.let(::onUrlChange)
    }

    fun parse() {
        val rawInput = _uiState.value.inputUrl
        if (rawInput.isBlank()) {
            _uiState.update { it.copy(error = parseSupport.emptyInputMessage()) }
            return
        }
        val sourceUrl = parseSupport.normalizeInput(rawInput)

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isParsing = true,
                    error = null,
                    parseResult = null,
                    shareUrl = "",
                    stats = null,
                    videoBackups = emptyList(),
                    videoUrl = ""
                )
            }

            parseSupport.parse(sourceUrl).fold(
                onSuccess = { result ->
                    _uiState.update {
                        it.copy(
                            isParsing = false,
                            parseResult = result.items,
                            parseTitle = result.title,
                            parseDesc = result.desc,
                            parseCover = result.cover,
                            authorName = result.authorName,
                            authorAvatar = result.authorAvatar,
                            contentType = result.contentType,
                            shareUrl = result.shareUrl,
                            stats = result.stats,
                            videoBackups = result.videoBackups,
                            videoUrl = result.items.firstOrNull()?.url.orEmpty()
                        )
                    }
                    parseSupport.saveHistory(sourceUrl, result)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isParsing = false,
                            error = parseSupport.userMessage(error)
                        )
                    }
                }
            )
        }
    }

    fun downloadItem(item: ContentItem) {
        parseSupport.startDownload(item)
    }

    fun downloadBackupUrl(url: String, label: String) {
        downloadItem(
            ContentItem(
                id = "backup_${System.currentTimeMillis()}",
                type = ContentType.VIDEO,
                url = url,
                description = label,
                mediaInfo = MediaInfo(format = "MP4")
            )
        )
    }

    fun dismissDownloadDialog(background: Boolean = false) {
        parseSupport.dismissDownloadDialog(background)
    }
}
