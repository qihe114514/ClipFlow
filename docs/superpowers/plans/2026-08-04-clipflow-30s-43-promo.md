# ClipFlow 30-Second 4:3 Promo Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build and deliver a Chinese 30-second ClipFlow product promo at 1440x1080 and 30 fps with Mandarin narration, locally generated background music, and safe real-device UI captures.

**Architecture:** Keep all promo-specific source and generated media isolated under `promo/clipflow-30s-4x3/`. Use one HyperFrames HTML composition with a paused GSAP master timeline, safe portrait device captures inside fixed phone frames, a separate narration audio track, and a separate music track. Keep Android source and build files untouched.

**Tech Stack:** HyperFrames HTML, GSAP, FFmpeg, Kokoro TTS through `npx hyperframes tts`, PowerShell, Android Debug Bridge.

## Global Constraints

- Canvas is exactly 1440x1080, 4:3.
- Frame rate is exactly 30 fps.
- Duration is exactly 30 seconds.
- Spoken and written language is simplified Chinese.
- Use `DESIGN.md` palette: `#0B0D14`, `#6B9DFC`, `#9B7BFC`, `#00D4D4`, `#FF3B5C`.
- Use only safe device frames; do not capture or ship account identifiers, cookies, tokens, private links, or private media.
- Do not modify Android Kotlin, Gradle, manifest, resource, or build files.
- Run `npx hyperframes lint`, `npx hyperframes validate`, and `npx hyperframes inspect --samples 15 --strict` before final rendering.

---

### Task 1: Create the isolated promo project and capture workflow

**Files:**
- Create: `promo/clipflow-30s-4x3/capture.ps1`
- Create: `promo/clipflow-30s-4x3/assets/.gitkeep`
- Create: `promo/clipflow-30s-4x3/renders/.gitkeep`

**Interfaces:**
- Consumes: authorized `adb` device and installed package `com.qihe.clipflow`.
- Produces: local asset directory and a capture script that fails when no authorized device is present.

- [ ] **Step 1: Create the project directories.**

Run:

```powershell
New-Item -ItemType Directory -Force -Path 'promo/clipflow-30s-4x3/assets' | Out-Null
New-Item -ItemType Directory -Force -Path 'promo/clipflow-30s-4x3/audio' | Out-Null
New-Item -ItemType Directory -Force -Path 'promo/clipflow-30s-4x3/renders' | Out-Null
```

Expected: all three directories exist under `promo/clipflow-30s-4x3/`.

- [ ] **Step 2: Add a narrow ADB capture script.**

The script captures only the visible screen to `assets/device-current.png`. With `-Record`, it records up to 12 seconds, pulls it to `assets/device-flow.mp4`, and removes the temporary device file. It must not dump logs or arbitrary device files.

- [ ] **Step 3: Verify the package and current screen.**

Run:

```powershell
adb devices -l
adb shell pm list packages | Select-String 'qihe|clipflow'
adb shell dumpsys window | Select-String 'mCurrentFocus|mFocusedApp'
```

Expected: one authorized device, `package:com.qihe.clipflow`, and `com.qihe.clipflow.MainActivity` in focus.

- [ ] **Step 4: Commit the capture workflow.**

```powershell
git add -- promo/clipflow-30s-4x3/capture.ps1 promo/clipflow-30s-4x3/assets/.gitkeep promo/clipflow-30s-4x3/renders/.gitkeep
git commit -m "build: add ClipFlow 4:3 promo capture workflow"
```

### Task 2: Capture and vet safe device assets

**Files:**
- Create: `promo/clipflow-30s-4x3/assets/device-home.png`
- Create: `promo/clipflow-30s-4x3/assets/device-douyin.png`
- Create: `promo/clipflow-30s-4x3/assets/device-xiaohongshu.png`
- Create: `promo/clipflow-30s-4x3/assets/device-history.png`
- Copy: `promo/clipflow-30s-4x3/assets/fallback-home.jpg`
- Copy: `promo/clipflow-30s-4x3/assets/fallback-douyin.jpg`
- Copy: `promo/clipflow-30s-4x3/assets/fallback-demo.gif`

**Interfaces:**
- Consumes: installed app, existing repository screenshots, and `capture.ps1`.
- Produces: portrait captures and fallbacks with no private data visible.

- [ ] **Step 1: Launch ClipFlow and capture the home surface.**

Run `adb shell monkey -p com.qihe.clipflow 1`, wait for the activity, capture the screen, inspect it, and rename only after confirming it is safe.

- [ ] **Step 2: Capture parser and history surfaces.**

Navigate on-device to the Douyin parser, Xiaohongshu parser, and history page. Leave fields empty or use a masked demo link. Save only safe frames as `device-douyin.png`, `device-xiaohongshu.png`, and `device-history.png`; use repository fallbacks when a frame contains private data or cannot be reproduced.

- [ ] **Step 3: Copy repository fallbacks without modifying originals.**

Copy `screenshots/user-home.jpg`, `screenshots/user-douyin.jpg`, and `screenshots/clipflow-demo.gif` into the promo asset directory with the fallback names above.

- [ ] **Step 4: Inspect image dimensions and files.**

Run a PowerShell file-size check and visually inspect all captures. Expected: nonzero files, portrait app captures, no credentials or private media.

