package com.qihe.clipflow.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

class BottomBarOrderParsingTest {

    private val defaults = listOf("home", "douyin", "xiaohongshu", "bilibili")

    @Test
    fun nullOrBlankFallsBackToDefaults() {
        assertEquals(defaults, parseBottomBarOrder(null))
        assertEquals(defaults, parseBottomBarOrder("   "))
    }

    @Test
    fun malformedJsonFallsBackToDefaults() {
        assertEquals(defaults, parseBottomBarOrder("not-json"))
        assertEquals(defaults, parseBottomBarOrder("{\"route\":\"home\"}"))
        assertEquals(defaults, parseBottomBarOrder("[\"home\","))
    }

    @Test
    fun emptyArrayFallsBackToDefaults() {
        assertEquals(defaults, parseBottomBarOrder("[]"))
    }

    @Test
    fun validOrderIsPreserved() {
        assertEquals(
            listOf("bilibili", "home", "douyin"),
            parseBottomBarOrder("[\"bilibili\",\"home\",\"douyin\"]"),
        )
    }

    @Test
    fun duplicatesAndBlankEntriesAreRemoved() {
        assertEquals(
            listOf("home", "douyin"),
            parseBottomBarOrder("[\"home\",\" \",\"home\",\"douyin\"]"),
        )
    }
}
