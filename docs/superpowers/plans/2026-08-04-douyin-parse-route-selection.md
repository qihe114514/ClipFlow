# Douyin Parse Route Selection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a bottom-of-screen Route 1/Route 2 selector to the Douyin parser, with Route 1 as the default current API and Route 2 backed by the upstream hybrid parsing API.

**Architecture:** Keep route state in `ParsePageUiState` and pass an optional route argument through the existing repository/parser boundary. Route 1 remains the current `ApiService` flow; Route 2 uses a focused Retrofit service and maps the upstream minimal response into the existing `ParseResult` model. The shared parser screen renders the selector only when the Douyin screen supplies it.

**Tech Stack:** Kotlin, Android Jetpack Compose, Material 3, Retrofit 2, Gson, Kotlin coroutines, JUnit 4, cached Gradle.

## Global Constraints

- Route 1 is selected by default and keeps its current GET then POST fallback behavior.
- Route 2 calls `https://douyin.wtf/api/hybrid/video_data?url=<input>&minimal=true`.
- A non-empty input causes route changes to reparse; an empty input causes route changes to update state only.
- A selected route failure is surfaced; there is no silent fallback between routes.
- Existing download retry behavior and non-Douyin parser behavior remain unchanged.
- Do not persist the route selection.
- Verify with `:app:testDebugUnitTest`, `:app:assembleDebug`, and `git diff --check` using cached offline Gradle.

---

## File Map

- Modify `app/src/main/java/com/qihe/clipflow/data/api/ApiService.kt`: leave the primary API contract intact.
- Create `app/src/main/java/com/qihe/clipflow/data/api/DouyinBackupApiService.kt`: define the upstream Route 2 endpoint.
- Modify `app/src/main/java/com/qihe/clipflow/data/api/RetrofitClient.kt`: expose a Retrofit client for `https://douyin.wtf/`.
- Create `app/src/main/java/com/qihe/clipflow/data/api/model/DouyinBackupModels.kt`: model only the stable minimal-response fields needed by the UI.
- Modify `app/src/main/java/com/qihe/clipflow/data/repository/ParseModels.kt`: add `DouyinParseRoute`.
- Modify `app/src/main/java/com/qihe/clipflow/data/repository/PlatformParser.kt`: add a default route-aware overload that preserves existing parser callers.
- Modify `app/src/main/java/com/qihe/clipflow/data/repository/ParseRepository.kt`: pass the selected route through to the parser.
- Modify `app/src/main/java/com/qihe/clipflow/data/repository/DouyinPlatformParser.kt`: select Route 1 or Route 2 and map Route 2 data.
- Modify `app/src/main/java/com/qihe/clipflow/ui/parser/ParseScreenSupport.kt`: accept and forward the selected route.
- Modify `app/src/main/java/com/qihe/clipflow/ui/parser/PlatformParseViewModel.kt`: own route state and reparse on non-empty route changes.
- Create `app/src/main/java/com/qihe/clipflow/ui/parser/ParseRouteCard.kt`: render the bottom route selector.
- Modify `app/src/main/java/com/qihe/clipflow/ui/parser/PlatformParseScreenContent.kt`: render the optional selector after result/download content.
- Modify `app/src/main/java/com/qihe/clipflow/ui/douyin/DouyinScreen.kt`: provide the route state and callback.
- Create `app/src/test/java/com/qihe/clipflow/data/repository/DouyinPlatformParserTest.kt`: test Route 1 preservation and Route 2 mapping/failures with fake APIs.

## Task 1: Add the route and API contracts

**Files:**
- Create: `app/src/main/java/com/qihe/clipflow/data/api/DouyinBackupApiService.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/data/api/RetrofitClient.kt`
- Create: `app/src/main/java/com/qihe/clipflow/data/api/model/DouyinBackupModels.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/data/repository/ParseModels.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/data/repository/PlatformParser.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/data/repository/ParseRepository.kt`