- [ ] **Step 5: Commit vetted assets.**

```powershell
git add -- promo/clipflow-30s-4x3/assets
git commit -m "feat: add ClipFlow 4:3 promo assets"
```

### Task 3: Generate Mandarin narration and music bed

**Files:**
- Create: `promo/clipflow-30s-4x3/audio/narration.wav`
- Create: `promo/clipflow-30s-4x3/audio/music.wav`
- Create: `promo/clipflow-30s-4x3/audio/mix.wav`
- Create: `promo/clipflow-30s-4x3/audio/narration.txt`

**Interfaces:**
- Consumes: approved narration script and local TTS/FFmpeg tools.
- Produces: narration and music tracks with a 30-second mixed master track.

- [ ] **Step 1: Write the exact narration text.**

Save the approved Chinese narration into `audio/narration.txt`; do not add claims beyond README functionality.

- [ ] **Step 2: Generate Mandarin speech.**

Run `npx hyperframes tts audio/narration.txt --voice zf_xiaobei --lang zh --output audio/narration.wav` after checking `npx hyperframes tts --list` for the installed Mandarin voice name. Expected: a readable WAV with no English translation.

- [ ] **Step 3: Normalize narration duration and volume.**

Use FFmpeg to normalize loudness and pad the narration to the 30-second composition duration while retaining natural speech timing.

- [ ] **Step 4: Generate a local electronic music bed.**

Use deterministic FFmpeg tone layers or an equivalent local synthesis command, with no network audio asset. Keep it unobtrusive and exactly 30 seconds long.

- [ ] **Step 5: Mix narration above music.**

Mix the two WAV tracks with narration at full speech level and music attenuated below it. Confirm the mixed file is 30 seconds and contains two audible source layers.

### Task 4: Author the 1440x1080 HyperFrames composition

**Files:**
- Create: `promo/clipflow-30s-4x3/index.html`

**Interfaces:**
- Consumes: vetted assets and `audio/mix.wav`.
- Produces: composition id `clipflow-30s-4x3`, duration 30, width 1440, height 1080, and one registered paused timeline.

- [ ] **Step 1: Build static scene end states.**

Create five scenes at `0`, `4`, `11`, `19`, and `25` seconds. Use full-size `.scene-content` flex layouts, fixed phone frames with preserved portrait aspect ratio, and safe text widths. Use literal palette colors from `DESIGN.md`.

- [ ] **Step 2: Add audio tracks.**

Add one `<audio>` element for `audio/mix.wav` with `data-start="0"`, `data-duration="30"`, and its own `data-track-index`. Do not use video audio or call media playback methods.

- [ ] **Step 3: Add Chinese copy and caption groups.**

Use short caption groups matching the narration beats. Keep one group visible at a time and hard-kill each group with a timeline `set` at its end.

- [ ] **Step 4: Add distinct entrances and transitions.**

Use `gsap.from()` entrances with varied eases and motions per scene. Use liquid-glass crossfade/slide overlays for related scene changes and a faster accent at the platform section. Do not pre-fade outgoing scenes before transitions; only the final scene may fade out.

- [ ] **Step 5: Register and lint the timeline.**

Register `window.__timelines['clipflow-30s-4x3'] = tl` synchronously with `tl` paused. Run `npx hyperframes lint` and fix all errors before proceeding.

### Task 5: Inspect, render, and verify final delivery

**Files:**
- Create: `promo/clipflow-30s-4x3/renders/ClipFlow-30s-4x3.mp4`
- Modify: `promo/clipflow-30s-4x3/index.html` only if inspection finds a real layout issue.

**Interfaces:**
- Consumes: linted composition and audio/assets.
- Produces: final MP4 and verified source tree.

- [ ] **Step 1: Run environment diagnostics.**

Run `npx hyperframes doctor` from `promo/clipflow-30s-4x3`. Expected: Node 22+, Chrome, and FFmpeg are available.

- [ ] **Step 2: Run validation and layout inspection.**

Run:

```powershell
npx hyperframes validate
npx hyperframes inspect --samples 15 --strict
```

Expected: no validation errors and no unintended canvas, clipping, or text overflow.

- [ ] **Step 3: Render a draft at 30 fps.**

Run `npx hyperframes render --output renders/ClipFlow-30s-4x3-draft.mp4 --quality draft --fps 30`. Review opening, parser, result, platform, and closing timestamps.

- [ ] **Step 4: Render the final MP4.**

Run `npx hyperframes render --output renders/ClipFlow-30s-4x3.mp4 --quality high --fps 30 --strict`.

- [ ] **Step 5: Probe final media properties.**

Run:

```powershell
ffprobe -v error -show_entries format=duration:stream=width,height,r_frame_rate,codec_name -of default=noprint_wrappers=1 renders/ClipFlow-30s-4x3.mp4
```

Expected: duration is 30 seconds within one frame, width 1440, height 1080, `r_frame_rate=30/1`, and H.264 video.

- [ ] **Step 6: Run final repository checks.**

Run `git diff --check`, `npx hyperframes lint`, `npx hyperframes validate`, `npx hyperframes inspect --samples 15 --strict`, and `git status --short`. Confirm only the design, plan, and promo deliverable paths changed; Android source changes remain pre-existing and untouched.
