package com.qihe.clipflow.util

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.qihe.clipflow.data.api.model.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 将下载的文件写入用户选择的自定义目录（SAF）或系统媒体库。
 * 视频→Movies/ClipFlow  图片→Pictures/ClipFlow  音频→Music/ClipFlow
 */
object MediaStoreHelper {

    data class SaveOutcome(
        val uri: Uri? = null,
        val error: String? = null
    ) {
        val isSuccess: Boolean get() = uri != null
    }

    /**
     * 保存结果：uri 非空表示成功；否则 error 说明原因。
     * customTreeUri 非空时只写自定义目录，失败不会回退到媒体库（避免“设置无效”的静默行为）。
     */
    suspend fun saveToGallery(
        context: Context,
        sourceFile: File,
        type: ContentType,
        customTreeUri: String? = null
    ): SaveOutcome = withContext(Dispatchers.IO) {
        if (!customTreeUri.isNullOrBlank()) {
            return@withContext saveToCustomTree(context, sourceFile, customTreeUri)
        }
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveUsingMediaStore(context, sourceFile, type)
        } else {
            saveUsingLegacy(context, sourceFile, type)
        }
        if (uri != null) SaveOutcome(uri = uri) else SaveOutcome(error = "保存到系统相册失败")
    }

    private fun saveToCustomTree(
        context: Context,
        sourceFile: File,
        treeUri: String
    ): SaveOutcome {
        val tree = runCatching { DocumentFile.fromTreeUri(context, treeUri.toUri()) }.getOrNull()
            ?: return SaveOutcome(error = "自定义保存目录不可用，请在设置中重新选择")
        if (!tree.exists()) {
            return SaveOutcome(error = "自定义保存目录已不存在，请在设置中重新选择")
        }
        if (!tree.canWrite()) {
            return SaveOutcome(error = "自定义保存目录没有写入权限")
        }
        val mimeType = getMimeType(sourceFile.name)
        val target = tree.findFile(sourceFile.name)?.takeIf { it.isFile }
            ?: runCatching { tree.createFile(mimeType, sourceFile.name) }.getOrNull()
        if (target == null) {
            return SaveOutcome(error = "无法在自定义目录创建文件")
        }
        return try {
            context.contentResolver.openOutputStream(target.uri)?.use { output ->
                sourceFile.inputStream().use { input -> input.copyTo(output) }
            } ?: run {
                runCatching { target.delete() }
                return SaveOutcome(error = "无法写入自定义目录")
            }
            SaveOutcome(uri = target.uri)
        } catch (e: Exception) {
            runCatching { target.delete() }
            SaveOutcome(error = e.message ?: "写入自定义目录失败")
        }
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
    private fun saveUsingMediaStore(
        context: Context,
        sourceFile: File,
        type: ContentType
    ): Uri? {
        val isAudio = type == ContentType.AUDIO
        val contentValues = ContentValues().apply {
            when {
                isAudio -> {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, sourceFile.name)
                    put(MediaStore.Audio.Media.MIME_TYPE, getMimeType(sourceFile.name))
                    put(
                        MediaStore.Audio.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_MUSIC}/ClipFlow"
                    )
                }
                type == ContentType.VIDEO || type == ContentType.LIVE_VIDEO -> {
                    put(MediaStore.Video.Media.DISPLAY_NAME, sourceFile.name)
                    put(MediaStore.Video.Media.MIME_TYPE, getMimeType(sourceFile.name))
                    put(
                        MediaStore.Video.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_MOVIES}/ClipFlow"
                    )
                }
                else -> {
                    put(MediaStore.Images.Media.DISPLAY_NAME, sourceFile.name)
                    put(MediaStore.Images.Media.MIME_TYPE, getMimeType(sourceFile.name))
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/ClipFlow"
                    )
                }
            }
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val collection = when {
            isAudio -> MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            type == ContentType.VIDEO || type == ContentType.LIVE_VIDEO ->
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            else ->
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }

        val uri = resolver.insert(collection, contentValues) ?: return null

        return try {
            val output = resolver.openOutputStream(uri)
                ?: run {
                    resolver.delete(uri, null, null)
                    return null
                }
            output.use { outputStream ->
                sourceFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
            uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }

    private fun saveUsingLegacy(
        context: Context,
        sourceFile: File,
        type: ContentType
    ): Uri? {
        val isAudio = type == ContentType.AUDIO
        val dirType = when {
            isAudio -> Environment.DIRECTORY_MUSIC
            type == ContentType.VIDEO || type == ContentType.LIVE_VIDEO -> Environment.DIRECTORY_MOVIES
            else -> Environment.DIRECTORY_PICTURES
        }

        val dir = File(
            Environment.getExternalStoragePublicDirectory(dirType),
            "ClipFlow"
        )
        if (!dir.exists()) dir.mkdirs()

        val destFile = File(dir, sourceFile.name)
        return try {
            sourceFile.copyTo(destFile, overwrite = true)
            MediaScannerConnection.scanFile(
                context,
                arrayOf(destFile.absolutePath),
                null,
                null
            )
            Uri.fromFile(destFile)
        } catch (e: Exception) {
            null
        }
    }

    private fun getMimeType(fileName: String): String {
        return when {
            // 音频
            fileName.endsWith(".mp3", ignoreCase = true) -> "audio/mpeg"
            fileName.endsWith(".aac", ignoreCase = true) -> "audio/aac"
            fileName.endsWith(".m4a", ignoreCase = true) -> "audio/mp4"
            fileName.endsWith(".wav", ignoreCase = true) -> "audio/wav"
            fileName.endsWith(".ogg", ignoreCase = true) -> "audio/ogg"
            // 视频
            fileName.endsWith(".mp4", ignoreCase = true) -> "video/mp4"
            fileName.endsWith(".mov", ignoreCase = true) -> "video/quicktime"
            fileName.endsWith(".mkv", ignoreCase = true) -> "video/x-matroska"
            fileName.endsWith(".avi", ignoreCase = true) -> "video/x-msvideo"
            fileName.endsWith(".webm", ignoreCase = true) -> "video/webm"
            fileName.endsWith(".ts", ignoreCase = true) -> "video/mp2ts"
            // 图片
            fileName.endsWith(".webp", ignoreCase = true) -> "image/webp"
            fileName.endsWith(".png", ignoreCase = true) -> "image/png"
            fileName.endsWith(".gif", ignoreCase = true) -> "image/gif"
            fileName.endsWith(".jpg", ignoreCase = true) -> "image/jpeg"
            fileName.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
            else -> "image/jpeg"
        }
    }
}
