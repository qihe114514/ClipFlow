package com.qihe.clipflow.ui.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.qihe.clipflow.data.local.HistoryEntity
import com.qihe.clipflow.ui.components.GlassCard
import com.qihe.clipflow.ui.components.TypeBadge
import com.qihe.clipflow.ui.theme.DouyinAccent
import com.qihe.clipflow.ui.theme.XiaohongshuAccent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryItemCard(
    item: HistoryEntity,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    GlassCard(modifier = modifier.combinedClickable(onClick = onTap, onLongClick = onLongPress)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (isSelectionMode) {
                Checkbox(checked = isSelected, onCheckedChange = { onTap() }, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
            }
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                if (item.coverUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(item.coverUrl).crossfade(true).build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val isDouyin = item.platform == "douyin"
                    Icon(
                        imageVector = if (isDouyin) Icons.Filled.MusicNote else Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = (if (isDouyin) DouyinAccent else XiaohongshuAccent).copy(alpha = 0.4f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title.ifEmpty { "未命名" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!item.authorName.isNullOrEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(item.authorName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val isDouyin = item.platform == "douyin"
                    TypeBadge(
                        text = when (item.platform) {
                            "douyin" -> "抖音"
                            "xiaohongshu" -> "小红书"
                            else -> item.platform
                        },
                        color = if (isDouyin) DouyinAccent else XiaohongshuAccent
                    )
                    Text(formatTime(item.timestamp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                }
            }
            if (!isSelectionMode) {
                Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

private fun formatTime(timestamp: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
