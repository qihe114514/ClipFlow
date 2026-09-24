# Settings, Personalization, and Download Destinations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Settings and Personalization understandable for ordinary users while making selected media folders actually control where downloads are saved, without removing any advanced appearance controls.

**Architecture:** Keep the existing Compose screens and ViewModel ownership. Add one small destination-selection boundary mapping `ContentType` to the existing per-media preference flows, then let `MediaStoreHelper` choose either a user-authorized Storage Access Framework tree or the existing MediaStore default. Settings becomes task-oriented; Personalization exposes simple defaults with the existing technical controls under a collapsed Advanced appearance section.

**Tech Stack:** Kotlin 2.3.10, Jetpack Compose Material 3, DataStore Preferences, Storage Access Framework, `androidx.documentfile:documentfile:1.0.0`, JUnit 4.13.2, Gradle 9.6.0.

## Global Constraints

- Preserve every existing advanced appearance control and preference key.
- Custom video, image, and audio folders must be honored by ordinary downloads and Bilibili merged MP4 downloads.
- If a custom folder is configured but unavailable, report a save failure and do not fall back to MediaStore.
- File transfer and save operations run on `Dispatchers.IO`; cache files are deleted after success, failure, or cancellation.
- Do not redesign parsing, history, navigation, or media preview.
- Do not package an APK or change `versionCode` or `versionName`.
- Preserve the existing untracked plan files under `docs/superpowers/plans/`.

---

### Task 1: Establish the Gradle test baseline

**Files:**
- No source changes.

**Interfaces:**
- Produces a usable JDK 21 shell configuration and baseline Gradle command for later tasks, or records the exact verification blocker.

- [ ] **Step 1: Locate a usable JDK**

Run:

```powershell
$java = Get-Command java -ErrorAction SilentlyContinue
if ($java) { $java.Source }
Get-ChildItem 'C:\Program Files\Android\Android Studio\jbr','C:\Program Files\Java','C:\Program Files\Eclipse Adoptium' -Directory -ErrorAction SilentlyContinue |
    Select-Object -ExpandProperty FullName
```

Expected: a Java executable or candidate JDK roots are reported.

- [ ] **Step 2: Configure only this PowerShell session**

Run, replacing the path with the discovered JDK root:

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
```

Expected: Java 21 is reported, matching `app/build.gradle.kts`.

- [ ] **Step 3: Verify the existing test baseline**

Run:

```powershell
./gradlew.bat --offline --no-daemon --no-watch-fs --console=plain -g E:/clioipflow/.gradle-clipflow :app:testDebugUnitTest
```

Expected: zero test failures. If no JDK is available, stop verification and report that environmental blocker without claiming a passing baseline.

---

### Task 2: Decode stored navigation order safely and map media to destination preferences

**Files:**
- Modify: `app/src/main/java/com/qihe/clipflow/data/preferences/AppPreferences.kt`
- Create: `app/src/test/java/com/qihe/clipflow/data/preferences/AppPreferencesTest.kt`
- Create: `app/src/test/java/com/qihe/clipflow/data/preferences/MediaDestinationTest.kt`

**Interfaces:**
- Produces `internal fun parseBottomBarOrder(raw: String?): List<String>`.
- Produces `fun AppPreferences.destinationUri(type: ContentType): Flow<String>`.
- Produces `internal fun destinationPreferenceKey(type: ContentType): String` for JVM tests.

- [ ] **Step 1: Add failing tests**

Create `AppPreferencesTest.kt`:

```kotlin
package com.qihe.clipflow.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

class AppPreferencesTest {
    @Test
    fun malformedBottomBarOrderFallsBackToAllKnownRoutes() {
        assertEquals(
            listOf("home", "douyin", "xiaohongshu", "bilibili"),
            parseBottomBarOrder("not-json"),
        )
    }

