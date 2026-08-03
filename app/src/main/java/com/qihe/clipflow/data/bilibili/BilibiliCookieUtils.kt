package com.qihe.clipflow.data.bilibili

import java.util.Locale

internal fun mergeBilibiliCookieHeaders(headers: Iterable<String?>): String {
    val cookies = linkedMapOf<String, String>()
    headers.asSequence()
        .filterNotNull()
        .flatMap { it.split(';').asSequence() }
        .map(String::trim)
        .filter(String::isNotBlank)
        .mapNotNull { token ->
            val separator = token.indexOf('=')
            if (separator <= 0) return@mapNotNull null
            val name = token.substring(0, separator).trim()
            if (name.lowercase(Locale.ROOT) in cookieAttributes) return@mapNotNull null
            val value = token.substring(separator + 1).trim()
            if (value.isBlank()) return@mapNotNull null
            name to "$name=$value"
        }
        .forEach { (name, cookie) -> cookies.putIfAbsent(name, cookie) }
    return cookies.values.joinToString("; ")
}

private val cookieAttributes = setOf(
    "domain",
    "expires",
    "httponly",
    "max-age",
    "path",
    "partitioned",
    "priority",
    "samesite",
    "secure"
)
