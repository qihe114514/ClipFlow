package com.qihe.clipflow.ui.components

import com.qihe.clipflow.data.api.model.ContentItem
import com.qihe.clipflow.data.api.model.ContentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaPreviewModelTest {
    @Test
    fun `video and live video are video previews`() {
        assertEquals(PreviewMediaKind.VIDEO, item(ContentType.VIDEO).previewKind)
        assertEquals(PreviewMediaKind.VIDEO, item(ContentType.LIVE_VIDEO).previewKind)
    }

    @Test
    fun `images and live images are image previews`() {
        assertEquals(PreviewMediaKind.IMAGE, item(ContentType.IMAGE).previewKind)
        assertEquals(PreviewMediaKind.IMAGE, item(ContentType.LIVE_IMAGE).previewKind)
    }

    @Test
    fun `audio is excluded from preview`() {
        assertNull(item(ContentType.AUDIO).previewKind)
        assertEquals(
            listOf("video", "image"),
            listOf(
                item(ContentType.VIDEO, "video"),
                item(ContentType.AUDIO, "audio"),
                item(ContentType.IMAGE, "image")
            ).previewableItems().map { it.id }
        )
    }

    private fun item(type: ContentType, id: String = type.name): ContentItem = ContentItem(
        id = id,
        type = type,
        url = "https://example.com/$id"
    )
}
