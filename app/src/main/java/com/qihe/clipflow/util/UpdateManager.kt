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
        val fileName: String,
        /** GitHub Release 提供的 SHA-256（可能为空，为空时不做完整性校验） */
        val sha256: String? = null
    )

    private val client = AppHttp.shared.newBuilder()
        .addInterceptor(HttpRetry.interceptor())
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
                    fileName = apk.name,
                    sha256 = apk.digest
                        ?.takeIf { it.startsWith("sha256:", ignoreCase = true) }
                        ?.substringAfter(':')
                        ?.takeIf { it.length == 64 }
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

    /**
     * 校验下载得到 APK 的 SHA-256 是否与 Release 声明一致。
     * 返回 null 表示 Release 未提供摘要（无法校验），false 表示校验失败。
     */
    suspend fun verifyDownload(file: File, info: UpdateInfo): Boolean? = withContext(Dispatchers.IO) {
        val expected = info.sha256 ?: return@withContext null
        val actual = runCatching { sha256(file) }.getOrNull() ?: return@withContext false
        actual.equals(expected, ignoreCase = true)
    }

    private fun sha256(file: File): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * 摘要 + 签名双重校验：Release 提供 sha256 时必须一致；下载包必须与已安装应用签名一致。
     * 返回 false 时调用方应删除安装包并提示重试。
     */
    suspend fun verifyPackage(context: Context, file: File, info: UpdateInfo): Boolean {
        if (verifyDownload(file, info) == false) return false
        return withContext(Dispatchers.IO) { hasSameSignature(context, file) }
    }

    /** 校验下载 APK 与已安装应用的签名是否一致，防止被替换为第三方签名包。 */
    @Suppress("DEPRECATION")
    fun hasSameSignature(context: Context, apkFile: File): Boolean {
        val packageManager = context.packageManager
        val archiveInfo = runCatching {
            packageManager.getPackageArchiveInfo(
                apkFile.absolutePath,
                android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES,
            )
        }.getOrNull() ?: return false
        val archiveSigners = archiveInfo.signingInfo?.apkContentsSigners.orEmpty()
        val installedInfo = runCatching {
            packageManager.getPackageInfo(
                context.packageName,
                android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES,
            )
        }.getOrNull() ?: return false
        val installedSigners = installedInfo.signingInfo?.apkContentsSigners.orEmpty()
        if (archiveSigners.isEmpty() || installedSigners.isEmpty()) return false
        val archiveBytes = archiveSigners.map { it.toByteArray().toList() }.toSet()
        val installedBytes = installedSigners.map { it.toByteArray().toList() }.toSet()
        return archiveBytes == installedBytes
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
