package com.qihe.clipflow.data.bilibili

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class BilibiliResponse<T>(
    val code: Int,
    val message: String? = null,
    val data: T? = null
)

@Keep
data class BilibiliNavigation(
    @SerializedName("isLogin") val isLogin: Boolean = false,
    val mid: Long = 0,
    val uname: String? = null,
    val face: String? = null,
    val money: Double = 0.0,
    @SerializedName("wbi_img") val wbiImg: BilibiliWbiImage? = null
)

@Keep
data class BilibiliWbiImage(
    @SerializedName("img_url") val imageUrl: String? = null,
    @SerializedName("sub_url") val subUrl: String? = null
)

@Keep
data class BilibiliRelationCounts(
    val following: Long = 0,
    val follower: Long = 0
)

@Keep
data class BilibiliVideoView(
    val bvid: String? = null,
    val title: String? = null,
    val desc: String? = null,
    val pic: String? = null,
    val owner: BilibiliOwner? = null,
    val pages: List<BilibiliVideoPage>? = null
)

@Keep
data class BilibiliOwner(
    val mid: Long = 0,
    val name: String? = null,
    val face: String? = null
)

@Keep
data class BilibiliVideoPage(
    val cid: Long = 0,
    val page: Int = 0,
    val part: String? = null
)

@Keep
data class BilibiliPlayUrl(
    val quality: Int = 0,
    val accept_quality: List<Int>? = null,
    val accept_description: List<String>? = null,
    val dash: BilibiliDash? = null,
    val durl: List<BilibiliDurl>? = null
)

@Keep
data class BilibiliDash(
    val video: List<BilibiliDashVideo>? = null,
    val audio: List<BilibiliDashAudio>? = null
)

@Keep
data class BilibiliDashVideo(
    val id: Int = 0,
    @SerializedName("base_url") val baseUrl: String? = null,
    @SerializedName("baseUrl") val legacyBaseUrl: String? = null,
    @SerializedName("backup_url") val backupUrl: List<String>? = null,
    @SerializedName("backupUrl") val legacyBackupUrl: List<String>? = null,
    val bandwidth: Long = 0,
    val codecs: String? = null
)

@Keep
data class BilibiliDashAudio(
    val id: Int = 0,
    @SerializedName("base_url") val baseUrl: String? = null,
    @SerializedName("baseUrl") val legacyBaseUrl: String? = null,
    @SerializedName("backup_url") val backupUrl: List<String>? = null,
    @SerializedName("backupUrl") val legacyBackupUrl: List<String>? = null,
    val bandwidth: Long = 0,
    val codecs: String? = null
)

@Keep
data class BilibiliDurl(
    val url: String? = null
)

data class BilibiliAccount(
    val mid: Long,
    val name: String,
    val avatar: String,
    val following: Long = 0,
    val followers: Long = 0,
    val coins: Double = 0.0
)

data class BilibiliSession(
    val cookie: String,
    val account: BilibiliAccount? = null
)
