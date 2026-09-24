# Predictive Back and Secondary Layer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Keep secondary pages opaque over the primary pager and enable Android predictive back for History, Settings, About, and Open Source.

**Architecture:** Keep `MainPager` mounted so its `PagerState` always has a layout, but pass an explicit visibility flag that sets its alpha to `0f` and disables pager gestures on secondary routes. Register one `PredictiveBackHandler` at the navigation-shell level; completed gestures call `NavHostController.popBackStack()`, while cancelled gestures leave navigation unchanged.

**Tech Stack:** Kotlin 2.3.10, Jetpack Compose, `androidx.activity:activity-compose:1.9.0`, Navigation Compose 2.7.7, JUnit 4.13.2, Gradle 9.6.0.

## Global Constraints

- Secondary routes are exactly `history`, `settings`, `about`, and `open-source`.
- Primary parser pages remain ordinary primary destinations and are not handled by the predictive-back callback.
- Do not upgrade Navigation Compose or add another navigation framework.
- Keep parser ViewModels, history persistence, downloads, and pager state ownership unchanged.
- `MainPager` must remain mounted for layout stability, but must be fully transparent and non-scrollable on secondary routes.
- Before every APK build, increment `versionCode` by exactly one and keep `versionName` unchanged.
- Preserve the existing unrelated untracked file `docs/superpowers/plans/2026-08-05-liquid-buttons.md`.

---

### Task 1: Add a tested secondary-route predicate

**Files:**
- Modify: `app/src/main/java/com/qihe/clipflow/navigation/Screen.kt`
- Test: `app/src/test/java/com/qihe/clipflow/navigation/PrimaryNavigationTest.kt`

**Interfaces:**
- Produce `secondaryRoutes: Set<String>` containing `history`, `settings`, `about`, and `open-source`.
- Produce `isSecondaryRoute(route: String?): Boolean`, returning true only for those four routes.

- [ ] **Step 1: Add failing tests for the exact route boundary.**

Append to `PrimaryNavigationTest`:

```kotlin
@Test
fun secondaryRoutePredicateCoversOnlyDetailPages() {
    assertTrue(isSecondaryRoute(Screen.History.route))
    assertTrue(isSecondaryRoute(Screen.Settings.route))
    assertTrue(isSecondaryRoute(Screen.About.route))
    assertTrue(isSecondaryRoute(Screen.OpenSource.route))
    assertFalse(isSecondaryRoute(Screen.Home.route))
    assertFalse(isSecondaryRoute(Screen.Douyin.route))
    assertFalse(isSecondaryRoute(Screen.Bilibili.route))
}
```

Add `assertFalse` and `assertTrue` imports alongside the existing `assertEquals` import.

- [ ] **Step 2: Run the focused test to verify it fails.**

Run:

```text
E:/clioipflow/.gradle-clipflow/wrapper/dists/gradle-9.6.0-bin/42k10rwplmzkhuboz9kdazi7s/gradle-9.6.0/bin/gradle.bat --offline --no-daemon --no-watch-fs --console=plain -g E:/clioipflow/.gradle-clipflow :app:testDebugUnitTest --tests com.qihe.clipflow.navigation.PrimaryNavigationTest
```

Expected: compilation failure because `isSecondaryRoute` is not defined.

- [ ] **Step 3: Implement the smallest route predicate in `Screen.kt`.**

Add after `bottomNavItems`:

```kotlin
val secondaryRoutes = setOf(
    Screen.History.route,
    Screen.Settings.route,
    Screen.About.route,
    Screen.OpenSource.route,
)

fun isSecondaryRoute(route: String?): Boolean = route in secondaryRoutes
```

- [ ] **Step 4: Run the focused test to verify it passes.**

Run the same focused Gradle command. Expected: all `PrimaryNavigationTest` cases pass with zero failures.

- [ ] **Step 5: Commit only the helper and its tests.**

```text
git -c safe.directory=E:/clioipflow/ClipFlow add app/src/main/java/com/qihe/clipflow/navigation/Screen.kt app/src/test/java/com/qihe/clipflow/navigation/PrimaryNavigationTest.kt
git -c safe.directory=E:/clioipflow/ClipFlow commit -m "test: define secondary navigation routes"
```

### Task 2: Isolate the mounted primary pager from secondary pages

**Files:**
- Modify: `app/src/main/java/com/qihe/clipflow/navigation/MainPager.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/navigation/ClipFlowNavHost.kt`

**Interfaces:**
- Change `MainPager` to accept `visible: Boolean`.
- `ClipFlowNavHost` passes `visible = currentRoute in primaryRoutes`.
- `MainPager` uses the same `visible` value for drawing and `HorizontalPager.userScrollEnabled`.

- [ ] **Step 1: Add the visibility parameter without changing page content.**

Change the `MainPager` signature to:

```kotlin
fun MainPager(
    navController: NavHostController,
    pages: List<BottomNavItem>,
    pagerState: PagerState,
    stateHolder: SaveableStateHolder,
    currentRoute: String?,
    sourceUrl: String?,
    visible: Boolean,
) {
```

Add `import androidx.compose.ui.graphics.graphicsLayer` and update the pager:

```kotlin
HorizontalPager(
    state = pagerState,
    modifier = Modifier
        .fillMaxSize()
        .graphicsLayer { alpha = if (visible) 1f else 0f },
    userScrollEnabled = visible,
    beyondViewportPageCount = 1,
    key = { page -> pages[page].route },
) {
```

