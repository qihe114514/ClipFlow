# Live Pager Progress in Bottom Navigation

## Goal

Keep the bottom navigation indicator visually synchronized with the main
horizontal pager while the user is dragging between primary pages.

## Current Behavior

`ClipFlowNavHost` owns a shared `PagerState`, but the bottom navigation receives
only the settled route-derived integer index. The indicator therefore changes
after `settledPage` and route navigation update, rather than during the drag.

## Design

Pass the shared `PagerState` into the navigation chrome. The bottom bar keeps
its existing integer selected index for click and tab-drag navigation, and
accepts an optional external indicator progress provider for pager gestures.

The provider returns:

```text
currentPage + currentPageOffsetFraction
```

clamped to the registered primary page range. While the pager is scrolling,
the liquid indicator and its highlight use this fractional position directly.
When the pager is idle, the existing damped bottom-bar animation remains the
visual source of truth for taps, tab dragging, route restoration, and settling.

Pager-to-route synchronization remains based on `settledPage`; the live
fractional value never triggers navigation. Primary page registration and
persisted `bottomBarOrder` continue to determine the page count and order, so
future registered pages use the same behavior automatically.

## Boundaries

- Clamp progress to `0f..(tabsCount - 1)` for first and last pages.
- Use the same progress for forward and backward drags.
- Let pager cancellation or edge resistance return the indicator to its source
  position.
- Keep the bottom bar hidden on History, Settings, and About.
- Preserve existing bottom-bar clicks and bottom-bar drag selection behavior.

## Implementation Surface

- `ClipFlowNavHost.kt`: pass the shared `PagerState` to the bottom bar.
- `NavigationChrome.kt`: derive and provide the pager indicator progress and
  active-scrolling signal.
- `ui/component/FloatingBottomBar.kt`: render the external fractional position
  only while the pager is actively scrolling, without changing navigation
  callbacks.
- `PrimaryNavigationTest.kt` or a focused navigation helper test: cover
  progress calculation and clamping.

## Verification

- Run the focused and full debug unit tests.
- Build the debug APK with the repository's offline Gradle procedure.
- Manually verify bidirectional drags, cancellation, edge clamping, bottom-bar
  clicks, bottom-bar drags, and secondary-page visibility when a device is
  available.
