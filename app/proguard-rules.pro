# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ============================================================
# ADD THESE RULES FOR YOUR APP (Moshi, Retrofit, Room, Firebase, Media3)
# ============================================================

# Keep your data/model classes (Moshi/JSON needs these)
-keep class com.example.data.model.** { *; }
-keep class com.example.data.local.** { *; }

# Keep Moshi's generated code and annotations
-keep class com.example.**.MoshiJsonAdapterFactory
-keep @com.squareup.moshi.JsonClass class *
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# Keep Retrofit interfaces
-keep interface com.example.**.api.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Keep Room database classes
-keep class com.example.data.local.AppDatabase { *; }
-keep class com.example.data.local.dao.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Keep Firebase/Play Services (avoid warnings)
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep Media3 ExoPlayer classes
-keep class androidx.media3.** { *; }

# Keep Kotlin metadata (for coroutines/reflection)
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keep class kotlin.Metadata { *; }

# Keep your ViewModels and Composables (to avoid reflection issues with SavedStateHandle)
-keep class com.example.ui.viewmodels.** { *; }

# Keep Compose UI internals (optional, but helps prevent crashes)
-keep class androidx.compose.** { *; }