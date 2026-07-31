package com.qihe.clipflow.data.repository

import com.qihe.clipflow.data.api.ApiService

data class PlatformDescriptor(
    val platform: SupportedPlatform,
    val historyKey: String,
    val fallbackContentType: String? = null,
    val defaultTitle: (ParseResult) -> String,
    val errorMessage: (ParseFailure) -> String
)

/**
 * New platforms add one parser adapter and one registration here. Existing adapters stay unchanged.
 */
data class PlatformRegistration(
    val descriptor: PlatformDescriptor,
    val createParser: (ApiService) -> PlatformParser
)

object PlatformRegistry {
    private val registrations = listOf(
        PlatformRegistration(
            descriptor = PlatformDescriptor(
                platform = SupportedPlatform.DOUYIN,
                historyKey = "douyin",
                defaultTitle = { result ->
                    if (result.contentType.isNotEmpty()) {
                        "抖音 ${result.contentType}"
                    } else {
                        "抖音作品"
                    }
                },
                errorMessage = { failure ->
                    when (failure.kind) {
                        ParseErrorKind.EMPTY_INPUT -> "请粘贴抖音分享链接"
                        ParseErrorKind.INVALID_INPUT -> "请输入有效的抖音链接"
                        ParseErrorKind.REMOTE_FAILURE -> failure.detail ?: "解析失败，请稍后重试"
                        ParseErrorKind.NO_DOWNLOADABLE_CONTENT -> "未找到可下载的内容"
                        ParseErrorKind.UNEXPECTED -> "解析失败，请检查链接后重试"
                    }
                }
            ),
            createParser = ::DouyinPlatformParser
        ),
        PlatformRegistration(
            descriptor = PlatformDescriptor(
                platform = SupportedPlatform.XIAOHONGSHU,
                historyKey = "xiaohongshu",
                fallbackContentType = "note",
                defaultTitle = { "小红书笔记" },
                errorMessage = { failure ->
                    when (failure.kind) {
                        ParseErrorKind.EMPTY_INPUT -> "请粘贴小红书分享链接"
                        ParseErrorKind.INVALID_INPUT -> "请输入有效的小红书链接"
                        ParseErrorKind.REMOTE_FAILURE -> failure.detail ?: "解析失败，请确认链接是否有效"
                        ParseErrorKind.NO_DOWNLOADABLE_CONTENT -> "未找到可下载的内容，请确认链接是否有效"
                        ParseErrorKind.UNEXPECTED -> "解析失败，请检查链接后重试"
                    }
                }
            ),
            createParser = ::XiaohongshuPlatformParser
        )
    )

    private val registrationsByPlatform = registrations.associateBy { it.descriptor.platform }

    fun descriptor(platform: SupportedPlatform): PlatformDescriptor {
        return registrationsByPlatform.getValue(platform).descriptor
    }

    fun createParsers(api: ApiService): Map<SupportedPlatform, PlatformParser> {
        return registrations.associate { registration ->
            val parser = registration.createParser(api)
            parser.platform to parser
        }
    }
}
