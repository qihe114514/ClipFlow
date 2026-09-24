package com.qihe.clipflow.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 系统分享（ACTION_SEND text/plain）到本应用的一次性通道。
 * MainActivity 收到 Intent 后 publish，导航层消费后调用 consume()。
 */
object IncomingShare {
    private val _sharedText = MutableStateFlow<String?>(null)
    val sharedText: StateFlow<String?> = _sharedText

    fun publish(text: String?) {
        _sharedText.value = text?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun consume() {
        _sharedText.value = null
    }
}
