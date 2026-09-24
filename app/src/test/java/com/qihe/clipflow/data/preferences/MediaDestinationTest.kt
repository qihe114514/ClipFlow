package com.qihe.clipflow.data.preferences

import com.qihe.clipflow.data.api.model.ContentType
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaDestinationTest {

    @Test
    fun videoTypesUseVideoDestination() {
        assertEquals(MediaDestinationKind.VIDEO, destinationKindOf(ContentType.VIDEO))
        assertEquals(MediaDestinationKind.VIDEO, destinationKindOf(ContentType.LIVE_VIDEO))
    }

    @Test
    fun imageTypesUseImageDestination() {
        assertEquals(MediaDestinationKind.IMAGE, destinationKindOf(ContentType.IMAGE))
        assertEquals(MediaDestinationKind.IMAGE, destinationKindOf(ContentType.LIVE_IMAGE))
    }

    @Test
    fun audioUsesAudioDestination() {
        assertEquals(MediaDestinationKind.AUDIO, destinationKindOf(ContentType.AUDIO))
    }
}
