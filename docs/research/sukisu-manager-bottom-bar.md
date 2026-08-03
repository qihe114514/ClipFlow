# SukiSU-Ultra 管理器底栏研究

## 研究基准

- 仓库：[SukiSU-Ultra/SukiSU-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra)
- 固定提交：`35467545b2826e3acfc88699755981a889956b1a`（`main`）
- 研究范围：`manager/app` Android 管理器应用；以下 GitHub 链接均固定到该提交，行号可复核。

## 简明结论

1. 底栏入口和布局分发在 `MainActivity.kt` 的 `MainScreen`：窄屏使用 `BottomBar`，宽屏切换到 `SideRail`；四个主页面共用一个 `HorizontalPager`。
2. 底栏选中态的直接状态源是 `LocalMainPagerState.current.selectedPage`，实际所有者是 `BottomBar.kt` 中的 `MainPagerState`。点击底栏时它先写入目标页，再驱动 `PagerState` 滚动，因此动画期间选中态可以先于页面稳定切换。
3. 页面稳定后，`MainActivity` 通过 `settledPage` 调用 `onPageChanged`，最终写入 `MainActivityViewModel` 的 `SavedStateHandle["selected_main_page"]`。这个是恢复/持久化边界，不是底栏每帧读取的直接状态源。
4. Material 底栏使用 Material 3 `ShortNavigationBarItem(selected = ...)` 的内置指示器；普通 Miuix 底栏使用 `NavigationBarItem(selected = ...)`；启用 Miuix 浮动底栏时，选中胶囊由 `FloatingBottomBar.kt` 自绘并用 `DampedDragAnimation` 做弹簧、按压缩放和拖拽动画。

## 1. 底栏实现文件

### 1.1 公共分发与状态

文件：[BottomBar.kt](https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/35467545b2826e3acfc88699755981a889956b1a/manager/app/src/main/java/com/sukisu/ultra/ui/component/bottombar/BottomBar.kt)

