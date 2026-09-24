package com.qihe.clipflow.data.repository

data class BilibiliVideoDetails(
    val bvid: String,
    val parts: List<BilibiliPart>,
    val qualities: List<BilibiliQuality>
)

data class BilibiliPart(
    val index: Int,
    val cid: Long,
    val title: String
)

data class BilibiliQuality(
    val id: Int,
    val label: String,
    val streamUrl: String,
    val audioUrl: String? = null,
    val streamUrls: List<String> = listOf(streamUrl),
    val audioUrls: List<String> = listOfNotNull(audioUrl),
    val previewUrl: String? = null,
    /** 视频编码（avc1 / hev1 / av01…），用于在 UI 中标注并优先选择兼容封装器的流。 */
    val codec: String? = null
)
