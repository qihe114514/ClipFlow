# Progressive Parser Content Blur Design

## Goal

Add a persistent, top-originating progressive blur to ClipFlow parser content.
The Douyin, Xiaohongshu, and Bilibili input/result content should become most
blurred directly below the top app bar and gradually return to full clarity
lower on the page. The top app bar title, history action, and settings action
must remain sharp.

When a floating bottom-bar gesture or a foreground dialog is active, increase
the content blur strength without changing the gradient direction or layout.

## Boundaries

- Blur the parser input, loading/error state, parsed result, and download option
  content only.
- Keep the top app bar, its actions, download/privacy/tutorial foreground
  surfaces, and the wallpaper outside the blur layer.
- Apply the same content treatment to the Bilibili LazyColumn.
- Keep the existing API 29-32 compatibility boundary: do not initialize
  RuntimeShader or Miuix liquid resources on unsupported APIs.

## Rendering

Create a focused composable/effect for the parser content blur. It will be
drawn above the parser scroll content and below page-local foreground dialogs.
On API 33+, use the existing Miuix Backdrop and RuntimeShader infrastructure.
The shader uses a fixed number of content samples and derives its blur radius
from the normalized vertical coordinate, with the maximum radius at the top
and a smooth falloff toward the bottom. The interaction strength is a uniform
multiplier clamped to a small, stable range.

On API 29-32, render the same top-to-bottom region as a translucent gradient
overlay. This preserves the visual hierarchy without constructing unsupported
real-time blur resources.

## State flow

- The parser content starts at the base strength.
- Bottom-bar press/drag progress is lifted to the navigation host and mapped
  to an additional strength contribution.
- A parser download dialog raises the strength behind the dialog.
- The global privacy dialog raises the strength through the navigation host.
- Foreground dialogs remain outside the blur composable, so their text and
  controls stay sharp.

## Changes

- Add the progressive blur effect and its shader/fallback implementation in the
  existing liquid/component area.
- Pass the root Backdrop and interaction strength only to parser page content.
- Add the same layer to Bilibili's LazyColumn-based page.
- Expose only the existing bottom-bar press progress needed by the navigation
  host; do not add a new settings preference.

## Validation

- Unit-test blur strength clamping and the API capability branch.
- Verify parser pages use the blur layer while the top-bar composable remains
  outside it by source-level structure and compilation.
- Run `git diff --check`.
- Run cached offline `:app:testDebugUnitTest` and `:app:assembleDebug`.

