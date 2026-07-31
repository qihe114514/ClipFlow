package com.qihe.clipflow.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import com.qihe.clipflow.data.preferences.AppPreferences

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
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val bottomBarOrder by produceState(initialValue = listOf("home", "douyin", "xiaohongshu")) {
        prefs.bottomBarOrder.collect { value = it }
    }
    val items = bottomBarOrder.mapNotNull { key -> bottomNavItems.find { it.route == key } }
    val shape = RoundedCornerShape(28.dp)
    val containerColor = if (isDark) Color.Black.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.55f)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.25f)

    Row(
        modifier = modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)
            .clip(shape).background(containerColor, shape).border(0.5.dp, borderColor, shape).padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route?.substringBefore("?") == item.route } == true
            val labelColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(22.dp))
                    .then(if (selected) Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), RoundedCornerShape(22.dp)) else Modifier)
                    .clickable {
                        if (!selected) navController.navigate(item.route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }.padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(if (selected) item.selectedIcon else item.unselectedIcon, item.label, tint = labelColor, modifier = Modifier.size(24.dp))
                    Text(item.label, style = MaterialTheme.typography.labelSmall, color = labelColor, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal)
                }
            }
        }
    }
}
