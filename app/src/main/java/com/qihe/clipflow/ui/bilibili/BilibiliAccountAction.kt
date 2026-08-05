package com.qihe.clipflow.ui.bilibili

import android.webkit.CookieManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.qihe.clipflow.data.bilibili.BilibiliAccount
import com.qihe.clipflow.data.bilibili.BilibiliLoginClient
import com.qihe.clipflow.data.bilibili.BilibiliQrCode
import com.qihe.clipflow.data.bilibili.BilibiliQrPollStatus
import com.qihe.clipflow.data.bilibili.BilibiliSession
import com.qihe.clipflow.data.bilibili.BilibiliSessionStore
import com.qihe.clipflow.data.bilibili.bilibiliQrValiditySeconds
import com.qihe.clipflow.data.bilibili.generateBilibiliQrBitmap
import com.qihe.clipflow.data.bilibili.isBilibiliPhoneValid
import com.qihe.clipflow.data.bilibili.isBilibiliSmsCodeValid
import com.qihe.clipflow.data.bilibili.BilibiliApiClient
import com.qihe.clipflow.ui.component.LiquidButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BilibiliAccountAction() {
    var showLogin by remember { mutableStateOf(false) }
    var showAccount by remember { mutableStateOf(false) }
    val account = BilibiliSessionStore.session()?.account

    LiquidButton(
        onClick = { if (account == null) showLogin = true else showAccount = true },
        modifier = Modifier.size(40.dp),
        contentPadding = PaddingValues(0.dp),
    ) {
        if (account?.avatar.isNullOrBlank()) {
            Icon(Icons.Outlined.AccountCircle, contentDescription = "Bilibili account")
        } else {
            Image(
                painter = rememberAsyncImagePainter(account.avatar),
                contentDescription = "Bilibili avatar",
                modifier = Modifier.size(30.dp).clip(CircleShape)
            )
        }
    }
    if (showLogin) {
        BilibiliLoginDialog(
            onSuccess = { showLogin = false },
            onDismiss = { showLogin = false }
        )
    }
    if (showAccount && account != null) {
        BilibiliAccountDialog(
            account = account,
            onDismiss = { showAccount = false },
            onLogout = {
                BilibiliSessionStore.clear()
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().flush()
                showAccount = false
            }
        )
    }
}

private enum class BilibiliLoginTab {
    Phone,
    Qr
}

