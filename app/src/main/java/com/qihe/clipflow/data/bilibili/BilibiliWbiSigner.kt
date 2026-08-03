package com.qihe.clipflow.data.bilibili

import java.net.URLEncoder
import java.security.MessageDigest

data class BilibiliSignedParameters(
    val parameters: Map<String, String>
)

object BilibiliWbiSigner {
    private val mixinTable = intArrayOf(
        46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35,
        27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39, 12, 54, 48, 20,
        55, 30, 17, 22, 25, 51, 11, 57, 0, 1, 56, 21, 6, 7, 13, 37,
        40, 4, 41, 59, 16, 38, 24, 52, 44, 36, 34, 26
    )

    fun sign(
        parameters: Map<String, String>,
        imageUrl: String,
        subUrl: String,
        timestampSeconds: Long
    ): BilibiliSignedParameters {
        val mixinKey = mixinKey(imageUrl, subUrl)
        val signingValues = parameters.toMutableMap().apply { put("wts", timestampSeconds.toString()) }
        val query = signingValues.toSortedMap()
            .entries.joinToString("&") { (key, value) -> "$key=${encode(value.filterNot { it in "!'()*" })}" }
        val signed = linkedMapOf<String, String>()
        signingValues.toSortedMap().forEach { (key, value) -> signed[key] = value.filterNot { it in "!'()*" } }
        signed["w_rid"] = md5(query + mixinKey)
        return BilibiliSignedParameters(signed)
    }

    fun mixinKey(imageUrl: String, subUrl: String): String {
        val imageKey = imageUrl.substringAfterLast('/').substringBefore('.')
        val subKey = subUrl.substringAfterLast('/').substringBefore('.')
        val source = imageKey + subKey
        require(source.length >= mixinTable.maxOrNull()!! + 1) { "Invalid WBI keys" }
        return mixinTable.joinToString(separator = "") { source[it].toString() }.take(32)
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    private fun md5(value: String): String = MessageDigest.getInstance("MD5")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
