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

    @Test
    fun pagerIndicatorPositionFollowsFractionalPageOffset() {
        assertEquals(1.25f, pagerIndicatorPosition(1, 0.25f, 3), 0.0001f)
        assertEquals(0.6f, pagerIndicatorPosition(1, -0.4f, 3), 0.0001f)
    }

    @Test
    fun pagerIndicatorPositionClampsToRegisteredPageBounds() {
        assertEquals(0f, pagerIndicatorPosition(0, -0.4f, 3), 0.0001f)
        assertEquals(2f, pagerIndicatorPosition(2, 0.4f, 3), 0.0001f)
        assertEquals(0f, pagerIndicatorPosition(0, 0.4f, 1), 0.0001f)
    }

    @Test
    fun pagerIndicatorRemainsActiveUntilRouteSelectionCatchesUp() {
        assertEquals(true, pagerIndicatorPositionActive(false, 1, 0))
        assertEquals(true, pagerIndicatorPositionActive(true, 0, 0))
        assertEquals(false, pagerIndicatorPositionActive(false, 1, 1))
    }

    @Test
    fun liquidBottomBarRequiresApi33() {
        assertEquals(false, supportsLiquidBottomBar(29))
        assertEquals(false, supportsLiquidBottomBar(30))
        assertEquals(false, supportsLiquidBottomBar(32))
        assertEquals(true, supportsLiquidBottomBar(33))
        assertEquals(true, supportsLiquidBottomBar(37))
    }

}
