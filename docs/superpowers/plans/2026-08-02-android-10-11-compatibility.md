# Android 10 and Android 11 Compatibility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make ClipFlow installable and functional on Android 10 (API 29) and Android 11 (API 30), with a non-blurred liquid-style bottom bar on API 29-32 and the existing shader-backed liquid bar on API 33+.

**Architecture:** Keep one navigation state and one bottom-bar motion implementation. A tested SDK predicate controls whether `miuix-blur` resources are created; unsupported systems follow the component's existing solid/translucent rendering branches. Replace the one direct platform system-bar call with AndroidX compatibility APIs, then lower the application minimum SDK.

**Tech Stack:** Kotlin 2.3.10, Jetpack Compose, AndroidX Core, Miuix 0.9.2, JUnit 4, Android Gradle Plugin 9.1.0, Gradle 9.6.0, JDK 21.

## Global Constraints

- Set `minSdk` to exactly 29 and leave `compileSdk = 37` and `targetSdk = 37` unchanged.
- API 33+ must retain the existing blur, refraction, shader highlight, pager progress, tap, and drag behavior.
- API 29-32 must retain the same destinations, pager progress, tap, drag selection, and elastic movement without initializing runtime shader or backdrop effects.
- Do not add dependencies or change parsing, persistence, download, gallery paths, permissions, version code, or version name.
- Keep changes limited to the compatibility boundary, bottom-bar resource creation, fullscreen system bars, tests, and support documentation.
- Use `C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot` and `E:\clioipflow\.gradle-clipflow`; run Gradle tasks serially and offline.
- Use `-Pkotlin.compiler.execution.strategy=in-process -Pkotlin.incremental=false` for verification because an interrupted baseline run exposed unstable Kotlin incremental-cache state.
- Use `:app:lintAnalyzeDebug` for main-source analysis and `:app:lintVitalRelease` for a failing report on fatal issues. The aggregate `:app:lintDebug` cannot run offline because the declared Android-test libraries are not cached.

---

### Task 1: Define and Test the Graphics Capability Boundary

**Files:**
- Modify: `app/src/test/java/com/qihe/clipflow/navigation/PrimaryNavigationTest.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/navigation/NavigationChrome.kt`

**Interfaces:**
- Consumes: integer Android SDK levels supplied by callers.
- Produces: `internal fun supportsLiquidBottomBar(sdkInt: Int): Boolean`, returning `true` only for API 33+.

- [ ] **Step 1: Write the failing boundary test**

Add this test to `PrimaryNavigationTest`:

```kotlin
@Test
fun liquidBottomBarRequiresApi33() {
    assertEquals(false, supportsLiquidBottomBar(29))
    assertEquals(false, supportsLiquidBottomBar(30))
    assertEquals(false, supportsLiquidBottomBar(32))
    assertEquals(true, supportsLiquidBottomBar(33))
    assertEquals(true, supportsLiquidBottomBar(37))
}
```

- [ ] **Step 2: Run the focused test and verify it fails**

Run from `E:\clioipflow\ClipFlow`:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'
& 'E:\clioipflow\.gradle-clipflow\wrapper\dists\gradle-9.6.0-bin\42k10rwplmzkhuboz9kdazi7s\gradle-9.6.0\bin\gradle.bat' -g 'E:\clioipflow\.gradle-clipflow' '-Pkotlin.compiler.execution.strategy=in-process' '-Pkotlin.incremental=false' --offline --no-daemon --no-watch-fs --console=plain :app:testDebugUnitTest --tests 'com.qihe.clipflow.navigation.PrimaryNavigationTest.liquidBottomBarRequiresApi33'
```

Expected: Kotlin test compilation fails with an unresolved reference to `supportsLiquidBottomBar`.

- [ ] **Step 3: Implement the minimal SDK predicate**

Add this package-level function above `FloatingBottomBar` in `NavigationChrome.kt`:

```kotlin
private const val LIQUID_BOTTOM_BAR_MIN_SDK = 33

