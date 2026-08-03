package com.qihe.clipflow.ui.bilibili

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import com.qihe.clipflow.data.bilibili.BilibiliAccount
import com.qihe.clipflow.data.bilibili.BilibiliApiClient
import com.qihe.clipflow.data.bilibili.BilibiliSession
import com.qihe.clipflow.data.bilibili.BilibiliSessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BilibiliAccountAction() {
    var showLogin by remember { mutableStateOf(false) }
    var confirmLogin by remember { mutableStateOf(false) }
    var showAccount by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf(false) }
    var validating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val account = BilibiliSessionStore.session()?.account
    IconButton(onClick = { if (account == null) showLogin = true else showAccount = true }) {
        if (account?.avatar.isNullOrBlank()) Icon(Icons.Outlined.AccountCircle, "B站账号")
        else Image(rememberAsyncImagePainter(account.avatar), "B站头像", Modifier.size(30.dp).clip(CircleShape))
    }
    if (showLogin) BilibiliLoginDialog(
        onClose = {
            showLogin = false
            confirmLogin = true
        },
        onDismiss = { showLogin = false }
    )
    if (confirmLogin) AlertDialog(
        onDismissRequest = { confirmLogin = false },
        title = { Text("登录完成了吗？") },
        text = { Text("请确认已在 B 站页面完成登录，再读取当前账号 Cookie。") },
        confirmButton = {
            TextButton(
                enabled = !validating,
                onClick = {
                    validating = true
                    val cookie = readBilibiliCookies()
                    scope.launch {
                        val result = withContext(Dispatchers.IO) { validateAndSave(cookie) }
                        validating = false
                        confirmLogin = false
                        if (result.isFailure) loginError = true
                    }
                }
            ) { Text(if (validating) "验证中…" else "完成") }
        },
        dismissButton = { TextButton(onClick = { confirmLogin = false; showLogin = true }) { Text("继续登录") } }
    )
    if (loginError) AlertDialog(
        onDismissRequest = { loginError = false },
        title = { Text("登录验证失败") },
        text = { Text("没有读取到有效的 B 站登录状态，请回到登录页面完成登录后重试。") },
        confirmButton = {
            TextButton(onClick = {
                loginError = false
                showLogin = true
            }) { Text("重新登录") }
        },
        dismissButton = { TextButton(onClick = { loginError = false }) { Text("取消") } }
    )
    if (showAccount && account != null) BilibiliAccountDialog(account, onDismiss = { showAccount = false }, onLogout = {
        BilibiliSessionStore.clear()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        showAccount = false
    })
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun BilibiliLoginDialog(onClose: () -> Unit, onDismiss: () -> Unit) {
    val configuration = LocalConfiguration.current
    val dialogHeight = (configuration.screenHeightDp - 48).coerceAtLeast(520).dp
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .height(dialogHeight),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("登录 B 站", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(12.dp))
                AndroidView(
                    factory = { context -> WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webViewClient = WebViewClient()
                        loadUrl("https://passport.bilibili.com/login")
                    } },
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    TextButton(onClick = onClose) { Text("登录完成") }
                }
            }
        }
    }
}

@Composable
private fun BilibiliAccountDialog(account: BilibiliAccount, onDismiss: () -> Unit, onLogout: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(account.name) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("硬币 ${account.coins}")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("${account.following} 关注")
                Text("${account.followers} 粉丝")
            }
        }
    }, confirmButton = { TextButton(onClick = onLogout) { Text("退出登录") } })
}

private fun readBilibiliCookies(): String {
    return sequenceOf(
        CookieManager.getInstance().getCookie("https://www.bilibili.com"),
        CookieManager.getInstance().getCookie("https://passport.bilibili.com")
    )
        .filterNotNull()
        .flatMap { it.split(';').asSequence() }
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinct()
        .joinToString("; ")
}

private suspend fun validateAndSave(cookie: String): Result<BilibiliAccount> = runCatching {
    require(cookie.isNotBlank() && cookie.contains("SESSDATA="))
    val navigation = BilibiliApiClient.authenticatedApi.navigation(cookie)
    check(navigation.code == 0 && navigation.data?.isLogin == true)
    val data = requireNotNull(navigation.data)
    val relation = runCatching { BilibiliApiClient.authenticatedApi.relationCounts(data.mid, cookie) }.getOrNull()?.data
    val account = BilibiliAccount(
        data.mid,
        data.uname.orEmpty(),
        data.face.orEmpty().replace("http://", "https://"),
        relation?.following ?: 0,
        relation?.follower ?: 0,
        data.money
    )
    BilibiliSessionStore.save(BilibiliSession(cookie, account))
    account
}
