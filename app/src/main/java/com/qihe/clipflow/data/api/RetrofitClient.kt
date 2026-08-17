package com.qihe.clipflow.data.api

import com.google.gson.GsonBuilder
import com.qihe.clipflow.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = "https://api.bugpk.com/"
    private const val DOUYIN_PRIMARY_BASE_URL = "https://api-new.ifphp.com/"
    private const val BACKUP_BASE_URL = "https://douyin.wtf/"

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
            redactHeader("X-API-Key")
        }

        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    private fun retrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit(BASE_URL).create(ApiService::class.java)
    }

    val douyinPrimaryApiService: DouyinPrimaryApiService by lazy {
        val client = okHttpClient.newBuilder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("X-API-Key", BuildConfig.DOUYIN_API_KEY)
                    .build()
                chain.proceed(request)
            }
            .build()
        Retrofit.Builder()
            .baseUrl(DOUYIN_PRIMARY_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(DouyinPrimaryApiService::class.java)
    }

    val douyinBackupApiService: DouyinBackupApiService by lazy {
        retrofit(BACKUP_BASE_URL).create(DouyinBackupApiService::class.java)
    }
}
