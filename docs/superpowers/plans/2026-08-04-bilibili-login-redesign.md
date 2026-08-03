# Bilibili Login Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the manual WebView confirmation flow with native SMS and desktop-UA QR login tabs that validate and save the Bilibili session automatically.

**Architecture:** Keep Compose responsible for the dialog and form state, and put Bilibili login HTTP, transient Cookie collection, response mapping, and account validation in a focused login client. SMS verification uses a small WebView only for the Geetest challenge; the Bilibili login page is never used as the form. QR content is generated locally from the URL returned by Bilibili, while QR status is polled by the client.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, OkHttp, Gson, Android WebView, ZXing core, JUnit 4, Gradle 9.6.

## Global Constraints

- Keep `versionName = "3.2"` and `versionCode = 47` unchanged.
- Do not add a remote login proxy or write credentials, captcha values, or Cookies to logs.
- Save a session only after `x/web-interface/nav` returns `code == 0` and `data.isLogin == true`.
- Keep Bilibili parsing, WBI signing, downloads, and the existing encrypted session format unchanged.
- Preserve the current uncommitted Cookie utility and parser-input changes; modify them only when the login work directly requires it.
- Run Gradle tasks serially with `--offline --no-daemon --no-watch-fs --console=plain -g E:/clioipflow/.gradle-clipflow` whenever all dependencies are available.

## File Map

- Create `app/src/main/java/com/qihe/clipflow/data/bilibili/BilibiliLoginModels.kt` for protocol and UI-facing result types.
- Create `app/src/main/java/com/qihe/clipflow/data/bilibili/BilibiliLoginClient.kt` for Bilibili login requests and response Cookie capture.
- Create `app/src/main/java/com/qihe/clipflow/data/bilibili/BilibiliQrCode.kt` for QR bitmap generation.
- Create `app/src/main/java/com/qihe/clipflow/ui/bilibili/BilibiliCaptchaDialog.kt` for the isolated Geetest verification surface.
- Modify `app/src/main/java/com/qihe/clipflow/ui/bilibili/BilibiliAccountAction.kt` to render the tabbed login UI and consume login results.
- Modify `app/build.gradle.kts` to add `com.google.zxing:core:3.5.3`.
- Extend `app/src/main/java/com/qihe/clipflow/data/bilibili/BilibiliCookieUtils.kt` and its existing test for response Cookie merging.
- Create `app/src/test/java/com/qihe/clipflow/data/bilibili/BilibiliLoginModelsTest.kt` for QR state mapping and input validation.
- Create `app/src/test/java/com/qihe/clipflow/data/bilibili/BilibiliLoginClientTest.kt` for fixed HTTP response and Cookie capture tests.

---

### Task 1: Add protocol models and pure state mapping

**Files:**
- Create: `app/src/main/java/com/qihe/clipflow/data/bilibili/BilibiliLoginModels.kt`
- Create: `app/src/test/java/com/qihe/clipflow/data/bilibili/BilibiliLoginModelsTest.kt`

**Interfaces:**
- Produces `BilibiliQrCode(url: String, key: String)`, `BilibiliQrPollStatus`, `BilibiliCaptchaChallenge`, and `BilibiliLoginFailure` for later client/UI tasks.
- `mapBilibiliQrCode(code: Int, url: String?, refreshToken: String?): BilibiliQrPollStatus` maps Bilibili response codes without performing I/O.

- [ ] **Step 1: Write failing tests for QR response mapping.**

```kotlin
@Test
fun qrPollCodesMapToStableStates() {
    assertEquals(BilibiliQrPollStatus.Waiting, mapBilibiliQrCode(86101, null, null))
    assertEquals(BilibiliQrPollStatus.Scanned, mapBilibiliQrCode(86090, null, null))
    assertEquals(BilibiliQrPollStatus.Expired, mapBilibiliQrCode(86038, null, null))
    assertEquals(
        BilibiliQrPollStatus.Success("https://bilibili.com/callback", "refresh"),
        mapBilibiliQrCode(0, "https://bilibili.com/callback", "refresh")
    )
}
```

