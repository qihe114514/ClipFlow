# 保留 Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.douyin.downloaderqh.model.**$$serializer { *; }
-keepclassmembers class com.douyin.downloaderqh.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.douyin.downloaderqh.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
# Coil
-dontwarn coil.**
