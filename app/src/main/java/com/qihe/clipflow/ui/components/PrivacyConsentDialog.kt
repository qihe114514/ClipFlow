package com.qihe.clipflow.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun PrivacyConsentDialog(
    onAgree: (() -> Unit)? = null,
    onDisagree: (() -> Unit)? = null,
    viewOnly: Boolean = false
) {
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
                            append("隐私权政策链接：https://www.umeng.com/page/policy\n\n")

                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("三、你的权利\n")
                            }
                            append("你可以随时在设置中撤回同意。撤回后，我们将停止通过友盟 SDK 收集你的设备信息。\n\n")

                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("四、联系我们\n")
                            }
                            append("如果你对隐私政策有任何疑问，请联系我们。")
                        },
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (viewOnly) {
                    Button(
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
                        OutlinedButton(
                            onClick = { onDisagree?.invoke() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("不同意")
                        }
                        Button(
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
