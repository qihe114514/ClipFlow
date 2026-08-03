# ClipFlow 30-Second 4:3 Promo Design

## Goal

Create a 30-second Chinese product promo for ClipFlow at 1440x1080, 30 fps, using real Android captures, Chinese narration, and a restrained electronic music bed.

## Audience and Message

The audience is Android users who want a direct way to turn shared links into locally saved media. The message is: copy a share link, let ClipFlow parse it, inspect the available media, and save what the user is allowed to keep.

## Visual Identity

Use the existing ClipFlow identity from `DESIGN.md`: dark fluid technology, real ClipFlow interface captures, translucent glass framing, and restrained liquid motion. Use `#0B0D14` as the canvas, `#6B9DFC` and `#9B7BFC` for the primary light, `#00D4D4` for the Douyin accent, and `#FF3B5C` for the Xiaohongshu accent. Use the existing system sans-serif direction with large, high-contrast Chinese copy.

## Format and Audio

- Canvas: 1440x1080, 4:3
- Frame rate: 30 fps
- Duration: exactly 30 seconds
- Delivery: H.264 MP4
- Narration: Mandarin Chinese, clear and concise, mixed above the music bed
- Music: low-volume, locally generated light electronic bed with no copyrighted source material
- Captions: simplified Chinese, short phrase groups synced to narration

## Narrative and Timing

| Time | Beat | Visual | On-screen copy |
| --- | --- | --- | --- |
| 0.0-4.0s | Brand hook | ClipFlow mark resolves over dark liquid light; a phone frame enters | `复制链接，保存想要的内容` |
| 4.0-11.0s | Parse | Real device capture shows the Douyin parser surface and a safe masked share-link chip | `粘贴分享链接，一键解析` |
| 11.0-19.0s | Inspect and save | Real result/download capture with callouts for original quality, no watermark, progress, and notification | `原画 / 无水印 / 进度清晰可见` |
| 19.0-25.0s | Breadth and organization | Platform tags and real UI cards for Xiaohongshu, Bilibili, and history; category paths appear as compact labels | `抖音 · 小红书 · 哔哩哔哩` |
| 25.0-30.0s | Close | History/library card settles into the brand lockup and CTA | `ClipFlow` / `开源 · 免费` |

## Narration Script

想把喜欢的视频保存下来，不必反复截图。ClipFlow，复制分享链接，粘贴即可解析。抖音支持原画与无水印下载，小红书支持图集、视频和音乐，哔哩哔哩按账号权限获取清晰度。下载进度、后台任务、完成通知一目了然；历史记录随时回看，视频、图片、音乐分别保存。ClipFlow，开源、免费，请只下载自己有权使用的内容。

## Motion and Transitions

Use one paused GSAP master timeline. Every scene enters with a distinct motion signature: brand scale/opacity, parser lateral slide, result card rise, platform tag stagger, and final lockup settle. Use short liquid-glass crossfades between related scenes and one faster grid-like accent for the platform change. Do not pre-fade outgoing scenes before a transition. The final scene may fade to black during the last 0.4 seconds.

## Asset Safety

- Use ADB only to capture the installed ClipFlow package and only after checking the visible frame.
- Mask or avoid account names, cookies, tokens, private links, and private media.
- Preserve portrait device captures at their native aspect ratio inside a fixed phone frame; never stretch the app UI.
- Keep repository fallbacks available if a live state cannot be reproduced safely.
- Do not modify Android source, Gradle files, manifests, or app resources.

## Acceptance Criteria

1. `promo/clipflow-30s-4x3/` contains the HyperFrames source, safe media assets, audio assets, and final MP4.
2. The final MP4 is exactly 30 seconds, 1440x1080, and 30 fps.
3. Mandarin narration and music are present, with narration remaining intelligible over the music.
4. `npx hyperframes lint` and `npx hyperframes validate` pass without errors.
5. `npx hyperframes inspect --samples 15 --strict` reports no unintended text or canvas overflow.
6. No private device data or unrelated Android source changes are included in the promo deliverable.
