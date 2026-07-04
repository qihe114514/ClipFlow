package com.qihe.clipflow.ui.xiaohongshu

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import coil.request.ImageRequest
import com.qihe.clipflow.data.api.model.ContentItem
import com.qihe.clipflow.data.api.model.ContentType
import com.qihe.clipflow.ui.components.*
import com.qihe.clipflow.ui.douyin.DownloadProgressDialog
import com.qihe.clipflow.ui.theme.*
import com.qihe.clipflow.navigation.ParseBridge

@Composable
fun XiaohongshuScreen(viewModel: XiaohongshuViewModel = viewModel(viewModelStoreOwner = LocalContext.current as androidx.activity.ComponentActivity)) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        ParseBridge.pendingUrl?.let { url ->
            ParseBridge.pendingUrl = null
            viewModel.onUrlChange(url)
            viewModel.parse()
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
                            placeholder = "粘贴小红书分享链接...",
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
                                    containerColor = XiaohongshuAccent.copy(alpha = 0.12f)
                                )
                            ) {
                                Icon(
                                    imageVector = if (hasContent) Icons.Filled.Close else Icons.Outlined.ContentPaste,
                                    contentDescription = if (hasContent) "清空" else "粘贴",
                                    tint = XiaohongshuAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            GlassButton(
                                text = if (uiState.isParsing) "解析中..." else "开始解析",
                                onClick = { viewModel.parse() },
                                enabled = !uiState.isParsing && uiState.inputUrl.isNotBlank(),
                                containerColor = XiaohongshuAccent,
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
                                color = XiaohongshuAccent
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
                        title = uiState.parseTitle.ifEmpty { "小红书笔记" },
                        desc = uiState.parseDesc,
                        authorName = uiState.authorName,
                        authorAvatar = uiState.authorAvatar,
                        contentType = uiState.contentType,
                        videoUrl = uiState.parseResult?.firstOrNull { it.type == com.qihe.clipflow.data.api.model.ContentType.VIDEO }?.url ?: ""
                    )
                }
                // 下载卡片
                item(key = "download_card") {
                    DownloadOptionsCard(
                        items = uiState.parseResult!!,
                        downloadStates = uiState.downloadStates,
                        onDownload = { viewModel.downloadItem(it) }
                    )
                }
            }

            item(key = "spacer") {
                Spacer(Modifier.height(20.dp))
            }
        }

        // ========== 下载进度弹窗 ==========
        if (uiState.showDownloadDialog && uiState.downloadingItemId != null) {
            val downloadState = uiState.downloadStates[uiState.downloadingItemId]
            DownloadProgressDialog(
                state = downloadState,
                onDismiss = { viewModel.dismissDownloadDialog() }
            )
        }
    }
}

