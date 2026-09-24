package com.qihe.clipflow.util

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * 对幂等 GET 请求做一次快速重试，缓解弱网与瞬时 5xx。
 * 只接入 API/更新检查客户端；下载客户端有自己的候选与保存流程，不接入以免重复请求体。
 */
object HttpRetry {
    fun interceptor(maxRetries: Int = 1): Interceptor = Interceptor { chain ->
        val request = chain.request()
        val retryable = request.method.equals("GET", ignoreCase = true)
        var attempt = 0
        var response: Response? = null
        while (response == null) {
            try {
                val candidate = chain.proceed(request)
                if (retryable && candidate.code in 500..599 && attempt < maxRetries) {
                    candidate.close()
                } else {
                    response = candidate
                }
            } catch (e: IOException) {
                if (!retryable || attempt >= maxRetries) throw e
            }
            attempt++
        }
        response!!
    }
}
