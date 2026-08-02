## Commits

6f42148 test: cover live pager indicator progress

## Stat

 app/src/main/java/com/qihe/clipflow/navigation/Screen.kt    | 10 ++++++++++
 app/src/test/java/com/qihe/clipflow/navigation/PrimaryNavigationTest.kt | 13 +++++++++++++
 2 files changed, 23 insertions(+)

## Diff

```diff
diff --git a/app/src/main/java/com/qihe/clipflow/navigation/Screen.kt b/app/src/main/java/com/qihe/clipflow/navigation/Screen.kt
index c699202..184860a 100644
--- a/app/src/main/java/com/qihe/clipflow/navigation/Screen.kt
+++ b/app/src/main/java/com/qihe/clipflow/navigation/Screen.kt
@@ -80,6 +80,16 @@ fun primaryPageIndex(route: String?, items: List<BottomNavItem>): Int {
     return items.indexOfFirst { it.route == route }.coerceAtLeast(0)
 }
 
+fun pagerIndicatorPosition(
+    currentPage: Int,
+    currentPageOffsetFraction: Float,
+    pageCount: Int,
+): Float {
+    if (pageCount <= 1) return 0f
+    return (currentPage + currentPageOffsetFraction)
+        .coerceIn(0f, (pageCount - 1).toFloat())
+}
+
 fun NavHostController.navigateToPrimary(route: String) {
     navigate(route) {
         popUpTo(graph.findStartDestination().id) { saveState = true }
diff --git a/app/src/test/java/com/qihe/clipflow/navigation/PrimaryNavigationTest.kt b/app/src/test/java/com/qihe/clipflow/navigation/PrimaryNavigationTest.kt
index aade0a9..b497f7e 100644
--- a/app/src/test/java/com/qihe/clipflow/navigation/PrimaryNavigationTest.kt
+++ b/app/src/test/java/com/qihe/clipflow/navigation/PrimaryNavigationTest.kt
@@ -25,4 +25,17 @@ class PrimaryNavigationTest {
         assertEquals(0, primaryPageIndex("missing", bottomNavItems))
         assertEquals(1, primaryPageIndex("douyin", bottomNavItems))
     }
 
+    @Test
+    fun pagerIndicatorPositionFollowsFractionalPageOffset() {
+        assertEquals(1.25f, pagerIndicatorPosition(1, 0.25f, 3), 0.0001f)
+        assertEquals(0.6f, pagerIndicatorPosition(1, -0.4f, 3), 0.0001f)
+    }
+
+    @Test
+    fun pagerIndicatorPositionClampsToRegisteredPageBounds() {
+        assertEquals(0f, pagerIndicatorPosition(0, -0.4f, 3), 0.0001f)
+        assertEquals(2f, pagerIndicatorPosition(2, 0.4f, 3), 0.0001f)
+        assertEquals(0f, pagerIndicatorPosition(0, 0.4f, 1), 0.0001f)
+    }
 }
```
