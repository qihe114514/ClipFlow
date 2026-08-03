# ClipFlow 20-Second Promo Video Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build and verify a 20-second 1080x1920 HyperFrames product promo for ClipFlow using real Android device captures, the repository's app screenshots, and a dark fluid-tech motion system.

**Architecture:** Keep the video project isolated under `promo/clipflow-20s/`. `capture.ps1` handles reproducible ADB screenshots and recording only; `assets/` contains copied or captured media; `index.html` is the single HyperFrames composition source with five timed scene groups and one GSAP master timeline. Rendered files go under `promo/clipflow-20s/renders/` and are not required to affect the Android project.

**Tech Stack:** HyperFrames HTML, GSAP, CSS, Node.js 22, HyperFrames CLI via `npx`, PowerShell, Android Debug Bridge.

## Global Constraints

- Output is exactly 1080x1920 vertical and 20 seconds long.
- Use the existing ClipFlow visual identity in `DESIGN.md`: `#0B0D14`, `#6B9DFC`, `#9B7BFC`, `#00D4D4`, and `#FF3B5C`.
- Use real device captures as the primary product evidence and repository screenshots as explicit fallback assets.
- Do not capture or commit credentials, cookies, private links, account data, or private media.
- Do not modify Android Kotlin, Gradle, manifest, resources, or build outputs.
- Verify with `npx hyperframes lint`, `npx hyperframes inspect`, and a rendered MP4 probe before claiming completion.

---

### Task 1: Create the video project and capture workflow

**Files:**
- Create: `promo/clipflow-20s/capture.ps1`
- Create: `promo/clipflow-20s/assets/.gitkeep`

**Interfaces:**
- Consumes: connected Android device selected by `adb devices`.
- Produces: stable capture paths `assets/device-home.png`, `assets/device-douyin.png`, `assets/device-xiaohongshu.png`, `assets/device-history.png`, and optional `assets/device-flow.mp4`.

- [ ] **Step 1: Confirm the device and package without changing device state**

Run from `E:\clioipflow\ClipFlow`:

```powershell
adb devices
adb shell pm list packages | Select-String 'qihe|clipflow'
```

Expected: one device is listed as `device`, and the installed package is `com.qihe.clipflow`.

- [ ] **Step 2: Add a narrow ADB capture script**

The script should create the local `assets/` directory, capture only the current visible screen, and optionally record a short local screen video. It must fail if no device is connected and must not dump device files or logs.

```powershell
param([switch]$Record)
$ErrorActionPreference = 'Stop'
$assetDir = Join-Path $PSScriptRoot 'assets'
New-Item -ItemType Directory -Force -Path $assetDir | Out-Null
$device = adb get-state 2>$null
if ($device -ne 'device') { throw 'No authorized Android device is connected.' }
adb exec-out screencap -p > (Join-Path $assetDir 'device-current.png')
if ($Record) {
    adb shell screenrecord --time-limit 12 /sdcard/clipflow-promo.mp4
    adb pull /sdcard/clipflow-promo.mp4 (Join-Path $assetDir 'device-flow.mp4') | Out-Null
    adb shell rm /sdcard/clipflow-promo.mp4
}
```

- [ ] **Step 3: Capture the current app state and inspect dimensions**

Run:

```powershell
pwsh -File promo/clipflow-20s/capture.ps1
```

Expected: `promo/clipflow-20s/assets/device-current.png` exists and is a portrait screenshot. Rename or copy it to the scene-specific path only after visually checking that no private data is visible.

- [ ] **Step 4: Commit the capture workflow**

```powershell
git add promo/clipflow-20s/capture.ps1 promo/clipflow-20s/assets/.gitkeep
git commit -m "build: add ClipFlow promo capture workflow"
```

### Task 2: Collect and vet product assets

**Files:**
- Create: `promo/clipflow-20s/assets/device-home.png`
- Create: `promo/clipflow-20s/assets/device-douyin.png`
- Create: `promo/clipflow-20s/assets/device-xiaohongshu.png`
- Create: `promo/clipflow-20s/assets/device-history.png`
- Copy: `promo/clipflow-20s/assets/fallback-home.jpg` from `screenshots/user-home.jpg`
- Copy: `promo/clipflow-20s/assets/fallback-douyin.jpg` from `screenshots/user-douyin.jpg`
- Copy: `promo/clipflow-20s/assets/fallback-demo.gif` from `screenshots/clipflow-demo.gif`

**Interfaces:**
- Consumes: the connected device and existing repository screenshots.
- Produces: a vetted asset set whose names are referenced directly by `index.html`.

- [ ] **Step 1: Capture a clean home screen**

Open the installed app with `adb shell monkey -p com.qihe.clipflow 1`, wait for the home screen, and run `capture.ps1`. Save the result as `device-home.png`. Confirm the frame contains no account identifier or private media.

- [ ] **Step 2: Capture parser surfaces**

Use the device UI to navigate to Douyin and Xiaohongshu parser pages, leaving the URL field empty or using a masked demo URL. Capture each page as `device-douyin.png` and `device-xiaohongshu.png`. Capture the history page as `device-history.png` if it contains only safe local demo entries; otherwise use `fallback-home.jpg` for the close.

- [ ] **Step 3: Copy repository fallbacks**

Copy the three named screenshots into the project assets directory. Preserve the originals in `screenshots/` and do not modify them.

