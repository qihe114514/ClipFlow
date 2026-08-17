package com.qihe.clipflow.navigation

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlin.math.abs

class PrimaryPagerState(
    val pagerState: PagerState,
    private val coroutineScope: CoroutineScope,
) {
    var selectedPage by mutableIntStateOf(pagerState.currentPage)
        private set

    var isNavigating by mutableStateOf(false)
        private set

    private var navigationJob: Job? = null
    private var pendingPage: Int? = null

    fun animateToPage(targetIndex: Int) {
        if (targetIndex == selectedPage && pagerState.currentPage == targetIndex) return

        navigationJob?.cancel()
        pendingPage = null

        selectedPage = targetIndex

        val distance = abs(targetIndex - pagerState.currentPage).coerceAtLeast(2)
        val duration = 100 * distance + 100
        val pageSize = pagerState.layoutInfo.pageSize + pagerState.layoutInfo.pageSpacing
        if (pageSize <= 0) {
            pendingPage = targetIndex
            pagerState.requestScrollToPage(targetIndex)
            isNavigating = false
            return
        }

        isNavigating = true

        navigationJob = coroutineScope.launch {
            val myJob = coroutineContext.job
            try {
                pagerState.animateScrollToPage(
                    page = targetIndex,
                    animationSpec = tween(easing = EaseInOut, durationMillis = duration),
                )
            } finally {
                if (navigationJob == myJob) {
                    isNavigating = false
                    if (pagerState.currentPage != targetIndex) {
                        selectedPage = pagerState.currentPage
                    } else {
                        pendingPage = null
                    }
                }
            }
        }
    }

    fun syncPage() {
        pendingPage?.let { targetPage ->
            if (pagerState.currentPage == targetPage) {
                pendingPage = null
            } else {
                return
            }
        }
        if (!isNavigating && selectedPage != pagerState.currentPage) {
            selectedPage = pagerState.currentPage
        }
    }
}

@Composable
fun rememberPrimaryPagerState(
    pagerState: PagerState,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): PrimaryPagerState {
    return remember(pagerState, coroutineScope) {
        PrimaryPagerState(pagerState, coroutineScope)
    }
}
