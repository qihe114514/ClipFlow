package com.qihe.clipflow.data.repository

import com.qihe.clipflow.data.bilibili.BilibiliAccount
import com.qihe.clipflow.data.bilibili.BilibiliApi
import com.qihe.clipflow.data.bilibili.BilibiliDash
import com.qihe.clipflow.data.bilibili.BilibiliDashAudio
import com.qihe.clipflow.data.bilibili.BilibiliDashVideo
import com.qihe.clipflow.data.bilibili.BilibiliNavigation
import com.qihe.clipflow.data.bilibili.BilibiliPlayUrl
import com.qihe.clipflow.data.bilibili.BilibiliRelationCounts
import com.qihe.clipflow.data.bilibili.BilibiliResponse
import com.qihe.clipflow.data.bilibili.BilibiliSession
import com.qihe.clipflow.data.bilibili.BilibiliSessionRepository
import com.qihe.clipflow.data.bilibili.BilibiliVideoPage
import com.qihe.clipflow.data.bilibili.BilibiliVideoView
import com.qihe.clipflow.data.bilibili.BilibiliWbiImage
import com.qihe.clipflow.data.bilibili.BilibiliWbiSigner
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BilibiliPlatformParserTest {
    @Test
    fun recognizesAndNormalizesVideoUrls() {
        val parser = parser()

        assertTrue(parser.supports("watch https://www.bilibili.com/video/BV1xx411c7mD?p=2 now"))
        assertTrue(parser.supports("https://b23.tv/fixture"))
        assertFalse(parser.supports("https://example.com/video/BV1xx411c7mD"))
        assertEquals(
            "https://www.bilibili.com/video/BV1xx411c7mD?p=2",
            parser.normalizeInput("https://www.bilibili.com/video/BV1xx411c7mD?p=2")
        )
        assertEquals("BV1xx411c7mD", BilibiliUrl.extractBvid("https://www.bilibili.com/video/BV1xx411c7mD"))
        assertNull(BilibiliUrl.extractBvid("https://www.bilibili.com/video/av1"))
    }

    @Test
    fun wbiSigningIsDeterministicAndFiltersReservedCharacters() {
        val imageUrl = "https://i0.hdslb.com/bfs/wbi/7cd084941338484aae1ad9425b84077c.png"
        val subUrl = "https://i0.hdslb.com/bfs/wbi/4932caff0ff746eab6f01bf08b70ac45.png"
        val first = BilibiliWbiSigner.sign(mapOf("bvid" to "BV1xx411c7mD", "note" to "a!'()* b"), imageUrl, subUrl, 1_700_000_000)
        val second = BilibiliWbiSigner.sign(mapOf("note" to "a!'()* b", "bvid" to "BV1xx411c7mD"), imageUrl, subUrl, 1_700_000_000)

        assertEquals("ea1db124af3c7062474693fa704f4fbd", BilibiliWbiSigner.mixinKey(imageUrl, subUrl))
        assertEquals(first.parameters["w_rid"], second.parameters["w_rid"])
        assertEquals("a b", first.parameters["note"])
        assertEquals("1700000000", first.parameters["wts"])
    }

    @Test
    fun mapsAccountAndReturnsFixturePartsAndQualities() = runBlocking {
        val session = FakeSessionStore()
        val parser = parser(session)

        val result = parser.parse("https://www.bilibili.com/video/BV1xx411c7mD")

        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()
        assertEquals("Fixture title", parsed.title)
        assertEquals(listOf(80, 64), parsed.bilibili?.qualities?.map { it.id })
        assertEquals(2, parsed.bilibili?.parts?.size)
        assertEquals("https://fixture/audio", parsed.items.first().companionUrl)
        assertEquals("https://fixture/official-preview", parsed.items.first().previewUrl)
        assertEquals(
            mapOf("avid" to "12345", "cid" to "11", "qn" to "32", "fnval" to "0", "fnver" to "0", "fourk" to "0", "platform" to "html5"),
            (parserApi as FixtureApi).officialParameters
        )
        assertEquals("Fixture Account", session.session()?.account?.name)
        assertEquals(12L, session.session()?.account?.following)
        assertEquals(34L, session.session()?.account?.followers)
    }

    @Test
    fun invalidSessionClearsFixtureCookieWithoutLeakingIt() = runBlocking {
        val session = FakeSessionStore()
        val api = FixtureApi(navigationCode = -101)
        val parser = BilibiliPlatformParser(api, session, { "https://www.bilibili.com/video/BV1xx411c7mD" }, { 1 })

        val failure = parser.parse("https://www.bilibili.com/video/BV1xx411c7mD").exceptionOrNull()

        assertNotNull(failure)
        assertNull(session.session())
        assertFalse(failure!!.message.orEmpty().contains("fixture_cookie"))
    }

    @Test
    fun streamQualityFilteringKeepsOnlyUsableFixtureUrls() {
        val qualities = BilibiliStreamMapper.qualities(
            BilibiliPlayUrl(
                accept_quality = listOf(80, 64),
                accept_description = listOf("1080P", "720P"),
                dash = BilibiliDash(
                    listOf(
                        BilibiliDashVideo(80, baseUrl = "https://media.fixture/80"),
                        BilibiliDashVideo(64, baseUrl = ""),
                        BilibiliDashVideo(80, baseUrl = "https://media.fixture/duplicate")
                    )
                )
            )
        )

        assertEquals(1, qualities.size)
        assertEquals(80, qualities.single().id)
        assertEquals("1080P", qualities.single().label)
    }

    @Test
    fun streamQualityKeepsBilibiliBackupUrls() {
        val qualities = BilibiliStreamMapper.qualities(
            BilibiliPlayUrl(
                accept_quality = listOf(80),
                accept_description = listOf("1080P"),
                dash = BilibiliDash(
                    video = listOf(
                        BilibiliDashVideo(
                            id = 80,
                            baseUrl = "https://primary.fixture/video",
                            backupUrl = listOf("https://backup.fixture/video")
                        )
                    ),
                    audio = listOf(
                        BilibiliDashAudio(
                            id = 30280,
                            baseUrl = "https://primary.fixture/audio",
                            backupUrl = listOf("https://backup.fixture/audio"),
                            bandwidth = 1
                        )
                    )
                )
            )
        )

        assertEquals(listOf("https://primary.fixture/video", "https://backup.fixture/video"), qualities.single().streamUrls)
        assertEquals(listOf("https://primary.fixture/audio", "https://backup.fixture/audio"), qualities.single().audioUrls)
    }
    private lateinit var parserApi: BilibiliApi

    private fun parser(session: FakeSessionStore = FakeSessionStore()): BilibiliPlatformParser {
        parserApi = FixtureApi()
        return BilibiliPlatformParser(parserApi, session, { "https://www.bilibili.com/video/BV1xx411c7mD" }, { 1_700_000_000 })
    }

    private class FakeSessionStore : BilibiliSessionRepository {
        private var value: BilibiliSession? = BilibiliSession("fixture_cookie=not_real")
        override fun session(): BilibiliSession? = value
        override fun save(session: BilibiliSession) {
            value = session
        }
        override fun clear() {
            value = null
        }
    }

    private class FixtureApi(private val navigationCode: Int = 0) : BilibiliApi {
        var officialParameters: Map<String, String>? = null
        override suspend fun navigation() = BilibiliResponse(
            navigationCode,
            data = BilibiliNavigation(
                isLogin = navigationCode == 0,
                mid = 1,
                uname = "Fixture Account",
                face = "https://fixture/avatar",
                wbiImg = BilibiliWbiImage(
                    "https://i0.hdslb.com/bfs/wbi/7cd084941338484aae1ad9425b84077c.png",
                    "https://i0.hdslb.com/bfs/wbi/4932caff0ff746eab6f01bf08b70ac45.png"
                )
            )
        )

        override suspend fun relationCounts(mid: Long) = BilibiliResponse(0, data = BilibiliRelationCounts(12, 34))

        override suspend fun videoView(bvid: String) = BilibiliResponse(
            0,
            data = BilibiliVideoView(
                aid = 12345,
                bvid = bvid,
                title = "Fixture title",
                desc = "Fixture description",
                pic = "https://fixture/cover",
                pages = listOf(BilibiliVideoPage(11, 1, "Part one"), BilibiliVideoPage(22, 2, "Part two"))
            )
        )

        override suspend fun signedPlayUrl(parameters: Map<String, String>) = BilibiliResponse(
            0,
            data = BilibiliPlayUrl(
                durl = if (parameters["fnval"] == "0") {
                    listOf(com.qihe.clipflow.data.bilibili.BilibiliDurl("https://fixture/preview"))
                } else null,
                accept_quality = listOf(80, 64),
                accept_description = listOf("1080P", "720P"),
                dash = BilibiliDash(
                    video = listOf(
                        BilibiliDashVideo(80, baseUrl = "https://fixture/80"),
                        BilibiliDashVideo(64, baseUrl = "https://fixture/64")
                    ),
                    audio = listOf(
                        BilibiliDashAudio(30280, baseUrl = "https://fixture/audio")
                    )
                )
            )
        )

        override suspend fun officialPlayUrl(parameters: Map<String, String>): BilibiliResponse<BilibiliPlayUrl> {
            officialParameters = parameters
            return BilibiliResponse(
                0,
                data = BilibiliPlayUrl(
                    durl = listOf(com.qihe.clipflow.data.bilibili.BilibiliDurl("https://fixture/official-preview"))
                )
            )
        }
    }
}