internal fun supportsLiquidBottomBar(sdkInt: Int): Boolean {
    return sdkInt >= LIQUID_BOTTOM_BAR_MIN_SDK
}
```

- [ ] **Step 4: Run the focused and full unit tests**

Run the focused command from Step 2, then run:

```powershell
& 'E:\clioipflow\.gradle-clipflow\wrapper\dists\gradle-9.6.0-bin\42k10rwplmzkhuboz9kdazi7s\gradle-9.6.0\bin\gradle.bat' -g 'E:\clioipflow\.gradle-clipflow' '-Pkotlin.compiler.execution.strategy=in-process' '-Pkotlin.incremental=false' --offline --no-daemon --no-watch-fs --console=plain :app:testDebugUnitTest
```

Expected: the focused test and the full debug unit-test suite pass.

- [ ] **Step 5: Commit the capability boundary**

```powershell
git add -- 'app/src/main/java/com/qihe/clipflow/navigation/NavigationChrome.kt' 'app/src/test/java/com/qihe/clipflow/navigation/PrimaryNavigationTest.kt'
git diff --cached --check
git commit -m "test: define liquid bottom bar SDK boundary"
```

---

### Task 2: Add the API 29-32 Rendering Path

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/com/qihe/clipflow/navigation/ClipFlowNavHost.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/navigation/NavigationChrome.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/ui/component/FloatingBottomBar.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/ui/components/ParseInfoCard.kt`

**Interfaces:**
- Consumes: `supportsLiquidBottomBar(Build.VERSION.SDK_INT)`, the existing `PagerState`, selected-index provider, and navigation callback.
- Produces: a nullable `Backdrop` passed through `NavigationChrome`; `null` selects the current solid/translucent branches and non-null selects the current liquid branches.
- Produces: `private fun setSystemBarsHidden(activity: Activity?, hidden: Boolean)` using AndroidX insets compatibility APIs.

- [ ] **Step 1: Lower the declared minimum SDK**

Change only this line in `app/build.gradle.kts`:

```kotlin
minSdk = 29
```

