# Android Jetpack Compose 视频预览播放器调研

## 调研范围

- 调研日期：2026-08-04。
- 活跃性门槛：GitHub 在 2026-02-04 至 2026-08-04 之间存在真实 commit 或已发布 Release；日期以 GitHub commit/release 页面为准。
- 目标：点击抖音/小红书封面后，在 Android Jetpack Compose 中打开一个可自定义、支持横竖屏切换和全屏体验的视频预览。
- 资料范围：只使用候选仓库的 GitHub 仓库页、README、LICENSE、Release、commit history 和源码；所有链接均为一手资料，访问日期为 2026-08-04。

当前 ClipFlow 已经使用 Jetpack Compose，`minSdk = 29`，并在 `app/build.gradle.kts` 中引入 `androidx.media3:media3-exoplayer`、`media3-ui` 和 `media3-common:1.3.1`。预览现有实现是 Compose `AndroidView` 包装 Media3 `PlayerView`。因此本次优先评价“能否复用现有 Media3 播放链路”和“控制器是否能做成 ClipFlow 自己的视觉”，而不是单纯比较格式支持。

## 结论速览

| 候选 | 类型/引擎 | 许可证 | Compose 接入与控制器 | 全屏/方向 | 引入成本 | 结论 |
| --- | --- | --- | --- | --- | --- | --- |
| [AndroidX Media3](https://github.com/androidx/media) | 官方 Android 库；ExoPlayer/Media3 | Apache-2.0 | `media3-ui-compose` 和 `ui-compose-material3`；控制器槽位可完全替换 | 播放 surface 和内容缩放有 API；全屏、方向由宿主页面实现 | 低 | 首选，最适合 ClipFlow 现有依赖 |
| [Compose Media Player](https://github.com/kdroidFilter/ComposeMediaPlayer) | Compose Multiplatform 库；Android 使用 Media3 | MIT | `VideoPlayerSurface`、状态对象、自定义 overlay、字幕和缓存 | 有 `toggleFullscreen()`，但文档标为 experimental；方向仍由宿主处理 | 中 | 想快速得到“状态 + overlay + 全屏”的第二选择 |
| [MediaMP](https://github.com/open-ani/mediamp) | Compose Multiplatform 抽象层；Android 后端为 ExoPlayer | Apache-2.0 为主，后端需看各模块 | `MediampPlayerSurface` 只有画面，没有控制栏；控制器需自己写 | 没有通用全屏/方向 API | 中高 | 适合未来 KMP，不适合当前 Android-only 的最小改动 |
| [mpvRex](https://github.com/sfsakhawat999/mpvRex) | 完整 Android 应用/源码参考；libmpv | Apache-2.0（依赖仍需单独审查） | Compose 玻璃风格、手势、Shorts 等可借鉴，但不是可直接依赖的库 | README 明确有按视频强制横竖屏；全屏逻辑需从应用源码移植 | 很高 | 仅在需要 libmpv 能力或复杂播放器视觉时参考，不建议直接集成 |

## 1. AndroidX Media3：首选

### 活跃性和许可证

这是 AndroidX 官方仓库，README 将其定义为包含 ExoPlayer、Transformer、MediaSession 等 Android 媒体库的集合，许可证为 Apache License 2.0：[仓库 README](https://github.com/androidx/media/blob/release/README.md)、[LICENSE](https://github.com/androidx/media/blob/release/LICENSE)（访问：2026-08-04）。

在窗口期内存在多个真实 Release：`1.10.0` 发布于 2026-03-27，`1.11.0-beta01` 发布于 2026-07-08，`1.11.0-rc01` 发布于 2026-07-22：[Release 页面](https://github.com/androidx/media/releases)（访问：2026-08-04；对应发布日期：2026-03-27、2026-07-08、2026-07-22）。`5fb3064` commit 于 2026-05-07 更新到 1.10.1：[commit](https://github.com/androidx/media/commit/5fb306449733dd71595700c1227ad6087578c559)（访问：2026-08-04；提交日期：2026-05-07）。

### Android/Compose 兼容方式和引擎

Android 侧直接使用 Maven 模块，例如 `androidx.media3:media3-exoplayer`、`androidx.media3:media3-ui-compose`；官方 UI Compose README 要求 UI 模块与其他 Media3 模块使用相同版本：[ui_compose README](https://github.com/androidx/media/blob/release/libraries/ui_compose/README.md)（访问：2026-08-04）。播放器内核仍是 Media3/ExoPlayer，ClipFlow 当前已经使用同一技术栈，因此不需要新增 native 播放后端：[顶层 README 的依赖示例](https://github.com/androidx/media/blob/release/README.md)（访问：2026-08-04）。

`PlayerSurface` 是官方 Compose 包装，支持 `SurfaceView` 和 `TextureView` 两种 surface，并用 `AndroidView` 绑定 `Player`：[PlayerSurface.kt](https://github.com/androidx/media/blob/release/libraries/ui_compose/src/main/java/androidx/media3/ui/compose/PlayerSurface.kt)（访问：2026-08-04）。这与短视频预览需要的叠加 Compose UI 相匹配：如果要做动画、圆角、玻璃层或拖拽手势，应优先选 `TextureView`；如果更重视渲染效率，可评估 `SurfaceView` 的限制。

### 控制器样式能力

官方 Material 3 `Player` composable 把内容框和控制器拆开，暴露 `topControls`、`centerControls`、`bottomControls` 三个 Compose 插槽，以及 `showControls`、`contentScale`、`shutter` 等参数：[Player.kt](https://github.com/androidx/media/blob/release/libraries/ui_compose_material3/src/main/java/androidx/media3/ui/compose/material3/Player.kt)（访问：2026-08-04）。基础 Compose UI 模块还提供播放/暂停、前后跳转、静音、倍速、循环等按钮状态和进度/时间指示器：[ui_compose 源码目录](https://github.com/androidx/media/tree/release/libraries/ui_compose/src/main/java/androidx/media3/ui/compose)（访问：2026-08-04）。

这意味着 ClipFlow 可以保留 Media3 播放状态和 `Player`，只重做控制器层：封面淡出、播放按钮、进度条、倍速菜单、错误层和全屏按钮都可以用现有 Material 3/自有 Compose 组件完成，不必接受传统 `PlayerView` 的固定控制栏。官方 Compose demo 也展示了自定义进度条、显示/隐藏控制器和 `ContentScale`：[Compose demo `MainScreen.kt`](https://github.com/androidx/media/blob/release/demos/compose/src/main/java/androidx/media3/demo/compose/layout/MainScreen.kt)（访问：2026-08-04）。

### 横竖屏、全屏和引入成本

`PlayerSurface`/Material 3 `Player` 负责视频 surface、内容缩放和控制器布局；在审阅的官方 Compose API 中没有发现一个替应用切换 Activity orientation、沉浸式系统栏或全屏路由的通用 API：[Player.kt](https://github.com/androidx/media/blob/release/libraries/ui_compose_material3/src/main/java/androidx/media3/ui/compose/material3/Player.kt)、[PlayerSurface.kt](https://github.com/androidx/media/blob/release/libraries/ui_compose/src/main/java/androidx/media3/ui/compose/PlayerSurface.kt)（访问：2026-08-04）。因此横竖屏应由 ClipFlow 的预览页面通过 `requestedOrientation`/WindowInsets 管理；全屏可以用同一个 `Player` 放进全屏页面或 Dialog，并将控制器槽位保持在视频上层。

引入成本最低：将现有三项 Media3 依赖统一升级到选定版本，增加 `media3-ui-compose`；若使用 Material 3 `Player`，再增加 `media3-ui-compose-material3`。所有 Media3 模块必须保持同版本：[官方依赖说明](https://github.com/androidx/media/blob/release/README.md)（访问：2026-08-04）。需要注意新 Compose UI 中部分 API 标为 `@UnstableApi` 或 `@ExperimentalApi`，应在 ClipFlow 的封装边界集中处理 opt-in：[Player.kt](https://github.com/androidx/media/blob/release/libraries/ui_compose_material3/src/main/java/androidx/media3/ui/compose/material3/Player.kt)（访问：2026-08-04）。

**判断：首选。** 它和 ClipFlow 现有播放链路一致，依赖最少，控制器可完全 Compose 化；“美观预览”的主要工作留在 ClipFlow 自己的页面和动效，而不是承担第三方状态层的迁移。

## 2. Compose Media Player：快速得到完整状态和全屏骨架

### 活跃性、许可证和接入

仓库是 Compose Multiplatform 视频库，README 给出 Maven Central 坐标 `io.github.kdroidfilter:composemediaplayer`，许可证为 MIT：[主 README](https://github.com/kdroidfilter/ComposeMediaPlayer/blob/master/README.MD)、[LICENSE](https://github.com/kdroidfilter/ComposeMediaPlayer/blob/master/LICENSE)（访问：2026-08-04）。窗口期内有 `v0.9.0`（2026-04-11）、`v0.10.0`（2026-04-17）、`v0.10.1`（2026-05-03）和 `v0.11.2`（2026-07-16）等 Release：[Release 页面](https://github.com/kdroidFilter/ComposeMediaPlayer/releases)（访问：2026-08-04；对应发布日期：2026-04-11 至 2026-07-16）。`800fd0e` 于 2026-07-18 提交了 Android 之外的音频 URL 处理修复：[commit](https://github.com/kdroidFilter/ComposeMediaPlayer/commit/800fd0e0b219d17241ce9076318f5430290f6c28)（访问：2026-08-04；提交日期：2026-07-18）。

Android actual 实现通过 `AndroidView` 包装 Media3 `PlayerView`，并默认使用 `TextureView`；也提供 `SurfaceType.SurfaceView`。源码将 `contentScale` 映射为 Media3 的 resize mode，并把控制器关闭后交给 Compose overlay：[VideoPlayerSurface.android.kt](https://github.com/kdroidFilter/ComposeMediaPlayer/blob/master/mediaplayer/src/androidMain/kotlin/io/github/kdroidfilter/composemediaplayer/VideoPlayerSurface.android.kt)（访问：2026-08-04）。README 明确说明 Android 后端是 Media3，并给出 HLS 额外依赖 `androidx.media3:media3-exoplayer-hls`：[README_VIDEO.MD](https://github.com/kdroidFilter/ComposeMediaPlayer/blob/master/README_VIDEO.MD)（访问：2026-08-04）。

### 控制器、横竖屏和全屏

它提供 `rememberVideoPlayerState()` 和 `VideoPlayerSurface`，状态对象包含播放/暂停、停止、seek、音量、循环、倍速、错误、元数据和字幕等能力：[VideoPlayerState.kt](https://github.com/kdroidFilter/ComposeMediaPlayer/blob/master/mediaplayer/src/commonMain/kotlin/io/github/kdroidfilter/composemediaplayer/VideoPlayerState.kt)（访问：2026-08-04）。README 的 “Full Controls” 章节要求调用这些状态 API 自己组合按钮和进度控件；因此控制器样式自由度高，但它不是拿来即用的完整 Material 控制栏：[Full Controls](https://github.com/kdroidFilter/ComposeMediaPlayer/blob/master/README_VIDEO.MD#-full-controls)（访问：2026-08-04）。

`overlay` 会叠加在视频上，并且在全屏时仍显示，适合放返回、播放/暂停、进度条和倍速菜单：[Custom Overlay UI](https://github.com/kdroidFilter/ComposeMediaPlayer/blob/master/README_VIDEO.MD#custom-overlay-ui)（访问：2026-08-04）。播放器提供 `isFullscreen` 和 `toggleFullscreen()`，Android actual 实现使用无平台宽度的 Compose `Dialog` 承载全屏内容：[FullScreenLayout.kt](https://github.com/kdroidFilter/ComposeMediaPlayer/blob/master/mediaplayer/src/commonMain/kotlin/io/github/kdroidfilter/composemediaplayer/util/FullScreenLayout.kt)、[全屏文档](https://github.com/kdroidFilter/ComposeMediaPlayer/blob/master/README_VIDEO.MD#-fullscreen-mode)（访问：2026-08-04）。文档明确把全屏标为 experimental，且全屏默认不提供 UI，必须自行写 overlay；没有看到一个自动锁定横屏/恢复竖屏的 Android orientation API，因此方向仍应由 ClipFlow 宿主管理。

### 引入成本和判断

表面引入只需一个 Maven 依赖，但它是 Kotlin Multiplatform 库，当前 README 的兼容性表只列到 0.9.0，而窗口期最新 Release 已是 0.11.2：[安装和兼容性表](https://github.com/kdroidFilter/ComposeMediaPlayer/blob/master/README_VIDEO.MD#-installation)（访问：2026-08-04）。ClipFlow 还要核对 Kotlin/Compose/Media3 版本解析结果，避免它带来的 `media3-ui` 与当前 1.3.1 冲突；HLS 预览还要显式增加 HLS 模块。若需要磁盘缓存，README 还提供 Android Media3 `SimpleCache` 的配置，但这会增加缓存生命周期和清理策略的验证成本：[缓存文档](https://github.com/kdroidFilter/ComposeMediaPlayer/blob/master/README_VIDEO.MD#video-caching)（访问：2026-08-04）。

**判断：第二选择。** 当目标是尽快获得一个带状态对象、字幕、缓存、overlay 和全屏骨架的预览页时它很有吸引力；但对于当前已经拥有 Media3 `PlayerView` 的 ClipFlow，迁移到它的状态模型会比直接采用官方 Media3 Compose UI 多一层适配。

## 3. MediaMP：适合未来的 Compose Multiplatform 抽象

### 活跃性、许可证和 Android 后端

MediaMP README 将自己定义为 Compose Multiplatform 的媒体播放器，Android 后端使用 ExoPlayer，且通过 `mediamp-all` 提供统一 API：[README](https://github.com/open-ani/mediamp/blob/main/README.md)（访问：2026-08-04）。窗口期内至少有 `dd6d92b`（2026-08-03，升级 AGP/Gradle）和 `0661c9d`（2026-08-03，HTTP proxy 修复）等真实提交：[commit `dd6d92b`](https://github.com/open-ani/mediamp/commit/dd6d92bb9c0e9ab5f32b06728d5a4374b3b359df)、[commit `0661c9d`](https://github.com/open-ani/mediamp/commit/0661c9d26c07b519100e7f13faf5687d9103662a)（访问：2026-08-04；提交日期：2026-08-03）。

仓库说明版本示例为 `0.0.23`，并明确 `mediamp-all` 包含 common API、Compose UI API 和 Android ExoPlayer backend；Android 后端还可带 HLS 模块：[安装章节](https://github.com/open-ani/mediamp/blob/main/README.md#installation)（访问：2026-08-04）。根许可证为 Apache License 2.0，但 README 也说明不同后端要单独检查依赖许可证；`mediamp-exoplayer` 是 Apache-2.0，而 VLC 后端含 GPLv3：[许可证章节](https://github.com/open-ani/mediamp/blob/main/README.md#license)、[LICENSE](https://github.com/open-ani/mediamp/blob/main/LICENSE)（访问：2026-08-04）。

### 控制器、横竖屏和全屏

`MediampPlayerSurface` 是 common Compose surface，源码明确写着“没有 control bar 或其他 UI”，它更像一块 Canvas；播放状态、seek、倍速、缓冲和媒体信息通过 `MediampPlayer`/features 暴露：[MediampPlayerSurface.kt](https://github.com/open-ani/mediamp/blob/main/mediamp-api/src/commonMain/kotlin/compose/MediampPlayerSurface.kt)、[MediampPlayer.kt](https://github.com/open-ani/mediamp/blob/main/mediamp-api/src/commonMain/kotlin/MediampPlayer.kt)（访问：2026-08-04）。Android 专用 `ExoPlayerMediampPlayerSurface` 仍是 `AndroidView` + Media3 `PlayerView`，并提供 FIT/STRETCH/CROP 的 aspect-ratio 配置：[ExoPlayerMediampSurface.kt](https://github.com/open-ani/mediamp/blob/main/mediamp-exoplayer/src/androidMain/kotlin/compose/ExoPlayerMediampSurface.kt)（访问：2026-08-04）。

因此控制器样式完全由 ClipFlow 自己实现，但需要自己监听 StateFlow、处理生命周期和错误态。审阅的 common API 和 Android surface 中没有通用全屏或 orientation API；横竖屏、沉浸式系统栏和全屏页面必须由宿主 Android 页面实现。它的优点是可通过 `player.impl` 取到底层 Android `ExoPlayer` 做高级控制：[底层播放器示例](https://github.com/open-ani/mediamp/blob/main/README.md#obtaining-the-platform-player)（访问：2026-08-04）。

### 引入成本和判断

它需要把 KMP 依赖接入 `commonMain`，再决定使用 `mediamp-all` 还是拆分 `mediamp-api`/`mediamp-exoplayer`；后者能减少无关平台后端，但需要更仔细的 source set 配置：[模块树](https://github.com/open-ani/mediamp/tree/main)（访问：2026-08-04）。README 明确警告 0.1.0 前没有 API/ABI 保证，并且项目仍是 work in progress：[稳定性警告](https://github.com/open-ani/mediamp/blob/main/README.md#installation)（访问：2026-08-04）。

**判断：不建议作为当前 ClipFlow Android-only 预览的第一实现。** 它的抽象很干净，适合以后需要 Android/iOS/Desktop 共用播放器 API 时采用；当前需求会为一个已经使用 Media3 的 Android 应用增加 KMP 层和自制控制器工作量。

## 4. mpvRex：完整播放器源码参考，不是直接依赖

### 活跃性、许可证和定位

mpvRex README 将项目定位为基于 libmpv、使用 Jetpack Compose 的完整 Android 视频播放器，并声明 Apache License 2.0：[README](https://github.com/sfsakhawat999/mpvRex/blob/master/README.md)、[LICENSE](https://github.com/sfsakhawat999/mpvRex/blob/master/LICENSE)（访问：2026-08-04）。窗口期内有 `v4.0.0`（2026-06-10）、`v4.3.0`（2026-07-12）、`v4.4.1`（2026-07-31）等 Release：[Release 页面](https://github.com/sfsakhawat999/mpvRex/releases)（访问：2026-08-04；对应发布日期：2026-06-10 至 2026-07-31）。`75402fe` 于 2026-08-03 修复后台播放关闭时 MiniPlayer 仍显示的问题：[commit](https://github.com/sfsakhawat999/mpvRex/commit/75402fe537370429db2d51eab86df4d43cd43d42)（访问：2026-08-04；提交日期：2026-08-03）。

它不是 Maven 播放库：README 的安装内容是下载应用 Release/Preview build，而不是向另一个 Android 项目添加依赖：[安装章节](https://github.com/sfsakhawat999/mpvRex/blob/master/README.md#installation)（访问：2026-08-04）。因此这里只把它算作“可复用源码组件/设计参考”候选，不把它和前三个库按同一引入方式比较。

### 控制器、横竖屏和引擎差异

README 明确列出玻璃风格 player UI、可配置双击 seek、seek 取消、缩放/平移、字幕拖动、Shorts mode 和手势控制：[Playback & Gestures](https://github.com/sfsakhawat999/mpvRex/blob/master/README.md#playback--gestures)、[UI & Aesthetics](https://github.com/sfsakhawat999/mpvRex/blob/master/README.md#ui--aesthetics)（访问：2026-08-04）。这些内容对 ClipFlow 的“美观视频预览”很有参考价值，但需要移植应用内部状态、手势和 JNI 边界，不能只替换一个 Compose composable。

它使用 libmpv，不是 Media3/ExoPlayer；README 的 Engine & Customization 章节还列出 HDR-to-SDR、网络流代理和独立音频等能力：[Engine & Customization](https://github.com/sfsakhawat999/mpvRex/blob/master/README.md#engine--customization)（访问：2026-08-04）。README 明确支持按视频保存的 Smart Orientation，可强制横屏/竖屏：[Smart Orientation](https://github.com/sfsakhawat999/mpvRex/blob/master/README.md#engine--customization)（访问：2026-08-04）。不过它没有提供可直接调用的全屏组件 API；全屏/播放器页面需要从完整应用源码中拆出后自行维护。

### 引入成本和判断

移植它需要引入或复用 libmpv/mpv-android 的 native/JNI 构建、播放器状态、文件浏览/网络代理和应用级设置，同时重新审查 libmpv 及相关依赖的许可证；这远高于在现有 Media3 上加入 Compose 控制器。仓库 README 也说明项目源自 mpvEx，并基于 mpv-android：[Credits](https://github.com/sfsakhawat999/mpvRex/blob/master/README.md#credits)（访问：2026-08-04）。

**判断：不作为 ClipFlow 的直接集成方案。** 只有当 Media3 在目标视频格式、HDR、手势或 Shorts 播放体验上无法满足需求时，才值得将其作为源码和视觉参考；当前“封面点击进入预览”的需求没有足够理由承担 libmpv 的 native 成本。

## 推荐落地顺序

1. **AndroidX Media3 Compose UI**：继续使用现有 `ExoPlayer`，将 `PlayerView` 逐步替换为 `PlayerSurface`/Material 3 `Player`，全屏和方向由 ClipFlow 预览页控制。预计改动面和风险最低。
2. **Compose Media Player**：如果希望直接获得 `VideoPlayerState`、自定义 overlay、缓存和 experimental 全屏骨架，再做一次依赖冲突与 Kotlin/Compose 版本验证。
3. **MediaMP**：仅在 ClipFlow 明确要走 Compose Multiplatform、并接受 0.1.0 前 API 不稳定时考虑。
4. **mpvRex**：只提取交互和视觉设计，不把完整应用当作播放器依赖；除非以后明确需要 libmpv 的格式或处理能力。

## 未纳入的项目

本次没有把普通 Compose 示例应用当作可用库。比如 [Next Player](https://github.com/anilbeesetti/nextplayer) 在窗口期有真实 Release，但仓库定位是完整 Android 应用，许可证为 GPL-3.0，README 没有可直接引入的库 API：[README](https://github.com/anilbeesetti/nextplayer/blob/main/README.md)、[LICENSE](https://github.com/anilbeesetti/nextplayer/blob/main/LICENSE)（访问：2026-08-04）。这类项目可以用于交互参考，但不如上面四个候选适合作为 ClipFlow 的播放器库筛选结果。
