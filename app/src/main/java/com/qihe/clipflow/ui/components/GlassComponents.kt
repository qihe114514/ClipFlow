package com.qihe.clipflow.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.qihe.clipflow.ui.component.LiquidButton
import com.qihe.clipflow.ui.component.LocalGlassStyle
import com.qihe.clipflow.ui.component.LocalLiquidBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.shapes.RoundedRectangle

internal val LocalWallpaperEnabled = staticCompositionLocalOf { true }

fun TextStyle.withTitleShadow(): TextStyle = copy(
    shadow = Shadow(
        color = Color.Black.copy(alpha = 0.48f),
        offset = Offset(0f, 1f),
        blurRadius = 3f,
    )
)

/**
 * 玻璃拟态卡片 — 与 AndroidLiquidGlass 的 ScrollContainer 卡片一致：
 * 32dp 大圆角，vibrancy + lens 对背后场景做实时毛玻璃折射
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    cornerRadius: Dp = 32.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val backdrop = LocalLiquidBackdrop.current ?: rememberLayerBackdrop()
    val glassStyle = LocalGlassStyle.current
    val glass = glassStyle.card
    val shape = RoundedCornerShape(cornerRadius)
    val density = LocalDensity.current
    val isDarkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val readabilityScrim = if (LocalWallpaperEnabled.current) {
        if (isDarkSurface) Color.Black.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.28f)
    } else Color.Transparent
    val offSurfaceColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    Column(
            modifier = modifier
                .graphicsLayer {
                    shadowElevation = with(density) { 3.dp.toPx() }
                    this.shape = shape
                    clip = false
                }
                .then(
                    if (glass.isOff) {
                        // 关闭档：无玻璃效果，用半透明表面色兜底保证可读性
                        Modifier.background(offSurfaceColor, shape)
                    } else {
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(cornerRadius) },
                            effects = {
                                colorControls(
                                    contrast = glassStyle.contrast,
                                    saturation = 1.5f,
                                )
                                if (glass.extraBlur > 0f) {
                                    blur(glass.extraBlur.dp.toPx())
                                }
                                lens(glass.lensBlur.dp.toPx(), glass.refraction.dp.toPx())
                            },
                        )
                    }
                )
                .background(readabilityScrim, shape)
                .clip(shape)
                .then(
                    if (onClick != null) Modifier.clickable(onClick = onClick)
                    else Modifier
                )
                .padding(20.dp),
                content = content
    )
}
/**
 * 玻璃拟态输入框
 */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    readOnly: Boolean = false,
    singleLine: Boolean = true
) {
    val backdrop = LocalLiquidBackdrop.current ?: rememberLayerBackdrop()
    val glassStyle = LocalGlassStyle.current
    val glass = glassStyle.card
    val shape = RoundedCornerShape(16.dp)
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val textColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(16.dp) },
                effects = {
                    if (!glass.isOff) {
                        colorControls(contrast = glassStyle.contrast, saturation = 1.5f)
                        if (glass.extraBlur > 0f) blur(glass.extraBlur.dp.toPx())
                        lens(glass.lensBlur.dp.toPx(), glass.refraction.dp.toPx())
                    }
                },
            )
            .background(surfaceColor.copy(alpha = 0.16f), shape)
            .clip(shape)
            .border(0.5.dp, borderColor, shape)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            readOnly = readOnly,
            singleLine = singleLine,
            textStyle = TextStyle(
                color = textColor,
                fontSize = MaterialTheme.typography.bodyLarge.fontSize
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(
                MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary
) {
    LiquidButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        tint = containerColor,
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium, color = Color.White)
    }
}

@Composable
fun TypeBadge(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
        )
    }
}

@Composable
fun AttributeChip(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
        )
    }
}

@Composable
fun DashedDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
) {
    Canvas(modifier = modifier.fillMaxWidth().height(1.dp)) {
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
        )
    }
}
