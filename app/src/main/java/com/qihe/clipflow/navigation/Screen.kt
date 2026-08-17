package com.qihe.clipflow.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Shop
import androidx.compose.ui.graphics.vector.ImageVector
import android.net.Uri
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Douyin : Screen("douyin") {
        const val sourceUrlArgument = "sourceUrl"
        val destinationRoute = "$route?$sourceUrlArgument={$sourceUrlArgument}"

        fun withSourceUrl(sourceUrl: String): String {
            return "$route?$sourceUrlArgument=${Uri.encode(sourceUrl)}"
        }
    }
    data object Xiaohongshu : Screen("xiaohongshu") {
        const val sourceUrlArgument = "sourceUrl"
        val destinationRoute = "$route?$sourceUrlArgument={$sourceUrlArgument}"

        fun withSourceUrl(sourceUrl: String): String {
            return "$route?$sourceUrlArgument=${Uri.encode(sourceUrl)}"
        }
    }
    data object Bilibili : Screen("bilibili") {
        const val sourceUrlArgument = "sourceUrl"
        val destinationRoute = "$route?$sourceUrlArgument={$sourceUrlArgument}"

        fun withSourceUrl(sourceUrl: String): String {
            return "$route?" + sourceUrlArgument + "=" + Uri.encode(sourceUrl)
        }
    }
    data object History : Screen("history")
    data object Settings : Screen("settings")
    data object Personalization : Screen("personalization")
    data object About : Screen("about")
    data object OpenSource : Screen("open-source")
}

data class BottomNavItem(
    val label: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(
        label = "主页",
        route = Screen.Home.route,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    BottomNavItem(
        label = "抖音",
        route = Screen.Douyin.route,
        selectedIcon = Icons.Filled.MusicNote,
        unselectedIcon = Icons.Outlined.MusicNote
    ),
    BottomNavItem(
        label = "小红书",
        route = Screen.Xiaohongshu.route,
        selectedIcon = Icons.Filled.Shop,
        unselectedIcon = Icons.Outlined.Shop
    ),
    BottomNavItem(
        label = "B站",
        route = Screen.Bilibili.route,
        selectedIcon = Icons.Filled.LiveTv,
        unselectedIcon = Icons.Outlined.LiveTv
    )
)

val secondaryRoutes = setOf(
    Screen.History.route,
    Screen.Settings.route,
    Screen.Personalization.route,
    Screen.About.route,
    Screen.OpenSource.route,
)

fun isSecondaryRoute(route: String?): Boolean = route in secondaryRoutes

fun orderedBottomNavItems(
    order: List<String>,
    registeredItems: List<BottomNavItem> = bottomNavItems,
): List<BottomNavItem> {
    val distinctRegistered = registeredItems.distinctBy { it.route }
    val configured = order.mapNotNull { key ->
        distinctRegistered.find { it.route == key }
    }.distinctBy { it.route }
    val configuredRoutes = configured.map { it.route }.toSet()
    val missing = distinctRegistered.filterNot { it.route in configuredRoutes }
    return (configured + missing).ifEmpty { distinctRegistered }
}

fun primaryPageIndex(route: String?, items: List<BottomNavItem>): Int {
    return items.indexOfFirst { it.route == route }.coerceAtLeast(0)
}

fun historyPlatformRoute(platform: String): String {
    return when (platform) {
        Screen.Douyin.route -> Screen.Douyin.route
        Screen.Xiaohongshu.route -> Screen.Xiaohongshu.route
        Screen.Bilibili.route -> Screen.Bilibili.route
        else -> Screen.Home.route
    }
}

fun historyDestinationRoute(platform: String, sourceUrl: String): String {
    return when (historyPlatformRoute(platform)) {
        Screen.Douyin.route -> Screen.Douyin.withSourceUrl(sourceUrl)
        Screen.Xiaohongshu.route -> Screen.Xiaohongshu.withSourceUrl(sourceUrl)
        Screen.Bilibili.route -> Screen.Bilibili.withSourceUrl(sourceUrl)
        else -> Screen.Home.route
    }
}

data class PrimaryNavigationOptions(
    val saveState: Boolean,
    val launchSingleTop: Boolean,
    val restoreState: Boolean,
)

fun primaryNavigationOptions(route: String): PrimaryNavigationOptions {
    val hasExplicitSource = route.substringAfter("?", missingDelimiterValue = "").isNotBlank()
    return if (hasExplicitSource) {
        PrimaryNavigationOptions(
            saveState = false,
            launchSingleTop = false,
            restoreState = false,
        )
    } else {
        PrimaryNavigationOptions(
            saveState = true,
            launchSingleTop = true,
            restoreState = true,
        )
    }
}

fun NavHostController.navigateToPrimary(route: String) {
    navigate(route) {
        val options = primaryNavigationOptions(route)
        popUpTo(graph.findStartDestination().id) { saveState = options.saveState }
        launchSingleTop = options.launchSingleTop
        restoreState = options.restoreState
    }
}
