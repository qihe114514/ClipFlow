package com.qihe.clipflow.data.api

import com.qihe.clipflow.data.api.model.DouyinResponse
import com.qihe.clipflow.data.api.model.XiaohongshuResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    /**
     * 抖音无水印解析
     * GET/POST 均支持，参数 url = 抖音分享短链接
     */
    @GET("api/douyin")
    suspend fun parseDouyinGet(
        @Query("url") url: String
    ): DouyinResponse

    @FormUrlEncoded
    @POST("api/douyin")
    suspend fun parseDouyinPost(
        @Field("url") url: String
    ): DouyinResponse

    /**
     * 小红书无水印解析
     * GET，参数 url = 小红书分享链接
     */
    @GET("api/xhs")
    suspend fun parseXiaohongshu(
        @Query("url") url: String
    ): XiaohongshuResponse
}
