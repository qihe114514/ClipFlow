package com.qihe.clipflow.data.repository

import com.qihe.clipflow.data.api.ApiService
import com.qihe.clipflow.data.api.model.ContentItem
import com.qihe.clipflow.data.api.model.ContentType
import com.qihe.clipflow.data.api.model.MediaInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DouyinPlatformParser(
    private val api: ApiService
) : PlatformParser {

    override val platform: SupportedPlatform = SupportedPlatform.DOUYIN

    override fun supports(rawInput: String): Boolean {
        val normalized = normalizeInput(rawInput)
        return normalized.contains("douyin.com") || normalized.contains("iesdouyin.com")
    }

    override fun normalizeInput(rawInput: String): String {
        return rawInput.trim()
    }

    override suspend fun parse(normalizedInput: String): Result<ParseResult> {
        if (normalizedInput.isBlank()) {
            return Result.failure(ParseException(ParseFailure(ParseErrorKind.EMPTY_INPUT)))
        }
        if (!supports(normalizedInput)) {
            return Result.failure(ParseException(ParseFailure(ParseErrorKind.INVALID_INPUT)))
        }

        return withContext(Dispatchers.IO) {
            try {
                var response = api.parseDouyinGet(normalizedInput)
                if (response.code != 200) {
                    response = api.parseDouyinPost(normalizedInput)
                }

                val data = response.data
                if (response.code != 200 || data == null) {
                    return@withContext Result.failure(
                        ParseException(
                            ParseFailure(
                                kind = ParseErrorKind.REMOTE_FAILURE,
                                detail = response.msg.takeIf { it.isNotBlank() }
                            )
                        )
                    )
                }

                val items = buildList {
                    var index = 0

                    when (data.type) {
                        "video" -> {
                            data.url?.takeIf { it.isNotBlank() }?.let { videoUrl ->
                                add(
                                    ContentItem(
                                        id = "dy_video_$index",
                                        type = ContentType.VIDEO,
                                        url = videoUrl,
                                        thumbnailUrl = data.cover,
                                        mediaInfo = MediaInfo(format = "MP4"),
                                        description = "[原画] 主链接"
                                    )
                                )
                                index++
                            }

                            data.music?.url?.takeIf { it.isNotBlank() }?.let { musicUrl ->
                                add(
                                    ContentItem(
                                        id = "dy_music_$index",
                                        type = ContentType.AUDIO,
                                        url = musicUrl,
                                        thumbnailUrl = data.music.cover ?: data.cover,
                                        mediaInfo = MediaInfo(format = "MP3"),
                                        description = "[音乐] ${(data.music.title ?: "").take(20)}"
                                    )
                                )
                            }
                        }

                        "image" -> {
                            data.images.orEmpty().forEachIndexed { itemIndex, imageUrl ->
                                if (imageUrl.isNotBlank()) {
                                    add(
                                        ContentItem(
                                            id = "dy_image_$itemIndex",
                                            type = ContentType.IMAGE,
                                            url = imageUrl,
                                            thumbnailUrl = imageUrl,
                                            mediaInfo = MediaInfo(format = "WEBP/JPEG"),
                                            description = "图片 ${itemIndex + 1}"
                                        )
                                    )
                                }
                            }
                        }

                        "live" -> {
                            data.livePhoto.orEmpty().forEachIndexed { itemIndex, live ->
                                live.image?.takeIf { it.isNotBlank() }?.let { imageUrl ->
                                    add(
                                        ContentItem(
                                            id = "dy_live_img_$itemIndex",
                                            type = ContentType.LIVE_IMAGE,
                                            url = imageUrl,
                                            thumbnailUrl = imageUrl,
                                            mediaInfo = MediaInfo(format = "WEBP/JPEG"),
                                            description = "实况图 ${itemIndex + 1}"
                                        )
                                    )
                                }
                                live.video?.takeIf { it.isNotBlank() }?.let { videoUrl ->
                                    add(
                                        ContentItem(
                                            id = "dy_live_vid_$itemIndex",
                                            type = ContentType.LIVE_VIDEO,
                                            url = videoUrl,
                                            thumbnailUrl = live.image,
                                            mediaInfo = MediaInfo(format = "MP4"),
                                            description = "实况视频 ${itemIndex + 1}"
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                if (items.isEmpty()) {
                    return@withContext Result.failure(
                        ParseException(ParseFailure(ParseErrorKind.NO_DOWNLOADABLE_CONTENT))
                    )
                }

                Result.success(
                    ParseResult(
                        items = items,
                        title = data.title.orEmpty(),
                        desc = data.desc.orEmpty(),
                        cover = data.cover.orEmpty(),
                        authorName = data.author?.name.orEmpty(),
                        authorAvatar = data.author?.avatar.orEmpty(),
                        contentType = data.type.orEmpty(),
                        musicUrl = data.music?.url.orEmpty(),
                        musicTitle = data.music?.title.orEmpty(),
                        shareUrl = data.extra?.shareUrl.orEmpty(),
                        stats = data.extra?.statistics,
                        videoBackups = data.videoBackup.orEmpty()
                    )
                )
            } catch (e: Exception) {
                Result.failure(
                    ParseException(ParseFailure(ParseErrorKind.UNEXPECTED, cause = e))
                )
            }
        }
    }
}