**Interfaces:**
- Produce `DouyinBackupApiService.parseVideo(url: String, minimal: Boolean = true): DouyinBackupResponse` using `@GET("api/hybrid/video_data")`, `@Query("url")`, and `@Query("minimal")`.
- Produce `RetrofitClient.douyinBackupApiService` using base URL `https://douyin.wtf/` and the existing OkHttp client/Gson converter.
- Produce `enum class DouyinParseRoute { PRIMARY, BACKUP }` with `PRIMARY` first so it is the default.
- Change `PlatformParser` by adding `suspend fun parse(normalizedInput: String, route: DouyinParseRoute): Result<ParseResult> = parse(normalizedInput)`; keep the existing one-argument method unchanged.
- Change `ParseRepository.parse` to `parse(platform: SupportedPlatform, rawInput: String, route: DouyinParseRoute = DouyinParseRoute.PRIMARY)` and call the new parser overload.

- [ ] **Step 1: Add the route enum and default parser overload.**

Use this exact compatibility shape so Xiaohongshu, Bilibili, and existing tests keep their one-argument parse implementations:

```kotlin
enum class DouyinParseRoute {
    PRIMARY,
    BACKUP
}

interface PlatformParser {
    val platform: SupportedPlatform

    fun supports(rawInput: String): Boolean

    fun normalizeInput(rawInput: String): String

    suspend fun parse(normalizedInput: String): Result<ParseResult>

    suspend fun parse(
        normalizedInput: String,
        route: DouyinParseRoute
    ): Result<ParseResult> = parse(normalizedInput)
}
```

- [ ] **Step 2: Add the upstream Retrofit service and DTOs.**

Use DTOs that avoid deserializing unstable upstream nested objects as strings:

```kotlin
interface DouyinBackupApiService {
    @GET("api/hybrid/video_data")
    suspend fun parseVideo(
        @Query("url") url: String,
        @Query("minimal") minimal: Boolean = true
    ): DouyinBackupResponse
}

data class DouyinBackupResponse(
    val code: Int = 0,
    val message: String? = null,
    val data: DouyinBackupData? = null
)

data class DouyinBackupData(
    val type: String? = null,
    val desc: String? = null,
    val author: DouyinBackupAuthor? = null,
    @SerializedName("cover_data") val coverData: DouyinBackupCoverData? = null,
    @SerializedName("video_data") val videoData: DouyinBackupVideoData? = null,
    @SerializedName("image_data") val imageData: DouyinBackupImageData? = null,
    val statistics: DouyinStatistics? = null
)

data class DouyinBackupAuthor(
    val nickname: String? = null,
    @SerializedName("avatar_thumb") val avatarThumb: DouyinBackupImageRef? = null
)

data class DouyinBackupCoverData(val cover: DouyinBackupImageRef? = null)
data class DouyinBackupImageRef(@SerializedName("url_list") val urlList: List<String>? = null)
data class DouyinBackupVideoData(
    @SerializedName("nwm_video_url_HQ") val noWatermarkHighQualityUrl: String? = null,
    @SerializedName("nwm_video_url") val noWatermarkUrl: String? = null
)
data class DouyinBackupImageData(
    @SerializedName("no_watermark_image_list") val noWatermarkImages: List<String>? = null
)
```

Annotate response DTOs with `@Keep` consistently with the existing API models because release R8 reflection must retain Gson fields.

- [ ] **Step 3: Expose the second Retrofit service.**

Refactor only the Retrofit builder duplication needed to create both services:

```kotlin
private const val BACKUP_BASE_URL = "https://douyin.wtf/"

private fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
    .baseUrl(baseUrl)
    .client(okHttpClient)
    .addConverterFactory(GsonConverterFactory.create(gson))
    .build()

val apiService: ApiService by lazy {
    retrofit(BASE_URL).create(ApiService::class.java)
}

val douyinBackupApiService: DouyinBackupApiService by lazy {
    retrofit(BACKUP_BASE_URL).create(DouyinBackupApiService::class.java)
}
```

- [ ] **Step 4: Pass the route through `ParseRepository`.**

Normalize exactly once, then call `parser.parse(normalized, route)`. Keep `normalize` unchanged.

- [ ] **Step 5: Run compilation-focused tests.**

