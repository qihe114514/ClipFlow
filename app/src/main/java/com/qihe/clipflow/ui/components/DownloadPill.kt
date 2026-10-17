package com.qihe.clipflow.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qihe.clipflow.ui.component.LiquidButton
import com.kyant.shapes.Capsule

/**
 * 灵动岛药丸 — 后台下载时显示在页面左上角。
 * 与顶栏按钮共用自适应玻璃配色：有壁纸时深色玻璃 + 白字，无壁纸时实体表面 + 深色文字。
 */
@Composable
fun DownloadPill(
    progress: Float,
    speedText: String,
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val wallpaperEnabled = LocalWallpaperEnabled.current
    val isLightTheme = MaterialTheme.colorScheme.surface.luminance() >= 0.5f
    val surfaceColor = when {
        !wallpaperEnabled && isLightTheme -> Color.White.copy(alpha = 0.88f)
        !wallpaperEnabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
        else -> Color.Black.copy(alpha = 0.34f)
    }
    val contentColor = if (wallpaperEnabled) Color.White else MaterialTheme.colorScheme.onSurface

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)) +
            slideInVertically(tween(260)) { -it / 2 } +
            scaleIn(tween(260), initialScale = 0.88f),
        exit = fadeOut(tween(140)) +
            slideOutVertically(tween(180)) { -it / 2 } +
            scaleOut(tween(180), targetScale = 0.9f),
        modifier = modifier
    ) {
        LiquidButton(
            onClick = onClick,
            height = 48.dp,
            shape = { Capsule() },
            surfaceColor = surfaceColor,
            contentPadding = PaddingValues(horizontal = 18.dp),
        ) {
            CircularProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.size(24.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = contentColor.copy(alpha = 0.22f),
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
            )
            if (speedText.isNotEmpty()) {
                Text(
                    text = speedText,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.72f),
                )
            }
        }
    }
}
