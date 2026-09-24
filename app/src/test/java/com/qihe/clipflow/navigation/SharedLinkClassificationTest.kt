package com.qihe.clipflow.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SharedLinkClassificationTest {

    @Test
    fun blankOrNullIsNotRecognized() {
        assertNull(classifySharedLink(null))
        assertNull(classifySharedLink("   "))
    }

    @Test
    fun douyinLinksAreRecognized() {
        assertEquals(SharedLinkPlatform.DOUYIN, classifySharedLink("https://v.douyin.com/abc123/"))
        assertEquals(SharedLinkPlatform.DOUYIN, classifySharedLink("看看这个 https://www.iesdouyin.com/share/video/1"))
    }

    @Test
    fun xiaohongshuLinksAreRecognized() {
        assertEquals(SharedLinkPlatform.XIAOHONGSHU, classifySharedLink("https://xhslink.com/a/bcd"))
        assertEquals(SharedLinkPlatform.XIAOHONGSHU, classifySharedLink("https://www.xiaohongshu.com/discovery/item/1"))
    }

    @Test
    fun bilibiliLinksAreRecognized() {
        assertEquals(SharedLinkPlatform.BILIBILI, classifySharedLink("https://b23.tv/abcd"))
        assertEquals(SharedLinkPlatform.BILIBILI, classifySharedLink("https://www.bilibili.com/video/BV1xx411c7mD"))
    }

    @Test
    fun unknownTextIsNotRecognized() {
        assertNull(classifySharedLink("https://example.com/video/1"))
        assertNull(classifySharedLink("这是一段普通文字"))
    }

    @Test
    fun platformOrderPrefersDouyinWhenMultipleKeywordsAppear() {
        assertEquals(
            SharedLinkPlatform.DOUYIN,
            classifySharedLink("douyin.com xiaohongshu.com bilibili.com"),
        )
    }
}
