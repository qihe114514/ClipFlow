# Android Compose 局部高斯模糊库调研

调研日期：2026-08-04（UTC）
活动窗口：2026-02-04 至 2026-08-04（含首尾）
目标：为 ClipFlow 的标题栏选择一种只采集主界面内容、不采集壁纸，并能在 Compose 中实现局部实时/渐进模糊的方案。

## ClipFlow 约束

- 当前应用是 Jetpack Compose Android 应用，`minSdk = 29`。
- 当前标题栏和主界面都在 Compose 导航层中，壁纸位于更外层；因此需要“显式标记模糊源”的 API，而不是对整个窗口截图。
- 当前需求是标题栏位置的局部背景模糊，标题、返回、历史、设置按钮保持清晰；如果库支持渐进模糊，还希望顶部更强、向内容方向逐渐减弱。
- 现有 `miuix-blur-android` 方案在当前代码中只对 API 33+ 启用；本次调研只研究候选库，不修改现有实现。

## 结论

推荐顺序：

1. **Haze**：最适合作为 ClipFlow 的 Compose 原生首选。`hazeSource` 只登记需要被采集的内容，`hazeEffect` 只在标题栏区域绘制模糊，天然满足排除壁纸和保持标题控件清晰的结构要求；官方文档还提供 progressive blur 和 Android 低版本说明。
2. **Cloudy**：如果“渐进式模糊”是第一优先级，它的 `progressive = CloudyProgressive.TopToBottom()` API 最直接，且 `rememberSky`/`Modifier.sky` 同样能限定采集源。但当前版本线包含 `1.0.0-alpha01`，Android 32 及以下的 backdrop progressive 会降级为均匀模糊。
3. **Dimezis/BlurView**：Android 端成熟度和 API 29/30 兼容路径较好，且 `BlurTarget` 可以明确限定采集内容；但它是 View 库，不是 Compose modifier。接入 ClipFlow 需要增加 `AndroidView`/View 层桥接，调试和布局成本高于前两者。

三者都比“对整层 Compose 或窗口做模糊”更适合当前问题，因为它们允许把模糊源与模糊显示层分开。若只在现有 Compose 页面中替换标题栏效果，优先验证 Haze；若验证结果中渐进方向和视觉控制更重要，再对比 Cloudy。

## 候选比较

