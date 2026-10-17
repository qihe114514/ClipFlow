package com.qihe.clipflow.navigation

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.qihe.clipflow.R
import com.qihe.clipflow.data.preferences.AppPreferences
import com.qihe.clipflow.ui.component.IntensityPreset
import com.qihe.clipflow.ui.component.LocalLiquidBackdrop
import com.qihe.clipflow.ui.component.wallpaperOpacityPercent
import com.qihe.clipflow.ui.components.LocalWallpaperEnabled
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun BackgroundWallpaperLayer(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }

    var wallpaperUri by remember { mutableStateOf("") }
    var opacityPresetKey by remember { mutableStateOf("medium") }

    LaunchedEffect(Unit) {
        wallpaperUri = prefs.wallpaperUri.first()
        opacityPresetKey = prefs.wallpaperOpacityPreset.first()
        launch { prefs.wallpaperUri.collect { wallpaperUri = it } }
        launch { prefs.wallpaperOpacityPreset.collect { opacityPresetKey = it } }
    }

    val opacityPreset = IntensityPreset.fromKey(opacityPresetKey)
    // 透明度档位=关闭 → 不显示壁纸
    val hasWallpaper = opacityPreset != IntensityPreset.OFF
    val wallpaperBackdrop = rememberLayerBackdrop()

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(wallpaperBackdrop)
        ) {
            WallpaperArtwork(
                wallpaperUri = wallpaperUri,
                opacityPreset = opacityPreset,
                modifier = Modifier.fillMaxSize(),
            )
        }

        CompositionLocalProvider(
            LocalWallpaperEnabled provides hasWallpaper,
            LocalLiquidBackdrop provides wallpaperBackdrop,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}

/** Opaque wallpaper artwork used by the root layer and secondary page covers. */
@Composable
fun WallpaperArtwork(
    wallpaperUri: String,
    opacityPreset: IntensityPreset,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val hasWallpaper = opacityPreset != IntensityPreset.OFF
    // opacityPercent: 100=完全不透明 → overlay alpha 0；越小遮罩越深
    val overlayAlpha = 1f - wallpaperOpacityPercent(opacityPreset) / 100f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (hasWallpaper) {
            val useCustom = wallpaperUri.isNotEmpty()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
            ) {
                if (useCustom) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(Uri.parse(wallpaperUri))
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    // 内置默认壁纸
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(R.drawable.default_wallpaper)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }

            // 遮罩：深色模式偏暗，浅色模式偏白；透明度由用户控制
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = overlayAlpha))
            )
        }
    }
}

/** 对比度 → ColorMatrix（已移至卡片内 Adaptive luminance glass，此函数不再使用） */
// (removed)

/** 采样壁纸（自定义 URI 或内置默认图）的平均亮度 0..1，供 GlassCard 自适应文字颜色 */
