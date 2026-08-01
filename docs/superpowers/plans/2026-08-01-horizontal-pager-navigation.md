# Horizontal Pager Navigation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a real `HorizontalPager` to the primary ClipFlow navigation while keeping `NavController`, parser state, secondary screens, and persisted tab order intact.

**Architecture:** Keep `ClipFlowNavHost` as the shell and keep History, Settings, and About as ordinary `NavHost` destinations. Primary destinations render one hoisted `PagerState` through a focused `MainPager` composable; route changes and settled pager changes synchronize through a shared primary-navigation helper. The bottom bar and pager both consume the same filtered primary-item list.

**Tech Stack:** Kotlin 2.3.10, Java/Kotlin 21, Compose BOM 2026.05.01, Compose Foundation Pager, Material 3, Miuix 0.9.2, Navigation Compose 2.7.7, JUnit 4.13.2, Gradle 9.6.0.

## Global Constraints

- Horizontal paging applies only to registered bottom-navigation pages.
- The initial primary pages are `home`, `douyin`, and `xiaohongshu`.
- `bottomBarOrder` remains the persisted source for primary page order.
- History, Settings, and About remain outside the pager.
- Parser and download ViewModels remain activity-scoped and own their existing state.
- Do not add a second navigation framework or change parser/download business logic.
- Use the repository cache `E:/clioipflow/.gradle-clipflow` and run Gradle tasks serially.

---

### Task 1: Add tested primary-navigation ordering helpers

**Files:**
- Modify: `app/src/main/java/com/qihe/clipflow/navigation/Screen.kt`
- Create: `app/src/test/java/com/qihe/clipflow/navigation/PrimaryNavigationTest.kt`

**Interfaces:**
- `orderedBottomNavItems(order: List<String>, registeredItems: List<BottomNavItem> = bottomNavItems): List<BottomNavItem>` filters unknown and duplicate keys, appends registered items missing from an older order, and falls back to all registered items when no configured key is valid.
- `primaryPageIndex(route: String?, items: List<BottomNavItem>): Int` returns the matching index or `0` for an unknown route.
- `NavHostController.navigateToPrimary(route: String)` performs tab-style navigation with `popUpTo(findStartDestination()) { saveState = true }`, `launchSingleTop = true`, and `restoreState = true`.

- [ ] **Step 1: Write the failing helper tests.**

```kotlin
package com.qihe.clipflow.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class PrimaryNavigationTest {
    @Test
    fun orderedItemsFiltersUnknownAndDuplicateKeys() {
        val routes = orderedBottomNavItems(
            listOf("xiaohongshu", "missing", "xiaohongshu", "home")
        ).map { it.route }

        assertEquals(listOf("xiaohongshu", "home", "douyin"), routes)
    }

    @Test
    fun orderedItemsFallsBackWhenConfigurationHasNoKnownKeys() {
        val routes = orderedBottomNavItems(listOf("missing")).map { it.route }

        assertEquals(bottomNavItems.map { it.route }, routes)
    }

    @Test
    fun primaryPageIndexUsesZeroForUnknownRoutes() {
        assertEquals(0, primaryPageIndex("missing", bottomNavItems))
        assertEquals(1, primaryPageIndex("douyin", bottomNavItems))
    }
}
```

- [ ] **Step 2: Run the focused test to verify it fails because the helpers do not exist.**

Run: `.\gradlew.bat -g E:/clioipflow/.gradle-clipflow --offline --no-daemon --no-watch-fs --console=plain :app:testDebugUnitTest --tests com.qihe.clipflow.navigation.PrimaryNavigationTest`

Expected: compilation failure naming the missing helper functions.

- [ ] **Step 3: Implement the minimal helpers in `Screen.kt`.**

```kotlin
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination

fun orderedBottomNavItems(
    order: List<String>,
    registeredItems: List<BottomNavItem> = bottomNavItems,
): List<BottomNavItem> {
    val distinctRegistered = registeredItems.distinctBy { it.route }
    val configured = order.mapNotNull { key ->
        distinctRegistered.find { it.route == key }
    }.distinctBy { it.route }
    val configuredRoutes = configured.map { it.route }.toSet()
    val missing = distinctRegistered.filterNot { it.route in configuredRoutes }
    return (configured + missing).ifEmpty { distinctRegistered }
}

fun primaryPageIndex(route: String?, items: List<BottomNavItem>): Int {
    return items.indexOfFirst { it.route == route }.coerceAtLeast(0)
}

fun NavHostController.navigateToPrimary(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
```

