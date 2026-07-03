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

        // 默认打开页面: home / douyin / xiaohongshu
        val KEY_DEFAULT_PAGE = stringPreferencesKey("default_page")

        // 底栏按钮排序: JSON 数组如 ["home","douyin","xiaohongshu"]
        val KEY_BOTTOM_BAR_ORDER = stringPreferencesKey("bottom_bar_order")

        // 背景壁纸 URI
        val KEY_WALLPAPER_URI = stringPreferencesKey("wallpaper_uri")

        // 背景壁纸模糊度 0-100
        val KEY_WALLPAPER_BLUR = floatPreferencesKey("wallpaper_blur")

        // 背景壁纸透明度 10-100
        val KEY_WALLPAPER_OPACITY = floatPreferencesKey("wallpaper_opacity")

        // 背景壁纸类型: image / video
        val KEY_WALLPAPER_TYPE = stringPreferencesKey("wallpaper_type")
    }

    val savePath: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SAVE_PATH] ?: ""
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
                listOf("home", "douyin", "xiaohongshu")
            }
        } else {
            listOf("home", "douyin", "xiaohongshu")
        }
    }

    val wallpaperUri: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_WALLPAPER_URI] ?: ""
    }

    val wallpaperBlur: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_WALLPAPER_BLUR] ?: 50f
    }

    val wallpaperOpacity: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_WALLPAPER_OPACITY] ?: 60f
    }

    val wallpaperType: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_WALLPAPER_TYPE] ?: "image"
    }

    // ========== Setters ==========

    suspend fun setSavePath(path: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SAVE_PATH] = path
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

    suspend fun setWallpaperBlur(blur: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_WALLPAPER_BLUR] = blur
        }
    }

    suspend fun setWallpaperOpacity(opacity: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_WALLPAPER_OPACITY] = opacity
        }
    }

    suspend fun setWallpaperType(type: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_WALLPAPER_TYPE] = type
        }
    }
}
