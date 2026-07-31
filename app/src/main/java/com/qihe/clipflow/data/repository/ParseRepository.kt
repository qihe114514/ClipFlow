package com.qihe.clipflow.data.repository

import com.qihe.clipflow.data.api.RetrofitClient

class ParseRepository(
    private val parsers: Map<SupportedPlatform, PlatformParser> =
        PlatformRegistry.createParsers(RetrofitClient.apiService)
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
        return parser.parse(normalize(platform, rawInput))
    }

    fun normalize(
        platform: SupportedPlatform,
        rawInput: String
    ): String {
        return parsers.getValue(platform).normalizeInput(rawInput)
    }
}