- [ ] **Step 4: Run the focused test and confirm all three cases pass.**

Run: `.\gradlew.bat -g E:/clioipflow/.gradle-clipflow --offline --no-daemon --no-watch-fs --console=plain :app:testDebugUnitTest --tests com.qihe.clipflow.navigation.PrimaryNavigationTest`

Expected: `3 tests completed, 0 failed`.

- [ ] **Step 5: Commit the helper and tests.**

```bash
git add app/src/main/java/com/qihe/clipflow/navigation/Screen.kt app/src/test/java/com/qihe/clipflow/navigation/PrimaryNavigationTest.kt
git commit -m "test: cover primary navigation ordering"
```

### Task 2: Implement the saveable HorizontalPager host

**Files:**
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/com/qihe/clipflow/navigation/MainPager.kt`

**Interfaces:**
- `MainPager(navController: NavHostController, pages: List<BottomNavItem>, pagerState: PagerState, stateHolder: SaveableStateHolder, currentRoute: String?, sourceUrl: String?)` renders the registered primary pages and forwards `sourceUrl` only to the currently active parser page.

- [ ] **Step 1: Add the explicit Compose Foundation dependency.**

Add this dependency inside the existing Compose dependency block in
`app/build.gradle.kts`:

```kotlin
implementation("androidx.compose.foundation:foundation")
```

- [ ] **Step 2: Create `MainPager` with stable route keys and per-route saveable state.**

```kotlin
@Composable
fun MainPager(
    navController: NavHostController,
    pages: List<BottomNavItem>,
    pagerState: PagerState,
    stateHolder: SaveableStateHolder,
    currentRoute: String?,
    sourceUrl: String?,
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 1,
        key = { page -> pages[page].route },
    ) { page ->
        val route = pages[page].route
        stateHolder.SaveableStateProvider(route) {
            when (route) {
                Screen.Home.route -> HomeScreen(navController)
                Screen.Douyin.route -> DouyinScreen(
                    sourceUrl = sourceUrl.takeIf { currentRoute == route },
                )
                Screen.Xiaohongshu.route -> XiaohongshuScreen(
                    sourceUrl = sourceUrl.takeIf { currentRoute == route },
                )
            }
        }
    }
}
```

The `when` remains the single registration point for page content; adding a
new primary page adds one branch next to its existing `Screen` and
`BottomNavItem` registration.

- [ ] **Step 3: Compile the new file and run the focused navigation test.**

Run: `.\gradlew.bat -g E:/clioipflow/.gradle-clipflow --offline --no-daemon --no-watch-fs --console=plain :app:testDebugUnitTest --tests com.qihe.clipflow.navigation.PrimaryNavigationTest`

Expected: the test task reaches the existing source compilation without an
unresolved `HorizontalPager`, `PagerState`, or `SaveableStateHolder` symbol.

- [ ] **Step 4: Commit the pager host.**

```bash
git add app/build.gradle.kts app/src/main/java/com/qihe/clipflow/navigation/MainPager.kt
git commit -m "feat: add saveable primary navigation pager"
```

### Task 3: Synchronize pager, routes, and bottom navigation

**Files:**
- Modify: `app/src/main/java/com/qihe/clipflow/navigation/ClipFlowNavHost.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/navigation/NavigationChrome.kt`

**Interfaces:**
- `ClipFlowNavHost` owns `primaryItems`, `primaryRoutes`, `PagerState`, and `SaveableStateHolder`.
- Route-to-pager synchronization calls `pagerState.animateScrollToPage(primaryPageIndex(...))` only when the target differs from the settled page.
- Pager-to-route synchronization observes `pagerState.settledPage` and calls `navController.navigateToPrimary(...)` only while the current route is a primary route.

- [ ] **Step 1: Replace the fixed `bottomBarRoutes` list with the shared ordered items.**

Use this state near the existing `bottomBarOrder` flow:

```kotlin
val primaryItems = remember(bottomBarOrder) {
    orderedBottomNavItems(bottomBarOrder)
}
val primaryRoutes = remember(primaryItems) { primaryItems.map { it.route } }
val showBottomBar = currentRoute in primaryRoutes
val initialPrimaryPage = primaryPageIndex(currentRoute ?: defaultPage, primaryItems)
val pagerState = rememberPagerState(
    initialPage = initialPrimaryPage,
    pageCount = { primaryItems.size },
)
val pagerStateHolder = rememberSaveableStateHolder()
```

Use `primaryRoutes.firstOrNull { it == defaultPage } ?: primaryRoutes.first()`
as the `NavHost` start destination so a persisted invalid default cannot point
to a page absent from the pager.

- [ ] **Step 2: Add route-to-pager and pager-to-route effects.**

```kotlin
LaunchedEffect(currentRoute, primaryRoutes) {
    val targetPage = primaryRoutes.indexOf(currentRoute)
    if (targetPage >= 0 && pagerState.currentPage != targetPage) {
        pagerState.animateScrollToPage(targetPage)
    }
}

