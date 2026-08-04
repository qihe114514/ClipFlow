package com.qihe.clipflow.data.repository

import com.qihe.clipflow.data.api.model.ContentItem
import com.qihe.clipflow.data.api.model.ContentType
import com.qihe.clipflow.data.api.model.MediaInfo
import com.qihe.clipflow.data.bilibili.BilibiliAccount
import com.qihe.clipflow.data.bilibili.BilibiliApi
import com.qihe.clipflow.data.bilibili.BilibiliApiClient
import com.qihe.clipflow.data.bilibili.BilibiliDashAudio
import com.qihe.clipflow.data.bilibili.BilibiliDashVideo
import com.qihe.clipflow.data.bilibili.BilibiliNavigation
import com.qihe.clipflow.data.bilibili.BilibiliPlayUrl
import com.qihe.clipflow.data.bilibili.BilibiliSession
import com.qihe.clipflow.data.bilibili.BilibiliSessionRepository
import com.qihe.clipflow.data.bilibili.BilibiliSessionStore
import com.qihe.clipflow.data.bilibili.BilibiliShortUrlResolver
import com.qihe.clipflow.data.bilibili.BilibiliWbiSigner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI

class BilibiliPlatformParser(
    private val api: BilibiliApi = BilibiliApiClient.api,
    private val sessions: BilibiliSessionRepository = BilibiliSessionStore,
    private val shortUrlResolver: BilibiliShortUrlResolver = BilibiliApiClient.shortUrlResolver,
    private val nowSeconds: () -> Long = { System.currentTimeMillis() / 1000 }
) : PlatformParser {
    override val platform = SupportedPlatform.BILIBILI

    override fun supports(rawInput: String): Boolean = BilibiliUrl.extractUrl(rawInput)?.let { url ->
        BilibiliUrl.isVideoHost(url) || BilibiliUrl.isShortHost(url)
    } == true

    override fun normalizeInput(rawInput: String): String {
        val url = BilibiliUrl.extractUrl(rawInput) ?: return rawInput.trim()
        return BilibiliUrl.extractBvid(url)?.let { bvid ->
            val part = BilibiliUrl.partIndex(url)
            BilibiliUrl.canonicalUrl(bvid) + if (part > 1) "?p=$part" else ""
        } ?: url
    }

    override suspend fun parse(normalizedInput: String): Result<ParseResult> = withContext(Dispatchers.IO) {
        if (normalizedInput.isBlank()) return@withContext failure(ParseErrorKind.EMPTY_INPUT)
        if (!supports(normalizedInput)) return@withContext failure(ParseErrorKind.INVALID_INPUT)

        try {
            val resolvedUrl = if (BilibiliUrl.isShortHost(normalizedInput)) {
                shortUrlResolver.resolve(normalizedInput)
            } else {
                normalizedInput
            }
            val bvid = BilibiliUrl.extractBvid(resolvedUrl) ?: return@withContext failure(ParseErrorKind.INVALID_INPUT)
            val session = sessions.session() ?: return@withContext reLogin()
            val navigation = api.navigation()
            if (navigation.isSessionInvalid() || navigation.data?.isLogin != true) return@withContext reLogin()
            if (navigation.code != 0) return@withContext failure(ParseErrorKind.REMOTE_FAILURE, "Bilibili session validation failed")
            val navigationData = navigation.data
            val account = accountFrom(navigationData, api)
            sessions.save(session.copy(account = account))

            val view = api.videoView(bvid)
            if (view.isSessionInvalid()) return@withContext reLogin()
            if (view.code != 0) return@withContext failure(ParseErrorKind.REMOTE_FAILURE, "Bilibili video is unavailable")
            val video = view.data ?: return@withContext failure(ParseErrorKind.REMOTE_FAILURE, "Bilibili video is unavailable")
            val requestedPart = BilibiliUrl.partIndex(resolvedUrl)
            val firstPart = video.pages.orEmpty().filter { it.cid > 0 }.getOrNull(requestedPart - 1)
                ?: return@withContext failure(ParseErrorKind.NO_DOWNLOADABLE_CONTENT)
            val wbi = navigationData.wbiImg
                ?: return@withContext failure(ParseErrorKind.REMOTE_FAILURE, "Bilibili signing data is unavailable")
            val imageUrl = wbi.imageUrl.orEmpty()
            val subUrl = wbi.subUrl.orEmpty()
            if (imageUrl.isBlank() || subUrl.isBlank()) {
                return@withContext failure(ParseErrorKind.REMOTE_FAILURE, "Bilibili signing data is unavailable")
            }
            val signed = BilibiliWbiSigner.sign(
                parameters = mapOf("bvid" to bvid, "cid" to firstPart.cid.toString(), "qn" to "127", "fnval" to "4048"),
                imageUrl = imageUrl,
                subUrl = subUrl,
                timestampSeconds = nowSeconds()
            )
            val playUrl = api.signedPlayUrl(signed.parameters)
            if (playUrl.isSessionInvalid()) return@withContext reLogin()
            if (playUrl.code != 0) return@withContext failure(ParseErrorKind.REMOTE_FAILURE, "Bilibili stream request failed")
            val qualities = BilibiliStreamMapper.qualities(playUrl.data)
            if (qualities.isEmpty()) return@withContext failure(ParseErrorKind.NO_DOWNLOADABLE_CONTENT)
            val officialPreviewUrl = video.aid.takeIf { it > 0 }?.let { aid ->
                runCatching {
                    api.officialPlayUrl(
                        mapOf(
                            "avid" to aid.toString(),
                            "cid" to firstPart.cid.toString(),
                            "qn" to "32",
                            "fnval" to "0",
                            "fnver" to "0",
                            "fourk" to "0",
                            "platform" to "html5"
                        )
                    )
                }.getOrNull()
                    ?.takeIf { it.code == 0 }
                    ?.data
                    ?.let(BilibiliStreamMapper::previewUrl)
            }
            val previewUrl = officialPreviewUrl
                ?: BilibiliStreamMapper.previewUrl(playUrl.data)
                ?: if (playUrl.data?.dash != null) {
                    val previewParameters = BilibiliWbiSigner.sign(
                        parameters = mapOf(
                            "bvid" to bvid,
                            "cid" to firstPart.cid.toString(),
                            "qn" to qualities.first().id.toString(),
                            "fnval" to "0",
                            "fnver" to "0",
                            "fourk" to "1",
                            "platform" to "html5"
                        ),
                        imageUrl = imageUrl,
                        subUrl = subUrl,
                        timestampSeconds = nowSeconds()
                    )
                    runCatching { api.signedPlayUrl(previewParameters.parameters) }
                        .getOrNull()
                        ?.takeIf { it.code == 0 }
                        ?.data
                        ?.let(BilibiliStreamMapper::previewUrl)
                } else null
            val previewQualities = qualities.mapIndexed { index, quality ->
                quality.copy(previewUrl = previewUrl.takeIf { index == 0 })
            }
            val coverUrl = video.pic.orEmpty().httpsUrl()
            val authorAvatar = video.owner?.face.orEmpty().httpsUrl()
            val parts = video.pages.orEmpty().filter { it.cid > 0 }.mapIndexed { index, page ->
                BilibiliPart(page.page.takeIf { it > 0 } ?: index + 1, page.cid, page.part.orEmpty())
            }
            val details = BilibiliVideoDetails(bvid, parts, previewQualities)
            Result.success(
                ParseResult(
                    items = previewQualities.map { quality ->
                        ContentItem(
                            id = "bilibili_${firstPart.cid}_${quality.id}",
                            type = ContentType.VIDEO,
                            url = quality.streamUrl,
                            thumbnailUrl = coverUrl,
                            mediaInfo = MediaInfo(resolution = quality.label, format = "DASH"),
                            description = firstPart.part.orEmpty(),
                            companionUrl = quality.audioUrl,
                            previewUrl = quality.previewUrl
                        )
                    },
                    title = video.title.orEmpty(),
                    desc = video.desc.orEmpty(),
                    cover = coverUrl,
                    authorName = video.owner?.name.orEmpty(),
                    authorAvatar = authorAvatar,
                    contentType = "bilibili",
                    shareUrl = BilibiliUrl.canonicalUrl(bvid),
                    bilibili = details
                )
            )
        } catch (_: SessionInvalidException) {
            reLogin()
        } catch (_: Exception) {
            failure(ParseErrorKind.REMOTE_FAILURE, "Bilibili parsing failed")
        }
    }

    private suspend fun accountFrom(navigation: BilibiliNavigation, api: BilibiliApi): BilibiliAccount {
        val relation = api.relationCounts(navigation.mid)
        if (relation.isSessionInvalid()) throw SessionInvalidException
        return BilibiliAccount(
            mid = navigation.mid,
            name = navigation.uname.orEmpty(),
            avatar = navigation.face.orEmpty(),
            following = relation.data?.following ?: 0,
            followers = relation.data?.follower ?: 0,
            coins = navigation.money
        )
    }

    private fun <T> com.qihe.clipflow.data.bilibili.BilibiliResponse<T>.isSessionInvalid(): Boolean = code == -101

    private fun failure(kind: ParseErrorKind, detail: String? = null): Result<ParseResult> =
        Result.failure(ParseException(ParseFailure(kind, detail)))

    private fun reLogin(): Result<ParseResult> {
        sessions.clear()
        return failure(ParseErrorKind.REMOTE_FAILURE, "Bilibili session expired. Please log in again.")
    }

    private data object SessionInvalidException : Exception()
}

