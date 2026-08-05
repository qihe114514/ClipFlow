# Liquid Bottom Tabs and Miuix Removal Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace ClipFlow's primary navigation bar with the AndroidLiquidGlass four-tab implementation and remove the external Miuix dependency while preserving existing screens, routes, and pager behavior.

**Architecture:** Keep `NavigationChrome.kt` as the business adapter from persisted `BottomNavItem` values to the copied `LiquidBottomTab` slots. Put the sample rendering and gesture implementation in focused ClipFlow components that use `com.kyant.backdrop` and `io.github.kyant0:shapes`. Replace Miuix calls in the existing navigation shell and progressive blur helpers with the corresponding Kyant backdrop and Material 3 APIs.

**Tech Stack:** Kotlin 2.3.10, Jetpack Compose BOM 2026.05.01, Material 3, `io.github.kyant0:backdrop:2.0.0`, `io.github.kyant0:shapes:1.2.0`, Navigation Compose, DataStore, JUnit.

## Global Constraints

- Preserve minSdk 29, existing routes, `PrimaryPagerState`, and `AppPreferences.bottomBarOrder` semantics.
- The four registered tabs remain Home, Douyin, Xiaohongshu, and Bilibili; missing persisted routes continue to be appended by `orderedBottomNavItems`.
- Copy the AndroidLiquidGlass `LiquidBottomTabs` behavior and adapt only package names, app state callbacks, and icon/text content.
- Remove active Miuix Gradle coordinates, Manifest overrides, imports, and user-visible Miuix references.
- Do not migrate content screens, top-bar layout, dialogs, settings controls, or unrelated dead code in this plan.
- Use the cached offline Gradle workflow and preserve unrelated worktree changes.

---

### Task 1: Port the sample bottom-tab component and gesture utilities

**Files:**
- Create: `app/src/main/java/com/qihe/clipflow/ui/component/LiquidBottomTabs.kt`
- Create: `app/src/main/java/com/qihe/clipflow/ui/component/LiquidBottomTab.kt`
- Create: `app/src/main/java/com/qihe/clipflow/ui/component/DampedDragAnimation.kt`
- Create: `app/src/main/java/com/qihe/clipflow/ui/component/InteractiveHighlight.kt`
- Create: `app/src/main/java/com/qihe/clipflow/ui/component/DragGestureInspector.kt`
- Delete: `app/src/main/java/com/qihe/clipflow/ui/component/FloatingBottomBar.kt`
- Delete: `app/src/main/java/com/qihe/clipflow/ui/component/miuix/animation/DampedDragAnimation.kt`
- Delete: `app/src/main/java/com/qihe/clipflow/ui/component/miuix/animation/InteractiveHighlight.kt`
- Delete: `app/src/main/java/com/qihe/clipflow/ui/component/miuix/modifier/DragGestureInspector.kt`
- Modify: `app/src/test/java/com/qihe/clipflow/ui/component/FloatingBottomBarTest.kt`
- Modify: `app/src/test/java/com/qihe/clipflow/ui/component/miuix/animation/DampedDragAnimationTest.kt`

**Interfaces:**
- `LiquidBottomTabs(selectedTabIndex: () -> Int, onTabSelected: (Int) -> Unit, backdrop: Backdrop, tabsCount: Int, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit)` owns sample selection and drag animation.
- `RowScope.LiquidBottomTab(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit)` provides one weighted tab slot.
- `DampedDragAnimation` exposes `value`, `targetValue`, `pressProgress`, `scaleX`, `scaleY`, `velocity`, `modifier`, `gestureModifier` behavior exactly as required by `LiquidBottomTabs`.