- [ ] **Step 2: Run the focused test and verify it fails because the model and mapper do not exist.**

Run:

```powershell
& 'E:/clioipflow/.gradle-clipflow/wrapper/dists/gradle-9.6.0-bin/42k10rwplmzkhuboz9kdazi7s/gradle-9.6.0/bin/gradle.bat' --offline --no-daemon --no-watch-fs --console=plain -g E:/clioipflow/.gradle-clipflow :app:testDebugUnitTest --tests com.qihe.clipflow.data.bilibili.BilibiliLoginModelsTest
```

Expected: compilation fails with unresolved `BilibiliQrPollStatus` or `mapBilibiliQrCode`.

- [ ] **Step 3: Add the smallest protocol model and mapper.**

```kotlin
sealed interface BilibiliQrPollStatus {
    data object Waiting : BilibiliQrPollStatus
    data object Scanned : BilibiliQrPollStatus
    data object Expired : BilibiliQrPollStatus
    data class Success(val callbackUrl: String, val refreshToken: String) : BilibiliQrPollStatus
}

fun mapBilibiliQrCode(code: Int, url: String?, refreshToken: String?): BilibiliQrPollStatus = when (code) {
    0 -> BilibiliQrPollStatus.Success(requireNotNull(url), refreshToken.orEmpty())
    86090 -> BilibiliQrPollStatus.Scanned
    86038 -> BilibiliQrPollStatus.Expired
    else -> BilibiliQrPollStatus.Waiting
}
```

Add these exact types for later tasks:

```kotlin
data class BilibiliQrCode(val url: String, val key: String)
data class BilibiliQrPollResult(val status: BilibiliQrPollStatus, val cookie: String = "")
data class BilibiliCaptchaChallenge(val token: String, val challenge: String, val gt: String)
data class BilibiliCaptchaResult(
    val token: String,
    val validate: String,
    val challenge: String,
    val seccode: String
)
sealed interface BilibiliLoginFailure {
    data object InvalidInput : BilibiliLoginFailure
    data object CaptchaRequired : BilibiliLoginFailure
    data object InvalidSmsCode : BilibiliLoginFailure
    data object ExpiredQr : BilibiliLoginFailure
    data object Network : BilibiliLoginFailure
    data object Unauthenticated : BilibiliLoginFailure
}

const val bilibiliQrValiditySeconds = 180
fun isBilibiliPhoneValid(phone: String): Boolean = phone.trim().length >= 6
fun isBilibiliSmsCodeValid(code: String): Boolean = code.length == 6 && code.all(Char::isDigit)
```

- [ ] **Step 4: Run the focused test and verify it passes.**

Expected: all `BilibiliLoginModelsTest` tests pass.

- [ ] **Step 5: Commit the model boundary.**

```powershell
git -c safe.directory=E:/clioipflow/ClipFlow add app/src/main/java/com/qihe/clipflow/data/bilibili/BilibiliLoginModels.kt app/src/test/java/com/qihe/clipflow/data/bilibili/BilibiliLoginModelsTest.kt
git -c safe.directory=E:/clioipflow/ClipFlow commit -m "test: define Bilibili login protocol states"
```

### Task 2: Implement the Bilibili login client and Cookie capture

**Files:**
- Create: `app/src/main/java/com/qihe/clipflow/data/bilibili/BilibiliLoginClient.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/data/bilibili/BilibiliCookieUtils.kt`
- Modify: `app/src/test/java/com/qihe/clipflow/data/bilibili/BilibiliCookieUtilsTest.kt`
- Create: `app/src/test/java/com/qihe/clipflow/data/bilibili/BilibiliLoginClientTest.kt`

