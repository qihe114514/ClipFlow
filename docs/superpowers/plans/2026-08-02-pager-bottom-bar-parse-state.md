# 页面滑动、底栏与解析状态实施计划

> 给执行代理的要求：实施时按任务逐项执行，每个步骤使用复选框跟踪，并在每个任务结束后独立验证。

目标：让主页面 pager 和底栏导航立即响应，同时保留解析状态并阻止清除后的结果被重新解析。

架构：PagerState 负责主页面的视觉位置。底栏点击和拖拽都调用 PagerState.animateScrollToPage；页面 settled 后再同步主 NavController 路由。自定义底栏只保留自身拖拽所需的阻尼位置，pager 交接不再触发按压/释放动画。解析 ViewModel 保持现有生命周期，对外部 sourceUrl 只消费一次，并拒绝过期解析请求写回状态。

技术栈：Kotlin 2.3.10、Jetpack Compose Foundation Pager、Compose runtime、AndroidX ViewModel、Kotlin 协程、JUnit 4、Gradle 8.4。

## 全局约束

- 保留 Home、Douyin、Xiaohongshu、History、Settings、About、下载和解析接口行为。
- 普通页面切换保留解析 ViewModel 状态。
- 清除重置解析输入、结果、加载态、错误和结果元数据，但保留下载会话状态。
- 不增加依赖，不引入新的导航框架。
- 使用仓库内 Gradle 8.4 分发版和离线模式验证。

---

### 任务 1：固定指示器状态契约

文件：

- 修改：app/src/test/java/com/qihe/clipflow/ui/component/FloatingBottomBarTest.kt
- 修改：app/src/main/java/com/qihe/clipflow/ui/component/FloatingBottomBar.kt
- 检查：app/src/test/java/com/qihe/clipflow/navigation/PrimaryNavigationTest.kt

接口：

- indicatorValue(dampedValue, externalPosition, externalActive, tabsCount) 仅在 externalActive 为 true 时使用外部 pager 位置。
- pagerIndicatorPosition 和 pagerIndicatorPositionActive 保持现有纯函数签名。

- [ ] 步骤 1：替换旧 handoff 测试。

在 FloatingBottomBarTest.kt 中使用：

    @Test
    fun indicatorUsesPagerPositionOnlyDuringPagerTransition() {
        assertEquals(1f, indicatorValue(0f, { 1f }, { true }, tabsCount = 3), 0.0001f)
        assertEquals(0f, indicatorValue(0f, { 1f }, { false }, tabsCount = 3), 0.0001f)
    }

- [ ] 步骤 2：先运行聚焦测试。

    .\gradle-dist\gradle-8.4\bin\gradle.bat --offline --no-daemon --no-watch-fs --console=plain :app:testDebugUnitTest --tests com.qihe.clipflow.ui.component.FloatingBottomBarTest --tests com.qihe.clipflow.navigation.PrimaryNavigationTest

预期：新测试先失败，因为当前 indicatorValue 仍接受 handoff 状态。

- [ ] 步骤 3：删除 handoff 状态。

从 indicatorValue 和三个调用点删除 handoffActive。实现保持为：

    val value = if (externalPosition != null && externalActive?.invoke() == true) {
        externalPosition()
    } else {
        dampedValue
    }

- [ ] 步骤 4：再次运行步骤 2 的命令。

预期：两个聚焦测试类通过。

- [ ] 步骤 5：提交任务 1。

    git add app/src/main/java/com/qihe/clipflow/ui/component/FloatingBottomBar.kt app/src/test/java/com/qihe/clipflow/ui/component/FloatingBottomBarTest.kt
    git commit -m "fix: simplify pager indicator handoff"

### 任务 2：让 PagerState 驱动导航

文件：

- 修改：app/src/main/java/com/qihe/clipflow/navigation/NavigationChrome.kt
- 修改：app/src/main/java/com/qihe/clipflow/navigation/ClipFlowNavHost.kt
- 修改：app/src/main/java/com/qihe/clipflow/ui/component/FloatingBottomBar.kt
- 修改：app/src/main/java/com/qihe/clipflow/ui/component/miuix/animation/DampedDragAnimation.kt

