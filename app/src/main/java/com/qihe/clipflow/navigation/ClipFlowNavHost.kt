package com.qihe.clipflow.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.qihe.clipflow.ClipFlowApp
import com.qihe.clipflow.BuildConfig
import com.qihe.clipflow.data.preferences.AppPreferences
import com.qihe.clipflow.ui.components.PrivacyConsentDialog
import com.qihe.clipflow.ui.components.UpdateDialog
import com.qihe.clipflow.ui.components.DownloadPill
import com.qihe.clipflow.ui.components.DownloadPillState
import com.qihe.clipflow.util.UpdateManager
import com.qihe.clipflow.ui.douyin.DouyinScreen
import com.qihe.clipflow.ui.history.HistoryScreen
import com.qihe.clipflow.ui.home.HomeScreen
import com.qihe.clipflow.ui.settings.SettingsScreen
import com.qihe.clipflow.ui.about.AboutScreen
import com.qihe.clipflow.ui.xiaohongshu.XiaohongshuScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipFlowNavHost() {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    // ========== 隐私政策同意检查 ==========
    val privacyAgreed by produceState(initialValue = false) {
        prefs.privacyAgreed.collect { value = it }
    }

    // 已同意则初始化友盟 SDK（首次同意 + 后续每次冷启动）
    LaunchedEffect(privacyAgreed) {
        if (privacyAgreed) {
            (context.applicationContext as ClipFlowApp).initUmengIfNeeded()
        }
    }

    if (!privacyAgreed) {
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

    // ========== 更新检测 ==========
    // var updateInfo by remember { mutableStateOf<UpdateManager.UpdateInfo?>(null) }
    // LaunchedEffect(privacyAgreed) {
    //     if (privacyAgreed) {
    //         val result = UpdateManager.checkUpdate(BuildConfig.VERSION_NAME)
    //         updateInfo = result.getOrNull()
    //     }
    // }

    // updateInfo?.let { info ->
    //     UpdateDialog(
    //         info = info,
    //         onDownload = {
    //             UpdateManager.downloadAndInstall(context, info.downloadUrl, info.fileName) {
    //                 updateInfo = null
    //             }
    //         },
    //         onDismiss = { updateInfo = null }
    //     )
    // }

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
                NavHost(
                    navController = navController,
                    startDestination = when (defaultPage) {
                        "douyin" -> Screen.Douyin.route
                        "xiaohongshu" -> Screen.Xiaohongshu.route
                        else -> Screen.Home.route
                    },
                    modifier = Modifier
                        .padding(innerPadding)
                        .then(if (showBottomBar) Modifier.padding(bottom = 80.dp) else Modifier),
                    enterTransition = {
                        val from = initialState.destination.route
                        val to = targetState.destination.route
                        val fromIdx = bottomBarOrder.indexOf(from)
                        val toIdx = bottomBarOrder.indexOf(to)
                        val direction = if (toIdx > fromIdx) 1 else -1
                        fadeIn(tween(300)) + slideInHorizontally(tween(300)) { direction * it / 4 }
                    },
                    exitTransition = {
                        val from = initialState.destination.route
                        val to = targetState.destination.route
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
                    composable(Screen.Douyin.route) { DouyinScreen() }
                    composable(Screen.Xiaohongshu.route) { XiaohongshuScreen() }
                    composable(Screen.History.route) { HistoryScreen(navController) }
                    composable(Screen.Settings.route) { SettingsScreen(navController) }
                    composable(Screen.About.route) { AboutScreen(navController) }
                }
            }

            // ========== 悬浮底栏 ==========
            if (showBottomBar) {
                FloatingBottomBar(
                    navController = navController,
                    currentDestination = currentDestination,
                    prefs = prefs,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            // 全局下载药丸（跨页面持久）
            val pillVisible by DownloadPillState.visible.collectAsState()
            val pillProgress by DownloadPillState.progress.collectAsState()
            val pillSpeed by DownloadPillState.speedText.collectAsState()
            DownloadPill(
                progress = pillProgress,
                speedText = pillSpeed,
                visible = pillVisible,
                onClick = { DownloadPillState.performClick() },
                modifier = Modifier.align(Alignment.TopStart)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipFlowTopBar(
    currentRoute: String?,
    navController: NavHostController
) {
    val isSpecialPage = currentRoute == Screen.History.route
        || currentRoute == Screen.Settings.route
        || currentRoute == Screen.About.route
    val showHistory = !isSpecialPage
    val showSettings = !isSpecialPage
    val showBack = currentRoute == Screen.History.route
        || currentRoute == Screen.Settings.route
        || currentRoute == Screen.About.route

    CenterAlignedTopAppBar(
        navigationIcon = {
            if (showBack) {
                IconButton(onClick = { navController.popBackStack() }) {
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
            if (showHistory) {
                IconButton(onClick = { navController.navigate(Screen.History.route) }) {
                    Icon(Icons.Outlined.History, contentDescription = "历史")
                }
            }
            if (showSettings) {
                IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                    Icon(Icons.Outlined.Settings, contentDescription = "设置")
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent
        ),
        modifier = Modifier
            .statusBarsPadding()
            .windowInsetsPadding(WindowInsets(0, 0, 0, 0))
    )
}

@Composable
fun FloatingBottomBar(
    navController: NavHostController,
    currentDestination: NavDestination?,
    prefs: AppPreferences,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    val bottomBarOrder by produceState(initialValue = listOf("home", "douyin", "xiaohongshu")) {
        prefs.bottomBarOrder.collect { value = it }
    }

    val orderedItems = bottomBarOrder.mapNotNull { key ->
        bottomNavItems.find { it.route == key }
    }

    val containerBg = if (isDark)
        Color.Black.copy(alpha = 0.55f)
    else
        Color.White.copy(alpha = 0.55f)
    val borderColor = if (isDark)
        Color.White.copy(alpha = 0.08f)
    else
        Color.White.copy(alpha = 0.25f)

    Row(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(containerBg, RoundedCornerShape(28.dp))
            .border(0.5.dp, borderColor, RoundedCornerShape(28.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        orderedItems.forEach { item ->
            val selected = currentDestination?.hierarchy?.any {
                it.route == item.route
            } == true

            val labelColor = if (selected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .then(
                        if (selected) Modifier.background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            RoundedCornerShape(22.dp)
                        ) else Modifier
                    )
                    .clickable {
                        if (!selected) {
                            navController.navigate(item.route) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        tint = labelColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
        }
    }
}
