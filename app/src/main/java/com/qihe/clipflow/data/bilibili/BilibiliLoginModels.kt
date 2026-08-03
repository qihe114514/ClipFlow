package com.qihe.clipflow.data.bilibili

sealed interface BilibiliQrPollStatus {
    data object Waiting : BilibiliQrPollStatus
    data object Scanned : BilibiliQrPollStatus
    data object Expired : BilibiliQrPollStatus
    data class Success(val callbackUrl: String, val refreshToken: String) : BilibiliQrPollStatus
}

data class BilibiliQrCode(
    val url: String,
    val key: String
)

data class BilibiliQrPollResult(
    val status: BilibiliQrPollStatus,
    val cookie: String = ""
)

data class BilibiliCaptchaChallenge(
    val token: String,
    val challenge: String,
    val gt: String
)

data class BilibiliCaptchaResult(
    val token: String,
    val validate: String,
    val challenge: String,
    val seccode: String
)

sealed interface BilibiliLoginFailure {
    data object InvalidInput : BilibiliLoginFailure
    data object CaptchaRequired : BilibiliLoginFailure
    data object InvalidSmsCode : BilibiliLoginFailure
    data object ExpiredQr : BilibiliLoginFailure
    data object Network : BilibiliLoginFailure
    data object Unauthenticated : BilibiliLoginFailure
}

const val bilibiliQrValiditySeconds = 180

fun isBilibiliPhoneValid(phone: String): Boolean = phone.trim().length >= 6

fun isBilibiliSmsCodeValid(code: String): Boolean = code.length == 6 && code.all(Char::isDigit)

fun mapBilibiliQrCode(code: Int, url: String?, refreshToken: String?): BilibiliQrPollStatus = when (code) {
    0 -> BilibiliQrPollStatus.Success(
        callbackUrl = requireNotNull(url).also { require(it.isNotBlank()) },
        refreshToken = refreshToken.orEmpty()
    )
    86090 -> BilibiliQrPollStatus.Scanned
    86038 -> BilibiliQrPollStatus.Expired
    else -> BilibiliQrPollStatus.Waiting
}