接口：

- NavigationChrome 从 pagerState.currentPage 得到底栏选中项。
- 底栏点击和拖拽选择都调用 pagerState.animateScrollToPage(index)。
- ClipFlowNavHost 只从 pagerState.settledPage 同步 NavController；外部或次级导航仍保留路由到 pager 的同步。

- [ ] 步骤 1：删除 pager handoff 收集器。

从 FloatingBottomBar.kt 删除 indicatorHandoffActive、indicatorPositionState、indicatorActiveState，以及调用 press、跟踪外部位置、调用 release 的 LaunchedEffect。保留以下选中索引收集器：

    LaunchedEffect(dampedDragAnimation) {
        snapshotFlow { selectedIndex() }
            .distinctUntilChanged()
            .collectLatest { index ->
                dampedDragAnimation.snapToValue(index.toFloat())
            }
    }

该收集器不得调用 press、release 或 animateToValue。

- [ ] 步骤 2：让释放动作立即执行。

在 DampedDragAnimation.kt 中，将 release 替换为：

    fun release() {
        pressAnimationJob?.cancel()
        pressAnimationJob = animationScope.launch {
            launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(initialScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(initialScale, scaleYAnimationSpec) }
        }
    }

给 animateToValue 增加 animatePress: Boolean = true 参数，仅在参数为 true 时调用 press 和 release。底栏 onDragStopped 使用 animateToValue(targetIndex.toFloat(), animatePress = false)。同时删除因此变成无效的 awaitFrame、snapshotFlow、filter、first 和 abs 导入。

- [ ] 步骤 3：让底栏选择直接驱动 pager。

在 NavigationChrome.kt 中增加协程作用域和选择函数：

    val scope = rememberCoroutineScope()
    val selectPage: (Int) -> Unit = { index ->
        if (index in items.indices) {
            scope.launch { pagerState.animateScrollToPage(index) }
        }
    }

传入 onSelected = selectPage，从 pagerState.currentPage 得到 selectedIndex，使用 forEachIndexed 让每个底栏项调用 selectPage(index)。删除通过 NavDestination.hierarchy 计算选中的逻辑。

- [ ] 步骤 4：保持 settled 页面路由同步。

ClipFlowNavHost.kt 保留现有 settledPage 收集器。路由到 pager 的效果使用：

    if (targetPage >= 0 && pagerState.settledPage != targetPage) {
        pagerState.animateScrollToPage(targetPage)
    }

不要让该效果观察 pagerState.currentPage。这样底栏点击会先改变 pager，等过渡完成后再改变旧路由。

- [ ] 步骤 5：运行导航测试和 debug 构建。

    .\gradle-dist\gradle-8.4\bin\gradle.bat --offline --no-daemon --no-watch-fs --console=plain :app:testDebugUnitTest :app:assembleDebug

预期：现有单元测试通过，debug 构建成功。

- [ ] 步骤 6：提交任务 2。

    git add app/src/main/java/com/qihe/clipflow/navigation/ClipFlowNavHost.kt app/src/main/java/com/qihe/clipflow/navigation/NavigationChrome.kt app/src/main/java/com/qihe/clipflow/ui/component/FloatingBottomBar.kt app/src/main/java/com/qihe/clipflow/ui/component/miuix/animation/DampedDragAnimation.kt
    git commit -m "fix: drive primary navigation from pager state"

### 任务 3：正确保留和清除解析状态

文件：

- 修改：app/src/main/java/com/qihe/clipflow/ui/parser/PlatformParseViewModel.kt
- 修改：app/src/main/java/com/qihe/clipflow/ui/douyin/DouyinScreen.kt
- 修改：app/src/main/java/com/qihe/clipflow/ui/xiaohongshu/XiaohongshuScreen.kt
- 新建：app/src/test/java/com/qihe/clipflow/ui/parser/OneShotSourceUrlTest.kt

接口：

