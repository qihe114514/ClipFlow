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
    val audioUrls: List<String> = listOfNotNull(audioUrl)
)
