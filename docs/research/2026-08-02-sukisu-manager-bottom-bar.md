# SukiSU-Ultra 管理器底栏分析

## 结论

SukiSU-Ultra 管理器的底栏由两层组成：`MainPagerState` 管理主 pager 的选中页与点击导航，`FloatingBottomBar` 管理玻璃底栏、指示器拖动和按压视觉。

它解决“选中态晚一拍”的核心方式是：

1. 主界面使用 `HorizontalPager` 和一个共享的 `PagerState`。
2. `PagerState.currentPage` 改变时调用 `MainPagerState.syncPage()`，因此选中页在 pager 越过吸附阈值时同步，而不是等待 `settledPage`。
3. 点击 tab 时先写入 `selectedPage`，然后以自定义 `animateScrollBy` 滑到目标页；新的导航请求会取消旧的 `Job`。
4. `FloatingBottomBar` 内部的 `DampedDragAnimation` 负责指示器 pill 的弹性移动和按压反馈。

它并没有把底栏指示器直接绑定到 `currentPageOffsetFraction`。主界面滑动时，底栏的 `selectedPage` 会在 `currentPage` 改变后触发一次内部弹簧动画；因此这是“过中点后提前切换并播放 pill 动效”，不是每一帧与 pager 小数进度完全同步。

## 关键源码

### 1. MainPagerState

源码：
<https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/main/manager/app/src/main/java/com/sukisu/ultra/ui/component/bottombar/BottomBar.kt#L26-L75>

- `selectedPage` 初始化为 `pagerState.currentPage`。
- `animateToPage()` 第 41 行先取消 `navJob`，第 43 行立即更新 `selectedPage`，第 46-58 行根据当前 pager 偏移计算目标像素距离并用 `animateScrollBy` 动画。
- `finally` 中通过 `if (navJob == myJob)` 判断是否仍是最新请求，旧请求不会覆盖新请求的状态。
- `syncPage()` 只有在 `!isNavigating` 时才把选中页同步到 `pagerState.currentPage`，避免点击动画过程中被中间页抢回。

### 2. 主 pager 的同步时机

源码：
<https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/main/manager/app/src/main/java/com/sukisu/ultra/ui/MainActivity.kt#L287-L326>

- 第 287-290 行监听 `settledPage`，用于持久化最后完成的页码。
- 第 292-295 行监听 `currentPage`，调用 `mainPagerState.syncPage()`，这就是滑动越过页间阈值后更新底栏选中态的入口。
- 第 313-326 行的内容使用同一个 `mainPagerState.pagerState` 渲染 `HorizontalPager`。

这里有两个明确的时间点：`currentPage` 负责交互中的选中态，`settledPage` 负责最终完成状态和持久化。它没有使用路由变化来驱动底栏选中态。

### 3. Miuix 底栏接线

源码：
<https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/main/manager/app/src/main/java/com/sukisu/ultra/ui/component/bottombar/BottomBarMiuix.kt#L56-L134>

浮动底栏接收：

- `selectedIndex = { mainState.selectedPage }`
- `onSelected = { mainState.animateToPage(it) }`
- 每个 tab 的 `onClick` 也调用同一个 `mainState.animateToPage(index)`

普通 Miuix `NavigationBar` 使用同一个 `selectedPage` 判断 `selected`，点击也调用同一个导航方法。这样点击和拖动不会各自维护一份导航状态。

### 4. FloatingBottomBar 动画

源码：
<https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/main/manager/app/src/main/java/com/sukisu/ultra/ui/component/FloatingBottomBar.kt#L177-L275>

- `DampedDragAnimation` 的 `value` 是 pill 指示器的位置。
- 指示器自身拖动结束后，第 245-251 行把目标页四舍五入并清理面板橡皮筋偏移。
- 第 267-275 行监听 `selectedIndex()` 变化，调用 `animateToValue()`，并回调 `onSelected()`。
- 第 397-403 行把 `dampedDragAnimation.value` 转换为指示器的横向位移。

因此它的“中间动效”是内部 pill 从旧 tab 弹性移动到新 tab，而不是直接把 pill 的 `translationX` 设置成 pager 的小数位置。

## 对 ClipFlow 的借鉴

适合借鉴：

- 用一个 `MainPagerState` 统一“当前选中页、导航 Job、是否正在程序化导航”。
- 点击前立即更新选中页，避免等待 pager 动画完成。
- 新导航取消旧导航，并在 `finally` 中用 Job 身份检查防止旧请求回写状态。
- 将 `currentPage` 用于交互选中态，将 `settledPage` 用于最终同步/持久化。

不能直接照搬：

- SukiSU 的 `FloatingBottomBar` 不读取 `currentPageOffsetFraction`，所以它不是完全跟手的连续指示器。
- 如果 ClipFlow 的目标是“主界面拖到一半时，底栏 pill 已经开始从主页移动到抖音”，需要额外把 `currentPage + currentPageOffsetFraction` 作为视觉位置；仅复制 SukiSU 的 `currentPage` 监听，只能得到越过中点后的弹簧切换。

## 提交证据

SukiSU 在提交 `a13aa45c` 中增加了 `selectedMainPage` 的 `SavedStateHandle` 持久化，并把 `initialPage` 与 `onPageChanged` 传入 `MainScreen`：

<https://github.com/SukiSU-Ultra/SukiSU-Ultra/commit/a13aa45c>

这说明它的 pager 状态保存和底栏选中态同步是有意分开的设计，而不是依赖导航路由的偶然副作用。
