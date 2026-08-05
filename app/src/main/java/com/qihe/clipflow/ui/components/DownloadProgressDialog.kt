package com.qihe.clipflow.ui.components

import android.content.Intent
import android.provider.MediaStore
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.qihe.clipflow.util.DownloadState
import com.qihe.clipflow.ui.component.LiquidButton

@Composable
fun DownloadProgressDialog(
    state: DownloadState?,
    onDismiss: () -> Unit,
    onBackground: (() -> Unit)? = null
) {
    Dialog(onDismissRequest = { if (state?.isComplete == true) onDismiss() }) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = if (state?.isComplete == true) "下载完成" else "正在下载",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressWithSpeed(
                        progress = state?.progress ?: 0f,
                        speedText = state?.speedText.orEmpty(),
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
                Spacer(Modifier.height(20.dp))
                if (state?.isComplete == true || state?.error != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        if (state.isComplete) {
                            val context = LocalContext.current
                            LiquidButton(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                    }
                                    onDismiss()
                                }
                            ) { Text("打开") }
                        }
                        LiquidButton(onClick = onDismiss) { Text("关闭") }
                    }
                } else if (state?.isDownloading == true && onBackground != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        LiquidButton(onClick = onBackground) { Text("后台下载") }
                    }
                }
            }
        }
    }
}

fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> "%.1f GB".format(bytes / 1_000_000_000.0)
        bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
        bytes >= 1_000 -> "%.1f KB".format(bytes / 1_000.0)
        else -> "$bytes B"
    }
}
