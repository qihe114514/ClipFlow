package com.qihe.clipflow.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import com.qihe.clipflow.data.preferences.AppPreferences
import com.qihe.clipflow.ui.component.FloatingBottomBar as LiquidFloatingBottomBar
import com.qihe.clipflow.ui.component.FloatingBottomBarItem
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipFlowTopBar(currentRoute: String?, navController: NavHostController) {
    val isDetailPage = currentRoute in setOf(Screen.History.route, Screen.Settings.route, Screen.About.route)
    CenterAlignedTopAppBar(
        navigationIcon = {
            if (isDetailPage) {
                IconButton(onClick = navController::popBackStack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            }
        },
        title = {
            Text(
                text = when (currentRoute) {
                    Screen.Home.route -> "ClipFlow"
                    Screen.Douyin.route -> "抖音解析"
                    Screen.Xiaohongshu.route -> "小红书解析"
                    Screen.History.route -> "解析历史"
                    Screen.Settings.route -> "设置"
                    Screen.About.route -> "关于"
                    else -> "ClipFlow"
                },
                style = MaterialTheme.typography.titleLarge
            )
        },
        actions = {
            if (!isDetailPage) {
                IconButton(onClick = { navController.navigate(Screen.History.route) }) {
                    Icon(Icons.Outlined.History, contentDescription = "历史")
                }
                IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                    Icon(Icons.Outlined.Settings, contentDescription = "设置")
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent),
        modifier = Modifier.statusBarsPadding().windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0))
    )
}

@Composable
fun FloatingBottomBar(
    navController: NavHostController,
    currentDestination: NavDestination?,
    prefs: AppPreferences,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val bottomBarOrder by produceState(initialValue = listOf("home", "douyin", "xiaohongshu")) {
        prefs.bottomBarOrder.collect { value = it }
    }
    val items = bottomBarOrder.mapNotNull { key -> bottomNavItems.find { it.route == key } }
    if (items.isEmpty()) return

    val selectedIndex = items.indexOfFirst { item ->
        currentDestination?.hierarchy?.any { it.route?.substringBefore("?") == item.route } == true
    }.coerceAtLeast(0)
    val selectedIndexState = rememberUpdatedState(selectedIndex)
    val selectedIndexProvider = remember { { selectedIndexState.value } }

    fun navigateTo(item: BottomNavItem?) {
        if (item == null) return
        navController.navigate(item.route) {
            launchSingleTop = true
            restoreState = true
        }
    }

    LiquidFloatingBottomBar(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .padding(bottom = 12.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        selectedIndex = selectedIndexProvider,
        onSelected = { index -> navigateTo(items.getOrNull(index)) },
        backdrop = backdrop,
        tabsCount = items.size,
        isBlurEnabled = true,
    ) {
        items.forEach { item ->
            FloatingBottomBarItem(
                onClick = { navigateTo(item) },
                modifier = Modifier.defaultMinSize(minWidth = 76.dp),
            ) {
                MiuixIcon(
                    imageVector = item.selectedIcon,
                    contentDescription = item.label,
                    tint = MiuixTheme.colorScheme.onSurface,
                )
                MiuixText(
                    text = item.label,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible,
                )
            }
        }
    }
}
