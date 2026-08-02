### Task 1: Add and test the fractional pager-position helper

**Files:**
- Modify: `app/src/main/java/com/qihe/clipflow/navigation/Screen.kt`
- Test: `app/src/test/java/com/qihe/clipflow/navigation/PrimaryNavigationTest.kt`

**Interfaces:**
- Produces `pagerIndicatorPosition(currentPage: Int, currentPageOffsetFraction: Float, pageCount: Int): Float`.
- Returns a valid fractional position in `0f..(pageCount - 1).toFloat()`, or `0f` when `pageCount <= 1`.

- [ ] **Step 1: Write failing tests for forward, reverse, and boundary progress.**

Add these tests to `PrimaryNavigationTest`:

```kotlin
@Test
fun pagerIndicatorPositionFollowsFractionalPageOffset() {
    assertEquals(1.25f, pagerIndicatorPosition(1, 0.25f, 3), 0.0001f)
    assertEquals(0.6f, pagerIndicatorPosition(1, -0.4f, 3), 0.0001f)
}

@Test
fun pagerIndicatorPositionClampsToRegisteredPageBounds() {
    assertEquals(0f, pagerIndicatorPosition(0, -0.4f, 3), 0.0001f)
    assertEquals(2f, pagerIndicatorPosition(2, 0.4f, 3), 0.0001f)
    assertEquals(0f, pagerIndicatorPosition(0, 0.4f, 1), 0.0001f)
}
```

- [ ] **Step 2: Run the focused test and confirm it fails because the helper is absent.**

Run:

```powershell
& 'C:\Users\VOS-User\.gradle\wrapper\dists\gradle-9.6.0-bin\42k10rwplmzkhuboz9kdazi7s\gradle-9.6.0\bin\gradle.bat' -g E:/clioipflow/.gradle-clipflow --offline --no-daemon --no-watch-fs --console=plain :app:testDebugUnitTest --tests com.qihe.clipflow.navigation.PrimaryNavigationTest
```

Expected: compilation fails because `pagerIndicatorPosition` is not defined.

- [ ] **Step 3: Implement the minimal clamped helper in `Screen.kt`.**

Add:

```kotlin
fun pagerIndicatorPosition(
    currentPage: Int,
    currentPageOffsetFraction: Float,
    pageCount: Int,
): Float {
    if (pageCount <= 1) return 0f
    return (currentPage + currentPageOffsetFraction)
        .coerceIn(0f, (pageCount - 1).toFloat())
}
```

- [ ] **Step 4: Run the focused test and verify all navigation tests pass.**

Use the same Gradle command from Step 2. Expected: all `PrimaryNavigationTest` tests pass with zero failures.

- [ ] **Step 5: Commit the helper and tests.**

```powershell
git -c safe.directory=E:/clioipflow/ClipFlow add app/src/main/java/com/qihe/clipflow/navigation/Screen.kt app/src/test/java/com/qihe/clipflow/navigation/PrimaryNavigationTest.kt
git -c safe.directory=E:/clioipflow/ClipFlow commit -m "test: cover live pager indicator progress"
```