Run: `E:/clioipflow/.gradle-clipflow/wrapper/dists/gradle-9.6.0-bin/42k10rwplmzkhuboz9kdazi7s/gradle-9.6.0/bin/gradle.bat -g E:/clioipflow/.gradle-clipflow --offline --no-daemon --no-watch-fs --console=plain :app:testDebugUnitTest`

Expected: the existing unit-test suite compiles and passes; Route 2 tests are not added until Task 2.

## Task 2: Implement Route 2 parsing without changing Route 1

**Files:**
- Modify: `app/src/main/java/com/qihe/clipflow/data/repository/DouyinPlatformParser.kt`
- Create: `app/src/test/java/com/qihe/clipflow/data/repository/DouyinPlatformParserTest.kt`

**Interfaces:**
- Change the parser constructor to accept `private val api: ApiService` and `private val backupApi: DouyinBackupApiService = RetrofitClient.douyinBackupApiService`.
- Override the two-argument route-aware parse method in `DouyinPlatformParser`.
- Keep the existing one-argument method as a primary-route wrapper.

- [ ] **Step 1: Write Route 2 mapping tests first.**

Use fake implementations of `ApiService` and `DouyinBackupApiService`. Assert that Route 2 does not call primary methods and maps the HQ URL:

```kotlin
@Test
fun `backup route maps high quality video`() = runBlocking {
    val backup = FakeBackupApi(
        response = DouyinBackupResponse(
            code = 200,
            data = DouyinBackupData(
                type = "video",
                desc = "fixture",
                videoData = DouyinBackupVideoData(
                    noWatermarkHighQualityUrl = "https://fixture/high.mp4",
                    noWatermarkUrl = "https://fixture/normal.mp4"
                )
            )
        )
    )

    val result = DouyinPlatformParser(FakePrimaryApi(), backup)
        .parse("https://v.douyin.com/fixture", DouyinParseRoute.BACKUP)

    assertEquals("https://fixture/high.mp4", result.getOrThrow().items.single().url)
    assertEquals(0, primary.calls)
    assertEquals(1, backup.calls)
}
```

Also add tests for normal-URL fallback, image list mapping, non-200 failure, and missing media resulting in `NO_DOWNLOADABLE_CONTENT`.

- [ ] **Step 2: Run the new tests to confirm they fail.**

Run: `E:/clioipflow/.gradle-clipflow/wrapper/dists/gradle-9.6.0-bin/42k10rwplmzkhuboz9kdazi7s/gradle-9.6.0/bin/gradle.bat -g E:/clioipflow/.gradle-clipflow --offline --no-daemon --no-watch-fs --console=plain :app:testDebugUnitTest --tests com.qihe.clipflow.data.repository.DouyinPlatformParserTest`

Expected: compile or assertion failures because the route-aware parser and mapper do not exist yet.

- [ ] **Step 3: Implement route dispatch.**

Keep the current one-argument implementation body as the primary path and use a wrapper:

```kotlin
override suspend fun parse(normalizedInput: String): Result<ParseResult> =
    parse(normalizedInput, DouyinParseRoute.PRIMARY)

override suspend fun parse(
    normalizedInput: String,
    route: DouyinParseRoute
): Result<ParseResult> = when (route) {
    DouyinParseRoute.PRIMARY -> parsePrimary(normalizedInput)
    DouyinParseRoute.BACKUP -> parseBackup(normalizedInput)
}
```

Move the existing implementation into `parsePrimary` without changing its request ordering or item mapping.

- [ ] **Step 4: Implement minimal Route 2 mapping.**

In `parseBackup`, retain the same blank-input and `supports` checks, call `backupApi.parseVideo(normalizedInput, minimal = true)`, require `code == 200` and non-null data, and construct:

