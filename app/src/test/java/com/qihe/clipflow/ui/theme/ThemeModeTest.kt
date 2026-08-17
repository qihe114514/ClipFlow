package com.qihe.clipflow.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {
    @Test
    fun resolvesAllThemeModes() {
        assertTrue(resolveDarkTheme(ThemeMode.SYSTEM, true))
        assertFalse(resolveDarkTheme(ThemeMode.SYSTEM, false))
        assertFalse(resolveDarkTheme(ThemeMode.LIGHT, true))
        assertTrue(resolveDarkTheme(ThemeMode.DARK, false))
    }

    @Test
    fun fallsBackToSystemForUnknownStoredMode() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromKey(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromKey("unknown"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromKey("dark"))
    }

    @Test
    fun dynamicColorRequiresAndroidTwelveOrLater() {
        assertFalse(shouldUseDynamicColor(true, 30))
        assertTrue(shouldUseDynamicColor(true, 31))
        assertFalse(shouldUseDynamicColor(false, 35))
    }
}
