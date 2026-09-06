# Settings, Personalization, and Download Destinations Design

## Job and audience

- The settings surface serves ordinary ClipFlow users who want to choose where downloads go, what opens first, and how the app looks without understanding implementation terms.
- The personalization surface serves both ordinary users making broad appearance choices and advanced users who want the existing glass-effect controls.
- The product remains a practical Android media utility. This work preserves the current liquid-glass visual language without making decoration compete with task completion.

## Outcome and proof

- A user can see whether each media type saves to the system default or an explicitly chosen folder, change it by tapping the row, and restore the default only when a custom folder is active.
- Custom video, image, and audio folders must be honored by ordinary downloads and Bilibili's merged MP4 download. A missing custom folder must never silently redirect output elsewhere.
- A user can choose every primary route, including Bilibili, as the launch page and can understand the bottom-navigation order controls.
- A user can choose a theme, wallpaper, and one overall glass-strength setting without seeing technical controls. All existing technical controls remain available under Advanced appearance.

## Settings structure

- Show Download locations first. Each row has a media icon, media label, readable destination summary, and a trailing navigation affordance. Tapping the row opens the system folder picker. A custom destination also exposes a restore-default action. Do not show raw `content://` values.
- Keep Appearance as one destination row with the summary "Theme, wallpaper, and glass effects." Do not expose blur, refraction, or contrast terminology on the settings page.
- Group launch page and bottom-navigation ordering under App layout. The launch-page control includes Home, Douyin, Xiaohongshu, and Bilibili. Each navigation row has its full label, ordinal position, and accessible up/down controls.
- Keep About and reset tutorial as secondary actions at the bottom. The reset tutorial control remains visible at normal contrast.

## Personalization structure

- Keep the live appearance preview and restore-default action at the top.
- The default surface contains theme mode, dynamic color where supported, wallpaper source, wallpaper visibility, and an overall Glass strength control. Strength labels describe results: Off, Soft, Default, and Strong.
- Advanced appearance is collapsed initially. It retains wallpaper contrast, card glass effect, card blur, button glass effect, and button blur. Its collapsed summary reports the effective card and button settings.
- Every setting applies immediately and remains stored using the existing preference keys. No current preference is removed or renamed.

## Download destination behavior

- A selected Storage Access Framework tree URI is stored independently for video, image, and audio after persistable read/write permission is granted.
- When a custom tree URI exists, write the completed file into that directory using `DocumentFile` and `ContentResolver.openOutputStream`. Use the same destination resolver for regular downloads and Bilibili MP4 output.
- When no custom tree URI exists, preserve the current MediaStore behavior: videos go to `Movies/ClipFlow`, images to `Pictures/ClipFlow`, and audio to `Music/ClipFlow`.
- If the custom URI can no longer be opened, no output stream can be created, or a write fails, report a save failure and remove any partially created destination document when possible. Do not fall back to MediaStore.
- File transfer and save operations run on `Dispatchers.IO`. All cache files are deleted after success, failure, or cancellation.

## Boundaries and validation

- Scope is `SettingsScreen`, `PersonalizationScreen`, preferences needed by destination resolution, media-save utilities, and the two download paths. Do not redesign parsing, history, navigation, or media preview in this change.
- Preserve every existing advanced appearance control. Do not package an APK, change `versionCode`, or change `versionName`.
- Add focused JVM tests for destination selection by media type, the default destination fallback, invalid custom destination failures, launch-page choices, and persisted bottom-navigation-order parsing.
- Verify with `:app:testDebugUnitTest`, `:app:assembleDebug`, and `git diff --check` after a usable local JDK is configured. On a device or emulator, verify default and custom destinations for video, image, audio, Bilibili MP4, and revoked folder permission.
