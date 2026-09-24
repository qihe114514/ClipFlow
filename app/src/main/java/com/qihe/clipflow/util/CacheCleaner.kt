package com.qihe.clipflow.util

import android.content.Context
import java.io.File

/**
 * 下载临时文件清理（cacheDir/downloads）。
 * 有下载任务进行中时拒绝清理，避免删掉正在写入的文件。
 */
object CacheCleaner {

    private fun downloadTempDir(context: Context): File = File(context.cacheDir, "downloads")

    fun sizeBytes(context: Context): Long =
        downloadTempDir(context).walkBottomUp().filter { it.isFile }.sumOf { it.length() }

    /** 返回释放的字节数；有下载进行中时返回 null 表示拒绝清理。 */
    fun clear(context: Context): Long? {
        if (DownloadSessionTracker.activeCount.value > 0) return null
        val dir = downloadTempDir(context)
        val freed = sizeBytes(context)
        dir.listFiles()?.forEach { file ->
            runCatching { file.deleteRecursively() }
        }
        return freed
    }

    fun formatSize(bytes: Long): String = when {
        bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
        bytes >= 1_000 -> "%.1f KB".format(bytes / 1_000.0)
        else -> "$bytes B"
    }
}
