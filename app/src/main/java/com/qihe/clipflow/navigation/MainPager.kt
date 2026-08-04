package com.qihe.clipflow.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.qihe.clipflow.ui.bilibili.BilibiliScreen
import com.qihe.clipflow.ui.douyin.DouyinScreen
import com.qihe.clipflow.ui.home.HomeScreen
import com.qihe.clipflow.ui.xiaohongshu.XiaohongshuScreen
import top.yukonga.miuix.kmp.blur.Backdrop

@Composable
fun MainPager(
    navController: NavHostController,
    pages: List<BottomNavItem>,
    pagerState: PagerState,
    stateHolder: SaveableStateHolder,
    currentRoute: String?,
    sourceUrl: String?,
    contentBackdrop: Backdrop?,
    contentBlurStrength: Float,
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 1,
        key = { page -> pages[page].route },
    ) { page ->
        val route = pages[page].route
        stateHolder.SaveableStateProvider(route) {
            when (route) {
                Screen.Home.route -> HomeScreen(navController)
                Screen.Douyin.route -> DouyinScreen(
                    sourceUrl = sourceUrl.takeIf { currentRoute == route },
                    contentBackdrop = contentBackdrop,
                    contentBlurStrength = contentBlurStrength,
                )
                Screen.Xiaohongshu.route -> XiaohongshuScreen(
                    sourceUrl = sourceUrl.takeIf { currentRoute == route },
                    contentBackdrop = contentBackdrop,
                    contentBlurStrength = contentBlurStrength,
                )
                Screen.Bilibili.route -> BilibiliScreen(
                    sourceUrl = sourceUrl.takeIf { currentRoute == route },
                    contentBackdrop = contentBackdrop,
                    contentBlurStrength = contentBlurStrength,
                )
            }
        }
    }
}
