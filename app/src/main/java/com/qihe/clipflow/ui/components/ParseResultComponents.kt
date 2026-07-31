package com.qihe.clipflow.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qihe.clipflow.data.api.model.ContentItem
import com.qihe.clipflow.data.api.model.ContentType
import com.qihe.clipflow.data.api.model.VideoBackupItem
import com.qihe.clipflow.util.DownloadState

@Composable
fun DownloadOptionsCard(
    items: List<ContentItem>,
    downloadStates: Map<String, DownloadState>,
    onDownload: (ContentItem) -> Unit,
    videoBackups: List<VideoBackupItem> = emptyList(),
    onDownloadBackupUrl: ((String, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Column {
            Text("下载选项", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))

            items.forEachIndexed { index, item ->
                DownloadOptionRow(
                    item = item,
                    state = downloadStates[item.id],
                    onDownload = { onDownload(item) }
                )
                if (index < items.lastIndex) DashedDivider(modifier = Modifier.padding(vertical = 4.dp))
            }

            if (videoBackups.isNotEmpty()) {
                BackupDownloadOptions(videoBackups, onDownloadBackupUrl)
            }
        }
    }
}

@Composable
private fun DownloadOptionRow(item: ContentItem, state: DownloadState?, onDownload: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (item.type) {
                ContentType.VIDEO, ContentType.LIVE_VIDEO -> Icons.Filled.Videocam
                ContentType.AUDIO -> Icons.Filled.MusicNote
                else -> Icons.Filled.Image
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.description.ifEmpty { item.type.label }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            listOfNotNull(item.mediaInfo?.resolution, item.mediaInfo?.codec, item.mediaInfo?.format)
                .takeIf { it.isNotEmpty() }
                ?.let { Text(it.joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        if (state?.isDownloading == true) {
            CircularProgressIndicator(progress = { state.progress }, modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
        } else {
            TextButton(onClick = onDownload) { Text("下载", fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun BackupDownloadOptions(
    videoBackups: List<VideoBackupItem>,
    onDownloadBackupUrl: ((String, String) -> Unit)?
) {
    var expanded by remember { mutableStateOf(false) }
    DashedDivider(modifier = Modifier.padding(vertical = 4.dp))
    TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
        Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(4.dp))
        Text("备用画质 (${videoBackups.size})", style = MaterialTheme.typography.labelMedium)
    }
    AnimatedVisibility(visible = expanded) {
        Column {
            videoBackups.forEach { backup ->
                val parts = listOfNotNull(
                    backup.quality,
                    if (backup.width > 0 && backup.height > 0) "${backup.width}x${backup.height}" else null,
                    backup.codec?.uppercase(),
                    backup.format?.uppercase()
                ).filter { it.isNotEmpty() }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = { backup.url?.let { onDownloadBackupUrl?.invoke(it, parts.firstOrNull() ?: "备用") } }) {
                        Text("下载", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
