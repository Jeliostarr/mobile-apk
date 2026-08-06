package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val YoColorScheme = darkColorScheme(
    primary = YoPrimaryAmber,
    onPrimary = YoBaseBackground,
    primaryContainer = YoPrimaryAmberPress,
    onPrimaryContainer = YoBaseBackground,
    background = YoBaseBackground,
    onBackground = YoTextPrimary,
    surface = YoSurface,
    onSurface = YoTextPrimary,
    surfaceVariant = YoSurfaceVariant,
    onSurfaceVariant = YoTextMuted,
    outline = YoBorder,
    error = YoDestructive,
    onError = YoTextPrimary
)

@Composable
fun YocinemaTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = YoBaseBackground.toArgb()
            window.navigationBarColor = YoBaseBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = YoColorScheme,
        typography = Typography,
        content = content
    )
}
