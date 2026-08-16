package com.example.ui.components

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.YoAccentCyan
import com.example.ui.theme.YoBaseBackground
import com.example.ui.theme.YoPrimaryViolet
import com.example.ui.theme.YoTextMuted
import com.example.ui.theme.YoTextPrimary

@Composable
fun LoadingScreen(message: String? = null) {
    val transition = rememberInfiniteTransition(label = "loading")

    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "pulse"
    )
    val glowAlpha by transition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.60f,
        animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glow"
    )
    val sweepAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "sweep"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(YoBaseBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                // Soft two-layer glow behind the mark — no RenderEffect.blur
                // (that needs API 31+; minSdk here is 24), just stacked
                // radial gradients fading to transparent.
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(YoPrimaryViolet.copy(alpha = glowAlpha), Color.Transparent)
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(YoAccentCyan.copy(alpha = glowAlpha * 0.55f), Color.Transparent)
                            )
                        )
                )

                // Neon sweep ring
                Canvas(modifier = Modifier.size(102.dp)) {
                    val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(
                        brush = Brush.sweepGradient(listOf(YoPrimaryViolet, YoAccentCyan, YoPrimaryViolet)),
                        startAngle = sweepAngle,
                        sweepAngle = 250f,
                        useCenter = false,
                        style = stroke,
                        size = Size(size.width, size.height)
                    )
                }

                Image(
                    painter = painterResource(id = R.drawable.yocinema_logo_1786014644709),
                    contentDescription = "YOCINEMA Logo",
                    modifier = Modifier
                        .size(78.dp)
                        .clip(CircleShape)
                        .scale(pulse)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "YOCINEMA",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                color = YoTextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = message ?: "Loading your catalog",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = YoTextMuted
            )
        }
    }
}