- one `ContentItem` with `ContentType.VIDEO`, `nwm_video_url_HQ ?: nwm_video_url`, MP4 media info, and the existing Douyin video description style;
- one image item per non-blank `no_watermark_image_list` URL for `type == "image"`;
- `ParseResult` metadata from `desc`, `cover_data.cover.url_list[0]`, `author.nickname`, `author.avatar_thumb.url_list[0]`, `type`, `statistics`, and the original normalized input as `shareUrl`;
- `ParseException(ParseFailure(ParseErrorKind.NO_DOWNLOADABLE_CONTENT))` when no supported media URL remains;
- `ParseException(ParseFailure(ParseErrorKind.REMOTE_FAILURE, detail = response.message))` for a non-200 or missing data response.

Do not map unstable live-photo or music structures in Route 2 unless the DTO contains a directly usable URL; this keeps the selected route's supported contract explicit and avoids false download items.

- [ ] **Step 5: Run the tests to confirm they pass.**

Run: `E:/clioipflow/.gradle-clipflow/wrapper/dists/gradle-9.6.0-bin/42k10rwplmzkhuboz9kdazi7s/gradle-9.6.0/bin/gradle.bat -g E:/clioipflow/.gradle-clipflow --offline --no-daemon --no-watch-fs --console=plain :app:testDebugUnitTest --tests com.qihe.clipflow.data.repository.DouyinPlatformParserTest`

Expected: all focused tests pass, including the existing primary-route GET/POST behavior tests.

- [ ] **Step 6: Commit the parser/API slice.**

Run:

```text
git add app/src/main/java/com/qihe/clipflow/data app/src/test/java/com/qihe/clipflow/data/repository/DouyinPlatformParserTest.kt
git commit -m "feat: add Douyin backup parse route"
```

## Task 3: Add route state and automatic reparse behavior

**Files:**
- Modify: `app/src/main/java/com/qihe/clipflow/ui/parser/ParseScreenSupport.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/ui/parser/PlatformParseViewModel.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/ui/douyin/DouyinScreen.kt`

**Interfaces:**
- `ParseScreenSupport.parse(rawInput: String, route: DouyinParseRoute = DouyinParseRoute.PRIMARY)` forwards the route to `ParseRepository.parse`.
- `ParsePageUiState` adds `val parseRoute: DouyinParseRoute = DouyinParseRoute.PRIMARY`.
- `PlatformParseViewModel.setParseRoute(route: DouyinParseRoute)` updates state and calls `parse(route)` only when the current input is non-blank.

- [ ] **Step 1: Add route state with a primary default.**

Add `parseRoute` to `ParsePageUiState`. Preserve it in `clearUrl()` when rebuilding the state, so clearing an input does not unexpectedly reset the user's current route.

- [ ] **Step 2: Add the route-aware support/repository call.**

Change only the support method signature and call site:

```kotlin
suspend fun parse(
    rawInput: String,
    route: DouyinParseRoute = DouyinParseRoute.PRIMARY
): Result<ParseResult> = parseRepository.parse(descriptor.platform, rawInput, route)
```

- [ ] **Step 3: Implement route switching.**

Use the existing `parse()` request-id and job-cancellation path rather than duplicating parse setup:

```kotlin
fun setParseRoute(route: DouyinParseRoute) {
    _uiState.update { it.copy(parseRoute = route, error = null) }
    if (_uiState.value.inputUrl.isNotBlank()) {
        parse(route)
    }
}
```

Change `parse()` to accept `routeOverride: DouyinParseRoute? = null`, choose `routeOverride ?: _uiState.value.parseRoute`, and pass that route to `parseSupport.parse`. The parse-button caller still invokes `parse()` with the current state route.

- [ ] **Step 4: Wire only `DouyinScreen`.**

Pass `parseRoute = uiState.parseRoute` and `onParseRouteChange = viewModel::setParseRoute` to the shared screen content. Do not add route state or controls to Xiaohongshu.

- [ ] **Step 5: Run the existing unit tests.**

Run: `E:/clioipflow/.gradle-clipflow/wrapper/dists/gradle-9.6.0-bin/42k10rwplmzkhuboz9kdazi7s/gradle-9.6.0/bin/gradle.bat -g E:/clioipflow/.gradle-clipflow --offline --no-daemon --no-watch-fs --console=plain :app:testDebugUnitTest`

Expected: existing tests pass and the app source compiles with the new route state.

