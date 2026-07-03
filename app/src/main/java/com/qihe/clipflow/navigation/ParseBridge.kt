package com.qihe.clipflow.navigation

/**
 * 用于跨页面传递待解析的 URL（从历史记录页跳转到解析页）
 */
object ParseBridge {
    var pendingUrl: String? = null
}
