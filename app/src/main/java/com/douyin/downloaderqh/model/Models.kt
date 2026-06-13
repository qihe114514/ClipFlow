package com.douyin.downloaderqh.model

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
    val videoBackup: List<VideoBackupItem> = emptyList(),
    val images: List<String> = emptyList(),
    @SerialName("live_photo")
    val livePhoto: List<LivePhoto> = emptyList(),
    val music: Music? = null
)

@Serializable
data class VideoBackupItem(
    val label: String = "",
    val url: String = ""
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
 * 获取所有可下载的URL（含分辨率/码率/帧率信息）
 */
fun VideoData.getAllVideoUrls(): List<DownloadItem> {
    val items = mutableListOf<DownloadItem>()

    val baseTitle = title.ifEmpty { "${author?.name ?: "视频"}_${System.currentTimeMillis()}" }

    when (type) {
        "video" -> {
            url?.let {
                items.add(DownloadItem(
                    url = it,
                    title = baseTitle,
                    type = DownloadType.VIDEO,
                    displayInfo = buildDisplayInfo(label = null).ifEmpty { "源视频文件" }
                ))
            }
            videoBackup.forEach { backup ->
                items.add(DownloadItem(
                    url = backup.url,
                    title = baseTitle,
                    type = DownloadType.VIDEO,
                    displayInfo = buildDisplayInfo(label = backup.label)
                ))
            }
        }
        "image" -> {
            images.forEachIndexed { index, imgUrl ->
                items.add(DownloadItem(
                    url = imgUrl,
                    title = "${baseTitle}_图${index + 1}",
                    type = DownloadType.IMAGE,
                    displayInfo = "图片"
                ))
            }
        }
        "live" -> {
            livePhoto.forEachIndexed { index, lp ->
                items.add(DownloadItem(
                    url = lp.video,
                    title = "${baseTitle}_实况${index + 1}",
                    type = DownloadType.LIVE_PHOTO,
                    displayInfo = "实况视频"
                ))
                items.add(DownloadItem(
                    url = lp.image,
                    title = "${baseTitle}_实况${index + 1}_封面",
                    type = DownloadType.IMAGE,
                    displayInfo = "实况封面"
                ))
            }
        }
    }

    return items
}

/**
 * 将 API 返回的 label 解析成用户可读的显示字符串。
 * 例如：adapt_lowest_1440_112560pl → 2K · 30fps · 14.4Mbps
 */
private fun buildDisplayInfo(label: String?): String {
    if (label.isNullOrBlank()) return ""

    val parts = mutableListOf<String>()

    // 分辨率提取
    val resPattern = Regex("""(\d{3,4})p""", RegexOption.IGNORE_CASE)
    val resMatch = resPattern.find(label)
    if (resMatch != null) {
        val height = resMatch.groupValues[1].toInt()
        parts.add(when {
            height >= 2160 -> "4K"
            height >= 1440 -> "2K"
            height >= 1080 -> "1080p"
            height >= 720 -> "720p"
            height >= 480 -> "480p"
            else -> "${height}p"
        })
    }

    // 帧率提取
    val fpsPattern = Regex("""(\d{2,3})fps""", RegexOption.IGNORE_CASE)
    val fpsMatch = fpsPattern.find(label)
    if (fpsMatch != null) {
        parts.add("${fpsMatch.groupValues[1]}fps")
    }

    // 码率提取 (kbps)
    val brPattern = Regex("""(\d{3,6})kbps""", RegexOption.IGNORE_CASE)
    val brMatch = brPattern.find(label)
    if (brMatch != null) {
        val kbps = brMatch.groupValues[1].toDouble()
        parts.add("${"%.1f".format(kbps / 1000)}Mbps")
    }

    // 编码格式
    if (label.contains("h265", ignoreCase = true) || label.contains("hevc", ignoreCase = true)) {
        parts.add("HEVC")
    } else if (label.contains("h264", ignoreCase = true) || label.contains("avc", ignoreCase = true)) {
        parts.add("H.264")
    }

    // 如果没有匹配到任何模式，则显示简化 label（去掉前缀）
    if (parts.isEmpty()) {
        val simplified = label
            .replace(Regex("adapt_\\w+_", RegexOption.IGNORE_CASE), "")
            .replace("_", " ")
        return simplified.ifBlank { label }
    }

    return parts.joinToString(" · ")
}

data class DownloadItem(
    val url: String,
    val title: String,
    val type: DownloadType,
    val displayInfo: String = ""
)

enum class DownloadType {
    VIDEO, IMAGE, LIVE_PHOTO
}
