package com.qihe.clipflow.ui.component.liquid

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.BackdropEffectScope
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.runtimeShaderEffect

internal const val PROGRESSIVE_BLUR_BASE_STRENGTH = 0.55f

internal fun combineProgressiveBlurStrength(base: Float, interaction: Float): Float =
    (base + interaction).coerceIn(0f, 1f)

internal fun supportsProgressiveContentBlur(sdkInt: Int): Boolean = sdkInt >= 33

@Composable
internal fun rememberProgressiveContentBackdrop(): LayerBackdrop? {
    return if (supportsProgressiveContentBlur(Build.VERSION.SDK_INT)) {
        rememberLayerBackdrop()
    } else {
        null
    }
}

@Composable
fun ProgressiveContentBlur(
    backdrop: Backdrop?,
    strength: Float,
    fallbackColor: Color,
    modifier: Modifier = Modifier,
) {
    val clampedStrength = strength.coerceIn(0f, 1f)
    if (clampedStrength <= 0f) return

    val effectModifier = if (
        backdrop != null && supportsProgressiveContentBlur(Build.VERSION.SDK_INT)
    ) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { RectangleShape },
            effects = { progressiveBlur(clampedStrength) },
            onDrawSurface = { drawRect(Color.Transparent) },
        )
    } else {
        Modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    fallbackColor.copy(alpha = 0.28f * clampedStrength),
                    fallbackColor.copy(alpha = 0.14f * clampedStrength),
                    Color.Transparent,
                )
            )
        )
    }

    Box(modifier.fillMaxSize().then(effectModifier))
}

private fun BackdropEffectScope.progressiveBlur(strength: Float) {
    if (!isRuntimeShaderSupported()) return

    val scale = downscaleFactor.coerceAtLeast(1).toFloat()
    val maxRadius = 26f / scale
    padding = maxOf(padding, maxRadius)

    runtimeShaderEffect(
        key = "ClipFlowProgressiveContentBlur",
        shaderString = PROGRESSIVE_BLUR_SHADER,
        uniformShaderName = "content",
    ) {
        setFloatUniform("size", size.width / scale, size.height / scale)
        setFloatUniform("strength", strength)
    }
}

private const val PROGRESSIVE_BLUR_SHADER = """
uniform shader content;
uniform float2 size;
uniform float strength;

half4 main(float2 coord) {
    float height = max(size.y * 0.55, 1.0);
    float falloff = 1.0 - smoothstep(0.0, height, coord.y);
    float radius = 26.0 * strength * falloff;
    if (radius <= 0.01) return content.eval(coord);

    float2 horizontal = float2(radius, 0.0);
    float2 vertical = float2(0.0, radius);
    float2 diagonal = float2(radius * 0.7071, radius * 0.7071);

    half4 color = content.eval(coord) * 0.20;
    color += content.eval(coord + horizontal) * 0.10;
    color += content.eval(coord - horizontal) * 0.10;
    color += content.eval(coord + vertical) * 0.10;
    color += content.eval(coord - vertical) * 0.10;
    color += content.eval(coord + diagonal) * 0.10;
    color += content.eval(coord - diagonal) * 0.10;
    color += content.eval(coord + float2(diagonal.x, -diagonal.y)) * 0.10;
    color += content.eval(coord + float2(-diagonal.x, diagonal.y)) * 0.10;
    return color;
}
"""
