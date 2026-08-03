# 关于页开源信息设计

## 目标

在关于页的“更新”与“隐私”之间增加“开源与须知”入口，并在独立页面集中展示 ClipFlow 的开源协议、第三方库、平台使用限制和版权责任。

## 设计

- 新增 `Screen.OpenSource` 路由和 `OpenSourceScreen` Compose 页面。
- 页面沿用关于页的透明背景、`GlassCard`、Material 3 图标和滚动布局；顶部标题为“开源与须知”，通过普通详情页返回按钮返回关于页。
- 关于页只保留一个入口卡片，避免将协议正文塞进现有滚动页面。
- 文案明确 ClipFlow 使用 GPLv3，第三方依赖按各自许可证分发；B 站登录 Cookie 仅用于账号授权请求；用户必须拥有内容保存权并遵守平台条款。

## 版本发布

- `versionName = "3.2"`，`versionCode = 47`。
- README、CHANGELOG 和 `docs/RELEASE_NOTES_v3.2.md` 同步版本与 B 站解析、开源信息页面内容。
- Release APK 使用现有离线 Gradle/JDK 21 流程构建，产物命名为 `ClipFlow-v3.2.apk`。
