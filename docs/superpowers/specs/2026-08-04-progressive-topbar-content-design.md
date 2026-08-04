# Progressive Topbar Content Design

## Goal

Remove the hard visual boundary created by the top app bar. Parser content must
continue underneath the topbar, while the topbar title, history action, and
settings action remain sharp.

## Layout

The root navigation layout owns three layers:

1. A captured content layer containing the wallpaper, parser pager, and route
   content. It draws edge-to-edge and is not padded by the Scaffold topbar.
2. A topbar backdrop layer positioned above the content. It samples the
   captured content and applies the existing multi-layer progressive blur only
   within the topbar transition region. The blur fades toward the content
   below; it does not draw an opaque card or tint surface over the content.
3. The existing topbar foreground, drawn above the backdrop. Its title and
   actions remain crisp and keep their existing navigation behavior.

The bottom navigation remains outside the captured content layer, preserving
the existing backdrop isolation that prevents RenderNode self-reference.

## Behavior

- Parser input and parsed result content can appear behind the topbar and are
  progressively blurred there.
- The hard horizontal separation below the topbar is removed.
- No duplicate card, rounded white halo, or opaque overlay is introduced.
- History, settings, and page title remain clear and clickable.
- API 33+ uses the existing layered backdrop blur. API 29-32 keeps the
  non-shader fallback and must not initialize API 33 rendering resources.

## Verification

- Unit tests cover the progressive layer geometry and strength scaling.
- Debug unit tests and APK build must pass.
- ADB installation and repeated launches must keep the process alive with an
  empty crash buffer.
- A parser-page screenshot must show content continuing behind the topbar,
  no hard separator, and sharp topbar foreground controls.
