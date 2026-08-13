package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.data.model.BASE_URL
import com.example.data.model.CastMember
import com.example.data.model.Episode
import com.example.data.model.Movie
import com.example.data.model.formatDuration
import com.example.ui.theme.YoBaseBackground
import com.example.ui.theme.YoBorder
import com.example.ui.theme.YoPrimaryAmber
import com.example.ui.theme.YoSurface
import com.example.ui.theme.YoSurfaceVariant
import com.example.ui.theme.YoTextMuted
import com.example.ui.theme.YoTextPrimary

/**
 * A clickable modifier that scales down slightly on press, in addition to
 * the normal ripple. This one small addition is most of what makes tappable
 * elements feel like a native app rather than a web page — nothing here
 * physically responds to touch otherwise.
 */
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
 * Thin bottom-edge scrim for legibility — anything (badges, labels) sitting
 * directly on top of arbitrary photo content needs this behind it, since a
 * still or poster's own colors can't be relied on for contrast.
 */
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(YoPrimaryAmber)
                    .border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = YoBaseBackground,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "YOCINEMA",
                fontWeight = FontWeight.Black,
                fontSize = 9.sp,
                letterSpacing = 1.sp,
                color = YoPrimaryAmber
            )
        }
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
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(18.dp), clip = false)
            .clip(RoundedCornerShape(18.dp))
            .background(YoSurface)
            .border(1.dp, YoBorder, RoundedCornerShape(18.dp))
            .pressScaleClickable(onClick)
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poster Left
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .aspectRatio(2f / 3f)
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(14.dp), clip = false)
                    .clip(RoundedCornerShape(14.dp))
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

            Spacer(modifier = Modifier.width(14.dp))

            // Title + Genre + Synopsis Right
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
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = YoPrimaryAmber,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                if (!movie.description.isNull_orBlank()) {
                    Text(
                        text = movie.description!!,
                        fontSize = 13.sp,
                        color = YoTextMuted,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
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
            .pressScaleClickable(onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .shadow(elevation = 5.dp, shape = RoundedCornerShape(14.dp), clip = false)
                .clip(RoundedCornerShape(14.dp))
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

            if (!movie.vjName.isNull_orBlank()) {
                // A scrim behind the badge — a solid-color pill sitting
                // directly on unpredictable poster art can vanish into it.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .align(Alignment.BottomCenter)
                        .background(BottomLegibilityScrim)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                ) {
                    VJBadgeChip(vjName = movie.vjName!!, onImage = true)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = movie.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = YoTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (!movie.genre.isNull_orBlank()) {
            Text(
                text = movie.genre!!,
                fontSize = 11.sp,
                color = YoTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (!movie.imdbRating.isNull_orBlank() || (movie.duration != null && movie.duration > 0)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!movie.imdbRating.isNull_orBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = YoPrimaryAmber,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = movie.imdbRating!!,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = YoTextPrimary
                        )
                    }
                }
                if (movie.duration != null && movie.duration > 0) {
                    Text(
                        text = com.example.data.model.formatDuration(movie.duration),
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
            .background(if (onImage) Color.Black.copy(alpha = 0.55f) else YoBorder)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "VJ $vjName".uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (onImage) YoPrimaryAmber else YoTextPrimary
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
            .clip(RoundedCornerShape(20.dp))
            .background(YoSurface)
            .border(1.dp, YoBorder, RoundedCornerShape(20.dp))
            .pressScaleClickable(onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(YoPrimaryAmber),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                color = YoBaseBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = vjName,
            fontSize = 13.sp,
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
                .shadow(elevation = 4.dp, shape = RoundedCornerShape(14.dp), clip = false)
                .clip(RoundedCornerShape(14.dp))
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

            // Scrim across the bottom so the S·E label and duration pill stay
            // legible no matter what color the still underneath happens to be.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .align(Alignment.BottomCenter)
                    .background(BottomLegibilityScrim)
            )

            // Duration Pill top right
            if (episode.duration != null && episode.duration > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = com.example.data.model.formatEpisodeDuration(episode.duration),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = YoTextPrimary
                    )
                }
            }

            // Play icon overlay center
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp)
                    .shadow(elevation = 3.dp, shape = CircleShape, clip = false)
                    .clip(CircleShape)
                    .background(YoBaseBackground.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = YoPrimaryAmber,
                    modifier = Modifier.size(24.dp)
                )
            }

            // S1 E2 bottom left — sits on the scrim above, so it stays
            // readable regardless of the thumbnail's own colors.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = "S${episode.sNum} E${episode.eNum}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

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
                .size(80.dp)
                .shadow(elevation = 4.dp, shape = CircleShape, clip = false)
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

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = cast.name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = YoTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (!cast.character.isNull_orBlank()) {
            Text(
                text = "as ${cast.character}",
                fontSize = 11.sp,
                color = YoTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun String?.isNull_orBlank(): Boolean = this == null || this.trim().isEmpty()
