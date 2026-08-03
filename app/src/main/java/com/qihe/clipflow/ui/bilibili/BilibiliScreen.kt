package com.qihe.clipflow.ui.bilibili

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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

@Composable
fun BilibiliScreen(
    sourceUrl: String? = null,
    viewModel: BilibiliViewModel = viewModel(viewModelStoreOwner = LocalContext.current as androidx.activity.ComponentActivity)
) {
    val state by viewModel.state.collectAsState()
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
            GlassCard {
                Column {
                    OutlinedTextField(
                        value = state.input,
                        onValueChange = viewModel::setInput,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("粘贴 B 站视频链接...") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = viewModel::parse,
                        enabled = !state.loading && state.input.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.loading) CircularProgressIndicator(Modifier.padding(2.dp)) else Text("开始解析")
                    }
                }
            }
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
                    shareUrl = result.shareUrl
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
