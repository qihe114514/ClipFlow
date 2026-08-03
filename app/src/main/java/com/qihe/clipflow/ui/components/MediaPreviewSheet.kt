package com.qihe.clipflow.ui.components

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.qihe.clipflow.data.api.model.ContentItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPreviewSheet(
    items: List<ContentItem>,
    onDismiss: () -> Unit
) {
    if (items.isEmpty()) return

    val context = LocalContext.current
    val activity = context as? Activity
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pagerState = rememberPagerState(pageCount = { items.size })
    var isFullscreen by remember { mutableStateOf(false) }
    var restoreOrientation by remember { mutableStateOf<Int?>(null) }
    var restoreSystemBars by remember { mutableStateOf<Boolean?>(null) }
    var failedImagePages by remember { mutableStateOf(emptySet<Int>()) }
    var controlsVisible by remember { mutableStateOf(true) }
    var showLongPressSpeed by remember { mutableStateOf(false) }
    var selectedSpeed by remember { mutableFloatStateOf(1f) }
    var savedVolume by remember { mutableFloatStateOf(1f) }
    var isMuted by remember { mutableStateOf(false) }
    var isLandscapeVideo by remember { mutableStateOf(false) }
    var videoWidth by remember { mutableIntStateOf(0) }
    var videoHeight by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var duration by remember { mutableLongStateOf(0L) }
    var position by remember { mutableLongStateOf(0L) }
    var playerError by remember { mutableStateOf<PlaybackException?>(null) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableFloatStateOf(0f) }

    val currentItem = items.getOrNull(pagerState.currentPage)
    val currentVideoUrl = currentItem
        ?.takeIf { it.previewKind == PreviewMediaKind.VIDEO }
        ?.url
    val currentAudioUrl = currentItem
        ?.takeIf { it.previewKind == PreviewMediaKind.VIDEO }
        ?.companionUrl
        ?.takeIf { it.isNotBlank() }
    val player = remember(currentVideoUrl, currentAudioUrl) {
        currentVideoUrl?.let { url ->
            val requestHeaders = if (currentAudioUrl != null) {
                mapOf(
                    "User-Agent" to BILIBILI_PREVIEW_USER_AGENT,
                    "Referer" to BILIBILI_PREVIEW_REFERER
                )
            } else {
                emptyMap()
            }
            val dataSourceFactory = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setDefaultRequestProperties(requestHeaders)
            val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
            ExoPlayer.Builder(context, mediaSourceFactory).build().apply {
                val videoSource = mediaSourceFactory.createMediaSource(MediaItem.fromUri(url))
                val source = currentAudioUrl?.let { audioUrl ->
                    MergingMediaSource(
                        videoSource,
                        mediaSourceFactory.createMediaSource(MediaItem.fromUri(audioUrl))
                    )
                } ?: videoSource
                setMediaSource(source)
                prepare()
                playWhenReady = true
            }
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(value: Boolean) {
                isPlaying = value
                controlsVisible = true
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                duration = player?.duration?.coerceAtLeast(0L) ?: 0L
                if (playbackState == Player.STATE_READY) {
                    controlsVisible = true
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                playerError = error
                controlsVisible = true
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                videoWidth = videoSize.width
                videoHeight = videoSize.height
                isLandscapeVideo = videoSize.width > videoSize.height
            }
        }
        player?.addListener(listener)
        if (player != null) {
            savedVolume = player.volume
            isMuted = player.volume == 0f
        }
        onDispose {
            player?.removeListener(listener)
            player?.release()
        }
    }

    LaunchedEffect(player) {
        isPlaying = player?.isPlaying == true
        playerError = null
        isLandscapeVideo = false
        videoWidth = 0
        videoHeight = 0
        while (isActive) {
            if (player != null) {
                isPlaying = player.isPlaying
                duration = player.duration.coerceAtLeast(0L)
                position = player.currentPosition.coerceIn(0L, duration)
            }
            delay(250)
        }
    }

    LaunchedEffect(currentItem?.id, isPlaying) {
        controlsVisible = true
        if (isPlaying) {
            delay(2800)
            controlsVisible = false
        }
    }

    DisposableEffect(isFullscreen, activity) {
        onDispose {
            if (isFullscreen) {
                restoreFullscreenState(activity, restoreOrientation, restoreSystemBars)
            }
        }
    }

    LaunchedEffect(isFullscreen, isLandscapeVideo) {
        if (isFullscreen) {
            if (isLandscapeVideo) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
            setSystemBarsVisible(activity, false)
        }
    }

    BackHandler(enabled = true) {
        isFullscreen = false
        restoreFullscreenState(activity, restoreOrientation, restoreSystemBars)
        onDismiss()
    }

    fun setFullscreen(enabled: Boolean) {
        if (enabled == isFullscreen) return
        if (enabled) {
            restoreOrientation = activity?.requestedOrientation
            restoreSystemBars = activity?.let(::areSystemBarsVisible) ?: true
            isFullscreen = true
        } else {
            isFullscreen = false
            restoreFullscreenState(activity, restoreOrientation, restoreSystemBars)
        }
    }

    fun closeSheet() {
        setFullscreen(false)
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = ::closeSheet,
        sheetState = sheetState,
        containerColor = Color.Black,
        contentColor = Color.White,
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(Color.White.copy(alpha = 0.35f), MaterialTheme.shapes.small)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val item = items[page]
                    when (item.previewKind) {
                        PreviewMediaKind.VIDEO -> {
                            VideoPreviewPage(
                                player = if (page == pagerState.currentPage) player else null,
                                videoWidth = videoWidth,
                                videoHeight = videoHeight,
                                onToggleControls = { controlsVisible = !controlsVisible },
                                onLongPress = {
                                    player?.setPlaybackSpeed(2f)
                                    showLongPressSpeed = player != null
                                },
                                onLongPressRelease = {
                                    player?.setPlaybackSpeed(selectedSpeed)
                                    showLongPressSpeed = false
                                }
                            )
                        }

                        PreviewMediaKind.IMAGE -> {
                            ImagePreviewPage(
                                url = item.url,
                                failed = page in failedImagePages,
                                onRetry = { failedImagePages = failedImagePages - page },
                                onError = { failedImagePages = failedImagePages + page }
                            )
                        }

                        null -> Unit
                    }
                }

                IconButton(
                    onClick = ::closeSheet,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "关闭")
                }

                if (items.size > 1) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${items.size}",
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = 12.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                if (currentItem?.previewKind == PreviewMediaKind.VIDEO && player != null) {
                    VideoControls(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        player = player,
                        controlsVisible = controlsVisible,
                        isPlaying = isPlaying,
                        duration = duration,
                        position = position,
                        isScrubbing = isScrubbing,
                        scrubPosition = scrubPosition,
                        isMuted = isMuted,
                        selectedSpeed = selectedSpeed,
                        isFullscreen = isFullscreen,
                        playerError = playerError,
                        onPlayPause = {
                            if (player.isPlaying) player.pause() else player.play()
                            isPlaying = player.isPlaying
                            controlsVisible = true
                        },
                        onSeekChange = {
                            isScrubbing = true
                            scrubPosition = it
                            player.seekTo(it.toLong())
                        },
                        onSeekFinished = {
                            isScrubbing = false
                            player.seekTo(scrubPosition.toLong())
                        },
                        onToggleMute = {
                            if (player.volume == 0f) {
                                player.volume = savedVolume.coerceIn(0f, 1f)
                                isMuted = false
                            } else {
                                savedVolume = player.volume
                                player.volume = 0f
                                isMuted = true
                            }
                            controlsVisible = true
                        },
                        onSpeedSelected = {
                            selectedSpeed = it
                            player.setPlaybackSpeed(it)
                            controlsVisible = true
                        },
                        onFullscreen = { setFullscreen(!isFullscreen) },
                        onRetry = {
                            playerError = null
                            player.prepare()
                            player.playWhenReady = true
                        }
                    )
                }

                if (showLongPressSpeed) {
                    Text(
                        text = "2x",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Color.Black.copy(alpha = 0.55f), MaterialTheme.shapes.medium)
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoPreviewPage(
    player: ExoPlayer?,
    videoWidth: Int,
    videoHeight: Int,
    onToggleControls: () -> Unit,
    onLongPress: () -> Unit,
    onLongPressRelease: () -> Unit
) {
    if (player == null) {
        Box(Modifier.fillMaxSize().background(Color.Black))
        return
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(player) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val longPress = awaitLongPressOrCancellation(down.id)
                    if (longPress != null) {
                        onLongPress()
                        try {
                            waitForUpOrCancellation()
                        } finally {
                            onLongPressRelease()
                        }
                    } else {
                        onToggleControls()
                    }
                }
            }
    ) {
        val aspectRatio = videoWidth.takeIf { it > 0 }
            ?.let { width -> videoHeight.takeIf { it > 0 }?.let { width.toFloat() / it } }
        PlayerSurface(
            player = player,
            surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
            modifier = if (aspectRatio != null) {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio, matchHeightConstraintsFirst = true)
                    .align(Alignment.Center)
            } else {
                Modifier.fillMaxSize()
            }
        )
    }
}

