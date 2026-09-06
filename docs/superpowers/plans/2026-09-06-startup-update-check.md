# 启动自动检查更新实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 在首页启动时自动检查一次更新，并在首页提示最新状态或显示更新下载弹窗。

**Architecture:** `MainActivity` 在根 Compose 内容中以 `LaunchedEffect(Unit)` 调用现有 `UpdateManager`。检查结果由根层状态驱动 `Toast` 或 `AlertDialog`，下载和安装复用现有 `DownloadManager` 与 `UpdateManager.installApk`。

**Tech Stack:** Kotlin、Jetpack Compose、OkHttp、Android FileProvider。

## Global Constraints

- 不导航到关于页。
- 无更新提示“已是最新版本”；检查失败静默。
- 每次 Activity 创建只检查一次。
- 打包前 `versionCode` 加 1，`versionName` 保持 `3.6`。

### Task 1: 提交设计文档

**Files:**
- Create: `docs/superpowers/specs/2026-09-06-startup-update-check-design.md`

- [ ] **Step 1: Commit the approved design**

```powershell
git add docs/superpowers/specs/2026-09-06-startup-update-check-design.md
git commit -m "docs: design startup update check"
```

### Task 2: Add startup update check UI

**Files:**
- Modify: `app/src/main/java/com/qihe/clipflow/MainActivity.kt`

- [ ] **Step 1: Add root update state and one-shot check**

Use `LaunchedEffect(Unit)` to call `UpdateManager.checkUpdate(BuildConfig.VERSION_NAME)`. Show a `Toast` for `null`; store `UpdateInfo` for an `AlertDialog`; ignore failures.

- [ ] **Step 2: Reuse download/install flow in the dialog**

Use `DownloadManager` for the APK. Provide “稍后” and “下载更新” actions, show progress while downloading, and call `UpdateManager.installApk` after the download when package-install permission is available (otherwise open the existing unknown-sources settings screen).

### Task 3: Increment release metadata

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Increment only `versionCode`**

Change `versionCode = 86` to `versionCode = 87`; keep `versionName = "3.6"`.

### Task 4: Verify and publish

**Files:**
- Create: `RELEASE_NOTES_v3.6.1.md`

- [ ] **Step 1: Run unit tests and release build**

```powershell
.\gradlew.bat test
.\gradlew.bat assembleRelease
```

- [ ] **Step 2: Confirm version metadata and APK output**

Check `app/build.gradle.kts` and `app/build/outputs/apk/release/*.apk`.

- [ ] **Step 3: Commit, tag, and publish the GitHub release**

Create release tag `v3.6.1` (higher than the installed `3.6` for `UpdateManager.compareVersion`), attach the built APK, and push the commit/tag to `origin`.
