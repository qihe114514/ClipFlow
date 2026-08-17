package com.qihe.clipflow.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.navigation.NavHostController
import com.qihe.clipflow.ui.bilibili.BilibiliScreen
import com.qihe.clipflow.ui.douyin.DouyinScreen
import com.qihe.clipflow.ui.home.HomeScreen
import com.qihe.clipflow.ui.xiaohongshu.XiaohongshuScreen

@Composable
fun MainPager(
    navController: NavHostController,
    pages: List<BottomNavItem>,
    pagerState: PagerState,
    stateHolder: SaveableStateHolder,
    currentRoute: String?,
    sourceUrl: String?,
    visible: Boolean,
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = if (visible) 1f else 0f },
        userScrollEnabled = visible,
        beyondViewportPageCount = 1,
        key = { page -> pages[page].route },
    ) { page ->
        val route = pages[page].route
        stateHolder.SaveableStateProvider(route) {
            when (route) {
                Screen.Home.route -> HomeScreen(navController)
                Screen.Douyin.route -> DouyinScreen(
                    sourceUrl = sourceUrl.takeIf { currentRoute == route },
                )
                Screen.Xiaohongshu.route -> XiaohongshuScreen(
                    sourceUrl = sourceUrl.takeIf { currentRoute == route },
                )
                Screen.Bilibili.route -> BilibiliScreen(
                    sourceUrl = sourceUrl.takeIf { currentRoute == route },
                )
            }
        }
    }
}
