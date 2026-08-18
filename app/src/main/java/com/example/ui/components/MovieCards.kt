package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.R
import com.example.data.model.BASE_URL
import com.example.data.model.CastMember
import com.example.data.model.Episode
import com.example.data.model.Movie
import com.example.data.model.formatDuration
import com.example.ui.theme.YoAccentCyan
import com.example.ui.theme.YoBaseBackground
import com.example.ui.theme.YoBorder
import com.example.ui.theme.YoPrimaryViolet
import com.example.ui.theme.YoRatingGold
import com.example.ui.theme.YoSurface
import com.example.ui.theme.YoSurfaceVariant
import com.example.ui.theme.YoTextMuted
import com.example.ui.theme.YoTextPrimary

@Composable
private fun Modifier.pressScaleClickable(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "pressScale")
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            onClick = onClick
        )
}

/**
 * Same press-scale as pressScaleClickable, plus a hairline border that
 * glows violet while pressed. Used only where a border already exists
 * (poster cards) — the glow reads as "responsive surface," not just a
 * generic ripple, without adding a new visual language of its own.
 */
@Composable
private fun Modifier.pressGlowClickable(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "pressScale")
    val borderColor by animateColorAsState(
        targetValue = if (isPressed) YoPrimaryViolet.copy(alpha = 0.55f) else Color.Transparent,
        animationSpec = tween(180),
        label = "pressGlow"
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .border(1.5.dp, borderColor, RoundedCornerShape(18.dp))
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            onClick = onClick
        )
}

private val BottomLegibilityScrim = Brush.verticalGradient(
    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
)

@Composable
fun YoCinemaLogoPlaceholder(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(YoSurfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.yocinema_logo_1786014644709),
            contentDescription = "YOCINEMA Logo",
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )
    }
}

@Composable
fun SpotlightCard(
    movie: Movie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(26.dp), clip = false)
            .clip(RoundedCornerShape(26.dp))
            .background(YoSurface)
            .border(1.dp, YoBorder, RoundedCornerShape(26.dp))
            .pressScaleClickable(onClick)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .aspectRatio(2f / 3f)
                    .shadow(6.dp, RoundedCornerShape(18.dp), clip = false)
                    .clip(RoundedCornerShape(18.dp))
                    .background(YoSurfaceVariant)
            ) {
                SubcomposeAsyncImage(
                    model = movie.displayPosterUrl,
                    contentDescription = movie.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = { YoCinemaLogoPlaceholder() },
                    error = { YoCinemaLogoPlaceholder() }
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = movie.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = YoTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (!movie.genre.isNull_orBlank()) {
                    Text(
                        text = movie.genre!!,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = YoPrimaryViolet,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                if (!movie.description.isNull_orBlank()) {
                    Text(
                        text = movie.description!!,
                        fontSize = 14.sp,
                        color = YoTextMuted,
                        maxLines = 2,  // Limited to 2 lines for mobile readability
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )
                }

                if (!movie.vjName.isNull_orBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    VJBadgeChip(vjName = movie.vjName!!)
                }
            }
        }
    }
}

