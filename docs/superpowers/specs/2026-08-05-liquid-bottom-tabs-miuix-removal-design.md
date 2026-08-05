# Liquid Bottom Tabs and Miuix Removal Design

## Goal

Replace ClipFlow's primary navigation bottom bar with the four-tab implementation from Kyant0/AndroidLiquidGlass and remove the external Miuix dependency without changing existing page content or navigation behavior.

## Scope

- Migrate the sample's `LiquidBottomTabs` and `LiquidBottomTab` behavior: capsule layout, backdrop rendering, vibrancy, blur, lens refraction, interactive highlight, drag selection, and press animation.
- Keep ClipFlow's existing `PrimaryPagerState`, `BottomNavItem`, `AppPreferences.bottomBarOrder`, route navigation, and four registered destinations: Home, Douyin, Xiaohongshu, and Bilibili.
- Adapt icons and labels through Material 3 instead of Miuix basic components.
- Replace Miuix backdrop/theme calls in the navigation shell with the existing `com.kyant.backdrop` API and Material 3 theme context.
- Remove both Miuix Gradle dependencies, the Manifest library override, and all `top.yukonga.miuix` source imports.

## Out Of Scope

- Migrating the content screens, top bar visual system, dialogs, settings controls, or other UI to the AndroidLiquidGlass catalog.
- Changing routes, persistence keys, page ordering semantics, or the app's registered bottom destinations.
- Removing unrelated mirrored liquid-glass helper code unless it is required to compile after the dependency removal.

## Architecture

`NavigationChrome.kt` remains the business adapter: it reads the persisted order, maps each `BottomNavItem` to a tab, and forwards selection to `PrimaryPagerState`. `LiquidBottomTabs.kt` and `LiquidBottomTab.kt` contain the copied sample interaction/rendering implementation and depend only on `com.kyant.backdrop`, `io.github.kyant0:shapes`, Compose, and the copied animation utilities.

The navigation shell continues to provide a backdrop layer for the bar. The bar's surface and selection indicator use the sample's `Capsule` shape and backdrop effects. Material 3 supplies content colors and icon/text rendering, so no Miuix theme provider is required.

## Compatibility and Failure Handling

- Keep the existing minSdk 29 and let `com.kyant.backdrop` handle unsupported rendering APIs through its own platform implementation.
- Do not instantiate Miuix or direct Android `RuntimeShader` code from the migrated bar.
- If the persisted order omits a registered route, preserve the existing `orderedBottomNavItems` behavior that appends missing routes, ensuring the bar still has four tabs.
- If a bar item is selected while an animation is running, preserve `PrimaryPagerState.animateToPage` cancellation and selected-page synchronization.

## Verification

- Add or update focused tests for the four registered routes and persisted-order completion.
- Run the relevant unit tests with the cached offline Gradle workflow.
- Run a Debug build.
- Search the repository for `top.yukonga.miuix`, Miuix Gradle coordinates, and the Manifest override; no active source or dependency reference may remain.

