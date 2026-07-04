package com.qihe.clipflow.ui.douyin

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import coil.request.ImageRequest
import com.qihe.clipflow.data.api.model.ContentItem
import com.qihe.clipflow.data.api.model.ContentType
import com.qihe.clipflow.ui.components.*
import com.qihe.clipflow.ui.theme.*
import com.qihe.clipflow.navigation.ParseBridge

@Composable
fun DouyinScreen(viewModel: DouyinViewModel = viewModel(viewModelStoreOwner = LocalContext.current as androidx.activity.ComponentActivity)) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        ParseBridge.pendingUrl?.let { url ->
            ParseBridge.pendingUrl = null
            viewModel.onUrlChange(url)
            viewModel.parse()
        }
    }

    // ========== 新手教程（函数级，不在 Box 内）==========
    val context = LocalContext.current
    val prefs = remember { com.qihe.clipflow.data.preferences.AppPreferences(context) }
    val scope = rememberCoroutineScope()
    val tutorialShown by produceState(initialValue = true) {
        prefs.tutorialShown.collect { value = it }
    }
    var showTutorial by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.parseResult) {
        if (uiState.parseResult != null && !tutorialShown) {
            delay(600)
            showTutorial = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp, end = 20.dp,
                top = 16.dp, bottom = 100.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ========== 输入区 ==========
            item(key = "input") {
                GlassCard {
                    Column {
                        GlassTextField(
                            value = uiState.inputUrl,
                            onValueChange = { viewModel.onUrlChange(it) },
                            placeholder = "粘贴抖音分享链接...",
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val hasContent = uiState.inputUrl.isNotBlank()
                            FilledTonalIconButton(
                                onClick = {
                                    if (hasContent) viewModel.clearUrl()
                                    else viewModel.pasteFromClipboard()
                                },
                                modifier = Modifier.size(44.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = DouyinAccent.copy(alpha = 0.12f)
                                )
                            ) {
                                Icon(
                                    imageVector = if (hasContent) Icons.Filled.Close else Icons.Outlined.ContentPaste,
                                    contentDescription = if (hasContent) "清空" else "粘贴",
                                    tint = DouyinAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            GlassButton(
                                text = if (uiState.isParsing) "解析中..." else "开始解析",
                                onClick = { viewModel.parse() },
                                enabled = !uiState.isParsing && uiState.inputUrl.isNotBlank(),
                                containerColor = DouyinAccent,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // ========== 错误提示 ==========
            if (uiState.error != null) {
                item(key = "error") {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = uiState.error ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // ========== 加载中 ==========
            if (uiState.isParsing) {
                item(key = "loading") {
                    GlassCard {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = DouyinAccent
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "正在解析链接...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ========== 解析结果 ==========
            if (uiState.parseResult != null && uiState.parseResult!!.isNotEmpty()) {
                // 信息卡片
                item(key = "info_card") {
                    ParseInfoCard(
                        cover = uiState.parseCover,
                        title = uiState.parseTitle.ifEmpty { "抖音作品" },
                        desc = uiState.parseDesc,
                        authorName = uiState.authorName,
                        authorAvatar = uiState.authorAvatar,
                        contentType = uiState.contentType,
                        shareUrl = uiState.shareUrl,
                        stats = uiState.stats,
                        videoUrl = uiState.parseResult?.firstOrNull { it.type == com.qihe.clipflow.data.api.model.ContentType.VIDEO }?.url ?: ""
                    )
                }
                // 下载卡片
                item(key = "download_card") {
                    DownloadOptionsCard(
                        items = uiState.parseResult!!,
                        downloadStates = uiState.downloadStates,
                        onDownload = { viewModel.downloadItem(it) },
                        videoBackups = uiState.videoBackups,
                        onDownloadBackupUrl = { url, label -> viewModel.downloadBackupUrl(url, label) }
                    )
                }
            }

            item(key = "spacer") {
                Spacer(Modifier.height(20.dp))
            }
        }

        // 下载进度弹窗
        if (uiState.showDownloadDialog && uiState.downloadingItemId != null) {
            val downloadState = uiState.downloadStates[uiState.downloadingItemId]
            DownloadProgressDialog(
                state = downloadState,
                onDismiss = { viewModel.dismissDownloadDialog() },
                onBackground = { viewModel.dismissDownloadDialog(background = true) }
            )
        }
    }

    if (showTutorial) {
        TutorialOverlay(
            onDismiss = {
                showTutorial = false
                scope.launch { prefs.setTutorialShown(true) }
            }
        )
    }
}

// ==================== 信息卡片 ====================
// ==================== 下载进度弹窗 ====================

@Composable
fun DownloadProgressDialog(
    state: com.qihe.clipflow.util.DownloadState?,
    onDismiss: () -> Unit,
    onBackground: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = { if (state?.isComplete == true) onDismiss() },
        title = {
            Text(
                text = if (state?.isComplete == true) "下载完成" else "正在下载",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressWithSpeed(
                    progress = state?.progress ?: 0f,
                    speedText = state?.speedText ?: "",
                    size = 130.dp,
                    strokeWidth = 10.dp,
                    progressColor = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(16.dp))

                if (state?.totalBytes != null && state.totalBytes > 0) {
                    Text(
                        text = "${formatFileSize(state.downloadedBytes)} / ${formatFileSize(state.totalBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (state?.error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            if (state?.isComplete == true || state?.error != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.isComplete) {
                        val ctx = LocalContext.current
                        TextButton(onClick = {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                    setDataAndType(
                                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                        "image/*"
                                    )
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                ctx.startActivity(intent)
                            } catch (_: Exception) {}
                            onDismiss()
                        }) {
                            Text("打开")
                        }
                    }
                    TextButton(onClick = onDismiss) {
                        Text("关闭")
                    }
                }
            } else if (state?.isDownloading == true) {
                TextButton(onClick = onBackground) {
                    Text("后台下载")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    )
}

fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> "%.1f GB".format(bytes / 1_000_000_000.0)
        bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
        bytes >= 1_000 -> "%.1f KB".format(bytes / 1_000.0)
        else -> "$bytes B"
    }
}
