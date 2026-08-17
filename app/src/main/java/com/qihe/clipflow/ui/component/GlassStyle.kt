package com.qihe.clipflow.ui.component

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 四档强度：关闭 / 低 / 默认 / 高，默认选中「默认」。
 * key 为持久化到 DataStore 的字符串值。
 */
enum class IntensityPreset(val key: String, val label: String) {
    OFF("off", "关闭"),
    LOW("low", "低"),
    MEDIUM("medium", "默认"),
    HIGH("high", "高");

    companion object {
        fun fromKey(key: String?): IntensityPreset =
            entries.firstOrNull { it.key == key } ?: MEDIUM
    }
}

/** 对比度三档：低 / 默认 / 高 */
enum class ContrastPreset(val key: String, val label: String) {
    LOW("low", "低"),
    DEFAULT("default", "默认"),
    HIGH("high", "高");

    companion object {
        fun fromKey(key: String?): ContrastPreset =
            entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

/** 玻璃效果作用的组件类型 */
enum class GlassTarget { CARD, BUTTON }

/**
 * 一套玻璃效果参数。
 *
 * @param lensBlur   玻璃效果档位 → lens(blur, refraction) 的模糊半径（dp）
 * @param refraction 玻璃效果档位 → lens(blur, refraction) 的折射强度（dp）
 * @param extraBlur  模糊度档位 → 额外的 blur() 半径（dp）
 */
data class GlassEffectValues(
    val lensBlur: Float = 16f,
    val refraction: Float = 32f,
    val extraBlur: Float = 4f,
) {
    /** 玻璃效果=关闭（额外模糊不参与判断，玻璃关闭即无效果） */
    val isOff: Boolean get() = lensBlur <= 0f && refraction <= 0f
}

/**
 * 玻璃效果档位 → lens 参数。数值与 kyant backdrop 文档示例 / 仓库现有组件保持一致：
 * - 卡片：lens(16dp, 32dp)
 * - 按钮：blur(2dp) + lens(12dp, 24dp)
 * 低 / 高在此基础上缩放，色差全部关闭。
 */
fun glassEffectsFor(preset: IntensityPreset, target: GlassTarget): GlassEffectValues =
    when (target) {
        GlassTarget.CARD -> when (preset) {
            IntensityPreset.OFF -> GlassEffectValues(0f, 0f)
            IntensityPreset.LOW -> GlassEffectValues(8f, 16f)
            IntensityPreset.MEDIUM -> GlassEffectValues(16f, 32f)
            IntensityPreset.HIGH -> GlassEffectValues(24f, 48f)
        }
        GlassTarget.BUTTON -> when (preset) {
            IntensityPreset.OFF -> GlassEffectValues(0f, 0f)
            IntensityPreset.LOW -> GlassEffectValues(8f, 16f)
            IntensityPreset.MEDIUM -> GlassEffectValues(12f, 24f)
            IntensityPreset.HIGH -> GlassEffectValues(18f, 36f)
        }
    }

/** 模糊度档位 → 额外 blur() 半径（dp），默认「低」= 4dp */
fun blurExtraFor(preset: IntensityPreset): Float = when (preset) {
    IntensityPreset.OFF -> 0f
    IntensityPreset.LOW -> 4f
    IntensityPreset.MEDIUM -> 8f
    IntensityPreset.HIGH -> 16f
}

/** 壁纸透明度档位 → 百分比（关闭 = 0%，隐藏壁纸） */
fun wallpaperOpacityPercent(preset: IntensityPreset): Float = when (preset) {
    IntensityPreset.OFF -> 0f
    IntensityPreset.LOW -> 35f
    IntensityPreset.MEDIUM -> 75f
    IntensityPreset.HIGH -> 100f
}

/** 壁纸对比度档位 → ColorMatrix 缩放系数 */
fun wallpaperContrastScale(preset: ContrastPreset): Float = when (preset) {
    ContrastPreset.LOW -> 0.8f
    ContrastPreset.DEFAULT -> 1.0f
    ContrastPreset.HIGH -> 1.3f
}

/**
 * 卡片与按钮各自的玻璃效果参数，由 ClipFlowNavHost 从 DataStore 读取后提供。
 * 默认值：玻璃效果=默认档，额外模糊=低档。
 */
data class GlassStyle(
    val card: GlassEffectValues = GlassEffectValues(
        lensBlur = 16f,
        refraction = 32f,
        extraBlur = blurExtraFor(IntensityPreset.MEDIUM),
    ),
    val button: GlassEffectValues = GlassEffectValues(
        lensBlur = 12f,
        refraction = 24f,
        extraBlur = blurExtraFor(IntensityPreset.LOW),
    ),
    /** 壁纸对比度档位 → 卡片内壁纸内容的对比度系数（默认 1.0） */
    val contrast: Float = 1f,
)

/** 全局玻璃效果样式 CompositionLocal */
val LocalGlassStyle = staticCompositionLocalOf { GlassStyle() }


/**
 * 壁纸平均亮度（0..1），由 BackgroundWallpaperLayer 采样后提供。
 * GlassCard 据此做自适应文字颜色（暗壁纸→浅色文字，亮壁纸→深色文字）。
 */
