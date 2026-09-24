package com.qihe.clipflow.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * 全局下载会话状态：用于前台服务保活与统一进度通知。
 * 只记录“数量 + 最新进度”，具体每个任务的 UI 状态仍由各自 ViewModel 维护。
 */
object DownloadSessionTracker {

    private val activeIds = ConcurrentHashMap.newKeySet<String>()

    private val _activeCount = MutableStateFlow(0)
    val activeCount: StateFlow<Int> = _activeCount.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _statusText = MutableStateFlow("正在下载")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    fun begin(id: String) {
        activeIds.add(id)
        _activeCount.value = activeIds.size
    }

    fun update(progress: Float, speedText: String) {
        _progress.value = progress.coerceIn(0f, 1f)
        _statusText.value = if (speedText.isBlank()) "正在下载" else "正在下载 $speedText"
    }

    fun finish(id: String) {
        activeIds.remove(id)
        _activeCount.value = activeIds.size
        if (activeIds.isEmpty()) {
            _progress.value = 0f
            _statusText.value = "正在下载"
        }
    }
}
