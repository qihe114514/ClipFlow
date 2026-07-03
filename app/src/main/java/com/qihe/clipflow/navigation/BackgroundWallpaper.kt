package com.qihe.clipflow.navigation

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.qihe.clipflow.data.preferences.AppPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun BackgroundWallpaperLayer(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    val isDark = isSystemInDarkTheme()

    var wallpaperUri by remember { mutableStateOf("") }
    var wallpaperType by remember { mutableStateOf("image") }
    var opacityAmount by remember { mutableFloatStateOf(60f) }

    LaunchedEffect(Unit) {
        wallpaperUri = prefs.wallpaperUri.first()
        wallpaperType = prefs.wallpaperType.first()
        opacityAmount = prefs.wallpaperOpacity.first()

        launch { prefs.wallpaperUri.collect { wallpaperUri = it } }
        launch { prefs.wallpaperType.collect { wallpaperType = it } }
        launch { prefs.wallpaperOpacity.collect { opacityAmount = it } }
    }

    // opacityAmount: 10=almost transparent, 100=fully opaque wall → overlay alpha inverted
    val overlayAlpha = (1f - opacityAmount.coerceIn(10f, 100f) / 100f)

    Box(modifier = Modifier.fillMaxSize()) {
        if (wallpaperUri.isNotEmpty()) {
            val isImage = wallpaperType != "video"
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (isImage) Modifier.blur(25.dp) else Modifier)
                    .clipToBounds()
            ) {
                when (wallpaperType) {
                    "video" -> {
                        AndroidView(
                            factory = { ctx ->
                                VideoView(ctx).apply {
                                    setVideoURI(Uri.parse(wallpaperUri))
                                    setOnPreparedListener { mp ->
                                        mp.isLooping = true
                                        mp.setVolume(0f, 0f)
                                    }
                                    start()
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(Uri.parse(wallpaperUri))
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // 遮罩：深色模式偏暗，浅色模式偏白；透明度由用户控制
            val baseColor = if (isDark) Color.Black else Color.White
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(baseColor.copy(alpha = overlayAlpha))
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}
