package com.qihe.clipflow.data.api

import com.qihe.clipflow.data.api.model.DouyinResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface DouyinPrimaryApiService {
    @GET("api/dyjx")
    suspend fun parseVideo(@Query("url") url: String): DouyinResponse
}
