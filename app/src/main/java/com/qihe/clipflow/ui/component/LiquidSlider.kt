package com.qihe.clipflow.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LiquidSlider(
    value: () -> Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit = {},
    valueRange: ClosedFloatingPointRange<Float>,
    visibilityThreshold: Float,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
) {
    val isLightTheme = !isSystemInDarkTheme()
    val accentColor = if (isLightTheme) Color(0xFF0088FF) else Color(0xFF0091FF)
    val trackColor = if (isLightTheme) {
        Color(0xFF787878).copy(alpha = 0.2f)
    } else {
        Color(0xFF787880).copy(alpha = 0.36f)
    }
    val trackBackdrop = rememberLayerBackdrop()
    val currentValue = rememberUpdatedState(value)

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart,
    ) {
        val trackWidth = constraints.maxWidth
        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        val didDrag = remember { mutableStateOf(false) }
        val isDragging = remember { mutableStateOf(false) }
        val dragTarget = remember { mutableFloatStateOf(value()) }

        lateinit var dampedDragAnimation: DampedDragAnimation

        fun updateDragTarget(value: Float) {
            dragTarget.floatValue = value.coerceIn(valueRange)
            dampedDragAnimation.updateValue(dragTarget.floatValue)
            onValueChange(dragTarget.floatValue)
        }

        dampedDragAnimation = remember(animationScope, valueRange) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = value(),
                valueRange = valueRange,
                visibilityThreshold = visibilityThreshold,
                initialScale = 1f,
                pressedScale = 1.5f,
                onDragStarted = {
                    isDragging.value = true
                    didDrag.value = false
                    dragTarget.floatValue = targetValue
                },
                onDragStopped = {
                    if (didDrag.value) onValueChange(dragTarget.floatValue)
                    isDragging.value = false
                },
                onDrag = { _, dragAmount ->
                    if (!didDrag.value) didDrag.value = dragAmount.x != 0f
                    val delta = (valueRange.endInclusive - valueRange.start) *
                        (dragAmount.x / trackWidth)
                    val nextValue = if (isLtr) {
                        dragTarget.floatValue + delta
                    } else {
                        dragTarget.floatValue - delta
                    }
                    updateDragTarget(nextValue)
                },
            )
        }

        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { currentValue.value() }.collectLatest { externalValue ->
                if (!isDragging.value && dampedDragAnimation.targetValue != externalValue) {
                    dragTarget.floatValue = externalValue
                    dampedDragAnimation.updateValue(externalValue)
                }
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .layerBackdrop(trackBackdrop),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .clip(Capsule())
                    .background(trackColor)
                    .pointerInput(animationScope) {
                        detectTapGestures { position ->
                            val delta = (valueRange.endInclusive - valueRange.start) *
                                (position.x / trackWidth)
                            val targetValue = (
                                if (isLtr) valueRange.start + delta
                                else valueRange.endInclusive - delta
                            ).coerceIn(valueRange)
                            dampedDragAnimation.animateToValue(targetValue)
                            onValueChange(targetValue)
                            onValueChangeFinished()
                        }
                    }
                    .height(6.dp)
                    .fillMaxWidth()
            )

            Box(
                Modifier
                    .clip(Capsule())
                    .background(accentColor)
                    .height(6.dp)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val width = (constraints.maxWidth * dampedDragAnimation.progress)
                            .fastRoundToInt()
                        layout(width, placeable.height) { placeable.place(0, 0) }
                    }
            )
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .pointerInput(animationScope, trackWidth) {
                    val touchSlopPx = viewConfiguration.touchSlop
                    awaitEachGesture {
                        // 按下即消费并持续接管所有移动：外层滚动容器永远无法
                        // 通过触摸 slop，拖动滑块时页面不会跟着上下滚动
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        dampedDragAnimation.press()
                        var pointer = down.id
                        var accumulated = Offset.Zero
                        var dragStarted = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.fastFirstOrNull { it.id == pointer } ?: break
                            if (change.changedToUpIgnoreConsumed()) break
                            if (change.isConsumed) break
                            // 先计算位移再消费事件：consume 之后 positionChange() 会返回 0，
                            // 导致滑块拖动永远无法启动（同时保持对外层滚动容器的锁定）
                            val dragAmount = change.positionChange()
                            change.consume()
                            accumulated += dragAmount
                            if (!dragStarted) {
                                // 累计位移超过触摸 slop 才进入拖动（轻点不改变值）
                                if (kotlin.math.abs(accumulated.x) >= touchSlopPx ||
                                    kotlin.math.abs(accumulated.y) >= touchSlopPx
                                ) {
                                    dragStarted = true
                                    isDragging.value = true
                                    didDrag.value = true
                                }
                            }
                            if (dragStarted) {
                                val delta = (valueRange.endInclusive - valueRange.start) *
                                    (dragAmount.x / trackWidth)
                                val nextValue = if (isLtr) {
                                    dragTarget.floatValue + delta
                                } else {
                                    dragTarget.floatValue - delta
                                }
                                updateDragTarget(nextValue)
                            }
                            pointer = change.id
                        }
                        if (dragStarted) {
                            onValueChangeFinished()
                            isDragging.value = false
                        }
                        dampedDragAnimation.release()
                    }
                }
        )

        Box(
            Modifier
                .graphicsLayer {
                    translationX = (
                        -size.width / 2f + trackWidth * dampedDragAnimation.progress
                    ).fastCoerceIn(
                        -size.width / 4f,
                        trackWidth - size.width * 3f / 4f,
                    ) * if (isLtr) 1f else -1f
                }
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(
                        backdrop,
                        rememberBackdrop(trackBackdrop) { drawTrackBackdrop ->
                            val progress = dampedDragAnimation.pressProgress
                            val scaleX = lerp(2f / 3f, 1f, progress)
                            val scaleY = lerp(0f, 1f, progress)
                            scale(scaleX, scaleY) {
                                drawTrackBackdrop()
                            }
                        },
                    ),
                    shape = { Capsule() },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        blur(8.dp.toPx() * (1f - progress))
                        lens(
                            10.dp.toPx() * progress,
                            14.dp.toPx() * progress,
                            chromaticAberration = true,
                        )
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Ambient.copy(
                            width = Highlight.Ambient.width / 1.5f,
                            blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                            alpha = progress,
                        )
                    },
                    shadow = {
                        Shadow(radius = 4.dp, color = Color.Black.copy(alpha = 0.05f))
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(radius = 4.dp * progress, alpha = progress)
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val progress = dampedDragAnimation.pressProgress
                        drawRect(Color.White.copy(alpha = 1f - progress))
                    },
                )
                .size(40.dp, 24.dp)
        )
    }
}
