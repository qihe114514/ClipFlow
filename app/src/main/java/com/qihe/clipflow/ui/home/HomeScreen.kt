package com.qihe.clipflow.ui.home

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.qihe.clipflow.navigation.Screen
import com.qihe.clipflow.ui.components.GlassCard
import com.qihe.clipflow.ui.theme.DouyinAccent
import com.qihe.clipflow.ui.theme.XiaohongshuAccent

private val features = listOf(
    Triple("无水印解析", "原画品质下载", Icons.Outlined.HighQuality),
    Triple("实况支持", "Live Photo 完整保存", Icons.Outlined.Animation),
    Triple("相册入库", "自动归入系统相册", Icons.Outlined.PhotoLibrary),
    Triple("Material You", "跟随系统壁纸变色", Icons.Outlined.Palette),
    Triple("历史回溯", "解析记录永不丢失", Icons.Outlined.History)
)

@Composable
fun HomeScreen(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        // ========== 品牌区 ==========
        Icon(
            imageVector = Icons.Filled.WaterDrop,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "ClipFlow",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "抖音 · 小红书 无水印解析",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(36.dp))

        // ========== 功能卡片 抖音 ==========
        GlassCard(
            onClick = { navController.navigate(Screen.Douyin.route) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = DouyinAccent.copy(alpha = 0.12f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = DouyinAccent,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                Spacer(Modifier.width(18.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "抖音解析",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "支持视频 · 图集 · 实况",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ========== 功能卡片 小红书 ==========
        GlassCard(
            onClick = { navController.navigate(Screen.Xiaohongshu.route) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = XiaohongshuAccent.copy(alpha = 0.12f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = XiaohongshuAccent,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                Spacer(Modifier.width(18.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "小红书解析",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "笔记图片 · 视频无水印",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // ========== 特色标签 ==========
        Text(
            text = "核心特性",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(features) { (title, desc, icon) ->
                GlassCard(
                    modifier = Modifier.width(140.dp).height(115.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = MaterialTheme.typography.labelSmall.lineHeight
                    )
                }
            }
        }
    }
}
