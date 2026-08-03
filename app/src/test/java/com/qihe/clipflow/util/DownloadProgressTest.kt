package com.qihe.clipflow.util

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadProgressTest {
    @Test
    fun fractionNeverExceedsOneWhenBytesOvershootResponseLength() {
        assertEquals(1f, DownloadProgress.fraction(downloadedBytes = 101, totalBytes = 100))
    }

    @Test
    fun fractionIsZeroWhenLengthIsUnknown() {
        assertEquals(0f, DownloadProgress.fraction(downloadedBytes = 101, totalBytes = 0))
    }
}
