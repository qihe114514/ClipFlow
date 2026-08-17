package com.qihe.clipflow.navigation

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kyant.backdrop.backdrops.layerBackdrop as contentLayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop as rememberContentLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.qihe.clipflow.data.preferences.AppPreferences
import com.qihe.clipflow.ui.components.PrivacyConsentDialog
import com.qihe.clipflow.ui.components.DownloadPill
import com.qihe.clipflow.ClipFlowApp
import com.qihe.clipflow.ui.components.DownloadPillState
import com.qihe.clipflow.ui.component.LocalLiquidBackdrop
import com.qihe.clipflow.ui.component.liquid.ProgressiveTopBarBlur
import com.qihe.clipflow.ui.component.liquid.ProgressiveBottomBlur
import com.qihe.clipflow.ui.component.liquid.BOTTOM_BLUR_HEIGHT_DP
import com.qihe.clipflow.ui.component.liquid.DETAIL_BOTTOM_BLUR_HEIGHT_DP
import com.qihe.clipflow.ui.component.liquid.TOP_BAR_BLUR_HEIGHT_DP
import com.qihe.clipflow.ui.component.liquid.PROGRESSIVE_TOPBAR_CONTENT_START_DP
import com.qihe.clipflow.ui.history.HistoryScreen
import com.qihe.clipflow.ui.settings.SettingsScreen
import com.qihe.clipflow.ui.personalization.PersonalizationScreen
import com.qihe.clipflow.ui.about.AboutScreen
import com.qihe.clipflow.ui.about.OpenSourceScreen
import com.qihe.clipflow.ui.component.ContrastPreset
import com.qihe.clipflow.ui.component.GlassStyle
import com.qihe.clipflow.ui.component.GlassTarget
import com.qihe.clipflow.ui.component.IntensityPreset
import com.qihe.clipflow.ui.component.LocalGlassStyle
import com.qihe.clipflow.ui.component.blurExtraFor
import com.qihe.clipflow.ui.component.glassEffectsFor
import com.qihe.clipflow.ui.component.wallpaperContrastScale
import com.qihe.clipflow.ui.component.SecondaryPageEasing
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun ClipFlowNavHost() {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route?.substringBefore("?")
    val currentRouteState = rememberUpdatedState(currentRoute)
    val secondaryRoute = isSecondaryRoute(currentRoute)
    var predictiveBackProgress by remember { mutableFloatStateOf(0f) }
    val settledBackProgress by animateFloatAsState(
        targetValue = predictiveBackProgress,
        animationSpec = tween(120, easing = SecondaryPageEasing),
        label = "predictiveBackProgress",
    )

    // ========== 隐私政策同意检查 ==========
    var isPrivacyCheckReady by remember { mutableStateOf(false) }
    val privacyAgreed by produceState(initialValue = false) {
        prefs.privacyAgreed.collect { value = it }
    }

    LaunchedEffect(privacyAgreed) {
        isPrivacyCheckReady = true
        if (privacyAgreed) {
            (context.applicationContext as ClipFlowApp).initUmengIfNeeded()
        }
    }

    if (!privacyAgreed && isPrivacyCheckReady) {
        val scope = rememberCoroutineScope()
        PrivacyConsentDialog(
            onAgree = {
                scope.launch {
                    prefs.setPrivacyAgreed(true)
                }
            },
            onDisagree = {
                (context as? android.app.Activity)?.finishAffinity()
            }
        )
    }


    val defaultPage by produceState(initialValue = "home") {
        value = prefs.defaultPage.first()
    }

    val bottomBarOrder by produceState(initialValue = listOf("home", "douyin", "xiaohongshu", "bilibili")) {
        prefs.bottomBarOrder.collect { value = it }
    }

    val primaryItems = remember(bottomBarOrder) {
        orderedBottomNavItems(bottomBarOrder)
    }
    val primaryRoutes = remember(primaryItems) { primaryItems.map { it.route } }
    val showBottomBar = currentRoute in primaryRoutes
    val showTopBar = currentRoute != null
    val showTopBarBlur = showTopBar && currentRoute != Screen.History.route
    val showDetailBottomBlur = currentRoute in setOf(
        Screen.History.route,
        Screen.Settings.route,
        Screen.Personalization.route,
    )
    val pagerState = rememberPagerState(
        initialPage = primaryPageIndex(currentRoute ?: defaultPage, primaryItems),
        pageCount = { primaryItems.size },
    )
    val primaryPagerState = rememberPrimaryPagerState(pagerState)
    val pagerStateHolder = rememberSaveableStateHolder()
    val currentSourceUrl = when (currentRoute) {
        Screen.Douyin.route -> navBackStackEntry?.arguments?.getString(Screen.Douyin.sourceUrlArgument)
        Screen.Xiaohongshu.route -> navBackStackEntry?.arguments?.getString(Screen.Xiaohongshu.sourceUrlArgument)
        Screen.Bilibili.route -> navBackStackEntry?.arguments?.getString(Screen.Bilibili.sourceUrlArgument)
        else -> null
    }

    LaunchedEffect(currentRoute, primaryRoutes) {
        val targetPage = primaryRoutes.indexOf(currentRoute)
        if (targetPage >= 0 && pagerState.settledPage != targetPage) {
            primaryPagerState.animateToPage(targetPage)
        }
    }

    LaunchedEffect(primaryPagerState, pagerState.currentPage) {
        primaryPagerState.syncPage()
    }

    LaunchedEffect(pagerState, primaryRoutes) {
        snapshotFlow { pagerState.settledPage }
            .drop(1)
            .distinctUntilChanged()
            .collectLatest { page ->
                val route = primaryRoutes.getOrNull(page)
                val routeNow = currentRouteState.value
                if (route != null && routeNow != null && routeNow in primaryRoutes && routeNow != route) {
                    navController.navigateToPrimary(route)
                }
            }
    }

    val bottomBarBackdrop = rememberLayerBackdrop { drawContent() }
    val sceneBackdrop = rememberContentLayerBackdrop { drawContent() }

    // ========== 玻璃效果档位（个性化设置 → 全局 CompositionLocal） ==========
    val glassCardPresetKey by produceState(initialValue = IntensityPreset.MEDIUM.key) {
        prefs.glassCardPreset.collect { value = it }
    }
    val glassCardBlurPresetKey by produceState(initialValue = IntensityPreset.MEDIUM.key) {
        prefs.glassCardBlurPreset.collect { value = it }
    }
    val glassButtonPresetKey by produceState(initialValue = IntensityPreset.MEDIUM.key) {
        prefs.glassButtonPreset.collect { value = it }
    }
    val glassButtonBlurPresetKey by produceState(initialValue = IntensityPreset.LOW.key) {
        prefs.glassButtonBlurPreset.collect { value = it }
    }
    val wallpaperContrastPresetKey by produceState(initialValue = ContrastPreset.DEFAULT.key) {
        prefs.wallpaperContrastPreset.collect { value = it }
    }
    val glassStyle = GlassStyle(
        card = glassEffectsFor(IntensityPreset.fromKey(glassCardPresetKey), GlassTarget.CARD)
            .copy(extraBlur = blurExtraFor(IntensityPreset.fromKey(glassCardBlurPresetKey))),
        button = glassEffectsFor(IntensityPreset.fromKey(glassButtonPresetKey), GlassTarget.BUTTON)
            .copy(extraBlur = blurExtraFor(IntensityPreset.fromKey(glassButtonBlurPresetKey))),
        contrast = wallpaperContrastScale(ContrastPreset.fromKey(wallpaperContrastPresetKey)),
    )

    CompositionLocalProvider(LocalGlassStyle provides glassStyle) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .contentLayerBackdrop(sceneBackdrop)
            ) {
                BackgroundWallpaperLayer {
                    Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    topBar = {}
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    MainPager(
                        navController = navController,
                        pages = primaryItems,
                        pagerState = pagerState,
                        stateHolder = pagerStateHolder,
                        currentRoute = currentRoute,
                        sourceUrl = currentSourceUrl,
                        visible = currentRoute in primaryRoutes,
                    )

                    NavHost(
                        navController = navController,
                        startDestination = primaryRoutes.firstOrNull { it == defaultPage }
                            ?: primaryRoutes.first(),
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                if (secondaryRoute) {
                                    translationX = size.width * settledBackProgress
                                    scaleX = 1f - 0.04f * settledBackProgress
                                    scaleY = 1f - 0.04f * settledBackProgress
                                    alpha = 1f - 0.15f * settledBackProgress
                                }
                            },
                    enterTransition = {
                            val to = targetState.destination.route?.substringBefore("?")
                            if (to in primaryRoutes) {
                                EnterTransition.None
                            } else {
                                fadeIn(tween(280, easing = SecondaryPageEasing)) +
                                    slideInHorizontally(tween(280, easing = SecondaryPageEasing)) { it / 3 }
                        }
                    },
                    exitTransition = {
                        val from = initialState.destination.route?.substringBefore("?")
                        if (from in primaryRoutes) {
                            ExitTransition.None
                        } else {
                            fadeOut(tween(280, easing = SecondaryPageEasing)) +
                                slideOutHorizontally(tween(280, easing = SecondaryPageEasing)) { -it / 6 }
                        }
                    },
                    popEnterTransition = {
                        val to = targetState.destination.route?.substringBefore("?")
                        if (to in primaryRoutes) {
                            EnterTransition.None
                        } else {
                            fadeIn(tween(280, easing = SecondaryPageEasing)) +
                                slideInHorizontally(tween(280, easing = SecondaryPageEasing)) { -it / 6 }
                        }
                    },
                    popExitTransition = {
                        val from = initialState.destination.route?.substringBefore("?")
                        if (from in primaryRoutes) {
                            ExitTransition.None
                        } else {
                            fadeOut(tween(280, easing = SecondaryPageEasing)) +
                                slideOutHorizontally(tween(280, easing = SecondaryPageEasing)) { it / 3 }
                        }
                    }
                    ) {
                        composable(Screen.Home.route) {
                            Spacer(Modifier.fillMaxSize())
                        }
                        composable(
                            route = Screen.Douyin.destinationRoute,
                            arguments = listOf(
                                navArgument(Screen.Douyin.sourceUrlArgument) {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) { Spacer(Modifier.fillMaxSize()) }
                        composable(
                            route = Screen.Xiaohongshu.destinationRoute,
                            arguments = listOf(
                                navArgument(Screen.Xiaohongshu.sourceUrlArgument) {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) { Spacer(Modifier.fillMaxSize()) }
                        composable(
                            route = Screen.Bilibili.destinationRoute,
                            arguments = listOf(
                                navArgument(Screen.Bilibili.sourceUrlArgument) {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) { Spacer(Modifier.fillMaxSize()) }
                        composable(Screen.History.route) { HistoryScreen(navController) }
                        composable(Screen.Settings.route) { SettingsScreen(navController) }
                        composable(Screen.Personalization.route) { PersonalizationScreen(navController) }
                        composable(Screen.About.route) { AboutScreen(navController) }
                        composable(Screen.OpenSource.route) { OpenSourceScreen() }
                    }
                }
            }

            // ========== 悬浮底栏 ==========

            // 全局下载药丸（跨页面持久）
            val pillVisible by DownloadPillState.visible.collectAsState()
            val pillProgress by DownloadPillState.progress.collectAsState()
            val pillSpeed by DownloadPillState.speedText.collectAsState()
            DownloadPill(
                progress = pillProgress,
                speedText = pillSpeed,
                visible = pillVisible,
                onClick = {
                    val route = when (DownloadPillState.sourceRoute) {
                        "xiaohongshu" -> Screen.Xiaohongshu.route
                        "bilibili" -> Screen.Bilibili.route
                        else -> Screen.Douyin.route
                    }
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                    DownloadPillState.performClick()
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(top = 8.dp)
            )
        }
            }
        }

        if (showTopBarBlur) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TOP_BAR_BLUR_HEIGHT_DP.dp)
                    .align(Alignment.TopCenter)
            ) {
                ProgressiveTopBarBlur(
                    backdrop = sceneBackdrop,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }

        if (showTopBar) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PROGRESSIVE_TOPBAR_CONTENT_START_DP.dp)
                    .align(Alignment.TopCenter)
            ) {
                CompositionLocalProvider(LocalLiquidBackdrop provides sceneBackdrop) {
                    ClipFlowTopBar(
                        currentRoute = currentRoute,
                        navController = navController,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                }
            }
        }

        if (showBottomBar) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BOTTOM_BLUR_HEIGHT_DP.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .then(
                            Modifier.layerBackdrop(bottomBarBackdrop)
                        )
                ) {
                    ProgressiveBottomBlur(
                        backdrop = sceneBackdrop,
                        modifier = Modifier.matchParentSize(),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                FloatingBottomBar(
                    prefs = prefs,
                    backdrop = bottomBarBackdrop,
                    primaryPagerState = primaryPagerState,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }

        if (showDetailBottomBlur) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DETAIL_BOTTOM_BLUR_HEIGHT_DP.dp)
                    .align(Alignment.BottomCenter)
            ) {
                ProgressiveBottomBlur(
                    backdrop = sceneBackdrop,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }

            PredictiveBackHandler(
                enabled = secondaryRoute && navController.previousBackStackEntry != null,
            ) { progress ->
                try {
                    progress.collect { event ->
                        predictiveBackProgress = event.progress
                    }
                    navController.popBackStack()
                } catch (_: CancellationException) {
                    // The gesture was cancelled; keep the current destination.
                } finally {
                    predictiveBackProgress = 0f
                }
            }
        }
    }
}
