package com.qihe.clipflow.ui.parser

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.qihe.clipflow.data.api.model.ContentItem
import com.qihe.clipflow.ui.components.DownloadOptionsCard
import com.qihe.clipflow.ui.components.DownloadProgressDialog
import com.qihe.clipflow.ui.components.GlassButton
import com.qihe.clipflow.ui.components.GlassCard
import com.qihe.clipflow.ui.components.GlassTextField
import com.qihe.clipflow.ui.components.ParseInfoCard

/** Shared parser-page layout. Platform screens only provide labels, color, and actions. */
@Composable
fun PlatformParseScreenContent(
    uiState: ParsePageUiState,
    placeholder: String,
    resultFallbackTitle: String,
    accent: Color,
    onUrlChange: (String) -> Unit,
    onClearOrPaste: (hasInput: Boolean) -> Unit,
    onParse: () -> Unit,
    onDownload: (ContentItem) -> Unit,
    onDownloadBackupUrl: ((String, String) -> Unit)? = null,
    onDismissDownloadDialog: (background: Boolean) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "input") {
                PlatformParseInputCard(
                    inputUrl = uiState.inputUrl,
                    isParsing = uiState.isParsing,
                    placeholder = placeholder,
                    accent = accent,
                    onUrlChange = onUrlChange,
                    onClearOrPaste = onClearOrPaste,
                    onParse = onParse
                )
            }

            uiState.error?.let { message ->
                item(key = "error") { ParseErrorCard(message) }
            }

            if (uiState.isParsing) {
                item(key = "loading") { ParseLoadingCard(accent) }
            }

            uiState.parseResult?.takeIf { it.isNotEmpty() }?.let { items ->
                item(key = "info_card") {
                    ParseInfoCard(
                        cover = uiState.parseCover,
                        title = uiState.parseTitle.ifEmpty { resultFallbackTitle },
                        desc = uiState.parseDesc,
                        authorName = uiState.authorName,
                        authorAvatar = uiState.authorAvatar,
                        contentType = uiState.contentType,
                        shareUrl = uiState.shareUrl,
                        stats = uiState.stats,
                        previewItems = items
                    )
                }
                item(key = "download_card") {
                    DownloadOptionsCard(
                        items = items,
                        downloadStates = uiState.downloadStates,
                        onDownload = onDownload,
                        videoBackups = uiState.videoBackups,
                        onDownloadBackupUrl = onDownloadBackupUrl
                    )
                }
            }

            item(key = "spacer") { Spacer(Modifier.height(20.dp)) }
        }

        if (uiState.showDownloadDialog && uiState.downloadingItemId != null) {
            DownloadProgressDialog(
                state = uiState.downloadStates[uiState.downloadingItemId],
                onDismiss = { onDismissDownloadDialog(false) },
                onBackground = { onDismissDownloadDialog(true) }
            )
        }
    }
}

@Composable
fun PlatformParseInputCard(
    inputUrl: String,
    isParsing: Boolean,
    placeholder: String,
    accent: Color,
    onUrlChange: (String) -> Unit,
    onClearOrPaste: (hasInput: Boolean) -> Unit,
    onParse: () -> Unit
) {
    GlassCard {
        Column {
            GlassTextField(
                value = inputUrl,
                onValueChange = onUrlChange,
                placeholder = placeholder,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val hasInput = inputUrl.isNotBlank()
                FilledTonalIconButton(
                    onClick = { onClearOrPaste(hasInput) },
                    modifier = Modifier.size(44.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = accent.copy(alpha = 0.12f)
                    )
                ) {
                    Icon(
                        imageVector = if (hasInput) Icons.Filled.Close else Icons.Outlined.ContentPaste,
                        contentDescription = if (hasInput) "清空" else "粘贴",
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                GlassButton(
                    text = if (isParsing) "解析中..." else "开始解析",
                    onClick = onParse,
                    enabled = !isParsing && hasInput,
                    containerColor = accent,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ParseErrorCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ParseLoadingCard(accent: Color) {
    GlassCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp), color = accent)
            Spacer(Modifier.height(12.dp))
            Text(
                text = "正在解析链接...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
