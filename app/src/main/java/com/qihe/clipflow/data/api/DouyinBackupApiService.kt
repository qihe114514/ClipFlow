package com.qihe.clipflow.data.api

import com.qihe.clipflow.data.api.model.DouyinBackupResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface DouyinBackupApiService {

    @GET("api/hybrid/video_data")
    suspend fun parseVideo(
        @Query("url") url: String,
        @Query("minimal") minimal: Boolean = true
    ): DouyinBackupResponse
}
