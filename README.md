# ClipFlow

> 抖音、小红书视频和图片下载工具

ClipFlow 是一款 Android 应用。复制抖音或小红书的分享链接，粘贴到 ClipFlow 中，就可以查看并下载视频、图片、音乐和图集内容。

## 下载

- [下载最新版 APK](https://github.com/qihe114514/ClipFlow/releases/latest)
- [查看所有版本](https://github.com/qihe114514/ClipFlow/releases)

当前版本：**v3.0**

最低支持：**Android 12**

## 主要功能

### 抖音 4K 原画视频

ClipFlow 会优先提供抖音返回的原画视频下载地址。对于原本支持 4K 的视频，下载结果可以保留更高的清晰度和码率。

相比抖音电脑端播放器中的“4K”选项，ClipFlow 下载的原画文件通常更清晰，因为电脑端播放的 4K 版本仍可能经过码率压缩。

实际清晰度取决于原视频质量以及抖音返回的资源。解析结果中标有“原画”的选项，就是优先推荐的下载选项；如果原画链接不可用，还可以展开“备用画质”选择其他版本。

### 抖音

- 下载无水印视频。
- 下载视频中的音乐。
- 下载图集中的多张图片。
- 下载实况照片对应的图片和视频。
- 查看作者、标题、封面和作品信息。
- 长按封面，可以将封面保存到相册。

### 小红书

- 下载无水印视频。
- 下载多图笔记中的图片。
- 下载实况图对应的图片和视频。
- 查看笔记正文和作者信息。
- 长按封面，可以将封面保存到相册。
- 备用下载选项会明确标注“有水印”。

### 其他功能

- 自动读取剪贴板中的分享链接。
- 自动保存解析历史，可以搜索和重新打开以前的记录。
- 下载时显示实时进度，即使离开当前页面也可以继续下载。
- 下载完成后发送系统通知，并可直接打开文件。
- 视频、图片和音乐分别保存到不同位置。
- 支持更换应用背景图片、调整透明度或关闭壁纸。
- 可以设置启动时打开的页面和底部按钮顺序。

## 使用方法

### 下载视频或图片

1. 在抖音或小红书中打开想保存的内容。
2. 点击“分享”，选择“复制链接”。
3. 打开 ClipFlow，进入“抖音”或“小红书”页面。
4. 将链接粘贴到输入框中，点击“开始解析”。
5. 等待解析完成，在结果中选择需要的内容。
6. 点击对应选项右侧的“下载”。
7. 下载完成后，可以在系统相册、文件管理器或下载通知中打开文件。

如果应用已经自动填入剪贴板内容，只需要确认链接正确，然后点击“开始解析”即可。

### 选择清晰度

解析完成后，结果会显示可用的下载选项：

- **原画**：优先选择，通常拥有更高的清晰度和码率。
- **备用画质**：原画无法下载时使用，可能包含不同分辨率、编码或格式。
- **有水印**：小红书部分备用链接可能带水印，应用会明确标注。

### 查看下载文件

默认保存位置如下：

| 内容 | 默认位置 |
| --- | --- |
| 视频 | `Movies/ClipFlow` |
| 图片 | `Pictures/ClipFlow` |
| 音乐 | `Music/ClipFlow` |

也可以进入“设置”，分别为视频、图片和音乐选择其他保存位置。

## 常见问题

### 为什么解析失败？

请确认复制的是抖音或小红书的分享链接，而不是普通网页地址。也可以重新复制链接，确认网络正常后再次解析。

如果仍然失败，可能是该内容已删除、设置了权限，或者解析服务暂时不可用。

### 为什么没有 4K 或原画选项？

只有原视频本身提供了较高画质，并且平台返回对应资源时，才会出现原画或 4K 资源。普通清晰度视频无法通过下载工具提升为真正的 4K。

### 下载后在哪里找？

视频默认在 `Movies/ClipFlow`，图片默认在 `Pictures/ClipFlow`，音乐默认在 `Music/ClipFlow`。如果之前在设置中修改过保存位置，请以设置页面显示的位置为准。

### 下载可以放到后台吗？

可以。开始下载后可以切换页面，顶部的下载进度提示会保留当前状态。下载完成后，系统通知会提醒你。

### 安装 APK 时提示“未知来源”怎么办？

这是 Android 对应用商店以外安装包的正常提醒。请在系统设置中允许当前使用的浏览器或文件管理器安装应用，然后返回继续安装。安装完成后，可以关闭这项权限。

## 隐私和网络说明

ClipFlow 需要网络连接来解析分享链接和下载文件。解析服务由 [BugPk-Api](https://api.bugpk.com/) 提供。

应用会在本地保存解析历史、下载位置和界面设置。你可以在应用内的设置中调整相关选项，也可以删除历史记录。

请只下载自己有权保存和使用的内容，并遵守相关平台的服务条款和版权规定。

---

## 开发者信息

以下内容面向需要构建或二次开发 ClipFlow 的开发者，普通用户无需阅读。

### 技术栈

| 类别 | 技术 |
| --- | --- |
| UI | Jetpack Compose、Material 3 |
| 架构 | MVVM、ViewModel、StateFlow |
| 网络 | Retrofit 2、OkHttp 4 |
| JSON | Gson |
| 本地数据 | Room、DataStore |
| 图片加载 | Coil |
| 视频播放 | AndroidX Media3 |
| 导航 | Navigation Compose |
| 语言 | Kotlin |

### 开发环境

- Android Studio Hedgehog（2023.1.1）或更高版本
- JDK 21
- Gradle 9.6
- Android Gradle Plugin 9.1.0
- Kotlin 2.3.10
- compileSdk 37
- minSdk 31（Android 12）

### 构建 APK

```bash
git clone https://github.com/qihe114514/ClipFlow.git
cd ClipFlow
./gradlew assembleRelease
```

Windows 可以运行：

```bat
gradlew.bat assembleRelease
```

生成的 APK 位于：

```text
app/build/outputs/apk/release/app-release.apk
```

### API 接口

ClipFlow 使用 [BugPk-Api](https://api.bugpk.com/) 提供解析服务：

- 抖音：`GET/POST /api/douyin?url=<分享链接>`
- 小红书：`GET /api/xhs?url=<分享链接>`
- [接口文档](https://api.bugpk.com/doc-xhs.html)

### 项目结构

```text
ClipFlow/
├── app/
│   └── src/main/java/com/qihe/clipflow/
│       ├── data/          # 网络接口、本地数据和解析逻辑
│       ├── navigation/    # 页面导航和背景壁纸
│       ├── ui/            # Compose 页面和通用组件
│       └── util/          # 下载和媒体保存工具
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

### 许可证

GNU General Public License v3.0（GPLv3）。详见 [LICENSE](LICENSE)。

部分界面设计参考 [KernelSU-Style-UI-Kit](https://github.com/chenaizhang/KernelSU-Style-UI-Kit)，同样遵循 GPLv3。

### 项目地址

- GitHub：[qihe114514/ClipFlow](https://github.com/qihe114514/ClipFlow)
- B 站：[其核](https://space.bilibili.com/1049283248)
- 抖音：[其核](https://www.douyin.com/user/MS4wLjABAAAAuUtKOArTFKTBm4C6o5MwDQuGMNZ9-0CWZfUay6U9wUI)
