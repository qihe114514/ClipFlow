package com.qihe.clipflow.data.bilibili

import org.junit.Assert.assertEquals
import org.junit.Test

class BilibiliCookieUtilsTest {
    @Test
    fun mergeCookieHeadersTrimsAndDeduplicatesCookieTokens() {
        val result = mergeBilibiliCookieHeaders(
            listOf(
                " SESSDATA=one; bili_jct=abc ",
                "SESSDATA=one; DedeUserID=42",
                null,
                ""
            )
        )

        assertEquals("SESSDATA=one; bili_jct=abc; DedeUserID=42", result)
    }

    @Test
    fun mergeCookieHeadersKeepsDistinctPairsFromResponseFragments() {
        assertEquals(
            "SESSDATA=one; bili_jct=csrf; DedeUserID=42",
            mergeBilibiliCookieHeaders(
                listOf(
                    "SESSDATA=one; Path=/",
                    "bili_jct=csrf; HttpOnly",
                    "DedeUserID=42",
                    "SESSDATA=one"
                )
            )
        )
    }
}
