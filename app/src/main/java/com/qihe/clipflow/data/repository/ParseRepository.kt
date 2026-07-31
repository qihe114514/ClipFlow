package com.qihe.clipflow.data.repository

import com.qihe.clipflow.data.api.RetrofitClient

class ParseRepository(
    private val parsers: Map<SupportedPlatform, PlatformParser> = listOf(
        DouyinPlatformParser(RetrofitClient.apiService),
        XiaohongshuPlatformParser(RetrofitClient.apiService)
    ).associateBy { it.platform }
) {

    suspend fun parseDouyin(rawInput: String): Result<ParseResult> {
        return parse(SupportedPlatform.DOUYIN, rawInput)
    }

    suspend fun parseXiaohongshu(rawInput: String): Result<ParseResult> {
        return parse(SupportedPlatform.XIAOHONGSHU, rawInput)
    }

    suspend fun parse(
        platform: SupportedPlatform,
        rawInput: String
    ): Result<ParseResult> {
        val parser = parsers.getValue(platform)
        return parser.parse(parser.normalizeInput(rawInput))
    }
}
