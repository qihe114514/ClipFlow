# Progressive Topbar Content Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let parser content draw underneath a translucent progressive-blur topbar while keeping topbar foreground controls sharp.

**Architecture:** The Scaffold will stop reserving space for a topbar. Its content layer will draw edge-to-edge inside the existing isolated navigation backdrop. A root-level overlay outside that captured layer will render the progressive blur transition, and `ClipFlowTopBar` will draw above it as the clear foreground.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3 Scaffold, Miuix `LayerBackdrop`/`drawBackdrop`, Android API 29-35, Gradle 9.6.0.

## Global Constraints

- Preserve the existing parser backdrop isolation that prevents RenderNode self-reference.
- API 33+ uses layered backdrop blur; API 29-32 uses the existing fallback and must not initialize API 33 rendering resources.
- Do not add a visible opaque card, tint panel, or duplicate rounded parser card in the topbar region.
- Keep page title, history, and settings controls clear and clickable.
- Preserve unrelated dirty changes already present in the three existing blur files.

---

### Task 1: Define the Topbar Transition Contract

**Files:**
- Modify: `app/src/main/java/com/qihe/clipflow/ui/component/liquid/ProgressiveContentBlur.kt`
- Test: `app/src/test/java/com/qihe/clipflow/ui/component/liquid/ProgressiveContentBlurTest.kt`

**Interfaces:**
- Consumes: existing `progressiveBlurLayers(heightDp, maxBlurDp, strength)`.
- Produces: a stable topbar blur region of `128.dp` with layer heights bounded by that region.

- [ ] **Step 1: Add a regression assertion for the topbar region.**

  Expose an internal constant `PROGRESSIVE_TOPBAR_REGION_DP = 128f` and add a test that calls `progressiveBlurLayers(PROGRESSIVE_TOPBAR_REGION_DP, 24f, 1f)` and asserts every layer height is greater than `0f` and less than or equal to `128f`.

- [ ] **Step 2: Run the focused test to verify the new contract fails or is absent.**

  Run:

  ```powershell
  $g='E:/clioipflow/.gradle-clipflow/wrapper/dists/gradle-9.6.0-bin/42k10rwplmzkhuboz9kdazi7s/gradle-9.6.0/bin/gradle.bat'
  & $g -g E:/clioipflow/.gradle-clipflow --offline --no-daemon --no-watch-fs --console=plain :app:testDebugUnitTest --tests com.qihe.clipflow.ui.component.liquid.ProgressiveContentBlurTest
  ```

  Expected before implementation: compilation failure because the new constant is not defined.

- [ ] **Step 3: Add the constant and keep the existing layer algorithm unchanged.**

  Add:

  ```kotlin
  internal const val PROGRESSIVE_TOPBAR_REGION_DP = 128f
  ```

  Do not change the existing radius distribution in this task; the layout fix must remain isolated from the already-tested progressive algorithm.

- [ ] **Step 4: Run the focused test and verify it passes.**

  Run the command from Step 2. Expected: the `ProgressiveContentBlurTest` suite passes.

### Task 2: Make Content Edge-to-Edge Under the Scaffold

**Files:**
- Modify: `app/src/main/java/com/qihe/clipflow/navigation/ClipFlowNavHost.kt`

**Interfaces:**
- Consumes: `PROGRESSIVE_TOPBAR_REGION_DP`, `ProgressiveContentBlur`, and the existing `backdrop`.
- Produces: content that is no longer offset below the Scaffold topbar and an overlay container with a fixed `128.dp` transition region.

- [ ] **Step 1: Remove the Scaffold topbar spacer.**

  Replace the current `topBar` slot containing `Spacer(Modifier.statusBarsPadding().height(64.dp))` with the default empty slot. Keep `contentWindowInsets = WindowInsets(0, 0, 0, 0)` so the `innerPadding` passed to the content remains zero.

- [ ] **Step 2: Keep the backdrop attached to the captured content for every route.**

  Change the layer condition from `showBottomBar && backdrop != null` to `backdrop != null`. The topbar overlay and bottom bar are both outside this captured box, so this does not reintroduce the previous self-reference cycle.

