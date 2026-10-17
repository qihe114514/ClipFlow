package com.qihe.clipflow.ui.components

import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import com.qihe.clipflow.data.api.model.ContentType
import com.qihe.clipflow.ui.icons.ClipFlowIcons
import com.qihe.clipflow.util.DownloadState

@Composable
fun DownloadProgressDialog(
    state: DownloadState?,
    onDismiss: () -> Unit,
    onBackground: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    mediaType: ContentType? = null
) {
    val progress by animateFloatAsState(
        targetValue = (state?.progress ?: 0f).coerceIn(0f, 1f),
        animationSpec = tween(220),
        label = "downloadProgress",
    )

    Dialog(onDismissRequest = { if (state?.isComplete == true) onDismiss() }) {
        GlassDialogCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = when {
                        state?.isComplete == true -> ClipFlowIcons.Check
                        state?.error != null -> ClipFlowIcons.Warning
                        else -> ClipFlowIcons.Download
                    },
                    contentDescription = null,
                    tint = when {
                        state?.error != null -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(22.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            state?.isComplete == true -> "下载完成"
                            state?.error != null -> "下载失败"
                            else -> "正在下载"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = when {
                            state?.isComplete == true -> "文件已保存到相册或下载目录"
                            state?.error != null -> "请检查网络后重新下载"
                            else -> "可以留在本页等待，也可以转到后台继续"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center) {
                        when {
                            state?.error != null -> Icon(
                                imageVector = ClipFlowIcons.Close,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(54.dp),
                            )
                            state?.isComplete == true -> Icon(
                                imageVector = ClipFlowIcons.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(54.dp),
                            )
                            else -> CircularProgressWithSpeed(
                                progress = progress,
                                speedText = if (state?.isDownloading == true) state.speedText.orEmpty() else "",
                                size = 132.dp,
                                strokeWidth = 9.dp,
                                progressColor = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    if (state?.totalBytes != null && state.totalBytes > 0) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "${formatFileSize(state.downloadedBytes)} / ${formatFileSize(state.totalBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (state?.error != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = state.error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            when {
                state?.isComplete == true -> {
                    val context = LocalContext.current
                    GlassDialogActions {
                        DialogSecondaryButton(text = "关闭", onClick = onDismiss)
                        DialogPrimaryButton(
                            text = "打开",
                            onClick = {
                                openSavedMedia(context, state, mediaType)
                                onDismiss()
                            },
                        )
                    }
                }
                state?.error != null -> {
                    GlassDialogActions {
                        DialogPrimaryButton(text = "关闭", onClick = onDismiss)
                    }
                }
                state?.isDownloading == true -> {
                    GlassDialogActions {
                        if (onCancel != null) {
                            DialogDangerButton(text = "取消下载", onClick = onCancel)
                        }
                        if (onBackground != null) {
                            DialogPrimaryButton(text = "后台下载", onClick = onBackground)
                        }
                    }
                }
            }
        }
    }
}

private fun openSavedMedia(context: Context, state: DownloadState, type: ContentType?) {
    val mimeType = when (type) {
        ContentType.AUDIO -> "audio/*"
        ContentType.IMAGE, ContentType.LIVE_IMAGE -> "image/*"
        ContentType.VIDEO, ContentType.LIVE_VIDEO -> "video/*"
        null -> "*/*"
    }
    val savedUri = state.savedMediaUri?.takeIf { it.isNotBlank() }?.toUri()
    val intent = Intent(Intent.ACTION_VIEW).apply {
        if (savedUri != null) {
            setDataAndType(savedUri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            setDataAndType(collectionFor(type), mimeType)
        }
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}

private fun collectionFor(type: ContentType?) = when (type) {
    ContentType.AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    ContentType.IMAGE, ContentType.LIVE_IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    else -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
}

fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> "%.1f GB".format(bytes / 1_000_000_000.0)
        bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
        bytes >= 1_000 -> "%.1f KB".format(bytes / 1_000.0)
        else -> "$bytes B"
    }
}
