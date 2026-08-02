package com.qihe.clipflow.ui.components

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.qihe.clipflow.data.api.model.DouyinStatistics
import com.qihe.clipflow.ui.theme.ImageTypeBadge
import com.qihe.clipflow.ui.theme.LiveTypeBadge
import com.qihe.clipflow.ui.theme.VideoTypeBadge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ParseInfoCard(
    cover: String,
    title: String,
    desc: String = "",
    authorName: String,
    authorAvatar: String,
    contentType: String,
    shareUrl: String = "",
    stats: DouyinStatistics? = null,
    videoUrl: String = "",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSaveDialog by remember { mutableStateOf(false) }
    var showVideoPlayer by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    var isFullscreen by rememberSaveable { mutableStateOf(false) }

    InlineVideoDialog(showVideoPlayer, videoUrl, onDismiss = { showVideoPlayer = false }, onFullscreen = { isFullscreen = true })
    FullscreenVideoDialog(isFullscreen, videoUrl, onDismiss = { isFullscreen = false })
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("保存封面到相册？") },
            text = { Text("是否将封面保存到系统相册？") },
            confirmButton = {
                TextButton(onClick = {
                    showSaveDialog = false
                    scope.launch { saveCoverToGallery(context, cover) }
                }) { Text("好的喵") }
            },
            dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text("不好喵") } }
        )
    }

    GlassCard(modifier = modifier) {
        Column {
            if (cover.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(cover).crossfade(true).build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)).combinedClickable(
                        onClick = { if (videoUrl.isNotEmpty()) showVideoPlayer = true },
                        onLongClick = { showSaveDialog = true }
                    ),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(14.dp))
            }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (desc.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                ExpandableDescription(desc, isExpanded, onExpandedChange = { isExpanded = it })
            }
            Spacer(Modifier.height(10.dp))
            ParseAuthorRow(authorName, authorAvatar, contentType, shareUrl)
            stats?.let {
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatItem(Icons.Filled.Favorite, formatCount(it.diggCount))
                    StatItem(Icons.Filled.ChatBubbleOutline, formatCount(it.commentCount))
                    StatItem(Icons.Filled.BookmarkBorder, formatCount(it.collectCount))
                    StatItem(Icons.Filled.Share, formatCount(it.shareCount))
                }
            }
        }
    }
}

@Composable
private fun InlineVideoDialog(show: Boolean, videoUrl: String, onDismiss: () -> Unit, onFullscreen: () -> Unit) {
    if (!show || videoUrl.isEmpty()) return
    val context = LocalContext.current
    val player = remember(videoUrl) {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            setMediaItem(androidx.media3.common.MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("在线播放", style = MaterialTheme.typography.titleSmall) },
        text = {
            Box(Modifier.fillMaxWidth().height(280.dp)) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { androidx.media3.ui.PlayerView(it).apply { this.player = player; useController = true } },
                    modifier = Modifier.fillMaxSize()
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onDismiss(); onFullscreen() }) { Text("全屏") }
                TextButton(onClick = onDismiss) { Text("关闭") }
            }
        }
    )
}

private fun setSystemBarsHidden(activity: Activity?, hidden: Boolean) {
    val window = activity?.window ?: return
    val controller = WindowCompat.getInsetsController(window, window.decorView)
    controller.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    if (hidden) {
        controller.hide(WindowInsetsCompat.Type.systemBars())
    } else {
        controller.show(WindowInsetsCompat.Type.systemBars())
    }
}

@Composable
private fun FullscreenVideoDialog(show: Boolean, videoUrl: String, onDismiss: () -> Unit) {
    if (!show || videoUrl.isEmpty()) return
    val context = LocalContext.current
    val activity = context as? Activity
    val player = remember(videoUrl) {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            setMediaItem(androidx.media3.common.MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = true
        }
    }
    var isLandscapeVideo by remember { mutableStateOf(false) }
    DisposableEffect(player) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                isLandscapeVideo = videoSize.width > videoSize.height
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    LaunchedEffect(isLandscapeVideo) {
        if (isLandscapeVideo) activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        setSystemBarsHidden(activity, true)
    }
    fun close() {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setSystemBarsHidden(activity, false)
        onDismiss()
    }
    Dialog(
        onDismissRequest = ::close,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { androidx.media3.ui.PlayerView(it).apply { this.player = player; useController = true } },
                modifier = Modifier.fillMaxSize()
            )
            IconButton(onClick = ::close, modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp)) {
                Icon(Icons.Filled.Close, "退出全屏", tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
private fun ExpandableDescription(desc: String, isExpanded: Boolean, onExpandedChange: (Boolean) -> Unit) {
    var isOverflowed by remember(desc) { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = desc,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (isExpanded) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!isExpanded && it.hasVisualOverflow) isOverflowed = true },
            modifier = Modifier.weight(1f)
        )
        if (isOverflowed || isExpanded) {
            TextButton(onClick = { onExpandedChange(!isExpanded) }, modifier = Modifier.widthIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                Text(if (isExpanded) "收起" else "展开", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun ParseAuthorRow(authorName: String, authorAvatar: String, contentType: String, shareUrl: String) {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (authorAvatar.isNotEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(authorAvatar).crossfade(true).build(),
                contentDescription = null,
                modifier = Modifier.size(28.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(authorName.ifEmpty { "未知作者" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (contentType.isNotEmpty()) {
            Spacer(Modifier.width(10.dp))
            val badge = when (contentType) {
                "video" -> "视频" to VideoTypeBadge
                "image" -> "图集" to ImageTypeBadge
                "live" -> "实况" to LiveTypeBadge
                else -> contentType to VideoTypeBadge
            }
            TypeBadge(badge.first, badge.second)
        }
        if (shareUrl.isNotEmpty()) {
            Spacer(Modifier.weight(1f))
            FilledTonalIconButton(
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(shareUrl))) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Filled.OpenInBrowser, "打开原链接", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun StatItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatCount(count: Long): String = when {
    count >= 10000 -> "${count / 10000}.${(count % 10000) / 1000}w"
    count >= 1000 -> "${count / 1000}.${(count % 1000) / 100}k"
    else -> count.toString()
}

private suspend fun saveCoverToGallery(context: Context, imageUrl: String) {
    val saved = runCatching {
        withContext(Dispatchers.IO) {
            OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
                .newCall(Request.Builder().url(imageUrl).addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36").build())
                .execute().use { response ->
                    if (!response.isSuccessful) return@withContext false
                    val bytes = response.body?.bytes() ?: return@withContext false
                    val fileName = "ClipFlow_cover_${System.currentTimeMillis()}.jpg"
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val values = ContentValues().apply {
                            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/ClipFlow")
                            put(MediaStore.MediaColumns.IS_PENDING, 1)
                        }
                        val uri = context.contentResolver.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
                            ?: return@withContext false
                        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: return@withContext false
                        values.clear()
                        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        context.contentResolver.update(uri, values, null, null)
                        true
                    } else {
                        val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "ClipFlow")
                        if (!directory.exists()) directory.mkdirs()
                        FileOutputStream(File(directory, fileName)).use { it.write(bytes) }
                        true
                    }
                }
        }
    }.getOrDefault(false)
    Toast.makeText(context, if (saved) "封面已保存到相册" else "保存封面失败", Toast.LENGTH_SHORT).show()
}
