package com.douyin.downloader.data

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val SAVE_PATH = stringPreferencesKey("save_path")
        val BG_WALLPAPER_URI = stringPreferencesKey("bg_wallpaper_uri")
        val BG_WALLPAPER_TYPE = stringPreferencesKey("bg_wallpaper_type")
        val BG_BLUR_RADIUS = floatPreferencesKey("bg_blur_radius")
        val BG_OPACITY = floatPreferencesKey("bg_opacity")
    }

    val savePath: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[SAVE_PATH] ?: ""
    }

    val bgWallpaperUri: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[BG_WALLPAPER_URI] ?: ""
    }

    val bgWallpaperType: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[BG_WALLPAPER_TYPE] ?: "none"
    }

    val bgBlurRadius: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[BG_BLUR_RADIUS] ?: 0f
    }

    val bgOpacity: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[BG_OPACITY] ?: 0.5f
    }

    suspend fun setSavePath(path: String) {
        context.dataStore.edit { prefs ->
            prefs[SAVE_PATH] = path
        }
    }

    suspend fun setBgWallpaper(uri: String, type: String) {
        context.dataStore.edit { prefs ->
            prefs[BG_WALLPAPER_URI] = uri
            prefs[BG_WALLPAPER_TYPE] = type
        }
    }

    suspend fun setBgBlurRadius(radius: Float) {
        context.dataStore.edit { prefs ->
            prefs[BG_BLUR_RADIUS] = radius.coerceIn(0f, 25f)
        }
    }

    suspend fun setBgOpacity(opacity: Float) {
        context.dataStore.edit { prefs ->
            prefs[BG_OPACITY] = opacity.coerceIn(0f, 1f)
        }
    }
}

