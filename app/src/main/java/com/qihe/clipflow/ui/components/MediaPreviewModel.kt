package com.qihe.clipflow.ui.components

import com.qihe.clipflow.data.api.model.ContentItem
import com.qihe.clipflow.data.api.model.ContentType

enum class PreviewMediaKind {
    VIDEO,
    IMAGE
}

val ContentItem.previewKind: PreviewMediaKind?
    get() = when (type) {
        ContentType.VIDEO, ContentType.LIVE_VIDEO -> PreviewMediaKind.VIDEO
        ContentType.IMAGE, ContentType.LIVE_IMAGE -> PreviewMediaKind.IMAGE
        ContentType.AUDIO -> null
    }

fun List<ContentItem>.previewableItems(): List<ContentItem> = filter {
    it.url.isNotBlank() && it.previewKind != null
}
