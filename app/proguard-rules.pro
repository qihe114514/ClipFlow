# Gson 泛型保护（R8 full mode 会删除泛型签名，导致 Class→ParameterizedType 转换失败）
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# 保护所有 API Model 不被混淆（Gson 反序列化依赖原名）
-keep class com.qihe.clipflow.data.api.model.** { *; }
-keep class com.qihe.clipflow.data.repository.ParseResult { *; }

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
