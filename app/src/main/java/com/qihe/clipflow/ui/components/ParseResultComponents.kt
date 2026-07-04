package com.qihe.clipflow.ui.components

import android.content.ContentValues
import android.content.pm.ActivityInfo
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.qihe.clipflow.data.api.model.ContentItem
import com.qihe.clipflow.data.api.model.ContentType
import com.qihe.clipflow.data.api.model.DouyinStatistics
import com.qihe.clipflow.ui.douyin.formatFileSize
import com.qihe.clipflow.ui.theme.*
import com.qihe.clipflow.util.DownloadState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color

// ==================== 新手教程引导遮罩 ====================

@Composable
fun TutorialOverlay(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "arrow")
    val arrowOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrow_bounce"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 160.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "↓",
                fontSize = 40.sp,
                color = Color.White,
                modifier = Modifier.offset(y = arrowOffset.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "点击封面在线播放",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "长按封面保存到相册",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
            Spacer(Modifier.height(120.dp))
            Text(
                text = "点任意位置关闭",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp
            )
        }
    }
}

// ==================== 信息卡片（抖音 + 小红书通用） ====================

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
    var showSaveDialog by remember { mutableStateOf(false) }
    var showVideoPlayer by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }

    // 视频播放弹窗
    if (showVideoPlayer && videoUrl.isNotEmpty()) {
        var isFullscreen by remember { mutableStateOf(false) }
        val player = remember {
            androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
                setMediaItem(androidx.media3.common.MediaItem.fromUri(android.net.Uri.parse(videoUrl)))
                prepare()
                playWhenReady = true
            }
        }
        // 页面隐藏时释放
        DisposableEffect(Unit) {
            onDispose { player.release() }
        }
        if (isFullscreen) {
            // 全屏沉浸式播放
            val activity = context as? android.app.Activity
            SideEffect {
                activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                activity?.window?.insetsController?.apply {
                    hide(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
                    systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
            Dialog(
                onDismissRequest = {
                    activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    activity?.window?.insetsController?.show(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
                    isFullscreen = false
                },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = false
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    androidx.compose.ui.viewinterop.AndroidView(
                        factory = { viewCtx ->
                            androidx.media3.ui.PlayerView(viewCtx).apply {
                                this.player = player
                                useController = true
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    // 退出全屏按钮
                    IconButton(
                        onClick = {
                            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            activity?.window?.insetsController?.show(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
                            isFullscreen = false
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "退出全屏",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        } else {
            AlertDialog(
                onDismissRequest = { showVideoPlayer = false; player.release() },
                title = { Text("在线播放", style = MaterialTheme.typography.titleSmall) },
                text = {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(280.dp)
                    ) {
                        androidx.compose.ui.viewinterop.AndroidView(
                            factory = { ctx ->
                                androidx.media3.ui.PlayerView(ctx).apply {
                                    this.player = player
                                    useController = true
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { isFullscreen = true }) { Text("全屏") }
                        TextButton(onClick = { showVideoPlayer = false; player.release() }) { Text("关闭") }
                    }
                }
            )
        }
    }

    // 长按封面 → 保存确认弹窗
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("保存封面到相册？") },
            text = { Text("是否将封面保存到系统相册？") },
            confirmButton = {
                TextButton(onClick = {
                    showSaveDialog = false
                    saveCoverToGallery(context, cover)
                }) {
                    Text("好的喵")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("不好喵")
                }
            }
        )
    }

    GlassCard(modifier = modifier) {
        Column {
            if (cover.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(cover)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .combinedClickable(
                            onClick = { if (videoUrl.isNotEmpty()) showVideoPlayer = true },
                            onLongClick = { showSaveDialog = true }
                        ),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(14.dp))
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // 笔记内容（desc），超过 2 行折叠
            if (desc.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                var isOverflowed by remember { mutableStateOf(false) }
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { result ->
                            if (!isExpanded && result.hasVisualOverflow) {
                                isOverflowed = true
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    if (isOverflowed || isExpanded) {
                        TextButton(
                            onClick = { isExpanded = !isExpanded },
                            modifier = Modifier.widthIn(min = 48.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isExpanded) "收起" else "展开",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (authorAvatar.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(authorAvatar)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = authorName.ifEmpty { "未知作者" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (contentType.isNotEmpty()) {
                    Spacer(Modifier.width(10.dp))
                    val (typeLabel, typeColor) = when (contentType) {
                        "video" -> "视频" to VideoTypeBadge
                        "image" -> "图集" to ImageTypeBadge
                        "live" -> "实况" to LiveTypeBadge
                        else -> contentType to VideoTypeBadge
                    }
                    TypeBadge(text = typeLabel, color = typeColor)
                }
                if (shareUrl.isNotEmpty()) {
                    Spacer(Modifier.weight(1f))
                    FilledTonalIconButton(
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(shareUrl))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Filled.OpenInBrowser, contentDescription = "打开原链接", modifier = Modifier.size(16.dp))
                    }
                }
            }
            // 统计数据
            if (stats != null) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(icon = Icons.Filled.Favorite, text = formatCount(stats.diggCount))
                    StatItem(icon = Icons.Filled.ChatBubbleOutline, text = formatCount(stats.commentCount))
                    StatItem(icon = Icons.Filled.BookmarkBorder, text = formatCount(stats.collectCount))
                    StatItem(icon = Icons.Filled.Share, text = formatCount(stats.shareCount))
                }
            }
        }
    }
}

// ==================== 保存封面的工具函数 ====================

private fun saveCoverToGallery(context: android.content.Context, imageUrl: String) {
    val scope = kotlinx.coroutines.MainScope()
    scope.launch {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder().url(imageUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()
            val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
            if (!response.isSuccessful) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "下载封面失败", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val bytes = withContext(Dispatchers.IO) { response.body?.bytes() } ?: run {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "下载封面失败", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val fileName = "ClipFlow_cover_${System.currentTimeMillis()}.jpg"
            val saved = withContext(Dispatchers.IO) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/ClipFlow")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                    val uri = context.contentResolver.insert(
                        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                        values
                    )
                    uri?.let {
                        context.contentResolver.openOutputStream(it)?.use { os -> os.write(bytes) }
                        values.clear()
                        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        context.contentResolver.update(it, values, null, null)
                        true
                    } ?: false
                } else {
                    val dir = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        "ClipFlow"
                    )
                    if (!dir.exists()) dir.mkdirs()
                    val file = File(dir, fileName)
                    FileOutputStream(file).use { it.write(bytes) }
                    true
                }
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    if (saved) "封面已保存到相册" else "保存封面失败",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "保存封面失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

// ==================== 下载选项卡片（抖音 + 小红书通用） ====================

@Composable
fun DownloadOptionsCard(
    items: List<ContentItem>,
    downloadStates: Map<String, DownloadState>,
    onDownload: (ContentItem) -> Unit,
    videoBackups: List<com.qihe.clipflow.data.api.model.VideoBackupItem> = emptyList(),
    onDownloadBackupUrl: ((String, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Column {
            Text(
                text = "下载选项",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))

            items.forEachIndexed { idx, item ->
                val state = downloadStates[item.id]
                val isDownloading = state?.isDownloading == true
                val progress = state?.progress ?: 0f

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (item.type) {
                            ContentType.VIDEO, ContentType.LIVE_VIDEO -> Icons.Filled.Videocam
                            else -> Icons.Filled.Image
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.description.ifEmpty { item.type.label },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        val infoParts = mutableListOf<String>()
                        item.mediaInfo?.resolution?.let { infoParts.add(it) }
                        item.mediaInfo?.codec?.let { infoParts.add(it) }
                        item.mediaInfo?.format?.let { infoParts.add(it) }
                        if (infoParts.isNotEmpty()) {
                            Text(
                                text = infoParts.joinToString(" · "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isDownloading) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                    } else {
                        TextButton(onClick = { onDownload(item) }) {
                            Text("下载", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                if (idx < items.size - 1) {
                    DashedDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }

            // 备用画质
            if (videoBackups.isNotEmpty()) {
                var showBackups by remember { mutableStateOf(false) }
                DashedDivider(modifier = Modifier.padding(vertical = 4.dp))
                TextButton(
                    onClick = { showBackups = !showBackups },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        if (showBackups) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        null, modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("备用画质 (${videoBackups.size})", style = MaterialTheme.typography.labelMedium)
                }
                AnimatedVisibility(visible = showBackups) {
                    Column {
                        videoBackups.forEach { backup ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val label = backup.quality ?: ""
                                val codec = backup.codec ?: ""
                                val res = if (backup.width > 0 && backup.height > 0) "${backup.width}×${backup.height}" else ""
                                val infoParts = listOfNotNull(label, res, codec.uppercase(), backup.format?.uppercase()).filter { it.isNotEmpty() }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(infoParts.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                                }
                                TextButton(onClick = {
                                    backup.url?.let { url ->
                                        onDownloadBackupUrl?.invoke(url, infoParts.firstOrNull() ?: "备用")
                                    }
                                }) {
                                    Text("下载", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
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
