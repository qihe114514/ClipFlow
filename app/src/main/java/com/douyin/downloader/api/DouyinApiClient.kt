package com.douyin.downloader.api

import com.douyin.downloader.model.ApiResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.*
import java.io.IOException

class DouyinApiClient {

    companion object {
        private const val API_URL = "https://api.bugpk.com/api/douyin"

        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
    }

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    suspend fun parseVideo(shareUrl: String): Result<ApiResponse> = withContext(Dispatchers.IO) {
        try {
            val requestBody = FormBody.Builder()
                .add("url", shareUrl)
                .build()

            val request = Request.Builder()
                .url(API_URL)
                .post(requestBody)
                .addHeader("User-Agent", "DouyinDownloader/1.0 (Android)")
                .addHeader("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()

            val bodyString = response.body?.string() ?: throw IOException("响应体为空")

            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("HTTP ${response.code}: $bodyString"))
            }

            val apiResponse = json.decodeFromString<ApiResponse>(bodyString)

            if (apiResponse.code == 200) {
                Result.success(apiResponse)
            } else {
                Result.failure(IOException(apiResponse.msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
