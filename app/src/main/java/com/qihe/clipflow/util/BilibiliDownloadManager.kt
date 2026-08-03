package com.qihe.clipflow.util

import android.content.Context
import android.util.Log
import com.qihe.clipflow.data.api.model.ContentType
import com.qihe.clipflow.data.bilibili.BilibiliSessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class BilibiliDownloadManager(private val context: Context) {
    private companion object { const val TAG = "ClipFlowDownload" }
    private val downloader = DownloadManager(context)

    suspend fun download(
        videoUrls: List<String>,
        audioUrls: List<String>,
        title: String,
        onProgress: (DownloadState) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        val safeName = title.replace(Regex("""[\\/:*?"<>|]"""), "_").take(80).ifBlank { "Bilibili" }
        val headers = buildMap {
            put("Referer", "https://www.bilibili.com")
            put("User-Agent", "Mozilla/5.0")
            put("Accept", "*/*")
            put("Range", "bytes=0-")
            put("Accept-Encoding", "gzip, deflate")
            BilibiliSessionStore.session()?.cookie?.takeIf { it.isNotBlank() }?.let { put("Cookie", it) }
        }
        Log.i(TAG, "bilibili request headers cookie=" + headers.containsKey("Cookie") + " referer=" + headers.containsKey("Referer"))
        var videoState = DownloadState(isDownloading = true)
        var audioState = DownloadState()
        fun emitCombined() {
            val audioStarted = audioState.isDownloading || audioState.isComplete || audioState.error != null || audioState.totalBytes > 0
            val videoProgress = videoState.progress.coerceIn(0f, 1f)
            val audioProgress = audioState.progress.coerceIn(0f, 1f)
            val progress = (if (audioStarted) 0.5f + audioProgress * 0.5f else videoProgress * 0.5f)
                .coerceAtMost(0.99f)
            val total = if (videoState.totalBytes > 0 && audioState.totalBytes > 0) {
                videoState.totalBytes + audioState.totalBytes
            } else {
                0L
            }
            val downloaded = if (total > 0L) {
                videoState.downloadedBytes + audioState.downloadedBytes
            } else {
                0L
            }
            onProgress(
                DownloadState(
                    progress = progress.coerceIn(0f, 1f),
                    speedText = audioState.speedText.ifBlank { videoState.speedText },
                    isDownloading = true,
                    totalBytes = total,
                    downloadedBytes = downloaded
                )
            )
        }
        fun emitFailure(error: Throwable) {
            onProgress(DownloadState(isDownloading = false, error = error.message ?: "下载失败"))
        }

        Log.i(TAG, "bilibili video download candidates=" + videoUrls.size)
        val videoResult = downloader.downloadWithProgress(videoUrls, safeName + "_video.m4s", onProgress = {
            videoState = it
            emitCombined()
        }, requestHeaders = headers)
        val video = videoResult.getOrElse {
            emitFailure(it)
            return@withContext Result.failure(it)
        }

        Log.i(TAG, "bilibili audio download candidates=" + audioUrls.size)
        val audioResult = downloader.downloadWithProgress(audioUrls, safeName + "_audio.m4s", onProgress = {
            audioState = it
            emitCombined()
        }, requestHeaders = headers)
        val audio = audioResult.getOrElse {
            video.delete()
            emitFailure(it)
            return@withContext Result.failure(it)
        }

        val output = File(context.cacheDir, "downloads/" + safeName + ".mp4")
        try {
            Log.i(TAG, "bilibili mux start")
            return@withContext DashMuxer.mux(video, audio, output).fold(
                onSuccess = { file ->
                    val uri = MediaStoreHelper.saveToGallery(context, file, ContentType.VIDEO)
                    if (uri == null) {
                        val error = IllegalStateException("保存视频失败")
                        emitFailure(error)
                        return@fold Result.failure(error)
                    }
                    onProgress(
                        DownloadState(
                            progress = 1f,
                            isComplete = true,
                            totalBytes = videoState.totalBytes + audioState.totalBytes,
                            downloadedBytes = videoState.downloadedBytes + audioState.downloadedBytes
                        )
                    )
                    Result.success(uri.toString())
                },
                onFailure = {
                    emitFailure(it)
                    Log.e(TAG, "bilibili mux failed type=" + it::class.java.simpleName)
                    Result.failure(it)
                }
            )
        } finally {
            video.delete()
            audio.delete()
            output.delete()
        }
    }
}
