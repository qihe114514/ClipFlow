package com.qihe.clipflow.data.bilibili

import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

fun interface BilibiliShortUrlResolver {
    suspend fun resolve(url: String): String
}

object BilibiliApiClient {
    private const val browserUserAgent =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124 Safari/537.36"

    private fun isBilibiliHost(host: String): Boolean {
        return host == "bilibili.com" || host.endsWith(".bilibili.com") || host == "b23.tv" || host.endsWith(".b23.tv")
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request()
                if (isBilibiliHost(request.url.host)) {
                    val builder = request.newBuilder().header("User-Agent", browserUserAgent)
                    BilibiliSessionStore.session()?.cookie?.takeIf { it.isNotBlank() }?.let {
                        builder.header("Cookie", it)
                    }
                    chain.proceed(builder.build())
                } else {
                    chain.proceed(request)
                }
            }
            .build()
    }

    val api: BilibiliApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.bilibili.com/")
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().setLenient().create()))
            .build()
            .create(BilibiliApi::class.java)
    }

    val shortUrlResolver = BilibiliShortUrlResolver { url ->
        withContext(Dispatchers.IO) {
            httpClient.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
                response.request.url.toString()
            }
        }
    }
}
