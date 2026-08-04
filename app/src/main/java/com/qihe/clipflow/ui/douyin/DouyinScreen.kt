package com.qihe.clipflow.ui.douyin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qihe.clipflow.data.preferences.AppPreferences
import com.qihe.clipflow.ui.components.TutorialOverlay
import com.qihe.clipflow.ui.parser.PlatformParseScreenContent
import com.qihe.clipflow.ui.theme.DouyinAccent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.Backdrop

@Composable
fun DouyinScreen(
    sourceUrl: String? = null,
    contentBackdrop: Backdrop? = null,
    contentBlurStrength: Float = 0f,
    viewModel: DouyinViewModel = viewModel(viewModelStoreOwner = LocalContext.current as androidx.activity.ComponentActivity)
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    val scope = rememberCoroutineScope()
    val tutorialShown by produceState(initialValue = true) { prefs.tutorialShown.collect { value = it } }
    var showTutorial by remember { mutableStateOf(false) }

    LaunchedEffect(sourceUrl) {
        viewModel.consumeSourceUrl(sourceUrl)?.let { url ->
            viewModel.onUrlChange(url)
            viewModel.parse()
        }
    }
    LaunchedEffect(uiState.parseResult) {
        if (uiState.parseResult != null && !tutorialShown) {
            delay(600)
            showTutorial = true
        }
    }

    Box(Modifier.fillMaxSize()) {
        PlatformParseScreenContent(
            uiState = uiState,
            placeholder = "粘贴抖音分享链接...",
            resultFallbackTitle = "抖音作品",
            accent = DouyinAccent,
            onUrlChange = viewModel::onUrlChange,
            onClearOrPaste = { hasInput -> if (hasInput) viewModel.clearUrl() else viewModel.pasteFromClipboard() },
            onParse = viewModel::parse,
            onDownload = viewModel::downloadItem,
            onDownloadBackupUrl = viewModel::downloadBackupUrl,
            onDismissDownloadDialog = viewModel::dismissDownloadDialog,
            contentBackdrop = contentBackdrop,
            contentBlurStrength = contentBlurStrength,
        )
        if (showTutorial) {
            TutorialOverlay(
                onDismiss = {
                    showTutorial = false
                    scope.launch { prefs.setTutorialShown(true) }
                }
            )
        }
    }
}
