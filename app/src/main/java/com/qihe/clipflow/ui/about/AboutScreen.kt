package com.qihe.clipflow.ui.about

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.qihe.clipflow.ClipFlowApp
import com.qihe.clipflow.data.preferences.AppPreferences
import com.qihe.clipflow.ui.components.GlassCard
import com.qihe.clipflow.ui.components.PrivacyConsentDialog
import kotlinx.coroutines.launch

@Composable
fun AboutScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { AppPreferences(context) }
    var showPrivacy by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(36.dp))

        // 应用图标
        Icon(
            imageVector = Icons.Filled.WaterDrop,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = "ClipFlow",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "抖音 · 小红书无水印解析",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "版本 2.4.0",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )

        Spacer(Modifier.height(40.dp))

        // ========== 基本信息 ==========
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                AboutInfoRow(label = "应用名称", value = "ClipFlow")
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                )
                AboutInfoRow(label = "版本号", value = "2.4.0 (4)")
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                )
                AboutInfoRow(label = "最低支持", value = "Android 12+")
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                )
                AboutInfoRow(label = "技术栈", value = "Jetpack Compose + Material 3")
            }
        }

        Spacer(Modifier.height(16.dp))

        // ========== 隐私 ==========
        Text(
            text = "隐私",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                // 查看隐私政策
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = "查看隐私政策",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalIconButton(
                        onClick = { showPrivacy = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.OpenInNew,
                            contentDescription = "查看",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                )

                // 撤销隐私同意
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.GppBad,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = "撤销隐私同意",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalIconButton(
                        onClick = {
                            scope.launch {
                                prefs.setPrivacyAgreed(false)
                            }
                            (context as? android.app.Activity)?.finishAffinity()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "撤销",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 隐私政策只读弹窗
        if (showPrivacy) {
            PrivacyConsentDialog(
                viewOnly = true,
                onDisagree = { showPrivacy = false }
            )
        }

        Spacer(Modifier.height(16.dp))

        // ========== 开发者信息 ==========
        Text(
            text = "开发者",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                AboutInfoRow(label = "开发者", value = "其核")
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                )
                Text(
                    text = "感谢使用 ClipFlow！ 关注其核谢谢喵~",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ========== 链接 ==========
        Text(
            text = "链接",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                // B站
                LinkRow(
                    icon = Icons.Filled.SmartDisplay,
                    label = "B站主页",
                    url = "https://space.bilibili.com/1049283248",
                    context = context
                )
                Spacer(Modifier.height(10.dp))
                // 抖音
                LinkRow(
                    icon = Icons.Filled.MusicNote,
                    label = "抖音主页",
                    url = "https://www.douyin.com/user/MS4wLjABAAAAuUtKOArTFKTBm4C6o5MwDQuGMNZ9-0CWZfUay6U9wUI",
                    context = context
                )
                Spacer(Modifier.height(10.dp))
                // GitHub
                LinkRow(
                    icon = Icons.Filled.Code,
                    label = "GitHub 开源仓库",
                    url = "https://github.com/qihe114514/qihe-douyin",
                    context = context
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Made with ❤️ by 其核",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun AboutInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun LinkRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    url: String,
    context: android.content.Context
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        FilledTonalIconButton(
            onClick = {
                try {
                    CustomTabsIntent.Builder()
                        .setShowTitle(true)
                        .build()
                        .launchUrl(context, Uri.parse(url))
                } catch (_: Exception) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                }
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.OpenInNew,
                contentDescription = "打开",
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