    @Test
    fun validBottomBarOrderPreservesStoredSequence() {
        assertEquals(
            listOf("bilibili", "home", "douyin"),
            parseBottomBarOrder("[\"bilibili\",\"home\",\"douyin\"]"),
        )
    }

    @Test
    fun emptyOrWrongJsonFallsBackToAllKnownRoutes() {
        val expected = listOf("home", "douyin", "xiaohongshu", "bilibili")
        assertEquals(expected, parseBottomBarOrder(null))
        assertEquals(expected, parseBottomBarOrder("[]"))
        assertEquals(expected, parseBottomBarOrder("{\"route\":\"home\"}"))
    }
}
```

Create `MediaDestinationTest.kt`:

```kotlin
package com.qihe.clipflow.data.preferences

import com.qihe.clipflow.data.api.model.ContentType
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaDestinationTest {
    @Test
    fun destinationKeyMatchesEachMediaType() {
        assertEquals("video_save_path", destinationPreferenceKey(ContentType.VIDEO))
        assertEquals("video_save_path", destinationPreferenceKey(ContentType.LIVE_VIDEO))
        assertEquals("image_save_path", destinationPreferenceKey(ContentType.IMAGE))
        assertEquals("image_save_path", destinationPreferenceKey(ContentType.LIVE_IMAGE))
        assertEquals("audio_save_path", destinationPreferenceKey(ContentType.AUDIO))
    }
}
```

- [ ] **Step 2: Verify the tests fail**

Run:

```powershell
./gradlew.bat --offline --no-daemon --no-watch-fs --console=plain -g E:/clioipflow/.gradle-clipflow :app:testDebugUnitTest --tests com.qihe.clipflow.data.preferences.AppPreferencesTest --tests com.qihe.clipflow.data.preferences.MediaDestinationTest
```

Expected: compilation errors because the helpers do not yet exist.

- [ ] **Step 3: Add the minimal preference boundary**

In `AppPreferences.kt`, add imports for `ContentType` and `JsonParser`, then define:

```kotlin
private val defaultBottomBarOrder = listOf("home", "douyin", "xiaohongshu", "bilibili")

internal fun parseBottomBarOrder(raw: String?): List<String> {
    return runCatching {
        JsonParser.parseString(raw).asJsonArray
            .mapNotNull { value ->
                value.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
            }
            .takeIf { it.isNotEmpty() }
    }.getOrNull() ?: defaultBottomBarOrder
}

internal fun destinationPreferenceKey(type: ContentType): String = when (type) {
    ContentType.AUDIO -> "audio_save_path"
    ContentType.VIDEO, ContentType.LIVE_VIDEO -> "video_save_path"
    else -> "image_save_path"
}
```

Replace manual string splitting in `bottomBarOrder` with:

```kotlin
val bottomBarOrder: Flow<List<String>> = context.dataStore.data.map { prefs ->
    parseBottomBarOrder(prefs[KEY_BOTTOM_BAR_ORDER])
}
```

Add this extension after `AppPreferences`:

```kotlin
fun AppPreferences.destinationUri(type: ContentType): Flow<String> = when (type) {
    ContentType.AUDIO -> audioSavePath
    ContentType.VIDEO, ContentType.LIVE_VIDEO -> videoSavePath
    else -> imageSavePath
}
```

- [ ] **Step 4: Verify focused tests pass**

Run the command from Step 2.

Expected: all three preference tests pass.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/qihe/clipflow/data/preferences/AppPreferences.kt app/src/test/java/com/qihe/clipflow/data/preferences/AppPreferencesTest.kt app/src/test/java/com/qihe/clipflow/data/preferences/MediaDestinationTest.kt
git commit -m "refactor: centralize media destination preferences"
```

---

