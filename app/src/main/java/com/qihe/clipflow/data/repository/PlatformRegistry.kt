package com.qihe.clipflow.data.repository

import com.qihe.clipflow.data.api.ApiService

data class PlatformDescriptor(
    val platform: SupportedPlatform,
    val historyKey: String,
    val emptyInputMessage: String,
    val fallbackContentType: String? = null,
    val defaultTitle: (ParseResult) -> String
)

object PlatformRegistry {
    val douyin = PlatformDescriptor(
        platform = SupportedPlatform.DOUYIN,
        historyKey = "douyin",
        emptyInputMessage = "请粘贴抖音分享链接",
        defaultTitle = { result ->
            if (result.contentType.isNotEmpty()) {
                "抖音 ${result.contentType}"
            } else {
                "抖音作品"
            }
        }
    )

    val xiaohongshu = PlatformDescriptor(
        platform = SupportedPlatform.XIAOHONGSHU,
        historyKey = "xiaohongshu",
        emptyInputMessage = "请粘贴小红书分享链接",
        fallbackContentType = "note",
        defaultTitle = { "小红书笔记" }
    )

    val descriptors: Map<SupportedPlatform, PlatformDescriptor> = listOf(
        douyin,
        xiaohongshu
    ).associateBy { it.platform }

    fun descriptor(platform: SupportedPlatform): PlatformDescriptor {
        return descriptors.getValue(platform)
    }

    fun createParsers(api: ApiService): Map<SupportedPlatform, PlatformParser> {
        return listOf(
            DouyinPlatformParser(api),
            XiaohongshuPlatformParser(api)
        ).associateBy { it.platform }
    }
}