LaunchedEffect(pagerState, primaryRoutes, currentRoute) {
    snapshotFlow { pagerState.settledPage }
        .drop(1)
        .distinctUntilChanged()
        .collectLatest { page ->
            val route = primaryRoutes.getOrNull(page)
            if (route != null && currentRoute != null && currentRoute in primaryRoutes && currentRoute != route) {
                navController.navigateToPrimary(route)
            }
        }
}
```

Import `HorizontalPager` state APIs, `rememberSaveableStateHolder`,
`distinctUntilChanged`, and `collectLatest` without changing any ViewModel or
parser state collection.

- [ ] **Step 3: Pass the current parser argument into the pager.**

Derive the current route argument from the active back-stack entry:

```kotlin
val currentSourceUrl = when (currentRoute) {
    Screen.Douyin.route -> navBackStackEntry?.arguments?.getString(Screen.Douyin.sourceUrlArgument)
    Screen.Xiaohongshu.route -> navBackStackEntry?.arguments?.getString(Screen.Xiaohongshu.sourceUrlArgument)
    else -> null
}
```

Render `MainPager(...)` from the Home, Douyin, and Xiaohongshu `NavHost`
destinations, leaving the existing route argument declarations in place.

- [ ] **Step 4: Disable duplicate transitions for primary destinations.**

In all four `NavHost` transition lambdas, return `EnterTransition.None` when
the target route is primary and `ExitTransition.None` when the source route is
primary. Keep the existing fade/slide implementation for transitions whose
destination is History, Settings, or About. Normalize routes with
`substringBefore("?")` before checking membership.

- [ ] **Step 5: Make the floating bottom bar use the shared item helper and navigation action.**

Replace its local `mapNotNull` expression with
`orderedBottomNavItems(bottomBarOrder)`, and replace the local `navigate` body
with `navController.navigateToPrimary(item.route)`. Keep the existing visual
renderer, labels, backdrop, and selected-index calculation unchanged.

- [ ] **Step 6: Run unit tests and a debug build.**

Run: `.\gradlew.bat -g E:/clioipflow/.gradle-clipflow --offline --no-daemon --no-watch-fs --console=plain :app:testDebugUnitTest`

Expected: all tests pass and the new navigation code compiles.

Run: `.\gradlew.bat -g E:/clioipflow/.gradle-clipflow --offline --no-daemon --no-watch-fs --console=plain :app:assembleDebug`

Expected: `BUILD SUCCESSFUL` and a debug APK under `app/build/outputs/apk/debug/`.

- [ ] **Step 7: Commit the synchronized navigation shell.**

```bash
git add app/src/main/java/com/qihe/clipflow/navigation/ClipFlowNavHost.kt app/src/main/java/com/qihe/clipflow/navigation/NavigationChrome.kt
git commit -m "feat: sync horizontal pager with primary navigation"
```

### Task 4: Release verification and regression checks

**Files:**
- No source changes expected.

- [ ] **Step 1: Check formatting and repository status.**

Run: `git diff --check`

Expected: no output and exit code 0.

- [ ] **Step 2: Build the release APK serially.**

Run: `.\gradlew.bat -g E:/clioipflow/.gradle-clipflow --offline --no-daemon --no-watch-fs --console=plain :app:assembleRelease`

Expected: `BUILD SUCCESSFUL` and
`app/build/outputs/apk/release/app-release.apk` exists.

- [ ] **Step 3: Inspect the final diff for scope.**

Run: `git status --short` and `git log -5 --oneline`

Expected: only the design, plan, pager dependency, navigation code, and focused
tests are present; parser, downloader, wallpaper, privacy, and settings
business files are unchanged.

- [ ] **Step 4: Report manual acceptance limits.**

If no Android device or emulator is available, report build/test verification
as complete but state that physical left/right swipe acceptance was not run.
