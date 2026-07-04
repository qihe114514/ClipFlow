package com.qihe.clipflow.data.api.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

// ==================== 抖音 ====================

@Keep
data class DouyinResponse(
    val code: Int,
    val msg: String,
    val data: DouyinData? = null
)

@Keep
data class DouyinData(
    val type: String? = null,
    val title: String? = null,
    val desc: String? = null,
    val author: DouyinAuthor? = null,
    val cover: String? = null,
    val url: String? = null,
    val quality: String? = null,
    @SerializedName("video_backup")
    val videoBackup: List<VideoBackupItem>? = null,
    val images: List<String>? = null,
    @SerializedName("live_photo")
    val livePhoto: List<DouyinLivePhoto>? = null,
    val music: DouyinMusic? = null,
    val extra: DouyinExtra? = null
)

@Keep
data class DouyinExtra(
    @SerializedName("share_url") val shareUrl: String? = null,
    val statistics: DouyinStatistics? = null
)

@Keep
data class DouyinStatistics(
    @SerializedName("digg_count") val diggCount: Long = 0,
    @SerializedName("comment_count") val commentCount: Long = 0,
    @SerializedName("collect_count") val collectCount: Long = 0,
    @SerializedName("share_count") val shareCount: Long = 0
)

@Keep
data class DouyinAuthor(
    val name: String? = null,
    val id: Long? = null,
    val avatar: String? = null
)

@Keep
data class DouyinMusic(
    val title: String? = null,
    val author: String? = null,
    val url: String? = null,
    val cover: String? = null
)

@Keep
data class DouyinLivePhoto(
    val image: String? = null,
    val video: String? = null
)

@Keep
data class VideoBackupItem(
    val url: String? = null,
    val quality: String? = null,
    val label: String? = null,
    val format: String? = null,
    val codec: String? = null,
    @SerializedName("bit_rate") val bitRate: Long = 0,
    val width: Int = 0,
    val height: Int = 0
)

@Keep
data class ContentItem(
    val id: String,
    val type: ContentType,
    val url: String,
    val thumbnailUrl: String? = null,
    val mediaInfo: MediaInfo? = null,
    val description: String = ""
)

@Keep
enum class ContentType(val label: String) {
    VIDEO("视频"),
    IMAGE("图片"),
    AUDIO("音频"),
    LIVE_IMAGE("实况-图"),
    LIVE_VIDEO("实况-视频")
}

@Keep
data class MediaInfo(
    val resolution: String? = null,
    val frameRate: Int? = null,
    val bitrate: String? = null,
    val codec: String? = null,
    val format: String? = null,
    val estimatedSize: String? = null
)

// ==================== 小红书 ====================

@Keep
data class XiaohongshuResponse(
    val code: Int,
    val msg: String,
    val data: com.google.gson.JsonElement? = null
)

@Keep
data class XiaohongshuData(
    val author: String? = null,
    val authorID: String? = null,
    val title: String? = null,
    val desc: String? = null,
    val avatar: String? = null,
    val cover: String? = null,
    val url: String? = null
)
