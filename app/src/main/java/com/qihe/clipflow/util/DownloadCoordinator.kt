package com.qihe.clipflow.util

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.qihe.clipflow.data.api.model.ContentItem
import com.qihe.clipflow.data.api.model.ContentType
import com.qihe.clipflow.data.preferences.AppPreferences
import com.qihe.clipflow.data.preferences.destinationFlowOf
import com.qihe.clipflow.data.preferences.destinationKindOf
import com.qihe.clipflow.ui.components.DownloadPillState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

data class DownloadSessionState(
    val downloadStates: Map<String, DownloadState> = emptyMap(),
    val showDownloadDialog: Boolean = false,
    val downloadingItemId: String? = null,
    val isBackgroundDownload: Boolean = false
)

class DownloadCoordinator(
    context: Context,
    private val sourceRoute: String,
    private val scope: CoroutineScope
) {

    private val appContext = context.applicationContext
    private val downloadManager = DownloadManager(appContext)
    private val jobs = ConcurrentHashMap<String, Job>()
    private val jobTitles = ConcurrentHashMap<String, String>()

    private val _session = MutableStateFlow(DownloadSessionState())
    val session: StateFlow<DownloadSessionState> = _session.asStateFlow()

    fun startDownload(item: ContentItem) {
        val fileName = buildFileName(item)
        val title = item.description.ifEmpty { fileName }
        jobTitles[item.id] = title
        _session.update {
            it.copy(
                showDownloadDialog = true,
                downloadingItemId = item.id,
                isBackgroundDownload = false
            )
        }
        DownloadPillState.hide()
        DownloadSessionTracker.begin(item.id)
        DownloadForegroundService.start(appContext)
        DownloadSessionTracker.update(0f, "")

        jobs[item.id] = scope.launch {
            try {
                val result = downloadManager.downloadWithProgress(item.url, fileName, onProgress = { state ->
                    updateItemState(item.id, state)
                })

                result.onSuccess { tempFile ->
                    // 保存包含大文件复制，MediaStoreHelper 内部已切到 IO；
                    // 自定义目录失败时明确报错，不回退系统默认目录。
                    val customTree = AppPreferences(appContext)
                        .destinationFlowOf(destinationKindOf(item.type))
                        .first()
                    val outcome = MediaStoreHelper.saveToGallery(appContext, tempFile, item.type, customTree)
                    if (outcome.isSuccess) {
                        updateItemState(
                            item.id,
                            latestState(item.id).copy(savedMediaUri = outcome.uri.toString())
                        )
                    } else {
                        updateItemState(
                            item.id,
                            latestState(item.id).copy(
                                isComplete = false,
                                error = outcome.error ?: "保存到相册失败"
                            )
                        )
                    }
                    tempFile.delete()
                }
            } finally {
                // 协程被取消（重复下载/页面销毁）时也要释放前台服务计数，避免服务常驻。
                DownloadSessionTracker.finish(item.id)
            }
        }
    }

    /** 取消下载并丢弃临时文件/进度，不写入媒体库。 */
    fun cancel(itemId: String?) {
        val id = itemId ?: return
        jobs.remove(id)?.cancel()
        jobTitles.remove(id)
        DownloadSessionTracker.finish(id)
        DownloadNotifier.cancel(appContext, id)
        _session.update { current ->
            current.copy(
                downloadStates = current.downloadStates - id,
                showDownloadDialog = false,
                downloadingItemId = null,
                isBackgroundDownload = false
            )
        }
        DownloadPillState.hide()
    }

    fun dismiss(background: Boolean) {
        val itemId = _session.value.downloadingItemId
        val activeState = itemId?.let(::latestState)

        _session.update {
            it.copy(
                showDownloadDialog = false,
                downloadingItemId = if (background) it.downloadingItemId else null,
                isBackgroundDownload = background && activeState?.isDownloading == true
            )
        }

        if (background && itemId != null && activeState?.isDownloading == true) {
            DownloadPillState.sourceRoute = sourceRoute
            DownloadPillState.show(activeState.progress, activeState.speedText) {
                showDialogFromPill()
            }
        } else {
            DownloadPillState.hide()
        }
    }

    fun showDialogFromPill() {
        DownloadPillState.hide()
        _session.update {
            if (it.downloadingItemId == null) {
                it
            } else {
                it.copy(
                    showDownloadDialog = true,
                    isBackgroundDownload = false
                )
            }
        }
    }

    @VisibleForTesting
    internal fun buildFileName(item: ContentItem): String {
        val format = item.mediaInfo?.format
        val extension = when {
            item.type == ContentType.AUDIO -> ".mp3"
            format != null -> {
                val normalized = format
                    .split("/")
                    .firstOrNull()
                    ?.trim()
                    ?.lowercase()
                    .orEmpty()
                when (normalized) {
                    "jpeg" -> ".jpg"
                    "webp", "jpg", "png", "gif", "mp4", "mp3", "mov", "mkv", "avi", "webm", "aac", "m4a", "wav" -> ".$normalized"
                    else -> fallbackExtension(item.type)
                }
            }

            else -> fallbackExtension(item.type)
        }

        return "ClipFlow_${System.currentTimeMillis()}$extension"
    }

    private fun updateItemState(itemId: String, state: DownloadState) {
        val title = jobTitles[itemId] ?: "下载"
        when {
            state.isDownloading -> DownloadSessionTracker.update(state.progress, state.speedText)
            state.isComplete -> {
                DownloadSessionTracker.finish(itemId)
                DownloadNotifier.complete(appContext, itemId, title)
                jobTitles.remove(itemId)
            }
            state.error != null -> {
                DownloadSessionTracker.finish(itemId)
                DownloadNotifier.cancel(appContext, itemId)
                jobTitles.remove(itemId)
            }
        }

        _session.update { current ->
            val nextStates = current.downloadStates.toMutableMap()
            nextStates[itemId] = state

            val isCurrentItem = current.downloadingItemId == itemId
            if (current.isBackgroundDownload && isCurrentItem && state.isDownloading) {
                DownloadPillState.update(state.progress, state.speedText)
            } else if (isCurrentItem && (state.isComplete || state.error != null)) {
                jobs.remove(itemId)
                DownloadPillState.hide()
            }

            current.copy(
                downloadStates = nextStates,
                isBackgroundDownload = if (isCurrentItem && !state.isDownloading) false else current.isBackgroundDownload
            )
        }
    }

    private fun latestState(itemId: String): DownloadState {
        return _session.value.downloadStates[itemId] ?: DownloadState()
    }

    private fun fallbackExtension(type: ContentType): String {
        return when (type) {
            ContentType.AUDIO -> ".mp3"
            ContentType.VIDEO, ContentType.LIVE_VIDEO -> ".mp4"
            else -> ".jpg"
        }
    }
}
