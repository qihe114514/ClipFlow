# 启动自动检查更新设计

## 目标

应用启动进入首页后自动执行一次更新检查。检查过程不阻塞启动，也不导航到关于页。

## 行为

- 使用现有 `UpdateManager.checkUpdate(BuildConfig.VERSION_NAME)`。
- 无更新时在首页显示一次轻量提示“已是最新版本”。
- 有更新时在首页弹出更新对话框，展示版本号和发布说明，并提供下载、稍后处理。
- 下载复用现有 `DownloadManager`；下载完成后复用 `UpdateManager.installApk`。
- 检查失败静默处理，避免启动时打扰用户。
- `LaunchedEffect(Unit)` 保证每次 Activity 创建只检查一次。

## 版本与发布

- `versionCode` 从 86 递增到 87。
- `versionName` 保持 `3.6`，除非发布标签需要更高版本名才能触发更新比较；发布前根据现有远端标签确认最终标签。

## 验证

- 运行 `.\gradlew.bat test` 或至少执行相关单元测试。
- 构建 `assembleRelease` 并确认 APK 版本元数据为预期值。
- 检查 Git 差异后提交并发布 GitHub Release。