- OneShotSourceUrl.consume(sourceUrl: String?): String? 只返回一次非空参数；空值和重复值返回 null。
- PlatformParseViewModel.consumeSourceUrl(sourceUrl: String?): String? 对外提供门闩。
- PlatformParseViewModel 管理一个活动解析 Job 和递增请求编号。清除时递增编号并取消任务，过期响应不能重新写入 UI。

- [ ] 步骤 1：新增一次性参数失败测试。

新建测试文件：

    class OneShotSourceUrlTest {
        @Test
        fun blankSourceIsIgnored() {
            val gate = OneShotSourceUrl()
            assertNull(gate.consume("  "))
        }

        @Test
        fun sameSourceIsConsumedOnlyOnceAcrossCompositionReentry() {
            val gate = OneShotSourceUrl()
            assertEquals("https://example.test/video", gate.consume("https://example.test/video"))
            assertNull(gate.consume(null))
            assertNull(gate.consume("https://example.test/video"))
        }

        @Test
        fun differentSourceCanBeConsumed() {
            val gate = OneShotSourceUrl()
            gate.consume("https://example.test/one")
            assertEquals("https://example.test/two", gate.consume("https://example.test/two"))
        }
    }

预期：实现前因 OneShotSourceUrl 不存在而编译失败。

- [ ] 步骤 2：实现参数门闩和请求取消。

在解析包中新增 internal 的 OneShotSourceUrl。consume 需要忽略空白值，返回新的非空值，并在值等于上次消费值时返回 null。

在 PlatformParseViewModel 增加门闩、parseJob 和 parseRequestId。增加 consumeSourceUrl。clearUrl 递增请求编号、取消当前 Job，再创建只保留下载会话字段的 ParsePageUiState。

- [ ] 步骤 3：让 parse 拒绝过期响应。

parse 开始时递增请求编号并取消旧 Job，保存本次编号。parseSupport.parse(sourceUrl) 返回后，在任何 UI 更新或 saveHistory 前检查：

    if (requestId != parseRequestId) return@launch

保留现有成功和失败状态字段映射不变。

- [ ] 步骤 4：两个解析页面只通过门闩消费参数。

将两个页面原来的 sourceUrl?.let 替换为：

    LaunchedEffect(sourceUrl) {
        viewModel.consumeSourceUrl(sourceUrl)?.let { url ->
            viewModel.onUrlChange(url)
            viewModel.parse()
        }
    }

不得在页面进入效果中重置解析状态。

- [ ] 步骤 5：运行解析测试和 debug 构建。

    .\gradle-dist\gradle-8.4\bin\gradle.bat --offline --no-daemon --no-watch-fs --console=plain :app:testDebugUnitTest --tests com.qihe.clipflow.ui.parser.OneShotSourceUrlTest :app:assembleDebug

预期：三个参数门闩测试通过，debug APK 构建成功。

- [ ] 步骤 6：提交任务 3。

    git add app/src/main/java/com/qihe/clipflow/ui/parser/PlatformParseViewModel.kt app/src/main/java/com/qihe/clipflow/ui/douyin/DouyinScreen.kt app/src/main/java/com/qihe/clipflow/ui/xiaohongshu/XiaohongshuScreen.kt app/src/test/java/com/qihe/clipflow/ui/parser/OneShotSourceUrlTest.kt
    git commit -m "fix: preserve cleared parser state across navigation"

### 任务 4：最终验证

文件：

- 仅检查：任务 1 到 3 修改的文件

- [ ] 步骤 1：运行全部 JVM 测试和 release 构建。

    .\gradle-dist\gradle-8.4\bin\gradle.bat --offline --no-daemon --no-watch-fs --console=plain :app:testDebugUnitTest :app:assembleRelease

预期：所有单元测试通过，release 构建成功。

- [ ] 步骤 2：检查空白和最终范围。

    git diff --check HEAD~3..HEAD
    git status --short --branch
    git diff --stat HEAD~3..HEAD

预期：没有空白错误，只包含本次任务文件，没有被 Git 跟踪的构建产物。

