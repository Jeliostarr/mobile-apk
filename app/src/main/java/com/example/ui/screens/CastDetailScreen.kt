package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.ui.components.YoCinemaLogoPlaceholder
import com.example.data.model.CastDetail
import com.example.data.model.FilmographyItem
import com.example.repository.YocinemaRepository
import com.example.ui.components.ModernLoader
import com.example.ui.components.VJBadgeChip
import com.example.ui.theme.YoBaseBackground
import com.example.ui.theme.YoBorder
import com.example.ui.theme.YoPrimaryAmber
import com.example.ui.theme.YoSuccessGreen
import com.example.ui.theme.YoSurface
import com.example.ui.theme.YoSurfaceVariant
import com.example.ui.theme.YoTextMuted
import com.example.ui.theme.YoTextPrimary
import kotlinx.coroutines.launch

@Composable
fun CastDetailScreen(
    castId: String,
    repository: YocinemaRepository,
    onBackClick: () -> Unit,
    onMovieClick: (String) -> Unit
) {
    var castDetail by remember { mutableStateOf<CastDetail?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(castId) {
        isLoading = true
        castDetail = repository.getCastDetail(castId)
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(YoBaseBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = YoTextPrimary
                )
            }
            Text(
                text = "Cast Profile",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = YoTextPrimary
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                ModernLoader()
            }
        } else if (castDetail == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Cast information unavailable", color = YoTextMuted)
            }
        } else {
            val c = castDetail!!
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(6.dp, RoundedCornerShape(18.dp), clip = false)
                            .clip(RoundedCornerShape(18.dp))
                            .background(YoSurface)
                            .border(1.dp, YoBorder, RoundedCornerShape(18.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(YoSurfaceVariant)
                        ) {
                            SubcomposeAsyncImage(
                                model = c.photo,
                                contentDescription = c.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                loading = { YoCinemaLogoPlaceholder() },
                                error = { YoCinemaLogoPlaceholder() }
                            )
                        }

                        Spacer(modifier = Modifier.width(18.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = c.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = YoTextPrimary
                            )
                            if (!c.birthday.isNull_orBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Born: ${c.birthday}",
                                    fontSize = 13.sp,
                                    color = YoTextMuted
                                )
                            }
                            if (!c.placeOfBirth.isNull_orBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = c.placeOfBirth!!,
                                    fontSize = 13.sp,
                                    color = YoTextMuted
                                )
                            }
                        }
                    }
                }

                if (!c.bio.isNull_orBlank()) {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Biography",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = YoTextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = c.bio!!,
                            fontSize = 14.sp,
                            color = YoTextMuted,
                            lineHeight = 20.sp
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(28.dp))
                    Text(
                        text = "Filmography",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = YoTextPrimary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                val filmography = c.filmography.orEmpty()
                items(filmography) { item ->
                    FilmographyRow(
                        castId = castId,
                        item = item,
                        repository = repository,
                        onMovieClick = onMovieClick
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun FilmographyRow(
    castId: String,
    item: FilmographyItem,
    repository: YocinemaRepository,
    onMovieClick: (String) -> Unit
) {
    val versionMovieId = item.versions?.firstOrNull()?.movieId
    val isAvailable = item.onYocinema || !versionMovieId.isNull_orBlank()
    var isRequested by remember { mutableStateOf(false) }
    var isRequesting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(12.dp), clip = false)
            .clip(RoundedCornerShape(12.dp))
            .background(YoSurface)
            .border(1.dp, YoBorder, RoundedCornerShape(12.dp))
            .clickable(enabled = isAvailable) {
                if (!versionMovieId.isNull_orBlank()) {
                    onMovieClick(versionMovieId!!)
                }
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(55.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
                .background(YoSurfaceVariant)
        ) {
            SubcomposeAsyncImage(
                model = item.poster,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = { YoCinemaLogoPlaceholder() },
                error = { YoCinemaLogoPlaceholder() }
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isAvailable) YoTextPrimary else YoTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!item.character.isNull_orBlank()) {
                Text(
                    text = "as ${item.character}",
                    fontSize = 13.sp,
                    color = YoTextMuted
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            if (isAvailable) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(YoSuccessGreen.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "AVAILABLE ON YOCINEMA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = YoSuccessGreen
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isRequested) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(YoPrimaryAmber.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "REQUEST SENT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = YoPrimaryAmber
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(YoPrimaryAmber)
                                .clickable(enabled = !isRequesting) {
                                    isRequesting = true
                                    scope.launch {
                                        val tmdbId = item.tmdbId ?: item.id ?: ""
                                        val mediaType = item.mediaType ?: "movie"
                                        repository.requestCastMovie(
                                            castId = castId,
                                            tmdbId = tmdbId,
                                            mediaType = mediaType,
                                            title = item.title,
                                            poster = item.poster
                                        )
                                        isRequesting = false
                                        isRequested = true
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isRequesting) "Sending..." else "Request Title",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = YoBaseBackground
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun String?.isNull_orBlank(): Boolean = this == null || this.trim().isEmpty()