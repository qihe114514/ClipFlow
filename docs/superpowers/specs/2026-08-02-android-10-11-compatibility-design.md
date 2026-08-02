# Android 10 and Android 11 Compatibility

## Goal

Make ClipFlow installable and usable on Android 10 (API 29) and Android 11
(API 30) without changing the current target SDK or removing the liquid bottom
navigation from supported systems.

Compatibility means the app starts successfully and its existing parsing,
preview, download, gallery save, history, settings, update, and navigation
workflows remain available. The Android 10/11 bottom bar may use a visual
fallback where newer graphics APIs do not exist.

## Current Constraints

- The application currently declares `minSdk = 31`.
- `miuix-blur-android:0.9.2` declares a minimum SDK of 33.
- The liquid highlight directly uses `RuntimeShader`, which was added in API
  33. Related render effects are also unavailable on Android 10/11.
- Media downloads and cover saves already use scoped-storage `MediaStore` APIs
  on API 29 and later.
- Fullscreen video currently calls the platform `WindowInsetsController`
  directly and therefore needs a compatibility implementation for API 29.

Merely lowering `minSdk` would produce an installable package that could fail
when rendering navigation. The graphics capability boundary must be explicit.

## Design

Set the application minimum SDK to 29 and keep `compileSdk` and `targetSdk` at
their current values.

Select the bottom-navigation renderer from the device SDK level:

- API 33 and later use the existing liquid backdrop, blur, refraction, shader
  highlight, drag, and pager-progress behavior.
- API 29 through 32 use a compatibility bottom bar with the same destinations,
  selection state, pager-progress indicator, tap handling, drag selection, and
  elastic movement. It uses a translucent surface, border highlight, and shadow
  instead of real-time backdrop blur and refraction.

Keep the API 33 implementation in an API-gated component so Android 10-12L do
not execute or initialize `miuix-blur` or `RuntimeShader` code. A small,
testable SDK-capability function defines the boundary at API 33. Both renderers
receive the same navigation state and callbacks; neither renderer owns routes
or changes navigation behavior.

Replace direct fullscreen system-bar calls with AndroidX window-insets
compatibility APIs. Hiding, transient swipe reveal, and restoration remain the
same on supported versions.

Do not change the download or gallery-save implementation. API 29 and 30 stay
on the existing `MediaStore` path, which does not require legacy broad storage
write access.

## Failure Handling

The compatible renderer is selected automatically and has no user-facing
setting. Unsupported graphics features are never attempted on API 29-32, so a
missing shader or blur capability cannot turn into a startup failure. Existing
network, parsing, download, and save errors retain their current behavior.

## Verification

- Add unit coverage for the API 32/API 33 liquid-effect boundary and preserve
  existing bottom-navigation behavior tests.
- Run Android Lint and all debug unit tests.
- Build debug and release APKs with the repository's JDK 21 and offline Gradle
  workflow.
- Inspect the packaged release manifest to confirm `minSdkVersion` is 29 and
  `targetSdkVersion` is unchanged.
- When local emulator images are available, smoke-test API 29 and API 30 for
  startup, primary navigation, pager and bottom-bar gestures, parsing, media
  download/gallery save, and fullscreen video enter/exit.
- Smoke-test API 33 or later to confirm the existing liquid renderer is still
  selected.

## Out of Scope

- Reproducing real-time shader refraction or backdrop blur on API 29-32.
- Changing the target SDK, parsing APIs, persistence schema, or download
  locations.
- Refactoring unrelated navigation or visual components.
