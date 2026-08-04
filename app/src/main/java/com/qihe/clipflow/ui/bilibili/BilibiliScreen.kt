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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
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
import com.qihe.clipflow.ui.components.DownloadProgressDialog
import com.qihe.clipflow.ui.components.GlassCard
import com.qihe.clipflow.ui.components.ParseInfoCard
import com.qihe.clipflow.ui.parser.PlatformParseInputCard
import com.qihe.clipflow.ui.component.liquid.ProgressiveContentBlur
import com.qihe.clipflow.ui.component.liquid.combineProgressiveBlurStrength
import top.yukonga.miuix.kmp.blur.Backdrop

@Composable
fun BilibiliScreen(
    sourceUrl: String? = null,
    contentBackdrop: Backdrop? = null,
    contentBlurStrength: Float = 0f,
    viewModel: BilibiliViewModel = viewModel(viewModelStoreOwner = LocalContext.current as androidx.activity.ComponentActivity)
) {
    val state by viewModel.state.collectAsState()
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
            contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 140.dp),
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
                        clipboard?.primaryClip?.getItemAt(0)?.text?.toString()?.let(viewModel::setInput)
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
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        details.parts.forEachIndexed { index, part ->
                            AssistChip(
                                onClick = { viewModel.selectPart(index) },
                                label = { Text("P${part.index}") }
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
                                    Text(quality.label)
                                    Button(onClick = { viewModel.download(index) }) { Text("下载 MP4") }
                                }
                            }
                        }
                    }
                }
            }
            }
        }
        ProgressiveContentBlur(
            backdrop = contentBackdrop,
            strength = combineProgressiveBlurStrength(
                base = contentBlurStrength,
                interaction = if (state.showDownloadDialog) 0.35f else 0f,
            ),
            fallbackColor = MaterialTheme.colorScheme.surface,
        )
        state.download?.let { download ->
            if (state.showDownloadDialog && (download.isDownloading || download.isComplete || download.error != null)) {
                DownloadProgressDialog(
                    state = download,
                    onDismiss = viewModel::dismissDownload,
                    onBackground = viewModel::backgroundDownload
                )
            }
        }
}
}