- [ ] **Step 1: Copy the upstream component source into focused ClipFlow files.**

  Copy the implementation from these upstream files at the requested `kmp` revision, changing only package declarations and the source utility package:

  - `app/src/commonMain/kotlin/com/kyant/backdrop/catalog/components/LiquidBottomTabs.kt`
  - `app/src/commonMain/kotlin/com/kyant/backdrop/catalog/components/LiquidBottomTab.kt`
  - `app/src/commonMain/kotlin/com/kyant/backdrop/catalog/utils/DampedDragAnimation.kt`
  - `app/src/commonMain/kotlin/com/kyant/backdrop/catalog/utils/InteractiveHighlight.kt`
  - `app/src/commonMain/kotlin/com/kyant/backdrop/catalog/utils/DragGestureInspector.kt`

  Use these ClipFlow imports in the migrated component:

  ```kotlin
  import com.kyant.backdrop.Backdrop
  import com.kyant.backdrop.backdrops.layerBackdrop
  import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
  import com.kyant.backdrop.backdrops.rememberLayerBackdrop
  import com.kyant.backdrop.drawBackdrop
  import com.kyant.backdrop.effects.blur
  import com.kyant.backdrop.effects.lens
  import com.kyant.backdrop.effects.vibrancy
  import com.kyant.backdrop.highlight.Highlight
  import com.kyant.backdrop.shadow.InnerShadow
  import com.kyant.backdrop.shadow.Shadow
  import com.kyant.shapes.Capsule
  ```

  Keep `indicatorValue(dampedValue: Float, tabsCount: Int): Float` as an internal pure helper in `LiquidBottomTabs.kt` so the existing unit test can continue to verify clamping at both ends.

- [ ] **Step 2: Move the animation tests to the new package without changing their assertions.**

  Update package/import declarations from `com.qihe.clipflow.ui.component.miuix.animation` to `com.qihe.clipflow.ui.component`, preserving tests for initial value, clamped updates, press/release progress, and animation callbacks.

- [ ] **Step 3: Run the focused unit tests before integration.**

  Run:

  ```powershell
  & 'E:\clioipflow\.gradle-clipflow\wrapper\dists\gradle-9.6.0-bin\42k10rwplmzkhuboz9kdazi7s\gradle-9.6.0\bin\gradle.bat' --offline --no-daemon --no-watch-fs --console=plain -g 'E:\clioipflow\.gradle-clipflow' :app:testDebugUnitTest --tests 'com.qihe.clipflow.ui.component.FloatingBottomBarTest' --tests 'com.qihe.clipflow.ui.component.DampedDragAnimationTest'
  ```

  Expected: `BUILD SUCCESSFUL` and all selected tests pass. If compilation fails because a copied API belongs to a different backdrop revision, adjust only the import/signature to the checked-in `backdrop:2.0.0` API and rerun.

- [ ] **Step 4: Commit the component port.**

  ```powershell
  git -c safe.directory=E:/clioipflow/ClipFlow add app/src/main/java/com/qihe/clipflow/ui/component app/src/test/java/com/qihe/clipflow/ui/component
  git -c safe.directory=E:/clioipflow/ClipFlow commit -m "feat: port liquid bottom tabs"
  ```

---

### Task 2: Replace Miuix-backed progressive blur helpers

**Files:**
- Modify: `app/src/main/java/com/qihe/clipflow/ui/component/liquid/CombinedBackdrop.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/ui/component/liquid/Vibrancy.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/ui/component/liquid/Lens.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/ui/component/liquid/InnerShadow.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/ui/component/liquid/ProgressiveTopBarBlur.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/ui/component/liquid/ProgressiveBottomBlur.kt`

**Interfaces:**
- Preserve the existing `ProgressiveTopBarBlur(backdrop: Backdrop, modifier: Modifier)` and `ProgressiveBottomBlur(backdrop: Backdrop, modifier: Modifier)` call signatures.
- Replace only external API namespaces and effect construction; callers continue to use the existing navigation shell.

- [ ] **Step 1: Replace old Miuix imports with the matching Kyant backdrop APIs.**

  Use `com.kyant.backdrop.Backdrop`, `com.kyant.backdrop.effects.*`, `com.kyant.backdrop.backdrops.*`, and `com.kyant.backdrop.shadow.*` equivalents. Keep the existing progressive blur geometry and API guards for the top/bottom blur helpers unless the new API makes the guard unnecessary.

- [ ] **Step 2: Update helper tests only where the public package or type changes.**

  Keep the existing mathematical assertions in `ProgressiveTopBarBlurTest`; add no visual snapshot dependency. The test must still cover the blur height/alpha interpolation helpers exposed by the file.

