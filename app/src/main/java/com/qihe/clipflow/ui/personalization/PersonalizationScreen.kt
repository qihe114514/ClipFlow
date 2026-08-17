package com.qihe.clipflow.ui.personalization

import android.app.Application
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.qihe.clipflow.R
import com.qihe.clipflow.data.preferences.AppPreferences
import com.qihe.clipflow.ui.component.ContrastPreset
import com.qihe.clipflow.ui.component.IntensityPreset
import com.qihe.clipflow.ui.component.LiquidButton
import com.qihe.clipflow.ui.component.LiquidToggle
import com.qihe.clipflow.ui.component.secondaryPageEntrance
import com.qihe.clipflow.ui.component.liquid.PROGRESSIVE_TOPBAR_CONTENT_START_DP
import com.qihe.clipflow.ui.component.wallpaperOpacityPercent
import com.qihe.clipflow.ui.components.GlassCard
import com.qihe.clipflow.ui.components.withTitleShadow
import com.qihe.clipflow.ui.settings.SectionHeader
import com.qihe.clipflow.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PersonalizationUiState(
    val wallpaperUri: String = "",
    val themeMode: String = ThemeMode.SYSTEM.key,
    val dynamicColor: Boolean = true,
    val wallpaperOpacityPreset: String = IntensityPreset.MEDIUM.key,
    val wallpaperContrastPreset: String = ContrastPreset.DEFAULT.key,
    val cardPreset: String = IntensityPreset.MEDIUM.key,
    val cardBlurPreset: String = IntensityPreset.MEDIUM.key,
    val buttonPreset: String = IntensityPreset.MEDIUM.key,
    val buttonBlurPreset: String = IntensityPreset.LOW.key,
)

class PersonalizationViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = AppPreferences(application)

    private val _uiState = MutableStateFlow(PersonalizationUiState())
    val uiState: StateFlow<PersonalizationUiState> = _uiState

    init {
        viewModelScope.launch {
            launch { prefs.wallpaperUri.collect { _uiState.value = _uiState.value.copy(wallpaperUri = it) } }
            launch { prefs.themeMode.collect { _uiState.value = _uiState.value.copy(themeMode = it) } }
            launch { prefs.dynamicColor.collect { _uiState.value = _uiState.value.copy(dynamicColor = it) } }
            launch { prefs.wallpaperOpacityPreset.collect { _uiState.value = _uiState.value.copy(wallpaperOpacityPreset = it) } }
            launch { prefs.wallpaperContrastPreset.collect { _uiState.value = _uiState.value.copy(wallpaperContrastPreset = it) } }
            launch { prefs.glassCardPreset.collect { _uiState.value = _uiState.value.copy(cardPreset = it) } }
            launch { prefs.glassCardBlurPreset.collect { _uiState.value = _uiState.value.copy(cardBlurPreset = it) } }
            launch { prefs.glassButtonPreset.collect { _uiState.value = _uiState.value.copy(buttonPreset = it) } }
            launch { prefs.glassButtonBlurPreset.collect { _uiState.value = _uiState.value.copy(buttonBlurPreset = it) } }
        }
    }

    fun setWallpaperUri(uri: String) {
        _uiState.value = _uiState.value.copy(wallpaperUri = uri)
        viewModelScope.launch { prefs.setWallpaperUri(uri) }
    }

    fun setThemeMode(mode: String) {
        _uiState.value = _uiState.value.copy(themeMode = mode)
        viewModelScope.launch { prefs.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(dynamicColor = enabled)
        viewModelScope.launch { prefs.setDynamicColor(enabled) }
    }

    fun setWallpaperOpacityPreset(preset: String) {
        _uiState.value = _uiState.value.copy(wallpaperOpacityPreset = preset)
        viewModelScope.launch { prefs.setWallpaperOpacityPreset(preset) }
    }

    fun setWallpaperContrastPreset(preset: String) {
        _uiState.value = _uiState.value.copy(wallpaperContrastPreset = preset)
        viewModelScope.launch { prefs.setWallpaperContrastPreset(preset) }
    }

    fun setCardPreset(preset: String) {
        _uiState.value = _uiState.value.copy(cardPreset = preset)
        viewModelScope.launch { prefs.setGlassCardPreset(preset) }
    }

    fun setCardBlurPreset(preset: String) {
        _uiState.value = _uiState.value.copy(cardBlurPreset = preset)
        viewModelScope.launch { prefs.setGlassCardBlurPreset(preset) }
    }

    fun setButtonPreset(preset: String) {
        _uiState.value = _uiState.value.copy(buttonPreset = preset)
        viewModelScope.launch { prefs.setGlassButtonPreset(preset) }
    }

    fun setButtonBlurPreset(preset: String) {
        _uiState.value = _uiState.value.copy(buttonBlurPreset = preset)
        viewModelScope.launch { prefs.setGlassButtonBlurPreset(preset) }
    }

    fun resetAll() {
        _uiState.value = PersonalizationUiState()
        viewModelScope.launch {
            prefs.setWallpaperUri("")
            prefs.setThemeMode(ThemeMode.SYSTEM.key)
            prefs.setDynamicColor(true)
            prefs.setWallpaperOpacityPreset(IntensityPreset.MEDIUM.key)
            prefs.setWallpaperContrastPreset(ContrastPreset.DEFAULT.key)
            prefs.setGlassCardPreset(IntensityPreset.MEDIUM.key)
            prefs.setGlassCardBlurPreset(IntensityPreset.MEDIUM.key)
            prefs.setGlassButtonPreset(IntensityPreset.MEDIUM.key)
            prefs.setGlassButtonBlurPreset(IntensityPreset.LOW.key)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PersonalizationScreen(
    navController: NavHostController,
    viewModel: PersonalizationViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var advancedExpanded by rememberSaveable { mutableStateOf(false) }
    var showResetSheet by rememberSaveable { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            viewModel.setWallpaperUri(it.toString())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = PROGRESSIVE_TOPBAR_CONTENT_START_DP.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AppearancePreview(
            state = uiState,
            onReset = { showResetSheet = true },
        )

        SectionHeader(title = "外观模式")
        GlassCard(modifier = Modifier.fillMaxWidth().secondaryPageEntrance(1), cornerRadius = 20.dp) {
            ThemeModeButtons(
                selectedKey = uiState.themeMode,
                onSelect = viewModel::setThemeMode,
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("动态配色", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                        Text(
                            "使用系统取色",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    LiquidToggle(
                        checked = uiState.dynamicColor,
                        onCheckedChange = viewModel::setDynamicColor,
                    )
                }
            }
        }

        SectionHeader(title = "背景壁纸")
        GlassCard(modifier = Modifier.fillMaxWidth().secondaryPageEntrance(2), cornerRadius = 20.dp) {
            WallpaperSourceRow(
                hasCustomWallpaper = uiState.wallpaperUri.isNotEmpty(),
                onChoose = { imagePickerLauncher.launch("image/*") },
                onReset = { viewModel.setWallpaperUri("") },
            )
            Spacer(Modifier.height(16.dp))
            ControlLabel("透明度")
            Spacer(Modifier.height(6.dp))
            PresetButtons(
                selectedKey = uiState.wallpaperOpacityPreset,
                onSelect = viewModel::setWallpaperOpacityPreset,
            )
            if (IntensityPreset.fromKey(uiState.wallpaperOpacityPreset) != IntensityPreset.OFF) {
                Spacer(Modifier.height(16.dp))
                ControlLabel("对比度")
                Spacer(Modifier.height(6.dp))
                ContrastButtons(
                    selectedKey = uiState.wallpaperContrastPreset,
                    onSelect = viewModel::setWallpaperContrastPreset,
                )
            }
        }

        SectionHeader(title = "玻璃效果")
        GlassCard(modifier = Modifier.fillMaxWidth().secondaryPageEntrance(3), cornerRadius = 20.dp) {
            GlassSummaryRow(
                icon = Icons.Filled.BlurOn,
                label = "卡片",
                value = "效果${presetLabel(uiState.cardPreset)} · 模糊${blurPresetLabel(IntensityPreset.fromKey(uiState.cardBlurPreset))}",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f))
            GlassSummaryRow(
                icon = Icons.Filled.AutoAwesome,
                label = "按钮",
                value = "效果${presetLabel(uiState.buttonPreset)} · 模糊${blurPresetLabel(IntensityPreset.fromKey(uiState.buttonBlurPreset))}",
            )
            Spacer(Modifier.height(14.dp))
            LiquidButton(
                onClick = { advancedExpanded = !advancedExpanded },
                modifier = Modifier.fillMaxWidth(),
                height = 40.dp,
                contentPadding = PaddingValues(horizontal = 14.dp),
            ) {
                Icon(Icons.Filled.Tune, null, Modifier.size(18.dp))
                Text("高级微调", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                Icon(
                    if (advancedExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    null,
                    Modifier.size(18.dp),
                )
            }
            if (advancedExpanded) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f))
                Spacer(Modifier.height(16.dp))
                AdvancedGlassControls(
                    title = "卡片",
                    effectKey = uiState.cardPreset,
                    blurKey = uiState.cardBlurPreset,
                    onEffectSelect = viewModel::setCardPreset,
                    onBlurSelect = viewModel::setCardBlurPreset,
                )
                Spacer(Modifier.height(20.dp))
                AdvancedGlassControls(
                    title = "按钮",
                    effectKey = uiState.buttonPreset,
                    blurKey = uiState.buttonBlurPreset,
                    onEffectSelect = viewModel::setButtonPreset,
                    onBlurSelect = viewModel::setButtonBlurPreset,
                )
            }
        }
    }

    if (showResetSheet) {
        ModalBottomSheet(onDismissRequest = { showResetSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("恢复默认", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "将恢复跟随系统、动态配色、默认壁纸和全部玻璃效果参数。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    LiquidButton(
                        onClick = { showResetSheet = false },
                        modifier = Modifier.weight(1f),
                        height = 44.dp,
                    ) {
                        Text("取消", style = MaterialTheme.typography.labelLarge)
                    }
                    LiquidButton(
                        onClick = {
                            viewModel.resetAll()
                            showResetSheet = false
                        },
                        modifier = Modifier.weight(1f),
                        tint = MaterialTheme.colorScheme.error,
                        height = 44.dp,
                    ) {
                        Text("恢复默认", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppearancePreview(state: PersonalizationUiState, onReset: () -> Unit) {
    val context = LocalContext.current
    val opacityPreset = IntensityPreset.fromKey(state.wallpaperOpacityPreset)
    val overlayAlpha = 1f - wallpaperOpacityPercent(opacityPreset) / 100f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(144.dp)
            .clip(RoundedCornerShape(20.dp)),
    ) {
            if (opacityPreset == IntensityPreset.OFF) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(if (state.wallpaperUri.isEmpty()) R.drawable.default_wallpaper else Uri.parse(state.wallpaperUri))
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = overlayAlpha)),
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
                    Text("当前外观", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${ThemeMode.fromKey(state.themeMode).label} · ${if (state.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "动态配色" else "应用配色"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LiquidButton(
                    onClick = onReset,
                    modifier = Modifier.align(Alignment.End),
                    height = 32.dp,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) {
                    Icon(Icons.Filled.Restore, null, Modifier.size(14.dp))
                    Text("恢复默认", style = MaterialTheme.typography.labelSmall)
                }
            }
    }
}

@Composable
private fun WallpaperSourceRow(
    hasCustomWallpaper: Boolean,
    onChoose: () -> Unit,
    onReset: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("图片来源", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Text(
                if (hasCustomWallpaper) "已选择自定义图片" else "默认图片",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LiquidButton(
            onClick = onChoose,
            modifier = Modifier.size(36.dp),
            shape = { androidx.compose.foundation.shape.CircleShape },
            height = 36.dp,
            contentPadding = PaddingValues(0.dp),
        ) {
            Icon(Icons.Filled.Image, contentDescription = "选择图片", modifier = Modifier.size(18.dp))
        }
        if (hasCustomWallpaper) {
            Spacer(Modifier.size(8.dp))
            LiquidButton(
                onClick = onReset,
                modifier = Modifier.size(36.dp),
                shape = { androidx.compose.foundation.shape.CircleShape },
                height = 36.dp,
                contentPadding = PaddingValues(0.dp),
            ) {
                Icon(Icons.Filled.Restore, contentDescription = "恢复默认图片", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun GlassSummaryRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.size(10.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AdvancedGlassControls(
    title: String,
    effectKey: String,
    blurKey: String,
    onEffectSelect: (String) -> Unit,
    onBlurSelect: (String) -> Unit,
) {
    Text(title, style = MaterialTheme.typography.titleSmall.withTitleShadow(), fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(10.dp))
    ControlLabel("玻璃效果")
    Spacer(Modifier.height(6.dp))
    PresetButtons(selectedKey = effectKey, onSelect = onEffectSelect)
    Spacer(Modifier.height(12.dp))
    ControlLabel("模糊度")
    Spacer(Modifier.height(6.dp))
    PresetButtons(selectedKey = blurKey, onSelect = onBlurSelect, labelOf = ::blurPresetLabel)
}

@Composable
private fun ControlLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ThemeModeButtons(selectedKey: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ThemeMode.entries.forEach { mode ->
            val selected = mode.key == selectedKey
            LiquidButton(
                onClick = { onSelect(mode.key) },
                modifier = Modifier.weight(1f),
                tint = if (selected) MaterialTheme.colorScheme.primary else Color.Unspecified,
                height = 38.dp,
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                Text(
                    mode.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) Color.White else Color.Unspecified,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PresetButtons(
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    labelOf: (IntensityPreset) -> String = { it.label },
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IntensityPreset.entries.forEach { preset ->
            val selected = preset.key == selectedKey
            LiquidButton(
                onClick = { onSelect(preset.key) },
                modifier = Modifier.widthIn(min = 56.dp),
                tint = if (selected) MaterialTheme.colorScheme.primary else Color.Unspecified,
                height = 36.dp,
                contentPadding = PaddingValues(horizontal = 14.dp),
            ) {
                Text(
                    labelOf(preset),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) Color.White else Color.Unspecified,
                )
            }
        }
    }
}

@Composable
private fun ContrastButtons(selectedKey: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ContrastPreset.entries.forEach { preset ->
            val selected = preset.key == selectedKey
            LiquidButton(
                onClick = { onSelect(preset.key) },
                modifier = Modifier.weight(1f),
                tint = if (selected) MaterialTheme.colorScheme.primary else Color.Unspecified,
                height = 36.dp,
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                Text(
                    preset.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) Color.White else Color.Unspecified,
                )
            }
        }
    }
}

private fun presetLabel(key: String): String = IntensityPreset.fromKey(key).label

private fun blurPresetLabel(preset: IntensityPreset): String = when (preset) {
    IntensityPreset.OFF -> "关闭"
    IntensityPreset.LOW -> "低"
    IntensityPreset.MEDIUM -> "中"
    IntensityPreset.HIGH -> "高"
}