### Task 3: Save to a selected folder without silent fallback

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/com/qihe/clipflow/util/MediaStoreHelper.kt`
- Create: `app/src/test/java/com/qihe/clipflow/util/MediaSaveDestinationTest.kt`

**Interfaces:**
- Changes `MediaStoreHelper.saveToGallery(context, sourceFile, type, customTreeUri = null): Uri?`.
- A non-null custom URI uses only `DocumentFile.fromTreeUri` and `ContentResolver.openOutputStream`.
- A null custom URI preserves the existing MediaStore/legacy default behavior.
- Produces pure `internal fun saveDestinationMode(customTreeUri: String?): SaveDestinationMode` and `internal fun customDestinationFailureFallsBack(): Boolean` for regression tests.

- [ ] **Step 1: Add failing tests for explicit custom-folder behavior**

Create `MediaSaveDestinationTest.kt`:

```kotlin
package com.qihe.clipflow.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaSaveDestinationTest {
    @Test
    fun blankDestinationUsesTheExistingMediaStoreDefault() {
        assertEquals(SaveDestinationMode.MEDIA_STORE, saveDestinationMode(null))
        assertEquals(SaveDestinationMode.MEDIA_STORE, saveDestinationMode("  "))
    }

    @Test
    fun configuredDestinationUsesTheTreeAndNeverRequestsFallback() {
        assertEquals(
            SaveDestinationMode.CUSTOM_TREE,
            saveDestinationMode("content://com.android.externalstorage.documents/tree/primary%3ADownload"),
        )
    }

    @Test
    fun unavailableCustomFolderNeverFallsBackToMediaStore() {
        assertEquals(false, customDestinationFailureFallsBack())
    }
}
```

- [ ] **Step 2: Verify tests fail**

Run:

```powershell
./gradlew.bat --offline --no-daemon --no-watch-fs --console=plain -g E:/clioipflow/.gradle-clipflow :app:testDebugUnitTest --tests com.qihe.clipflow.util.MediaSaveDestinationTest
```

Expected: compilation errors for `SaveDestinationMode` and `saveDestinationMode`.

- [ ] **Step 3: Implement explicit destination routing**

Add the dependency in `app/build.gradle.kts` beside the AndroidX dependencies:

```kotlin
implementation("androidx.documentfile:documentfile:1.0.0")
```

In `MediaStoreHelper.kt`, add `DocumentFile`, `VisibleForTesting`, and a private mode:

```kotlin
internal enum class SaveDestinationMode { MEDIA_STORE, CUSTOM_TREE }

@VisibleForTesting
internal fun saveDestinationMode(customTreeUri: String?): SaveDestinationMode =
    if (customTreeUri.isNullOrBlank()) SaveDestinationMode.MEDIA_STORE else SaveDestinationMode.CUSTOM_TREE