private fun String.httpsUrl(): String = replace("http://", "https://")

object BilibiliUrl {
    private val urlPattern = Regex("""https?://[^\s]+""", RegexOption.IGNORE_CASE)
    private val bvidPattern = Regex("""(?i)(?:^|/)BV([0-9A-Za-z]{10})(?:[/?#]|$)""")

    fun extractUrl(rawInput: String): String? = urlPattern.find(rawInput)?.value?.trimEnd('.', ',', ';', ')', ']', '}')

    fun extractBvid(url: String): String? = bvidPattern.find(url)?.let { "BV${it.groupValues[1]}" }

    fun canonicalUrl(bvid: String): String = "https://www.bilibili.com/video/$bvid"

    fun partIndex(url: String): Int = Regex("[?&]p=(\\d+)").find(url)?.groupValues?.getOrNull(1)?.toIntOrNull()?.coerceAtLeast(1) ?: 1

    fun isVideoHost(url: String): Boolean = host(url)?.let { it == "bilibili.com" || it.endsWith(".bilibili.com") } == true

    fun isShortHost(url: String): Boolean = host(url)?.let { it == "b23.tv" || it.endsWith(".b23.tv") } == true

    private fun host(url: String): String? = runCatching { URI(url).host?.lowercase() }.getOrNull()
}

