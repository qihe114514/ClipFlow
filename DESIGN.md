# ClipFlow Visual Identity

## Style Prompt

A practical Android media utility presented through liquid glass: real app captures, translucent surfaces, wallpaper-driven color, and restrained motion. The interface should read as fast and precise, while small-screen text and controls stay legible over photographic wallpapers.

The reference is the running app, not an invented concept screen. Every promo frame should be traceable to a real ClipFlow screen at the current version.

## Visual System

- **Surface**: Kyant Backdrop glass for the top bar, bottom bar, cards, dialogs, download panels and controls. Dialogs and sheets render their own local backdrop; they never sample the activity behind their window.
- **Shape**: continuous-curvature (G2 / squircle) corners for cards, inputs, thumbnails, dialogs and grouped containers. No mixed rounded-rectangle styles.
- **Icons**: Lucide line icons at a 24dp / 2dp-stroke rhythm for interface actions. The bottom bar uses line-drawn Douyin and Bilibili brand glyphs at the same weight; home and history cards use filled brand marks for recognition.
- **Color**: dynamic color extracted from the visible wallpaper. With the wallpaper off, Android 12+ falls back to system Monet while Android 10 / 11 uses the built-in brand palette. Platform accents remain recognizable: Douyin cyan, Xiaohongshu red, Bilibili blue.
- **Wallpaper**: user images sit behind the UI with adjustable opacity and contrast. When the wallpaper is off, cards become solid theme surfaces with a subtle solid border for contrast.
- **Motion**: parallax between primary tabs, scale/fade for secondary pages, spring feedback on tab icons, press feedback on cards, and predictive-back transitions on Android 14+. Motion should explain hierarchy, not decorate idle screens.

## Colors

- `#0B0D14` — dark canvas
- `#6B9DFC` — ClipFlow blue
- `#9B7BFC` — ClipFlow purple
- `#00D4D4` — Douyin accent
- `#FF3B5C` — Xiaohongshu accent
- `#00A1D6` — Bilibili accent

Dynamic color may replace the blue/purple pair on themed screens; keep platform accents stable.

## Typography

- System sans-serif stack (Roboto / device default).
- Semibold display text for page titles and card headings; regular body and captions.
- No condensed, decorative or all-caps display faces.
- Text over wallpapers uses adaptive glass chips or a local scrim, never a bare shadow.

## Layout

- 20dp page gutters; grouped cards separated by section labels with glass chips.
- Buttons keep at least 8dp spacing from adjacent controls; primary actions span the content width.
- Bottom bar respects gesture navigation insets and stays readable over bright wallpapers.
- Secondary pages use a back affordance in the top-left and a single right-aligned action cluster.

## Screenshot and Capture Rules

- Capture at a mainstream 1.5K profile: `1272 × 2772`, `450 dpi` (OnePlus 15-class panel), then downscale to 1000px width for the repository.
- Keep the status bar and system insets in frame; do not crop the app UI to hide content.
- Use a user-owned or self-created wallpaper only; the repository demo uses a generated abstract gradient. Avoid private links, account names, cookies, avatars and notification content.
- Prefer empty or demo states over real personal history.

## What NOT to Do

- Do not use a generic blue gradient as the entire background.
- Do not invent screens, features or measurements that contradict the Android app.
- Do not stretch, crop or fake device frames.
- Do not use oversized text that covers the interface.
- Do not show private links, account data, cookies or tokens.