@VisibleForTesting
internal fun customDestinationFailureFallsBack(): Boolean = false
```

Replace the entry point with:

```kotlin
fun saveToGallery(
    context: Context,
    sourceFile: File,
    type: ContentType,
    customTreeUri: Uri? = null,
): Uri? {
    return when (saveDestinationMode(customTreeUri?.toString())) {
        SaveDestinationMode.CUSTOM_TREE -> saveUsingTree(context, sourceFile, customTreeUri!!)
        SaveDestinationMode.MEDIA_STORE -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveUsingMediaStore(context, sourceFile, type)
            } else {
                saveUsingLegacy(context, sourceFile, type)
            }
        }
    }
}
```

Implement the tree branch. It must return `null` after deleting a partial document, and it must not call the default branch:

```kotlin
private fun saveUsingTree(context: Context, sourceFile: File, treeUri: Uri): Uri? {
    val directory = DocumentFile.fromTreeUri(context, treeUri) ?: return null
    val document = directory.createFile(getMimeType(sourceFile.name), sourceFile.name) ?: return null
    return try {
        val output = context.contentResolver.openOutputStream(document.uri)
            ?: error("无法打开目标文件")
        output.use { stream -> sourceFile.inputStream().use { input -> input.copyTo(stream) } }
        document.uri
    } catch (_: Exception) {
        document.delete()
        null
    }
}
```

In the existing MediaStore branch, replace the nullable output-stream block with:

```kotlin
val output = resolver.openOutputStream(uri) ?: error("无法打开目标文件")
output.use { stream -> sourceFile.inputStream().use { input -> input.copyTo(stream) } }
```

This ensures an unwritable MediaStore URI enters the existing delete-and-fail path.

Add an Android instrumentation test at `app/src/androidTest/java/com/qihe/clipflow/util/MediaStoreHelperInstrumentedTest.kt`. Construct an invalid document-tree URI, call `saveToGallery` with a small temporary file and that URI, assert the result is `null`, then query the applicable MediaStore collection for the generated display name and assert no row exists. This proves a configured but unusable tree fails rather than silently falling back.

- [ ] **Step 4: Verify focused tests and existing download tests pass**

Run:

```powershell
./gradlew.bat --offline --no-daemon --no-watch-fs --console=plain -g E:/clioipflow/.gradle-clipflow :app:testDebugUnitTest --tests com.qihe.clipflow.util.MediaSaveDestinationTest --tests com.qihe.clipflow.util.DownloadProgressTest
```

Expected: selected unit tests pass; when an emulator or device is attached, the instrumentation test passes. If no device is attached, record that as a separate verification limitation.

- [ ] **Step 5: Commit**

```powershell
git add app/build.gradle.kts app/src/main/java/com/qihe/clipflow/util/MediaStoreHelper.kt app/src/test/java/com/qihe/clipflow/util/MediaSaveDestinationTest.kt app/src/androidTest/java/com/qihe/clipflow/util/MediaStoreHelperInstrumentedTest.kt
git commit -m "fix: save media to custom folders"
```

---

### Task 4: Apply selected destinations to both download pipelines and serialize requests

**Files:**
- Modify: `app/src/main/java/com/qihe/clipflow/util/DownloadCoordinator.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/util/BilibiliDownloadManager.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/ui/bilibili/BilibiliViewModel.kt`
- Create: `app/src/test/java/com/qihe/clipflow/util/DownloadCoordinatorStateTest.kt`

**Interfaces:**
- `DownloadCoordinator` reads `AppPreferences.destinationUri(item.type).first()` before saving.
- `BilibiliDownloadManager` reads `destinationUri(ContentType.VIDEO).first()` before saving the merged MP4.
- Each pipeline permits one active request; a repeated request reopens the active progress state instead of creating a hidden parallel task.
- Produces `internal fun displayedDownloadItem(state: DownloadSessionState, requestedItemId: String): String`.

- [ ] **Step 1: Add the regression test**

Create `DownloadCoordinatorStateTest.kt`:

```kotlin
package com.qihe.clipflow.util

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadCoordinatorStateTest {
    @Test
    fun activeRequestRemainsTheDisplayedRequest() {
        val state = DownloadSessionState(
            downloadStates = mapOf("first" to DownloadState(isDownloading = true)),
            downloadingItemId = "first",
            isBackgroundDownload = true,
        )
        assertEquals("first", displayedDownloadItem(state, "second"))
    }

    @Test
    fun idleStateDisplaysTheRequestedItem() {
        assertEquals("second", displayedDownloadItem(DownloadSessionState(), "second"))
    }
}
```

- [ ] **Step 2: Verify the test fails**

Run:

```powershell
./gradlew.bat --offline --no-daemon --no-watch-fs --console=plain -g E:/clioipflow/.gradle-clipflow :app:testDebugUnitTest --tests com.qihe.clipflow.util.DownloadCoordinatorStateTest
```

Expected: compilation error for `displayedDownloadItem`.

- [ ] **Step 3: Implement one-active-download semantics and destination lookup**

In `DownloadCoordinator.kt`, replace the `jobs` map with:

```kotlin
private val preferences = AppPreferences(appContext)
private var activeJob: Job? = null
private var activeItemId: String? = null

