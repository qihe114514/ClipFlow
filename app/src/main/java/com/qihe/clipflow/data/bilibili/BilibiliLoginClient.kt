package com.qihe.clipflow.data.bilibili

import android.util.Log
import androidx.annotation.Keep
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit

class BilibiliLoginClient(
    private val callFactory: Call.Factory = defaultCallFactory,
    private val gson: Gson = Gson()
) {
    private val cookieLock = Any()
    private val cookieHeaders = mutableListOf<String>()

    suspend fun generateQrCode(): Result<BilibiliQrCode> {
        val result = withContext(Dispatchers.IO) {
            var lastFailure: Throwable = IllegalStateException("Bilibili QR generation failed")
            repeat(qrGenerationAttempts) { attempt ->
                val current = runCatching {
                    val data = execute(
                        Request.Builder()
                            .url("$passportBaseUrl/x/passport-login/web/qrcode/generate?source=main_web")
                            .get()
                            .build(),
                        BilibiliQrGenerateData::class.java
                    )
                    BilibiliQrCode(
                        url = requireNotNull(data.url).takeIf(String::isNotBlank)
                            ?: error("Bilibili QR URL is missing"),
                        key = requireNotNull(data.qrCodeKey).takeIf(String::isNotBlank)
                            ?: error("Bilibili QR key is missing")
                    )
                }
                current.exceptionOrNull()?.let { lastFailure = it }
                if (current.isSuccess) return@withContext current
                if (attempt + 1 < qrGenerationAttempts) delay(qrGenerationRetryDelayMs)
            }
            Result.failure(lastFailure)
        }
        return result.onFailure { error ->
            Log.e("BilibiliLogin", "QR generation failed: ${error.javaClass.simpleName}: ${error.message}")
        }
    }

    suspend fun pollQrCode(key: String): Result<BilibiliQrPollResult> = withContext(Dispatchers.IO) {
        runCatching {
            require(key.isNotBlank())
            val url = "$passportBaseUrl/x/passport-login/web/qrcode/poll".toHttpUrl()
                .newBuilder()
                .addQueryParameter("qrcode_key", key)
                .addQueryParameter("source", "main_web")
                .build()
            val data = execute(
                Request.Builder().url(url).get().build(),
                BilibiliQrPollData::class.java
            )
            val status = mapBilibiliQrCode(data.code, data.url, data.refreshToken)
            val callbackCookies = (status as? BilibiliQrPollStatus.Success)
                ?.let { extractBilibiliCallbackCookies(it.callbackUrl) }
                .orEmpty()
            BilibiliQrPollResult(
                status = status,
                cookie = mergeBilibiliCookieHeaders(listOf(callbackCookies, cookieHeader()))
            )
        }
    }

    suspend fun requestCaptcha(): Result<BilibiliCaptchaChallenge> = withContext(Dispatchers.IO) {
        runCatching {
            val data = execute(
                Request.Builder()
                    .url("$passportBaseUrl/x/passport-login/captcha?source=main_web")
                    .get()
                    .build(),
                BilibiliCaptchaData::class.java
            )
            val geetest = requireNotNull(data.geetest)
            BilibiliCaptchaChallenge(
                token = requireNotNull(data.token).takeIf(String::isNotBlank)
                    ?: error("Bilibili captcha token is missing"),
                challenge = requireNotNull(geetest.challenge).takeIf(String::isNotBlank)
                    ?: error("Bilibili captcha challenge is missing"),
                gt = requireNotNull(geetest.gt).takeIf(String::isNotBlank)
                    ?: error("Bilibili captcha id is missing")
            )
        }
    }

    suspend fun sendSms(phone: String, challenge: BilibiliCaptchaResult): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(isBilibiliPhoneValid(phone))
            val body = FormBody.Builder()
                .add("cid", "86")
                .add("tel", phone.trim())
                .add("source", "main_web")
                .add("token", challenge.token)
                .add("challenge", challenge.challenge)
                .add("validate", challenge.validate)
                .add("seccode", challenge.seccode)
                .build()
            val data = execute(
                Request.Builder()
                    .url("$passportBaseUrl/x/passport-login/web/sms/send")
                    .post(body)
                    .build(),
                BilibiliSmsSendData::class.java
            )
            requireNotNull(data.captchaKey).takeIf(String::isNotBlank)
                ?: error("Bilibili captcha key is missing")
        }
    }

    suspend fun loginBySms(phone: String, code: String, captchaKey: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(isBilibiliPhoneValid(phone) && isBilibiliSmsCodeValid(code) && captchaKey.isNotBlank())
            val body = FormBody.Builder()
                .add("cid", "86")
                .add("tel", phone.trim())
                .add("code", code)
                .add("source", "main_mini")
                .add("keep", "0")
                .add("captcha_key", captchaKey)
                .add("go_url", "https://www.bilibili.com")
                .build()
            val data = execute(
                Request.Builder()
                    .url("$passportBaseUrl/x/passport-login/web/login/sms")
                    .post(body)
                    .build(),
                BilibiliSmsLoginData::class.java
            )
            check(data.status == 0) { data.message ?: "Bilibili SMS login failed" }
            cookieHeader().takeIf(String::isNotBlank) ?: error("Bilibili login Cookie is missing")
        }
    }

    private fun cookieHeader(): String = synchronized(cookieLock) {
        mergeBilibiliCookieHeaders(cookieHeaders)
    }

    private fun <T> execute(request: Request, dataClass: Class<T>): T {
        val requestWithHeaders = request.newBuilder()
            .header("User-Agent", desktopUserAgent)
            .header("Referer", loginReferer)
            .apply { cookieHeader().takeIf(String::isNotBlank)?.let { header("Cookie", it) } }
            .build()
        callFactory.newCall(requestWithHeaders).execute().use { response ->
            collectCookies(response)
            check(response.isSuccessful) { "Bilibili login request failed" }
            val body = response.body?.string().orEmpty()
            check(body.isNotBlank()) { "Bilibili login response is empty" }
            val type = TypeToken.getParameterized(BilibiliResponse::class.java, dataClass).type
            val envelope: BilibiliResponse<T> = gson.fromJson(body, type)
            check(envelope.code == 0) { envelope.message ?: "Bilibili login request was rejected" }
            return requireNotNull(envelope.data) { "Bilibili login response has no data" }
        }
    }

    private fun collectCookies(response: Response) {
        val headers = response.headers.values("Set-Cookie")
        if (headers.isEmpty()) return
        synchronized(cookieLock) { cookieHeaders += headers }
    }

    @Keep
    private data class BilibiliQrGenerateData(
        val url: String? = null,
        @SerializedName("qrcode_key") val qrCodeKey: String? = null
    )

    @Keep
    private data class BilibiliQrPollData(
        val url: String? = null,
        @SerializedName("refresh_token") val refreshToken: String? = null,
        val code: Int = 86101
    )

    @Keep
    private data class BilibiliGeetestData(
        val challenge: String? = null,
        val gt: String? = null
    )

    @Keep
    private data class BilibiliCaptchaData(
        val token: String? = null,
        val geetest: BilibiliGeetestData? = null
    )

    @Keep
    private data class BilibiliSmsSendData(
        @SerializedName("captcha_key") val captchaKey: String? = null
    )

    @Keep
    private data class BilibiliSmsLoginData(
        val status: Int = -1,
        val message: String? = null
    )

    companion object {
        private const val qrGenerationAttempts = 3
        private const val qrGenerationRetryDelayMs = 500L
        private const val passportBaseUrl = "https://passport.bilibili.com"
        private const val desktopUserAgent =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        private const val loginReferer = "https://passport.bilibili.com/login"

        private val defaultCallFactory: Call.Factory by lazy {
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        }
    }
}

internal fun extractBilibiliCallbackCookies(callbackUrl: String): String {
    val url = callbackUrl.toHttpUrlOrNull() ?: return ""
    return bilibiliCallbackCookieNames.mapNotNull { name ->
        url.queryParameter(name)?.takeIf(String::isNotBlank)?.let { "$name=$it" }
    }.joinToString("; ")
}

private val bilibiliCallbackCookieNames = listOf(
    "SESSDATA",
    "bili_jct",
    "DedeUserID",
    "DedeUserID__ckMd5",
    "sid"
)
