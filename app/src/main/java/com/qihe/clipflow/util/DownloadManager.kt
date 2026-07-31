package com.qihe.clipflow.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import okio.buffer
import okio.sink
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

data class DownloadState(
    val progress: Float = 0f,
    val speedText: String = "",
    val isDownloading: Boolean = false,
    val isComplete: Boolean = false,
    val error: String? = null,
    val totalBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val savedMediaUri: String? = null
)

class DownloadManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val _downloadState = MutableStateFlow(DownloadState())
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    suspend fun downloadWithProgress(
        url: String,
        fileName: String,
        onProgress: (DownloadState) -> Unit
    ): Result<File> {
        emitState(DownloadState(isDownloading = true), onProgress)

        return withContext(Dispatchers.IO) {
            try {
                val tempDir = File(context.cacheDir, "downloads")
                if (!tempDir.exists()) {
                    tempDir.mkdirs()
                }

                val tempFile = File(tempDir, fileName)
                val request = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("ÏÂÔØÊ§°Ü: HTTP ${response.code}")
                    }

                    val body = response.body ?: throw IOException("ÏìÓ¦ÌåÎª¿Õ")
                    val totalBytes = body.contentLength()
                    val buffer = Buffer()
                    var downloadedBytes = 0L
                    var lastUpdateTime = System.currentTimeMillis()
                    var lastBytes = 0L

                    body.source().use { source ->
                        tempFile.sink().buffer().use { sink ->
                            var bytesRead: Long
                            while (source.read(buffer, 8192).also { bytesRead = it } != -1L) {
                                sink.write(buffer, bytesRead)
                                downloadedBytes += bytesRead

                                val now = System.currentTimeMillis()
                                val elapsedMs = (now - lastUpdateTime).coerceAtLeast(1)
                                val speed = (downloadedBytes - lastBytes) * 1000 / elapsedMs

                                if (now - lastUpdateTime >= 200) {
                                    emitState(
                                        DownloadState(
                                            progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f,
                                            speedText = formatSpeed(speed),
                                            isDownloading = true,
                                            totalBytes = totalBytes,
                                            downloadedBytes = downloadedBytes
                                        ),
                                        onProgress
                                    )
                                    lastUpdateTime = now
                                    lastBytes = downloadedBytes
                                }
                            }
                            sink.flush()
                        }
                    }

                    val completedState = DownloadState(
                        progress = 1f,
                        speedText = "",
                        isDownloading = false,
                        isComplete = true,
                        totalBytes = totalBytes,
                        downloadedBytes = downloadedBytes
                    )
                    emitState(completedState, onProgress)
                    Result.success(tempFile)
                }
            } catch (e: Exception) {
                emitState(
                    DownloadState(
                        isDownloading = false,
                        error = e.message ?: "ÏÂÔØÊ§°Ü"
                    ),
                    onProgress
                )
                Result.failure(e)
            }
        }
    }

    suspend fun download(
        url: String,
        fileName: String,
        onComplete: (File) -> Unit
    ) {
        downloadWithProgress(url, fileName) { }.onSuccess(onComplete)
    }

    fun reset() {
        _downloadState.value = DownloadState()
    }

    private fun emitState(state: DownloadState, onProgress: (DownloadState) -> Unit) {
        _downloadState.value = state
        onProgress(state)
    }

    private fun formatSpeed(bytesPerSecond: Long): String {
        return when {
            bytesPerSecond >= 1_000_000 -> "%.1f MB/s".format(bytesPerSecond / 1_000_000.0)
            bytesPerSecond >= 1_000 -> "%.1f KB/s".format(bytesPerSecond / 1_000.0)
            else -> "$bytesPerSecond B/s"
        }
    }
}
