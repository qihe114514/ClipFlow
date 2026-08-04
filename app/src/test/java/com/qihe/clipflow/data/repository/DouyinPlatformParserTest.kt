package com.qihe.clipflow.data.repository

import com.qihe.clipflow.data.api.ApiService
import com.qihe.clipflow.data.api.DouyinBackupApiService
import com.qihe.clipflow.data.api.model.ContentType
import com.qihe.clipflow.data.api.model.DouyinBackupData
import com.qihe.clipflow.data.api.model.DouyinBackupImageData
import com.qihe.clipflow.data.api.model.DouyinBackupResponse
import com.qihe.clipflow.data.api.model.DouyinBackupVideoData
import com.qihe.clipflow.data.api.model.DouyinData
import com.qihe.clipflow.data.api.model.DouyinResponse
import com.qihe.clipflow.data.api.model.XiaohongshuResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DouyinPlatformParserTest {

    @Test
    fun `primary route keeps get then post fallback`() = runBlocking {
        val primary = FakePrimaryApi(
            getResponse = DouyinResponse(500, "GET failed"),
            postResponse = DouyinResponse(
                200,
                "OK",
                DouyinData(type = "video", url = "https://primary.fixture/video.mp4")
            )
        )
        val backup = FakeBackupApi()

        val result = DouyinPlatformParser(primary, backup)
            .parse("https://v.douyin.com/fixture")

        assertEquals("https://primary.fixture/video.mp4", result.getOrThrow().items.single().url)
        assertEquals(1, primary.getCalls)
        assertEquals(1, primary.postCalls)
        assertEquals(0, backup.calls)
    }

    @Test
    fun `backup route maps high quality video without calling primary`() = runBlocking {
        val primary = FakePrimaryApi()
        val backup = FakeBackupApi(
            DouyinBackupResponse(
                code = 200,
                data = DouyinBackupData(
                    type = "video",
                    desc = "fixture",
                    videoData = DouyinBackupVideoData(
                        noWatermarkHighQualityUrl = "https://backup.fixture/high.mp4",
                        noWatermarkUrl = "https://backup.fixture/normal.mp4"
                    )
                )
            )
        )

        val result = DouyinPlatformParser(primary, backup)
            .parse("https://v.douyin.com/fixture", DouyinParseRoute.BACKUP)

        assertEquals("https://backup.fixture/high.mp4", result.getOrThrow().items.single().url)
        assertEquals(ContentType.VIDEO, result.getOrThrow().items.single().type)
        assertEquals(0, primary.getCalls)
        assertEquals(0, primary.postCalls)
        assertEquals(1, backup.calls)
    }

    @Test
    fun `backup route falls back to normal video url`() = runBlocking {
        val result = DouyinPlatformParser(
            FakePrimaryApi(),
            FakeBackupApi(
                DouyinBackupResponse(
                    code = 200,
                    data = DouyinBackupData(
                        type = "video",
                        videoData = DouyinBackupVideoData(
                            noWatermarkUrl = "https://backup.fixture/normal.mp4"
                        )
                    )
                )
            )
        ).parse("https://v.douyin.com/fixture", DouyinParseRoute.BACKUP)

        assertEquals("https://backup.fixture/normal.mp4", result.getOrThrow().items.single().url)
    }

    @Test
    fun `backup route maps no watermark images`() = runBlocking {
        val result = DouyinPlatformParser(
            FakePrimaryApi(),
            FakeBackupApi(
                DouyinBackupResponse(
                    code = 200,
                    data = DouyinBackupData(
                        type = "image",
                        imageData = DouyinBackupImageData(
                            noWatermarkImages = listOf("https://backup.fixture/1.jpg", "")
                        )
                    )
                )
            )
        ).parse("https://v.douyin.com/fixture", DouyinParseRoute.BACKUP)

        assertEquals(1, result.getOrThrow().items.size)
        assertEquals(ContentType.IMAGE, result.getOrThrow().items.single().type)
        assertEquals("https://backup.fixture/1.jpg", result.getOrThrow().items.single().url)
    }

    @Test
    fun `backup route extracts url from a Douyin share text`() = runBlocking {
        val backup = FakeBackupApi(
            DouyinBackupResponse(
                code = 200,
                data = DouyinBackupData(
                    type = "video",
                    videoData = DouyinBackupVideoData(
                        noWatermarkUrl = "https://backup.fixture/normal.mp4"
                    )
                )
            )
        )
        val shareText = "1.23 D@u.SL :0pm uSL:/ 08/09 title https://v.douyin.com/v_KQfHFRvRk/ copy this link"

        val result = DouyinPlatformParser(FakePrimaryApi(), backup)
            .parse(shareText, DouyinParseRoute.BACKUP)

        assertEquals("https://v.douyin.com/v_KQfHFRvRk/", backup.lastUrl)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `backup route reports remote failure`() = runBlocking {
        val result = DouyinPlatformParser(
            FakePrimaryApi(),
            FakeBackupApi(DouyinBackupResponse(code = 400, message = "service unavailable"))
        ).parse("https://v.douyin.com/fixture", DouyinParseRoute.BACKUP)

        val failure = result.exceptionOrNull() as ParseException
        assertEquals(ParseErrorKind.REMOTE_FAILURE, failure.failure.kind)
        assertEquals("service unavailable", failure.failure.detail)
    }

    @Test
    fun `backup route reports missing downloadable content`() = runBlocking {
        val result = DouyinPlatformParser(
            FakePrimaryApi(),
            FakeBackupApi(
                DouyinBackupResponse(
                    code = 200,
                    data = DouyinBackupData(type = "video")
                )
            )
        ).parse("https://v.douyin.com/fixture", DouyinParseRoute.BACKUP)

        val failure = result.exceptionOrNull() as ParseException
        assertEquals(ParseErrorKind.NO_DOWNLOADABLE_CONTENT, failure.failure.kind)
    }

    private class FakePrimaryApi(
        private val getResponse: DouyinResponse = DouyinResponse(500, "GET failed"),
        private val postResponse: DouyinResponse = DouyinResponse(500, "POST failed")
    ) : ApiService {
        var getCalls = 0
        var postCalls = 0

        override suspend fun parseDouyinGet(url: String): DouyinResponse {
            getCalls++
            return getResponse
        }

        override suspend fun parseDouyinPost(url: String): DouyinResponse {
            postCalls++
            return postResponse
        }

        override suspend fun parseXiaohongshu(url: String): XiaohongshuResponse {
            assertTrue("Xiaohongshu should not be called", false)
            return XiaohongshuResponse(500, "unexpected")
        }
    }

    private class FakeBackupApi(
        private val response: DouyinBackupResponse = DouyinBackupResponse(code = 500)
    ) : DouyinBackupApiService {
        var calls = 0
        var lastUrl: String? = null

        override suspend fun parseVideo(url: String, minimal: Boolean): DouyinBackupResponse {
            calls++
            lastUrl = url
            assertTrue("Route 2 must request minimal data", minimal)
            return response
        }
    }
}
