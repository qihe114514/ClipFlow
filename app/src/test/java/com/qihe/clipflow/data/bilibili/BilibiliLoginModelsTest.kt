package com.qihe.clipflow.data.bilibili

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BilibiliLoginModelsTest {
    @Test
    fun qrPollCodesMapToStableStates() {
        assertEquals(BilibiliQrPollStatus.Waiting, mapBilibiliQrCode(86101, null, null))
        assertEquals(BilibiliQrPollStatus.Scanned, mapBilibiliQrCode(86090, null, null))
        assertEquals(BilibiliQrPollStatus.Expired, mapBilibiliQrCode(86038, null, null))
        assertEquals(
            BilibiliQrPollStatus.Success("https://bilibili.com/callback", "refresh"),
            mapBilibiliQrCode(0, "https://bilibili.com/callback", "refresh")
        )
    }

    @Test
    fun loginInputsUseStableGuards() {
        assertTrue(isBilibiliPhoneValid(" 13800138000 "))
        assertFalse(isBilibiliPhoneValid("12345"))
        assertFalse(isBilibiliPhoneValid("1380013800"))
        assertTrue(isBilibiliSmsCodeValid("123456"))
        assertFalse(isBilibiliSmsCodeValid("12345a"))
        assertEquals(180, bilibiliQrValiditySeconds)
    }
}
