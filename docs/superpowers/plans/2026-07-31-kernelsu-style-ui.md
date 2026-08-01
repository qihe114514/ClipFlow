# KernelSU Style UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace ClipFlow's navigation chrome with the KernelSU Style UI Kit application shell while preserving all ClipFlow parsing and download behavior.

**Architecture:** Keep the current MVVM/data layer and secondary NavHost destinations. Add a template-compatible theme and bottom-bar layer, then make the main route a three-page `HorizontalPager` whose pages are the existing Home, Douyin, and Xiaohongshu screens. Persist only the renderer choice in DataStore; Miuix is the default and Material is the alternate renderer.

**Tech Stack:** Kotlin 2.4.0, Gradle 9.6.0, AGP 9.1.0, Jetpack Compose BOM 2026.05.01, Miuix 0.9.2, Material 3, Navigation Compose, Room, DataStore, Retrofit, Coil.

## Global Constraints

- Work only in `E:\clioipflow\ClipFlow-ui-kernelsu-miuix` on branch `codex/ui-kernelsu-miuix`.
- The main pager has exactly three pages in this order: `home`, `douyin`, `xiaohongshu`.
- Home must not render a parser input.
- The persisted UI-mode key is `ui_mode`; missing or invalid values resolve to `miuix`.
- The borrowed shell is GPLv3; retain its license text and document the source URL.
- Use JVM 17 and Android compile/target SDK 37 on this machine.

---

### Task 1: Record the design and upgrade build foundations

**Files:**
- Create: `docs/superpowers/specs/2026-07-31-kernelsu-style-ui-design.md`
- Create: `docs/superpowers/plans/2026-07-31-kernelsu-style-ui.md`
- Modify: `build.gradle.kts`
- Modify: `settings.gradle.kts`
- Modify: `gradle/libs.versions.toml`
- Modify: `gradle/wrapper/gradle-wrapper.properties`
- Modify: `app/build.gradle.kts`

**Interfaces:**
- Produces the Miuix, Navigation 3, Material Kolor, Compose compiler, and
  lifecycle dependencies used by later tasks.
- Keeps existing Retrofit, Room, DataStore, Coil, analytics, and media
  dependencies available to unchanged business code.

- [ ] Set the root plugin versions to AGP 9.2.1, Kotlin 2.4.0, and the Kotlin
  Compose compiler plugin; set Gradle to 9.5.1.
- [ ] Set compile/target SDK to 36 and Java/Kotlin JVM target to 17.
- [ ] Add Miuix 0.9.2, Material Kolor 4.1.1, Compose pager, and lifecycle
  Compose dependencies; replace the old Compose compiler extension setting
  with the Kotlin Compose compiler plugin.
- [ ] Replace KSP with Kotlin KAPT for Room if the Kotlin 2.4 KSP marker is not
  available, preserving `room-compiler` generation and the existing DAO APIs.
- [ ] Run `./gradlew :app:dependencies` and confirm dependency resolution
  before editing application code.

### Task 2: Add GPLv3 attribution and the dual theme contract

**Files:**
- Create: `LICENSE` (GPLv3 text from the template repository)
- Create: `app/src/main/java/com/qihe/clipflow/ui/UiMode.kt`
- Create: `app/src/main/java/com/qihe/clipflow/ui/theme/MiuixTheme.kt`
- Create: `app/src/main/java/com/qihe/clipflow/ui/theme/MaterialTheme.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/ui/theme/Theme.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/data/preferences/AppPreferences.kt`
- Modify: `README.md`

**Interfaces:**
- `UiMode.fromValue(value: String): UiMode` returns `Material` only for the
  exact material value and otherwise returns `Miuix`.
- `AppPreferences.uiMode: Flow<String>` and
  `suspend fun setUiMode(value: String)` persist the renderer choice.
- `TemplateTheme(uiMode: UiMode, content: @Composable () -> Unit)` selects
  the template Miuix or Material root.

- [ ] Add the `UiMode` enum and `LocalUiMode` with Miuix as the default.
- [ ] Add template-derived `TemplateTheme`, `MiuixTemplateTheme`, and
  `MaterialTemplateTheme`, keeping ClipFlow's brand colors and typography.
