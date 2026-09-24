package com.qihe.clipflow.util

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class HttpRetryTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun client(maxRetries: Int = 1) = OkHttpClient.Builder()
        .addInterceptor(HttpRetry.interceptor(maxRetries))
        .build()

    @Test
    fun getRetriesOnceAfterServerError() {
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))

        client().newCall(Request.Builder().url(server.url("/parse")).build()).execute().use { response ->
            assertEquals(200, response.code)
        }
        assertEquals(2, server.requestCount)
    }

    @Test
    fun getGivesUpAfterMaxRetries() {
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(MockResponse().setResponseCode(503))

        client().newCall(Request.Builder().url(server.url("/parse")).build()).execute().use { response ->
            assertEquals(503, response.code)
        }
        assertEquals(2, server.requestCount)
    }

    @Test
    fun postIsNotRetried() {
        server.enqueue(MockResponse().setResponseCode(500))

        client().newCall(
            Request.Builder()
                .url(server.url("/parse"))
                .post("url=abc".toRequestBody())
                .build()
        ).execute().use { response ->
            assertEquals(500, response.code)
        }
        assertEquals(1, server.requestCount)
    }
}