- [ ] **Step 2: Run Lint and verify the current compatibility failure**

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'
& 'E:\clioipflow\.gradle-clipflow\wrapper\dists\gradle-9.6.0-bin\42k10rwplmzkhuboz9kdazi7s\gradle-9.6.0\bin\gradle.bat' -g 'E:\clioipflow\.gradle-clipflow' '-Pkotlin.compiler.execution.strategy=in-process' '-Pkotlin.incremental=false' --offline --no-daemon --no-watch-fs --console=plain :app:lintVitalRelease
```

Expected: Release Lint fails with a `NewApi` error for the API 30 `Window.getInsetsController()` usage in `ParseInfoCard.kt` against the new API 29 minimum. This is the static reproduction of the Android 10 failure.

- [ ] **Step 3: Gate backdrop creation in the navigation host**

Add `import android.os.Build` to `ClipFlowNavHost.kt`. Before `MiuixTheme`, calculate:

```kotlin
val liquidBottomBarEnabled = supportsLiquidBottomBar(Build.VERSION.SDK_INT)
```

Replace unconditional backdrop creation with:

```kotlin
val backdrop = if (liquidBottomBarEnabled) {
    rememberLayerBackdrop {
        drawRect(surfaceColor.copy(alpha = 0.18f))
        drawContent()
    }
} else {
    null
}
```

Capture content only when the backdrop exists:

```kotlin
.then(
    if (showBottomBar && backdrop != null) {
        Modifier.layerBackdrop(backdrop)
    } else {
        Modifier
    }
)
```

Continue passing `backdrop` to the bottom-bar wrapper. Do not duplicate or move the `Scaffold`, `MainPager`, or `NavHost` state.

- [ ] **Step 4: Make the navigation wrapper select blur from backdrop availability**

Change the `NavigationChrome.kt` wrapper parameter to:

```kotlin
backdrop: Backdrop?,
```

Keep all route, pager, tap, and item content code unchanged. Pass the nullable value and select effects with:

```kotlin
backdrop = backdrop,
isBlurEnabled = backdrop != null,
```

- [ ] **Step 5: Prevent liquid resources from initializing on the fallback path**

In `FloatingBottomBar.kt`, change the component parameter to `backdrop: Backdrop?` and derive one source of truth:

```kotlin
val useBlur = isBlurEnabled && backdrop != null
val containerColor = if (useBlur) surfaceContainer.copy(0.4f) else surfaceContainer
val tabsBackdrop = if (useBlur) rememberLayerBackdrop() else null
```

Replace the top-level `iosIndicatorSpecular` value with a function so its Miuix objects are created only on the liquid path:

```kotlin
private fun createIosIndicatorSpecular(): Highlight = Highlight(
    width = 1.dp,
    alpha = 1f,
    style = BloomStroke(
        color = Color.White.copy(alpha = 0.12f),
        innerBlurRadius = 2.0.dp,
        primaryLight = LightSource(
            position = LightPosition(0.5f, -0.3f, -0.05f),
            color = Color.White,
            intensity = 1f,
        ),
        secondaryLight = LightSource(
            position = LightPosition(0.5f, 0.8f, -0.5f),
            color = Color.White,
            intensity = 0.4f,
        ),
        dualPeak = true,
    ),
)
```

Create shader, highlight, and combined-backdrop resources conditionally after the existing motion state is initialized:

```kotlin
val interactiveHighlight = if (useBlur) {
    remember(animationScope, tabWidthPx) {
        InteractiveHighlight(
            animationScope = animationScope,
            position = { size, _ ->
                val indicator = indicatorValue(
                    dampedValue = dampedDragAnimation.value,
                    externalPosition = indicatorPosition,
                    externalActive = indicatorPositionActive,
                    handoffActive = indicatorHandoffActive,
                    tabsCount = tabsCount,
                )
                Offset(
                    if (isLtr) (indicator + 0.5f) * tabWidthPx + panelOffset
                    else size.width - (indicator + 0.5f) * tabWidthPx + panelOffset,
                    size.height / 2f,
                )
            },
        )
    }
} else {
    null
}
val highlightSpec = if (useBlur) remember { createIosIndicatorSpecular() } else null
val baseHighlight = if (highlightSpec != null) {
    rememberGravityRotatedHighlight(highlightSpec, extraDegrees = -45f)
} else {
    null
}
val pillHighlight = if (highlightSpec != null) {
    rememberGravityRotatedHighlight(highlightSpec, extraDegrees = 90f)
} else {
    null
}
val combinedBackdrop = if (useBlur) {
    rememberCombinedBackdrop(requireNotNull(backdrop), requireNotNull(tabsBackdrop))
} else {
    null
}
```

Replace every rendering condition `if (isBlurEnabled)` with `if (useBlur)`. Inside those liquid-only branches, use these exact non-null resources:

- `requireNotNull(backdrop)` for both `drawBackdrop` calls.
- `requireNotNull(tabsBackdrop)` for `layerBackdrop`.
- `requireNotNull(interactiveHighlight).modifier` and `.gestureModifier`.
- `requireNotNull(combinedBackdrop)` for the indicator `drawBackdrop`.
- `requireNotNull(baseHighlight)` and `requireNotNull(pillHighlight)` for highlight copies.

The existing non-blur branches at the panel background and selected indicator remain unchanged; they are the API 29-32 renderer. Do not change the existing `DampedDragAnimation`, pager handoff, or item content.

- [ ] **Step 6: Replace direct system-bar APIs with AndroidX compatibility APIs**

Add these imports to `ParseInfoCard.kt`:

```kotlin
import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
```

Add this helper above `FullscreenVideoDialog`:

```kotlin
private fun setSystemBarsHidden(activity: Activity?, hidden: Boolean) {
    val window = activity?.window ?: return
    val controller = WindowCompat.getInsetsController(window, window.decorView)
    controller.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    if (hidden) {
        controller.hide(WindowInsetsCompat.Type.systemBars())
    } else {
        controller.show(WindowInsetsCompat.Type.systemBars())
    }
}
```

Use `val activity = context as? Activity`, replace the direct hide block with `setSystemBarsHidden(activity, true)`, and replace the direct show call in `close()` with `setSystemBarsHidden(activity, false)`. Preserve orientation and player lifecycle behavior.

- [ ] **Step 7: Run compatibility checks and the debug build**

Run these commands serially:

```powershell
& 'E:\clioipflow\.gradle-clipflow\wrapper\dists\gradle-9.6.0-bin\42k10rwplmzkhuboz9kdazi7s\gradle-9.6.0\bin\gradle.bat' -g 'E:\clioipflow\.gradle-clipflow' '-Pkotlin.compiler.execution.strategy=in-process' '-Pkotlin.incremental=false' --offline --no-daemon --no-watch-fs --console=plain :app:lintAnalyzeDebug
& 'E:\clioipflow\.gradle-clipflow\wrapper\dists\gradle-9.6.0-bin\42k10rwplmzkhuboz9kdazi7s\gradle-9.6.0\bin\gradle.bat' -g 'E:\clioipflow\.gradle-clipflow' '-Pkotlin.compiler.execution.strategy=in-process' '-Pkotlin.incremental=false' --offline --no-daemon --no-watch-fs --console=plain :app:lintVitalRelease
& 'E:\clioipflow\.gradle-clipflow\wrapper\dists\gradle-9.6.0-bin\42k10rwplmzkhuboz9kdazi7s\gradle-9.6.0\bin\gradle.bat' -g 'E:\clioipflow\.gradle-clipflow' '-Pkotlin.compiler.execution.strategy=in-process' '-Pkotlin.incremental=false' --offline --no-daemon --no-watch-fs --console=plain :app:testDebugUnitTest
& 'E:\clioipflow\.gradle-clipflow\wrapper\dists\gradle-9.6.0-bin\42k10rwplmzkhuboz9kdazi7s\gradle-9.6.0\bin\gradle.bat' -g 'E:\clioipflow\.gradle-clipflow' '-Pkotlin.compiler.execution.strategy=in-process' '-Pkotlin.incremental=false' --offline --no-daemon --no-watch-fs --console=plain :app:assembleDebug
```

Expected: debug analysis and release fatal Lint succeed with no API-level error, all unit tests pass, and `app/build/outputs/apk/debug/app-debug.apk` is produced.

- [ ] **Step 8: Commit the compatibility implementation**

```powershell
git add -- 'app/build.gradle.kts' 'app/src/main/java/com/qihe/clipflow/navigation/ClipFlowNavHost.kt' 'app/src/main/java/com/qihe/clipflow/navigation/NavigationChrome.kt' 'app/src/main/java/com/qihe/clipflow/ui/component/FloatingBottomBar.kt' 'app/src/main/java/com/qihe/clipflow/ui/components/ParseInfoCard.kt'
git diff --cached --check
git commit -m "feat: support Android 10 and 11"
```

---

### Task 3: Document, Package, and Inspect the Compatible Release

**Files:**
- Modify: `README.md`
- Verify: `app/build/outputs/apk/release/app-release.apk`

**Interfaces:**
- Consumes: the completed API 29 compatibility implementation.
- Produces: support metadata matching Gradle configuration and a signed release APK whose manifest declares minimum SDK 29 and target SDK 37.

- [ ] **Step 1: Update support metadata**

Replace the two SDK rows in `README.md` with:

```markdown
| 最低 SDK | Android 10 (API 29) |
| 目标 SDK | API 37 |
```

Do not alter release notes or version numbers.

- [ ] **Step 2: Remove generated state from the interrupted baseline run**

Run Gradle's scoped clean task. This removes only `app/build`, including the quarantined `cacheable.interrupted` generated cache; all contents are reproducible:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'
& 'E:\clioipflow\.gradle-clipflow\wrapper\dists\gradle-9.6.0-bin\42k10rwplmzkhuboz9kdazi7s\gradle-9.6.0\bin\gradle.bat' -g 'E:\clioipflow\.gradle-clipflow' --offline --no-daemon --no-watch-fs --console=plain :app:clean
```

