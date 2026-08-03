package com.qihe.clipflow.navigation

import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.qihe.clipflow.data.preferences.AppPreferences
import com.qihe.clipflow.ui.components.PrivacyConsentDialog
import com.qihe.clipflow.ui.components.DownloadPill
import com.qihe.clipflow.ClipFlowApp
import com.qihe.clipflow.ui.components.DownloadPillState
import com.qihe.clipflow.ui.history.HistoryScreen
import com.qihe.clipflow.ui.settings.SettingsScreen
import com.qihe.clipflow.ui.about.AboutScreen
import com.qihe.clipflow.ui.about.OpenSourceScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

@Composable
fun ClipFlowNavHost() {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route?.substringBefore("?")
    val currentRouteState = rememberUpdatedState(currentRoute)

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

    val bottomBarOrder by produceState(initialValue = listOf("home", "douyin", "xiaohongshu")) {
        prefs.bottomBarOrder.collect { value = it }
    }

    val primaryItems = remember(bottomBarOrder) {
        orderedBottomNavItems(bottomBarOrder)
    }
    val primaryRoutes = remember(primaryItems) { primaryItems.map { it.route } }
    val showBottomBar = currentRoute in primaryRoutes
    val showTopBar = currentRoute != null
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

    val isDark = isSystemInDarkTheme()
    val miuixController = remember(isDark) {
        ThemeController(
            ColorSchemeMode.System,
            isDark = isDark,
            paletteStyle = ThemePaletteStyle.TonalSpot,
            colorSpec = ThemeColorSpec.Spec2021,
        )
    }

    val liquidBottomBarEnabled = supportsLiquidBottomBar(Build.VERSION.SDK_INT)

    MiuixTheme(controller = miuixController) {
        val surfaceColor = MiuixTheme.colorScheme.surface
        val backdrop = if (liquidBottomBarEnabled) {
            rememberLayerBackdrop {
                drawRect(surfaceColor.copy(alpha = 0.18f))
                drawContent()
            }
        } else {
            null
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (showBottomBar && backdrop != null) {
                            Modifier.layerBackdrop(backdrop)
                        } else {
                            Modifier
                        }
                    )
            ) {
                BackgroundWallpaperLayer {
                    Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                topBar = {
                    if (showTopBar) {
                        ClipFlowTopBar(
                            currentRoute = currentRoute,
                            navController = navController
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (showBottomBar) {
                        MainPager(
                            navController = navController,
                            pages = primaryItems,
                            pagerState = pagerState,
                            stateHolder = pagerStateHolder,
                            currentRoute = currentRoute,
                            sourceUrl = currentSourceUrl,
                        )
                    }

                    NavHost(
                        navController = navController,
                        startDestination = primaryRoutes.firstOrNull { it == defaultPage }
                            ?: primaryRoutes.first(),
                        modifier = Modifier.fillMaxSize(),
                    enterTransition = {
                        val from = initialState.destination.route?.substringBefore("?")
                        val to = targetState.destination.route?.substringBefore("?")
                        if (to in primaryRoutes) {
                            EnterTransition.None
                        } else {
                            val fromIdx = primaryRoutes.indexOf(from)
                            val toIdx = primaryRoutes.indexOf(to)
                            val direction = if (toIdx > fromIdx) 1 else -1
                            fadeIn(tween(300)) + slideInHorizontally(tween(300)) { direction * it / 4 }
                        }
                    },
                    exitTransition = {
                        val from = initialState.destination.route?.substringBefore("?")
                        if (from in primaryRoutes) {
                            ExitTransition.None
                        } else {
                            val to = targetState.destination.route?.substringBefore("?")
                            val fromIdx = primaryRoutes.indexOf(from)
                            val toIdx = primaryRoutes.indexOf(to)
                            val direction = if (toIdx > fromIdx) 1 else -1
                            fadeOut(tween(300)) + slideOutHorizontally(tween(300)) { -direction * it / 4 }
                        }
                    },
                    popEnterTransition = {
                        val to = targetState.destination.route?.substringBefore("?")
                        if (to in primaryRoutes) {
                            EnterTransition.None
                        } else {
                            fadeIn(tween(300)) + slideInHorizontally(tween(300)) { -it / 4 }
                        }
                    },
                    popExitTransition = {
                        val from = initialState.destination.route?.substringBefore("?")
                        if (from in primaryRoutes) {
                            ExitTransition.None
                        } else {
                            fadeOut(tween(300)) + slideOutHorizontally(tween(300)) { it / 4 }
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

        if (showBottomBar) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                FloatingBottomBar(
                    prefs = prefs,
                    backdrop = backdrop,
                    primaryPagerState = primaryPagerState,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
}