internal fun displayedDownloadItem(state: DownloadSessionState, requestedItemId: String): String =
    state.downloadingItemId?.takeIf { state.downloadStates[it]?.isDownloading == true } ?: requestedItemId
```

At the start of `startDownload`, reopen a running task:

```kotlin
if (activeJob?.isActive == true) {
    _session.update {
        it.copy(
            showDownloadDialog = true,
            downloadingItemId = displayedDownloadItem(it, item.id),
            isBackgroundDownload = false,
        )
    }
    DownloadPillState.hide()
    return
}
```

Start the active job with the selected destination and guaranteed state cleanup:

```kotlin
activeItemId = item.id
activeJob = scope.launch {
    val customTreeUri = preferences.destinationUri(item.type).first()
        .trim()
        .takeIf { it.isNotEmpty() }
        ?.let(Uri::parse)
    try {
        val result = downloadManager.downloadWithProgress(item.url, buildFileName(item)) { state ->
            updateItemState(item.id, state)
        }
        result.onSuccess { tempFile ->
            try {
                val savedUri = withContext(Dispatchers.IO) {
                    MediaStoreHelper.saveToGallery(appContext, tempFile, item.type, customTreeUri)
                }
                val state = latestState(item.id)
                updateItemState(
                    item.id,
                    if (savedUri != null) state.copy(savedMediaUri = savedUri.toString())
                    else state.copy(isComplete = false, error = "保存到目标文件夹失败"),
                )
            } finally {
                tempFile.delete()
            }
        }
    } finally {
        activeJob = null
        activeItemId = null
    }
}
```

Add the imports for `Uri`, `AppPreferences`, `destinationUri`, `Dispatchers`, `withContext`, and `first`.

In `BilibiliDownloadManager.kt`, before the `DashMuxer.mux` call, resolve:

```kotlin
val customTreeUri = AppPreferences(context).destinationUri(ContentType.VIDEO).first()
    .trim()
    .takeIf { it.isNotEmpty() }
    ?.let(Uri::parse)
```

Pass `customTreeUri` to `MediaStoreHelper.saveToGallery`. This method is already inside `withContext(Dispatchers.IO)`, so no extra dispatcher wrapper is needed.

In `BilibiliViewModel.kt`, add `private var downloadJob: Job? = null`; change `fun download` to assign its `viewModelScope.launch` result to `downloadJob`, return when `downloadJob?.isActive == true`, and set `downloadJob = null` inside the coroutine's `finally` block.

- [ ] **Step 4: Verify selected tests and Kotlin compilation**

Run:

```powershell
./gradlew.bat --offline --no-daemon --no-watch-fs --console=plain -g E:/clioipflow/.gradle-clipflow :app:testDebugUnitTest --tests com.qihe.clipflow.util.DownloadCoordinatorStateTest --tests com.qihe.clipflow.util.DownloadProgressTest
./gradlew.bat --offline --no-daemon --no-watch-fs --console=plain -g E:/clioipflow/.gradle-clipflow :app:compileDebugKotlin
```

Expected: selected tests pass and Kotlin compilation succeeds.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/qihe/clipflow/util/DownloadCoordinator.kt app/src/main/java/com/qihe/clipflow/util/BilibiliDownloadManager.kt app/src/main/java/com/qihe/clipflow/ui/bilibili/BilibiliViewModel.kt app/src/test/java/com/qihe/clipflow/util/DownloadCoordinatorStateTest.kt
git commit -m "fix: route and serialize downloads"
```

---

### Task 5: Reorganize Settings around routine user tasks

**Files:**
- Modify: `app/src/main/java/com/qihe/clipflow/ui/settings/SettingsScreen.kt`
- Create: `app/src/test/java/com/qihe/clipflow/ui/settings/SettingsLabelsTest.kt`