Expected: `BUILD SUCCESSFUL`; `app/build` is removed and will be recreated by later tasks.

- [ ] **Step 3: Run final source verification**

Run serially:

```powershell
& 'E:\clioipflow\.gradle-clipflow\wrapper\dists\gradle-9.6.0-bin\42k10rwplmzkhuboz9kdazi7s\gradle-9.6.0\bin\gradle.bat' -g 'E:\clioipflow\.gradle-clipflow' '-Pkotlin.compiler.execution.strategy=in-process' '-Pkotlin.incremental=false' --offline --no-daemon --no-watch-fs --console=plain :app:testDebugUnitTest
& 'E:\clioipflow\.gradle-clipflow\wrapper\dists\gradle-9.6.0-bin\42k10rwplmzkhuboz9kdazi7s\gradle-9.6.0\bin\gradle.bat' -g 'E:\clioipflow\.gradle-clipflow' '-Pkotlin.compiler.execution.strategy=in-process' '-Pkotlin.incremental=false' --offline --no-daemon --no-watch-fs --console=plain :app:lintAnalyzeDebug
& 'E:\clioipflow\.gradle-clipflow\wrapper\dists\gradle-9.6.0-bin\42k10rwplmzkhuboz9kdazi7s\gradle-9.6.0\bin\gradle.bat' -g 'E:\clioipflow\.gradle-clipflow' '-Pkotlin.compiler.execution.strategy=in-process' '-Pkotlin.incremental=false' --offline --no-daemon --no-watch-fs --console=plain :app:lintVitalRelease
git diff --check
```

