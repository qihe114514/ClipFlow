package com.qihe.clipflow.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.qihe.clipflow.navigation.Screen
import com.qihe.clipflow.ui.component.SecondaryPageEasing
import com.qihe.clipflow.ui.component.liquid.PROGRESSIVE_TOPBAR_CONTENT_START_DP
import com.qihe.clipflow.ui.components.ClipFlowSectionHeader
import com.qihe.clipflow.ui.components.GlassOrb
import com.qihe.clipflow.ui.components.GlassCard
import com.qihe.clipflow.ui.components.withAdaptiveTitleShadow
import com.qihe.clipflow.ui.icons.ClipFlowIcons
import com.qihe.clipflow.ui.theme.ClipFlowMotion
import com.qihe.clipflow.ui.theme.DouyinAccent
import com.qihe.clipflow.ui.theme.XiaohongshuAccent

private data class Feature(
    val title: String,
    val desc: String,
    val icon: ImageVector,
)

private val features = listOf(
    Feature("无水印解析", "原画品质下载", ClipFlowIcons.Sparkles),
    Feature("实况支持", "Live Photo 完整保存", ClipFlowIcons.Film),
    Feature("相册入库", "自动归入系统相册", ClipFlowIcons.Images),
    Feature("Material You", "跟随系统壁纸变色", ClipFlowIcons.Palette),
    Feature("历史回溯", "解析记录永不丢失", ClipFlowIcons.History),
)

@Composable
fun HomeScreen(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = PROGRESSIVE_TOPBAR_CONTENT_START_DP.dp)
            .padding(bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ========== 品牌区 ==========
        GlassOrb(
            icon = ClipFlowIcons.Download,
            contentDescription = null,
            size = 76.dp,
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = "ClipFlow",
            style = MaterialTheme.typography.displayMedium.withAdaptiveTitleShadow(),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "抖音 · 小红书 · B 站 无水印解析",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(34.dp))

        HomePlatformCard(
            title = "抖音解析",
            subtitle = "视频 · 图集 · 实况",
            icon = ClipFlowIcons.DouyinBrand,
            accent = DouyinAccent,
            onClick = { navController.navigate(Screen.Douyin.route) },
        )

        Spacer(Modifier.height(14.dp))

        HomePlatformCard(
            title = "小红书解析",
            subtitle = "图片 · 视频无水印",
            icon = ClipFlowIcons.Xiaohongshu,
            accent = XiaohongshuAccent,
            onClick = { navController.navigate(Screen.Xiaohongshu.route) },
        )

        Spacer(Modifier.height(30.dp))

        // ========== 特色标签 ==========
        ClipFlowSectionHeader(title = "核心特性")

        Spacer(Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(features) { feature ->
                GlassCard(
                    modifier = Modifier.width(146.dp).height(128.dp),
                    cornerRadius = 20.dp,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                ) {
                    Icon(
                        imageVector = feature.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = feature.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = feature.desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        minLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomePlatformCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(ClipFlowMotion.StandardMillis, easing = SecondaryPageEasing)) +
            slideInVertically(
                tween(ClipFlowMotion.StandardMillis, easing = SecondaryPageEasing)
            ) { it / 6 },
    ) {
        GlassCard(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = com.kyant.shapes.RoundedRectangle(16.dp, com.kyant.shapes.RoundedCornerStyle.Continuous),
                    color = accent.copy(alpha = 0.14f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = ClipFlowIcons.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                )
            }
        }
    }
}
