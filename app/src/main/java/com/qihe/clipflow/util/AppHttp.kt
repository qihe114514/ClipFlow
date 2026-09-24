package com.qihe.clipflow.util

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * 全局共享的 OkHttpClient 基座。
 * 各模块通过 [OkHttpClient.newBuilder] 复用同一份连接池/线程池，避免每个下载器、解析器各建一套。
 */
object AppHttp {
    val shared: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
