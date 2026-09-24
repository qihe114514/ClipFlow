package com.qihe.clipflow.data.preferences

import com.qihe.clipflow.data.api.model.ContentType
import kotlinx.coroutines.flow.Flow

/** 下载落盘分类，与设置页三行保存路径一一对应。 */
enum class MediaDestinationKind {
    VIDEO,
    IMAGE,
    AUDIO,
}

/** ContentType → 保存路径分类。实况图归图片、实况视频归视频。 */
fun destinationKindOf(type: ContentType): MediaDestinationKind = when (type) {
    ContentType.AUDIO -> MediaDestinationKind.AUDIO
    ContentType.VIDEO, ContentType.LIVE_VIDEO -> MediaDestinationKind.VIDEO
    ContentType.IMAGE, ContentType.LIVE_IMAGE -> MediaDestinationKind.IMAGE
}

/** 读取用户为该媒体分类选择的 SAF 目录（空字符串 = 使用系统默认目录）。 */
fun AppPreferences.destinationFlowOf(kind: MediaDestinationKind): Flow<String> = when (kind) {
    MediaDestinationKind.VIDEO -> videoSavePath
    MediaDestinationKind.IMAGE -> imageSavePath
    MediaDestinationKind.AUDIO -> audioSavePath
}
