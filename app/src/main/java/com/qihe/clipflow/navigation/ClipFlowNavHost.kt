package com.qihe.clipflow.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.qihe.clipflow.ui.douyin.DouyinScreen
import com.qihe.clipflow.ui.history.HistoryScreen
import com.qihe.clipflow.ui.home.HomeScreen
import com.qihe.clipflow.ui.settings.SettingsScreen
import com.qihe.clipflow.ui.about.AboutScreen
import com.qihe.clipflow.ui.xiaohongshu.XiaohongshuScreen
import kotlinx.coroutines.flow.first
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

    val bottomBarRoutes = listOf(
        Screen.Home.route,
        Screen.Douyin.route,
        Screen.Xiaohongshu.route
    )

    val showBottomBar = currentRoute in bottomBarRoutes
    val showTopBar = currentRoute != null
    val isDark = isSystemInDarkTheme()
    val miuixController = remember(isDark) {
        ThemeController(
            ColorSchemeMode.System,
            isDark = isDark,
            paletteStyle = ThemePaletteStyle.TonalSpot,
            colorSpec = ThemeColorSpec.Spec2021,
        )
    }

    MiuixTheme(controller = miuixController) {
        val surfaceColor = MiuixTheme.colorScheme.surface
        val backdrop = rememberLayerBackdrop {
            drawRect(surfaceColor.copy(alpha = 0.18f))
            drawContent()
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (showBottomBar) Modifier.layerBackdrop(backdrop) else Modifier)
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
                    modifier = Modifier.fillMaxSize()
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = when (defaultPage) {
                            "douyin" -> Screen.Douyin.route
                            "xiaohongshu" -> Screen.Xiaohongshu.route
                            else -> Screen.Home.route
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    enterTransition = {
                        val from = initialState.destination.route?.substringBefore("?")
                        val to = targetState.destination.route?.substringBefore("?")
                        val fromIdx = bottomBarOrder.indexOf(from)
                        val toIdx = bottomBarOrder.indexOf(to)
                        val direction = if (toIdx > fromIdx) 1 else -1
                        fadeIn(tween(300)) + slideInHorizontally(tween(300)) { direction * it / 4 }
                    },
                    exitTransition = {
                        val from = initialState.destination.route?.substringBefore("?")
                        val to = targetState.destination.route?.substringBefore("?")
                        val fromIdx = bottomBarOrder.indexOf(from)
                        val toIdx = bottomBarOrder.indexOf(to)
                        val direction = if (toIdx > fromIdx) 1 else -1
                        fadeOut(tween(300)) + slideOutHorizontally(tween(300)) { -direction * it / 4 }
                    },
                    popEnterTransition = {
                        fadeIn(tween(300)) + slideInHorizontally(tween(300)) { -it / 4 }
                    },
                    popExitTransition = {
                        fadeOut(tween(300)) + slideOutHorizontally(tween(300)) { it / 4 }
                    }
                    ) {
                        composable(Screen.Home.route) { HomeScreen(navController) }
                        composable(
                            route = Screen.Douyin.destinationRoute,
                            arguments = listOf(
                                navArgument(Screen.Douyin.sourceUrlArgument) {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) { entry ->
                            DouyinScreen(entry.arguments?.getString(Screen.Douyin.sourceUrlArgument))
                        }
                        composable(
                            route = Screen.Xiaohongshu.destinationRoute,
                            arguments = listOf(
                                navArgument(Screen.Xiaohongshu.sourceUrlArgument) {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) { entry ->
                            XiaohongshuScreen(entry.arguments?.getString(Screen.Xiaohongshu.sourceUrlArgument))
                        }
                        composable(Screen.History.route) { HistoryScreen(navController) }
                        composable(Screen.Settings.route) { SettingsScreen(navController) }
                        composable(Screen.About.route) { AboutScreen(navController) }
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
                    navController = navController,
                    currentDestination = currentDestination,
                    prefs = prefs,
                    backdrop = backdrop,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
}