**Interfaces:**
- `BilibiliLoginClient.generateQrCode(): Result<BilibiliQrCode>`
- `BilibiliLoginClient.pollQrCode(key: String): Result<BilibiliQrPollResult>`
- `BilibiliLoginClient.requestCaptcha(): Result<BilibiliCaptchaChallenge>`
- `BilibiliLoginClient.sendSms(phone: String, challenge: BilibiliCaptchaResult): Result<String>`
- `BilibiliLoginClient.loginBySms(phone: String, code: String, captchaKey: String): Result<String>`
- `sendSms` returns the temporary `captchaKey`; `loginBySms` returns the merged transient Cookie header and never saves it directly. QR success returns the same Cookie through `BilibiliQrPollResult.cookie`.

- [ ] **Step 1: Extend the Cookie test with response fragments and duplicate names.**

```kotlin
@Test
fun mergeCookieHeadersKeepsDistinctPairsFromResponseFragments() {
    assertEquals(
        "SESSDATA=one; bili_jct=csrf; DedeUserID=42",
        mergeBilibiliCookieHeaders(
            listOf("SESSDATA=one; Path=/", "bili_jct=csrf; HttpOnly", "DedeUserID=42", "SESSDATA=one")
        )
    )
}
```

- [ ] **Step 2: Run the focused Cookie test and verify it fails if the current utility includes attributes.**

Run `:app:testDebugUnitTest --tests com.qihe.clipflow.data.bilibili.BilibiliCookieUtilsTest` with the repository Gradle command from Task 1. Expected: the new assertion fails because `Path=/` and `HttpOnly` are currently treated as Cookie tokens.

- [ ] **Step 3: Implement Cookie-pair normalization and the HTTP client.**

Normalize each semicolon-delimited token by keeping only the first `name=value` pair, dropping attributes such as `Path`, `Domain`, `Expires`, `Secure`, and `HttpOnly`, and de-duplicating by Cookie name while preserving first-seen order. In `BilibiliLoginClient`, use an OkHttp client with a per-instance `CookieJar`-style accumulator and request headers:

```kotlin
private const val desktopUserAgent =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

private const val loginReferer = "https://passport.bilibili.com/login"
```

Use form bodies for SMS endpoints, query parameters for QR endpoints, parse JSON with Gson, and collect every `Set-Cookie` response header into the accumulator. Map QR codes `86101`, `86090`, `86038`, and `0` through `mapBilibiliQrCode`.

- [ ] **Step 4: Add `BilibiliLoginClientTest.kt` for request response mapping without real network calls.**

Keep network injection as a constructor parameter (`Call.Factory`) so tests can return fixed JSON and `Set-Cookie` headers. Assert that a successful QR poll returns `Success` and a response containing `SESSDATA` returns a merged Cookie header. Do not use real accounts or phone numbers in tests.

- [ ] **Step 5: Run the Cookie and client-focused tests.**

Expected: all focused tests pass and no Cookie value is printed by the test output.

- [ ] **Step 6: Commit the protocol client.**

```powershell
git -c safe.directory=E:/clioipflow/ClipFlow add app/src/main/java/com/qihe/clipflow/data/bilibili/BilibiliLoginClient.kt app/src/main/java/com/qihe/clipflow/data/bilibili/BilibiliCookieUtils.kt app/src/test/java/com/qihe/clipflow/data/bilibili/BilibiliCookieUtilsTest.kt
git -c safe.directory=E:/clioipflow/ClipFlow commit -m "feat: add Bilibili login protocol client"
```

### Task 3: Add the isolated Geetest view and local QR renderer

**Files:**
- Create: `app/src/main/java/com/qihe/clipflow/ui/bilibili/BilibiliCaptchaDialog.kt`
- Create: `app/src/main/java/com/qihe/clipflow/data/bilibili/BilibiliQrCode.kt`
- Modify: `app/build.gradle.kts`

**Interfaces:**
- `BilibiliCaptchaDialog(challenge, onResult, onDismiss)` returns `BilibiliCaptchaResult(token, validate, challenge, seccode)` through the callback.
- `generateBilibiliQrBitmap(content: String, size: Int): Bitmap` produces an app-owned bitmap from the Bilibili URL.

