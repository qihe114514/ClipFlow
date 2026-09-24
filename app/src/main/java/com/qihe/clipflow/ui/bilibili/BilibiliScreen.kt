package com.qihe.clipflow.ui.bilibili

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qihe.clipflow.data.api.model.ContentType
import com.qihe.clipflow.ui.components.rememberNotificationPermissionRequest
import com.qihe.clipflow.ui.components.DownloadProgressDialog
import com.qihe.clipflow.ui.components.GlassCard
import com.qihe.clipflow.ui.components.ParseInfoCard
import com.qihe.clipflow.ui.parser.PlatformParseInputCard
import com.qihe.clipflow.ui.component.liquid.PROGRESSIVE_TOPBAR_CONTENT_START_DP
import com.qihe.clipflow.ui.component.LiquidButton

@Composable
fun BilibiliScreen(
    sourceUrl: String? = null,
    viewModel: BilibiliViewModel = viewModel(viewModelStoreOwner = LocalContext.current as androidx.activity.ComponentActivity)
) {
    val state by viewModel.state.collectAsState()
    val requestNotificationPermission = rememberNotificationPermissionRequest()
    val context = LocalContext.current
    LaunchedEffect(sourceUrl) {
        sourceUrl?.let {
            viewModel.setInput(it)
            viewModel.parse()
        }
    }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = PROGRESSIVE_TOPBAR_CONTENT_START_DP.dp + 10.dp,
                end = 20.dp,
                bottom = 140.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
        item {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            PlatformParseInputCard(
                inputUrl = state.input,
                isParsing = state.loading,
                placeholder = "粘贴 B 站视频链接...",
                accent = MaterialTheme.colorScheme.primary,
                onUrlChange = viewModel::setInput,
                onClearOrPaste = { hasInput ->
                    if (hasInput) {
                        viewModel.setInput("")
                    } else {
                        val clip = clipboard?.primaryClip
                        if (clip != null && clip.itemCount > 0) {
                            runCatching { clip.getItemAt(0)?.text?.toString() }.getOrNull()?.let(viewModel::setInput)
                        }
                    }
                },
                onParse = { viewModel.parse() }
            )
            }
        state.error?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error) } }
        state.result?.let { result ->
            val details = result.bilibili
            item {
                ParseInfoCard(
                    cover = result.cover,
                    title = result.title,
                    desc = result.desc,
                    authorName = result.authorName,
                    authorAvatar = result.authorAvatar,
                    contentType = "video",
                    shareUrl = result.shareUrl,
                    previewItems = result.items
                )
            }
            if (details != null && details.parts.size > 1) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        itemsIndexed(details.parts) { index, part ->
                            AssistChip(
                                onClick = { viewModel.selectPart(index) },
                                label = { Text("P${index + 1}") }
                            )
                        }
                    }
                }
            }
            details?.qualities?.let { qualities ->
                item {
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("可用清晰度", style = MaterialTheme.typography.titleSmall)
                            qualities.forEachIndexed { index, quality ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(listOfNotNull(quality.label, quality.codec).joinToString(" · "))
                                    if (index in state.completedQualityIds) {
                                        Text(
                                            text = "已保存",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    } else {
                                        LiquidButton(onClick = { requestNotificationPermission(); viewModel.download(index) }) { Text("下载 MP4") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }
        }
        state.download?.let { download ->
            if (state.showDownloadDialog && (download.isDownloading || download.isComplete || download.error != null)) {
                DownloadProgressDialog(
                    state = download,
                    onDismiss = viewModel::dismissDownload,
                    onBackground = viewModel::backgroundDownload,
                    onCancel = viewModel::cancelDownload,
                    mediaType = ContentType.VIDEO
                )
            }
        }
}
}
