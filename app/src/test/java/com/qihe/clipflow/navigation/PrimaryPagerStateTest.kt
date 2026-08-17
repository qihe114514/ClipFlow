package com.qihe.clipflow.navigation

import androidx.compose.foundation.pager.PagerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Test

class PrimaryPagerStateTest {
    @Test
    fun navigationWaitsForPagerLayoutBeforeChangingPage() = runBlocking {
        val scope = kotlinx.coroutines.CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val pagerState = TestPagerState()
        val state = PrimaryPagerState(pagerState, scope)

        state.animateToPage(2)
        assertFalse(state.isNavigating)
        assertEquals(2, state.selectedPage)
        scope.cancel()
    }

    private class TestPagerState : PagerState(0, 0f) {
        override val pageCount: Int = 4
    }
}
