package com.qihe.clipflow.ui.component

import org.junit.Assert.assertEquals
import org.junit.Test

class GlassStyleTest {
    @Test
    fun cardUsesMediumExtraBlurByDefault() {
        assertEquals(8f, GlassStyle().card.extraBlur, 0f)
    }
}
