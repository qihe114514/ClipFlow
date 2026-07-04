package com.qihe.clipflow.data.repository

import com.qihe.clipflow.data.api.RetrofitClient
import com.qihe.clipflow.data.api.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


data class ParseResult(
    val items: List<ContentItem>,
    val title: String = "",
    val desc: String = "",
    val cover: String = "",
    val authorName: String = "",
    val authorAvatar: String = "",
    val contentType: String = "",
    val musicUrl: String = "",
    val musicTitle: String = "",
    val shareUrl: String = "",
    val stats: DouyinStatistics? = null,
    val videoBackups: List<VideoBackupItem> = emptyList()
)

class ParseRepository {

    private val api = RetrofitClient.apiService

    /**
     * 解析抖音链接，返回统一 ContentItem 列表
     */
    suspend fun parseDouyin(url: String): Result<ParseResult> {
        return withContext(Dispatchers.IO) {
            try {
                var response = api.parseDouyinGet(url)
                if (response.code != 200) {
                    response = api.parseDouyinPost(url)
                }

                if (response.code != 200 || response.data == null) {
                    return@withContext Result.failure(
                        Exception(response.msg.ifEmpty { "解析失败" })
                    )
                }

                val data = response.data!!
                val items = mutableListOf<ContentItem>()
                var index = 0

                when (data.type) {
                    "video" -> {
                        // 视频：主链接 + 备用链接
                        data.url?.let { videoUrl ->
                            items.add(
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
                        // 音乐
                        data.music?.url?.let { musicUrl ->
                            items.add(
                                ContentItem(
                                    id = "dy_music_$index",
                                    type = ContentType.AUDIO,
                                    url = musicUrl,
                                    thumbnailUrl = data.music?.cover ?: data.cover,
                                    mediaInfo = MediaInfo(format = "MP3"),
                                    description = "[音乐] ${(data.music?.title ?: "").take(20)}"
                                )
                            )
                            index++
                        }
                    }
                    "image" -> {
                        // 图集
                        data.images?.forEachIndexed { i, imageUrl ->
                            items.add(
                                ContentItem(
                                    id = "dy_image_$i",
                                    type = ContentType.IMAGE,
                                    url = imageUrl,
                                    thumbnailUrl = imageUrl,
                                    mediaInfo = MediaInfo(format = "WEBP/JPEG"),
                                    description = "图片 ${i + 1}"
                                )
                            )
                        }
                    }
                    "live" -> {
                        // 实况：image + video 配对
                        data.livePhoto?.forEachIndexed { i, live ->
                            live.image?.let { imgUrl ->
                                items.add(
                                    ContentItem(
                                        id = "dy_live_img_$i",
                                        type = ContentType.LIVE_IMAGE,
                                        url = imgUrl,
                                        thumbnailUrl = imgUrl,
                                        mediaInfo = MediaInfo(format = "WEBP/JPEG"),
                                        description = "实况图 ${i + 1}"
                                    )
                                )
                            }
                            live.video?.let { vidUrl ->
                                items.add(
                                    ContentItem(
                                        id = "dy_live_vid_$i",
                                        type = ContentType.LIVE_VIDEO,
                                        url = vidUrl,
                                        thumbnailUrl = live.image,
                                        mediaInfo = MediaInfo(format = "MP4"),
                                        description = "实况视频 ${i + 1}"
                                    )
                                )
                            }
                        }
                    }
                }

                if (items.isEmpty()) {
                    return@withContext Result.failure(Exception("未找到可下载的内容"))
                }

                Result.success(ParseResult(
                    items = items,
                    title = data.title ?: "",
                    desc = data.desc ?: "",
                    cover = data.cover ?: "",
                    authorName = data.author?.name ?: "",
                    authorAvatar = data.author?.avatar ?: "",
                    contentType = data.type ?: "",
                    musicUrl = data.music?.url ?: "",
                    musicTitle = data.music?.title ?: "",
                    shareUrl = data.extra?.shareUrl ?: "",
                    stats = data.extra?.statistics,
                    videoBackups = data.videoBackup ?: emptyList()
                ))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 解析小红书链接
     */
    suspend fun parseXiaohongshu(url: String): Result<ParseResult> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.parseXiaohongshu(url)

                if ((response.code != 200 && response.code != 0) || response.data == null || response.data.isJsonNull) {
                    val detail = if (response.msg.isNotEmpty()) "(${response.msg})" else "(code=${response.code})"
                    return@withContext Result.failure(
                        Exception("解析失败${detail}，请确认链接是否有效")
                    )
                }

                // data 可能是对象或数组
                val jsonElement = response.data!!
                val jsonObj = when {
                    jsonElement.isJsonObject -> jsonElement.asJsonObject
                    jsonElement.isJsonArray && jsonElement.asJsonArray.size() > 0 ->
                        jsonElement.asJsonArray[0].asJsonObject
                    else -> {
                        val hint = if (response.msg.isNotEmpty()) response.msg else "返回数据为空"
                        return@withContext Result.failure(Exception(hint))
                    }
                }

                // 手动提取字段，兼容 author 为字符串或对象
                val type = jsonObj.get("type")?.asString ?: ""
                val title = jsonObj.get("title")?.asString ?: ""
                val desc = jsonObj.get("desc")?.asString ?: ""
                val cover = jsonObj.get("cover")?.asString ?: ""

                // 根据 type 字段提取媒体URL
                val items = mutableListOf<ContentItem>()
                when (type) {
                    "video" -> {
                        // 无水印视频链接
                        jsonObj.get("url")?.asString?.let { videoUrl ->
                            if (videoUrl.isNotEmpty()) {
                                items.add(ContentItem(
                                    id = "xhs_video_0",
                                    type = ContentType.VIDEO,
                                    url = videoUrl,
                                    thumbnailUrl = cover,
                                    mediaInfo = MediaInfo(format = "MP4"),
                                    description = title
                                ))
                            }
                        }
                        // 备用视频链接（可能是字符串或数组）
                        val backup = jsonObj.get("video_backup")
                        if (backup != null && !backup.isJsonNull) {
                            when {
                                backup.isJsonArray -> {
                                    backup.asJsonArray.forEachIndexed { i, elem ->
                                        elem?.asString?.let { url ->
                                            if (url.isNotEmpty()) {
                                                items.add(ContentItem(
                                                    id = "xhs_backup_$i",
                                                    type = ContentType.VIDEO,
                                                    url = url,
                                                    thumbnailUrl = cover,
                                                    mediaInfo = MediaInfo(format = "MP4"),
                                                    description = "备用下载[有水印] ${i + 1}"
                                                ))
                                            }
                                        }
                                    }
                                }
                                backup.isJsonPrimitive -> {
                                    val url = backup.asString
                                    if (url.isNotEmpty()) {
                                        items.add(ContentItem(
                                            id = "xhs_backup_0",
                                            type = ContentType.VIDEO,
                                            url = url,
                                            thumbnailUrl = cover,
                                            mediaInfo = MediaInfo(format = "MP4"),
                                            description = "备用下载[有水印]"
                                        ))
                                    }
                                }
                            }
                        }
                    }
                    "image" -> {
                        val imagesElement = jsonObj.get("images")
                        if (imagesElement != null && imagesElement.isJsonArray) {
                            imagesElement.asJsonArray.forEachIndexed { i, element ->
                                element?.asString?.let { imageUrl ->
                                    if (imageUrl.isNotEmpty()) {
                                        items.add(ContentItem(
                                            id = "xhs_img_$i",
                                            type = ContentType.IMAGE,
                                            url = imageUrl,
                                            thumbnailUrl = imageUrl,
                                            mediaInfo = MediaInfo(format = "JPEG/WEBP"),
                                            description = "图片 ${i + 1}"
                                        ))
                                    }
                                }
                            }
                        }
                    }
                    "live" -> {
                        val liveElement = jsonObj.get("live_photo")
                        if (liveElement != null && liveElement.isJsonArray) {
                            liveElement.asJsonArray.forEachIndexed { i, elem ->
                                if (elem != null && elem.isJsonObject) {
                                    val obj = elem.asJsonObject
                                    obj.get("image")?.asString?.let { imgUrl ->
                                        items.add(ContentItem(
                                            id = "xhs_live_img_$i",
                                            type = ContentType.LIVE_IMAGE,
                                            url = imgUrl,
                                            thumbnailUrl = imgUrl,
                                            mediaInfo = MediaInfo(format = "WEBP/JPEG"),
                                            description = "实况图 ${i + 1}"
                                        ))
                                    }
                                    obj.get("video")?.asString?.let { vidUrl ->
                                        items.add(ContentItem(
                                            id = "xhs_live_vid_$i",
                                            type = ContentType.LIVE_VIDEO,
                                            url = vidUrl,
                                            thumbnailUrl = obj.get("image")?.asString,
                                            mediaInfo = MediaInfo(format = "MP4"),
                                            description = "实况视频 ${i + 1}"
                                        ))
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        // 无 type 字段时，回退到通用逻辑：url / video / images / photos
                        val url = jsonObj.get("url")?.asString
                            ?: jsonObj.get("video")?.asString
                            ?: jsonObj.get("video_url")?.asString
                            ?: jsonObj.get("play_url")?.asString ?: ""
                        if (url.isNotEmpty()) {
                            val isVideo = url.endsWith(".mp4", ignoreCase = true)
                                    || url.contains("/video/")
                                    || url.contains("video")
                            items.add(ContentItem(
                                id = "xhs_0",
                                type = if (isVideo) ContentType.VIDEO else ContentType.IMAGE,
                                url = url,
                                thumbnailUrl = cover,
                                mediaInfo = MediaInfo(format = if (isVideo) "MP4" else "JPEG/WEBP"),
                                description = title
                            ))
                        }
                        // 尝试 images / photos 数组
                        val imgElem = jsonObj.get("images") ?: jsonObj.get("photos")
                        if (imgElem != null && imgElem.isJsonArray) {
                            imgElem.asJsonArray.forEachIndexed { i, element ->
                                element?.asString?.let { imageUrl ->
                                    if (imageUrl.isNotEmpty()) {
                                        items.add(ContentItem(
                                            id = "xhs_img_$i",
                                            type = ContentType.IMAGE,
                                            url = imageUrl,
                                            thumbnailUrl = imageUrl,
                                            mediaInfo = MediaInfo(format = "JPEG/WEBP"),
                                            description = "图片 ${i + 1}"
                                        ))
                                    }
                                }
                            }
                        }
                    }
                }

                // author 可能是字符串，也可能是包含 name/nickname/avatar 的对象
                val authorElement = jsonObj.get("author")
                val (authorName, authorAvatar) = when {
                    authorElement == null || authorElement.isJsonNull -> "" to ""
                    authorElement.isJsonObject -> {
                        val authorObj = authorElement.asJsonObject
                        val name = authorObj.get("name")?.asString
                            ?: authorObj.get("nickname")?.asString
                            ?: authorObj.get("nick_name")?.asString
                            ?: ""
                        val avatar = authorObj.get("avatar")?.asString ?: ""
                        name to avatar
                    }
                    else -> authorElement.asString to ""
                }
                // 如果 author 对象里没有 avatar，再用根级别的 avatar
                val finalAvatar = if (authorAvatar.isNotEmpty()) authorAvatar
                    else jsonObj.get("avatar")?.asString ?: ""

                if (items.isEmpty()) {
                    return@withContext Result.failure(Exception("未找到可下载的内容，请确认链接是否有效"))
                }

                Result.success(ParseResult(
                    items = items,
                    title = title,
                    desc = desc,
                    cover = cover,
                    authorName = authorName,
                    authorAvatar = finalAvatar,
                    contentType = "xiaohongshu"
                ))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

}
