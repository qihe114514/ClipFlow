package com.qihe.clipflow

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.qihe.clipflow.ui.theme.ClipFlowTheme
import com.qihe.clipflow.ui.theme.ThemeMode
import com.qihe.clipflow.ui.theme.resolveDarkTheme
import com.qihe.clipflow.util.DownloadManager
import com.qihe.clipflow.util.UpdateManager
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        splashScreen.setKeepOnScreenCondition { false }

        setContent {
            val preferences = remember { AppPreferences(applicationContext) }
            val themeModeKey by preferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM.key)
            val dynamicColor by preferences.dynamicColor.collectAsState(initial = true)
            val isSystemDark = isSystemInDarkTheme()
            ClipFlowTheme(
                darkTheme = resolveDarkTheme(ThemeMode.fromKey(themeModeKey), isSystemDark),
                dynamicColor = dynamicColor,
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    StartupUpdateCheck()
                    ClipFlowNavHost()
                }
            }
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

    LaunchedEffect(Unit) {
        try {
            UpdateManager.checkUpdate(BuildConfig.VERSION_NAME).onSuccess { info ->
                if (info == null) {
                    Toast.makeText(appContext, "已是最新版本", Toast.LENGTH_SHORT).show()
                } else {
                    updateInfo = info
                }
            }
        } catch (_: Exception) {
            // Startup update checks must not interrupt normal app use.
        }
    }

    updateInfo?.let { info ->
        AlertDialog(
            onDismissRequest = {
                if (!downloadState.isDownloading) updateInfo = null
            },
            title = { Text("发现新版本 v${info.latestVersion}") },
            text = {
                Column {
                    if (info.releaseNotes.isNotBlank()) {
                        Text(info.releaseNotes)
                        Spacer(Modifier.height(12.dp))
                    }
                    downloadError?.let {
                        Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                    }
                    if (downloadState.isDownloading) {
                        LinearProgressIndicator(
                            progress = { downloadState.progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("${(downloadState.progress * 100).toInt()}%  ${downloadState.speedText}")
                    } else if (downloadedApk != null) {
                        Text("下载完成，可以安装更新")
                    }
                }
            },
            confirmButton = {
                when {
                    downloadedApk != null -> Button(
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
                        }
                    ) {
                        Text("安装更新")
                    }
                    downloadState.isDownloading -> TextButton(onClick = {}, enabled = false) {
                        Text("下载中")
                    }
                    else -> TextButton(
                        onClick = {
                            downloadError = null
                            downloadedApk = null
                            downloadManager.reset()
                            scope.launch {
                                downloadManager.downloadWithProgress(
                                    info.downloadUrl,
                                    info.fileName,
                                    onProgress = {}
                                ).onSuccess { file ->
                                    downloadedApk = file
                                }.onFailure { error ->
                                    downloadError = error.message ?: "下载失败"
                                }
                            }
                        }
                    ) {
                        Text("下载更新")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { updateInfo = null },
                    enabled = !downloadState.isDownloading
                ) {
                    Text("稍后")
                }
            }
        )
    }
}
