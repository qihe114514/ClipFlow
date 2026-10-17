package com.qihe.clipflow.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * One motion vocabulary for navigation, sheets and pressed states.
 * Fast, gently damped and never bouncy enough to feel toy-like.
 */
object ClipFlowMotion {
    const val FastMillis = 160
    const val StandardMillis = 260
    const val PageMillis = 340

    val StandardEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedEasing = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)

    val standard = tween<Float>(StandardMillis, easing = StandardEasing)
    val fast = tween<Float>(FastMillis, easing = FastOutSlowInEasing)
    val page = tween<Float>(PageMillis, easing = EmphasizedEasing)

    fun <T> pressSpring() = spring<T>(
        dampingRatio = 0.72f,
        stiffness = Spring.StiffnessMediumLow,
    )

    fun <T> settleSpring() = spring<T>(
        dampingRatio = 0.9f,
        stiffness = Spring.StiffnessMedium,
    )
}
