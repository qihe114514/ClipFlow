package com.qihe.clipflow.ui.history

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.qihe.clipflow.data.local.HistoryEntity
import com.qihe.clipflow.navigation.ParseBridge
import com.qihe.clipflow.navigation.Screen
import com.qihe.clipflow.ui.components.GlassCard
import com.qihe.clipflow.ui.components.GlassTextField
import com.qihe.clipflow.ui.components.TypeBadge
import com.qihe.clipflow.ui.theme.DouyinAccent
import com.qihe.clipflow.ui.theme.XiaohongshuAccent
import java.text.SimpleDateFormat
import java.util.*


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    navController: NavHostController,
    viewModel: HistoryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Toast via snackbar
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    // 导出启动器
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportSelected(it) }
    }

    // 导入启动器
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            viewModel.importFromUri(it)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(8.dp))

        // ========== 搜索 + 操作栏 ==========
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlassTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearch(it) },
                placeholder = "搜索历史...",
                modifier = Modifier.weight(1f)
            )

            // 导入按钮
            FilledTonalIconButton(
                onClick = { importLauncher.launch(arrayOf("application/json")) },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(Icons.Outlined.FileUpload, contentDescription = "导入", Modifier.size(20.dp))
            }

            // 选择模式切换
            FilledTonalIconButton(
                onClick = { viewModel.toggleSelectionMode() },
                modifier = Modifier.size(44.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (uiState.isSelectionMode)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Icon(
                    imageVector = if (uiState.isSelectionMode) Icons.Filled.Close else Icons.Outlined.Checklist,
                    contentDescription = "选择",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // ========== 选择模式操作栏 — 玻璃透明 ==========
        AnimatedVisibility(
            visible = uiState.isSelectionMode && uiState.selectedIds.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (uiState.selectedIds.size == uiState.items.size && uiState.items.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearSelection() }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.SelectAll, "取消全选", Modifier.size(18.dp))
                        }
                    } else {
                        IconButton(onClick = { viewModel.selectAll() }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Outlined.SelectAll, "全选", Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "已选 ${uiState.selectedIds.size} 项",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(
                    onClick = { viewModel.requestDelete() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Filled.Delete, "删除", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // ========== 导出按钮（选择模式下） ==========
        if (uiState.isSelectionMode && uiState.selectedIds.isNotEmpty()) {
            TextButton(
                onClick = { exportLauncher.launch("clipflow_history.json") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.FileDownload, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("导出选中记录")
            }
            Spacer(Modifier.height(4.dp))
        }

        // ========== 历史列表 ==========
        if (uiState.items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.History, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (uiState.searchQuery.isNotEmpty()) "没有匹配的记录" else "暂无解析历史",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    if (uiState.searchQuery.isEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text("解析过的链接会自动保存在这里", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val groupedItems = uiState.items.groupBy { formatDateHeader(it.timestamp) }
                groupedItems.forEach { (dateHeader, items) ->
                    item(key = "header_$dateHeader") {
                        Text(
                            dateHeader,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                        )
                    }
                    items(items = items, key = { it.id }) { item ->
                        HistoryItemCard(
                            item = item,
                            isSelectionMode = uiState.isSelectionMode,
                            isSelected = item.id in uiState.selectedIds,
                            onTap = {
                                if (uiState.isSelectionMode) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.toggleItemSelection(item.id)
                                } else {
                                    val route = when (item.platform) {
                                        "douyin" -> Screen.Douyin.route
                                        "xiaohongshu" -> Screen.Xiaohongshu.route
                                        else -> Screen.Home.route
                                    }
                                    ParseBridge.pendingUrl = item.url
                                    navController.navigate(route) {
                                        popUpTo(Screen.Home.route) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            },
                            onLongPress = {
                                if (!uiState.isSelectionMode) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.toggleSelectionMode()
                                    viewModel.toggleItemSelection(item.id)
                                }
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        SnackbarHost(hostState = snackbarHostState)
    }

    // ========== 删除确认弹窗 ==========
    if (uiState.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null) },
            title = { Text("确认删除") },
            text = { Text("确定要删除选中的 ${uiState.selectedIds.size} 条记录吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDelete() }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) { Text("取消") }
            }
        )
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryItemCard(
    item: HistoryEntity,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    GlassCard(
        modifier = modifier.combinedClickable(
            onClick = onTap,
            onLongClick = onLongPress
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 选择模式勾选框
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onTap() },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
            }

            // 封面缩略图
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                if (item.coverUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(item.coverUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // 平台图标占位
                    Icon(
                        imageVector = if (item.platform == "douyin") Icons.Filled.MusicNote else Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = if (item.platform == "douyin") DouyinAccent.copy(alpha = 0.4f)
                        else XiaohongshuAccent.copy(alpha = 0.4f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            // 文字信息
            Column(modifier = Modifier.weight(1f)) {
                // 标题
                Text(
                    text = item.title.ifEmpty { "未命名" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                // 作者
                if (!item.authorName.isNullOrEmpty()) {
                    Text(
                        text = item.authorName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                }
                // 平台 + 时间
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TypeBadge(
                        text = when (item.platform) {
                            "douyin" -> "抖音"
                            "xiaohongshu" -> "小红书"
                            else -> item.platform
                        },
                        color = if (item.platform == "douyin") DouyinAccent else XiaohongshuAccent
                    )
                    Text(
                        text = formatTime(item.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }

            if (!isSelectionMode) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}


private fun formatDateHeader(timestamp: Long): String {
    val today = Calendar.getInstance()
    val date = Calendar.getInstance().apply { timeInMillis = timestamp }
    return when {
        today.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR) -> "今天"
        today.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) - date.get(Calendar.DAY_OF_YEAR) == 1 -> "昨天"
        today.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) - date.get(Calendar.DAY_OF_YEAR) < 7 -> {
            SimpleDateFormat("EEEE", Locale.CHINESE).format(Date(timestamp))
        }
        else -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}
