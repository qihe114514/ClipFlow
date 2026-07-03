package com.qihe.clipflow.data.api.model

import com.google.gson.annotations.SerializedName

/** GitHub Release API 响应 */
data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String,
    val name: String,
    val body: String,
    val assets: List<GitHubAsset> = emptyList()
)

data class GitHubAsset(
    val name: String,
    @SerializedName("browser_download_url") val downloadUrl: String,
    val size: Long = 0
)
