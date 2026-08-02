package com.qihe.clipflow.ui.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OneShotSourceUrlTest {
    @Test
    fun blankSourceIsIgnored() {
        val gate = OneShotSourceUrl()

        assertNull(gate.consume("  "))
    }

    @Test
    fun sameSourceIsConsumedOnlyOnceAcrossCompositionReentry() {
        val gate = OneShotSourceUrl()

        assertEquals("https://example.test/video", gate.consume("https://example.test/video"))
        assertNull(gate.consume(null))
        assertNull(gate.consume("https://example.test/video"))
    }

    @Test
    fun differentSourceCanBeConsumed() {
        val gate = OneShotSourceUrl()
        gate.consume("https://example.test/one")

        assertEquals("https://example.test/two", gate.consume("https://example.test/two"))
    }
}
