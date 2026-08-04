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

internal const val BOTTOM_BLUR_HEIGHT_DP = 160f
internal const val DETAIL_BOTTOM_BLUR_HEIGHT_DP = 96f

private const val PROGRESSIVE_BOTTOM_SHADER = """
uniform shader content;
uniform float2 size;

half4 main(float2 coord) {
    float mask = smoothstep(0.0, size.y * 0.5, coord.y);
    return content.eval(coord) * mask;
}
"""

@Composable
internal fun ProgressiveBottomBlur(
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
                        key = "progressiveBottomBlur",
                        shaderString = PROGRESSIVE_BOTTOM_SHADER,
                        uniformShaderName = "content",
                    ) {
                        setFloatUniform("size", size.width, size.height)
                    }
                },
            )
    )
}
