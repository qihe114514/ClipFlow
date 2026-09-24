# ClipFlow Liquid Buttons Implementation Plan

**Goal:** Replace ClipFlow's interactive buttons with the AndroidLiquidGlass example's transparent or tinted liquid button, while leaving the media preview dialog container unchanged and converting its playback controls and progress bar as requested.

**Architecture:** Add one reusable `LiquidButton` based directly on the upstream Buttons page implementation and reuse the existing `Backdrop` from the app shell. Replace Material3 button call sites in place, using the blue tint only for settings, history, parse-route switching, parse, and paste actions. Keep the preview sheet as a dialog/sheet and change only its controls to liquid buttons plus a Material3 `Slider`.

**Tech Stack:** Kotlin, Jetpack Compose Material3, `io.github.kyant0:backdrop`, `io.github.kyant0:shapes`.

## Global Constraints

- Preserve all existing click handlers, enabled states, navigation, and selection state.
- Do not reintroduce Miuix or add a second liquid-glass dependency.
- Do not replace the media preview dialog container.
- Keep the existing bottom tabs component and current width adjustment unchanged.

### Task 1: Add the shared liquid button

**Files:**
- Create: `app/src/main/java/com/qihe/clipflow/ui/component/LiquidButton.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/ui/component/InteractiveHighlight.kt` only if the upstream API needs an adapter.

- [ ] Copy the upstream `LiquidButton` behavior and adapt only package/import paths to the existing project.
- [ ] Verify the component supports transparent, tinted, non-interactive, enabled, and icon-only call sites without changing the upstream 48dp height and 16dp horizontal padding.

### Task 2: Replace app buttons

**Files:**
- Modify: `navigation/NavigationChrome.kt`, parser screens/cards, settings, history, about, Bilibili account/login, dialogs, and shared parse-result components.

- [ ] Replace `Button`, `TextButton`, `OutlinedButton`, `IconButton`, and `FilledTonalIconButton` call sites with `LiquidButton` content while preserving handlers and compact icon sizing.
- [ ] Apply the blue tint to settings, history, parse-route switch, parse, and paste actions; use transparent styling for all other buttons.
- [ ] Keep segmented-route selection semantics by rendering each option as a liquid button and preserving the selected route state.

### Task 3: Update media preview controls

**Files:**
- Modify: `app/src/main/java/com/qihe/clipflow/ui/components/MediaPreviewSheet.kt`.

- [ ] Leave the preview dialog/sheet container and media surface unchanged.
- [ ] Replace play/pause, mute, speed, and other playback controls with transparent liquid buttons.
- [ ] Replace the playback progress control with `Slider`, preserving seek callbacks and current position updates.

### Task 4: Verify

- [ ] Run `:app:testDebugUnitTest :app:assembleDebug` with the cached offline Gradle 9.6 workflow.
- [ ] Run `git diff --check` and search for remaining Material3 button call sites.
- [ ] Confirm the existing `NavigationChrome.kt` width change remains present and no Miuix dependency is added.
