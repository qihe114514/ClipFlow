package com.douyin.downloader.download

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.provider.MediaStore
import com.douyin.downloader.model.DownloadItem
import com.douyin.downloader.model.DownloadType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.TimeUnit

class DownloadManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun downloadToGalleryWithProgress(
        item: DownloadItem,
        savePath: String?,
        onProgress: (progress: Float, speed: String) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        var tmpFile: File? = null
        try {
            val request = Request.Builder()
                .url(item.url)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 16; Pixel) AppleWebKit/537.36")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))
            val body = response.body ?: return@withContext Result.failure(Exception("响应体为空"))
            val contentLen = body.contentLength().coerceAtLeast(0L)

            val extension = when (item.type) {
                DownloadType.VIDEO, DownloadType.LIVE_PHOTO -> ".mp4"
                DownloadType.IMAGE -> ".jpg"
            }
            val safeTitle = item.title.replace(Regex("[/\\\\:*?\"<>|]"), "_").take(80)
            val fileName = "${safeTitle}$extension"

            // 1) 先存本地临时文件
            tmpFile = File(context.cacheDir, "dl_${System.currentTimeMillis()}")
            var totalRead = 0L
            var lastTs = System.currentTimeMillis()
            var lastBytes = 0L
            val buffer = okio.Buffer()

            tmpFile.outputStream().buffered().use { fos ->
                val source = body.source()
                while (!source.exhausted()) {
                    val chunk = source.read(buffer, 65536)
                    if (chunk == -1L) break
                    buffer.readAll(fos.sink().buffer())
                    totalRead += chunk
                    val now = System.currentTimeMillis()
                    if (now - lastTs >= 300 || totalRead == contentLen) {
                        val dt = (now - lastTs).coerceAtLeast(1L)
                        val speed = formatSpeed((totalRead - lastBytes).toDouble() * 1000.0 / dt)
                        val progress = if (contentLen > 0) (totalRead.toFloat() / contentLen).coerceIn(0f, 1f) else -1f
                        onProgress(progress, speed)
                        lastTs = now
                        lastBytes = totalRead
                    }
                }
            }

            // 2) 拷贝到系统相册
            val mime = when (item.type) {
                DownloadType.VIDEO, DownloadType.LIVE_PHOTO -> "video/mp4"
                DownloadType.IMAGE -> "image/jpeg"
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val cv = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mime)
                    if (item.type == DownloadType.IMAGE) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Douyin")
                    } else {
                        put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Douyin")
                    }
                }
                val collection = if (item.type == DownloadType.IMAGE) MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                                else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                val uri = context.contentResolver.insert(collection, cv) ?: throw Exception("创建媒体条目失败")
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    val buf = ByteArray(8192)
                    FileInputStream(tmpFile).use { fis ->
                        var read: Int
                        while (fis.read(buf).also { read = it } != -1) {
                            out.write(buf, 0, read)
                        }
                    }
                } ?: throw Exception("无法打开输出流")
                Result.success(uri.toString())
            } else {
                val dir = if (!savePath.isNullOrEmpty() && File(savePath).exists()) File(savePath)
                          else File(context.getExternalFilesDir(null), "DouyinDownloads").also { it.mkdirs() }
                val dest = File(dir, fileName)
                tmpFile.copyTo(dest, overwrite = true)
                MediaScannerConnection.scanFile(context, arrayOf(dest.absolutePath), null) { _, _ -> }
                Result.success(dest.absolutePath)
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            tmpFile?.delete()
        }
    }

    private fun formatSpeed(bytesPerSec: Double): String = when {
        bytesPerSec < 1024 -> "${bytesPerSec.toLong()} B/s"
        bytesPerSec < 1024 * 1024 -> "${"%.1f".format(bytesPerSec / 1024)} KB/s"
        bytesPerSec < 1024 * 1024 * 1024 -> "${"%.1f".format(bytesPerSec / (1024 * 1024))} MB/s"
        else -> "${"%.2f".format(bytesPerSec / (1024 * 1024 * 1024))} GB/s"
    }
}