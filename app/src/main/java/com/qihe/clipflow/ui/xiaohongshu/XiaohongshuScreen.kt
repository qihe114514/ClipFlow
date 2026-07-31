package com.qihe.clipflow.ui.xiaohongshu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qihe.clipflow.ui.parser.PlatformParseScreenContent
import com.qihe.clipflow.ui.theme.XiaohongshuAccent

@Composable
fun XiaohongshuScreen(
    sourceUrl: String? = null,
    viewModel: XiaohongshuViewModel = viewModel(viewModelStoreOwner = LocalContext.current as androidx.activity.ComponentActivity)
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(sourceUrl) {
        sourceUrl?.takeIf { it.isNotBlank() }?.let {
            viewModel.onUrlChange(it)
            viewModel.parse()
        }
    }

    PlatformParseScreenContent(
        uiState = uiState,
        placeholder = "粘贴小红书分享链接...",
        resultFallbackTitle = "小红书笔记",
        accent = XiaohongshuAccent,
        onUrlChange = viewModel::onUrlChange,
        onClearOrPaste = { hasInput -> if (hasInput) viewModel.clearUrl() else viewModel.pasteFromClipboard() },
        onParse = viewModel::parse,
        onDownload = viewModel::downloadItem,
        onDismissDownloadDialog = viewModel::dismissDownloadDialog
    )
}
