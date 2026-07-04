# Gson 泛型保护
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 保护所有 API Model 不被混淆（Gson 反序列化依赖原名）
-keep class com.qihe.clipflow.data.api.model.** { *; }
-keep class com.qihe.clipflow.data.repository.ParseResult { *; }

# Retrofit 接口保护（R8 会删泛型签名导致 Class cannot be cast to ParameterizedType）
-keep,allowobfuscation interface com.qihe.clipflow.data.api.ApiService { *; }

# Retrofit 保护
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# 友盟统计混淆规则
-keep class com.umeng.** {*;}
-keep class org.repackage.** {*;}
-keep class com.uyumao.** { *; }
-keepclassmembers class * {
   public <init> (org.json.JSONObject);
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
# 保护 R 文件不被混淆删除
-keep public class com.qihe.clipflow.R$*{
    public static final int *;
}
