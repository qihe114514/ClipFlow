package com.qihe.clipflow.ui.component.liquid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressiveTopBarBlurTest {
    @Test
    fun maskKeepsTheUpperHalfOpaqueAndFadesToTheBottom() {
        assertEquals(0.55f, TOP_BAR_FADE_END, 0f)
        assertEquals(96f, TOP_BAR_HEIGHT_DP, 0f)
        assertEquals(128f, TOP_BAR_BLUR_HEIGHT_DP, 0f)
        assertTrue(TOP_BAR_BLUR_RADIUS_DP > 0f)
    }
}
