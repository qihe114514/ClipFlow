package com.qihe.clipflow.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrimaryNavigationTest {
    @Test
    fun orderedItemsFiltersUnknownAndDuplicateKeys() {
        val routes = orderedBottomNavItems(
            listOf("xiaohongshu", "missing", "xiaohongshu", "home")
        ).map { it.route }

        assertEquals(listOf("xiaohongshu", "home", "douyin", "bilibili"), routes)
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
    fun historyDestinationUsesTheRecordedPlatformRoute() {
        assertEquals(Screen.Bilibili.route, historyPlatformRoute("bilibili"))
        assertEquals(Screen.Douyin.route, historyPlatformRoute("douyin"))
        assertEquals(Screen.Home.route, historyPlatformRoute("unknown"))
    }

    @Test
    fun persistedThreeTabOrderStillProducesAllFourTabs() {
        assertEquals(
            listOf("home", "douyin", "xiaohongshu", "bilibili"),
            orderedBottomNavItems(listOf("home", "douyin", "xiaohongshu")).map { it.route },
        )
    }

    @Test
    fun secondaryRoutePredicateCoversOnlyDetailPages() {
        assertTrue(isSecondaryRoute(Screen.History.route))
        assertTrue(isSecondaryRoute(Screen.Settings.route))
        assertTrue(isSecondaryRoute(Screen.About.route))
        assertTrue(isSecondaryRoute(Screen.OpenSource.route))
        assertFalse(isSecondaryRoute(Screen.Home.route))
        assertFalse(isSecondaryRoute(Screen.Douyin.route))
        assertFalse(isSecondaryRoute(Screen.Bilibili.route))
    }

    @Test
    fun historyRequestsUseFreshPrimaryNavigationState() {
        val bilibili = primaryNavigationOptions(
            "bilibili?sourceUrl=https%3A%2F%2Fwww.bilibili.com%2Fvideo%2FBV1abc"
        )
        val douyin = primaryNavigationOptions(
            "douyin?sourceUrl=https%3A%2F%2Fwww.douyin.com%2Fvideo%2F123"
        )

        assertEquals(
            PrimaryNavigationOptions(
                saveState = false,
                launchSingleTop = false,
                restoreState = false,
            ),
            bilibili,
        )
        assertEquals(bilibili, douyin)
    }

    @Test
    fun tabRequestsKeepStateRestoration() {
        assertEquals(
            PrimaryNavigationOptions(
                saveState = true,
                launchSingleTop = true,
                restoreState = true,
            ),
            primaryNavigationOptions(Screen.Douyin.route),
        )
    }
}