- [ ] Add `ui_mode` to DataStore without changing existing preference keys or
  defaults.
- [ ] Update README license text from MIT to GPLv3 and cite
  `https://github.com/chenaizhang/KernelSU-Style-UI-Kit` as the source of the
  application shell.

### Task 3: Implement the three-page pager and template bottom bar

**Files:**
- Create: `app/src/main/java/com/qihe/clipflow/ui/component/FloatingBottomBar.kt`
- Create: `app/src/main/java/com/qihe/clipflow/ui/component/bottombar/BottomBar.kt`
- Create: `app/src/main/java/com/qihe/clipflow/ui/component/bottombar/BottomBarMiuix.kt`
- Create: `app/src/main/java/com/qihe/clipflow/ui/component/bottombar/BottomBarMaterial.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/navigation/Screen.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/navigation/ClipFlowNavHost.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/navigation/NavigationChrome.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/MainActivity.kt`

**Interfaces:**
- `MainPagerConfig.PAGE_COUNT` is `3` and `coercePage` clamps to `0..2`.
- `MainPagerState.animateToPage(index: Int)` drives the pager and exposes
  `selectedPage` for bottom-bar selection.
- `BottomBar` dispatches to Miuix or Material renderer using `LocalUiMode`.

- [ ] Add the template pager-state synchronization logic and three destination
  definitions using Home, MusicNote, and Favorite icons.
- [ ] Move the existing main route into `MainScreen`; render
  `HomeScreen`, `DouyinScreen`, and `XiaohongshuScreen` as pager pages.
- [ ] Keep the floating bottom bar at the bottom of the main route and remove
  the old route-based three-item bar.
- [ ] Keep History, Settings, About, privacy dialog, wallpaper, and download
  pill overlays reachable from the shell.
- [ ] Add a detail top bar renderer that uses Miuix `TopAppBar` in Miuix mode
  and Material 3 `TopAppBar` in Material mode.

### Task 4: Connect settings and deep-link behavior

**Files:**
- Modify: `app/src/main/java/com/qihe/clipflow/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/ui/history/HistoryScreen.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/ui/douyin/DouyinScreen.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/ui/xiaohongshu/XiaohongshuScreen.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/ui/parser/PlatformParseScreenContent.kt`

**Interfaces:**
- Settings exposes a UI-mode selector with Miuix first and Material second.
- History opens the main route with a platform argument and source URL; the
  selected platform page receives that URL and invokes its existing parser.
- Parser ViewModels remain the owners of parse/download state and methods.

- [ ] Add a `uiMode` field and `setUiMode` method to `SettingsViewModel`.
- [ ] Render the new UI-mode setting in both the Miuix and Material settings
  sections while retaining wallpaper, path, default-page, and bottom-order
  settings.
- [ ] Update history navigation to use the main-route deep-link contract.
- [ ] Ensure a deep-link URL is applied once per route argument and does not
  re-trigger parsing on ordinary recomposition.
- [ ] Preserve tutorial, download-dialog, and background-download behavior.

### Task 5: Add focused tests and verify the APKs

**Files:**
- Create: `app/src/test/java/com/qihe/clipflow/ui/UiModeTest.kt`
- Create: `app/src/test/java/com/qihe/clipflow/navigation/ScreenTest.kt`
- Create: `app/src/test/java/com/qihe/clipflow/ui/component/MainPagerStateTest.kt`

**Interfaces:**
- Tests cover invalid UI-mode fallback, three-page bounds, and URL encoding
  for both platform deep links.

- [ ] Write unit tests for `UiMode.fromValue`, including empty and unknown
  values.
- [ ] Write unit tests for page-count/index bounds and deep-link URL round trips.
- [ ] Run `./gradlew :app:testDebugUnitTest` and inspect failures.
- [ ] Run `./gradlew :app:assembleDebug`.
- [ ] Run `./gradlew :app:assembleRelease` and verify
  `app/build/outputs/apk/release/app-release.apk` exists.
