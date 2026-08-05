package com.qihe.clipflow.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.qihe.clipflow.data.preferences.AppPreferences
import com.qihe.clipflow.ui.bilibili.BilibiliAccountAction
import com.qihe.clipflow.ui.component.LiquidBottomTab
import com.qihe.clipflow.ui.component.LiquidBottomTabs
import com.qihe.clipflow.ui.component.LiquidButton
import com.kyant.backdrop.Backdrop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipFlowTopBar(
    currentRoute: String?,
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val isDetailPage = currentRoute in setOf(
        Screen.History.route,
        Screen.Settings.route,
        Screen.About.route,
        Screen.OpenSource.route,
    )
    CenterAlignedTopAppBar(
        navigationIcon = {
            if (isDetailPage) {
                LiquidButton(
                    onClick = navController::popBackStack,
                    modifier = Modifier.padding(start = 12.dp).size(40.dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            }
        },
        title = {
            Text(
                text = when (currentRoute) {
                    Screen.Home.route -> "ClipFlow"
                    Screen.Douyin.route -> "抖音解析"
                    Screen.Bilibili.route -> "Bilibili"
                    Screen.Xiaohongshu.route -> "小红书解析"
                    Screen.History.route -> "解析历史"
                    Screen.Settings.route -> "设置"
                    Screen.About.route -> "关于"
                    Screen.OpenSource.route -> "开源与须知"
                    else -> "ClipFlow"
                },
                style = MaterialTheme.typography.titleLarge
            )
        },
        actions = {
            if (!isDetailPage) {
                LiquidButton(
                    onClick = { navController.navigate(Screen.History.route) },
                    modifier = Modifier.size(40.dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Icon(Icons.Outlined.History, contentDescription = "历史", tint = Color.White)
                }
                Spacer(Modifier.width(4.dp))
                if (currentRoute == Screen.Bilibili.route) {
                    BilibiliAccountAction()
                    Spacer(Modifier.width(4.dp))
                }
                LiquidButton(
                    onClick = { navController.navigate(Screen.Settings.route) },
                    modifier = Modifier.padding(end = 12.dp).size(40.dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Icon(Icons.Outlined.Settings, contentDescription = "设置", tint = Color.White)
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent),
        modifier = modifier
            .statusBarsPadding()
            .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0))
    )
}

@Composable
fun FloatingBottomBar(
    prefs: AppPreferences,
    backdrop: Backdrop,
    primaryPagerState: PrimaryPagerState,
    modifier: Modifier = Modifier,
) {
    val bottomBarOrder by produceState(initialValue = listOf("home", "douyin", "xiaohongshu", "bilibili")) {
        prefs.bottomBarOrder.collect { value = it }
    }
    val items = orderedBottomNavItems(bottomBarOrder)
    if (items.isEmpty()) return

    LiquidBottomTabs(
        modifier = modifier
            .fillMaxWidth(0.8f)
            .padding(bottom = 12.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        selectedTabIndex = primaryPagerState.selectedPage,
        onTabSelected = primaryPagerState::animateToPage,
        backdrop = backdrop,
        tabsCount = items.size,
    ) {
        items.forEachIndexed { index, item ->
            LiquidBottomTab(
                onClick = { primaryPagerState.animateToPage(index) },
            ) {
                Icon(item.selectedIcon, contentDescription = item.label)
                Text(
                    text = item.label,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                )
            }
        }
    }
}