- [ ] **Step 1: Add the single QR dependency and verify Gradle can resolve it.**

Add the following dependency:

```kotlin
implementation("com.google.zxing:core:3.5.3")
```

Run `:app:dependencies --configuration debugCompileClasspath`. If the offline cache cannot resolve it, run the same command without `--offline` once, then repeat the offline command and record the resolved version in the plan execution notes.

- [ ] **Step 2: Implement and smoke-check QR bitmap generation by dimensions.**

Use `MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size)` and copy the matrix into an `ARGB_8888` Bitmap. Reject blank content and non-positive sizes with `require`. Add a debug-only call or instrumented smoke assertion for a fixed non-secret URL and verify `bitmap.width == size` and `bitmap.height == size`; do not put Android `Bitmap` assertions in the JVM unit-test task.

The implementation must use this guard before calling ZXing:

```kotlin
require(content.isNotBlank() && size > 0)
```

- [ ] **Step 3: Implement the constrained captcha dialog.**

Build a `Dialog` containing only a WebView-loaded Geetest widget. Load the Geetest script and call the widget with the challenge's `gt` and `challenge`; expose only the successful validation values through a `JavascriptInterface`. Disable file/content access, enable JavaScript and DOM storage, and destroy the WebView in `onDispose`. Never load `passport.bilibili.com/login` in this view.

- [ ] **Step 4: Build the app after adding the WebView and QR code components.**

Run `:app:assembleDebug`. Expected: compilation succeeds before the main login dialog is rewired.

- [ ] **Step 5: Commit the captcha and QR components.**

```powershell
git -c safe.directory=E:/clioipflow/ClipFlow add app/build.gradle.kts app/src/main/java/com/qihe/clipflow/data/bilibili/BilibiliQrCode.kt app/src/main/java/com/qihe/clipflow/ui/bilibili/BilibiliCaptchaDialog.kt
git -c safe.directory=E:/clioipflow/ClipFlow commit -m "feat: add Bilibili captcha and QR rendering"
```

### Task 4: Replace the manual WebView dialog with native login tabs

**Files:**
- Modify: `app/src/main/java/com/qihe/clipflow/ui/bilibili/BilibiliAccountAction.kt`

**Interfaces:**
- Keep `BilibiliAccountAction()` and the existing account dialog signature unchanged.
- Replace `BilibiliLoginDialog` with a dialog that owns `Phone` and `Qr` tab state and calls `BilibiliLoginClient`.

- [ ] **Step 1: Add UI state tests for validation guards.**

Cover pure helpers for `phone.trim().length >= 6`, a six-digit SMS code, and QR validity resetting to `180`. Keep `isBilibiliPhoneValid`, `isBilibiliSmsCodeValid`, and `bilibiliQrValiditySeconds` in `BilibiliLoginModels.kt` so they can be tested without Compose. Verify tab-switch and dialog-dismiss cancellation by checking the polling `Job` is cancelled in the Compose effect cleanup; no network call may continue after disposal.

- [ ] **Step 2: Remove the manual completion confirmation path.**

Delete `confirmLogin`, `loginError`, `readBilibiliCookies`, and the old WebView login form from `BilibiliAccountAction.kt`. Keep account display and logout behavior. Use one remembered `BilibiliLoginClient`, one coroutine scope, and a success callback that closes the dialog only after validation and `BilibiliSessionStore.save` complete.

- [ ] **Step 3: Render the tabbed native form.**

Use `TabRow` with `Phone login` and `QR login`. The phone tab contains a country-code prefix, numeric phone field, SMS code field, countdown button, login button, loading indicator, and an error label. The QR tab contains the generated bitmap, a 180-second countdown, a refresh icon button, polling status, and a retry action. Use existing Material 3 theme colors and compact spacing; do not embed the full Bilibili web page. Keep the exact tab labels `Phone login` and `QR login` so UI tests can find both controls.

- [ ] **Step 4: Wire the phone flow.**

