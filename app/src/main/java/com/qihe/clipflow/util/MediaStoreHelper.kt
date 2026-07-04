package com.qihe.clipflow.util

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.qihe.clipflow.data.api.model.ContentType
import java.io.File

/**
 * 将下载的文件写入系统媒体库
 * 视频→Movies/ClipFlow  图片→Pictures/ClipFlow  音频→Music/ClipFlow
 */
object MediaStoreHelper {

    fun saveToGallery(
        context: Context,
        sourceFile: File,
        type: ContentType
    ): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveUsingMediaStore(context, sourceFile, type)
        } else {
            saveUsingLegacy(context, sourceFile, type)
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

        try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                sourceFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)

            return uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            return null
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
        try {
            sourceFile.copyTo(destFile, overwrite = true)
            // 通知系统扫描
            MediaScannerConnection.scanFile(
                context,
                arrayOf(destFile.absolutePath),
                null,
                null
            )
            return Uri.fromFile(destFile)
        } catch (e: Exception) {
            return null
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