- [ ] **Step 3: Size the external overlay independently from the foreground topbar.**

  Change the root overlay to:

  ```kotlin
  Box(
      modifier = Modifier
          .fillMaxWidth()
          .height(PROGRESSIVE_TOPBAR_REGION_DP.dp)
          .align(Alignment.TopCenter)
  ) {
      ProgressiveContentBlur(
          backdrop = backdrop,
          strength = contentBlurStrength,
          fallbackColor = surfaceColor,
          modifier = Modifier.matchParentSize(),
          regionHeightDp = PROGRESSIVE_TOPBAR_REGION_DP,
          maxBlurDp = 24f,
      )
      ClipFlowTopBar(
          currentRoute = currentRoute,
          navController = navController,
          modifier = Modifier.align(Alignment.TopCenter),
      )
  }
  ```

  The blur is drawn first. The topbar foreground is drawn second and remains sharp.

### Task 3: Allow the Topbar Foreground to Be Positioned Above the Blur

**Files:**
- Modify: `app/src/main/java/com/qihe/clipflow/navigation/NavigationChrome.kt`

**Interfaces:**
- Consumes: optional `Modifier` from `ClipFlowNavHost`.
- Produces: `ClipFlowTopBar(currentRoute, navController, modifier)` with the existing default behavior for any other call site.

- [ ] **Step 1: Add the optional modifier parameter.**

  Change the signature to:

  ```kotlin
  fun ClipFlowTopBar(
      currentRoute: String?,
      navController: NavHostController,
      modifier: Modifier = Modifier,
  )
  ```

- [ ] **Step 2: Apply the modifier without changing title or action content.**

  Append the caller modifier before the existing `statusBarsPadding()` and transparent topbar configuration, preserving all current navigation callbacks and labels.

- [ ] **Step 3: Run compilation.**

  Run `:app:compileDebugKotlin` with the cached offline Gradle command. Expected: `BUILD SUCCESSFUL`.

### Task 4: Remove Blur Edge Bleed

**Files:**
- Modify: `app/src/main/java/com/qihe/clipflow/ui/component/liquid/ProgressiveContentBlur.kt`

**Interfaces:**
- Consumes: the existing `ProgressiveBlurLayer` list.
- Produces: clipped blur layers without a white surface duplicate or rounded halo.

- [ ] **Step 1: Clip each layer to its own rectangular bounds.**

  Add `clipToBounds()` after each layer's `height(layer.heightDp.dp)` and before `drawBackdrop`. This prevents blur padding from painting outside the progressive transition region.

- [ ] **Step 2: Keep API 33+ surface drawing transparent.**

  Remove the `fallbackColor` gradient from the API 33+ `onDrawSurface` branch. Leave a transparent `drawRect(Color.Transparent)` so the backdrop effect does not add a visible white panel over the sampled parser card. Keep the existing fallback gradient only in the non-runtime-shader branch.

- [ ] **Step 3: Run the focused unit test.**

  Expected: all `ProgressiveContentBlurTest` tests pass; no layer geometry behavior changes.

### Task 5: Full Verification and Device Screenshot

**Files:**
- Verify: `app/src/main/java/com/qihe/clipflow/navigation/ClipFlowNavHost.kt`
- Verify: `app/src/main/java/com/qihe/clipflow/navigation/NavigationChrome.kt`
- Verify: `app/src/main/java/com/qihe/clipflow/ui/component/liquid/ProgressiveContentBlur.kt`
- Verify: `app/src/test/java/com/qihe/clipflow/ui/component/liquid/ProgressiveContentBlurTest.kt`

- [ ] **Step 1: Run full unit tests and build the debug APK.**

  Run:

  ```powershell
  & $g -g E:/clioipflow/.gradle-clipflow --offline --no-daemon --no-watch-fs --console=plain :app:testDebugUnitTest :app:assembleDebug
  ```

  Expected: `BUILD SUCCESSFUL`, all unit tests passing, APK at `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 2: Install and launch with ADB.**

  Run:

  ```powershell
  adb install -r E:/clioipflow/ClipFlow/app/build/outputs/apk/debug/app-debug.apk
  adb shell am force-stop com.qihe.clipflow
  adb logcat -c
  adb shell am start -n com.qihe.clipflow/.MainActivity
  Start-Sleep -Seconds 5
  adb shell pidof com.qihe.clipflow
  adb logcat -d -b crash -t 200
  ```

  Expected: a live process id and an empty crash buffer.

- [ ] **Step 3: Capture the parser-page screenshot.**

  Navigate to a parser tab and capture with `adb shell screencap -p`. Confirm visually that parser content continues behind the titlebar, the transition is blurred rather than hard-separated, title/history/settings remain sharp, and no duplicate white card appears.

- [ ] **Step 4: Run final static checks.**

  Run `git diff --check` and confirm only the three intended source files are modified in addition to the already committed design and plan documents.
