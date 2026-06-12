package com.douyin.downloader.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse(
    val code: Int,
    val msg: String,
    val data: VideoData? = null
)

@Serializable
data class VideoData(
    val type: String = "video",
    val title: String = "",
    val desc: String = "",
    val author: Author? = null,
    val cover: String = "",
    val url: String? = null,
    @SerialName("video_backup")
    val videoBackup: String? = null,
    val images: List<String> = emptyList(),
    @SerialName("live_photo")
    val livePhoto: List<LivePhoto> = emptyList(),
    val music: Music? = null
)

@Serializable
data class Author(
    val name: String = "",
    val id: Long = 0,
    val avatar: String = ""
)

@Serializable
data class LivePhoto(
    val image: String = "",
    val video: String = ""
)

@Serializable
data class Music(
    val title: String = "",
    val author: String = "",
    val url: String = ""
)

/**
 * 获取所有可下载的视频URL
 */
fun VideoData.getAllVideoUrls(): List<DownloadItem> {
    val items = mutableListOf<DownloadItem>()

    when (type) {
        "video" -> {
            url?.let {
                items.add(DownloadItem(
                    url = it,
                    title = title.ifEmpty { "${author?.name ?: "视频"}_${System.currentTimeMillis()}" },
                    type = DownloadType.VIDEO
                ))
            }
            videoBackup?.let {
                items.add(DownloadItem(
                    url = it,
                    title = title.ifEmpty { "${author?.name ?: "视频"}_${System.currentTimeMillis()}" } + "_备份",
                    type = DownloadType.VIDEO
                ))
            }
        }
        "image" -> {
            images.forEachIndexed { index, imgUrl ->
                items.add(DownloadItem(
                    url = imgUrl,
                    title = "${title}_图${index + 1}",
                    type = DownloadType.IMAGE
                ))
            }
        }
        "live" -> {
            livePhoto.forEachIndexed { index, lp ->
                items.add(DownloadItem(
                    url = lp.video,
                    title = "${title}_实况${index + 1}",
                    type = DownloadType.LIVE_PHOTO
                ))
                items.add(DownloadItem(
                    url = lp.image,
                    title = "${title}_实况${index + 1}_封面",
                    type = DownloadType.IMAGE
                ))
            }
        }
    }

    return items
}

data class DownloadItem(
    val url: String,
    val title: String,
    val type: DownloadType
)

enum class DownloadType {
    VIDEO, IMAGE, LIVE_PHOTO
}
