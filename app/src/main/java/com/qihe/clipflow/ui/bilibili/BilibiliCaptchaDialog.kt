package com.qihe.clipflow.ui.bilibili

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.qihe.clipflow.data.bilibili.BilibiliCaptchaChallenge
import com.qihe.clipflow.data.bilibili.BilibiliCaptchaResult
import com.qihe.clipflow.ui.component.LiquidButton
import org.json.JSONObject

@Composable
@SuppressLint("SetJavaScriptEnabled")
fun BilibiliCaptchaDialog(
    challenge: BilibiliCaptchaChallenge,
    onResult: (BilibiliCaptchaResult) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val bridge = remember(challenge) {
        CaptchaBridge(challenge.token, onResult)
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            tonalElevation = 6.dp
        ) {
            androidx.compose.foundation.layout.Column {
                Text(
                    text = "Complete verification",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium
                )
                AndroidView(
                    factory = {
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            settings.cacheMode = WebSettings.LOAD_NO_CACHE
                            addJavascriptInterface(bridge, "ClipFlowCaptcha")
                            loadDataWithBaseURL(
                                "https://static.geetest.com/",
                                captchaHtml(challenge),
                                "text/html",
                                "UTF-8",
                                null
                            )
                        }
                    },
                    update = {},
                    modifier = Modifier.fillMaxWidth().height(340.dp)
                )
                LiquidButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    }
    DisposableEffect(bridge) {
        onDispose { bridge.dispose() }
    }
}

private class CaptchaBridge(
    private val token: String,
    private val onResult: (BilibiliCaptchaResult) -> Unit
) {
    @Volatile
    private var disposed = false

    @JavascriptInterface
    fun onSuccess(payload: String) {
        if (disposed) return
        runCatching {
            val result = JSONObject(payload)
            BilibiliCaptchaResult(
                token = token,
                validate = result.getString("geetest_validate"),
                challenge = result.getString("geetest_challenge"),
                seccode = result.getString("geetest_seccode")
            )
        }.onSuccess { result ->
            Handler(Looper.getMainLooper()).post {
                if (!disposed) onResult(result)
            }
        }
    }

    fun dispose() {
        disposed = true
    }
}

private fun captchaHtml(challenge: BilibiliCaptchaChallenge): String {
    val gt = JSONObject.quote(challenge.gt)
    val challengeValue = JSONObject.quote(challenge.challenge)
    return """
        <!doctype html>
        <html><head><meta name="viewport" content="width=device-width, initial-scale=1">
        <script src="https://static.geetest.com/static/tools/gt.js"></script>
        <style>body{margin:0;padding:24px;font-family:sans-serif;background:#ffffff}#captcha{min-height:80px}</style>
        </head><body><div id="captcha"></div>
        <script>
        initGeetest({gt:$gt,challenge:$challengeValue,offline:false,new_captcha:true,product:'popup',width:'100%'},function(captcha){
            captcha.appendTo('#captcha');
            captcha.onSuccess(function(){ClipFlowCaptcha.onSuccess(JSON.stringify(captcha.getValidate()));});
        });
        </script></body></html>
    """.trimIndent()
}
