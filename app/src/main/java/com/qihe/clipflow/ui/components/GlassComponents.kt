package com.qihe.clipflow.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.shapes.RoundedRectangle
import com.qihe.clipflow.ui.component.LiquidButton
import com.qihe.clipflow.ui.component.LocalGlassStyle
import com.qihe.clipflow.ui.component.LocalLiquidBackdrop
import com.qihe.clipflow.ui.theme.ClipFlowDimens
import com.qihe.clipflow.ui.theme.ClipFlowMotion

internal val LocalWallpaperEnabled = staticCompositionLocalOf { true }
internal val LocalWallpaperIsLight = staticCompositionLocalOf { true }

fun TextStyle.withTitleShadow(): TextStyle = copy(
    shadow = Shadow(
        color = Color.Black.copy(alpha = 0.48f),
        offset = Offset(0f, 1f),
        blurRadius = 3f,
    )
)

/**
 * Titles sit directly on the wallpaper, so they need a small shadow only when
 * a wallpaper is actually visible. On a flat light background the shadow is
 * noise and hurts legibility.
 */
@Composable
fun TextStyle.withAdaptiveTitleShadow(): TextStyle =
    if (LocalWallpaperEnabled.current) withTitleShadow() else this

@Composable
fun crispOnSurface(): Color = MaterialTheme.colorScheme.onSurface

/**
 * Subtle press feedback shared by clickable glass surfaces.
 */
fun Modifier.glassPressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.978f,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = ClipFlowMotion.pressSpring(),
        label = "glassPressScale",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * 玻璃拟态卡片 — 与 AndroidLiquidGlass 的 ScrollContainer 卡片一致：
 * 24dp 大圆角，vibrancy + lens 对背后场景做实时毛玻璃折射。
 * 关闭壁纸 / 关闭玻璃时退回高不透明度实体表面，保证可读性。
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    cornerRadius: Dp = ClipFlowDimens.RadiusCard,
    content: @Composable ColumnScope.() -> Unit
) {
    val backdrop = LocalLiquidBackdrop.current ?: rememberLayerBackdrop()
    val glassStyle = LocalGlassStyle.current
    val glass = glassStyle.card
    val shape = RoundedCornerShape(cornerRadius)
    val density = LocalDensity.current
    val wallpaperEnabled = LocalWallpaperEnabled.current
    val isDarkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface

    val readabilityScrim = when {
        !wallpaperEnabled -> Color.Transparent
        isDarkSurface -> Color.Black.copy(alpha = 0.20f)
        else -> Color.White.copy(alpha = 0.30f)
    }
    val offSurfaceColor = if (isDarkSurface) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
    }
    val borderBrush = when {
        !wallpaperEnabled -> Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f),
            )
        )
        isDarkSurface -> Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.04f))
        )
        else -> Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.65f), Color.White.copy(alpha = 0.18f))
        )
    }

    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .graphicsLayer {
                shadowElevation = with(density) { if (wallpaperEnabled) 4.dp.toPx() else 1.dp.toPx() }
                this.shape = shape
                clip = false
            }
            .then(
                if (glass.isOff) {
                    Modifier.background(offSurfaceColor, shape)
                } else {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(cornerRadius) },
                        effects = {
                            colorControls(
                                contrast = glassStyle.contrast,
                                saturation = 1.45f,
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
            .border(1.dp, borderBrush, shape)
            .animateContentSize(animationSpec = ClipFlowMotion.settleSpring())
            .then(
                if (onClick != null) {
                    Modifier
                        .glassPressScale(interactionSource)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClick,
                        )
                } else Modifier
            )
            .padding(ClipFlowDimens.Space20),
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
    val shape = RoundedCornerShape(ClipFlowDimens.RadiusControl)
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    val textColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .then(
                if (glass.isOff) {
                    Modifier.background(surfaceColor.copy(alpha = 0.35f), shape)
                } else {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(ClipFlowDimens.RadiusControl) },
                        effects = {
                            colorControls(contrast = glassStyle.contrast, saturation = 1.35f)
                            if (glass.extraBlur > 0f) blur(glass.extraBlur.dp.toPx())
                            lens(glass.lensBlur.dp.toPx(), glass.refraction.dp.toPx())
                        },
                    )
                }
            )
            .background(surfaceColor.copy(alpha = 0.16f), shape)
            .clip(shape)
            .border(1.dp, borderColor, shape)
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
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
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

/**
 * Adaptive circular glass action used in the top bar.
 * - wallpaper visible: dark scrim + white glyph (works over photos)
 * - wallpaper off: tonal surface + onSurface glyph (works in light/dark themes)
 */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = ClipFlowDimens.TopBarAction,
    enabled: Boolean = true,
    tint: Color? = null,
) {
    val wallpaperEnabled = LocalWallpaperEnabled.current
    val isLightTheme = MaterialTheme.colorScheme.surface.luminance() >= 0.5f
    val surfaceColor = when {
        !wallpaperEnabled && isLightTheme -> Color.White.copy(alpha = 0.62f)
        !wallpaperEnabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
        else -> Color.Black.copy(alpha = 0.30f)
    }
    val glyphTint = tint ?: if (wallpaperEnabled) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    LiquidButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(size),
        surfaceColor = surfaceColor,
        contentPadding = PaddingValues(0.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = glyphTint,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Self-contained glass dialog surface.
 *
 * A Dialog lives in its own window, so it cannot sample the activity scene.
 * Previously the buttons inside dialogs still used the activity backdrop and
 * therefore refracted the page *behind* the dialog. This surface builds a
 * tiny local backdrop from its own brand-tinted background and provides it to
 * any LiquidButton inside, so glass effects stay inside the dialog.
 */
@Composable
fun GlassDialogCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(24.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val isLightTheme = MaterialTheme.colorScheme.surface.luminance() >= 0.5f
    val shape = RoundedCornerShape(ClipFlowDimens.RadiusDialog)
    val density = LocalDensity.current
    val backdrop = rememberLayerBackdrop()
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val borderBrush = if (isLightTheme) {
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.95f), MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        )
    } else {
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.05f))
        )
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                shadowElevation = with(density) { 24.dp.toPx() }
                this.shape = shape
                clip = false
            }
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .layerBackdrop(backdrop)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.93f),
                            )
                        ),
                        shape,
                    )
            )
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    color = primary.copy(alpha = if (isLightTheme) 0.10f else 0.16f),
                    radius = size.maxDimension * 0.52f,
                    center = Offset(size.width * 0.02f, -size.height * 0.08f),
                )
                drawCircle(
                    color = secondary.copy(alpha = if (isLightTheme) 0.08f else 0.12f),
                    radius = size.maxDimension * 0.42f,
                    center = Offset(size.width * 1.02f, size.height * 1.06f),
                )
            }
        }
        CompositionLocalProvider(LocalLiquidBackdrop provides backdrop) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .border(1.dp, borderBrush, shape)
                    .padding(contentPadding),
                content = content,
            )
        }
    }
}

@Composable
fun GlassDialogActions(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            ClipFlowDimens.Space10,
            Alignment.End,
        ),
        content = content,
    )
}

@Composable
fun DialogPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    LiquidButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        tint = MaterialTheme.colorScheme.primary,
        height = 42.dp,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
fun DialogSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    LiquidButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        surfaceColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
        height = 42.dp,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
        )
    }
}

@Composable
fun DialogDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    LiquidButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        tint = MaterialTheme.colorScheme.error,
        height = 42.dp,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onError,
        )
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
