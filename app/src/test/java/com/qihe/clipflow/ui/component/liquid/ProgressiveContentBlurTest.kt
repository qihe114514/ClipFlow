package com.qihe.clipflow.ui.component.liquid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressiveContentBlurTest {
    @Test
    fun strengthIsClampedToTheSupportedRange() {
        assertEquals(0f, combineProgressiveBlurStrength(-1f, -1f), 0.0001f)
        assertEquals(0.7f, combineProgressiveBlurStrength(0.35f, 0.35f), 0.0001f)
        assertEquals(1f, combineProgressiveBlurStrength(0.8f, 0.8f), 0.0001f)
    }

    @Test
    fun progressiveBlurRequiresApi33() {
        assertFalse(supportsProgressiveContentBlur(32))
        assertTrue(supportsProgressiveContentBlur(33))
    }
}
