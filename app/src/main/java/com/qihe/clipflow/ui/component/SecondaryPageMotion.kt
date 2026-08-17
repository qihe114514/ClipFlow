package com.qihe.clipflow.ui.component

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

val SecondaryPageEasing = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)
private const val EntranceDurationMillis = 260
private const val EntranceStaggerMillis = 90

@Composable
fun SecondaryPageSection(
    index: Int,
    key: Any? = Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    androidx.compose.foundation.layout.Box(modifier = modifier.secondaryPageEntrance(index, key)) {
        content()
    }
}

fun Modifier.secondaryPageEntrance(index: Int, key: Any? = Unit): Modifier = composed {
    val progress = remember(key, index) { Animatable(0f) }
    val offset = with(LocalDensity.current) { 28.dp.toPx() }
    LaunchedEffect(key, index) {
        delay(index * EntranceStaggerMillis.toLong())
        progress.animateTo(1f, tween(EntranceDurationMillis, easing = SecondaryPageEasing))
    }
    graphicsLayer {
        translationX = (1f - progress.value) * offset
    }
}
