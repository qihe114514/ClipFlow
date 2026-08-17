package com.qihe.clipflow.ui.components

import android.app.Activity
import android.graphics.Color as AndroidColor
import android.os.Build
import android.view.Window
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
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
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
import com.qihe.clipflow.data.bilibili.BilibiliSessionStore
import com.qihe.clipflow.ui.component.LiquidButton
import com.qihe.clipflow.ui.component.LocalLiquidBackdrop
import com.qihe.clipflow.ui.component.LiquidSlider
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
    var playbackEnded by remember { mutableStateOf(false) }
    var duration by remember { mutableLongStateOf(0L) }
    var position by remember { mutableLongStateOf(0L) }
    var playerError by remember { mutableStateOf<PlaybackException?>(null) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableFloatStateOf(0f) }
    val previewBackdrop = rememberLayerBackdrop()
    val scrubFinishScope = rememberCoroutineScope()

    val currentItem = items.getOrNull(pagerState.currentPage)
    val currentVideoUrl = currentItem
        ?.takeIf { it.previewKind == PreviewMediaKind.VIDEO }
        ?.let { it.previewUrl ?: it.url }
    val hasSingleFilePreview = currentItem?.previewUrl?.isNotBlank() == true
    val currentAudioUrl = currentItem
        ?.takeIf { it.previewKind == PreviewMediaKind.VIDEO && !hasSingleFilePreview }
        ?.companionUrl
        ?.takeIf { it.isNotBlank() }
    val isBilibiliVideo = currentItem?.id?.startsWith("bilibili_") == true
    val player = remember(currentVideoUrl, currentAudioUrl, isBilibiliVideo) {
        currentVideoUrl?.let { url ->
            val requestHeaders = if (isBilibiliVideo) {
                buildMap {
                    put("User-Agent", BILIBILI_PREVIEW_USER_AGENT)
                    put("Referer", BILIBILI_PREVIEW_REFERER)
                    put("Origin", BILIBILI_PREVIEW_ORIGIN)
                    BilibiliSessionStore.session()?.cookie
                        ?.takeIf { it.isNotBlank() }
                        ?.let { put("Cookie", it) }
                }
            } else {
                emptyMap()
            }
            val dataSourceFactory = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setDefaultRequestProperties(requestHeaders)
            val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
            ExoPlayer.Builder(context, mediaSourceFactory).build().apply {
                val videoItem = if (isBilibiliVideo && currentAudioUrl != null) {
                    MediaItem.Builder()
                        .setUri(url)
                        .setMimeType(MimeTypes.VIDEO_MP4)
                        .build()
                } else {
                    MediaItem.fromUri(url)
                }
                val videoSource = mediaSourceFactory.createMediaSource(videoItem)
                val source = currentAudioUrl?.let { audioUrl ->
                    MergingMediaSource(
                        videoSource,
                        mediaSourceFactory.createMediaSource(
                            MediaItem.Builder()
                                .setUri(audioUrl)
                                .setMimeType(MimeTypes.AUDIO_MP4)
                                .build()
                        )
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
                playbackEnded = playbackState == Player.STATE_ENDED
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
        playbackEnded = false
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

    DisposableEffect(activity) {
        val statusBarsVisible = activity?.let(::areStatusBarsVisible) ?: true
        setStatusBarVisible(activity, false)
        onDispose {
            setStatusBarVisible(activity, statusBarsVisible)
        }
    }

    BackHandler(enabled = true) {
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0) },
        containerColor = Color.Black,
        contentColor = Color.White,
        tonalElevation = 0.dp,
        dragHandle = null
    ) {
        PreviewDialogWindowEffect(activity)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .layerBackdrop(previewBackdrop)
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
                                    isLandscapeVideo = isLandscapeVideo,
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
                }

                CompositionLocalProvider(LocalLiquidBackdrop provides previewBackdrop) {
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
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding(),
                            player = player,
                            backdrop = previewBackdrop,
                            controlsVisible = controlsVisible,
                            isPlaying = isPlaying,
                            playbackEnded = playbackEnded,
                            duration = duration,
                            position = position,
                            isScrubbing = isScrubbing,
                            scrubPosition = scrubPosition,
                            isMuted = isMuted,
                            selectedSpeed = selectedSpeed,
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
                                player.seekTo(scrubPosition.toLong())
                                // seekTo 是异步的：若立刻复位 isScrubbing，进度条会改读
                                // 尚未更新的播放位置，滑块松手后先弹回旧位置、再跳回
                                // 目标位置（快速拖动时表现为"抖两下"）。
                                // 改为等播放器位置跟上目标后再复位。
                                scrubFinishScope.launch {
                                    val target = scrubPosition.toLong()
                                    repeat(20) { // 最多等 1 秒
                                        delay(50)
                                        if (kotlin.math.abs(player.currentPosition - target) < 200L) {
                                            // 若期间用户已开始新的拖动，交给新一次
                                            // onSeekFinished 处理，这里不复位
                                            if (scrubPosition.toLong() == target) isScrubbing = false
                                            return@launch
                                        }
                                    }
                                    if (scrubPosition.toLong() == target) isScrubbing = false
                                }
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
}

@Composable
private fun VideoPreviewPage(
    player: ExoPlayer?,
    videoWidth: Int,
    videoHeight: Int,
    isLandscapeVideo: Boolean,
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
            .clipToBounds()
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
        val shouldRotateLandscapeVideo = isLandscapeVideo && maxHeight > maxWidth
        PlayerSurface(
            player = player,
            surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
            modifier = if (aspectRatio != null) {
                if (shouldRotateLandscapeVideo) {
                    val unrotatedWidth = minOf(maxHeight, maxWidth * aspectRatio)
                    Modifier
                        .requiredWidth(unrotatedWidth)
                        .requiredHeight(unrotatedWidth / aspectRatio)
                        .align(Alignment.Center)
                        .graphicsLayer { rotationZ = 90f }
                } else {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspectRatio, matchHeightConstraintsFirst = true)
                        .align(Alignment.Center)
                }
            } else {
                Modifier.fillMaxSize()
            }
        )
    }
}

private const val BILIBILI_PREVIEW_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124 Safari/537.36"
private const val BILIBILI_PREVIEW_REFERER = "https://www.bilibili.com/"
private const val BILIBILI_PREVIEW_ORIGIN = "https://www.bilibili.com"

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
                LiquidButton(onClick = onRetry) {
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
    backdrop: Backdrop,
    controlsVisible: Boolean,
    isPlaying: Boolean,
    playbackEnded: Boolean,
    duration: Long,
    position: Long,
    isScrubbing: Boolean,
    scrubPosition: Float,
    isMuted: Boolean,
    selectedSpeed: Float,
    playerError: PlaybackException?,
    onPlayPause: () -> Unit,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onToggleMute: () -> Unit,
    onSpeedSelected: (Float) -> Unit,
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
                    LiquidButton(onClick = onRetry) { Text("重试", color = Color.White) }
                }
            }

            val sliderMax = duration.toFloat().coerceAtLeast(1f)
            val sliderValue = if (isScrubbing) scrubPosition else position.toFloat()
            LiquidSlider(
                value = {
                    (if (isScrubbing) scrubPosition else position.toFloat())
                        .coerceIn(0f, sliderMax)
                },
                onValueChange = onSeekChange,
                onValueChangeFinished = onSeekFinished,
                valueRange = 0f..sliderMax,
                visibilityThreshold = 0.001f,
                backdrop = backdrop,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiquidButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(40.dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    val icon = when {
                        isPlaying -> Icons.Filled.Pause
                        playbackEnded -> Icons.Filled.Replay
                        else -> Icons.Filled.PlayArrow
                    }
                    val description = when {
                        isPlaying -> "暂停"
                        playbackEnded -> "重播"
                        else -> "播放"
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = description
                    )
                    }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${formatPreviewTime(sliderValue.toLong())} / ${formatPreviewTime(duration)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
                Spacer(Modifier.weight(1f))
                LiquidButton(
                    onClick = onToggleMute,
                    modifier = Modifier.size(40.dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                        contentDescription = if (isMuted) "取消静音" else "静音"
                    )
                }
                Spacer(Modifier.width(8.dp))
                SpeedMenu(selectedSpeed = selectedSpeed, onSpeedSelected = onSpeedSelected)
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
        LiquidButton(
            onClick = { expanded = true },
            modifier = Modifier.height(40.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        ) {
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

private fun areStatusBarsVisible(activity: Activity): Boolean =
    ViewCompat.getRootWindowInsets(activity.window.decorView)
        ?.isVisible(WindowInsetsCompat.Type.statusBars())
        ?: true

private fun setStatusBarVisible(activity: Activity?, visible: Boolean) {
    val window = activity?.window ?: return
    val controller = WindowCompat.getInsetsController(window, window.decorView)
    controller.systemBarsBehavior =
        androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    if (visible) {
        controller.show(WindowInsetsCompat.Type.statusBars())
    } else {
        controller.hide(WindowInsetsCompat.Type.statusBars())
    }
}

@Composable
private fun PreviewDialogWindowEffect(activity: Activity?) {
    val view = LocalView.current
    val dialogWindow = (view.parent as? DialogWindowProvider)?.window

    androidx.compose.runtime.SideEffect {
        configurePreviewWindow(activity?.window)
        configurePreviewWindow(dialogWindow)
    }
}

private fun configurePreviewWindow(window: Window?) {
    window ?: return
    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.statusBarColor = AndroidColor.TRANSPARENT
    window.navigationBarColor = AndroidColor.TRANSPARENT
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.isNavigationBarContrastEnforced = false
    }
    val controller = WindowCompat.getInsetsController(window, window.decorView)
    controller.systemBarsBehavior =
        androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    controller.hide(WindowInsetsCompat.Type.statusBars())
}