- [ ] **Step 6: Commit the state slice.**

Run:

```text
git add app/src/main/java/com/qihe/clipflow/ui/parser/ParseScreenSupport.kt app/src/main/java/com/qihe/clipflow/ui/parser/PlatformParseViewModel.kt app/src/main/java/com/qihe/clipflow/ui/douyin/DouyinScreen.kt
git commit -m "feat: reparse Douyin input on route change"
```

## Task 4: Render the bottom route card

**Files:**
- Create: `app/src/main/java/com/qihe/clipflow/ui/parser/ParseRouteCard.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/ui/parser/PlatformParseScreenContent.kt`

**Interfaces:**
- `ParseRouteCard(selectedRoute: DouyinParseRoute, onRouteChange: (DouyinParseRoute) -> Unit)` renders the selector.
- `PlatformParseScreenContent` adds optional parameters `parseRoute: DouyinParseRoute? = null` and `onParseRouteChange: ((DouyinParseRoute) -> Unit)? = null`.

- [ ] **Step 1: Add the focused card composable.**

Wrap the title and Material 3 single-choice segmented control in the existing `GlassCard`. Use `DouyinParseRoute.entries` and show `Route 1`/`Route 2`; use `SegmentedButtonDefaults.itemShape` for stable adjacent shapes and call `onRouteChange` only from the selected button.

- [ ] **Step 2: Insert the optional card at the bottom.**

After the conditional result/download item and before the existing spacer, render the card only when both optional arguments are non-null:

```kotlin
if (parseRoute != null && onParseRouteChange != null) {
    item(key = "parse_route") {
        ParseRouteCard(parseRoute, onParseRouteChange)
    }
}
```

This keeps the card at the bottom of the parser content and keeps non-Douyin callers unchanged.

- [ ] **Step 3: Run formatting and compile checks.**

Run: `E:/clioipflow/.gradle-clipflow/wrapper/dists/gradle-9.6.0-bin/42k10rwplmzkhuboz9kdazi7s/gradle-9.6.0/bin/gradle.bat -g E:/clioipflow/.gradle-clipflow --offline --no-daemon --no-watch-fs --console=plain :app:testDebugUnitTest :app:assembleDebug`

Expected: tests pass and `app/build/outputs/apk/debug/app-debug.apk` is produced.

- [ ] **Step 4: Commit the UI slice.**

Run:

```text
git add app/src/main/java/com/qihe/clipflow/ui/parser/ParseRouteCard.kt app/src/main/java/com/qihe/clipflow/ui/parser/PlatformParseScreenContent.kt
git commit -m "feat: add Douyin parse route selector"
```

## Task 5: Final verification

**Files:**
- No source changes expected unless verification exposes a defect.

- [ ] **Step 1: Check the complete diff and worktree.**

Run: `git diff HEAD~3..HEAD --check; git status --short; git log -4 --oneline`

Expected: only the documented route/API/parser/state/UI files and the three feature commits are present; unrelated user changes are untouched.

- [ ] **Step 2: Run the full unit-test and debug-build verification.**

Run:

```text
E:/clioipflow/.gradle-clipflow/wrapper/dists/gradle-9.6.0-bin/42k10rwplmzkhuboz9kdazi7s/gradle-9.6.0/bin/gradle.bat -g E:/clioipflow/.gradle-clipflow --offline --no-daemon --no-watch-fs --console=plain :app:testDebugUnitTest :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL` and a non-empty `E:/clioipflow/ClipFlow/app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 3: Run whitespace verification.**

Run: `git diff HEAD~3..HEAD --check`

Expected: no output and exit code 0.

- [ ] **Step 4: Confirm behavior against the acceptance checklist.**

Check the implementation against these exact scenarios:

1. Fresh Douyin page shows Route 1 selected.
2. Empty input switched to Route 2 remains idle and shows no error.
3. Non-empty input switched to Route 2 starts a new parse without pressing the parse button.
4. Route 2 button parsing calls the upstream endpoint and maps a video URL.
5. Switching back to Route 1 with input reparses through the current API.
6. Xiaohongshu has no route card.
