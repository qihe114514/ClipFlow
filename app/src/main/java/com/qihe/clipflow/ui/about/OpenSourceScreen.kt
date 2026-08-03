package com.qihe.clipflow.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qihe.clipflow.ui.components.GlassCard

@Composable
fun OpenSourceScreen() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OpenSourceSection(
            icon = Icons.Filled.Gavel,
            title = "开源协议",
            body = "ClipFlow 以 GNU General Public License v3.0（GPLv3）发布。你可以使用、研究、修改和再分发本项目，但分发修改版本时请遵守 GPLv3 的源代码提供和许可证保留要求。完整协议见项目根目录的 LICENSE 文件。"
        )
        OpenSourceSection(
            icon = Icons.Filled.LibraryBooks,
            title = "第三方库",
            body = "本应用使用 Jetpack Compose、Material 3、AndroidX、Navigation Compose、Room、DataStore、Media3、Retrofit、OkHttp、Gson、Coil、Miuix、Accompanist 和友盟 SDK 等开源或第三方组件。各组件按照其项目声明的许可证和使用条款提供，具体版权归原作者所有。"
        )
        OpenSourceSection(
            icon = Icons.Filled.AccountCircle,
            title = "账号与隐私",
            body = "B 站解析需要用户主动登录。登录 Cookie 只保存在本机的 Keystore 加密会话中，用于向 B 站接口请求当前账号有权访问的视频资源，不会写入普通配置、历史记录或日志。退出登录或会话失效后，应用会清除本地会话和 WebView Cookie。"
        )
        OpenSourceSection(
            icon = Icons.Filled.Security,
            title = "平台使用范围",
            body = "ClipFlow 只请求抖音、小红书和哔哩哔哩等平台公开或账号有权访问的资源，不绕过付费、地区、会员或其他访问限制。解析结果和下载地址受平台权限、资源有效期及网络状况影响。"
        )
        OpenSourceSection(
            icon = Icons.Filled.WarningAmber,
            title = "使用须知",
            body = "请只下载你拥有保存和使用权的内容，并遵守所在地区法律、版权规定以及相关平台的服务条款。ClipFlow 不对用户下载、保存或再分发的内容承担责任。"
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    text = "项目仓库",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(10.dp))
                ExternalLinkRow(
                    icon = Icons.Filled.Code,
                    label = "GitHub · qihe114514/ClipFlow",
                    url = "https://github.com/qihe114514/ClipFlow",
                    context = context,
                )
            }
        }
    }
}

@Composable
private fun OpenSourceSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 12.dp),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}