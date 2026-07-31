package com.qihe.clipflow.util

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.qihe.clipflow.data.api.model.ContentItem
import com.qihe.clipflow.data.api.model.ContentType
import com.qihe.clipflow.ui.components.DownloadPillState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    private val jobs = mutableMapOf<String, Job>()

    private val _session = MutableStateFlow(DownloadSessionState())
    val session: StateFlow<DownloadSessionState> = _session.asStateFlow()

    fun startDownload(item: ContentItem) {
        val fileName = buildFileName(item)
        _session.update {
            it.copy(
                showDownloadDialog = true,
                downloadingItemId = item.id,
                isBackgroundDownload = false
            )
        }
        DownloadPillState.hide()

        jobs[item.id]?.cancel()
        jobs[item.id] = scope.launch {
            val result = downloadManager.downloadWithProgress(item.url, fileName) { state ->
                updateItemState(item.id, state)
            }

            result.onSuccess { tempFile ->
                val savedUri = MediaStoreHelper.saveToGallery(appContext, tempFile, item.type)
                if (savedUri != null) {
                    updateItemState(
                        item.id,
                        latestState(item.id).copy(savedMediaUri = savedUri.toString())
                    )
                } else {
                    updateItemState(
                        item.id,
                        latestState(item.id).copy(
                            isComplete = false,
                            error = "±£´æµ½Ïà²áÊ§°Ü"
                        )
                    )
                }
                tempFile.delete()
            }
        }
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
        _session.update { current ->
            val nextStates = current.downloadStates.toMutableMap()
            nextStates[itemId] = state

            val isCurrentItem = current.downloadingItemId == itemId
            if (current.isBackgroundDownload && isCurrentItem && state.isDownloading) {
                DownloadPillState.update(state.progress, state.speedText)
            } else if (isCurrentItem && (state.isComplete || state.error != null)) {
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

