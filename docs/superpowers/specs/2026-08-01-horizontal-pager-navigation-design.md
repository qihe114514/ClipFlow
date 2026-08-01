# Horizontal Pager Navigation Design

## Goal

Allow horizontal swipes on ClipFlow's main surface to switch between the
configured primary navigation pages while preserving the existing navigation
routes, parser behavior, page state, and secondary screens.

## Approved Product Decisions

- Horizontal paging applies only to the primary bottom-navigation pages.
- The initial primary pages are Home, Douyin, and Xiaohongshu.
- Page order comes from the persisted `bottomBarOrder` preference.
- History, Settings, and About remain secondary `NavHost` destinations and do
  not participate in the pager.
- Existing bottom-bar taps, top-bar actions, home cards, and history links
  continue to navigate through `NavController`.
- The pager must adapt to additional registered primary pages without a fixed
  page count or hard-coded swipe branches.

## Architecture

`ClipFlowNavHost` remains the navigation shell. The primary route destinations
render one shared `HorizontalPager` backed by a hoisted `PagerState` and a
`SaveableStateHolder`. The pager renders the existing `HomeScreen`,
`DouyinScreen`, and `XiaohongshuScreen` content; secondary destinations keep
their existing screen implementations.

The ordered primary page list is derived from the registered bottom-navigation
items and the persisted `bottomBarOrder`. Unknown or duplicate persisted keys
are ignored, and registered items missing from an older persisted order are
appended in registration order. If the resulting list is empty, the registered
primary list is used as a fallback so the pager always has a valid page count.
The same helper is used by the bottom bar and pager, keeping selection and
swipe order aligned.

Adding a primary page requires registering its `Screen`, bottom-navigation
item, pager content, and `NavHost` destination. Pager sizing, adjacent-page
selection, boundaries, and synchronization then work without further gesture
code changes. Arbitrary detail routes remain intentionally outside this
registry.

## Navigation Synchronization

The pager is the visual source for a completed swipe. When its settled page
changes, the matching primary route is navigated with the existing tab-style
state restoration behavior. A route change originating from a bottom-bar tap,
top-bar action, home card, history item, default-page preference, or external
entry animates the pager to the matching index when necessary.

Primary-to-primary `NavHost` transitions are disabled so a settled pager move
does not receive a second slide/fade animation. Detail transitions remain
unchanged. The current parser route argument is forwarded only to its matching
pager page, so history and external `sourceUrl` entries still trigger the
existing one-time parse effects.

Pager state is saveable per route. Activity-scoped parser ViewModels remain the
owners of input, parse, download, and dialog state; this change does not move
business logic into navigation.

## Interaction and Boundaries

`HorizontalPager` handles horizontal drag thresholds, settling, and edge
clamping. Vertical scrolling inside Home, Douyin, or Xiaohongshu remains
available because the pager uses Compose's directional gesture handling.
Secondary screens are rendered outside the pager and therefore cannot be
changed by a horizontal swipe.

## Verification

- Unit-test primary-order filtering, duplicate/unknown handling, fallback, and
  adjacent-page boundary behavior.
- Run the existing debug unit tests.
- Build the debug and release variants with the repository's offline Gradle
  procedure and verify the release APK exists.
- Report device/emulator gesture acceptance separately if no device is
  available in the environment.

## Non-Goals

- Replacing `NavController` with a second navigation framework.
- Making History, Settings, or About horizontally pageable.
- Changing parser, download, wallpaper, privacy, or persistence behavior
  beyond the state needed to synchronize the primary pager.
