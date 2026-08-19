# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Strip line numbers and original source file name from stack traces in
# the shipped APK — keeps crash traces useless to anyone reading a
# decompiled build, without affecting your own debugging (you still have
# mapping.txt from this build to deobfuscate your own crash reports with).
-keepattributes SourceFile
-renamesourcefileattribute SourceFile
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# ============================================================
# Moshi / Retrofit / Room — the PREVIOUS version of this file kept entire
# packages ("com.example.data.model.**", "com.example.data.local.**",
# "com.example.ui.viewmodels.**") fully intact with `{ *; }`, which tells
# R8 to leave every class/method/field name in those packages exactly as
# written. That's most of the app's actual business logic shipped
# completely readable in the APK — it defeated obfuscation for the parts
# that matter most. The rules below keep only the annotation-level
# guarantees Moshi/Room/Retrofit's code-generation actually needs (they
# generate adapters/DAO impls at COMPILE time referencing your classes
# directly, so R8 can safely rename both sides together — obfuscation-
# safe by construction). Everything else is now free to be renamed.
# ============================================================

# Moshi: keep classes marked for codegen discoverable, and keep fields
# that carry an explicit @Json name mapping (needed if any field name
# differs from its JSON key).
-keep @com.squareup.moshi.JsonClass class * { <fields>; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keepclasseswithmembers class * {
    @com.squareup.moshi.JsonClass <init>(...);
}

# Retrofit: keep HTTP-annotated interface methods and their signatures —
# required so Retrofit can build requests via reflection at runtime.
-keepattributes Signature, Exceptions
-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}

# Room: entities/DAOs are wired at compile time by the KSP processor, so
# they don't need a blanket keep — only the RoomDatabase subclass itself
# needs a stable, discoverable shape.
-keep class * extends androidx.room.RoomDatabase

# Firebase / Play Services — these libraries use reflection internally
# and ship their own consumer rules, but this covers older versions.
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Media3 ExoPlayer — some renderers are looked up reflectively.
-keep class androidx.media3.** { *; }

# Kotlin coroutines/reflection metadata (small, not your business logic).
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keep class kotlin.Metadata { *; }

# ViewModels: only the constructor needs to survive (SavedStateHandle /
# ViewModelProvider resolve it via reflection) — not every member.
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# NOTE ON COMPOSE: the previous file had `-keep class androidx.compose.** { *; }`,
# fully un-obfuscating the entire Compose runtime/UI library. Compose ships
# its own correct consumer ProGuard rules bundled in its AAR, so this
# blanket rule wasn't protecting your app — it was just bloating the APK
# and leaving a large chunk of library internals fully readable for no
# functional reason. Removed; Compose will obfuscate normally and still
# work, since its own consumer-rules.pro (bundled in the library) already
# covers what it genuinely needs kept.
