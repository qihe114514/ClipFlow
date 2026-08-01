package com.qihe.clipflow.ui.component

import org.junit.Assert.assertEquals
import org.junit.Test

class FloatingBottomBarTest {
    @Test
    fun indicatorHandoffKeepsPagerPositionUntilInternalAnimationCatchesUp() {
        assertEquals(
            1f,
            indicatorValue(
                dampedValue = 0f,
                externalPosition = { 1f },
                externalActive = { false },
                handoffActive = true,
                tabsCount = 3,
            ),
            0.0001f,
        )
    }
}
