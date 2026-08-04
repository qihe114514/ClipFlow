package com.qihe.clipflow.data.repository

import com.qihe.clipflow.data.api.RetrofitClient

class ParseRepository(
    private val parsers: Map<SupportedPlatform, PlatformParser> =
        PlatformRegistry.createParsers(RetrofitClient.apiService)
) {

    suspend fun parse(
        platform: SupportedPlatform,
        rawInput: String,
        route: DouyinParseRoute = DouyinParseRoute.PRIMARY
    ): Result<ParseResult> {
        val parser = parsers.getValue(platform)
        return parser.parse(normalize(platform, rawInput), route)
    }

    fun normalize(
        platform: SupportedPlatform,
        rawInput: String
    ): String {
        return parsers.getValue(platform).normalizeInput(rawInput)
    }
}
