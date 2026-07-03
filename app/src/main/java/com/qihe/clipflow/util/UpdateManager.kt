package com.qihe.clipflow.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.qihe.clipflow.data.api.model.GitHubRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

object UpdateManager {

    private const val REPO = "qihe114514/ClipFlow"
    private const val GITHUB_API = "https://api.github.com/repos/$REPO/releases/latest"
    const val GH_PROXY = "https://gh-proxy.com/"

    data class UpdateInfo(
        val latestVersion: String,
        val releaseNotes: String,
        val downloadUrl: String,
        val fileName: String
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    /**
     * 检查更新。返回:
     * - Result.success(UpdateInfo): 有新版本
     * - Result.success(null): 已是最新
     * - Result.failure(...): 检查失败
     */
    suspend fun checkUpdate(localVersion: String): Result<UpdateInfo?> {
        return withContext(Dispatchers.IO) {
            val release = fetchRelease()
                ?: return@withContext Result.failure(Exception("无法获取更新信息"))

            val latest = release.tagName.removePrefix("v").removePrefix("V")
            if (compareVersion(latest, localVersion) <= 0) {
                return@withContext Result.success(null)
            }

            val apk = release.assets.find { it.name.endsWith(".apk") }
                ?: return@withContext Result.failure(Exception("未找到 APK 文件"))

            Result.success(
                UpdateInfo(
                    latestVersion = latest,
                    releaseNotes = release.body.ifEmpty { release.name },
                    downloadUrl = "${GH_PROXY}${apk.downloadUrl}",
                    fileName = apk.name
                )
            )
        }
    }

    private suspend fun fetchRelease(): GitHubRelease? {
        val resp = client.newCall(Request.Builder().url(GITHUB_API).build()).execute()
        if (resp.isSuccessful) {
            val body = resp.body?.string()
            if (body != null) return gson.fromJson(body, GitHubRelease::class.java)
        }
        val proxyResp = client.newCall(
            Request.Builder().url("${GH_PROXY}$GITHUB_API").build()
        ).execute()
        if (proxyResp.isSuccessful) {
            val body = proxyResp.body?.string()
            if (body != null) return gson.fromJson(body, GitHubRelease::class.java)
        }
        return null
    }

    fun compareVersion(v1: String, v2: String): Int {
        val p1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val p2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(p1.size, p2.size)) {
            val a = p1.getOrElse(i) { 0 }
            val b = p2.getOrElse(i) { 0 }
            if (a != b) return a - b
        }
        return 0
    }

    /** 通过 FileProvider 安装 APK */
    fun installApk(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
