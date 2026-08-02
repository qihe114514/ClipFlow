# Pager, Bottom Bar, and Parse State Design

## Goal

Make primary-page swipes and bottom-bar taps settle immediately, keep the
bottom-bar indicator responsive during the transition, and prevent a cleared
parse result from being replayed when the user returns to a page.

## Scope

- Primary pages remain the existing Home, Douyin, and Xiaohongshu pages.
- History, Settings, About, downloads, and parser APIs remain unchanged.
- Normal page changes preserve each parser ViewModel state.
- Clear resets the parser page to an empty state and must not trigger the last
  external source URL again.

## State Ownership

`PagerState` is the visual source of truth for primary-page position:

- Horizontal swipes update the indicator from the pager's fractional position.
- Bottom-bar taps launch a pager scroll directly, so the indicator moves as
  soon as the tap is handled.
- A settled pager page synchronizes the primary NavController route for the
  top bar, deep-link compatibility, and secondary-page navigation.
- A route-to-pager effect remains only for entering a primary page from an
  external route or from a secondary destination.

The custom bottom bar keeps its local damped position only for its own drag
gesture. Pager-driven position handoff must not press the indicator or wait
for an unrelated damped animation before accepting another input.

## Parse Source Handling

The `sourceUrl` navigation argument is an external one-shot input. A parser
screen may apply it only when it is a new non-blank value for that screen. A
normal tab switch does not reparse the current ViewModel state. Clearing the
page invalidates the consumed source input, so returning to the page cannot
replay it.

## Failure and Interaction Rules

- Tapping another bottom-bar item while a page transition is running must
  interrupt or replace the current pager animation and begin the new one.
- Completing a swipe must leave the indicator at the destination immediately,
  without a delayed press/release state.
- A parser result remains visible after switching away and back.
- After clear, input, result, loading, error, and result metadata are empty;
  download session state remains preserved.

## Verification

- Unit-test fractional pager position and active-state behavior, including the
  settled handoff path without a delayed animation requirement.
- Unit-test one-shot external source consumption and clear invalidation.
- Run focused JVM tests and the offline debug build with the repository's
  Gradle 8.4 distribution.
- Use `git diff --check` and inspect the final diff for unrelated changes.