**Interfaces:**
- Produces `internal fun launchPageOptions(): List<Pair<String, String>>`.
- Produces `internal fun defaultPathLabel(type: String): String`.
- The UI presents Download locations, Appearance, App layout, and Help/About.
- Folder summaries use `DocumentFile.name` or a stable human-readable fallback, never a raw URI.

- [ ] **Step 1: Add failing label tests**

Create `SettingsLabelsTest.kt`:

```kotlin
package com.qihe.clipflow.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsLabelsTest {
    @Test
    fun launchPageOptionsIncludeBilibili() {
        assertEquals(
            listOf("主页", "抖音", "小红书", "B 站"),
            launchPageOptions().map { it.second },
        )
    }

    @Test
    fun defaultsDescribeTheActualFolders() {
        assertEquals("Movies/ClipFlow", defaultPathLabel("video"))
        assertEquals("Pictures/ClipFlow", defaultPathLabel("image"))
        assertEquals("Music/ClipFlow", defaultPathLabel("audio"))
    }
}
```

- [ ] **Step 2: Verify the tests fail**

Run:

```powershell
./gradlew.bat --offline --no-daemon --no-watch-fs --console=plain -g E:/clioipflow/.gradle-clipflow :app:testDebugUnitTest --tests com.qihe.clipflow.ui.settings.SettingsLabelsTest
```

Expected: compilation errors for the label helpers.

- [ ] **Step 3: Implement the task-oriented Settings screen**

Add the helpers:

```kotlin
internal fun launchPageOptions() = listOf(
    "home" to "主页",
    "douyin" to "抖音",
    "xiaohongshu" to "小红书",
    "bilibili" to "B 站",
)

internal fun defaultPathLabel(type: String): String = when (type) {
    "video" -> "Movies/ClipFlow"
    "audio" -> "Music/ClipFlow"
    else -> "Pictures/ClipFlow"
}
```

Add a readable-folder helper:

```kotlin
private fun readableDirectoryName(context: Context, rawUri: String, fallback: String): String {
    if (rawUri.isBlank()) return fallback
    return DocumentFile.fromTreeUri(context, Uri.parse(rawUri))?.name ?: "已选择的文件夹"
}
```

Replace the existing path rows with full-width clickable rows. The row invokes its existing folder-picker launcher; a custom path adds a 40dp restore-default icon button with a descriptive `contentDescription`. Remove the current circular text buttons.

Render these groups and copy:

```text
下载位置
选择后，新的下载会保存到对应文件夹

外观
主题、壁纸和玻璃效果

应用布局
打开时显示
底部导航顺序

帮助与关于
关于 ClipFlow
重置新手教程
```

Use `launchPageOptions()` in place of the current three-item list. Keep the current reorder mutation, but make every arrow a 40dp icon button with descriptions such as `上移主页` and `下移抖音`. Keep the unused `savePath` state and setter untouched; it is existing out-of-scope dead state, not part of this change.

- [ ] **Step 4: Verify tests and compile**

Run:

```powershell
./gradlew.bat --offline --no-daemon --no-watch-fs --console=plain -g E:/clioipflow/.gradle-clipflow :app:testDebugUnitTest --tests com.qihe.clipflow.ui.settings.SettingsLabelsTest
./gradlew.bat --offline --no-daemon --no-watch-fs --console=plain -g E:/clioipflow/.gradle-clipflow :app:compileDebugKotlin
```

Expected: selected tests pass and Kotlin compilation succeeds.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/qihe/clipflow/ui/settings/SettingsScreen.kt app/src/test/java/com/qihe/clipflow/ui/settings/SettingsLabelsTest.kt
git commit -m "ux: reorganize settings tasks"
```

---

### Task 6: Simplify Personalization without removing any advanced option

**Files:**
- Modify: `app/src/main/java/com/qihe/clipflow/data/preferences/AppPreferences.kt`
- Modify: `app/src/main/java/com/qihe/clipflow/ui/personalization/PersonalizationScreen.kt`
- Create: `app/src/test/java/com/qihe/clipflow/ui/personalization/PersonalizationLogicTest.kt`

**Interfaces:**
- Produces `internal fun appearanceStrengthLabel(preset: IntensityPreset): String`.
- Produces `internal fun overallGlassPreset(state: PersonalizationUiState): IntensityPreset?`.
- Produces `PersonalizationViewModel.applyGlassStrength(preset: IntensityPreset)`.
- Produces atomic `AppPreferences.setGlassPresets(card, cardBlur, button, buttonBlur)`.

- [ ] **Step 1: Add failing tests**

Create `PersonalizationLogicTest.kt`:

```kotlin
package com.qihe.clipflow.ui.personalization

