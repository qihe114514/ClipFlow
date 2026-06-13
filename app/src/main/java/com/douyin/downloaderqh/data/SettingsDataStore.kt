package com.douyin.downloaderqh.data

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.douyin.downloaderqh.ui.HistoryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val SAVE_PATH = stringPreferencesKey("save_path")
        val BG_WALLPAPER_URI = stringPreferencesKey("bg_wallpaper_uri")
        val BG_WALLPAPER_TYPE = stringPreferencesKey("bg_wallpaper_type")
        val BG_BLUR_RADIUS = floatPreferencesKey("bg_blur_radius")
        val BG_OPACITY = floatPreferencesKey("bg_opacity")
        val VIDEO_SOUND_ENABLED = booleanPreferencesKey("video_sound_enabled")
        val PARSE_HISTORY = stringPreferencesKey("parse_history")
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

    val videoSoundEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[VIDEO_SOUND_ENABLED] ?: false
    }

    val parseHistory: Flow<List<HistoryEntry>> = context.dataStore.data.map { prefs ->
        val json = prefs[PARSE_HISTORY] ?: "[]"
        try {
            Json.decodeFromString<List<HistoryEntry>>(json)
        } catch (_: Exception) {
            emptyList()
        }
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

    suspend fun setVideoSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[VIDEO_SOUND_ENABLED] = enabled
        }
    }

    suspend fun addParseHistory(entry: HistoryEntry) {
        context.dataStore.edit { prefs ->
            val json = prefs[PARSE_HISTORY] ?: "[]"
            val list = try {
                Json.decodeFromString<MutableList<HistoryEntry>>(json)
            } catch (_: Exception) {
                mutableListOf()
            }
            list.removeAll { it.url == entry.url }
            list.add(0, entry)
            if (list.size > 50) {
                list.removeAt(list.lastIndex)
            }
            prefs[PARSE_HISTORY] = Json.encodeToString(list)
        }
    }

    suspend fun clearParseHistory() {
        context.dataStore.edit { prefs ->
            prefs[PARSE_HISTORY] = "[]"
        }
    }
}