- [ ] **Step 2: Pass the visibility flag from `ClipFlowNavHost`.**

Update the existing always-mounted call:

```kotlin
MainPager(
    navController = navController,
    pages = primaryItems,
    pagerState = pagerState,
    stateHolder = pagerStateHolder,
    currentRoute = currentRoute,
    sourceUrl = currentSourceUrl,
    visible = currentRoute in primaryRoutes,
)
```

Do not restore the old conditional around `MainPager`; it must remain composed on secondary routes.

- [ ] **Step 3: Run the focused navigation tests and source compilation.**

Run:

```text
E:/clioipflow/.gradle-clipflow/wrapper/dists/gradle-9.6.0-bin/42k10rwplmzkhuboz9kdazi7s/gradle-9.6.0/bin/gradle.bat --offline --no-daemon --no-watch-fs --console=plain -g E:/clioipflow/.gradle-clipflow :app:testDebugUnitTest --tests com.qihe.clipflow.navigation.PrimaryNavigationTest --tests com.qihe.clipflow.navigation.PrimaryPagerStateTest
```

Expected: compilation succeeds and all selected tests pass.

### Task 3: Register Android predictive back for all secondary routes

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/com/qihe/clipflow/navigation/ClipFlowNavHost.kt`

**Interfaces:**
- Enable `OnBackInvokedDispatcher` behavior at the application level.
- Register `PredictiveBackHandler` only when `isSecondaryRoute(currentRoute)` is true and a previous back-stack entry exists.

- [ ] **Step 1: Opt the application into Android predictive back.**

Add this attribute to the existing `<application>` element:

```xml
android:enableOnBackInvokedCallback="true"
```

Keep the existing `tools:targetApi` and other application attributes unchanged.

- [ ] **Step 2: Add the predictive-back callback in `ClipFlowNavHost`.**

Add imports:

```kotlin
import androidx.activity.compose.PredictiveBackHandler
import kotlinx.coroutines.CancellationException
```

After `currentRoute` is derived, register:

```kotlin
val secondaryRoute = isSecondaryRoute(currentRoute)

PredictiveBackHandler(
    enabled = secondaryRoute && navController.previousBackStackEntry != null,
) { progress ->
    try {
        progress.collect { }
        navController.popBackStack()
    } catch (_: CancellationException) {
        // The gesture was cancelled; keep the current destination.
    }
}
```

Do not add a second `BackHandler` for these routes. The top-bar button continues to call `navController.popBackStack()` directly.

- [ ] **Step 3: Run the route tests and compile the app.**

Run:

```text
E:/clioipflow/.gradle-clipflow/wrapper/dists/gradle-9.6.0-bin/42k10rwplmzkhuboz9kdazi7s/gradle-9.6.0/bin/gradle.bat --offline --no-daemon --no-watch-fs --console=plain -g E:/clioipflow/.gradle-clipflow :app:testDebugUnitTest --tests com.qihe.clipflow.navigation.PrimaryNavigationTest
```

Expected: the app source compiles and all route tests pass.

### Task 4: Full verification and APK metadata update

**Files:**
- Modify: `app/build.gradle.kts` only when the APK build is executed.

- [ ] **Step 1: Inspect the current version metadata immediately before packaging.**

Run:

```powershell
Select-String -Path app/build.gradle.kts -Pattern 'versionCode|versionName'
```

Expected before this build: `versionCode = 69` and `versionName = "3.5"`.

- [ ] **Step 2: Increment only `versionCode` for the APK build.**

Change:

```kotlin
versionCode = 69
versionName = "3.5"
```

to:

```kotlin
versionCode = 70
versionName = "3.5"
```

- [ ] **Step 3: Run complete unit-test and Debug APK verification.**

Run:

```text
E:/clioipflow/.gradle-clipflow/wrapper/dists/gradle-9.6.0-bin/42k10rwplmzkhuboz9kdazi7s/gradle-9.6.0/bin/gradle.bat --offline --no-daemon --no-watch-fs --console=plain -g E:/clioipflow/.gradle-clipflow :app:testDebugUnitTest :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`, zero test failures, and a non-empty `app/build/outputs/apk/debug/app-debug.apk` whose output metadata reports version code 70 and version name 3.5.

- [ ] **Step 4: Run final scope and cleanup checks.**

Run:

```powershell
git -c safe.directory=E:/clioipflow/ClipFlow diff --check
$matches = rg -n "DEBUG-[A-Za-z0-9_-]+" app/src
if ($LASTEXITCODE -eq 1) { Write-Output 'No tagged debug logs found.' } else { $matches }
git -c safe.directory=E:/clioipflow/ClipFlow status --short
```

Expected: no whitespace errors, no tagged debug logs, and only intended source/test/build changes plus the user's pre-existing untracked plan.

- [ ] **Step 5: Perform manual Android 13+ acceptance.**

On a physical device or emulator:

1. Open each of History, Settings, About, and Open Source.
2. Confirm no Home/parser content is visible through empty page areas.
3. Start an edge-back gesture and cancel it; the same secondary page remains.
4. Complete the gesture; the app returns to the previous page.
5. Repeat after opening a history item and confirm the parser route remains usable.

If no Android device is available, report that limitation separately from the passing unit/build checks.
