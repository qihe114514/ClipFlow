package com.qihe.clipflow.ui.parser

import com.qihe.clipflow.data.api.model.ContentItem
import com.qihe.clipflow.data.api.model.ContentType
import com.qihe.clipflow.data.api.model.MediaInfo
import com.qihe.clipflow.util.DownloadState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ParseResultSessionTest {
    private val item = ContentItem(
        id = "dy_video_0",
        type = ContentType.VIDEO,
        url = "https://example.test/video.mp4",
        thumbnailUrl = "https://example.test/cover.jpg",
        mediaInfo = MediaInfo(resolution = "1080P", format = "MP4"),
        description = "原画"
    )

    @Test
    fun identicalSourceItemsReceiveDifferentIdsForDifferentParseRequests() {
        val first = scopeItemsToParseRequest(1, listOf(item)).single()
        val second = scopeItemsToParseRequest(2, listOf(item)).single()

        assertNotEquals(first.id, second.id)
    }

    @Test
    fun completedStateFromPreviousParseDoesNotMatchNewItem() {
        val first = scopeItemsToParseRequest(1, listOf(item)).single()
        val second = scopeItemsToParseRequest(2, listOf(item)).single()
        val states = mapOf(first.id to DownloadState(isComplete = true))

        assertNull(states[second.id])
    }

    @Test
    fun scopingPreservesDownloadMetadata() {
        val scoped = scopeItemsToParseRequest(1, listOf(item)).single()

        assertEquals(item.type, scoped.type)
        assertEquals(item.url, scoped.url)
        assertEquals(item.thumbnailUrl, scoped.thumbnailUrl)
        assertEquals(item.mediaInfo, scoped.mediaInfo)
        assertEquals(item.description, scoped.description)
    }
}
