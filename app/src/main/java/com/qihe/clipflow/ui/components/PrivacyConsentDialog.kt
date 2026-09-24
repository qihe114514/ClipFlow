package com.qihe.clipflow.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.qihe.clipflow.ui.component.LiquidButton

private const val UMENG_POLICY_URL = "https://www.umeng.com/page/policy"

@Composable
fun PrivacyConsentDialog(
    onAgree: (() -> Unit)? = null,
    onDisagree: (() -> Unit)? = null,
    viewOnly: Boolean = false
) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = { if (viewOnly) onDisagree?.invoke() },
        properties = DialogProperties(
            dismissOnBackPress = viewOnly,
            dismissOnClickOutside = viewOnly
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Text(
                    text = "隐私政策",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = buildAnnotatedString {
                            append("感谢你使用 ClipFlow！我们非常重视你的隐私。请仔细阅读本隐私政策，了解我们如何处理你的信息。\n\n")

                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("一、我们收集的信息\n")
                            }
                            append("本 App 不会主动收集你的个人身份信息。为了进行 App 运营统计与分析，我们接入了第三方 SDK。\n\n")

                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("二、第三方 SDK 说明\n")
                            }
                            append("SDK 名称：友盟移动统计SDK\n")
                            append("使用目的：进行APP数据统计分析\n")
                            append("运营方：友盟同欣（北京）科技有限公司\n")
                            append("收集个人信息类型：设备信息（Android ID/OAID/GUID；可选-IMEI/IMSI/ICCID）、网络信息、位置信息（可选）、应用列表（可选）\n")
                            append("隐私权政策链接：$UMENG_POLICY_URL\n\n")

                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("三、解析与下载使用的第三方服务\n")
                            }
                            append("为完成无水印解析，你主动粘贴或分享的作品链接会发送给第三方解析接口（api.bugpk.com、api-new.ifphp.com、douyin.wtf），下载内容来自对应平台的 CDN。请勿解析包含隐私或敏感信息的链接。\n\n")

                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("四、B 站账号登录\n")
                            }
                            append("登录时输入的手机号/验证码，或扫码凭证，会直接发送给哔哩哔哩官方接口完成验证；登录凭证仅以加密形式保存在本机，不会上传。\n\n")

                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("五、剪贴板与本地数据\n")
                            }
                            append("只有在你点击“粘贴”时才会读取剪贴板文本；解析历史与设置保存在本机，下载文件写入你选择的目录。\n\n")

                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("六、你的权利\n")
                            }
                            append("你可以随时在「关于 → 隐私」中撤回同意。撤回后，我们将停止通过友盟 SDK 收集你的设备信息，并要求你重新确认隐私政策后才能继续使用。\n\n")

                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("七、联系我们\n")
                            }
                            append("如果你对隐私政策有任何疑问，可以通过关于页的 GitHub 仓库联系我们。")
                        },
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(UMENG_POLICY_URL))
                                )
                            }
                        }
                    ) {
                        Text("查看友盟隐私政策", style = MaterialTheme.typography.labelLarge)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (viewOnly) {
                    LiquidButton(
                        onClick = { onDisagree?.invoke() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("关闭")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LiquidButton(
                            onClick = { onDisagree?.invoke() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("不同意")
                        }
                        LiquidButton(
                            onClick = { onAgree?.invoke() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("同意")
                        }
                    }
                }
            }
        }
    }
}
