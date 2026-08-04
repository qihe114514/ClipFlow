# Douyin Parse Route Selection Design

Date: 2026-08-04

## Goal

Add an explicit parse-route selector to the Douyin parser screen. Route 1 remains the default and preserves the current `api.bugpk.com` behavior. Route 2 uses the upstream `Douyin_TikTok_Download_API` hybrid video endpoint.

The selector is only a parse-route choice. It must not change the existing download retry behavior or silently switch routes after a selected route fails.

## User Flow

- The bottom of the Douyin parser screen contains a `parse route` card with two mutually exclusive choices: `Route 1` and `Route 2`.
- `Route 1` is selected by default.
- Switching routes with an empty input only changes the selected route.
- Switching routes with a non-empty input cancels any in-flight parse and starts a new parse using the selected route.
- Pressing the existing parse button uses the currently selected route.
- The route is held in screen/ViewModel state only. It is not persisted, so a new app session starts on route 1.

## Architecture

`PlatformParseViewModel` owns a `DouyinParseRoute` value in `ParsePageUiState`. Its route-change method updates state and conditionally starts parsing when the input is non-blank. The existing request-id and job-cancellation guards continue to prevent stale results from replacing newer results.

`ParseRepository` and `PlatformParser` gain an optional route-aware parse overload with a primary default. Existing Xiaohongshu and Bilibili parsers keep their current behavior through the default implementation. `DouyinPlatformParser` selects the API based on the route.

Route 1 keeps its current GET request, POST fallback, response validation, item construction, and error mapping unchanged.

Route 2 uses a separate Retrofit service at `https://douyin.wtf/`:

```text
GET api/hybrid/video_data?url=<input>&minimal=true
```

The response is mapped from the upstream minimal response into the existing `ParseResult` shape. Video parsing prefers `data.video_data.nwm_video_url_HQ` and falls back to `nwm_video_url`. Image parsing consumes `data.image_data.no_watermark_image_list`. Missing data, non-200 response codes, and empty downloadable content use the existing parse failure kinds.

The upstream endpoint is a server-side API and ClipFlow does not forward Douyin cookies or API keys. The endpoint's service-side Cookie configuration remains its operational prerequisite.

## UI

`PlatformParseScreenContent` receives an optional route selector callback and selected route. It renders the new card after the result/download section and before the bottom spacer. Xiaohongshu and other shared callers omit the optional arguments, so they do not show the card.

The selector uses the existing glass-card styling and a Material 3 single-choice segmented control. The labels are the explicit route names requested by the user.

## Error Handling

- Route selection itself never produces an empty-input error.
- A route 2 request failure is surfaced through the existing parser error card.
- Route 2 does not fall back to route 1 after selection.
- An in-flight parse from the previous route is cancelled when a new route parse starts.
- Existing download progress and download failure handling are unchanged.

## Verification

Add focused unit tests for:

- Route 1 still uses GET and then POST when the primary response is non-200.
- Route 2 maps a minimal video response, including the HQ and normal URL fallback.
- Route 2 rejects non-200 or contentless responses.
- Route changes with empty and non-empty input follow the specified behavior where the existing ViewModel test seams permit.

Run:

```text
gradle --offline --no-daemon --no-watch-fs --console=plain :app:testDebugUnitTest
gradle --offline --no-daemon --no-watch-fs --console=plain :app:assembleDebug
git diff --check
```

## Sources

- Upstream research: `docs/research/2026-08-04-douyin-backup-route.md`
- Upstream endpoint source at commit `42784ffc83a72a516bfe952153ad7e2a3998d16c`: https://github.com/Evil0ctal/Douyin_TikTok_Download_API/blob/42784ffc83a72a516bfe952153ad7e2a3998d16c/app/api/endpoints/hybrid_parsing.py
- Upstream minimal response mapping at the same commit: https://github.com/Evil0ctal/Douyin_TikTok_Download_API/blob/42784ffc83a72a516bfe952153ad7e2a3998d16c/crawlers/hybrid/hybrid_crawler.py
