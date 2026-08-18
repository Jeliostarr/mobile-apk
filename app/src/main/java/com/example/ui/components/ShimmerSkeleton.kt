package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.YoBorder
import com.example.ui.theme.YoSurfaceVariant

@Composable
fun ShimmerSkeleton(
    modifier: Modifier = Modifier,
    shapeRadius: Dp = 14.dp
) {
    val shimmerColors = listOf(
        YoSurfaceVariant,
        YoBorder.copy(alpha = 0.9f),
        YoSurfaceVariant
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = -600f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_anim"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim.value - 300f, 0f),
        end = Offset(translateAnim.value + 300f, 300f)
    )

    Box(
        modifier = modifier
            .background(brush = brush, shape = RoundedCornerShape(shapeRadius))
    )
}

@Composable
fun MovieCardSkeleton(
    modifier: Modifier = Modifier,
    widthDp: Dp? = null
) {
    val columnModifier = if (widthDp != null) modifier.width(widthDp) else modifier

    Column(modifier = columnModifier) {
        ShimmerSkeleton(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f),
            shapeRadius = 12.dp
        )
        Spacer(modifier = Modifier.height(8.dp))
        ShimmerSkeleton(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(14.dp),
            shapeRadius = 4.dp
        )
        Spacer(modifier = Modifier.height(4.dp))
        ShimmerSkeleton(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(12.dp),
            shapeRadius = 4.dp
        )
    }
}

@Composable
fun MovieGridSkeleton(
    modifier: Modifier = Modifier,
    columns: Int = 3,
    itemCount: Int = 9
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(itemCount) {
            MovieCardSkeleton()
        }
    }
}

@Composable
fun MovieRailSkeleton(
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShimmerSkeleton(
                modifier = Modifier
                    .width(130.dp)
                    .height(20.dp),
                shapeRadius = 4.dp
            )
            ShimmerSkeleton(
                modifier = Modifier
                    .width(60.dp)
                    .height(16.dp),
                shapeRadius = 4.dp
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(5) {
                MovieCardSkeleton(widthDp = 108.dp)
            }
        }
    }
}

@Composable
fun TranslatorRailSkeleton(
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShimmerSkeleton(
                modifier = Modifier
                    .width(120.dp)
                    .height(20.dp),
                shapeRadius = 4.dp
            )
            ShimmerSkeleton(
                modifier = Modifier
                    .width(60.dp)
                    .height(16.dp),
                shapeRadius = 4.dp
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(6) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ShimmerSkeleton(
                        modifier = Modifier.size(66.dp),
                        shapeRadius = 33.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ShimmerSkeleton(
                        modifier = Modifier
                            .width(60.dp)
                            .height(12.dp),
                        shapeRadius = 3.dp
                    )
                }
            }
        }
    }
}

@Composable
fun HomeSkeleton(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                ShimmerSkeleton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shapeRadius = 20.dp
                )
            }
        }

        item {
            TranslatorRailSkeleton()
        }

        item {
            MovieRailSkeleton()
        }

        item {
            MovieRailSkeleton()
        }
    }
}

/**
 * Full-screen skeleton for DetailScreen's initial load: mirrors the real
 * layout (backdrop → title block → action row → synopsis lines → cast
 * rail) so the shimmer reads as "this content is arriving" instead of a
 * generic spinner.
 */
@Composable
fun DetailSkeleton(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            ShimmerSkeleton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                shapeRadius = 0.dp
            )
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                ShimmerSkeleton(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(24.dp),
                    shapeRadius = 6.dp
                )
                Spacer(modifier = Modifier.height(10.dp))
                ShimmerSkeleton(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(14.dp),
                    shapeRadius = 4.dp
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ShimmerSkeleton(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shapeRadius = 12.dp
                    )
                    ShimmerSkeleton(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shapeRadius = 12.dp
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                ShimmerSkeleton(
                    modifier = Modifier.fillMaxWidth().height(14.dp),
                    shapeRadius = 4.dp
                )
                Spacer(modifier = Modifier.height(8.dp))
                ShimmerSkeleton(
                    modifier = Modifier.fillMaxWidth().height(14.dp),
                    shapeRadius = 4.dp
                )
                Spacer(modifier = Modifier.height(8.dp))
                ShimmerSkeleton(
                    modifier = Modifier.fillMaxWidth(0.6f).height(14.dp),
                    shapeRadius = 4.dp
                )
            }
        }

        item {
            TranslatorRailSkeleton()
        }
    }
}

/**
 * Skeleton for CastDetailScreen's initial load: avatar + name + bio lines.
 */
@Composable
fun CastDetailSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ShimmerSkeleton(
            modifier = Modifier.size(110.dp),
            shapeRadius = 55.dp
        )
        Spacer(modifier = Modifier.height(16.dp))
        ShimmerSkeleton(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(20.dp),
            shapeRadius = 6.dp
        )
        Spacer(modifier = Modifier.height(24.dp))
        ShimmerSkeleton(
            modifier = Modifier.fillMaxWidth().height(14.dp),
            shapeRadius = 4.dp
        )
        Spacer(modifier = Modifier.height(8.dp))
        ShimmerSkeleton(
            modifier = Modifier.fillMaxWidth().height(14.dp),
            shapeRadius = 4.dp
        )
        Spacer(modifier = Modifier.height(8.dp))
        ShimmerSkeleton(
            modifier = Modifier.fillMaxWidth(0.7f).height(14.dp),
            shapeRadius = 4.dp
        )
    }
}