# Live Pager Bottom Bar Progress Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the liquid bottom-navigation indicator follow the main `HorizontalPager` continuously while preserving settled-page route navigation and existing bottom-bar interactions.

**Architecture:** `ClipFlowNavHost` passes its shared `PagerState` into the navigation chrome. A pure navigation helper converts `currentPage` plus `currentPageOffsetFraction` into a clamped fractional tab position. The liquid bottom bar keeps its integer route selection for callbacks, but temporarily renders the external fractional position while the pager is scrolling.

**Tech Stack:** Kotlin 2.3.10, Jetpack Compose Foundation Pager, Compose Material 3, Miuix blur components, JUnit 4, Gradle 9.6.0, JDK 21.

## Global Constraints

- Primary page order remains derived from the registered bottom-navigation items and persisted `bottomBarOrder`.
- `settledPage` remains the only pager signal that triggers `NavController` navigation.
- History, Settings, and About remain outside the pager and keep the existing bottom-bar visibility behavior.
- Bottom-bar clicks and bottom-bar drag selection continue to call the existing `navigateToPrimary` path.
- Use `E:/clioipflow/.gradle-clipflow` and run Gradle tasks serially with `--offline --no-daemon --no-watch-fs --console=plain`.

---

### Task 1: Add and test the fractional pager-position helper

**Files:**
- Modify: `app/src/main/java/com/qihe/clipflow/navigation/Screen.kt`
- Test: `app/src/test/java/com/qihe/clipflow/navigation/PrimaryNavigationTest.kt`

**Interfaces:**
- Produces `pagerIndicatorPosition(currentPage: Int, currentPageOffsetFraction: Float, pageCount: Int): Float`.
- Returns a valid fractional position in `0f..(pageCount - 1).toFloat()`, or `0f` when `pageCount <= 1`.

- [x] **Step 1: Write failing tests for forward, reverse, and boundary progress.**

Add these tests to `PrimaryNavigationTest`:

```kotlin
@Test
fun pagerIndicatorPositionFollowsFractionalPageOffset() {
    assertEquals(1.25f, pagerIndicatorPosition(1, 0.25f, 3), 0.0001f)
    assertEquals(0.6f, pagerIndicatorPosition(1, -0.4f, 3), 0.0001f)
}

@Test
fun pagerIndicatorPositionClampsToRegisteredPageBounds() {
    assertEquals(0f, pagerIndicatorPosition(0, -0.4f, 3), 0.0001f)
    assertEquals(2f, pagerIndicatorPosition(2, 0.4f, 3), 0.0001f)
    assertEquals(0f, pagerIndicatorPosition(0, 0.4f, 1), 0.0001f)
}
```

- [x] **Step 2: Run the focused test and confirm it fails because the helper is absent.**

Run:

```powershell
& 'C:\Users\VOS-User\.gradle\wrapper\dists\gradle-9.6.0-bin\42k10rwplmzkhuboz9kdazi7s\gradle-9.6.0\bin\gradle.bat' -g E:/clioipflow/.gradle-clipflow --offline --no-daemon --no-watch-fs --console=plain :app:testDebugUnitTest --tests com.qihe.clipflow.navigation.PrimaryNavigationTest
```

Expected: compilation fails because `pagerIndicatorPosition` is not defined.

- [x] **Step 3: Implement the minimal clamped helper in `Screen.kt`.**

Add:

```kotlin
fun pagerIndicatorPosition(
    currentPage: Int,
    currentPageOffsetFraction: Float,
    pageCount: Int,
): Float {
    if (pageCount <= 1) return 0f
    return (currentPage + currentPageOffsetFraction)
        .coerceIn(0f, (pageCount - 1).toFloat())
}
```

- [x] **Step 4: Run the focused test and verify all navigation tests pass.**

Use the same Gradle command from Step 2. Expected: all `PrimaryNavigationTest` tests pass with zero failures.

- [x] **Step 5: Commit the helper and tests.**

```powershell
git -c safe.directory=E:/clioipflow/ClipFlow add app/src/main/java/com/qihe/clipflow/navigation/Screen.kt app/src/test/java/com/qihe/clipflow/navigation/PrimaryNavigationTest.kt
git -c safe.directory=E:/clioipflow/ClipFlow commit -m "test: cover live pager indicator progress"
```

### Task 2: Pass pager progress through the navigation chrome

**Files:**
- Modify: `app/src/main/java/com/qihe/clipflow/navigation/ClipFlowNavHost.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/navigation/NavigationChrome.kt`

**Interfaces:**
- `NavigationChrome.FloatingBottomBar` receives the existing `PagerState` in addition to its route and visual dependencies.
- It passes `indicatorPosition = { pagerIndicatorPosition(...) }` and `indicatorPositionActive = { pagerState.isScrollInProgress }` to the UI component.

- [x] **Step 1: Add the `PagerState` parameter at the call site.**

Update the `FloatingBottomBar` call in `ClipFlowNavHost`:

```kotlin
FloatingBottomBar(
    navController = navController,
    currentDestination = currentDestination,
    prefs = prefs,
    backdrop = backdrop,
    pagerState = pagerState,
    modifier = Modifier.align(Alignment.BottomCenter),
)
```

