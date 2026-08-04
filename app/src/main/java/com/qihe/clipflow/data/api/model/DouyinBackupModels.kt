package com.qihe.clipflow.data.api.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class DouyinBackupResponse(
    val code: Int = 0,
    val message: String? = null,
    val data: DouyinBackupData? = null
)

@Keep
data class DouyinBackupData(
    val type: String? = null,
    val desc: String? = null,
    val author: DouyinBackupAuthor? = null,
    @SerializedName("cover_data") val coverData: DouyinBackupCoverData? = null,
    @SerializedName("video_data") val videoData: DouyinBackupVideoData? = null,
    @SerializedName("image_data") val imageData: DouyinBackupImageData? = null,
    val statistics: DouyinStatistics? = null
)

@Keep
data class DouyinBackupAuthor(
    val nickname: String? = null,
    @SerializedName("avatar_thumb") val avatarThumb: DouyinBackupImageRef? = null
)

@Keep
data class DouyinBackupCoverData(
    val cover: DouyinBackupImageRef? = null
)

@Keep
data class DouyinBackupImageRef(
    @SerializedName("url_list") val urlList: List<String>? = null
)

@Keep
data class DouyinBackupVideoData(
    @SerializedName("nwm_video_url_HQ") val noWatermarkHighQualityUrl: String? = null,
    @SerializedName("nwm_video_url") val noWatermarkUrl: String? = null
)

@Keep
data class DouyinBackupImageData(
    @SerializedName("no_watermark_image_list") val noWatermarkImages: List<String>? = null
)
