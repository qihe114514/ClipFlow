package com.qihe.clipflow.ui.settings

import android.widget.Toast
import com.qihe.clipflow.BuildConfig
import android.app.Application
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.kyant.shapes.RoundedCornerStyle
import com.kyant.shapes.RoundedRectangle
import com.qihe.clipflow.data.preferences.AppPreferences
import com.qihe.clipflow.navigation.BottomNavItem
import com.qihe.clipflow.navigation.Screen
import com.qihe.clipflow.navigation.bottomNavItems
import com.qihe.clipflow.navigation.orderedBottomNavItems
import com.qihe.clipflow.util.CacheCleaner
import com.qihe.clipflow.ui.components.ClipFlowSectionHeader
import com.qihe.clipflow.ui.components.GlassCard
import com.qihe.clipflow.ui.icons.ClipFlowIcons
import com.qihe.clipflow.ui.components.withAdaptiveTitleShadow
import com.qihe.clipflow.ui.component.SecondaryPageSection
import com.qihe.clipflow.ui.component.secondaryPageEntrance
import com.qihe.clipflow.ui.component.LiquidButton
import com.qihe.clipflow.ui.components.GlassIconButton
import com.qihe.clipflow.ui.component.liquid.PROGRESSIVE_TOPBAR_CONTENT_START_DP
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val savePath: String = "",
    val videoSavePath: String = "",
    val imageSavePath: String = "",
    val audioSavePath: String = "",
    val defaultPage: String = "home",
    val bottomBarOrder: List<String> = listOf("home", "douyin", "xiaohongshu", "bilibili")
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = AppPreferences(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        viewModelScope.launch {
            launch { prefs.savePath.collect { _uiState.value = _uiState.value.copy(savePath = it) } }
            launch { prefs.videoSavePath.collect { _uiState.value = _uiState.value.copy(videoSavePath = it) } }
            launch { prefs.imageSavePath.collect { _uiState.value = _uiState.value.copy(imageSavePath = it) } }
            launch { prefs.audioSavePath.collect { _uiState.value = _uiState.value.copy(audioSavePath = it) } }
            launch { prefs.defaultPage.collect { _uiState.value = _uiState.value.copy(defaultPage = it) } }
            launch { prefs.bottomBarOrder.collect { _uiState.value = _uiState.value.copy(bottomBarOrder = it) } }
        }
    }

    fun setSavePath(path: String) { viewModelScope.launch { prefs.setSavePath(path) } }
    fun setVideoSavePath(path: String) { viewModelScope.launch { prefs.setVideoSavePath(path) } }
    fun setImageSavePath(path: String) { viewModelScope.launch { prefs.setImageSavePath(path) } }
    fun setAudioSavePath(path: String) { viewModelScope.launch { prefs.setAudioSavePath(path) } }
    fun resetVideoSavePath() { viewModelScope.launch { prefs.setVideoSavePath("") } }
    fun resetImageSavePath() { viewModelScope.launch { prefs.setImageSavePath("") } }
    fun resetAudioSavePath() { viewModelScope.launch { prefs.setAudioSavePath("") } }
    fun setDefaultPage(page: String) { viewModelScope.launch { prefs.setDefaultPage(page) } }
    fun setBottomBarOrder(order: List<String>) { viewModelScope.launch { prefs.setBottomBarOrder(order) } }
    fun resetTutorial() { viewModelScope.launch { prefs.setTutorialShown(false) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cacheSize by remember { mutableStateOf(0L) }
    var clearingCache by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        cacheSize = withContext(Dispatchers.IO) { CacheCleaner.sizeBytes(context) }
    }

    val videoDirLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.setVideoSavePath(it.toString())
        }
    }
    val imageDirLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.setImageSavePath(it.toString())
        }
    }
    val audioDirLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.setAudioSavePath(it.toString())
        }
    }

    SecondaryPageSection(index = 0) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = PROGRESSIVE_TOPBAR_CONTENT_START_DP.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ========== 保存路径 ==========
        SectionHeader(title = "保存路径")
        GlassCard(modifier = Modifier.fillMaxWidth().secondaryPageEntrance(0)) {
            Column {
                PathRow(
                    icon = ClipFlowIcons.Video,
                    label = "视频",
                    path = rememberDestinationLabel(uiState.videoSavePath, "Movies/ClipFlow（默认）"),
                    onCustomize = { videoDirLauncher.launch(null) },
                    onReset = { viewModel.resetVideoSavePath() }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f),
                )
                PathRow(
                    icon = ClipFlowIcons.Image,
                    label = "图片",
                    path = rememberDestinationLabel(uiState.imageSavePath, "Pictures/ClipFlow（默认）"),
                    onCustomize = { imageDirLauncher.launch(null) },
                    onReset = { viewModel.resetImageSavePath() }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f),
                )
                PathRow(
                    icon = ClipFlowIcons.Audio,
                    label = "音乐",
                    path = rememberDestinationLabel(uiState.audioSavePath, "Music/ClipFlow（默认）"),
                    onCustomize = { audioDirLauncher.launch(null) },
                    onReset = { viewModel.resetAudioSavePath() }
                )
            }
        }
        // ========== 个性化 ==========
        SectionHeader(title = "个性化")
        GlassCard(
            modifier = Modifier.secondaryPageEntrance(1),
            onClick = { navController.navigate(Screen.Personalization.route) },
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(ClipFlowIcons.Palette, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("壁纸与玻璃效果", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    Text("壁纸、模糊与折射", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(ClipFlowIcons.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
            }
        }

        SectionHeader(title = "页面设置")
        GlassCard(modifier = Modifier.secondaryPageEntrance(2)) {
            Column {
                Text("打开软件默认页面", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf("home" to "主页", "douyin" to "抖音", "xiaohongshu" to "小红书", "bilibili" to "B站").forEach { (key, label) ->
                        val selected = uiState.defaultPage == key
                        LiquidButton(
                            onClick = { viewModel.setDefaultPage(key) },
                            modifier = Modifier.weight(1f),
                            tint = if (selected) MaterialTheme.colorScheme.primary else Color.Unspecified,
                            height = 36.dp,
                            contentPadding = PaddingValues(horizontal = 4.dp),
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else Color.Unspecified,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("底栏按钮排序", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "长按拖动",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    )
                }
                Spacer(Modifier.height(10.dp))

                BottomBarOrderEditor(
                    order = uiState.bottomBarOrder,
                    onOrderChange = viewModel::setBottomBarOrder,
                )
            }
        }
        // ========== 存储 ==========
        SectionHeader(title = "存储")
        GlassCard(modifier = Modifier.fillMaxWidth().secondaryPageEntrance(3)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    ClipFlowIcons.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("下载缓存", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    Text(
                        text = CacheCleaner.formatSize(cacheSize),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LiquidButton(
                    onClick = {
                        clearingCache = true
                        scope.launch {
                            val freed = withContext(Dispatchers.IO) { CacheCleaner.clear(context) }
                            cacheSize = withContext(Dispatchers.IO) { CacheCleaner.sizeBytes(context) }
                            clearingCache = false
                            Toast.makeText(
                                context,
                                if (freed == null) "有下载任务进行中，暂不能清理" else "已清理 ${CacheCleaner.formatSize(freed)}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    enabled = !clearingCache,
                ) {
                    Text("清理")
                }
            }
        }


        // ========== 关于 ==========
        SectionHeader(title = "更多")
        GlassCard(
            modifier = Modifier.secondaryPageEntrance(3),
            onClick = { navController.navigate(Screen.About.route) },
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(ClipFlowIcons.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(14.dp))
                Text("关于 ClipFlow", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Text("v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Icon(ClipFlowIcons.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
            }
        }
        TextButton(
            onClick = {
                viewModel.resetTutorial()
                Toast.makeText(context, "已重置，下次解析成功后会再次显示引导", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .secondaryPageEntrance(4),
        ) {
            Text("重置新手教程", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
        }
        Spacer(Modifier.height(20.dp))
    }
    }
}


@Composable
fun SectionHeader(title: String) {
    ClipFlowSectionHeader(
        title = title,
        modifier = Modifier.padding(top = 4.dp),
    )
}

/**
 * Bottom-bar order editor: one horizontal strip that mirrors the real bottom
 * bar. Items are picked up with a long press and swap slots while dragging.
 */
@Composable
private fun BottomBarOrderEditor(
    order: List<String>,
    onOrderChange: (List<String>) -> Unit,
) {
    val normalized = remember(order) { orderedBottomNavItems(order).map { it.route } }
    val localOrder = remember { mutableStateListOf<String>().apply { addAll(normalized) } }
    var draggingRoute by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(normalized) {
        if (draggingRoute == null && localOrder.toList() != normalized) {
            localOrder.clear()
            localOrder.addAll(normalized)
        }
    }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val gap = 6.dp
        val trackPadding = 6.dp
        val tileWidth = (maxWidth - trackPadding * 2 - gap * 3) / 4
        val tileWidthPx = with(LocalDensity.current) { tileWidth.toPx() }

        fun moveItem(route: String, delta: Int) {
            val from = localOrder.indexOf(route)
            val target = (from + delta).coerceIn(0, localOrder.lastIndex)
            if (from >= 0 && target != from) {
                localOrder.removeAt(from)
                localOrder.add(target, route)
                onOrderChange(localOrder.toList())
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedRectangle(22.dp, RoundedCornerStyle.Continuous),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
        ) {
            Row(
                modifier = Modifier.padding(trackPadding),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                localOrder.forEachIndexed { index, route ->
                    val item = bottomNavItems.firstOrNull { it.route == route }
                        ?: return@forEachIndexed
                    key(route) {
                        BottomBarOrderTile(
                            item = item,
                            index = index,
                            dragging = draggingRoute == route,
                            dragOffset = if (draggingRoute == route) dragOffset else 0f,
                            width = tileWidth,
                            widthPx = tileWidthPx,
                            onDragStart = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                draggingRoute = route
                                dragOffset = 0f
                            },
                            onDrag = { deltaX ->
                                if (draggingRoute == route) {
                                    dragOffset += deltaX
                                    val currentIndex = localOrder.indexOf(route)
                                    if (currentIndex >= 0) {
                                        val direction = when {
                                            dragOffset > tileWidthPx * 0.45f -> 1
                                            dragOffset < -tileWidthPx * 0.45f -> -1
                                            else -> 0
                                        }
                                        if (direction != 0) {
                                            val target = (currentIndex + direction)
                                                .coerceIn(0, localOrder.lastIndex)
                                            if (target != currentIndex) {
                                                localOrder.removeAt(currentIndex)
                                                localOrder.add(target, route)
                                                dragOffset -= (target - currentIndex) * tileWidthPx
                                            }
                                        }
                                    }
                                    val limit = tileWidthPx * 0.55f
                                    dragOffset = dragOffset.coerceIn(-limit, limit)
                                }
                            },
                            onDragEnd = {
                                if (draggingRoute == route) {
                                    draggingRoute = null
                                    dragOffset = 0f
                                    onOrderChange(localOrder.toList())
                                }
                            },
                            onMoveLeft = if (index > 0) {
                                { moveItem(route, -1) }
                            } else {
                                null
                            },
                            onMoveRight = if (index < localOrder.lastIndex) {
                                { moveItem(route, 1) }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomBarOrderTile(
    item: BottomNavItem,
    index: Int,
    dragging: Boolean,
    dragOffset: Float,
    width: Dp,
    widthPx: Float,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onMoveLeft: (() -> Unit)?,
    onMoveRight: (() -> Unit)?,
) {
    val shape = remember { RoundedRectangle(16.dp, RoundedCornerStyle.Continuous) }
    var previousIndex by remember { mutableIntStateOf(index) }
    val settleOffset = remember { Animatable(0f) }
    val scale by animateFloatAsState(
        targetValue = if (dragging) 1.06f else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 700f),
        label = "bottomBarOrderTileScale",
    )

    LaunchedEffect(dragging) {
        if (dragging) settleOffset.snapTo(0f)
    }

    LaunchedEffect(index) {
        val delta = previousIndex - index
        previousIndex = index
        if (delta == 0) return@LaunchedEffect
        if (dragging) {
            // The dragged tile follows the finger directly; its slot changes
            // are already compensated by dragOffset in the parent.
            settleOffset.snapTo(0f)
        } else {
            settleOffset.snapTo(delta * widthPx)
            settleOffset.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = 520f),
            )
        }
    }

    Box(
        modifier = Modifier
            .width(width)
            .height(64.dp)
            .zIndex(if (dragging) 1f else 0f)
            .graphicsLayer {
                translationX = settleOffset.value + if (dragging) dragOffset else 0f
                scaleX = scale
                scaleY = scale
                shadowElevation = if (dragging) 12.dp.toPx() else 0f
                this.shape = shape
                clip = false
            }
            .clip(shape)
            .background(
                if (dragging) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f)
                } else {
                    Color.Transparent
                }
            )
            .pointerInput(item.route) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x)
                    },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd,
                )
            }
            .semantics(mergeDescendants = true) {
                contentDescription = "${item.label}，第 ${index + 1} 位，长按可拖动排序"
                customActions = listOfNotNull(
                    onMoveLeft?.let { move ->
                        CustomAccessibilityAction("左移") {
                            move()
                            true
                        }
                    },
                    onMoveRight?.let { move ->
                        CustomAccessibilityAction("右移") {
                            move()
                            true
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = item.selectedIcon,
                contentDescription = null,
                tint = if (dragging) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = if (dragging) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun rememberDestinationLabel(uri: String, defaultLabel: String): String {
    val context = LocalContext.current
    val label by produceState(initialValue = defaultLabel, key1 = uri) {
        value = if (uri.isBlank()) {
            defaultLabel
        } else {
            withContext(Dispatchers.IO) {
                val document = runCatching {
                    DocumentFile.fromTreeUri(context, uri.toUri())
                }.getOrNull()
                when {
                    document == null -> "自定义目录（不可用，请重新选择）"
                    !document.exists() -> "自定义目录（已不存在，请重新选择）"
                    !document.canWrite() -> "${document.name ?: "自定义目录"}（无写入权限）"
                    else -> document.name ?: "自定义目录"
                }
            }
        }
    }
    return label
}

@Composable
private fun PathRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    path: String,
    onCustomize: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Surface(
            shape = androidx.compose.foundation.shape.CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            modifier = Modifier.size(36.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Text(
                path,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(6.dp))
        LiquidButton(
            onClick = onCustomize,
            modifier = Modifier.size(36.dp),
            surfaceColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
            contentPadding = PaddingValues(0.dp),
        ) {
            Icon(
                imageVector = ClipFlowIcons.FolderOpen,
                contentDescription = "自定义${label}保存目录",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(6.dp))
        LiquidButton(
            onClick = onReset,
            modifier = Modifier.size(36.dp),
            surfaceColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
            contentPadding = PaddingValues(0.dp),
        ) {
            Icon(
                imageVector = ClipFlowIcons.Restore,
                contentDescription = "恢复${label}默认目录",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
