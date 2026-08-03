# ClipFlow 20-Second Promo Video Design

## Goal

Create a 20-second vertical product promo for ClipFlow at 1080x1920. The film should make the product feel fast, clear, and trustworthy by showing the real Android app handling shared links and saving media locally.

## Audience and Message

The audience is Android users who want a direct way to turn shared links into saved media. The message is: "Paste a shared link, understand the result, and save it with ClipFlow." The film should communicate product behavior rather than make unsupported claims.

## Approved Direction

- Mood: fluid technology.
- Canvas: dark.
- Brand: use the existing ClipFlow blue-purple palette and system sans-serif typography.
- Format: 1080x1920 vertical, 20 seconds, MP4.
- Primary source: real device screenshots or screen recordings captured from the installed app with ADB.
- Secondary source: repository screenshots and launcher artwork when the device capture cannot show a stable state.

## Narrative and Timing

| Time | Beat | Visual | On-screen copy |
| --- | --- | --- | --- |
| 0.0-2.0s | Brand hook | Dark canvas, ClipFlow mark, a liquid blue-purple line resolves into the wordmark | `Turn shared links into saved files` |
| 2.0-7.0s | Paste and parse | Real device enters the Douyin parser, link field fills, parse action is pressed, loading state resolves | `Paste. Parse.` |
| 7.0-12.0s | Inspect and save | Result card reveals cover/title/details, original or clear quality is highlighted, download action and progress state appear | `See the result. Save what you need.` |
| 12.0-16.0s | Platform breadth | Two quick real UI crops or screenshot cards introduce Xiaohongshu and Bilibili parser surfaces with colored accent tags | `Douyin  /  Xiaohongshu  /  Bilibili` |
| 16.0-20.0s | Close | Download completion flows into history/library state, then returns to a clean dark brand end frame | `ClipFlow` and `Open source. Free.` |

## Motion Language

Use a single continuous master timeline with short overlap between scenes. Prefer transform and opacity animation, smooth power eases, and restrained spring-like overshoot on cards. Transitions should feel like a liquid glass pane sliding across the phone screen, not like a generic wipe. Keep the device content readable for at least 0.8 seconds whenever it carries product information.

## Asset and Capture Contract

- Capture the installed app through ADB only after the composition structure is approved.
- Record only states that can be reproduced locally; do not show real account credentials, cookies, tokens, or private media.
- Use placeholder/demo links that are already safe to display or mask link text in the final composition.
- Keep screenshots at their native aspect ratio inside a fixed phone frame; do not stretch UI.
- Keep the repository's existing screenshots as fallback assets under the video project's `assets/` directory.

## Acceptance Criteria

1. The rendered file is exactly 20 seconds at 1080x1920.
2. The final composition passes `npx hyperframes lint` and layout inspection without unintended overflow.
3. The product name and core flow are legible on a mobile-sized vertical frame.
4. At least one real device capture shows the parse-to-result flow; fallback screenshots are clearly integrated if a state cannot be captured.
5. No Android source, build files, credentials, or private device data are modified or committed.
6. The final output path and HyperFrames Studio preview URL are reported after verification.