@Composable
private fun BilibiliLoginDialog(onSuccess: () -> Unit, onDismiss: () -> Unit) {
    val configuration = LocalConfiguration.current
    val dialogHeight = (configuration.screenHeightDp - 48).coerceAtLeast(520).dp
    val scope = rememberCoroutineScope()
    val client = remember { BilibiliLoginClient() }
    var selectedTab by remember { mutableStateOf(BilibiliLoginTab.Phone) }
    var phone by remember { mutableStateOf("") }
    var smsCode by remember { mutableStateOf("") }
    var captchaKey by remember { mutableStateOf("") }
    var isSendingSms by remember { mutableStateOf(false) }
    var isLoggingIn by remember { mutableStateOf(false) }
    var smsCountdown by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var captchaChallenge by remember { mutableStateOf<com.qihe.clipflow.data.bilibili.BilibiliCaptchaChallenge?>(null) }
    var qrCode by remember { mutableStateOf<BilibiliQrCode?>(null) }
    var qrRemaining by remember { mutableIntStateOf(bilibiliQrValiditySeconds) }
    var qrStatus by remember { mutableStateOf("准备二维码登录") }
    var qrRefreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(smsCountdown) {
        if (smsCountdown > 0) {
            delay(1000)
            smsCountdown -= 1
        }
    }

    LaunchedEffect(selectedTab, qrRefreshKey) {
        if (selectedTab != BilibiliLoginTab.Qr) return@LaunchedEffect
        qrCode = null
        qrRemaining = bilibiliQrValiditySeconds
        qrStatus = "正在获取二维码..."
        errorMessage = null
        val generated = client.generateQrCode()
        generated.fold(
            onSuccess = { code ->
                qrCode = code
                qrStatus = "请使用 B 站手机客户端扫码"
                while (qrRemaining > 0) {
                    delay(1000)
                    qrRemaining -= 1
                    val polled = client.pollQrCode(code.key).getOrElse {
                        errorMessage = "二维码状态获取失败，请检查网络后刷新"
                        return@fold
                    }
                    when (val status = polled.status) {
                        BilibiliQrPollStatus.Waiting -> qrStatus = "等待扫码"
                        BilibiliQrPollStatus.Scanned -> qrStatus = "已扫码，请在 B 站手机客户端确认"
                        BilibiliQrPollStatus.Expired -> {
                            qrStatus = "二维码已过期，请刷新"
                            return@fold
                        }
                        is BilibiliQrPollStatus.Success -> {
                            qrStatus = "正在验证登录状态..."
                            validateAndSave(polled.cookie).fold(
                                onSuccess = { onSuccess(); return@fold },
                                onFailure = { errorMessage = "登录状态验证失败，请刷新二维码重试" }
                            )
                            return@fold
                        }
                    }
                }
                if (qrRemaining == 0) qrStatus = "二维码已过期，请刷新"
            },
            onFailure = {
                qrRemaining = 0
                qrStatus = "二维码获取失败，请刷新"
                errorMessage = "二维码获取失败：${it.message ?: "请检查网络后刷新"}"
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f).height(dialogHeight),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("登录 B 站", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                TabRow(selectedTabIndex = selectedTab.ordinal) {
                    Tab(
                        selected = selectedTab == BilibiliLoginTab.Phone,
                        onClick = {
                            selectedTab = BilibiliLoginTab.Phone
                            errorMessage = null
                        },
                        text = { Text("手机号登录") },
                        icon = { Icon(Icons.Outlined.Sms, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == BilibiliLoginTab.Qr,
                        onClick = {
                            selectedTab = BilibiliLoginTab.Qr
                            errorMessage = null
                            qrRefreshKey += 1
                        },
                        text = { Text("二维码登录") },
                        icon = { Icon(Icons.Outlined.QrCode2, contentDescription = null) }
                    )
                }
                Spacer(Modifier.height(16.dp))
                when (selectedTab) {
                    BilibiliLoginTab.Phone -> PhoneLoginContent(
                        phone = phone,
                        smsCode = smsCode,
                        smsCountdown = smsCountdown,
                        isSendingSms = isSendingSms,
                        isLoggingIn = isLoggingIn,
                        errorMessage = errorMessage,
                        onPhoneChange = { phone = it.filter(Char::isDigit); errorMessage = null },
                        onSmsCodeChange = { smsCode = it.filter(Char::isDigit).take(6); errorMessage = null },
                        onRequestSms = {
                            if (!isBilibiliPhoneValid(phone)) {
                                errorMessage = "请输入有效手机号"
                            } else {
                                isSendingSms = true
                                errorMessage = null
                                scope.launch {
                                    client.requestCaptcha().fold(
                                        onSuccess = { captchaChallenge = it },
                                        onFailure = {
                                            isSendingSms = false
                                            errorMessage = "图形验证获取失败，请稍后重试"
                                        }
                                    )
                                }
                            }
                        },
                        onLogin = {
                            when {
                                !isBilibiliPhoneValid(phone) -> errorMessage = "请输入有效手机号"
                                !isBilibiliSmsCodeValid(smsCode) -> errorMessage = "请输入 6 位短信验证码"
                                captchaKey.isBlank() -> errorMessage = "请先获取短信验证码"
                                else -> {
                                    isLoggingIn = true
                                    errorMessage = null
                                    scope.launch {
                                        client.loginBySms(phone, smsCode, captchaKey).fold(
                                            onSuccess = { cookie ->
                                                validateAndSave(cookie).fold(
                                                    onSuccess = { isLoggingIn = false; onSuccess() },
                                                    onFailure = { isLoggingIn = false; errorMessage = "登录状态验证失败，请检查验证码后重试" }
                                                )
                                            },
                                            onFailure = { isLoggingIn = false; errorMessage = "短信验证码错误或已过期" }
                                        )
                                    }
                                }
                            }
                        }
                    )
                    BilibiliLoginTab.Qr -> QrLoginContent(
                        qrCode = qrCode,
                        remainingSeconds = qrRemaining,
                        status = qrStatus,
                        errorMessage = errorMessage,
                        onRefresh = { qrRefreshKey += 1 }
                    )
                }
                Spacer(Modifier.weight(1f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    LiquidButton(onClick = onDismiss) { Text("取消") }
                }
            }
        }
    }

    captchaChallenge?.let { challenge ->
        BilibiliCaptchaDialog(
            challenge = challenge,
            onResult = { result ->
                captchaChallenge = null
                isSendingSms = true
                scope.launch {
                    client.sendSms(phone, result).fold(
                        onSuccess = {
                            captchaKey = it
                            smsCountdown = 60
                            isSendingSms = false
                            errorMessage = null
                        },
                        onFailure = {
                            isSendingSms = false
                            errorMessage = "短信发送失败，请重新验证"
                        }
                    )
                }
            },
            onDismiss = {
                captchaChallenge = null
                isSendingSms = false
            }
        )
    }
}

@Composable
private fun PhoneLoginContent(
    phone: String,
    smsCode: String,
    smsCountdown: Int,
    isSendingSms: Boolean,
    isLoggingIn: Boolean,
    errorMessage: String?,
    onPhoneChange: (String) -> Unit,
    onSmsCodeChange: (String) -> Unit,
    onRequestSms: () -> Unit,
    onLogin: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("使用手机号短信验证码登录", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("手机号") },
            leadingIcon = { Text("+86") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true
        )
        OutlinedTextField(
            value = smsCode,
            onValueChange = onSmsCodeChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("短信验证码") },
            trailingIcon = {
                LiquidButton(
                    onClick = onRequestSms,
                    enabled = !isSendingSms && smsCountdown == 0,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Text(if (smsCountdown > 0) "重新获取 ${smsCountdown}s" else "获取验证码")
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        LiquidButton(
            onClick = onLogin,
            enabled = !isSendingSms && !isLoggingIn,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoggingIn) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            else Text("登录")
        }
        Text(
            "手机号仅用于 B 站官方验证，登录凭证只保存在本机加密会话中。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QrLoginContent(
    qrCode: BilibiliQrCode?,
    remainingSeconds: Int,
    status: String,
    errorMessage: String?,
    onRefresh: () -> Unit
) {
    val bitmap = remember(qrCode?.url) {
        qrCode?.url?.let { runCatching { generateBilibiliQrBitmap(it, 520) }.getOrNull() }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.QrCode2, contentDescription = null)
            Text("使用 B 站手机客户端扫码登录", modifier = Modifier.weight(1f))
            LiquidButton(onClick = onRefresh, modifier = Modifier.size(40.dp), contentPadding = PaddingValues(0.dp)) {
                Icon(Icons.Outlined.Refresh, contentDescription = "刷新二维码")
            }
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Bilibili login QR code",
                modifier = Modifier.size(220.dp)
            )
        } else {
            CircularProgressIndicator()
        }
        Text("${status} · ${remainingSeconds}s", style = MaterialTheme.typography.bodyMedium)
        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        LiquidButton(onClick = onRefresh) { Text("刷新二维码") }
    }
}

@Composable
private fun BilibiliAccountDialog(
    account: BilibiliAccount,
    onDismiss: () -> Unit,
    onLogout: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = MaterialTheme.shapes.extraLarge) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(account.name, style = MaterialTheme.typography.titleLarge)
                Text("硬币 ${account.coins}")
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text("${account.following} 关注")
                    Text("${account.followers} 粉丝")
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    LiquidButton(onClick = onLogout) { Text("退出登录") }
                }
            }
        }
    }
}

private suspend fun validateAndSave(cookie: String): Result<BilibiliAccount> = withContext(Dispatchers.IO) {
    runCatching {
        require(cookie.isNotBlank() && cookie.contains("SESSDATA="))
        val navigation = BilibiliApiClient.authenticatedApi.navigation(cookie)
        check(navigation.code == 0 && navigation.data?.isLogin == true)
        val data = requireNotNull(navigation.data)
        val relation = runCatching {
            BilibiliApiClient.authenticatedApi.relationCounts(data.mid, cookie)
        }.getOrNull()?.data
        val account = BilibiliAccount(
            mid = data.mid,
            name = data.uname.orEmpty(),
            avatar = data.face.orEmpty().replace("http://", "https://"),
            following = relation?.following ?: 0,
            followers = relation?.follower ?: 0,
            coins = data.money
        )
        BilibiliSessionStore.save(BilibiliSession(cookie, account))
        account
    }
}
