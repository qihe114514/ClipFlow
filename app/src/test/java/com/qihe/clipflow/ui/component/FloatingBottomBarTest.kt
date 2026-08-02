package com.qihe.clipflow.ui.component

import org.junit.Assert.assertEquals
import org.junit.Test

class FloatingBottomBarTest {
    @Test
    fun indicatorValueClampsToTheAvailableTabs() {
        assertEquals(
            0f,
            indicatorValue(
                dampedValue = -1f,
                tabsCount = 3,
            ),
            0.0001f,
        )
        assertEquals(
            2f,
            indicatorValue(
                dampedValue = 3f,
                tabsCount = 3,
            ),
            0.0001f,
        )
        assertEquals(0f, indicatorValue(1f, 0), 0.0001f)
    }
}