Expected: unit tests, main-source analysis, and release fatal Lint pass, and Git reports no whitespace errors.

- [ ] **Step 4: Build the release APK**

```powershell
& 'E:\clioipflow\.gradle-clipflow\wrapper\dists\gradle-9.6.0-bin\42k10rwplmzkhuboz9kdazi7s\gradle-9.6.0\bin\gradle.bat' -g 'E:\clioipflow\.gradle-clipflow' '-Pkotlin.compiler.execution.strategy=in-process' '-Pkotlin.incremental=false' --offline --no-daemon --no-watch-fs --console=plain :app:assembleRelease
```

Expected: `BUILD SUCCESSFUL` and `E:\clioipflow\ClipFlow\app\build\outputs\apk\release\app-release.apk` exists with nonzero size.

- [ ] **Step 5: Inspect the packaged SDK contract and signature**

```powershell
& 'C:\Users\VOS-User\AppData\Local\Android\Sdk\cmdline-tools\latest\bin\apkanalyzer.bat' manifest min-sdk 'E:\clioipflow\ClipFlow\app\build\outputs\apk\release\app-release.apk'
& 'C:\Users\VOS-User\AppData\Local\Android\Sdk\cmdline-tools\latest\bin\apkanalyzer.bat' manifest target-sdk 'E:\clioipflow\ClipFlow\app\build\outputs\apk\release\app-release.apk'
& 'C:\Users\VOS-User\AppData\Local\Android\Sdk\build-tools\37.0.0\apksigner.bat' verify --verbose 'E:\clioipflow\ClipFlow\app\build\outputs\apk\release\app-release.apk'
```

Expected: minimum SDK `29`, target SDK `37`, and APK signature verification succeeds.

- [ ] **Step 6: Record the runtime-test boundary**

```powershell
& 'C:\Users\VOS-User\AppData\Local\Android\Sdk\platform-tools\adb.exe' devices -l
Get-Command emulator -ErrorAction SilentlyContinue
```

Current expected result: no connected devices and no local emulator command/system images. Do not claim API 29/30 startup acceptance without a device. Report that static analysis, compilation, packaging, and APK metadata were verified, while device interaction remains unverified.

- [ ] **Step 7: Commit documentation and verify final Git state**

```powershell
git add -- 'README.md'
git diff --cached --check
git commit -m "docs: document Android 10 support"
git status --short --branch
```

Expected: the documentation commit succeeds and the worktree is clean on `main`.
