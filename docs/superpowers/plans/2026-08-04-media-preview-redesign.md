# ClipFlow Media Preview Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (\`- [ ]\`) syntax for tracking.

**Goal:** Replace the cover-click preview for Douyin, Xiaohongshu, and Bilibili with one Compose bottom-sheet preview that supports video, image galleries, custom controls, speed selection, long-press 2x playback, and fullscreen orientation behavior.

**Architecture:** Keep the existing \`ContentItem\` and \`ParseResult\` contracts. Add a pure preview-item filter, a focused \`MediaPreviewSheet\` that owns one ExoPlayer session, and a small \`ParseInfoCard\` integration point. The sheet uses official Media3 Compose UI for the video surface and ClipFlow Compose controls for all interactions.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, AndroidX Media3 \`1.10.0\`, Coil \`HorizontalPager\`, existing \`ContentItem\`/\`ParseResult\`, JUnit 4, Gradle offline verification.

## Global Constraints

- Scope is Douyin, Xiaohongshu, and Bilibili; keep their parsing, downloading, history, and navigation behavior unchanged.
- All Media3 modules use \`1.10.0\`; add \`media3-ui-compose\` and use official Compose video surface APIs.
- \`VIDEO\` and \`LIVE_VIDEO\` may create ExoPlayer; \`IMAGE\`, \`LIVE_IMAGE\`, and \`AUDIO\` must never create ExoPlayer.
- The first preview is a partially expanded \`ModalBottomSheet\`; half-screen does not change orientation or system bars.
- Fullscreen hides system bars; only a landscape video requests landscape. Exiting fullscreen restores the orientation and system-bar state captured on entry.
- Video opens with autoplay and system volume, starts at \`1x\`, offers \`0.5x / 1x / 1.25x / 1.5x / 2x\`, and uses temporary \`2x\` while the video surface is held.
- Do not add libmpv, Compose Multiplatform media abstractions, picture-in-picture, lock controls, background playback, caching, or download behavior.

---

### Task 1: Add Pure Preview Item Classification

**Files:**
- Create: \`app/src/main/java/com/qihe/clipflow/ui/components/MediaPreviewModel.kt\`
- Test: \`app/src/test/java/com/qihe/clipflow/ui/components/MediaPreviewModelTest.kt\`

**Interfaces:**
- Consumes: \`com.qihe.clipflow.data.api.model.ContentItem\` and \`ContentType\`.
- Produces: \`ContentItem.previewKind: PreviewMediaKind?\` and \`List<ContentItem>.previewableItems()\` for the sheet and callers.

- [ ] **Step 1: Write the failing classification tests**

Add JUnit tests covering the exact media boundary:

\`\`\`kotlin
class MediaPreviewModelTest {
    @Test
    fun \`video and live video are video previews\`() {
        assertEquals(PreviewMediaKind.VIDEO, item(ContentType.VIDEO).previewKind)
        assertEquals(PreviewMediaKind.VIDEO, item(ContentType.LIVE_VIDEO).previewKind)
    }

    @Test
    fun \`images and live images are image previews\`() {
        assertEquals(PreviewMediaKind.IMAGE, item(ContentType.IMAGE).previewKind)
        assertEquals(PreviewMediaKind.IMAGE, item(ContentType.LIVE_IMAGE).previewKind)
    }

    @Test
    fun \`audio is excluded from preview\`() {
        assertNull(item(ContentType.AUDIO).previewKind)
        assertEquals(listOf("video", "image"), listOf(
            item(ContentType.VIDEO, "video"),
            item(ContentType.AUDIO, "audio"),
            item(ContentType.IMAGE, "image")
        ).previewableItems().map { it.id })
    }
}
\`\`\`

Use a local \`item(type, id)\` helper with valid \`ContentItem\` fields so the tests do not depend on parser responses.

- [ ] **Step 2: Run the focused test and verify it fails**

Run:

\`\`\`powershell
.\\gradlew.bat --offline --no-daemon --no-watch-fs --console=plain :app:testDebugUnitTest --tests com.qihe.clipflow.ui.components.MediaPreviewModelTest
\`\`\`

Expected: compilation fails because \`PreviewMediaKind\`, \`previewKind\`, and \`previewableItems\` do not exist.

- [ ] **Step 3: Implement the minimal pure model**

Create \`MediaPreviewModel.kt\` with exactly this behavior:

\`\`\`kotlin
enum class PreviewMediaKind { VIDEO, IMAGE }

val ContentItem.previewKind: PreviewMediaKind?
    get() = when (type) {
        ContentType.VIDEO, ContentType.LIVE_VIDEO -> PreviewMediaKind.VIDEO
        ContentType.IMAGE, ContentType.LIVE_IMAGE -> PreviewMediaKind.IMAGE
        ContentType.AUDIO -> null
    }

fun List<ContentItem>.previewableItems(): List<ContentItem> = filter {
    it.url.isNotBlank() && it.previewKind != null
}
\`\`\`

- [ ] **Step 4: Run the focused test and verify it passes**

Run the same \`:app:testDebugUnitTest --tests ...MediaPreviewModelTest\` command. Expected: all three tests pass.

- [ ] **Step 5: Commit the pure boundary**

\`\`\`powershell
git add -- app/src/main/java/com/qihe/clipflow/ui/components/MediaPreviewModel.kt app/src/test/java/com/qihe/clipflow/ui/components/MediaPreviewModelTest.kt
git commit -m "test: define media preview item boundary"
\`\`\`

### Task 2: Build the Single-Session Media Preview Sheet

**Files:**
- Modify: \`app/build.gradle.kts\`
- Create: \`app/src/main/java/com/qihe/clipflow/ui/components/MediaPreviewSheet.kt\`

**Interfaces:**
- Consumes: \`List<ContentItem>\` already filtered by \`previewableItems()\`, \`onDismiss: () -> Unit\`.
- Produces: \`@Composable fun MediaPreviewSheet(items: List<ContentItem>, onDismiss: () -> Unit)\`.

- [ ] **Step 1: Align Media3 dependencies**

Change the three existing Media3 dependencies to \`1.10.0\` and add:

\`\`\`kotlin
implementation("androidx.media3:media3-ui-compose:1.10.0")
implementation("androidx.media3:media3-ui-compose-material3:1.10.0")
\`\`\`

Keep all Media3 modules on the same version. Use \`media3-ui-compose-material3\` only for the official surface/container APIs that compile against the current Compose BOM; if the exact Material 3 module is absent from the resolved version, use \`media3-ui-compose\` plus the existing Material 3 controls and record the resolution in the implementation diff.

- [ ] **Step 2: Add the sheet shell and lifecycle**

Implement \`MediaPreviewSheet\` with:

\`\`\`kotlin
@Composable
fun MediaPreviewSheet(
    items: List<ContentItem>,
    onDismiss: () -> Unit
)
\`\`\`

Use \`rememberModalBottomSheetState(skipPartiallyExpanded = false)\`. Show the sheet at partial expansion on first composition, expand it when fullscreen is selected, and call \`onDismiss\` from \`onDismissRequest\` after restoring orientation/system bars. Keep one \`ExoPlayer?\` remembered for the current video item, release it in \`DisposableEffect\`, and never create one for image items.

- [ ] **Step 3: Add the video surface and playback state**

For a \`PreviewMediaKind.VIDEO\` item, create one \`ExoPlayer\` with \`MediaItem.fromUri(item.url)\`, call \`prepare()\`, and set \`playWhenReady = true\`. Render the official Media3 Compose surface with a \`TextureView\`-compatible configuration so the Compose overlay can clip and animate it. Track \`isPlaying\`, \`isMuted\`, \`duration\`, \`position\`, \`bufferedPosition\`, and \`playerError\` from the player listener plus a 250 ms position ticker.

Add custom Compose controls:

- center play/pause button;
- bottom progress slider with current/duration labels;
- mute/unmute button that stores the pre-mute volume;
- speed menu with \`0.5f\`, \`1f\`, \`1.25f\`, \`1.5f\`, \`2f\`;
- fullscreen/exit-fullscreen icon button;
- close icon button;
- error state with retry button that calls \`prepare()\` and resumes autoplay.

Controls must be layered over the surface, auto-hide after playback starts, and stay visible while scrubbing or while a speed menu is open.

- [ ] **Step 4: Add temporary long-press 2x behavior**

Attach an \`awaitEachGesture\`/\`tryAwaitRelease\` gesture to the video surface. On a completed long press, save the selected speed, call \`player.setPlaybackSpeed(2f)\`, show a transient \`2x\` indicator, and restore the saved speed on release or cancellation. A normal tap only toggles controls and must not change the selected speed.

- [ ] **Step 5: Add image gallery content**

For a \`PreviewMediaKind.IMAGE\` item, render the ordered image items with \`HorizontalPager\`, Coil \`AsyncImage\`, a page indicator, and a retryable per-page error state. The image branch must not instantiate or reference an ExoPlayer. Fullscreen for images expands the sheet and hides system bars but does not request landscape.

- [ ] **Step 6: Add fullscreen orientation and system-bar handling**

Capture \`activity.requestedOrientation\` and the current \`WindowInsetsControllerCompat\` visibility before entering fullscreen. Hide system bars only while fullscreen. When the active video reports \`videoSize.width > videoSize.height\`, request \`SCREEN_ORIENTATION_SENSOR_LANDSCAPE\`; keep portrait video in the current orientation. Restore captured values on exit and on sheet dismissal, including dismissal through back press or outside tap.

- [ ] **Step 7: Compile the component before integration**

Run:

\`\`\`powershell
.\\gradlew.bat --offline --no-daemon --no-watch-fs --console=plain :app:compileDebugKotlin
\`\`\`

Expected: the new sheet and dependency update compile before the callers are changed; any Media3 API opt-in errors must be fixed inside \`MediaPreviewSheet.kt\`.

- [ ] **Step 8: Commit the sheet**

\`\`\`powershell
git add -- app/build.gradle.kts app/src/main/java/com/qihe/clipflow/ui/components/MediaPreviewSheet.kt
git commit -m "feat: add compose media preview sheet"
\`\`\`

### Task 3: Connect the Three Result Cards

**Files:**
- Modify: \`app/src/main/java/com/qihe/clipflow/ui/components/ParseInfoCard.kt\`
- Modify: \`app/src/main/java/com/qihe/clipflow/ui/parser/PlatformParseScreenContent.kt\`
- Modify: \`app/src/main/java/com/qihe/clipflow/ui/parser/PlatformParseViewModel.kt\`
- Modify: \`app/src/main/java/com/qihe/clipflow/ui/bilibili/BilibiliScreen.kt\`

**Interfaces:**
- Consumes: \`previewableItems()\` from Task 1 and \`MediaPreviewSheet\` from Task 2.
- Produces: cover clicks that open the same preview sheet for Douyin, Xiaohongshu, and Bilibili.

- [ ] **Step 1: Replace the old player parameters and dialogs**

In \`ParseInfoCard\`, replace \`videoUrl: String = ""\` with \`previewItems: List<ContentItem> = emptyList()\`. Add:

\`\`\`kotlin
var showMediaPreview by remember { mutableStateOf(false) }
val previewItems = previewItems.previewableItems()

if (showMediaPreview) {
    MediaPreviewSheet(
        items = previewItems,
        onDismiss = { showMediaPreview = false }
    )
}
\`\`\`

Change cover click to \`onClick = { if (previewItems.isNotEmpty()) showMediaPreview = true }\`. Keep the existing long-click cover save dialog. Delete \`InlineVideoDialog\`, \`FullscreenVideoDialog\`, and their now-unused \`PlayerView\`, orientation, dialog, and system-bar imports from this file.

- [ ] **Step 2: Pass parser items from the shared Douyin/Xiaohongshu screen**

In \`PlatformParseScreenContent.kt\`, pass \`previewItems = items\` to \`ParseInfoCard\`. Remove the obsolete \`videoUrl = uiState.videoUrl\` argument. If \`videoUrl\` has no remaining references after this change, remove it from \`ParsePageUiState\` and from the reset/success assignments in \`PlatformParseViewModel.kt\` so an image item can never be treated as a video URL.

- [ ] **Step 3: Pass Bilibili result items**

In \`BilibiliScreen.kt\`, pass \`previewItems = result.items\` to its \`ParseInfoCard\` call. Do not modify Bilibili part selection, quality cards, parser calls, or download callbacks.

- [ ] **Step 4: Run focused callers and compile**

Run:

\`\`\`powershell
.\\gradlew.bat --offline --no-daemon --no-watch-fs --console=plain :app:testDebugUnitTest --tests com.qihe.clipflow.ui.components.MediaPreviewModelTest
.\\gradlew.bat --offline --no-daemon --no-watch-fs --console=plain :app:compileDebugKotlin
\`\`\`

Expected: the classification tests pass and all three callers compile with no old \`InlineVideoDialog\`, \`FullscreenVideoDialog\`, or \`videoUrl\` preview references remaining.

- [ ] **Step 5: Commit the integration**

\`\`\`powershell
git add -- app/src/main/java/com/qihe/clipflow/ui/components/ParseInfoCard.kt app/src/main/java/com/qihe/clipflow/ui/parser/PlatformParseScreenContent.kt app/src/main/java/com/qihe/clipflow/ui/parser/PlatformParseViewModel.kt app/src/main/java/com/qihe/clipflow/ui/bilibili/BilibiliScreen.kt
git commit -m "feat: connect media preview to parser results"
\`\`\`

### Task 4: Full Verification and Acceptance

**Files:**
- Verify: all changed files from Tasks 1-3
- No new source files unless a compiler error identifies a required import or API adapter inside the existing component boundary.

**Interfaces:**
- Consumes: the completed preview sheet and all three platform callers.
- Produces: evidence for unit tests, build output, diff cleanliness, and manual acceptance checklist.

- [ ] **Step 1: Search for stale preview paths**

Run:

\`\`\`powershell
rg -n "InlineVideoDialog|FullscreenVideoDialog|PlayerView|videoUrl|previewableItems|MediaPreviewSheet" app/src/main/java/com/qihe/clipflow
\`\`\`

Expected: \`MediaPreviewSheet\` and \`previewableItems\` are present; the old dialog names are absent; any remaining \`PlayerView\` is unrelated to the new preview or is an intentional compatibility surface documented in the diff.

- [ ] **Step 2: Run all JVM tests**

\`\`\`powershell
.\\gradlew.bat --offline --no-daemon --no-watch-fs --console=plain :app:testDebugUnitTest
\`\`\`

Expected: exit code 0 and zero failed tests.

- [ ] **Step 3: Build debug and release**

\`\`\`powershell
.\\gradlew.bat --offline --no-daemon --no-watch-fs --console=plain :app:assembleDebug :app:assembleRelease
\`\`\`

Expected: both tasks complete successfully. If dependency resolution fails because Media3 \`1.10.0\` is not cached, report the exact dependency error and do not claim offline build success.

- [ ] **Step 4: Run repository checks**

\`\`\`powershell
git diff --check
git status --short --branch
\`\`\`

Expected: no whitespace errors and only the intended commits/working-tree state.

- [ ] **Step 5: Complete manual acceptance**

On a device or emulator, verify all of the following for Douyin, Xiaohongshu, and Bilibili:

1. Video cover opens a partial sheet, autoplay has sound, controls work, seeking works, mute restores volume, speed menu works, holding the video shows temporary \`2x\`, and releasing restores the selected speed.
2. Fullscreen hides system bars; a landscape video rotates to landscape; exiting restores the prior orientation and bars.
3. Image cover opens the same sheet, pages horizontally, shows the page indicator, and never shows video controls or creates a player.
4. Close/back/outside dismissal releases playback and leaves parsing, downloads, history, Bilibili part selection, and quality downloads unchanged.

- [ ] **Step 6: Commit any verification-only fixes**

If verification finds a real defect, add a focused fix and rerun the smallest reproducing command before committing it with a message such as \`fix: stabilize media preview lifecycle\`. Do not modify unrelated existing dead code.

## Plan Self-Review

- Spec coverage: three-platform scope, Media3 activity requirement, one-player lifecycle, video/image split, controls, speed behavior, fullscreen orientation, error states, no-regression boundaries, unit tests, builds, and manual acceptance all have explicit tasks.
- Placeholder scan: no unfinished-work marker or unspecified implementation step is required; the only conditional dependency step records the exact fallback if the Material 3 artifact is unavailable.
- Type consistency: Task 1 exports \`PreviewMediaKind\`, \`ContentItem.previewKind\`, and \`previewableItems\`; Task 2 consumes filtered \`ContentItem\` values; Task 3 passes \`previewItems\` into the exact sheet signature.
