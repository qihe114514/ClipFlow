package com.qihe.clipflow.data.repository

import com.qihe.clipflow.data.api.model.ContentItem
import com.qihe.clipflow.data.api.model.DouyinStatistics
import com.qihe.clipflow.data.api.model.VideoBackupItem

data class ParseResult(
    val items: List<ContentItem>,
    val title: String = "",
    val desc: String = "",
    val cover: String = "",
    val authorName: String = "",
    val authorAvatar: String = "",
    val contentType: String = "",
    val musicUrl: String = "",
    val musicTitle: String = "",
    val shareUrl: String = "",
    val stats: DouyinStatistics? = null,
    val videoBackups: List<VideoBackupItem> = emptyList()
)

enum class SupportedPlatform {
    DOUYIN,
    XIAOHONGSHU
}

data class ParseFailure(
    val message: String,
    val cause: Throwable? = null
)

class ParseException(
    val failure: ParseFailure
) : Exception(failure.message, failure.cause)
