package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val YoColorScheme = darkColorScheme(
    primary = YoPrimaryViolet,
    onPrimary = YoTextPrimary,
    primaryContainer = YoPrimaryVioletPress,
    onPrimaryContainer = YoTextPrimary,
    secondary = YoAccentCyan,
    onSecondary = YoBaseBackground,
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

// Neon Night shape scale — softer corners, used consistently
private val Shapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

// ─────────────────────────────────────────────────────────────
// Animation & Interaction Durations — for smooth, consistent feel
// ─────────────────────────────────────────────────────────────
object YoAnimationDurations {
    const val QUICK = 150       // fast taps, presses
    const val SHORT = 200       // standard interactions
    const val MEDIUM = 300      // transitions between states
    const val LONG = 500        // entrance/exit animations
    const val SPLASH = 2800     // splash screen duration
}

object YoElevationScale {
    const val NONE = 0
    const val TOUCH_PRESSED = 2
    const val CARD_IDLE = 4
    const val CARD_HOVER = 6
    const val MODAL = 12
}

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
        shapes = Shapes,
        content = content
    )
}
