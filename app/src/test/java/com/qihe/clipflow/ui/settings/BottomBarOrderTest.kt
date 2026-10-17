package com.qihe.clipflow.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class BottomBarOrderTest {
    @Test
    fun `target index clamps to the first slot`() {
        assertEquals(0, orderTargetIndex(-3f, 3))
        assertEquals(0, orderTargetIndex(0f, 3))
        assertEquals(0, orderTargetIndex(0.49f, 3))
    }

    @Test
    fun `target index rounds to the nearest slot`() {
        assertEquals(1, orderTargetIndex(0.51f, 3))
        assertEquals(2, orderTargetIndex(1.5f, 3))
        assertEquals(3, orderTargetIndex(2.51f, 3))
    }

    @Test
    fun `target index clamps to the last slot`() {
        assertEquals(3, orderTargetIndex(3.4f, 3))
        assertEquals(3, orderTargetIndex(99f, 3))
    }

    @Test
    fun `single item stays at slot zero`() {
        assertEquals(0, orderTargetIndex(0f, 0))
        assertEquals(0, orderTargetIndex(4f, 0))
    }
}
