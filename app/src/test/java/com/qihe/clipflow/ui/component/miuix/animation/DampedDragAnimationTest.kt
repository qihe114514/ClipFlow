package com.qihe.clipflow.ui.component.miuix.animation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DampedDragAnimationTest {
    @Test
    fun programmaticSelectionReleasesThePreviousPressedState() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val animation = DampedDragAnimation(
            animationScope = scope,
            initialValue = 0f,
            valueRange = 0f..2f,
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 1.2f,
            onDragStarted = {},
            onDragStopped = {},
            onDrag = { _: IntSize, _: Offset -> },
        )

        animation.press()
        assertTrue(animation.isPressed)

        animation.animateToValue(1f, animatePress = false)

        assertFalse(animation.isPressed)
        scope.coroutineContext.cancel()
    }
}
