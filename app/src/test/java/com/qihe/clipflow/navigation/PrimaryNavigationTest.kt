package com.qihe.clipflow.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class PrimaryNavigationTest {
    @Test
    fun orderedItemsFiltersUnknownAndDuplicateKeys() {
        val routes = orderedBottomNavItems(
            listOf("xiaohongshu", "missing", "xiaohongshu", "home")
        ).map { it.route }

        assertEquals(listOf("xiaohongshu", "home", "douyin"), routes)
    }

    @Test
    fun orderedItemsFallsBackWhenConfigurationHasNoKnownKeys() {
        val routes = orderedBottomNavItems(listOf("missing")).map { it.route }

        assertEquals(bottomNavItems.map { it.route }, routes)
    }

    @Test
    fun primaryPageIndexUsesZeroForUnknownRoutes() {
        assertEquals(0, primaryPageIndex("missing", bottomNavItems))
        assertEquals(1, primaryPageIndex("douyin", bottomNavItems))
    }

}