- [ ] **Step 3: Compile the affected source set.**

  Run:

  ```powershell
  & 'E:\clioipflow\.gradle-clipflow\wrapper\dists\gradle-9.6.0-bin\42k10rwplmzkhuboz9kdazi7s\gradle-9.6.0\bin\gradle.bat' --offline --no-daemon --no-watch-fs --console=plain -g 'E:\clioipflow\.gradle-clipflow' :app:testDebugUnitTest --tests 'com.qihe.clipflow.ui.component.liquid.ProgressiveTopBarBlurTest'
  ```

  Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit the helper migration.**

  ```powershell
  git -c safe.directory=E:/clioipflow/ClipFlow add app/src/main/java/com/qihe/clipflow/ui/component/liquid app/src/test/java/com/qihe/clipflow/ui/component/liquid
  git -c safe.directory=E:/clioipflow/ClipFlow commit -m "refactor: remove miuix blur helpers"
  ```

---

### Task 3: Integrate four tabs into ClipFlow navigation

**Files:**
- Modify: `app/src/main/java/com/qihe/clipflow/navigation/ClipFlowNavHost.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/navigation/NavigationChrome.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/navigation/Screen.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/data/preferences/AppPreferences.kt`
- Modify: `app/src/test/java/com/qihe/clipflow/navigation/PrimaryNavigationTest.kt`

**Interfaces:**
- `NavigationChrome.FloatingBottomBar` remains the adapter called by `ClipFlowNavHost`.
- The adapter passes `items.size` to `LiquidBottomTabs`, maps `primaryPagerState.selectedPage` to `selectedTabIndex`, and passes `primaryPagerState.animateToPage` to `onTabSelected`.
- Each tab uses `item.selectedIcon` and `item.label` via Material 3 `Icon` and `Text`; no route or pager API changes.

- [ ] **Step 1: Replace the component imports and adapter body.**

  The adapter must retain persisted ordering and bottom inset behavior, with the core call shaped as:

  ```kotlin
  LiquidBottomTabs(
      selectedTabIndex = { primaryPagerState.selectedPage },
      onTabSelected = primaryPagerState::animateToPage,
      backdrop = requireNotNull(backdrop),
      tabsCount = items.size,
      modifier = modifier.padding(
          bottom = 12.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
      ),
  ) {
      items.forEachIndexed { index, item ->
          LiquidBottomTab(onClick = { primaryPagerState.animateToPage(index) }) {
              Icon(item.selectedIcon, contentDescription = item.label)
              Text(item.label, fontSize = 11.sp, lineHeight = 14.sp, maxLines = 1)
          }
      }
  }
  ```

  Keep the existing `produceState` order collection and the early return for an empty list.

- [ ] **Step 2: Remove Miuix theme/backdrop setup from `ClipFlowNavHost`.**

  Remove `ThemeController`, `MiuixTheme`, and `top.yukonga.miuix` imports. Use the already available `com.kyant.backdrop.backdrops.layerBackdrop` and `rememberLayerBackdrop` for the bottom-bar backdrop, and keep the surrounding Material 3 `ClipFlowTheme` supplied by `MainActivity`.

- [ ] **Step 3: Make the four-tab default explicit in persistence fallback and tests.**

  Change fallback lists/comments to `listOf("home", "douyin", "xiaohongshu", "bilibili")` where defaults are declared, without changing `orderedBottomNavItems`'s append-missing behavior. Add an assertion like:

  ```kotlin
  @Test
  fun persistedThreeTabOrderStillProducesAllFourTabs() {
      assertEquals(
          listOf("home", "douyin", "xiaohongshu", "bilibili"),
          orderedBottomNavItems(listOf("home", "douyin", "xiaohongshu")).map { it.route }
      )
  }
  ```

- [ ] **Step 4: Run navigation and component tests.**

  Run:

  ```powershell
  & 'E:\clioipflow\.gradle-clipflow\wrapper\dists\gradle-9.6.0-bin\42k10rwplmzkhuboz9kdazi7s\gradle-9.6.0\bin\gradle.bat' --offline --no-daemon --no-watch-fs --console=plain -g 'E:\clioipflow\.gradle-clipflow' :app:testDebugUnitTest --tests 'com.qihe.clipflow.navigation.PrimaryNavigationTest' --tests 'com.qihe.clipflow.ui.component.FloatingBottomBarTest'
  ```

  Expected: all selected tests pass.

