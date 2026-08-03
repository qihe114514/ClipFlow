# Bilibili Login and Input UI Implementation Plan

> **For agentic workers:** Execute this plan task-by-task with verification after each task.

**Goal:** Make App-internal Bilibili WebView login validation consume the complete Cookie state and make the Bilibili parser input controls match Douyin and Xiaohongshu.

**Architecture:** Keep the existing authenticated navigation check. Extract only the pure Cookie-header merge into a small utility so host aggregation is testable, then flush and read the WebView CookieManager from the relevant Bilibili hosts. Promote the existing shared parser input card and use it from Bilibili without changing Bilibili result or download behavior.

**Tech Stack:** Kotlin, Android WebView CookieManager, Jetpack Compose, JUnit 4, Gradle 9.6.

## Global Constraints

- Do not change versionName or versionCode.
- Do not change Bilibili parsing, download, session encryption, or other platform behavior.
- Use the existing offline Gradle cache at `E:/clioipflow/.gradle-clipflow`.

### Task 1: Make WebView Cookie validation testable and complete

**Files:**
- Create: `app/src/main/java/com/qihe/clipflow/data/bilibili/BilibiliCookieUtils.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/ui/bilibili/BilibiliAccountAction.kt`
- Test: `app/src/test/java/com/qihe/clipflow/data/bilibili/BilibiliCookieUtilsTest.kt`

- [ ] Write a failing unit test that merges CookieManager header fragments, removes whitespace, removes duplicate cookie tokens, and preserves all distinct cookie pairs.
- [ ] Run `:app:testDebugUnitTest --tests com.qihe.clipflow.data.bilibili.BilibiliCookieUtilsTest` and confirm the new test fails before the utility exists.
- [ ] Add `mergeBilibiliCookieHeaders(headers: Iterable<String?>): String` and call it from `readBilibiliCookies()`.
- [ ] Call `CookieManager.getInstance().flush()` before reading cookies and collect cookies from `www.bilibili.com`, `passport.bilibili.com`, `api.bilibili.com`, and `space.bilibili.com`.
- [ ] Re-run the focused test and confirm it passes.

### Task 2: Reuse the shared parser input controls on Bilibili

**Files:**
- Modify: `app/src/main/java/com/qihe/clipflow/ui/parser/PlatformParseScreenContent.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/ui/bilibili/BilibiliScreen.kt`

- [ ] Promote the existing `ParseInputCard` to a reusable `PlatformParseInputCard` without changing its visual structure or callbacks.
- [ ] Replace Bilibili's `OutlinedTextField` and `Button` block with `PlatformParseInputCard`, using the Bilibili accent and existing `setInput`/`parse` actions; clear/paste behavior should use the system clipboard and `setInput`.
- [ ] Keep Bilibili-specific result cards, part chips, quality list, and download dialog unchanged.
- [ ] Run the complete unit test task and `:app:assembleRelease` serially; confirm the release APK exists and `git status --short` contains only intended source/test/plan files.