| 候选 | 最近活动证据 | Android / Compose | 局部实时高斯模糊 | 渐进模糊 | API 适配 | 许可证 | ClipFlow 接入成本 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| [chrisbanes/haze](https://github.com/chrisbanes/haze) | commit `2c95760`，2026-08-04；release `2.0.0-alpha03`，2026-06-08 | Compose Multiplatform，Android 支持 | `hazeSource` 标记源，`hazeEffect` 在指定 composable 区域渲染 | 官方 `HazeProgressive`，支持线性/径向/Brush mask | 官方文档标注 API 11+，Android 13+ 最优；Android 12 及以下有降级/性能差异 | Apache-2.0 | 低到中；需要 `haze` + `haze-blur`，并把主内容挂到 `hazeSource` |
| [skydoves/Cloudy](https://github.com/skydoves/Cloudy) | commit `cd34c2d`，2026-07-17；release `1.0.0-alpha01`，2026-07-17 | Compose Multiplatform，Android API 23+ | `rememberSky` + `Modifier.sky` 采集源，`Modifier.cloudy(sky = ...)` 绘制指定区域 | `CloudyProgressive.TopToBottom/BottomToTop/Edges` | Android 33+ 使用 AGSL；31-32 使用 RenderEffect；30 及以下 CPU blur 或 scrim | Apache-2.0 | 低；API 与需求高度贴合，但 alpha 版本和低版本降级需要额外验收 |
| [Dimezis/BlurView](https://github.com/Dimezis/BlurView) | commit `ab56ebc`，2026-07-26；最近 release `version-3.2.0`，2025-10-24 | Android View 库；README 明确讨论 Haze for Compose，但没有 Compose 原生 modifier | `BlurTarget` 限定被采集 View，`BlurView` 的 children 不会被模糊；可实时跟随 View 层变化 | 没有 Compose 渐进 blur API；需要自行叠加渐变 mask/多层效果 | library `minSdkVersion 18`；API 31+ RenderNode/RenderEffect，旧版本走兼容路径；近期提交加入 API 29-30 同步 OpenGL 路径 | Apache-2.0 | 中到高；需要 AndroidView 或 ComposeView/ViewGroup 桥接，且要单独验证 Compose 重绘与层级 |

## 1. Haze

官方仓库：<https://github.com/chrisbanes/haze>
官方 README：<https://github.com/chrisbanes/haze/blob/main/README.md>
官方 blur 用法：<https://chrisbanes.github.io/haze/latest/blur/usage/>
官方平台说明：<https://chrisbanes.github.io/haze/latest/blur/platforms/>
许可证：<https://github.com/chrisbanes/haze/blob/main/LICENSE>

官方用法是先创建 `HazeState`，在主内容容器使用 `Modifier.hazeSource(state)`，再在标题栏使用 `Modifier.hazeEffect(state)`。文档说明 `hazeEffect` 默认只绘制 z-order 低于对应 source 的层，并支持多个 source 和 `zIndex`。因此可以把 `hazeSource` 放在主界面 Compose 内容的外层、放在壁纸层内侧，标题栏只作为 effect 层；壁纸没有 source 标记时不会进入采集范围。

官方 blur 页面将 Android 支持列为 API 11+、API 13+ 优化。平台说明中，Android 13+ 是最佳路径；Android 12/12L 有 progressive 的性能和实现限制；Android 12 及以下需要额外失效处理；Android 11 及以下默认关闭真正模糊并使用 scrim，开启 RenderScript blur 属于实验能力。ClipFlow 的 API 29-30 因而不能只看“能编译”，必须在真机/模拟器上确认帧率、重绘和降级外观。

Haze 的官方 README 给出的接入模块是：

```kotlin
implementation("dev.chrisbanes.haze:haze:<version>")
implementation("dev.chrisbanes.haze:haze-blur:<version>")
```

活动证据：

- [2026-08-04 commit `2c95760`](https://github.com/chrisbanes/haze/commit/2c95760f4a2ebde4875b3801aa916f460689bdb9)：`Rename fixed Glass optics and add direct DSL (#1192)`。
- [2026-06-08 release `2.0.0-alpha03`](https://github.com/chrisbanes/haze/releases/tag/2.0.0-alpha03)。如果不希望接入 2.0 alpha，应单独评估同仓库的稳定 1.7.x API 与文档对应关系。

## 2. Cloudy

官方仓库：<https://github.com/skydoves/Cloudy>
官方 README：<https://github.com/skydoves/Cloudy/blob/main/README.md>
许可证：<https://github.com/skydoves/Cloudy/blob/main/LICENSE>

Cloudy 明确区分两种效果：`Modifier.cloudy(radius = ...)` 是模糊 composable 自身内容；`Modifier.cloudy(sky = ...)` 是 backdrop blur。当前需求必须使用后者。官方示例用 `rememberSky()` 创建共享状态，在要采集的主内容上使用 `Modifier.sky(sky)`，在标题栏 overlay 上使用 `Modifier.cloudy(sky = sky, ...)`。因此同样可以排除壁纸：只把主界面控件所在容器挂到 `sky`，不要把壁纸容器挂到 `sky`。

Cloudy 的渐进 API 是当前候选中最直观的：`CloudyProgressive.TopToBottom(start, end, easing)`、`BottomToTop` 和 `Edges`。官方 README 说明 Android 33+ 使用 AGSL progressive shader；Android 32 及以下 backdrop progressive 降级为均匀模糊。平台表还说明 Android 31-32 使用 RenderEffect，Android 30 及以下默认显示 scrim，打开 `cpuBlurEnabled` 才进行 CPU blur。ClipFlow 的 API 29 必须接受这个降级，或将 CPU blur 限制在低频变化场景。

官方 README 给出的 Maven 坐标是：

```kotlin
implementation("com.github.skydoves:cloudy:1.0.0-alpha01")
```

活动证据：

- [2026-07-17 commit `cd34c2d`](https://github.com/skydoves/Cloudy/commit/cd34c2dac45b2e990875b6b5c541c5d7c58089d9)：合并 `feature/edsl-array-read`，同日还有多个 blur/shader 相关提交。
- [2026-07-17 release `1.0.0-alpha01`](https://github.com/skydoves/Cloudy/releases/tag/1.0.0-alpha01)。

## 3. Dimezis/BlurView

官方仓库：<https://github.com/Dimezis/BlurView>
官方 README：<https://github.com/Dimezis/BlurView/blob/master/README.md>
3.0 API 说明：<https://github.com/Dimezis/BlurView/blob/master/BlurView_3.0.md>
许可证：<https://github.com/Dimezis/BlurView/blob/master/LICENSE.md>

BlurView 的 API 模型非常符合“只采集主界面”的要求：`BlurTarget` 包住要模糊的 View，`BlurView.setupWith(target)` 绑定它；README 明确说明 BlurView 自己的 children 不会被模糊。将标题栏作为 BlurView 的 children，可以保持标题、返回、历史和设置按钮清晰；将壁纸放在 BlurTarget 外，也能避免采集壁纸。

它的代价是接入边界不同。官方库是 Android View 的 `FrameLayout`/`ViewGroup` 方案，不提供 Haze/Cloudy 那样的 Compose modifier。ClipFlow 需要在 Compose 和 View 层之间增加桥接，且要核对 Compose 内容在 View snapshot 中的重绘时机。3.0 文档说明 API 31+ 使用 RenderNode/RenderEffect，旧 API 使用旧路径；library 的官方 Gradle 文件声明 `minSdkVersion 18`。近期提交还专门修复了 API 29-30 的同步 OpenGL 路径，因此它是低版本 Android 兼容性最强的候选，但不代表 Compose 接入成本最低。

官方 README 给出的稳定依赖示例是：

```gradle
implementation "com.github.Dimezis:BlurView:version-3.2.0"
```

该库使用 JitPack，版本更新和依赖可复现性需要按其 release tag 管理。官方 README 还说明 API 31+ 的模糊在系统 Render Thread 中完成，并强调只在层级发生变化时更新，而不是无条件让整个 View 层持续 invalidate。

活动证据：

- [2026-07-26 commit `ab56ebc`](https://github.com/Dimezis/BlurView/commit/ab56ebc7e864d673386db42e43cae81a3d330b85)：`Remove canRecordRenderNode`；同日连续提交修复缩放、旋转和 API < 31 路径。
- [2026-07-24 commit `7fb6aaf`](https://github.com/Dimezis/BlurView/commit/7fb6aafef94317f8bf1fff0d09a09ed7e8c7bc48)：`feat: add synchronous OpenGL blur path for API 29-30 (#265)`。
- 最近 release 是 [3.2.0，2025-10-24](https://github.com/Dimezis/BlurView/releases/tag/version-3.2.0)，不在最近六个月内；它满足本次“最近六个月有提交或发布”的条件，但应明确这是 commit 活跃而非 release 活跃。

## 不建议直接采用的方案

- `Modifier.blur`/`RenderEffect` 只能作为 Compose 自身内容的局部 blur 基础，不等价于 backdrop blur；它不能单独解决“标题栏采集下方主界面、排除壁纸”的 source 管理问题。
- `mmin18/RealtimeBlurView` 的仓库描述虽然是 Android realtime blurring overlay，但其 GitHub 页面没有清晰的 SPDX 许可证声明，而且其 README 所代表的持续 invalidate 模型不如 BlurView 的显式 target/按需更新模型适合当前页面；因此不列入推荐候选。

## 对 ClipFlow 的落地建议

先做一个最小验证页，而不是立刻替换全局导航：

1. 用一个 source 容器只包住 `MainPager`/解析内容，不包 `BackgroundWallpaperLayer`。
2. 在同一个 root overlay 中放一个固定高度的标题栏 effect，effect 层先绘制，标题栏文字和按钮作为其 children 后绘制。
3. 在 API 33+ 验证真正的渐进 blur；API 31-32 验证均匀 blur 降级；API 29-30 验证 scrim/CPU 降级和启动稳定性。
4. 用截图和像素检查确认：壁纸纹理不出现在标题栏模糊里，主界面控件的轮廓能被模糊带采集，标题文字/图标保持清晰，模糊区域位于标题栏自身而不是标题栏下方。

研究结论：**首选 Haze；以渐进 API 简洁度为优先时选 Cloudy；只有在需要 Android View 级兼容路径且愿意承担 Compose 桥接成本时选 BlurView。**
