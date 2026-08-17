package com.qihe.clipflow.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.Capsule

/**
 * 液态玻璃开关（参照 AndroidLiquidGlass 示例 app 的 LiquidToggle）：
 * 轨道为毛玻璃胶囊，开启时轨道着色、旋钮滑动到右侧。
 */
@Composable
fun LiquidToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val backdrop = LocalLiquidBackdrop.current ?: rememberLayerBackdrop()
    val knobProgress = remember { Animatable(if (checked) 1f else 0f) }
    LaunchedEffect(checked) {
        knobProgress.animateTo(
            targetValue = if (checked) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 380f),
        )
    }
    val density = LocalDensity.current
    val trackColor = if (checked) Color(0xFF0088FF) else Color.White.copy(alpha = 0.35f)

    BoxWithConstraints(
        modifier = modifier
            .width(52.dp)
            .height(32.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule() },
                effects = {
                    vibrancy()
                    blur(8.dp.toPx())
                    lens(12.dp.toPx(), 16.dp.toPx())
                },
                onDrawSurface = { drawRect(trackColor.copy(alpha = 0.9f)) },
            )
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart,
    ) {
        val knobSizePx = with(density) { 24.dp.toPx() }
        val travelPx = with(density) { maxWidth.toPx() - knobSizePx - 8.dp.toPx() }
        Box(
            modifier = Modifier
                .padding(start = 4.dp)
                .graphicsLayer { translationX = travelPx * knobProgress.value }
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.92f))
        )
    }
}
