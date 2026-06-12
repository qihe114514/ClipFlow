# 抖音无水印视频下载器 v1.3

基于 [BugPk-Api](https://api.bugpk.com) 提供的抖音无水印解析服务，支持**视频、图集、实况（Live Photo）** 的解析与下载。采用 **Jetpack Compose + Material 3** 构建，适配 **Android 16（API 36）**，原生沉浸式体验。

## ✨ 功能

- **链接解析** — 粘贴抖音分享链接，一键解析出无水印视频/图集/实况
- **视频信息展示** — 显示分辨率/画质标签/帧率/文件大小
- **批量下载** — 支持主视频、备份画质、多图集、实况视频同时下载
- **实时进度** — 圆形进度条 + 百分比 + 实时网速显示
- **震动反馈** — 点击下载后 30ms 轻触清脆震动
- **粘贴按钮** — 输入框空时显示粘贴图标，一键填入剪贴板链接
- **保存到相册** — 通过 `MediaStore` 写入系统相册（视频→Movies/Douyin，图片→Pictures/Douyin）
- **自定义保存路径** — 设置中可修改下载目录
- **背景壁纸** — 支持图片/视频作为主页背景，可调节模糊度和透明度
- **Material You 动态取色** — 跟随系统壁纸自动适配主题色（Android 12+）
- **解析历史** — 自动记录解析过的链接，可回溯重解析
- **页面导航** — Navigation Compose 实现页面切换，带过渡动画

## 🛠️ 技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| **语言** | Kotlin 2.1.0 | — |
| **UI 框架** | Jetpack Compose + Material 3 | BOM 2024.12.01 |
| **导航** | Navigation Compose | 2.8.5 |
| **网络** | OkHttp 4.12.0 | API 客户端 + 流式下载 |
| **序列化** | kotlinx.serialization 1.7.3 | JSON 解析 |
| **图片加载** | Coil 3.1.0 | 异步图片加载 |
| **视频播放** | Media3 ExoPlayer 1.4.0 | 视频背景播放 |
| **本地存储** | DataStore Preferences | 持久化设置及历史 |
| **构建** | Gradle 8.12, AGP 8.7.2 | — |
| **编译目标** | Android 16 (API 36) | — |
| **API** | BugPk-Api (api.bugpk.com) | — |

## 🚀 构建与安装

### 方式一：从 GitHub Actions 下载预构建 APK

前往 [Actions 页面](https://github.com/qihe114514/qihe-douyin/actions)，选择最新的 **Build APK** 运行，下载 `douyin-downloader-debug` 工件。

### 方式二：本地编译

```bash
git clone https://github.com/qihe114514/qihe-douyin.git
cd qihe-douyin
# 用 Android Studio 打开，Sync Gradle，然后 Run 'app'
# 或使用命令行：
./gradlew assembleDebug
```

构建产物：`app/build/outputs/apk/debug/app-debug.apk`

## 📁 项目结构

```
├── app/src/main/java/com/douyin/downloader/
│   ├── MainActivity.kt           # 入口 + NavHost 导航
│   ├── api/
│   │   └── DouyinApiClient.kt    # BugPk API 客户端
│   ├── data/
│   │   └── SettingsDataStore.kt  # DataStore 持久化（含历史记录）
│   ├── download/
│   │   └── DownloadManager.kt    # OkHttp 流式下载 + MediaStore 相册入库
│   ├── model/
│   │   └── Models.kt            # API 数据模型 + 下载项提取
│   └── ui/
│       ├── MainViewModel.kt      # 状态管理 + 震动反馈
│       ├── screens/
│       │   ├── MainScreen.kt     # 主界面
│       │   ├── SettingsScreen.kt # 设置页
│       │   └── HistoryScreen.kt  # 解析历史
│       └── theme/
│           └── Theme.kt         # Material You 动态主题
├── app/build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── .github/workflows/build.yml   # GitHub Actions 自动构建
```

## 🙋 关于

- **开发者**：其核 [@qihe114514](https://github.com/qihe114514)
- **B站**：[https://space.bilibili.com/1049283248](https://space.bilibili.com/1049283248)
- **抖音**：[https://www.douyin.com/user/MS4wLjABAAAAuUtKOArTFKTBm4C6o5MwDQuGMNZ9-0CWZfUay6U9wUI](https://www.douyin.com/user/MS4wLjABAAAAuUtKOArTFKTBm4C6o5MwDQuGMNZ9-0CWZfUay6U9wUI)
- **API**：BugPk-Api (https://api.bugpk.com)

## 📄 许可证

仅供学习交流使用，请勿用于商业或违规用途。视频版权归原作者及抖音平台所有。