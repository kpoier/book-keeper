# --- Retrofit & OkHttp ---
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# --- Gson & Data Models ---
# 防止 Gson 的 Data Class 欄位名稱被混淆
# 確保包含所有在 network package 下的資料類別 (AuthPayload, RecordPayload, RecordResponse, etc.)
-keep class com.example.book_keeper.network.** { *; }

# 防止所有使用了 SerializedName 註解的欄位被混淆，這是 Retrofit/Gson 解析的關鍵
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# --- AndroidX & Jetpack Compose ---
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# --- 保持所有的 Composable 函數 ---
-keepclasseswithmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# --- 保持所有的 Enum 類別 (如果有使用的話) ---
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}