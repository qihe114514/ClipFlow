package com.qihe.clipflow.data.repository

import com.qihe.clipflow.data.api.ApiService
import com.qihe.clipflow.data.api.model.ContentItem
import com.qihe.clipflow.data.api.model.ContentType
import com.qihe.clipflow.data.api.model.MediaInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class XiaohongshuPlatformParser(
    private val api: ApiService
) : PlatformParser {

    private val urlRegex = Regex("""https?://(xhslink\.com|xiaohongshu\.com)\S*""")

    override val platform: SupportedPlatform = SupportedPlatform.XIAOHONGSHU

    override fun supports(rawInput: String): Boolean {
        val normalized = normalizeInput(rawInput)
        return normalized.contains("xhslink.com") || normalized.contains("xiaohongshu.com")
    }

    override fun normalizeInput(rawInput: String): String {
        return urlRegex.find(rawInput)?.value ?: rawInput.trim()
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
                val response = api.parseXiaohongshu(normalizedInput)
                if ((response.code != 200 && response.code != 0) || response.data == null || response.data.isJsonNull) {
                    val detail = response.msg.takeIf { it.isNotBlank() } ?: "code=${response.code}"
                    return@withContext Result.failure(
                        ParseException(ParseFailure(ParseErrorKind.REMOTE_FAILURE, detail))
                    )
                }

                val element = response.data
                val jsonObj = when {
                    element.isJsonObject -> element.asJsonObject
                    element.isJsonArray && element.asJsonArray.size() > 0 -> element.asJsonArray[0].asJsonObject
                    else -> {
                        val hint = response.msg.takeIf { it.isNotBlank() }
                        return@withContext Result.failure(
                            ParseException(ParseFailure(ParseErrorKind.REMOTE_FAILURE, hint))
                        )
                    }
                }

                val type = jsonObj.get("type")?.asString.orEmpty()
                val title = jsonObj.get("title")?.asString.orEmpty()
                val desc = jsonObj.get("desc")?.asString.orEmpty()
                val cover = jsonObj.get("cover")?.asString.orEmpty()

                val items = buildList {
                    when (type) {
                        "video" -> {
                            jsonObj.get("url")?.asString
                                ?.takeIf { it.isNotBlank() }
                                ?.let { videoUrl ->
                                    add(
                                        ContentItem(
                                            id = "xhs_video_0",
                                            type = ContentType.VIDEO,
                                            url = videoUrl,
                                            thumbnailUrl = cover,
                                            mediaInfo = MediaInfo(format = "MP4"),
                                            description = title
                                        )
                                    )
                                }

                            val backup = jsonObj.get("video_backup")
                            when {
                                backup == null || backup.isJsonNull -> Unit
                                backup.isJsonArray -> {
                                    backup.asJsonArray.forEachIndexed { index, entry ->
                                        val url = entry?.asString.orEmpty()
                                        if (url.isNotBlank()) {
                                            add(
                                                ContentItem(
                                                    id = "xhs_backup_$index",
                                                    type = ContentType.VIDEO,
                                                    url = url,
                                                    thumbnailUrl = cover,
                                                    mediaInfo = MediaInfo(format = "MP4"),
                                                    description = "备用下载[有水印] ${index + 1}"
                                                )
                                            )
                                        }
                                    }
                                }

                                backup.isJsonPrimitive -> {
                                    val url = backup.asString.orEmpty()
                                    if (url.isNotBlank()) {
                                        add(
                                            ContentItem(
                                                id = "xhs_backup_0",
                                                type = ContentType.VIDEO,
                                                url = url,
                                                thumbnailUrl = cover,
                                                mediaInfo = MediaInfo(format = "MP4"),
                                                description = "备用下载[有水印]"
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        "image" -> {
                            jsonObj.get("images")
                                ?.takeIf { it.isJsonArray }
                                ?.asJsonArray
                                ?.forEachIndexed { index, entry ->
                                    val imageUrl = entry?.asString.orEmpty()
                                    if (imageUrl.isNotBlank()) {
                                        add(
                                            ContentItem(
                                                id = "xhs_img_$index",
                                                type = ContentType.IMAGE,
                                                url = imageUrl,
                                                thumbnailUrl = imageUrl,
                                                mediaInfo = MediaInfo(format = "JPEG/WEBP"),
                                                description = "图片 ${index + 1}"
                                            )
                                        )
                                    }
                                }
                        }

                        "live" -> {
                            jsonObj.get("live_photo")
                                ?.takeIf { it.isJsonArray }
                                ?.asJsonArray
                                ?.forEachIndexed { index, entry ->
                                    if (entry != null && entry.isJsonObject) {
                                        val live = entry.asJsonObject
                                        live.get("image")?.asString?.takeIf { it.isNotBlank() }?.let { imageUrl ->
                                            add(
                                                ContentItem(
                                                    id = "xhs_live_img_$index",
                                                    type = ContentType.LIVE_IMAGE,
                                                    url = imageUrl,
                                                    thumbnailUrl = imageUrl,
                                                    mediaInfo = MediaInfo(format = "WEBP/JPEG"),
                                                    description = "实况图 ${index + 1}"
                                                )
                                            )
                                        }
                                        live.get("video")?.asString?.takeIf { it.isNotBlank() }?.let { videoUrl ->
                                            add(
                                                ContentItem(
                                                    id = "xhs_live_vid_$index",
                                                    type = ContentType.LIVE_VIDEO,
                                                    url = videoUrl,
                                                    thumbnailUrl = live.get("image")?.asString,
                                                    mediaInfo = MediaInfo(format = "MP4"),
                                                    description = "实况视频 ${index + 1}"
                                                )
                                            )
                                        }
                                    }
                                }
                        }

                        else -> {
                            val directUrl = jsonObj.get("url")?.asString
                                ?: jsonObj.get("video")?.asString
                                ?: jsonObj.get("video_url")?.asString
                                ?: jsonObj.get("play_url")?.asString
                                ?: ""

                            if (directUrl.isNotBlank()) {
                                val isVideo = directUrl.endsWith(".mp4", ignoreCase = true) ||
                                    directUrl.contains("/video/") ||
                                    directUrl.contains("video")
                                add(
                                    ContentItem(
                                        id = "xhs_0",
                                        type = if (isVideo) ContentType.VIDEO else ContentType.IMAGE,
                                        url = directUrl,
                                        thumbnailUrl = cover,
                                        mediaInfo = MediaInfo(format = if (isVideo) "MP4" else "JPEG/WEBP"),
                                        description = title
                                    )
                                )
                            }

                            val imageArray = jsonObj.get("images") ?: jsonObj.get("photos")
                            imageArray?.takeIf { it.isJsonArray }?.asJsonArray?.forEachIndexed { index, entry ->
                                val imageUrl = entry?.asString.orEmpty()
                                if (imageUrl.isNotBlank()) {
                                    add(
                                        ContentItem(
                                            id = "xhs_img_$index",
                                            type = ContentType.IMAGE,
                                            url = imageUrl,
                                            thumbnailUrl = imageUrl,
                                            mediaInfo = MediaInfo(format = "JPEG/WEBP"),
                                            description = "图片 ${index + 1}"
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

                val author = jsonObj.get("author")
                val (authorName, authorAvatar) = when {
                    author == null || author.isJsonNull -> "" to ""
                    author.isJsonObject -> {
                        val authorObj = author.asJsonObject
                        val name = authorObj.get("name")?.asString
                            ?: authorObj.get("nickname")?.asString
                            ?: authorObj.get("nick_name")?.asString
                            ?: ""
                        name to authorObj.get("avatar")?.asString.orEmpty()
                    }

                    else -> author.asString.orEmpty() to ""
                }

                Result.success(
                    ParseResult(
                        items = items,
                        title = title,
                        desc = desc,
                        cover = cover,
                        authorName = authorName,
                        authorAvatar = authorAvatar.ifBlank {
                            jsonObj.get("avatar")?.asString.orEmpty()
                        },
                        contentType = "xiaohongshu"
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
