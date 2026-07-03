package com.qihe.clipflow.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Shop
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Douyin : Screen("douyin")
    data object Xiaohongshu : Screen("xiaohongshu")
    data object History : Screen("history")
    data object Settings : Screen("settings")
    data object About : Screen("about")
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
    )
)