- [x] **Step 2: Derive the live progress in `NavigationChrome.kt`.**

Import `androidx.compose.foundation.pager.PagerState`, add `pagerState: PagerState` to the chrome function, and pass these values to the liquid component:

```kotlin
LiquidFloatingBottomBar(
    // existing arguments
    indicatorPosition = {
        pagerIndicatorPosition(
            currentPage = pagerState.currentPage,
            currentPageOffsetFraction = pagerState.currentPageOffsetFraction,
            pageCount = items.size,
        )
    },
    indicatorPositionActive = { pagerState.isScrollInProgress },
) {
    // existing tab content
}
```

- [x] **Step 3: Compile the navigation changes with the focused unit test.**

Run the Task 1 focused Gradle command. Expected: the navigation source compiles; the test suite remains green after the new parameter wiring.

- [x] **Step 4: Commit the navigation-state wiring.**

```powershell
git -c safe.directory=E:/clioipflow/ClipFlow add app/src/main/java/com/qihe/clipflow/navigation/ClipFlowNavHost.kt app/src/main/java/com/qihe/clipflow/navigation/NavigationChrome.kt
git -c safe.directory=E:/clioipflow/ClipFlow commit -m "feat: expose pager progress to bottom navigation"
```

### Task 3: Render the external fractional indicator without changing callbacks

**Files:**
- Modify: `app/src/main/java/com/qihe/clipflow/ui/component/FloatingBottomBar.kt`

**Interfaces:**
- Add optional `indicatorPosition: (() -> Float)? = null` and `indicatorPositionActive: (() -> Boolean)? = null` parameters to the liquid bottom-bar composable.
- Existing `selectedIndex: () -> Int`, `onSelected`, tab count, and item content remain unchanged.

- [x] **Step 1: Add a clamped visual-position helper inside the component.**

Use this function so every visual consumer uses the same value:

```kotlin
private fun indicatorValue(
    dampedValue: Float,
    externalPosition: (() -> Float)?,
    externalActive: (() -> Boolean)?,
    tabsCount: Int,
): Float {
    val value = if (externalPosition != null && externalActive?.invoke() == true) {
        externalPosition()
    } else {
        dampedValue
    }
    return value.coerceIn(0f, (tabsCount - 1).coerceAtLeast(0).toFloat())
}
```

- [x] **Step 2: Use the resolved value for highlight and indicator translation.**

Replace the direct `dampedDragAnimation.value` reads in the interactive highlight position and both indicator `graphicsLayer` translation blocks with `indicatorValue(...)`. Keep `dampedDragAnimation.value` for `canDrag`, velocity, press scale, and existing bottom-bar drag callbacks so the bottom bar remains interactive.

- [x] **Step 3: Run the focused unit test and compile the component.**

Run:

```powershell
& 'C:\Users\VOS-User\.gradle\wrapper\dists\gradle-9.6.0-bin\42k10rwplmzkhuboz9kdazi7s\gradle-9.6.0\bin\gradle.bat' -g E:/clioipflow/.gradle-clipflow --offline --no-daemon --no-watch-fs --console=plain :app:testDebugUnitTest --tests com.qihe.clipflow.navigation.PrimaryNavigationTest
```

Expected: Kotlin compilation succeeds and all focused tests pass.

- [x] **Step 4: Commit the visual synchronization change.**

```powershell
git -c safe.directory=E:/clioipflow/ClipFlow add app/src/main/java/com/qihe/clipflow/ui/component/FloatingBottomBar.kt
git -c safe.directory=E:/clioipflow/ClipFlow commit -m "feat: sync bottom indicator with pager drag"
```

### Task 4: Full verification and manual acceptance checklist

**Files:**
- No source changes expected.

- [x] **Step 1: Run all debug unit tests and build the debug APK.**

```powershell
& 'C:\Users\VOS-User\.gradle\wrapper\dists\gradle-9.6.0-bin\42k10rwplmzkhuboz9kdazi7s\gradle-9.6.0\bin\gradle.bat' -g E:/clioipflow/.gradle-clipflow --offline --no-daemon --no-watch-fs --console=plain :app:testDebugUnitTest :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`, zero test failures, and `app/build/outputs/apk/debug/app-debug.apk` exists.

- [x] **Step 2: Run repository checks.**

```powershell
git -c safe.directory=E:/clioipflow/ClipFlow diff --check
git -c safe.directory=E:/clioipflow/ClipFlow status --short --branch
```

Expected: no whitespace errors; only the intended commits or clean working-tree state remain.

- [ ] **Step 3: Perform manual gesture acceptance when a device is connected.**

Verify all of the following:

- Drag Home toward Douyin and back; the indicator tracks continuously in both directions.
- Drag Douyin toward Xiaohongshu and cancel; the indicator returns to Douyin.
- Drag at the first and last pages; the indicator clamps instead of leaving the bar.
- Tap a bottom tab and drag the bottom-bar indicator; existing navigation still works.
- Open History, Settings, and About; the bottom bar remains hidden.

- [ ] **Step 4: Report verification scope.**

Report build/test results and whether a physical device or emulator was available for gesture acceptance.
