package com.qihe.clipflow.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.qihe.clipflow.ui.bilibili.BilibiliScreen
import com.qihe.clipflow.ui.douyin.DouyinScreen
import com.qihe.clipflow.ui.home.HomeScreen
import com.qihe.clipflow.ui.xiaohongshu.XiaohongshuScreen
import kotlin.math.abs

@Composable
fun MainPager(
    navController: NavHostController,
    pages: List<BottomNavItem>,
    pagerState: PagerState,
    stateHolder: SaveableStateHolder,
    currentRoute: String?,
    sourceUrl: String?,
    visible: Boolean,
    interactive: Boolean = visible,
) {
    val density = LocalDensity.current
    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = if (visible) 1f else 0f },
        userScrollEnabled = interactive,
        beyondViewportPageCount = 1,
        key = { page -> pages[page].route },
    ) { page ->
        val route = pages[page].route
        val pageOffset = (
            (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            ).coerceIn(-1f, 1f)
        val distance = abs(pageOffset)
        stateHolder.SaveableStateProvider(route) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Continuous depth parallax between primary tabs.
                        val scale = 1f - 0.045f * distance
                        scaleX = scale
                        scaleY = scale
                        alpha = (1f - 0.18f * distance).coerceIn(0f, 1f)
                        translationY = with(density) { 10.dp.toPx() } * distance
                        translationX = -pageOffset * with(density) { 18.dp.toPx() }
                    }
            ) {
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
}
