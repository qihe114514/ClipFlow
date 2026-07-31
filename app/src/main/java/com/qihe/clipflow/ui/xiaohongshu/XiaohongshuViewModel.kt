package com.qihe.clipflow.ui.xiaohongshu

import android.app.Application
import android.content.ClipboardManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.qihe.clipflow.data.api.model.ContentItem
import com.qihe.clipflow.data.local.AppDatabase
import com.qihe.clipflow.data.repository.HistoryRepository
import com.qihe.clipflow.data.repository.HistorySaver
import com.qihe.clipflow.data.repository.ParseException
import com.qihe.clipflow.data.repository.ParseRepository
import com.qihe.clipflow.util.DownloadCoordinator
import com.qihe.clipflow.util.DownloadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class XiaohongshuUiState(
    val inputUrl: String = "",
    val isParsing: Boolean = false,
    val parseResult: List<ContentItem>? = null,
    val parseTitle: String = "",
    val parseDesc: String = "",
    val parseCover: String = "",
    val authorName: String = "",
    val authorAvatar: String = "",
    val contentType: String = "",
    val error: String? = null,
    val downloadStates: Map<String, DownloadState> = emptyMap(),
    val showDownloadDialog: Boolean = false,
    val downloadingItemId: String? = null,
    val isBackgroundDownload: Boolean = false
)

class XiaohongshuViewModel(application: Application) : AndroidViewModel(application) {

    private val parseRepository = ParseRepository()
    private val historyRepository = HistoryRepository(
        AppDatabase.getInstance(application).historyDao()
    )
    private val historySaver = HistorySaver(historyRepository)
    private val downloadCoordinator = DownloadCoordinator(application, "xiaohongshu", viewModelScope)

    private val _uiState = MutableStateFlow(XiaohongshuUiState())
    val uiState: StateFlow<XiaohongshuUiState> = _uiState

    init {
        viewModelScope.launch {
            downloadCoordinator.session.collectLatest { session ->
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
        _uiState.value = XiaohongshuUiState(
            downloadStates = _uiState.value.downloadStates,
            showDownloadDialog = _uiState.value.showDownloadDialog,
            downloadingItemId = _uiState.value.downloadingItemId,
            isBackgroundDownload = _uiState.value.isBackgroundDownload
        )
    }

    fun pasteFromClipboard() {
        val clipboard = getApplication<Application>()
            .getSystemService(Application.CLIPBOARD_SERVICE) as ClipboardManager
        val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
        if (text.isNotEmpty()) {
            _uiState.update { it.copy(inputUrl = text, error = null) }
        }
    }

    fun parse() {
        val rawInput = _uiState.value.inputUrl
        if (rawInput.isBlank()) {
            _uiState.update { it.copy(error = "请粘贴小红书分享链接") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isParsing = true,
                    error = null,
                    parseResult = null
                )
            }

            parseRepository.parseXiaohongshu(rawInput).fold(
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
                            contentType = result.contentType
                        )
                    }
                    historySaver.save(
                        platform = "xiaohongshu",
                        sourceUrl = rawInput.trim(),
                        result = result,
                        defaultTitle = result.title.ifEmpty { "小红书笔记" },
                        fallbackContentType = "note"
                    )
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isParsing = false,
                            error = error.toUserMessage()
                        )
                    }
                }
            )
        }
    }

    fun downloadItem(item: ContentItem) {
        downloadCoordinator.startDownload(item)
    }

    fun dismissDownloadDialog(background: Boolean = false) {
        downloadCoordinator.dismiss(background)
    }

    private fun Throwable.toUserMessage(): String {
        return if (this is ParseException) {
            failure.message
        } else {
            message ?: "解析失败"
        }
    }
}
