package com.qihe.clipflow.data.bilibili

import java.io.IOException
import java.util.ArrayDeque
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Timeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BilibiliLoginClientTest {
    @Test
    fun generateQrCodeUsesDesktopHeaders() = runBlocking {
        val factory = QueueCallFactory(
            FakeResponse(
                """
                {"code":0,"data":{"url":"https://account.bilibili.com/scan?q=1","qrcode_key":"key"}}
                """.trimIndent()
            )
        )

        val result = BilibiliLoginClient(factory).generateQrCode().getOrThrow()

        assertEquals("key", result.key)
        assertEquals("https://account.bilibili.com/scan?q=1", result.url)
        assertTrue(factory.requests.single().header("User-Agent").orEmpty().contains("Windows NT"))
        assertEquals("https://passport.bilibili.com/login", factory.requests.single().header("Referer"))
    }

    @Test
    fun successfulQrPollReturnsMergedResponseCookies() = runBlocking {
        val factory = QueueCallFactory(
            FakeResponse(
                """
                {"code":0,"data":{"url":"https://bilibili.com/callback?SESSDATA=from-url&bili_jct=csrf&DedeUserID=42&gourl=https%3A%2F%2Fwww.bilibili.com","refresh_token":"refresh","code":0}}
                """.trimIndent(),
                listOf("sid=from-header; Path=/")
            )
        )

        val result = BilibiliLoginClient(factory).pollQrCode("key").getOrThrow()

        assertEquals(
            BilibiliQrPollStatus.Success(
                "https://bilibili.com/callback?SESSDATA=from-url&bili_jct=csrf&DedeUserID=42&gourl=https%3A%2F%2Fwww.bilibili.com",
                "refresh"
            ),
            result.status
        )
        assertEquals("SESSDATA=from-url; bili_jct=csrf; DedeUserID=42; sid=from-header", result.cookie)
    }

    @Test
    fun callbackCookiesIgnoreRedirectParametersAndDecodeValues() {
        assertEquals(
            "SESSDATA=session value; bili_jct=csrf; DedeUserID=42",
            extractBilibiliCallbackCookies(
                "https://passport.bilibili.com/login/success?SESSDATA=session%20value&bili_jct=csrf&DedeUserID=42&gourl=https%3A%2F%2Fwww.bilibili.com"
            )
        )
    }

    private data class FakeResponse(
        val body: String,
        val cookies: List<String> = emptyList()
    )

    private class QueueCallFactory(vararg responses: FakeResponse) : Call.Factory {
        private val queue = ArrayDeque(responses.toList())
        val requests = mutableListOf<Request>()

        override fun newCall(request: Request): Call {
            requests += request
            val response = queue.removeFirst()
            return object : Call {
                private var executed = false

                override fun request(): Request = request

                override fun execute(): Response {
                    check(!executed)
                    executed = true
                    val headers = Headers.Builder().apply {
                        response.cookies.forEach { add("Set-Cookie", it) }
                    }.build()
                    return Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .headers(headers)
                        .body(response.body.toResponseBody("application/json".toMediaType()))
                        .build()
                }

                override fun enqueue(responseCallback: Callback) {
                    responseCallback.onFailure(this, IOException("Not used in unit test"))
                }

                override fun cancel() = Unit
                override fun isExecuted(): Boolean = executed
                override fun isCanceled(): Boolean = false
                override fun timeout(): Timeout = Timeout.NONE
                override fun clone(): Call = this
            }
        }
    }
}
