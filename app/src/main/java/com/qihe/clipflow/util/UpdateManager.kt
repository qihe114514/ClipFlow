package com.qihe.clipflow.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
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
    private const val GH_PROXY = "https://gh-proxy.com/"

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
                    downloadUrl = apk.downloadUrl,
                    fileName = apk.name
                )
            )
        }
    }

    private suspend fun fetchRelease(): GitHubRelease? {
        // 直连
        val resp = client.newCall(Request.Builder().url(GITHUB_API).build()).execute()
        if (resp.isSuccessful) {
            val body = resp.body?.string()
            if (body != null) return gson.fromJson(body, GitHubRelease::class.java)
        }
        // 加速站
        val proxyResp = client.newCall(
            Request.Builder().url("${GH_PROXY}$GITHUB_API").build()
        ).execute()
        if (proxyResp.isSuccessful) {
            val body = proxyResp.body?.string()
            if (body != null) return gson.fromJson(body, GitHubRelease::class.java)
        }
        return null
    }

    private fun compareVersion(v1: String, v2: String): Int {
        val p1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val p2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(p1.size, p2.size)) {
            val a = p1.getOrElse(i) { 0 }
            val b = p2.getOrElse(i) { 0 }
            if (a != b) return a - b
        }
        return 0
    }

    fun downloadAndInstall(context: Context, url: String, fileName: String, onComplete: () -> Unit) {
        val proxyUrl = "${GH_PROXY}$url"

        val request = DownloadManager.Request(Uri.parse(proxyUrl))
            .setTitle("ClipFlow 更新下载中")
            .setDescription("正在下载 $fileName")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = dm.enqueue(request)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: -1
                if (id != downloadId) return

                context.unregisterReceiver(this)
                onComplete()

                val file = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    fileName
                )
                if (file.exists()) {
                    installApk(context, file)
                }
            }
        }

        context.registerReceiver(
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            Context.RECEIVER_NOT_EXPORTED
        )
    }

    private fun installApk(context: Context, file: File) {
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
