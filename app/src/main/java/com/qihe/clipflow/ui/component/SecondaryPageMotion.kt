package com.qihe.clipflow.ui.component

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

val SecondaryPageEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
private const val EntranceDurationMillis = 300
private const val EntranceStaggerMillis = 70

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
    // Only stage the cards when a page is opened forward. On pop, the whole
    // page enters from the left via NavHost, so replaying a right-to-left card
    // entrance would fight the navigation direction.
    var hasPlayed by rememberSaveable(key, index) { mutableStateOf(false) }
    val progress = remember(key, index) { Animatable(if (hasPlayed) 1f else 0f) }
    val offset = with(LocalDensity.current) { 28.dp.toPx() }
    LaunchedEffect(key, index) {
        if (!hasPlayed) {
            delay(index * EntranceStaggerMillis.toLong())
            progress.animateTo(1f, tween(EntranceDurationMillis, easing = SecondaryPageEasing))
            hasPlayed = true
        }
    }
    graphicsLayer {
        translationX = (1f - progress.value) * offset
    }
}
