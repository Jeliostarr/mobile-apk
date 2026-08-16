package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────
// Sora (headings/titles/numbers) + Manrope (body/labels/meta) is
// the intended pairing — tight tracking on display sizes, generous
// line-height on body. This file ships with a SYSTEM FONT FALLBACK
// so it compiles and looks intentional with zero new dependencies.
// Swap in real Sora/Manrope by uncommenting the Google Fonts block
// below once you've added the dependency to your version catalog —
// nothing else in the app needs to change, every screen reads
// `Typography` by name, not by font family.
// ─────────────────────────────────────────────────────────────

/*
// 1. Add to libs.versions.toml under [libraries]:
//    androidx-ui-text-google-fonts = { module = "androidx.compose.ui:ui-text-google-fonts", version = "1.7.5" }
// 2. In build.gradle.kts:
//    implementation(libs.androidx.ui.text.google.fonts)
// 3. Uncomment below and replace the FontFamily.Default assignments
//    for SoraFamily / ManropeFamily further down this file.

import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont

private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val SoraFamily = FontFamily(
    Font(GoogleFont("Sora"), fontProvider, FontWeight.Normal),
    Font(GoogleFont("Sora"), fontProvider, FontWeight.SemiBold),
    Font(GoogleFont("Sora"), fontProvider, FontWeight.Bold)
)
private val ManropeFamily = FontFamily(
    Font(GoogleFont("Manrope"), fontProvider, FontWeight.Normal),
    Font(GoogleFont("Manrope"), fontProvider, FontWeight.Medium),
    Font(GoogleFont("Manrope"), fontProvider, FontWeight.SemiBold)
)
*/

private val SoraFamily = FontFamily.Default
private val ManropeFamily = FontFamily.Default

val Typography = Typography(
    displaySmall = TextStyle(
        fontFamily = SoraFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = SoraFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = SoraFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = SoraFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = SoraFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp
    ),
    bodySmall = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp
    )
)
