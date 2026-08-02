### Task 2: Pass pager progress through the navigation chrome

**Files:**
- Modify: `app/src/main/java/com/qihe/clipflow/navigation/ClipFlowNavHost.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/navigation/NavigationChrome.kt`

**Interfaces:**
- `NavigationChrome.FloatingBottomBar` receives the existing `PagerState` in addition to its route and visual dependencies.
- It passes `indicatorPosition = { pagerIndicatorPosition(...) }` and `indicatorPositionActive = { pagerState.isScrollInProgress }` to the UI component.

- [ ] **Step 1: Add the `PagerState` parameter at the call site.**

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

- [ ] **Step 2: Derive the live progress in `NavigationChrome.kt`.**

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

- [ ] **Step 3: Compile the navigation changes with the focused unit test.**

Run:

```powershell
& 'C:\Users\VOS-User\.gradle\wrapper\dists\gradle-9.6.0-bin\42k10rwplmzkhuboz9kdazi7s\gradle-9.6.0\bin\gradle.bat' -g E:/clioipflow/.gradle-clipflow --offline --no-daemon --no-watch-fs --console=plain :app:testDebugUnitTest --tests com.qihe.clipflow.navigation.PrimaryNavigationTest
```

Expected: the navigation source compiles; the test suite remains green after the new parameter wiring.

- [ ] **Step 4: Commit the navigation-state wiring.**

```powershell
git -c safe.directory=E:/clioipflow/ClipFlow add app/src/main/java/com/qihe/clipflow/navigation/ClipFlowNavHost.kt app/src/main/java/com/qihe/clipflow/navigation/NavigationChrome.kt
git -c safe.directory=E:/clioipflow/ClipFlow commit -m "feat: expose pager progress to bottom navigation"
```
