package com.qihe.clipflow.data.repository

interface PlatformParser {
    val platform: SupportedPlatform

    fun supports(rawInput: String): Boolean

    fun normalizeInput(rawInput: String): String

    suspend fun parse(normalizedInput: String): Result<ParseResult>
}