object BilibiliStreamMapper {
    fun previewUrl(playUrl: BilibiliPlayUrl?): String? = playUrl?.durl.orEmpty()
        .asSequence()
        .mapNotNull { it.url }
        .firstOrNull { it.isNotBlank() }

    fun qualities(playUrl: BilibiliPlayUrl?): List<BilibiliQuality> {
        if (playUrl == null) return emptyList()
        val descriptions = playUrl.accept_quality.orEmpty().zip(playUrl.accept_description.orEmpty()).toMap()
        val audio = playUrl.dash?.audio.orEmpty()
            .filter { it.codecs.isNullOrBlank() || it.codecs.orEmpty().lowercase().contains("mp4a") }
            .maxByOrNull { it.bandwidth }
        val audioUrls = audio?.urls().orEmpty()
        val audioUrl = audioUrls.firstOrNull()
        val dash = playUrl.dash?.video.orEmpty().mapNotNull { video ->
            if (video.codecs?.contains("avc", ignoreCase = true) == false) return@mapNotNull null
            val urls = video.urls()
            urls.firstOrNull()?.let {
                BilibiliQuality(
                    id = video.id,
                    label = descriptions[video.id] ?: "Q${video.id}",
                    streamUrl = it,
                    audioUrl = audioUrl,
                    streamUrls = urls,
                    audioUrls = audioUrls
                )
            }
        }
        if (dash.isNotEmpty()) return dash.distinctBy { it.id }.sortedByDescending { it.id }
        return playUrl.durl.orEmpty().mapIndexedNotNull { index, item ->
            item.url?.takeIf { it.isNotBlank() }?.let { BilibiliQuality(playUrl.quality + index, "Q${playUrl.quality}", it) }
        }
    }

    private fun BilibiliDashVideo.urls(): List<String> = listOfNotNull(baseUrl, legacyBaseUrl)
        .plus(backupUrl.orEmpty())
        .plus(legacyBackupUrl.orEmpty())
        .filter { it.isNotBlank() }
        .distinct()

    private fun BilibiliDashAudio.urls(): List<String> = listOfNotNull(baseUrl, legacyBaseUrl)
        .plus(backupUrl.orEmpty())
        .plus(legacyBackupUrl.orEmpty())
        .filter { it.isNotBlank() }
        .distinct()
}