On send-code, call `requestCaptcha`; if successful, show `BilibiliCaptchaDialog`, then call `sendSms`. Save the returned captcha key only in Compose state. On login, call `loginBySms`, then validate the returned Cookie with `BilibiliApiClient.authenticatedApi.navigation(cookie)`, build the same `BilibiliAccount` mapping used today, and save `BilibiliSession(cookie, account)`.

- [ ] **Step 5: Wire the QR flow and cancellation.**

On QR tab entry and refresh, call `generateQrCode`, render its URL, and launch a child polling coroutine with `delay(1000)`. Stop on `Success`, `Expired`, or dialog disposal. On `Success`, validate and save the captured Cookie. Do not use `CookieManager` to detect the new login.

- [ ] **Step 6: Compile the dialog and run focused tests.**

Run:

```powershell
& 'E:/clioipflow/.gradle-clipflow/wrapper/dists/gradle-9.6.0-bin/42k10rwplmzkhuboz9kdazi7s/gradle-9.6.0/bin/gradle.bat' --offline --no-daemon --no-watch-fs --console=plain -g E:/clioipflow/.gradle-clipflow :app:testDebugUnitTest --tests com.qihe.clipflow.data.bilibili.BilibiliLoginModelsTest --tests com.qihe.clipflow.data.bilibili.BilibiliLoginClientTest
& 'E:/clioipflow/.gradle-clipflow/wrapper/dists/gradle-9.6.0-bin/42k10rwplmzkhuboz9kdazi7s/gradle-9.6.0/bin/gradle.bat' --offline --no-daemon --no-watch-fs --console=plain -g E:/clioipflow/.gradle-clipflow :app:assembleDebug
```

Expected: no references to the removed manual confirmation flow remain, and the debug APK compiles.

- [ ] **Step 7: Commit the native login surface.**

```powershell
git -c safe.directory=E:/clioipflow/ClipFlow add app/src/main/java/com/qihe/clipflow/ui/bilibili/BilibiliAccountAction.kt app/src/main/java/com/qihe/clipflow/data/bilibili/BilibiliLoginModels.kt app/src/test/java/com/qihe/clipflow/data/bilibili/BilibiliLoginModelsTest.kt
git -c safe.directory=E:/clioipflow/ClipFlow commit -m "feat: add native Bilibili login tabs"
```

### Task 5: Full verification and device acceptance checklist

**Files:**
- Modify only files required by failed verification; do not alter version metadata.

- [ ] **Step 1: Run all JVM unit tests serially.**

```powershell
& 'E:/clioipflow/.gradle-clipflow/wrapper/dists/gradle-9.6.0-bin/42k10rwplmzkhuboz9kdazi7s/gradle-9.6.0/bin/gradle.bat' --offline --no-daemon --no-watch-fs --console=plain -g E:/clioipflow/.gradle-clipflow :app:testDebugUnitTest
```

Expected: all tests pass.

- [ ] **Step 2: Build the release APK.**

```powershell
& 'E:/clioipflow/.gradle-clipflow/wrapper/dists/gradle-9.6.0-bin/42k10rwplmzkhuboz9kdazi7s/gradle-9.6.0/bin/gradle.bat' --offline --no-daemon --no-watch-fs --console=plain -g E:/clioipflow/.gradle-clipflow :app:assembleRelease
```

Expected artifact: `E:/clioipflow/ClipFlow/app/build/outputs/apk/release/app-release.apk`.

- [ ] **Step 3: Inspect static quality and final scope.**

Run `git diff --check`, `rg -n "passport.bilibili.com/login|readBilibiliCookies|confirmLogin" app/src/main/java`, and `git status --short`. Expected: no full login-page load or manual completion references; only intended source, test, and plan files are changed.

- [ ] **Step 4: Perform Android 14 and Android 16 acceptance.**

For each device, open the Bilibili account action, verify both tabs render, complete QR login with the Bilibili app, complete SMS login with a test account, confirm the dialog closes automatically, reopen the account action, and parse one authorized Bilibili URL. Record that build validation does not replace physical-device acceptance.
