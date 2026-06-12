package com.douyin.downloader.download

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.douyin.downloader.model.DownloadItem
import com.douyin.downloader.model.DownloadType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class DownloadManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.MINUTES)
        .followRedirects(true)
        .build()

    /**
     * OkHttp 流式下载 -> 写入应用私有临时文件 -> 完成后拷贝到系统相册并返回路径。
     * 通过 Flow 每 500ms emit (进度 0..1, 速度字符串)；完成时 emit null。
     */
    fun downloadWithProgress(
        item: DownloadItem,
        savePath: String?
    ): Flow<Pair<Float, String>?> = flow {
        val extension = when (item.type) {
            DownloadType.VIDEO, DownloadType.LIVE_PHOTO -> ".mp4"
            DownloadType.IMAGE -> ".jpg"
        }
        val safeTitle = item.title.replace(Regex("[/\\\\:*?\"<>|]"), "_").take(80)
        val fileName = "${safeTitle}$extension"

        // 临时目录：应用私有 cache
        val tempDir = File(context.cacheDir, "downloads")
        if (!tempDir.exists()) tempDir.mkdirs()
        val tempFile = File(tempDir, fileName)

        val request = Request.Builder().url(item.url).build()
        val response = withContext(Dispatchers.IO) {
            client.newCall(request).execute()
        }

        if (!response.isSuccessful) {
            throw RuntimeException("下载失败 HTTP ${response.code}")
        }

        val body = response.body ?: throw RuntimeException("空响应体")
        val contentLength = body.contentLength()
        val source = body.source()

        FileOutputStream(tempFile).use { fos ->
            val sink = fos.sink().buffer()
            var totalBytesRead = 0L
            var lastBytes = 0L
            var lastTime = System.currentTimeMillis()

            while (!source.exhausted()) {
                val read = source.read(sink.buffer, 8192)
                if (read == -1L) break
                totalBytesRead += read
                sink.emit()

                val now = System.currentTimeMillis()
                val elapsed = now - lastTime
                if (elapsed >= 500) {
                    val fraction = if (contentLength > 0) (totalBytesRead.toFloat() / contentLength).coerceIn(0f, 1f) else -1f
                    val speedStr = if (elapsed > 0) {
                        val bytesDelta = totalBytesRead - lastBytes
                        val bytesPerSecond = (bytesDelta * 1000L / elapsed)
                        formatSpeed(bytesPerSecond)
                    } else "—"
                    lastBytes = totalBytesRead
                    lastTime = now
                    emit(fraction to speedStr)
                }
            }
            // flush 最后的数据
            sink.flush()
        }

        response.close()

        // 最后一次进度 emit 100%
        if (contentLength > 0) {
            emit(1f to formatSpeed(contentLength / kotlin.math.max(1, ((System.currentTimeMillis() - lastTime) / 1000))))
        }

        // 拷贝到系统相册
        val savedUri = withContext(Dispatchers.IO) {
            moveToGallery(tempFile, fileName, item.type, savePath)
        }

        emit(null) // 完成
    }

    private fun moveToGallery(
        file: File,
        fileName: String,
        type: DownloadType,
        savePath: String?
    ): String? {
        return try {
            if (!file.exists() || file.length() == 0L) return null

            val mime = when (type) {
                DownloadType.VIDEO, DownloadType.LIVE_PHOTO -> "video/mp4"
                DownloadType.IMAGE -> "image/jpeg"
            }
            val cv = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH,
                        if (type == DownloadType.IMAGE) "Pictures/Douyin" else "Movies/Douyin")
                }
            }
            val collection = if (type == DownloadType.IMAGE) MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            else MediaStore.Video.Media.EXTERNAL_CONTENT_URI

            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.insert(collection, cv)
            } else {
                context.contentResolver.insert(collection, cv)
            } ?: return null

            context.contentResolver.openOutputStream(uri)?.use { out ->
                FileInputStream(file).use { fis ->
                    fis.copyTo(out, 8192)
                }
            }

            file.delete()
            uri.toString()
        } catch (e: Exception) {
            file.delete()
            null
        }
    }

    private fun formatSpeed(bytesPerSecond: Long): String = when {
        bytesPerSecond < 1024 -> "$bytesPerSecond B/s"
        bytesPerSecond < 1024 * 1024 -> "${"%.1f".format(bytesPerSecond / 1024.0)} KB/s"
        bytesPerSecond < 1024 * 1024 * 1024 -> "${"%.1f".format(bytesPerSecond / (1024.0 * 1024))} MB/s"
        else -> "${"%.2f".format(bytesPerSecond / (1024.0 * 1024 * 1024))} GB/s"
    }
}