package com.qihe.clipflow.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CancellationException
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

object DownloadProgress {
    fun fraction(downloadedBytes: Long, totalBytes: Long): Float {
        if (totalBytes <= 0L) return 0f
        return (downloadedBytes.toDouble() / totalBytes.toDouble()).toFloat().coerceIn(0f, 1f)
    }

    fun clamp(value: Float): Float = value.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
}

class DownloadManager(private val context: Context) {
    private companion object { const val TAG = "ClipFlowDownload" }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val _downloadState = MutableStateFlow(DownloadState())
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    suspend fun downloadWithProgress(
        url: String,
        fileName: String,
        onProgress: (DownloadState) -> Unit,
        requestHeaders: Map<String, String> = emptyMap()
    ): Result<File> = downloadWithProgress(listOf(url), fileName, onProgress, requestHeaders)

    suspend fun downloadWithProgress(
        urls: List<String>,
        fileName: String,
        onProgress: (DownloadState) -> Unit,
        requestHeaders: Map<String, String> = emptyMap()
    ): Result<File> {
        emitState(DownloadState(isDownloading = true), onProgress)

        return withContext(Dispatchers.IO) {
            val tempDir = File(context.cacheDir, "downloads").apply { mkdirs() }
            val tempFile = File(tempDir, fileName)
            var lastError: Exception? = null

            val candidates = urls.filter { it.isNotBlank() }.distinct()
            for ((attempt, url) in candidates.withIndex()) {
                try {
                    tempFile.delete()
                    val requestBuilder = Request.Builder()
                        .url(url)
                        .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                    requestHeaders.forEach { (name, value) -> requestBuilder.header(name, value) }

                    val host = runCatching { java.net.URI(url).host ?: "unknown" }.getOrDefault("unknown")
                    Log.i(TAG, "download attempt=${attempt + 1}/${candidates.size} host=$host")
                    client.newCall(requestBuilder.build()).execute().use { response ->
                        if (!response.isSuccessful) {
                            Log.w(TAG, "download rejected host=" + host + " code=" + response.code)
                            throw IOException("下载失败: HTTP ${response.code}")
                        }
                        val body = response.body ?: throw IOException("响应体为空")
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
                                    if (now - lastUpdateTime >= 200) {
                                        val elapsedMs = (now - lastUpdateTime).coerceAtLeast(1)
                                        val speed = (downloadedBytes - lastBytes) * 1000 / elapsedMs
                                        emitState(
                                            DownloadState(
                                                progress = DownloadProgress.fraction(downloadedBytes, totalBytes),
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
                            }
                        }

                        emitState(
                            DownloadState(
                                progress = 1f,
                                isDownloading = false,
                                isComplete = true,
                                totalBytes = totalBytes,
                                downloadedBytes = downloadedBytes
                            ),
                            onProgress
                        )
                        Log.i(TAG, "download complete host=$host bytes=$downloadedBytes")
                        return@withContext Result.success(tempFile)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "download attempt failed type=${e::class.java.simpleName}")
                    lastError = e
                }
            }

            tempFile.delete()
            val error = lastError ?: IOException("下载地址为空")
            emitState(DownloadState(isDownloading = false, error = error.message ?: "下载失败"), onProgress)
            Result.failure(error)
        }
    }
    suspend fun download(
        url: String,
        fileName: String,
        onComplete: (File) -> Unit
    ) {
        downloadWithProgress(url, fileName, onProgress = {}).onSuccess(onComplete)
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
