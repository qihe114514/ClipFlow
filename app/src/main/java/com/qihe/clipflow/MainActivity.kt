package com.qihe.clipflow

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.qihe.clipflow.data.preferences.AppPreferences
import com.qihe.clipflow.navigation.ClipFlowNavHost
import com.qihe.clipflow.ui.components.DialogDangerButton
import com.qihe.clipflow.ui.components.DialogPrimaryButton
import com.qihe.clipflow.ui.components.DialogSecondaryButton
import com.qihe.clipflow.ui.components.GlassDialogActions
import com.qihe.clipflow.ui.components.GlassDialogCard
import com.qihe.clipflow.ui.icons.ClipFlowIcons
import com.qihe.clipflow.ui.theme.ClipFlowTheme
import com.qihe.clipflow.ui.theme.ThemeMode
import com.qihe.clipflow.ui.theme.resolveDarkTheme
import com.qihe.clipflow.util.DownloadManager
import com.qihe.clipflow.util.IncomingShare
import com.qihe.clipflow.util.UpdateManager
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        splashScreen.setKeepOnScreenCondition { false }
        handleShareIntent(intent)

        setContent {
            val preferences = remember { AppPreferences(applicationContext) }
            val themeModeKey by preferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM.key)
            val dynamicColor by preferences.dynamicColor.collectAsState(initial = true)
            val isSystemDark = isSystemInDarkTheme()
            val privacyAgreed by preferences.privacyAgreed.collectAsState(initial = false)
            ClipFlowTheme(
                darkTheme = resolveDarkTheme(ThemeMode.fromKey(themeModeKey), isSystemDark),
                dynamicColor = dynamicColor,
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // 合规：同意隐私政策前不发起任何网络请求（含更新检查）。
                    if (privacyAgreed) StartupUpdateCheck()
                    ClipFlowNavHost()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    /** 接收系统分享的文本（抖音/小红书/B 站链接），交给导航层决定跳转与解析。 */
    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            IncomingShare.publish(intent.getStringExtra(Intent.EXTRA_TEXT))
        }
    }
}

@androidx.compose.runtime.Composable
private fun StartupUpdateCheck() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val downloadManager = remember { DownloadManager(appContext) }
    val downloadState by downloadManager.downloadState.collectAsState()
    var updateInfo by remember { mutableStateOf<UpdateManager.UpdateInfo?>(null) }
    var downloadedApk by remember { mutableStateOf<File?>(null) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var downloadJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    LaunchedEffect(Unit) {
        try {
            UpdateManager.checkUpdate(BuildConfig.VERSION_NAME).onSuccess { info ->
                if (info != null) updateInfo = info
            }
        } catch (_: Exception) {
            // Startup update checks must not interrupt normal app use.
        }
    }

    updateInfo?.let { info ->
        Dialog(
            onDismissRequest = {
                if (!downloadState.isDownloading) updateInfo = null
            }
        ) {
            GlassDialogCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(
                        imageVector = ClipFlowIcons.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "发现新版本 v${info.latestVersion}",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
                if (info.releaseNotes.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = info.releaseNotes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 7,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
                downloadError?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (downloadState.isDownloading) {
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { downloadState.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(999.dp)),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${(downloadState.progress * 100).toInt()}%  ${downloadState.speedText}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (downloadedApk != null) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "下载完成，可以安装更新",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(22.dp))
                GlassDialogActions {
                    when {
                        downloadedApk != null -> {
                            DialogSecondaryButton(
                                text = "稍后",
                                onClick = { updateInfo = null },
                            )
                            DialogPrimaryButton(
                                text = "安装更新",
                                onClick = {
                                    downloadedApk?.let { file ->
                                        if (context.packageManager.canRequestPackageInstalls()) {
                                            UpdateManager.installApk(context, file)
                                        } else {
                                            context.startActivity(
                                                Intent(
                                                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                                    Uri.parse("package:${context.packageName}")
                                                )
                                            )
                                        }
                                    }
                                },
                            )
                        }
                        downloadState.isDownloading -> {
                            DialogDangerButton(
                                text = "取消下载",
                                onClick = {
                                    downloadJob?.cancel()
                                    downloadJob = null
                                    downloadManager.reset()
                                    downloadedApk = null
                                    downloadError = "已取消下载"
                                },
                            )
                        }
                        else -> {
                            DialogSecondaryButton(
                                text = "稍后",
                                onClick = { updateInfo = null },
                            )
                            DialogPrimaryButton(
                                text = "下载更新",
                                onClick = {
                                    downloadError = null
                                    downloadedApk = null
                                    downloadManager.reset()
                                    downloadJob = scope.launch {
                                        downloadManager.downloadWithProgress(
                                            info.downloadUrl,
                                            info.fileName,
                                            onProgress = {}
                                        ).onSuccess { file ->
                                            if (UpdateManager.verifyPackage(context, file, info)) {
                                                downloadedApk = file
                                            } else {
                                                file.delete()
                                                downloadError = "安装包校验失败，已删除，请重试"
                                            }
                                        }.onFailure { error ->
                                            downloadError = error.message ?: "下载失败"
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
