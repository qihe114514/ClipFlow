# Progressive Parser Content Blur Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a top-originating progressive blur to parser input/result content while keeping the top app bar and foreground dialogs sharp.

**Architecture:** Add one reusable overlay composable in the existing liquid component package. Parser pages render it above their scroll content and below their dialogs, receiving the root Miuix backdrop and a navigation-host interaction strength. API 33+ uses a fixed-sample RuntimeShader; API 29-32 uses a translucent vertical gradient fallback.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Miuix blur 0.9.2, Android RuntimeShader on API 33+, JUnit 4, cached Gradle 9.6/JDK 21.

## Global Constraints

- Blur parser input, loading/error state, parsed result, and download options only.
- Keep the top app bar, its actions, foreground dialogs, and wallpaper outside the blur layer.
- Do not initialize RuntimeShader or Miuix liquid resources on API 29-32.
- Keep the change focused; do not add a user preference or refactor unrelated page code.
- Preserve existing dirty or untracked work.

---

### Task 1: Add the progressive blur effect and pure state helpers

**Files:**
- Create: `app/src/main/java/com/qihe/clipflow/ui/component/liquid/ProgressiveContentBlur.kt`
- Create: `app/src/test/java/com/qihe/clipflow/ui/component/liquid/ProgressiveContentBlurTest.kt`

**Interfaces:**
- Produces `ProgressiveContentBlur(backdrop: Backdrop?, strength: Float, fallbackColor: Color, modifier: Modifier = Modifier)`.
- Produces `combineProgressiveBlurStrength(base: Float, interaction: Float): Float`.
- Produces `supportsProgressiveContentBlur(sdkInt: Int): Boolean`.

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.qihe.clipflow.ui.component.liquid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressiveContentBlurTest {
    @Test
    fun strengthIsClampedToTheSupportedRange() {
        assertEquals(0f, combineProgressiveBlurStrength(-1f, -1f), 0.0001f)
        assertEquals(0.7f, combineProgressiveBlurStrength(0.35f, 0.35f), 0.0001f)
        assertEquals(1f, combineProgressiveBlurStrength(0.8f, 0.8f), 0.0001f)
    }

    @Test
    fun progressiveBlurRequiresApi33() {
        assertFalse(supportsProgressiveContentBlur(32))
        assertTrue(supportsProgressiveContentBlur(33))
    }
}
```

- [ ] **Step 2: Run the focused test and verify it fails**

Run:

```powershell
E:\clioipflow\.gradle-clipflow\wrapper\dists\gradle-9.6.0-bin\42k10rwplmzkhuboz9kdazi7s\gradle-9.6.0\bin\gradle.bat -g E:/clioipflow/.gradle-clipflow --offline --no-daemon --no-watch-fs --console=plain :app:testDebugUnitTest --tests com.qihe.clipflow.ui.component.liquid.ProgressiveContentBlurTest
```

Expected: compilation fails because the helper functions do not exist.

- [ ] **Step 3: Implement the minimal effect**

Use the following shape:

```kotlin
internal const val PROGRESSIVE_BLUR_BASE_STRENGTH = 0.55f

internal fun combineProgressiveBlurStrength(base: Float, interaction: Float): Float =
    (base + interaction).coerceIn(0f, 1f)

