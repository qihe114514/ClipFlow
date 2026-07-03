package com.qihe.clipflow.ui.components

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** 全局下载药丸状态，跨页面持久 */
object DownloadPillState {
    private val _visible = MutableStateFlow(false)
    val visible: StateFlow<Boolean> = _visible

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    private val _speedText = MutableStateFlow("")
    val speedText: StateFlow<String> = _speedText

    private var _onClick: (() -> Unit)? = null

    fun show(progress: Float, speedText: String, onClick: () -> Unit) {
        _progress.value = progress
        _speedText.value = speedText
        _onClick = onClick
        _visible.value = true
    }

    fun update(progress: Float, speedText: String) {
        _progress.value = progress
        _speedText.value = speedText
    }

    fun hide() {
        _visible.value = false
    }

    fun performClick() {
        _onClick?.invoke()
    }
}