- [ ] **Step 4: Inspect all media dimensions and visibility**

Run a PowerShell image-dimension check and visually inspect the four device captures plus fallbacks. Expected: portrait device images remain portrait, no asset is zero bytes, and no private content is visible.

- [ ] **Step 5: Commit the vetted assets**

```powershell
git add promo/clipflow-20s/assets
git commit -m "feat: add ClipFlow promo assets"
```

### Task 3: Author the HyperFrames composition

**Files:**
- Create: `promo/clipflow-20s/index.html`

**Interfaces:**
- Consumes: `DESIGN.md` and the vetted assets from Task 2.
- Produces: a HyperFrames composition with a 20-second duration, five scene groups, and a GSAP master timeline.

- [ ] **Step 1: Add the static end-state layout**

Create a 1080x1920 composition with a full-canvas `.scene-content`, a fixed `.phone-frame` whose inner screen preserves portrait device aspect ratio, and five scene sections whose hero positions are correct before animation. Keep decorative light streaks and glass panes marked with `data-layout-ignore`; do not mark product UI or readable copy as ignored.

- [ ] **Step 2: Add the scene structure and copy**

Use the approved copy exactly:

```text
Turn shared links into saved files
Paste. Parse.
See the result. Save what you need.
Douyin / Xiaohongshu / Bilibili
ClipFlow
Open source. Free.
```

Use a Chinese-free copy set for reliable rendering and keep platform names in Latin text. The product screenshots remain the source of truth for the app UI.

- [ ] **Step 3: Add the GSAP master timeline**

Implement a paused timeline and let HyperFrames control playback. The timeline must place the five beats at `0`, `2`, `7`, `12`, and `16` seconds, with scene overlap for transitions and a final fade ending at `20`. Use `autoAlpha`, `x`, `y`, `scale`, and `rotation` for motion; do not animate `top`, `left`, `width`, or `height`.

- [ ] **Step 4: Add liquid glass transitions and accessibility defaults**

Use CSS pseudo-elements or decorative DOM layers for soft light passes, glass borders, and clipped color ribbons. Add `prefers-reduced-motion` handling that sets animation duration to zero while leaving all product content visible. Keep contrast high enough that copy remains legible over the dark canvas.

- [ ] **Step 5: Run syntax and lint validation**

From `promo/clipflow-20s` run:

```powershell
npx hyperframes lint
npx hyperframes inspect --samples 15 --strict
```

Expected: no lint errors and no unintended text, canvas, or clipping overflow.

### Task 4: Preview and correct visual layout

**Files:**
- Modify: `promo/clipflow-20s/index.html`

**Interfaces:**
- Consumes: the authored composition from Task 3.
- Produces: a previewable composition with verified hero frames at `0`, `3`, `8`, `13`, `17`, and `19` seconds.

- [ ] **Step 1: Start the HyperFrames preview server**

Run:

```powershell
npx hyperframes preview --port 3027
```

Expected: the server starts on port `3027` and exposes the Studio project at `http://localhost:3027/#project/clipflow-20s`.

- [ ] **Step 2: Inspect fixed timestamps**

Run:

```powershell
npx hyperframes inspect --at 0,3,8,13,17,19 --strict
```

Expected: the phone frame, text blocks, platform labels, and final CTA stay within the 1080x1920 canvas at every sampled hero frame.

- [ ] **Step 3: Review a draft render**

Run:

```powershell
npx hyperframes render --output renders/clipflow-20s-draft.mp4 --quality draft --fps 30
```

Check the draft at the opening, parse state, result state, platform transition, and closing frame. Correct only timing, scale, contrast, or overlap defects found in those frames.

- [ ] **Step 4: Commit composition corrections**

```powershell
git add promo/clipflow-20s/index.html
git commit -m "feat: create ClipFlow promo composition"
```

### Task 5: Render and verify final delivery

**Files:**
- Create: `promo/clipflow-20s/renders/ClipFlow-20s.mp4`

**Interfaces:**
- Consumes: the linted and previewed composition.
- Produces: the final MP4 and verification evidence.

- [ ] **Step 1: Check the render environment**

Run `npx hyperframes doctor` from `promo/clipflow-20s`. Expected: Node, Chrome, and FFmpeg are available. Resolve only environment issues that prevent rendering.

- [ ] **Step 2: Render the final MP4**

Run:

```powershell
npx hyperframes render --output renders/ClipFlow-20s.mp4 --quality high --fps 30 --strict
```

Expected: an MP4 is produced at `promo/clipflow-20s/renders/ClipFlow-20s.mp4`.

- [ ] **Step 3: Verify duration and dimensions with FFprobe**

Run:

```powershell
ffprobe -v error -show_entries format=duration:stream=width,height,r_frame_rate -of default=noprint_wrappers=1 renders/ClipFlow-20s.mp4
```

Expected: duration is `20.000000` or within one frame of 20 seconds, width is `1080`, height is `1920`, and frame rate is `30/1`.

- [ ] **Step 4: Run final repository checks**

Run:

```powershell
npx hyperframes lint
npx hyperframes inspect --samples 15 --strict
git diff --check
git status --short
```

Expected: all checks pass; only intended promo files are changed; no Android source or secret files appear.

- [ ] **Step 5: Commit the final render metadata and source**

```powershell
git add promo/clipflow-20s
git commit -m "feat: deliver ClipFlow 20-second promo video"
```

