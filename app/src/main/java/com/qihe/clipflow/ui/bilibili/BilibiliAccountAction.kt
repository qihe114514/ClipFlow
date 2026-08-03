package com.qihe.clipflow.ui.bilibili

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.rememberAsyncImagePainter
import com.qihe.clipflow.data.bilibili.BilibiliAccount
import com.qihe.clipflow.data.bilibili.BilibiliApiClient
import com.qihe.clipflow.data.bilibili.BilibiliSession
import com.qihe.clipflow.data.bilibili.BilibiliSessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BilibiliAccountAction() {
    var showLogin by remember { mutableStateOf(false) }
    var confirmLogin by remember { mutableStateOf(false) }
    var showAccount by remember { mutableStateOf(false) }
    val account = BilibiliSessionStore.session()?.account
    IconButton(onClick = { if (account == null) showLogin = true else showAccount = true }) {
        if (account?.avatar.isNullOrBlank()) Icon(Icons.Outlined.AccountCircle, "B站账号")
        else Image(rememberAsyncImagePainter(account!!.avatar), "B站头像", Modifier.size(30.dp).clip(CircleShape))
    }
    if (showLogin) BilibiliLoginDialog(onClose = { confirmLogin = true }, onDismiss = { showLogin = false })
    if (confirmLogin) AlertDialog(
        onDismissRequest = { confirmLogin = false },
        title = { Text("登录完成了吗？") },
        text = { Text("请确认已在 B 站页面完成登录，再读取当前账号 Cookie。") },
        confirmButton = { TextButton(onClick = {
            confirmLogin = false
            val cookie = CookieManager.getInstance().getCookie("https://www.bilibili.com").orEmpty()
            CoroutineScope(Dispatchers.Main).launch {
                val saved = withContext(Dispatchers.IO) { validateAndSave(cookie) }
                if (saved) showLogin = false
            }
        }) { Text("完成") } },
        dismissButton = { TextButton(onClick = { confirmLogin = false; showLogin = true }) { Text("继续登录") } }
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
    AlertDialog(onDismissRequest = onClose, title = { Text("登录 B 站") }, text = {
        AndroidView(factory = { context -> WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            webViewClient = WebViewClient()
            loadUrl("https://passport.bilibili.com/login")
        } }, modifier = Modifier.fillMaxWidth().heightIn(min = 360.dp, max = 520.dp))
    }, confirmButton = { TextButton(onClick = onClose) { Text("登录完成") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
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

private suspend fun validateAndSave(cookie: String): Boolean {
    if (cookie.isBlank() || !cookie.contains("SESSDATA=")) return false
    BilibiliSessionStore.save(BilibiliSession(cookie))
    val navigation = runCatching { BilibiliApiClient.api.navigation() }.getOrNull() ?: return false
    val data = navigation.data ?: return false
    if (navigation.code != 0 || !data.isLogin) return false
    val relation = runCatching { BilibiliApiClient.api.relationCounts(data.mid) }.getOrNull()?.data
    BilibiliSessionStore.save(BilibiliSession(cookie, BilibiliAccount(data.mid, data.uname.orEmpty(), data.face.orEmpty().replace("http://", "https://"), relation?.following ?: 0, relation?.follower ?: 0, data.money)))
    return true
}
