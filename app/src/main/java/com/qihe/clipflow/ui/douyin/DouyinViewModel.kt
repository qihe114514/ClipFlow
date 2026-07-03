package com.qihe.clipflow.ui.douyin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.qihe.clipflow.data.api.model.ContentItem
import com.qihe.clipflow.data.local.AppDatabase
import com.qihe.clipflow.data.local.HistoryEntity
import com.qihe.clipflow.data.repository.HistoryRepository
import com.qihe.clipflow.data.repository.ParseRepository
import com.qihe.clipflow.util.DownloadManager
import com.qihe.clipflow.util.DownloadState
import com.qihe.clipflow.util.MediaStoreHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DouyinUiState(
    val inputUrl: String = "",
    val isParsing: Boolean = false,
    val parseResult: List<ContentItem>? = null,
    val parseTitle: String = "",
    val parseDesc: String = "",
    val parseCover: String = "",
    val authorName: String = "",
    val authorAvatar: String = "",
    val contentType: String = "",
    val isBackgroundDownload: Boolean = false,
    val error: String? = null,
    val downloadStates: Map<String, DownloadState> = emptyMap(),
    val showDownloadDialog: Boolean = false,
    val downloadingItemId: String? = null
)

class DouyinViewModel(application: Application) : AndroidViewModel(application) {

    private val parseRepository = ParseRepository()
    private val historyRepository = HistoryRepository(
        AppDatabase.getInstance(application).historyDao()
    )
    private val downloadManager = DownloadManager(application)

    private val _uiState = MutableStateFlow(DouyinUiState())
    val uiState: StateFlow<DouyinUiState> = _uiState

    fun onUrlChange(url: String) {
        _uiState.value = _uiState.value.copy(inputUrl = url, error = null)
    }

    fun clearUrl() {
        _uiState.value = _uiState.value.copy(inputUrl = "", error = null)
    }

    fun pasteFromClipboard() {
        val clipboard = getApplication<Application>()
            .getSystemService(Application.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
        if (text.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(inputUrl = text)
        }
    }

    fun parse() {
        val url = _uiState.value.inputUrl.trim()
        if (url.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "请粘贴抖音分享链接")
            return
        }
        if (!url.contains("douyin.com") && !url.contains("iesdouyin.com")) {
            _uiState.value = _uiState.value.copy(error = "请输入有效的抖音链接")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isParsing = true, error = null, parseResult = null)

            val result = parseRepository.parseDouyin(url)

            result.fold(
                onSuccess = { result ->
                    _uiState.value = _uiState.value.copy(
                        isParsing = false,
                        parseResult = result.items,
                        parseTitle = result.title,
                        parseDesc = result.desc,
                        parseCover = result.cover,
                        authorName = result.authorName,
                        authorAvatar = result.authorAvatar,
                        contentType = result.contentType
                    )
                    saveHistory(url, result)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isParsing = false,
                        error = e.message ?: "解析失败"
                    )
                }
            )
        }
    }

    fun downloadItem(item: ContentItem) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                showDownloadDialog = true,
                downloadingItemId = item.id
            )

            val ext = if (item.type.name.contains("VIDEO")) ".mp4" else ".jpg"
            val fileName = "ClipFlow_${System.currentTimeMillis()}$ext"
            val app = getApplication<Application>()

            downloadManager.reset()

            // 监听下载状态
            launch {
                downloadManager.downloadState.collect { state ->
                    val states = _uiState.value.downloadStates.toMutableMap()
                    states[item.id] = state
                    _uiState.value = _uiState.value.copy(downloadStates = states)

                    if (state.isComplete) {
                        _uiState.value = _uiState.value.copy(
                            showDownloadDialog = false,
                            downloadingItemId = null
                        )
                    }
                }
            }

            downloadManager.download(item.url, fileName) { tempFile ->
                val isVideo = item.type == com.qihe.clipflow.data.api.model.ContentType.VIDEO ||
                        item.type == com.qihe.clipflow.data.api.model.ContentType.LIVE_VIDEO
                MediaStoreHelper.saveToGallery(app, tempFile, isVideo)
            }
        }
    }

    fun dismissDownloadDialog(background: Boolean = false) {
        _uiState.value = _uiState.value.copy(
            showDownloadDialog = false,
            downloadingItemId = if (background) _uiState.value.downloadingItemId else null,
            isBackgroundDownload = background
        )
    }

    fun showDialogFromPill() {
        val itemId = _uiState.value.downloadingItemId
        if (itemId != null) {
            _uiState.value = _uiState.value.copy(
                showDownloadDialog = true,
                isBackgroundDownload = false
            )
        } else if (_uiState.value.parseResult?.isNotEmpty() == true) {
            // Reopen with last downloading item
            _uiState.value = _uiState.value.copy(
                showDownloadDialog = true,
                isBackgroundDownload = false,
                downloadingItemId = _uiState.value.parseResult?.firstOrNull()?.id
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private suspend fun saveHistory(url: String, result: com.qihe.clipflow.data.repository.ParseResult) {
        val existing = historyRepository.getByUrl(url)
        if (existing != null) {
            // 同链接只更新时间，不创建新记录
            historyRepository.updateTimestamp(url, System.currentTimeMillis())
        } else {
            historyRepository.insert(
                HistoryEntity(
                    url = url,
                    title = result.title.ifEmpty { "抖音 ${result.contentType}" },
                    platform = "douyin",
                    coverUrl = result.cover.ifEmpty { null },
                    authorName = result.authorName.ifEmpty { null },
                    contentType = result.contentType.ifEmpty { null }
                )
            )
        }
    }
}