private const val BILIBILI_PREVIEW_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124 Safari/537.36"
private const val BILIBILI_PREVIEW_REFERER = "https://www.bilibili.com/"

@Composable
private fun ImagePreviewPage(
    url: String,
    failed: Boolean,
    onRetry: () -> Unit,
    onError: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (failed) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("图片加载失败", color = Color.White)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onRetry) {
                    Icon(Icons.Filled.Replay, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("重试")
                }
            }
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                onError = { onError() }
            )
        }
    }
}

@Suppress("DEPRECATION")
@Composable
private fun VideoControls(
    modifier: Modifier,
    player: ExoPlayer,
    controlsVisible: Boolean,
    isPlaying: Boolean,
    duration: Long,
    position: Long,
    isScrubbing: Boolean,
    scrubPosition: Float,
    isMuted: Boolean,
    selectedSpeed: Float,
    isFullscreen: Boolean,
    playerError: PlaybackException?,
    onPlayPause: () -> Unit,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onToggleMute: () -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onFullscreen: () -> Unit,
    onRetry: () -> Unit
) {
    AnimatedVisibility(
        visible = controlsVisible || playerError != null,
        modifier = modifier.fillMaxWidth(),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.72f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (playerError != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("播放失败", color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onRetry) { Text("重试", color = Color.White) }
                }
            }

            val sliderMax = duration.toFloat().coerceAtLeast(1f)
            val sliderValue = if (isScrubbing) scrubPosition else position.toFloat()
            Slider(
                value = sliderValue.coerceIn(0f, sliderMax),
                onValueChange = onSeekChange,
                onValueChangeFinished = onSeekFinished,
                valueRange = 0f..sliderMax,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放"
                    )
                }
                Text(
                    text = "${formatPreviewTime(sliderValue.toLong())} / ${formatPreviewTime(duration)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onToggleMute) {
                    Icon(
                        imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                        contentDescription = if (isMuted) "取消静音" else "静音"
                    )
                }
                SpeedMenu(selectedSpeed = selectedSpeed, onSpeedSelected = onSpeedSelected)
                IconButton(onClick = onFullscreen) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                        contentDescription = if (isFullscreen) "退出全屏" else "全屏"
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedMenu(
    selectedSpeed: Float,
    onSpeedSelected: (Float) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val speeds = listOf(0.5f, 1f, 1.25f, 1.5f, 2f)
    Box {
        TextButton(onClick = { expanded = true }) {
            Text("${selectedSpeed}x", color = Color.White)
            Icon(Icons.Filled.ExpandMore, contentDescription = "倍速", tint = Color.White)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            speeds.forEach { speed ->
                DropdownMenuItem(
                    text = { Text("${speed}x") },
                    onClick = {
                        expanded = false
                        onSpeedSelected(speed)
                    }
                )
            }
        }
    }
}

private fun formatPreviewTime(millis: Long): String {
    val totalSeconds = (millis.coerceAtLeast(0L) / 1000L).toInt()
    return String.format(Locale.US, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}

private fun areSystemBarsVisible(activity: Activity): Boolean =
    ViewCompat.getRootWindowInsets(activity.window.decorView)
        ?.isVisible(WindowInsetsCompat.Type.systemBars())
        ?: true

private fun setSystemBarsVisible(activity: Activity?, visible: Boolean) {
    val window = activity?.window ?: return
    val controller = WindowCompat.getInsetsController(window, window.decorView)
    controller.systemBarsBehavior =
        androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    if (visible) {
        controller.show(WindowInsetsCompat.Type.systemBars())
    } else {
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}

private fun restoreFullscreenState(
    activity: Activity?,
    orientation: Int?,
    systemBarsVisible: Boolean?
) {
    if (activity == null) return
    orientation?.let { activity.requestedOrientation = it }
    systemBarsVisible?.let { setSystemBarsVisible(activity, it) }
}