import com.qihe.clipflow.ui.component.IntensityPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PersonalizationLogicTest {
    @Test
    fun appearanceStrengthUsesResultLanguage() {
        assertEquals("关闭", appearanceStrengthLabel(IntensityPreset.OFF))
        assertEquals("柔和", appearanceStrengthLabel(IntensityPreset.LOW))
        assertEquals("默认", appearanceStrengthLabel(IntensityPreset.MEDIUM))
        assertEquals("强烈", appearanceStrengthLabel(IntensityPreset.HIGH))
    }

    @Test
    fun unmatchedAdvancedValuesAreCustom() {
        val state = PersonalizationUiState(
            cardPreset = IntensityPreset.HIGH.key,
            cardBlurPreset = IntensityPreset.LOW.key,
            buttonPreset = IntensityPreset.MEDIUM.key,
            buttonBlurPreset = IntensityPreset.HIGH.key,
        )
        assertNull(overallGlassPreset(state))
    }
}
```

- [ ] **Step 2: Verify tests fail**

Run:

```powershell
./gradlew.bat --offline --no-daemon --no-watch-fs --console=plain -g E:/clioipflow/.gradle-clipflow :app:testDebugUnitTest --tests com.qihe.clipflow.ui.personalization.PersonalizationLogicTest
```

Expected: compilation errors for the missing helpers.

- [ ] **Step 3: Add the overall-strength convenience control**

In `AppPreferences.kt`, add one atomic setter:

```kotlin
suspend fun setGlassPresets(card: String, cardBlur: String, button: String, buttonBlur: String) {
    context.dataStore.edit { prefs ->
        prefs[KEY_GLASS_CARD_PRESET] = card
        prefs[KEY_GLASS_CARD_BLUR_PRESET] = cardBlur
        prefs[KEY_GLASS_BUTTON_PRESET] = button
        prefs[KEY_GLASS_BUTTON_BLUR_PRESET] = buttonBlur
    }
}
```

In `PersonalizationScreen.kt`, define:

```kotlin
internal fun appearanceStrengthLabel(preset: IntensityPreset): String = when (preset) {
    IntensityPreset.OFF -> "关闭"
    IntensityPreset.LOW -> "柔和"
    IntensityPreset.MEDIUM -> "默认"
    IntensityPreset.HIGH -> "强烈"
}

