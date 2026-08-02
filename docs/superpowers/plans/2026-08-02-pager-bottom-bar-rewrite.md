# Pager Bottom Bar Rewrite Implementation Plan

**Goal:** Make the bottom-bar selection switch when the main pager crosses its midpoint, then animate the selected pill with the SukiSU-style damped motion. Make every new page request interrupt the previous request immediately.

**Architecture:** `PrimaryPagerState` owns the selected page and navigation job. Pager `currentPage` is used for the midpoint handoff; the bottom bar then animates its pill to that discrete index with `DampedDragAnimation`. Pager settlement is the only point that synchronizes the navigation route, so route lag cannot pull the indicator backward.

**Tech Stack:** Kotlin, Jetpack Compose Foundation Pager, Compose UI pointer input, JUnit.

## Global Constraints

- Keep the existing parser-state preservation changes in this worktree.
- Do not add a new navigation or animation dependency.
- Preserve the existing floating glass bottom-bar visuals.
- Verify with unit tests, `git diff --check`, and the offline JDK 21 release build.

### Task 1: Lock Midpoint Page Selection

**Files:**
- Modify: `app/src/main/java/com/qihe/clipflow/navigation/Screen.kt`
- Test: `app/src/test/java/com/qihe/clipflow/navigation/PrimaryNavigationTest.kt`

- [ ] Add tests for the registered page order and page-index mapping.
- [ ] Keep the selected index independent from route state while a pager gesture is active.
- [ ] Run the focused navigation test and confirm it passes.

### Task 2: Make Bottom Bar Consume the Selected Index

**Files:**
- Modify: `app/src/main/java/com/qihe/clipflow/navigation/NavigationChrome.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/ui/component/FloatingBottomBar.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/ui/component/miuix/animation/DampedDragAnimation.kt`
- Test: `app/src/test/java/com/qihe/clipflow/ui/component/FloatingBottomBarTest.kt`

- [ ] Replace the external-position/external-active handoff with the selected page index and a damped pill animation.
- [ ] Keep local drag ownership only between pointer down and pointer release.
- [ ] On release, submit the rounded target once and release press visuals immediately.
- [ ] Make a new click cancel the prior pager animation before starting the new one.
- [ ] Run focused bottom-bar tests.

### Task 3: Synchronize Pager Settlement and Route

**Files:**
- Modify: `app/src/main/java/com/qihe/clipflow/navigation/ClipFlowNavHost.kt`
- Test: `app/src/test/java/com/qihe/clipflow/navigation/PrimaryNavigationTest.kt`

- [ ] Keep route-to-pager synchronization only for external route changes.
- [ ] Navigate the route from `settledPage` without using route state as the indicator source.
- [ ] Ensure rapid reverse requests leave the latest pager target authoritative.
- [ ] Run all unit tests.

### Task 4: Verify and Package

- [ ] Run all unit tests and `git diff --check`.
- [ ] Run the offline JDK 21 release build with the configured Gradle cache.
- [ ] Report the generated APK path and checksum.
