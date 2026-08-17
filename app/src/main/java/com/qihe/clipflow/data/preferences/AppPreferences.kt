package com.qihe.clipflow.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "clipflow_settings")

class AppPreferences(private val context: Context) {

    companion object {
        // 保存路径
        val KEY_SAVE_PATH = stringPreferencesKey("save_path")
        val KEY_VIDEO_SAVE_PATH = stringPreferencesKey("video_save_path")
        val KEY_IMAGE_SAVE_PATH = stringPreferencesKey("image_save_path")
        val KEY_AUDIO_SAVE_PATH = stringPreferencesKey("audio_save_path")

        // 默认打开页面: home / douyin / xiaohongshu
        val KEY_DEFAULT_PAGE = stringPreferencesKey("default_page")

        // 底栏按钮排序: JSON 数组如 ["home","douyin","xiaohongshu"]
        val KEY_BOTTOM_BAR_ORDER = stringPreferencesKey("bottom_bar_order")

        // 背景壁纸 URI（空 = 使用内置默认壁纸）
        val KEY_WALLPAPER_URI = stringPreferencesKey("wallpaper_uri")

        // 外观模式: system / light / dark；动态配色默认开启
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")

        // 壁纸档位预设: off / low / medium / high；对比度: low / default / high
        val KEY_WALLPAPER_OPACITY_PRESET = stringPreferencesKey("wallpaper_opacity_preset")
        val KEY_WALLPAPER_CONTRAST_PRESET = stringPreferencesKey("wallpaper_contrast_preset")

        // 卡片/按钮玻璃效果档位预设: off / low / medium / high
        val KEY_GLASS_CARD_PRESET = stringPreferencesKey("glass_card_preset")
        val KEY_GLASS_BUTTON_PRESET = stringPreferencesKey("glass_button_preset")

        // 卡片/按钮额外模糊度档位预设: off / low / medium / high
        val KEY_GLASS_CARD_BLUR_PRESET = stringPreferencesKey("glass_card_blur_preset")
        val KEY_GLASS_BUTTON_BLUR_PRESET = stringPreferencesKey("glass_button_blur_preset")

        // 用户是否已同意隐私政策
        val KEY_PRIVACY_AGREED = booleanPreferencesKey("privacy_agreed")

        // 新手教程是否已展示
        val KEY_TUTORIAL_SHOWN = booleanPreferencesKey("tutorial_shown")
    }

    val videoSavePath: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_VIDEO_SAVE_PATH] ?: ""
    }
    val savePath: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SAVE_PATH] ?: ""
    }

    val imageSavePath: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_IMAGE_SAVE_PATH] ?: ""
    }

    val audioSavePath: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUDIO_SAVE_PATH] ?: ""
    }

    val defaultPage: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_PAGE] ?: "home"
    }

    val bottomBarOrder: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val json = prefs[KEY_BOTTOM_BAR_ORDER]
        if (json != null) {
            try {
                json.trim('[', ']').split(",").map { it.trim('"', ' ') }
            } catch (_: Exception) {
                listOf("home", "douyin", "xiaohongshu", "bilibili")
            }
        } else {
            listOf("home", "douyin", "xiaohongshu", "bilibili")
        }
    }

    val wallpaperUri: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_WALLPAPER_URI] ?: ""
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE] ?: "system"
    }

    val dynamicColor: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DYNAMIC_COLOR] ?: true
    }

    val wallpaperOpacityPreset: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_WALLPAPER_OPACITY_PRESET] ?: "medium"
    }

    val wallpaperContrastPreset: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_WALLPAPER_CONTRAST_PRESET] ?: "default"
    }

    val glassCardPreset: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_GLASS_CARD_PRESET] ?: "medium"
    }

    val glassButtonPreset: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_GLASS_BUTTON_PRESET] ?: "medium"
    }

    val glassCardBlurPreset: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_GLASS_CARD_BLUR_PRESET] ?: "medium"
    }

    val glassButtonBlurPreset: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_GLASS_BUTTON_BLUR_PRESET] ?: "low"
    }

    val privacyAgreed: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_PRIVACY_AGREED] ?: false
    }

    val tutorialShown: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_TUTORIAL_SHOWN] ?: false
    }

    // ========== Setters ==========

    suspend fun setSavePath(path: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SAVE_PATH] = path
        }
    }

    suspend fun setVideoSavePath(path: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_VIDEO_SAVE_PATH] = path
        }
    }

    suspend fun setImageSavePath(path: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IMAGE_SAVE_PATH] = path
        }
    }

    suspend fun setAudioSavePath(path: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUDIO_SAVE_PATH] = path
        }
    }

    suspend fun setDefaultPage(page: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEFAULT_PAGE] = page
        }
    }

    suspend fun setBottomBarOrder(order: List<String>) {
        val json = order.joinToString(",") { "\"$it\"" }.let { "[$it]" }
        context.dataStore.edit { prefs ->
            prefs[KEY_BOTTOM_BAR_ORDER] = json
        }
    }

    suspend fun setWallpaperUri(uri: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_WALLPAPER_URI] = uri
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DYNAMIC_COLOR] = enabled
        }
    }

    suspend fun setWallpaperOpacityPreset(preset: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_WALLPAPER_OPACITY_PRESET] = preset
        }
    }

    suspend fun setWallpaperContrastPreset(preset: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_WALLPAPER_CONTRAST_PRESET] = preset
        }
    }

    suspend fun setGlassCardPreset(preset: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_GLASS_CARD_PRESET] = preset
        }
    }

    suspend fun setGlassButtonPreset(preset: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_GLASS_BUTTON_PRESET] = preset
        }
    }

    suspend fun setGlassCardBlurPreset(preset: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_GLASS_CARD_BLUR_PRESET] = preset
        }
    }

    suspend fun setGlassButtonBlurPreset(preset: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_GLASS_BUTTON_BLUR_PRESET] = preset
        }
    }

    suspend fun setPrivacyAgreed(agreed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PRIVACY_AGREED] = agreed
        }
    }

    suspend fun setTutorialShown(shown: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TUTORIAL_SHOWN] = shown
        }
    }
}