internal fun overallGlassPreset(state: PersonalizationUiState): IntensityPreset? {
    val current = listOf(state.cardPreset, state.cardBlurPreset, state.buttonPreset, state.buttonBlurPreset)
    val presets = mapOf(
        IntensityPreset.OFF to listOf("off", "off", "off", "off"),
        IntensityPreset.LOW to listOf("low", "low", "low", "low"),
        IntensityPreset.MEDIUM to listOf("medium", "medium", "medium", "low"),
        IntensityPreset.HIGH to listOf("high", "high", "high", "high"),
    )
    return presets.entries.firstOrNull { it.value == current }?.key
}
```

Add this ViewModel method:

```kotlin
fun applyGlassStrength(preset: IntensityPreset) {
    val values = when (preset) {
        IntensityPreset.OFF -> listOf("off", "off", "off", "off")
        IntensityPreset.LOW -> listOf("low", "low", "low", "low")
        IntensityPreset.MEDIUM -> listOf("medium", "medium", "medium", "low")
        IntensityPreset.HIGH -> listOf("high", "high", "high", "high")
    }
    _uiState.value = _uiState.value.copy(
        cardPreset = values[0],
        cardBlurPreset = values[1],
        buttonPreset = values[2],
        buttonBlurPreset = values[3],
    )
    viewModelScope.launch { prefs.setGlassPresets(values[0], values[1], values[2], values[3]) }
}
```

Keep the live preview, theme mode, dynamic color, wallpaper source, and wallpaper visibility at the top. Add a four-option overall `玻璃效果强度` control with `appearanceStrengthLabel`. When `overallGlassPreset(uiState)` is null, display `自定义（可在高级外观中调整）`.

Rename the existing collapsible section to `高级外观`. Keep wallpaper contrast, card effect, card blur, button effect, and button blur in it, with their current expert labels and immediate persistence.

- [ ] **Step 4: Verify tests and compile**

Run:

```powershell
./gradlew.bat --offline --no-daemon --no-watch-fs --console=plain -g E:/clioipflow/.gradle-clipflow :app:testDebugUnitTest --tests com.qihe.clipflow.ui.personalization.PersonalizationLogicTest --tests com.qihe.clipflow.ui.theme.ThemeModeTest
./gradlew.bat --offline --no-daemon --no-watch-fs --console=plain -g E:/clioipflow/.gradle-clipflow :app:compileDebugKotlin
```

Expected: selected tests pass and Kotlin compilation succeeds.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/qihe/clipflow/data/preferences/AppPreferences.kt app/src/main/java/com/qihe/clipflow/ui/personalization/PersonalizationScreen.kt app/src/test/java/com/qihe/clipflow/ui/personalization/PersonalizationLogicTest.kt
git commit -m "ux: simplify personalization defaults"
```

---

### Task 7: Full verification and Android acceptance

**Files:**
- No source changes unless verification exposes a regression.

**Interfaces:**
- Verifies implementation against the approved design and release-metadata constraint.

- [ ] **Step 1: Run all unit tests and assemble debug**

Run:

```powershell
./gradlew.bat --offline --no-daemon --no-watch-fs --console=plain -g E:/clioipflow/.gradle-clipflow :app:testDebugUnitTest :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`, zero failures, and a non-empty `app/build/outputs/apk/debug/app-debug.apk`. Building a debug APK here does not change release metadata.

- [ ] **Step 2: Run scope and whitespace checks**

Run:

```powershell
git diff --check
rg -n "Text\\(path|content://" app/src/main/java/com/qihe/clipflow/ui/settings
git status --short
```

Expected: no whitespace errors; Settings has no raw URI output; the two pre-existing untracked plan files remain untouched.

- [ ] **Step 3: Test on Android 10 and Android 12+**

1. Confirm each Settings media row reports a readable default destination and the entire row opens the folder picker.
2. Select distinct custom folders for video, image, and audio; download one of each and confirm every file reaches its selected folder.
3. Select a custom video folder; download a Bilibili merged MP4; confirm it reaches that folder.
4. Revoke a selected tree permission; download again; confirm a save error appears and no file appears in the default MediaStore folder.
5. Select B 站 as the launch page; restart; confirm Bilibili opens first.
6. Reorder bottom tabs; restart; confirm ordering persists.
7. Confirm Personalization simple controls are understandable, the preview updates, and Advanced appearance retains contrast plus all card/button effect and blur controls.
8. On a narrow portrait device, confirm text and controls do not overlap or clip.

- [ ] **Step 4: Review the final change set**

Run:

```powershell
git log --oneline -6
git diff --name-only HEAD~5..HEAD
git diff HEAD~5..HEAD -- app/build.gradle.kts | Select-String -Pattern 'versionCode|versionName'
```

Expected: only planned dependency, preference, save, download, Settings, Personalization, and test changes appear; the final command prints no release metadata changes.


