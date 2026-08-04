package com.qihe.clipflow.ui.component.liquid

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.runtimeShaderEffect

internal const val TOP_BAR_HEIGHT_DP = 96f
internal const val TOP_BAR_BLUR_HEIGHT_DP = 128f
internal const val PROGRESSIVE_TOPBAR_CONTENT_START_DP = TOP_BAR_HEIGHT_DP
internal const val TOP_BAR_BLUR_RADIUS_DP = 22f
internal const val TOP_BAR_FADE_END = 0.55f

private const val PROGRESSIVE_TOP_BAR_SHADER = """
uniform shader content;
uniform float2 size;
uniform float fadeEnd;

half4 main(float2 coord) {
    float mask = 1.0 - smoothstep(size.y * fadeEnd, size.y, coord.y);
    return content.eval(coord) * mask;
}
"""

@Composable
internal fun ProgressiveTopBarBlur(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .drawPlainBackdrop(
                backdrop = backdrop,
                shape = { RectangleShape },
                effects = {
                    blur(TOP_BAR_BLUR_RADIUS_DP.dp.toPx())
                    runtimeShaderEffect(
                        key = "progressiveTopBarBlur",
                        shaderString = PROGRESSIVE_TOP_BAR_SHADER,
                        uniformShaderName = "content",
                    ) {
                        setFloatUniform("size", size.width, size.height)
                        setFloatUniform("fadeEnd", TOP_BAR_FADE_END)
                    }
                },
            )
    )
}
