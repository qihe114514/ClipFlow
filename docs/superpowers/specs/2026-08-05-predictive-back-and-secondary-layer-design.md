# Secondary Page Layer and Predictive Back Design

Date: 2026-08-05

## Goal

Keep the primary pager mounted so primary-page navigation retains a valid
layout, while ensuring History, Settings, About, and Open Source never reveal
primary-page content underneath them. All secondary pages must support
Android's predictive back contract.

## Current Problem

`MainPager` is kept in the composition to avoid starting a page animation
before the pager has a layout. The secondary `NavHost` destinations are drawn
after it, but their surfaces are transparent. As a result, the pager remains
visible through empty areas of History and other secondary pages.

The app currently relies on ordinary navigation back behavior. Secondary page
back handling is not explicitly registered for the Android predictive-back
contract.

## Approved Approach

Use the platform-native predictive-back API without upgrading Navigation
Compose or adding a second navigation layer.

- Opt the activity into Android's `OnBackInvokedDispatcher` behavior.
- Register one `PredictiveBackHandler` in `ClipFlowNavHost` for secondary
  destinations only.
- Collect the gesture progress only to maintain the callback contract; commit
  by calling `navController.popBackStack()` when the gesture completes.
- Let cancellation leave the back stack unchanged.
- Keep the existing top-bar back button on the same `popBackStack` path.
- Keep `MainPager` mounted for layout stability, but make it fully transparent
  and non-interactive while the current route is secondary.

## Navigation Scope

Predictive back applies to:

- `history`
- `settings`
- `about`
- `open-source`

Primary parser pages remain normal primary destinations. The handler must be
disabled when the current route is a primary route or when there is no
previous back-stack entry.

## Data Flow

```text
system edge-back gesture
        |
        v
PredictiveBackHandler (secondary route)
        |
        +-- cancelled -> clear transient gesture state, keep route
        |
        +-- completed -> navController.popBackStack()
```

The pager's layout state and parser ViewModels remain unchanged. No business
data, history records, or parser state is moved between destinations.

## Visual Layer Rules

- Primary route: pager alpha is `1f`, pager gestures are enabled, primary
  bottom navigation is visible.
- Secondary route: pager alpha is `0f`, pager gestures are disabled, primary
  bottom navigation remains hidden.
- The secondary `NavHost` remains the only visible page layer above the
  wallpaper, so History cannot show Home or parser content through transparent
  gaps.

## Verification

- Add a pure navigation test covering the secondary-route predicate used by
  the predictive-back handler and pager visibility rules.
- Run the existing unit-test suite and the new regression test.
- Build the Debug APK and verify it contains the requested version metadata.
- On a device running Android 13 or newer, verify that a back gesture on each
  secondary page can be completed and cancelled without exposing primary page
  content before the gesture begins.

## Non-Goals

- No custom finger-driven page translation or spring animation.
- No Navigation Compose dependency upgrade.
- No changes to parser, download, history persistence, or primary pager
  business behavior.
