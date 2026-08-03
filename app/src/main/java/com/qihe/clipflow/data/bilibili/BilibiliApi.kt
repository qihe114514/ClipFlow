package com.qihe.clipflow.data.bilibili

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface BilibiliApi {
    @GET("x/web-interface/nav")
    suspend fun navigation(): BilibiliResponse<BilibiliNavigation>

    @GET("x/relation/stat")
    suspend fun relationCounts(@Query("vmid") mid: Long): BilibiliResponse<BilibiliRelationCounts>

    @GET("x/web-interface/view")
    suspend fun videoView(@Query("bvid") bvid: String): BilibiliResponse<BilibiliVideoView>

    @GET("x/player/wbi/playurl")
    suspend fun signedPlayUrl(@QueryMap parameters: Map<String, String>): BilibiliResponse<BilibiliPlayUrl>
}
