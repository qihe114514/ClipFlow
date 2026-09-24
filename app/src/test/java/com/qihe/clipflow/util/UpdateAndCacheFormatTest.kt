package com.qihe.clipflow.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateAndCacheFormatTest {

    @Test
    fun numericVersionComparisonHandlesMultiDigitSegments() {
        assertTrue(UpdateManager.compareVersion("3.10", "3.9") > 0)
        assertEquals(0, UpdateManager.compareVersion("3.9", "3.9"))
        assertTrue(UpdateManager.compareVersion("4", "3.9.9") > 0)
        assertEquals(0, UpdateManager.compareVersion("3.9.0", "3.9"))
    }

    @Test
    fun malformedVersionSegmentsFallBackToZero() {
        assertEquals(0, UpdateManager.compareVersion("abc", "0"))
        assertTrue(UpdateManager.compareVersion("3.9.1", "v3.9") > 0)
    }

    @Test
    fun cacheSizeFormattingUsesReadableUnits() {
        assertEquals("500 B", CacheCleaner.formatSize(500))
        assertEquals("1.5 KB", CacheCleaner.formatSize(1_500))
        assertEquals("2.0 MB", CacheCleaner.formatSize(2_000_000))
    }
}
