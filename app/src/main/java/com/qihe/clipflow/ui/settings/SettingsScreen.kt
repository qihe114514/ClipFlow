package com.qihe.clipflow.ui.settings

import com.qihe.clipflow.BuildConfig
import android.app.Application
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.qihe.clipflow.data.preferences.AppPreferences
import com.qihe.clipflow.navigation.Screen
import com.qihe.clipflow.ui.components.GlassCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val savePath: String = "",
    val wallpaperUri: String = "",
    val wallpaperType: String = "image",
    val wallpaperOpacity: Float = 75f,
    val wallpaperEnabled: Boolean = true,
    val defaultPage: String = "home",
    val bottomBarOrder: List<String> = listOf("home", "douyin", "xiaohongshu")
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = AppPreferences(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        viewModelScope.launch {
            launch { prefs.savePath.collect { _uiState.value = _uiState.value.copy(savePath = it) } }
            launch { prefs.wallpaperUri.collect { _uiState.value = _uiState.value.copy(wallpaperUri = it) } }
            launch { prefs.wallpaperType.collect { _uiState.value = _uiState.value.copy(wallpaperType = it) } }
            launch { prefs.wallpaperOpacity.collect { _uiState.value = _uiState.value.copy(wallpaperOpacity = it) } }
            launch { prefs.wallpaperEnabled.collect { _uiState.value = _uiState.value.copy(wallpaperEnabled = it) } }
            launch { prefs.defaultPage.collect { _uiState.value = _uiState.value.copy(defaultPage = it) } }
            launch { prefs.bottomBarOrder.collect { _uiState.value = _uiState.value.copy(bottomBarOrder = it) } }
        }
    }

    fun setSavePath(path: String) { viewModelScope.launch { prefs.setSavePath(path) } }
    fun setWallpaperUri(uri: String) { viewModelScope.launch { prefs.setWallpaperUri(uri) } }
    fun setWallpaperType(type: String) { viewModelScope.launch { prefs.setWallpaperType(type) } }
    fun setWallpaperOpacity(o: Float) { viewModelScope.launch { prefs.setWallpaperOpacity(o) } }
    fun setWallpaperEnabled(e: Boolean) { viewModelScope.launch { prefs.setWallpaperEnabled(e) } }
    fun setDefaultPage(page: String) { viewModelScope.launch { prefs.setDefaultPage(page) } }
    fun setBottomBarOrder(order: List<String>) { viewModelScope.launch { prefs.setBottomBarOrder(order) } }
    fun resetTutorial() { viewModelScope.launch { prefs.setTutorialShown(false) } }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            viewModel.setWallpaperUri(it.toString())
            viewModel.setWallpaperType("image")
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            viewModel.setWallpaperUri(it.toString())
            viewModel.setWallpaperType("video")
        }
    }

    val dirPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.setSavePath(it.toString())
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        // ========== 保存路径 ==========
        SectionHeader(title = "保存路径")
        GlassCard(onClick = { dirPickerLauncher.launch(null) }) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Folder, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("下载目录", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    Text(
                        uiState.savePath.ifEmpty { "Downloads/ClipFlow" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
            }
        }

        // ========== 背景壁纸 ==========
        SectionHeader(title = "背景壁纸")
        GlassCard {
            Column {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = uiState.wallpaperType == "image",
                        onClick = { imagePickerLauncher.launch("image/*") },
                        label = { Text("选择图片") },
                        leadingIcon = { Icon(Icons.Filled.Image, null, Modifier.size(16.dp)) }
                    )
                    FilterChip(
                        selected = uiState.wallpaperType == "video",
                        onClick = { videoPickerLauncher.launch("video/*") },
                        label = { Text("选择视频") },
                        leadingIcon = { Icon(Icons.Filled.Videocam, null, Modifier.size(16.dp)) }
                    )
                    if (uiState.wallpaperUri.isNotEmpty()) {
                        FilterChip(
                            selected = false,
                            onClick = { viewModel.setWallpaperUri("") },
                            label = { Text("恢复默认") },
                            leadingIcon = { Icon(Icons.Filled.Restore, null, Modifier.size(16.dp)) }
                        )
                    }
                    FilterChip(
                        selected = !uiState.wallpaperEnabled,
                        onClick = { viewModel.setWallpaperEnabled(!uiState.wallpaperEnabled) },
                        label = { Text(if (uiState.wallpaperEnabled) "关闭壁纸" else "已关闭") },
                        leadingIcon = { Icon(if (uiState.wallpaperEnabled) Icons.Filled.Wallpaper else Icons.Filled.HideImage, null, Modifier.size(16.dp)) }
                    )
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    "壁纸透明度: ${uiState.wallpaperOpacity.toInt()}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                Slider(
                    value = uiState.wallpaperOpacity,
                    onValueChange = { viewModel.setWallpaperOpacity(it) },
                    valueRange = 10f..100f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        SectionHeader(title = "页面设置")
        GlassCard {
            Column {
                Text("打开软件默认页面", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("home" to "主页", "douyin" to "抖音", "xiaohongshu" to "小红书").forEach { (key, label) ->
                        FilterChip(selected = uiState.defaultPage == key, onClick = { viewModel.setDefaultPage(key) }, label = { Text(label) })
                    }
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                Spacer(Modifier.height(16.dp))

                Text("底栏按钮排序", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))

                uiState.bottomBarOrder.forEachIndexed { index, key ->
                    val label = when (key) { "home" -> "主页"; "douyin" -> "抖音"; "xiaohongshu" -> "小红书"; else -> key }
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), modifier = Modifier.size(24.dp)) {
                            Box(contentAlignment = Alignment.Center) { Text("${index + 1}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.weight(1f))
                        if (index > 0) {
                            IconButton(onClick = {
                                val o = uiState.bottomBarOrder.toMutableList(); val t = o[index]; o[index] = o[index - 1]; o[index - 1] = t; viewModel.setBottomBarOrder(o)
                            }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.KeyboardArrowUp, "上移", Modifier.size(18.dp))
                            }
                        }
                        if (index < uiState.bottomBarOrder.size - 1) {
                            IconButton(onClick = {
                                val o = uiState.bottomBarOrder.toMutableList(); val t = o[index]; o[index] = o[index + 1]; o[index + 1] = t; viewModel.setBottomBarOrder(o)
                            }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.KeyboardArrowDown, "下移", Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // ========== 关于 ==========
        SectionHeader(title = "更多")
        GlassCard(onClick = { navController.navigate(Screen.About.route) }) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(14.dp))
                Text("关于 ClipFlow", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Text("v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
            }
        }
        TextButton(
                        onClick = {
                            viewModel.resetTutorial()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("重置新手教程", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    }
                    Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
}
