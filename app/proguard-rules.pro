# ============================
# CRITICAL: Keep ALL bytecode attributes for Gson + Retrofit generic type resolution
# Without this, R8 strips generic signatures → ParameterizedType cast crash
# ============================
-keepattributes *

# ============================
# ApiService — Retrofit needs full generic return type info
# ============================
-keep interface com.qihe.clipflow.data.api.ApiService { *; }
-keep class com.qihe.clipflow.data.api.RetrofitClient { *; }

# ============================
# All data models — full preservation
# ============================
-keep class com.qihe.clipflow.data.api.model.** {
    <init>(...);
    <fields>;
}

# ============================
# Gson: prevent reflection-based deserialization crashes
# ============================
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.** { *; }
-keep class com.google.gson.internal.** { *; }
-keep class com.google.gson.stream.** { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-dontwarn com.google.gson.**
-dontwarn sun.misc.**

# ============================
# Retrofit — keep interface methods with full signatures
# ============================
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit

# ============================
# Room
# ============================
-keep class com.qihe.clipflow.data.local.** { *; }

# ============================
# Kotlin
# ============================
-keep class kotlin.Metadata { *; }
-keep class kotlin.coroutines.Continuation
-dontwarn kotlin.**