- [ ] **Step 5: Commit the navigation integration.**

  ```powershell
  git -c safe.directory=E:/clioipflow/ClipFlow add app/src/main/java/com/qihe/clipflow/navigation app/src/main/java/com/qihe/clipflow/data/preferences app/src/test/java/com/qihe/clipflow/navigation
  git -c safe.directory=E:/clioipflow/ClipFlow commit -m "feat: use liquid tabs for primary navigation"
  ```

---

### Task 4: Remove the Miuix dependency and verify the app

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/com/qihe/clipflow/ui/about/OpenSourceScreen.kt`
- Delete: any now-unused `app/src/main/java/com/qihe/clipflow/ui/component/miuix/**` files after Task 1 migration

**Interfaces:**
- The app dependency graph contains `io.github.kyant0:backdrop:2.0.0` and `io.github.kyant0:shapes:1.2.0`, but no `top.yukonga.miuix` coordinates.
- The manifest contains no `tools:overrideLibrary="top.yukonga.miuix..."` declaration.

- [ ] **Step 1: Update dependencies and manifest.**

  Remove:

  ```kotlin
  implementation("top.yukonga.miuix.kmp:miuix-ui-android:0.9.2")
  implementation("top.yukonga.miuix.kmp:miuix-blur-android:0.9.2")
  ```

  Add alongside the existing backdrop dependency:

  ```kotlin
  implementation("io.github.kyant0:shapes:1.2.0")
  ```

  Remove only the Miuix `tools:overrideLibrary` entry from `AndroidManifest.xml`.

- [ ] **Step 2: Remove the user-visible Miuix attribution.**

  Update the open-source component list in `OpenSourceScreen.kt` to retain Jetpack Compose, Material 3, AndroidX, Navigation Compose, Room, DataStore, Media3, Retrofit, OkHttp, Gson, Coil, Accompanist, and Umeng without naming Miuix.

- [ ] **Step 3: Search for active residual references.**

  Run:

  ```powershell
  rg -n -i "top\\.yukonga\\.miuix|miuix-ui|miuix-blur|overrideLibrary.*miuix" app
  ```

  Expected: no output. Text-only stale `.rej` artifacts, if present outside active source, must be reported rather than removed as unrelated dead files.

- [ ] **Step 4: Run the complete verification set.**

  Run:

  ```powershell
  & 'E:\clioipflow\.gradle-clipflow\wrapper\dists\gradle-9.6.0-bin\42k10rwplmzkhuboz9kdazi7s\gradle-9.6.0\bin\gradle.bat' --offline --no-daemon --no-watch-fs --console=plain -g 'E:\clioipflow\.gradle-clipflow' :app:testDebugUnitTest :app:assembleDebug
  git -c safe.directory=E:/clioipflow/ClipFlow diff --check
  git -c safe.directory=E:/clioipflow/ClipFlow status --short
  ```

  Expected: unit tests pass, `assembleDebug` reports `BUILD SUCCESSFUL`, `diff --check` is clean, and the status lists only the implementation commits' intended files.

- [ ] **Step 5: Commit the dependency cleanup.**

  ```powershell
  git -c safe.directory=E:/clioipflow/ClipFlow add app/build.gradle.kts app/src/main/AndroidManifest.xml app/src/main/java/com/qihe/clipflow/ui/about/OpenSourceScreen.kt
  git -c safe.directory=E:/clioipflow/ClipFlow commit -m "refactor: remove miuix dependency"
  ```

## Self-Review

- Scope coverage: Tasks 1-2 port the sample rendering/gesture behavior and existing blur helpers; Task 3 preserves the four-route navigation contract; Task 4 removes dependency/configuration/user-visible references and runs the full build.
- Placeholder scan: no `TBD`, `TODO`, or deferred implementation step is used.
- Type consistency: `LiquidBottomTabs` consumes `Backdrop` and `RowScope` content; `NavigationChrome` passes the same `Backdrop`, tab count, selection state, and callback types defined in Task 1.

