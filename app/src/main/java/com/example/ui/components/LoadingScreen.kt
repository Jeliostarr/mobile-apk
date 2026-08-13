package com.example.ui.components

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.YoBaseBackground
import com.example.ui.theme.YoPrimaryAmber
import com.example.ui.theme.YoTextMuted
import com.example.ui.theme.YoTextPrimary

/**
 * Full-screen branded loading state — used on app launch (while we decide
 * Login vs. Home) and anywhere else a full-screen wait needs to feel like
 * part of the app rather than a bare spinner on a black background.
 */
@Composable
fun LoadingScreen(message: String? = null) {
    val transition = rememberInfiniteTransition(label = "splash")

    // A slow, gentle breathing pulse on the mark — enough to signal "alive"
    // without looking like a stuck/broken frame the way a static logo does
    // on a loading screen.
    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val dotPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dots"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(YoBaseBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .scale(pulse)
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(24.dp), clip = false, ambientColor = YoPrimaryAmber, spotColor = YoPrimaryAmber)
                    .clip(RoundedCornerShape(24.dp))
                    .background(YoPrimaryAmber),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = YoBaseBackground,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.padding(top = 20.dp))

            Text(
                text = "YOCINEMA",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                color = YoTextPrimary
            )

            Spacer(modifier = Modifier.padding(top = 6.dp))

            Text(
                text = message ?: "Loading your catalog",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = YoTextMuted
            )

            Spacer(modifier = Modifier.padding(top = 14.dp))

            Row {
                repeat(3) { i ->
                    val distance = kotlin.math.abs(dotPhase - i)
                    val active = distance < 0.5f
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (active) 7.dp else 5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (active) YoPrimaryAmber else YoTextMuted.copy(alpha = 0.35f))
                    )
                }
            }
        }
    }
}