@Composable
fun PosterCard(
    movie: Movie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    widthDp: Int? = 130
) {
    val containerModifier = if (widthDp != null) {
        modifier.width(widthDp.dp)
    } else {
        modifier.fillMaxWidth()
    }

    Column(
        modifier = containerModifier
    ) {
        // ─────────────────────────────────────────────────────────────
        // POSTER BOX with overlays: Rating (top-right) + Type (top-left)
        // ─────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .shadow(6.dp, RoundedCornerShape(18.dp), clip = false)
                .clip(RoundedCornerShape(18.dp))
                .background(YoSurfaceVariant)
                .pressGlowClickable(onClick)
        ) {
            SubcomposeAsyncImage(
                model = movie.displayPosterUrl,
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = { YoCinemaLogoPlaceholder() },
                error = { YoCinemaLogoPlaceholder() }
            )

            // ── VJ Badge (bottom-left if present) ──
            if (!movie.vjName.isNull_orBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .align(Alignment.BottomCenter)
                        .background(BottomLegibilityScrim)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                ) {
                    VJBadgeChip(vjName = movie.vjName!!, onImage = true)
                }
            }

            // ── MOVIE/SERIES TYPE BADGE (top-left) ──
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(YoPrimaryViolet.copy(alpha = 0.85f))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (movie.isSeries) "SERIES" else "MOVIE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = YoTextPrimary,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // ─────────────────────────────────────────────────────────────
        // METADATA BELOW POSTER (title, first genre, duration)
        // ─────────────────────────────────────────────────────────────
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = movie.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = YoTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // First genre + duration on one row, opposite ends
        val firstGenre = movie.genre?.split(",")?.first()?.trim()
        val durationText = if (movie.duration != null && movie.duration > 0) formatDuration(movie.duration) else null

        if (firstGenre != null || durationText != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (firstGenre != null) {
                    Text(
                        text = firstGenre,
                        fontSize = 11.sp,
                        color = YoTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                if (durationText != null) {
                    Text(
                        text = durationText,
                        fontSize = 10.sp,
                        color = YoTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun VJBadgeChip(vjName: String, onImage: Boolean = false) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (onImage) Color.Black.copy(alpha = 0.6f) else YoBorder)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "VJ $vjName".uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (onImage) YoAccentCyan else YoTextPrimary
        )
    }
}

@Composable
fun VJChip(
    vjName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val initial = vjName.take(1).uppercase()
    Row(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(20.dp), clip = false)
            .clip(RoundedCornerShape(20.dp))
            .background(YoSurface)
            .border(1.dp, YoBorder, RoundedCornerShape(20.dp))
            .pressScaleClickable(onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(YoPrimaryViolet, YoAccentCyan))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                color = YoBaseBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = vjName,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = YoTextPrimary
        )
    }
}

@Composable
fun EpisodeCard(
    episode: Episode,
    movieId: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(200.dp)
            .pressScaleClickable(onClick)
    ) {
        Box(
            modifier = Modifier
                .width(200.dp)
                .aspectRatio(16f / 9f)
                .shadow(6.dp, RoundedCornerShape(18.dp), clip = false)
                .clip(RoundedCornerShape(18.dp))
                .background(YoSurfaceVariant)
        ) {
            SubcomposeAsyncImage(
                model = episode.getDisplayStill(movieId),
                contentDescription = episode.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = { YoCinemaLogoPlaceholder() },
                error = { YoCinemaLogoPlaceholder() }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.BottomCenter)
                    .background(BottomLegibilityScrim)
            )

            if (episode.duration != null && episode.duration > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = com.example.data.model.formatEpisodeDuration(episode.duration),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = YoTextPrimary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(44.dp)
                    .shadow(6.dp, CircleShape, clip = false)
                    .clip(CircleShape)
                    .background(YoBaseBackground.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = YoPrimaryViolet,
                    modifier = Modifier.size(28.dp)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = "S${episode.sNum} E${episode.eNum}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = episode.title ?: "Episode ${episode.eNum}",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = YoTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun CastAvatarCard(
    cast: CastMember,
    movieId: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(110.dp)
            .pressScaleClickable(onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val avatarUrl = cast.avatarUrl ?: "$BASE_URL/api/v1/movies/$movieId/cast/0/avatar"
        Box(
            modifier = Modifier
                .size(84.dp)
                .shadow(6.dp, CircleShape, clip = false)
                .clip(CircleShape)
                .background(YoSurfaceVariant)
                .border(2.dp, YoBorder, CircleShape)
        ) {
            SubcomposeAsyncImage(
                model = avatarUrl,
                contentDescription = cast.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = { YoCinemaLogoPlaceholder() },
                error = { YoCinemaLogoPlaceholder() }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = cast.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = YoTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (!cast.character.isNull_orBlank()) {
            Text(
                text = "as ${cast.character}",
                fontSize = 12.sp,
                color = YoTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun String?.isNull_orBlank(): Boolean = this == null || this.trim().isEmpty()
