# KernelSU Style UI Shell Design

## Goal

Bring the application shell of `chenaizhang/KernelSU-Style-UI-Kit` into
ClipFlow while keeping ClipFlow's parser, downloader, history, wallpaper,
privacy, and update behavior intact.

## Approved Product Decisions

- The main surface has exactly three pages: Home, Douyin, and Xiaohongshu.
- The three pages live in a template-style `HorizontalPager`; swipes and the
  bottom navigation stay synchronized.
- The bottom bar uses the template interaction and visual language, but has
  three destinations rather than the template's Home/Settings pair.
- Home is a real landing page and never contains the share-link parser.
- Douyin and Xiaohongshu each keep their own parser input, loading, error,
  result, and download actions.
- Miuix is the default renderer. Settings can switch the whole shell to
  Material and the choice survives process restarts.
- History, Settings, and About remain secondary destinations outside the main
  pager and retain their existing business behavior.
- The repository accepts GPLv3 for the borrowed template shell and records the
  template source and license in the repository documentation.

## Architecture

`MainActivity` owns the lifecycle and renders a persisted `UiMode` through a
template-inspired theme root. `ClipFlowNavHost` owns secondary destinations
and places `MainScreen` at the main route. `MainScreen` owns one pager state;
its page count is three and its selected page is the single source of truth
for both pager swipes and the bottom bar.

The parser pages continue to use `DouyinScreen` and `XiaohongshuScreen`, so
their existing activity-scoped ViewModels and shared download coordinator are
unchanged. The settings ViewModel gains only the UI-mode flow and setter.
Existing Material-based content is kept inside a Material compatibility theme
when the Miuix root is active; template-owned chrome has separate Miuix and
Material renderers.

## Navigation and Deep Links

The existing history/settings/about routes remain NavHost destinations. The
main route is the only destination with the three-page bottom bar. A history
item opens the main route with a platform and URL argument; the main screen
selects the requested pager page and forwards the URL to that platform page.
Existing external intents and download-pill callbacks use the same route
contract.

## Verification

- Unit-test `UiMode` value parsing and the three-page index bounds.
- Unit-test route argument encoding/decoding for parser deep links.
- Build the debug and release APKs with the repository Gradle wrapper.
- Confirm the release APK exists at `app/build/outputs/apk/release/app-release.apk`.