internal fun supportsProgressiveContentBlur(sdkInt: Int): Boolean = sdkInt >= 33
```

The composable should return an empty layout when `strength <= 0f`. On API 33+
and with a non-null backdrop, use `drawBackdrop` with a rectangle shape and a
`progressiveBlur(strength)` effect. On older APIs, draw a vertical gradient
using `fallbackColor` with alpha proportional to `strength`. Keep all shader
creation inside the API-33 branch.

The `BackdropEffectScope.progressiveBlur` effect should set `padding` to the
maximum sample radius and use `runtimeShaderEffect` with a fixed-sample shader.
The shader must calculate `falloff = 1.0 - smoothstep(0.0, size.y * 0.55,
coord.y)`, multiply it by `strength`, sample the `content` shader at the
center and eight fixed offsets, and return their average.

- [ ] **Step 4: Run the focused test and verify it passes**

Run the same Gradle command from Step 2.

Expected: `ProgressiveContentBlurTest` passes.

- [ ] **Step 5: Commit the isolated effect**

```powershell
git add app/src/main/java/com/qihe/clipflow/ui/component/liquid/ProgressiveContentBlur.kt app/src/test/java/com/qihe/clipflow/ui/component/liquid/ProgressiveContentBlurTest.kt
git commit -m "feat: add progressive content blur effect"
```

### Task 2: Wire parser pages below the clear top bar

**Files:**
- Modify: `app/src/main/java/com/qihe/clipflow/navigation/ClipFlowNavHost.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/navigation/MainPager.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/ui/parser/PlatformParseScreenContent.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/ui/douyin/DouyinScreen.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/ui/xiaohongshu/XiaohongshuScreen.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/ui/bilibili/BilibiliScreen.kt`

**Interfaces:**
- `MainPager` consumes `contentBackdrop: Backdrop?` and `contentBlurStrength: Float`.
- Parser screen composables consume those two values and pass them to the shared content layer.

- [ ] **Step 1: Add the explicit data flow without changing rendering**

Extend the calls with named parameters:

```kotlin
MainPager(
    // existing arguments
    contentBackdrop = backdrop,
    contentBlurStrength = contentBlurStrength,
)
```

Then add matching parameters to `MainPager`, `DouyinScreen`,
`XiaohongshuScreen`, `BilibiliScreen`, and
`PlatformParseScreenContent`. Keep the existing default behavior by using
`backdrop: Backdrop?` and `strength: Float` only at page boundaries where a
default is needed.

- [ ] **Step 2: Add the parser overlay below local dialogs**

Keep this ordering in `PlatformParseScreenContent`:

```kotlin
Box(Modifier.fillMaxSize()) {
    LazyColumn(/* existing content */)
    ProgressiveContentBlur(
        backdrop = contentBackdrop,
        strength = contentBlurStrength,
        fallbackColor = MaterialTheme.colorScheme.surface,
    )
    if (uiState.showDownloadDialog && uiState.downloadingItemId != null) {
        DownloadProgressDialog(/* existing arguments */)
    }
}
```

Use the same ordering in `BilibiliScreen`, placing its blur layer after the
`LazyColumn` and before `state.download` dialog rendering. This keeps dialogs
sharp while only the parser content is affected.

- [ ] **Step 3: Run unit tests and compile the wiring**

Run:

```powershell
E:\clioipflow\.gradle-clipflow\wrapper\dists\gradle-9.6.0-bin\42k10rwplmzkhuboz9kdazi7s\gradle-9.6.0\bin\gradle.bat -g E:/clioipflow/.gradle-clipflow --offline --no-daemon --no-watch-fs --console=plain :app:testDebugUnitTest
```

Expected: all existing tests and `ProgressiveContentBlurTest` pass.

- [ ] **Step 4: Commit the parser-layer wiring**

```powershell
git add app/src/main/java/com/qihe/clipflow/navigation/ClipFlowNavHost.kt app/src/main/java/com/qihe/clipflow/navigation/MainPager.kt app/src/main/java/com/qihe/clipflow/ui/parser/PlatformParseScreenContent.kt app/src/main/java/com/qihe/clipflow/ui/douyin/DouyinScreen.kt app/src/main/java/com/qihe/clipflow/ui/xiaohongshu/XiaohongshuScreen.kt app/src/main/java/com/qihe/clipflow/ui/bilibili/BilibiliScreen.kt
git commit -m "feat: blur parser content below top bar"
```

### Task 3: Add interaction-strength propagation

**Files:**
- Modify: `app/src/main/java/com/qihe/clipflow/ui/component/FloatingBottomBar.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/navigation/NavigationChrome.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/navigation/ClipFlowNavHost.kt`
- Modify: parser screen files from Task 2 as needed for local dialog/tutorial state.

**Interfaces:**
- `FloatingBottomBar` consumes `onPressProgress: (Float) -> Unit = {}`.
- The navigation host combines bottom-bar progress with privacy-dialog state.
- Parser pages add local dialog/tutorial contributions before calling
  `combineProgressiveBlurStrength`.

- [ ] **Step 1: Add the bottom-bar progress callback**

Add the optional callback and collect the existing animation property:

```kotlin
LaunchedEffect(dampedDragAnimation, onPressProgress) {
    snapshotFlow { dampedDragAnimation.pressProgress }
        .collectLatest(onPressProgress)
}
```

Call it from the navigation wrapper and pass the host's state setter from
`ClipFlowNavHost`. Reset the host state to `0f` when the bottom bar is not
visible.

- [ ] **Step 2: Combine global and local foreground state**

Use the existing privacy readiness state and a bounded helper:

```kotlin
val privacyBlurStrength = if (!privacyAgreed && isPrivacyCheckReady) 0.35f else 0f
val contentBlurStrength = combineProgressiveBlurStrength(
    base = PROGRESSIVE_BLUR_BASE_STRENGTH,
    interaction = bottomBarPressProgress * 0.35f + privacyBlurStrength,
)
```

For parser download dialogs and the Douyin tutorial, add a local `0.35f`
contribution while the surface is visible. Do not place that surface inside
the blur composable.

- [ ] **Step 3: Run tests and build the debug APK**

```powershell
E:\clioipflow\.gradle-clipflow\wrapper\dists\gradle-9.6.0-bin\42k10rwplmzkhuboz9kdazi7s\gradle-9.6.0\bin\gradle.bat -g E:/clioipflow/.gradle-clipflow --offline --no-daemon --no-watch-fs --console=plain :app:testDebugUnitTest :app:assembleDebug
git diff --check
```

Expected: `BUILD SUCCESSFUL`, all unit tests pass, and no whitespace errors.

- [ ] **Step 4: Commit interaction propagation**

```powershell
git add app/src/main/java/com/qihe/clipflow/ui/component/FloatingBottomBar.kt app/src/main/java/com/qihe/clipflow/navigation/NavigationChrome.kt app/src/main/java/com/qihe/clipflow/navigation/ClipFlowNavHost.kt app/src/main/java/com/qihe/clipflow/ui/douyin/DouyinScreen.kt app/src/main/java/com/qihe/clipflow/ui/xiaohongshu/XiaohongshuScreen.kt app/src/main/java/com/qihe/clipflow/ui/bilibili/BilibiliScreen.kt
git commit -m "feat: strengthen blur during foreground interactions"
```

## Plan Self-Review

- Spec coverage: rendering, API fallback, parser/Bilibili placement, dialog
  ordering, bottom-bar progress, privacy state, tests, and build checks are
  all assigned to tasks.
- Placeholder scan: no TBD/TODO/implement-later instructions are present.
- Type consistency: `Backdrop?`, `Float`, and callback signatures are named
  consistently across the planned call chain.

