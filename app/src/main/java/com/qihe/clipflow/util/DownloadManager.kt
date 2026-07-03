package com.qihe.clipflow.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.qihe.clipflow.ClipFlowApp
import com.qihe.clipflow.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

data class DownloadState(
    val progress: Float = 0f,       // 0-1
    val speedText: String = "",     // 格式化网速
    val isDownloading: Boolean = false,
    val isComplete: Boolean = false,
    val error: String? = null,
    val totalBytes: Long = 0,
    val downloadedBytes: Long = 0
)

class DownloadManager(private val context: Context) {

    private val _downloadState = MutableStateFlow(DownloadState())
    val downloadState: StateFlow<DownloadState> = _downloadState

    private var notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    /**
     * 下载文件到临时目录，返回文件路径。通过 StateFlow 推送进度。
     */
    suspend fun download(
        url: String,
        fileName: String,
        onComplete: (File) -> Unit
    ) {
        _downloadState.value = DownloadState(isDownloading = true)

        withContext(Dispatchers.IO) {
            try {
                val tempDir = File(context.cacheDir, "downloads")
                if (!tempDir.exists()) tempDir.mkdirs()
                val tempFile = File(tempDir, fileName)

                val request = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IOException("下载失败: HTTP ${response.code}")
                }

                val body = response.body ?: throw IOException("响应体为空")
                val totalBytes = body.contentLength()
                val source = body.source()
                val sink = tempFile.sink().buffer()

                var downloadedBytes = 0L
                var lastUpdateTime = System.currentTimeMillis()
                var lastBytes = 0L

                _downloadState.value = _downloadState.value.copy(totalBytes = totalBytes)

                val buffer = okio.Buffer()
                var bytesRead: Long
                while (source.read(buffer, 8192).also { bytesRead = it } != -1L) {
                    sink.write(buffer, bytesRead)
                    downloadedBytes += bytesRead

                    val now = System.currentTimeMillis()
                    val timeDiff = (now - lastUpdateTime).coerceAtLeast(1)
                    val speed = (downloadedBytes - lastBytes) * 1000 / timeDiff

                    if (now - lastUpdateTime > 200) {
                        _downloadState.value = DownloadState(
                            progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f,
                            speedText = formatSpeed(speed),
                            isDownloading = true,
                            totalBytes = totalBytes,
                            downloadedBytes = downloadedBytes
                        )
                        lastUpdateTime = now
                        lastBytes = downloadedBytes
                    }
                }

                sink.flush()
                sink.close()
                source.close()

                _downloadState.value = DownloadState(
                    progress = 1f,
                    speedText = "",
                    isDownloading = false,
                    isComplete = true,
                    totalBytes = totalBytes,
                    downloadedBytes = downloadedBytes
                )

                onComplete(tempFile)

            } catch (e: Exception) {
                _downloadState.value = DownloadState(
                    isDownloading = false,
                    error = e.message ?: "下载失败"
                )
            }
        }
    }

    fun reset() {
        _downloadState.value = DownloadState()
    }

    private fun formatSpeed(bytesPerSecond: Long): String {
        return when {
            bytesPerSecond >= 1_000_000 -> "%.1f MB/s".format(bytesPerSecond / 1_000_000.0)
            bytesPerSecond >= 1_000 -> "%.1f KB/s".format(bytesPerSecond / 1_000.0)
            else -> "$bytesPerSecond B/s"
        }
    }
}
