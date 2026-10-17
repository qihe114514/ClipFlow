package com.qihe.clipflow.ui.theme

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.palette.graphics.Palette
import com.qihe.clipflow.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class ThemeMode(val key: String, val label: String) {
    SYSTEM("system", "跟随系统"),
    LIGHT("light", "浅色"),
    DARK("dark", "深色");

    companion object {
        fun fromKey(key: String?): ThemeMode =
            entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

fun resolveDarkTheme(mode: ThemeMode, systemDarkTheme: Boolean): Boolean = when (mode) {
    ThemeMode.SYSTEM -> systemDarkTheme
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@androidx.annotation.ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
fun shouldUseDynamicColor(dynamicColor: Boolean, sdkInt: Int): Boolean =
    dynamicColor && sdkInt >= Build.VERSION_CODES.S

private val LightColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBE8FF),
    onPrimaryContainer = Color(0xFF001B3E),
    secondary = BrandPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEBE0FF),
    onSecondaryContainer = Color(0xFF22005D),
    surface = Color(0xFFFEFBFF),
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    background = Color(0xFFFEFBFF),
    onBackground = Color(0xFF1B1B1F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0)
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandBlueDark,
    onPrimary = Color(0xFF003062),
    primaryContainer = Color(0xFF00468C),
    onPrimaryContainer = Color(0xFFDBE8FF),
    secondary = BrandPurpleDark,
    onSecondary = Color(0xFF37008A),
    secondaryContainer = Color(0xFF4F00C2),
    onSecondaryContainer = Color(0xFFEBE0FF),
    surface = Color(0xFF1B1B1F),
    onSurface = Color(0xFFE4E1E6),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    background = Color(0xFF1B1B1F),
    onBackground = Color(0xFFE4E1E6),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
)

@Composable
fun ClipFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    wallpaperUri: String = "",
    wallpaperEnabled: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val baseScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // Dynamic color sources, in priority order:
    // 1. A visible wallpaper -> extract its palette.
    // 2. Wallpaper off -> system Monet (Android 12+), else brand colors.
    val wallpaperScheme by produceState<ColorScheme?>(
        initialValue = null,
        dynamicColor,
        wallpaperEnabled,
        wallpaperUri,
        darkTheme,
    ) {
        value = if (dynamicColor && wallpaperEnabled) {
            withContext(Dispatchers.IO) {
                extractWallpaperScheme(context, wallpaperUri, darkTheme)
            }
        } else {
            null
        }
    }

    val colorScheme = when {
        !dynamicColor -> baseScheme
        wallpaperEnabled -> wallpaperScheme ?: baseScheme
        shouldUseDynamicColor(true, Build.VERSION.SDK_INT) ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        else -> baseScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ClipFlowTypography,
        content = content
    )
}

private fun extractWallpaperScheme(
    context: android.content.Context,
    wallpaperUri: String,
    darkTheme: Boolean,
): ColorScheme? {
    val bitmap = runCatching { loadWallpaperBitmap(context, wallpaperUri) }.getOrNull() ?: return null
    val palette = runCatching { Palette.from(bitmap).clearFilters().generate() }.getOrNull() ?: return null

    val seed = palette.vibrantSwatch?.rgb
        ?: palette.dominantSwatch?.rgb
        ?: palette.mutedSwatch?.rgb
        ?: return null
    val secondarySeed = palette.mutedSwatch?.rgb
        ?: palette.darkVibrantSwatch?.rgb
        ?: seed
    val tertiarySeed = palette.lightVibrantSwatch?.rgb
        ?: palette.darkMutedSwatch?.rgb
        ?: secondarySeed

    val base = if (darkTheme) DarkColorScheme else LightColorScheme
    val primary = adjustSeed(Color(seed), darkTheme)
    val secondary = adjustSeed(Color(secondarySeed), darkTheme)
    val tertiary = adjustSeed(Color(tertiarySeed), darkTheme)

    val primaryContainer = if (darkTheme) {
        blend(base.surface, primary, 0.30f)
    } else {
        blend(base.surface, primary, 0.20f)
    }
    val secondaryContainer = if (darkTheme) {
        blend(base.surface, secondary, 0.26f)
    } else {
        blend(base.surface, secondary, 0.16f)
    }

    return base.copy(
        primary = primary,
        onPrimary = onColorFor(primary),
        primaryContainer = primaryContainer,
        onPrimaryContainer = if (darkTheme) blend(primary, Color.White, 0.72f) else blend(primary, Color.Black, 0.62f),
        secondary = secondary,
        onSecondary = onColorFor(secondary),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = if (darkTheme) blend(secondary, Color.White, 0.72f) else blend(secondary, Color.Black, 0.62f),
        tertiary = tertiary,
        onTertiary = onColorFor(tertiary),
        tertiaryContainer = if (darkTheme) blend(base.surface, tertiary, 0.30f) else blend(base.surface, tertiary, 0.20f),
        onTertiaryContainer = if (darkTheme) blend(tertiary, Color.White, 0.72f) else blend(tertiary, Color.Black, 0.62f),
        surfaceTint = primary,
    )
}

private fun loadWallpaperBitmap(
    context: android.content.Context,
    wallpaperUri: String,
): Bitmap? {
    val source = if (wallpaperUri.isBlank()) {
        BitmapFactory.decodeResource(context.resources, R.drawable.default_wallpaper)
    } else {
        context.contentResolver.openInputStream(Uri.parse(wallpaperUri))?.use {
            BitmapFactory.decodeStream(it)
        }
    } ?: return null

    // Palette extraction only needs a small sample; keep it off the main thread.
    val longest = maxOf(source.width, source.height)
    if (longest <= 192) return source
    val scale = 192f / longest
    val width = (source.width * scale).toInt().coerceAtLeast(1)
    val height = (source.height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(source, width, height, true)
}

private fun adjustSeed(color: Color, darkTheme: Boolean): Color {
    var result = color
    if (darkTheme) {
        if (result.luminance() < 0.45f) result = blend(result, Color.White, 0.35f)
        if (result.luminance() > 0.86f) result = blend(result, Color.Black, 0.15f)
    } else {
        if (result.luminance() > 0.62f) result = blend(result, Color.Black, 0.32f)
        if (result.luminance() < 0.16f) result = blend(result, Color.White, 0.22f)
    }
    return result
}

private fun onColorFor(background: Color): Color =
    if (background.luminance() > 0.55f) Color(0xFF141414) else Color.White

private fun blend(from: Color, to: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = from.red + (to.red - from.red) * f,
        green = from.green + (to.green - from.green) * f,
        blue = from.blue + (to.blue - from.blue) * f,
        alpha = 1f,
    )
}
