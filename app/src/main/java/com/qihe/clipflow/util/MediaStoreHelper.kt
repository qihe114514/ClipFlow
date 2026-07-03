package com.qihe.clipflow.util

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/**
 * 将下载的文件写入系统相册
 * 视频 → Movies/ClipFlow
 * 图片 → Pictures/ClipFlow
 */
object MediaStoreHelper {

    fun saveToGallery(
        context: Context,
        sourceFile: File,
        isVideo: Boolean
    ): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveUsingMediaStore(context, sourceFile, isVideo)
        } else {
            saveUsingLegacy(context, sourceFile, isVideo)
        }
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
    private fun saveUsingMediaStore(
        context: Context,
        sourceFile: File,
        isVideo: Boolean
    ): Uri? {
        val contentValues = ContentValues().apply {
            if (isVideo) {
                put(MediaStore.Video.Media.DISPLAY_NAME, sourceFile.name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(
                    MediaStore.Video.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_MOVIES}/ClipFlow"
                )
            } else {
                put(MediaStore.Images.Media.DISPLAY_NAME, sourceFile.name)
                put(MediaStore.Images.Media.MIME_TYPE, getMimeType(sourceFile.name))
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/ClipFlow"
                )
            }
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val collection = if (isVideo) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
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
        isVideo: Boolean
    ): Uri? {
        val dirType = if (isVideo) {
            Environment.DIRECTORY_MOVIES
        } else {
            Environment.DIRECTORY_PICTURES
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
            fileName.endsWith(".webp", ignoreCase = true) -> "image/webp"
            fileName.endsWith(".png", ignoreCase = true) -> "image/png"
            fileName.endsWith(".gif", ignoreCase = true) -> "image/gif"
            else -> "image/jpeg"
        }
    }
}