关键证据（[L26-L76](https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/35467545b2826e3acfc88699755981a889956b1a/manager/app/src/main/java/com/sukisu/ultra/ui/component/bottombar/BottomBar.kt#L26-L76)）：

```kotlin
class MainPagerState(
    val pagerState: PagerState,
    private val coroutineScope: CoroutineScope
) {
    var selectedPage by mutableIntStateOf(pagerState.currentPage)
        private set
    var isNavigating by mutableStateOf(false)
        private set

    fun animateToPage(targetIndex: Int) {
        ...
        selectedPage = targetIndex
        isNavigating = true
        ...
        pagerState.animateScrollBy(
            value = scrollPixels,
            animationSpec = tween(easing = EaseInOut, durationMillis = duration)
        )
    }

    fun syncPage() {
        if (!isNavigating && selectedPage != pagerState.currentPage) {
            selectedPage = pagerState.currentPage
        }
    }
}
```

同文件 [L94-L115](https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/35467545b2826e3acfc88699755981a889956b1a/manager/app/src/main/java/com/sukisu/ultra/ui/component/bottombar/BottomBar.kt#L94-L115) 根据 `LocalUiMode` 分发到 `BottomBarMiuix` 或 `BottomBarMaterial`；同样的状态也用于宽屏 `SideRail`。

### 1.2 Material 底栏

文件：[BottomBarMaterial.kt](https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/35467545b2826e3acfc88699755981a889956b1a/manager/app/src/main/java/com/sukisu/ultra/ui/component/bottombar/BottomBarMaterial.kt)

证据（[L34-L80](https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/35467545b2826e3acfc88699755981a889956b1a/manager/app/src/main/java/com/sukisu/ultra/ui/component/bottombar/BottomBarMaterial.kt#L34-L80)）：

```kotlin
val mainPagerState = LocalMainPagerState.current
...
ShortNavigationBar {
    items.forEachIndexed { index, (label, selectedIcon, unselectedIcon) ->
        val selected = mainPagerState.selectedPage == index
        ShortNavigationBarItem(
            selected = selected,
            onClick = {
                if (!selected) mainPagerState.animateToPage(index)
            },
            icon = {
                NavigationIconWithBadge(
                    icon = if (selected) selectedIcon else unselectedIcon,
                    ...
                )
            },
            ...
        )
    }
}
```

因此 Material 的选中图标由本文件选择，选中背景/指示器由 Material 3 `ShortNavigationBarItem` 绘制。

### 1.3 Miuix 普通与浮动底栏

文件：[BottomBarMiuix.kt](https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/35467545b2826e3acfc88699755981a889956b1a/manager/app/src/main/java/com/sukisu/ultra/ui/component/bottombar/BottomBarMiuix.kt)

普通底栏证据（[L66-L86](https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/35467545b2826e3acfc88699755981a889956b1a/manager/app/src/main/java/com/sukisu/ultra/ui/component/bottombar/BottomBarMiuix.kt#L66-L86)）：

```kotlin
NavigationBar {
    items.forEachIndexed { index, item ->
        NavigationBarItem(
            selected = mainState.selectedPage == index,
            onClick = { mainState.animateToPage(index) },
            ...
        )
    }
}
```

浮动底栏接线证据（[L87-L133](https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/35467545b2826e3acfc88699755981a889956b1a/manager/app/src/main/java/com/sukisu/ultra/ui/component/bottombar/BottomBarMiuix.kt#L87-L133)）：

```kotlin
FloatingBottomBar(
    selectedIndex = { mainState.selectedPage },
    onSelected = { mainState.animateToPage(it) },
    tabsCount = items.size,
    ...
) { ... }
```

浮动底栏的实现文件是：[FloatingBottomBar.kt](https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/35467545b2826e3acfc88699755981a889956b1a/manager/app/src/main/java/com/sukisu/ultra/ui/component/FloatingBottomBar.kt)。`FloatingBottomBarItem` 在 [L148-L174](https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/35467545b2826e3acfc88699755981a889956b1a/manager/app/src/main/java/com/sukisu/ultra/ui/component/FloatingBottomBar.kt#L148-L174) 提供可点击的 Tab 内容。

## 2. 选中态与指示器状态源

状态关系如下：

```text
SavedStateHandle["selected_main_page"]
        ^                         |
        | settledPage             | initialPage
MainActivityViewModel.selectedMainPage
        ^                         v
MainActivity.onPageChanged   rememberPagerState
                                  ^
                                  |
                         MainPagerState.pagerState
                                  ^
                                  |
                         MainPagerState.selectedPage
                                  ^
                                  |
                   BottomBar / FloatingBottomBar
```

### 2.1 直接状态源：`MainPagerState.selectedPage`

底栏所有实现都通过 `LocalMainPagerState` 读取 `selectedPage`：

- Material：[BottomBarMaterial.kt:L36-L62](https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/35467545b2826e3acfc88699755981a889956b1a/manager/app/src/main/java/com/sukisu/ultra/ui/component/bottombar/BottomBarMaterial.kt#L36-L62)
- 普通 Miuix：[BottomBarMiuix.kt:L56-L82](https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/35467545b2826e3acfc88699755981a889956b1a/manager/app/src/main/java/com/sukisu/ultra/ui/component/bottombar/BottomBarMiuix.kt#L56-L82)
- 浮动 Miuix：[BottomBarMiuix.kt:L88-L100](https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/35467545b2826e3acfc88699755981a889956b1a/manager/app/src/main/java/com/sukisu/ultra/ui/component/bottombar/BottomBarMiuix.kt#L88-L100)

`MainPagerState.animateToPage` 会在启动滚动前将 `selectedPage` 写成目标页，[BottomBar.kt:L38-L68](https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/35467545b2826e3acfc88699755981a889956b1a/manager/app/src/main/java/com/sukisu/ultra/ui/component/bottombar/BottomBar.kt#L38-L68)；滚动结束后若实际页不等于目标页则回退到 `pagerState.currentPage`。用户直接滑动 Pager 时，[L71-L75](https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/35467545b2826e3acfc88699755981a889956b1a/manager/app/src/main/java/com/sukisu/ultra/ui/component/bottombar/BottomBar.kt#L71-L75) 的 `syncPage` 会同步选中态。

### 2.2 恢复/持久化源：`SavedStateHandle`

文件：[MainActivityViewModel.kt](https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/35467545b2826e3acfc88699755981a889956b1a/manager/app/src/main/java/com/sukisu/ultra/ui/viewmodel/MainActivityViewModel.kt)

证据（[L29-L43](https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/35467545b2826e3acfc88699755981a889956b1a/manager/app/src/main/java/com/sukisu/ultra/ui/viewmodel/MainActivityViewModel.kt#L29-L43) 与 [L73-L89](https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/35467545b2826e3acfc88699755981a889956b1a/manager/app/src/main/java/com/sukisu/ultra/ui/viewmodel/MainActivityViewModel.kt#L73-L89)）：

```kotlin
val selectedMainPage: StateFlow<Int> = mainPageState.selectedPage

fun setSelectedMainPage(page: Int) {
    mainPageState.updateSelectedPage(page)
}

private const val SELECTED_MAIN_PAGE_KEY = "selected_main_page"
val selectedPage: StateFlow<Int> = savedStateHandle.getStateFlow(SELECTED_MAIN_PAGE_KEY, 0)
```

它保存的是已稳定的主页面索引，范围由 `MainPagerConfig.PAGE_COUNT = 4` 限制。

## 3. 页面切换实现

文件：[MainActivity.kt](https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/35467545b2826e3acfc88699755981a889956b1a/manager/app/src/main/java/com/sukisu/ultra/ui/MainActivity.kt)

入口与恢复值（[L121-L169](https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/35467545b2826e3acfc88699755981a889956b1a/manager/app/src/main/java/com/sukisu/ultra/ui/MainActivity.kt#L121-L169)）：`MainActivity` 收集 `selectedMainPage`，把它作为 `MainScreen(initialPage = ...)`，并把 `viewModel::setSelectedMainPage` 作为页面稳定回调。

主 Pager（[L241-L250](https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/35467545b2826e3acfc88699755981a889956b1a/manager/app/src/main/java/com/sukisu/ultra/ui/MainActivity.kt#L241-L250)）：

```kotlin
val pagerState = rememberPagerState(
    initialPage = initialPage,
    pageCount = { MainPagerConfig.PAGE_COUNT }
)
val mainPagerState = rememberMainPagerState(pagerState)
```

`HorizontalPager` 在 [L310-L327](https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/35467545b2826e3acfc88699755981a889956b1a/manager/app/src/main/java/com/sukisu/ultra/ui/MainActivity.kt#L310-L327) 将索引映射为四个页面：`0 Home`、`1 SuperUser`、`2 Module`、`3 Setting`。

底栏挂载（[L365-L389](https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/35467545b2826e3acfc88699755981a889956b1a/manager/app/src/main/java/com/sukisu/ultra/ui/MainActivity.kt#L365-L389)）使用 `Scaffold(bottomBar = bottomBar)`；宽屏条件和侧栏分支在 [L299-L364](https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/35467545b2826e3acfc88699755981a889956b1a/manager/app/src/main/java/com/sukisu/ultra/ui/MainActivity.kt#L299-L364)。

页面与状态同步（[L287-L295](https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/35467545b2826e3acfc88699755981a889956b1a/manager/app/src/main/java/com/sukisu/ultra/ui/MainActivity.kt#L287-L295)）：

```kotlin
LaunchedEffect(settledPage) {
    onPageChanged(settledPage)
}

LaunchedEffect(currentPage) {
    mainPagerState.syncPage()
}
```

## 4. 动画与选中指示器

### 4.1 底栏点击到页面的动画

`MainPagerState.animateToPage` 按目标页与当前页距离计算持续时间（`100 * distance + 100`），根据 Pager 页面尺寸换算滚动像素，并调用 `animateScrollBy` + `tween(EaseInOut)`，见 [BottomBar.kt:L38-L68](https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/35467545b2826e3acfc88699755981a889956b1a/manager/app/src/main/java/com/sukisu/ultra/ui/component/bottombar/BottomBar.kt#L38-L68)。

### 4.2 Miuix 浮动指示胶囊

`FloatingBottomBar` 在 [L214-L275](https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/35467545b2826e3acfc88699755981a889956b1a/manager/app/src/main/java/com/sukisu/ultra/ui/component/FloatingBottomBar.kt#L214-L275) 创建 `DampedDragAnimation`：

- 初始值来自 `selectedIndex()`；
- 拖动结束时把目标值四舍五入到 Tab 索引，写入 `currentIndex`，并用 `spring(1f, 300f, 0.5f)` 把面板橡皮筋偏移归零；
- 外部 `selectedIndex` 变化时更新 `currentIndex`；
- `currentIndex` 变化时把动画值移到对应索引并回调 `onSelected(index)`。

指示器位置和形状在 [L391-L457](https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/35467545b2826e3acfc88699755981a889956b1a/manager/app/src/main/java/com/sukisu/ultra/ui/component/FloatingBottomBar.kt#L391-L457)：指示器 `Box` 的 `translationX` 是 `dampedDragAnimation.value * tabWidthPx`，宽度是单个 Tab 宽度；启用模糊时用 `drawBackdrop`/lens/highlight 绘制胶囊，否则使用 `accentColor.copy(alpha = 0.15f)` 的圆角背景。

底层拖拽/按压动画定义在 [DampedDragAnimation.kt:L19-L63](https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/35467545b2826e3acfc88699755981a889956b1a/manager/app/src/main/java/com/sukisu/ultra/ui/component/miuix/animation/DampedDragAnimation.kt#L19-L63)：位置、按压进度、X/Y 缩放和速度分别由 `Animatable` 保存，动画规格是 spring；拖动与释放处理见 [L65-L133](https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/35467545b2826e3acfc88699755981a889956b1a/manager/app/src/main/java/com/sukisu/ultra/ui/component/miuix/animation/DampedDragAnimation.kt#L65-L133)。

### 4.3 Material 与普通 Miuix 指示器

这两条路径没有在仓库内自绘底栏指示器：Material 把 `selected` 传给 `ShortNavigationBarItem`（[BottomBarMaterial.kt:L55-L78](https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/35467545b2826e3acfc88699755981a889956b1a/manager/app/src/main/java/com/sukisu/ultra/ui/component/bottombar/BottomBarMaterial.kt#L55-L78)），普通 Miuix 把 `selected` 传给 `NavigationBarItem`（[BottomBarMiuix.kt:L72-L82](https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/35467545b2826e3acfc88699755981a889956b1a/manager/app/src/main/java/com/sukisu/ultra/ui/component/bottombar/BottomBarMiuix.kt#L72-L82)）。具体指示器形态由相应 UI 库组件实现。

## 5. 复核要点

- 只在 `isManager && !Natives.requireNewKernel() && rootAvailable()` 成立时显示完整底栏，Material/Miuix 文件分别在 [BottomBarMaterial.kt:L36-L40](https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/35467545b2826e3acfc88699755981a889956b1a/manager/app/src/main/java/com/sukisu/ultra/ui/component/bottombar/BottomBarMaterial.kt#L36-L40) 和 [BottomBarMiuix.kt:L45-L54](https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/35467545b2826e3acfc88699755981a889956b1a/manager/app/src/main/java/com/sukisu/ultra/ui/component/bottombar/BottomBarMiuix.kt#L45-L54) 证明。
- 宽屏布局默认转为侧栏，但启用 Miuix 浮动底栏时保留底栏，条件见 [MainActivity.kt:L299-L305](https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/35467545b2826e3acfc88699755981a889956b1a/manager/app/src/main/java/com/sukisu/ultra/ui/MainActivity.kt#L299-L305)。侧栏仍读取同一个 `selectedPage`，Material/Miuix 证据分别见 [NavigationRailMaterial.kt:L94-L112](https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/35467545b2826e3acfc88699755981a889956b1a/manager/app/src/main/java/com/sukisu/ultra/ui/component/bottombar/NavigationRailMaterial.kt#L94-L112) 和 [NavigationRailMiuix.kt:L37-L46](https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/35467545b2826e3acfc88699755981a889956b1a/manager/app/src/main/java/com/sukisu/ultra/ui/component/bottombar/NavigationRailMiuix.kt#L37-L46)。
