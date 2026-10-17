package com.qihe.clipflow.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.kyant.backdrop.Backdrop
import com.qihe.clipflow.data.preferences.AppPreferences
import com.qihe.clipflow.ui.bilibili.BilibiliAccountAction
import com.qihe.clipflow.ui.component.LiquidBottomTab
import com.qihe.clipflow.ui.component.LiquidBottomTabs
import com.qihe.clipflow.ui.components.GlassIconButton
import com.qihe.clipflow.ui.components.withAdaptiveTitleShadow
import com.qihe.clipflow.ui.icons.ClipFlowIcons
import com.qihe.clipflow.ui.theme.ClipFlowMotion

private fun titleFor(route: String?): String = when (route) {
    Screen.Home.route -> "首页"
    Screen.Douyin.route -> "抖音解析"
    Screen.Bilibili.route -> "Bilibili"
    Screen.Xiaohongshu.route -> "小红书解析"
    Screen.History.route -> "解析历史"
    Screen.Settings.route -> "设置"
    Screen.Personalization.route -> "个性化"
    Screen.About.route -> "关于"
    Screen.OpenSource.route -> "开源与须知"
    else -> "ClipFlow"
}

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
        Screen.Personalization.route,
        Screen.About.route,
        Screen.OpenSource.route,
    )
    CenterAlignedTopAppBar(
        navigationIcon = {
            if (isDetailPage) {
                GlassIconButton(
                    icon = ClipFlowIcons.Back,
                    contentDescription = "返回",
                    onClick = navController::popBackStack,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        },
        title = {
            AnimatedContent(
                targetState = currentRoute,
                transitionSpec = {
                    (
                        fadeIn(tween(200, delayMillis = 40)) +
                            slideInVertically(
                                animationSpec = tween(
                                    durationMillis = ClipFlowMotion.StandardMillis,
                                    easing = ClipFlowMotion.EmphasizedEasing,
                                )
                            ) { height -> height / 5 }
                        ).togetherWith(fadeOut(tween(120)))
                },
                label = "topBarTitle",
            ) { route ->
                Text(
                    text = titleFor(route),
                    style = MaterialTheme.typography.titleLarge.withAdaptiveTitleShadow(),
                )
            }
        },
        actions = {
            if (!isDetailPage) {
                GlassIconButton(
                    icon = ClipFlowIcons.History,
                    contentDescription = "历史",
                    onClick = { navController.navigate(Screen.History.route) },
                )
                Spacer(Modifier.width(8.dp))
                if (currentRoute == Screen.Bilibili.route) {
                    BilibiliAccountAction()
                    Spacer(Modifier.width(8.dp))
                }
                GlassIconButton(
                    icon = ClipFlowIcons.Settings,
                    contentDescription = "设置",
                    onClick = { navController.navigate(Screen.Settings.route) },
                    modifier = Modifier.padding(end = 12.dp),
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        ),
        modifier = modifier
            .statusBarsPadding()
            .windowInsetsPadding(WindowInsets(0, 0, 0, 0))
    )
}

@Composable
fun FloatingBottomBar(
    prefs: AppPreferences,
    backdrop: Backdrop,
    primaryPagerState: PrimaryPagerState,
    onTabSelected: (Int) -> Unit,
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
        onTabSelected = onTabSelected,
        backdrop = backdrop,
        tabsCount = items.size,
    ) {
        items.forEachIndexed { index, item ->
            LiquidBottomTab(
                selected = index == primaryPagerState.selectedPage,
                onClick = { onTabSelected(index) },
            ) {
                androidx.compose.material3.Icon(
                    imageVector = item.selectedIcon,
                    contentDescription = item.label,
                    modifier = Modifier
                        .padding(bottom = 1.dp)
                        .animateTabIcon(index == primaryPagerState.selectedPage),
                )
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

@Composable
private fun Modifier.animateTabIcon(selected: Boolean): Modifier {
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = ClipFlowMotion.settleSpring(),
        label = "tabIconScale",
    )
    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (selected) 1f else 0.82f,
        animationSpec = tween(ClipFlowMotion.FastMillis),
        label = "tabIconAlpha",
    )
    return this
        .alpha(alpha)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
}
