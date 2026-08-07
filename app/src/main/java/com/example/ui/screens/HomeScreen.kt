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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.SubcomposeAsyncImage
import com.example.data.model.Movie
import com.example.data.model.formatDuration
import com.example.data.model.isNull_orEmpty
import com.example.repository.YocinemaRepository
import com.example.ui.components.HomeSkeleton
import com.example.ui.components.PosterCard
import com.example.ui.components.ShimmerSkeleton
import com.example.ui.components.SpotlightCard
import com.example.ui.components.VJBadgeChip
import com.example.ui.components.VJChip
import com.example.ui.components.YoCinemaLogoPlaceholder
import com.example.ui.theme.YoBaseBackground
import com.example.ui.theme.YoBorder
import com.example.ui.theme.YoPrimaryAmber
import com.example.ui.theme.YoSurface
import com.example.ui.theme.YoTextMuted
import com.example.ui.theme.YoTextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HeroSliderPager(
    movies: List<Movie>,
    onMovieClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (movies.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { movies.size })

    LaunchedEffect(movies) {
        if (movies.size > 1) {
            while (true) {
                delay(4000)
                val nextPage = (pagerState.currentPage + 1) % movies.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
        ) { page ->
            val movie = movies[page]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(YoSurface)
                    .border(1.dp, YoBorder, RoundedCornerShape(20.dp))
                    .clickable { onMovieClick(movie.id) }
            ) {
                // Backdrop / Cover Image
                SubcomposeAsyncImage(
                    model = movie.cover ?: movie.poster ?: movie.displayPosterUrl,
                    contentDescription = movie.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = { YoCinemaLogoPlaceholder() },
                    error = { YoCinemaLogoPlaceholder() }
                )

                // Dark Scrim Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )

                // Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!movie.vjName.isNull_orEmpty()) {
                            VJBadgeChip(vjName = movie.vjName!!)
                        }
                        if (!movie.imdbRating.isNull_orEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = YoPrimaryAmber,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = movie.imdbRating!!,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        val metaInfo = listOfNotNull(
                            movie.releaseDate?.take(4),
                            if (movie.duration != null && movie.duration > 0) formatDuration(movie.duration) else null
                        ).joinToString(" • ")
                        if (metaInfo.isNotBlank()) {
                            Text(
                                text = metaInfo,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = movie.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!movie.description.isNull_orEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = movie.description!!,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onMovieClick(movie.id) },
                            modifier = Modifier.height(36.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = YoPrimaryAmber,
                                contentColor = YoBaseBackground
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Watch Now", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { onMovieClick(movie.id) },
                            modifier = Modifier.height(36.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Details", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        if (movies.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(movies.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .size(if (isSelected) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) YoPrimaryAmber else YoBorder)
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    repository: YocinemaRepository,
    onMovieClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onWatchlistClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onAccountClick: () -> Unit,
    onViewAllVJsClick: () -> Unit,
    onVJClick: (String) -> Unit,
    onViewAllCategoryClick: (title: String, sort: String?, type: String?, genre: String?) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var spotlightMovie by remember { mutableStateOf<Movie?>(null) }
    var popularMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var latestMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var seriesList by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var actionMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var vjsList by remember { mutableStateOf<List<String>>(emptyList()) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            try {
                val pop = repository.getMovies(sort = "popular", limit = 15)
                val lat = repository.getMovies(sort = "latest", limit = 15)
                val ser = repository.getMovies(type = "series", limit = 15)
                val act = repository.getMovies(genre = "Action", limit = 15)
                val facets = repository.getFacets()

                popularMovies = pop
                latestMovies = lat
                seriesList = ser
                actionMovies = act
                vjsList = facets.vjs ?: listOf("Soul", "Chambers", "Lenon", "Junior", "Emmy", "Kin", "Ulio")

                if (pop.isNotEmpty()) {
                    spotlightMovie = pop.first()
                } else if (lat.isNotEmpty()) {
                    spotlightMovie = lat.first()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(YoBaseBackground)
    ) {
        // Pinned Top Rounded Search Bar with Quick Action Icons
        HomeSearchBar(
            onSearchClick = onSearchClick,
            onWatchlistClick = onWatchlistClick,
            onDownloadsClick = onDownloadsClick,
            onAccountClick = onAccountClick
        )

        if (isLoading) {
            HomeSkeleton()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Sliding Hero Header with latest movies
                val heroList = latestMovies.take(5).ifEmpty { popularMovies.take(5) }
                if (heroList.isNotEmpty()) {
                    item {
                        HeroSliderPager(
                            movies = heroList,
                            onMovieClick = onMovieClick
                        )
                    }
                }

                // VJs (Translators) Rail
                if (vjsList.isNotEmpty()) {
                    item {
                        RailHeader(
                            title = "Translators",
                            onViewAllClick = onViewAllVJsClick
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(vjsList) { vjName ->
                                VJChip(
                                    vjName = vjName,
                                    onClick = { onVJClick(vjName) }
                                )
                            }
                        }
                    }
                }

                // Popular Rail
                if (popularMovies.isNotEmpty()) {
                    item {
                        MovieRailSection(
                            title = "Popular",
                            movies = popularMovies,
                            onMovieClick = onMovieClick,
                            onViewAllClick = { onViewAllCategoryClick("Popular", "popular", null, null) }
                        )
                    }
                }

                // Latest Rail
                if (latestMovies.isNotEmpty()) {
                    item {
                        MovieRailSection(
                            title = "Latest Releases",
                            movies = latestMovies,
                            onMovieClick = onMovieClick,
                            onViewAllClick = { onViewAllCategoryClick("Latest", "latest", null, null) }
                        )
                    }
                }

                // Series Rail
                if (seriesList.isNotEmpty()) {
                    item {
                        MovieRailSection(
                            title = "TV Series",
                            movies = seriesList,
                            onMovieClick = onMovieClick,
                            onViewAllClick = { onViewAllCategoryClick("Series", null, "series", null) }
                        )
                    }
                }

                // Action Movies Rail
                if (actionMovies.isNotEmpty()) {
                    item {
                        MovieRailSection(
                            title = "Action Movies",
                            movies = actionMovies,
                            onMovieClick = onMovieClick,
                            onViewAllClick = { onViewAllCategoryClick("Action", null, null, "Action") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeSearchBar(
    onSearchClick: () -> Unit,
    onWatchlistClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onAccountClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(YoSurface)
            .border(1.dp, YoBorder, RoundedCornerShape(20.dp))
            .clickable { onSearchClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = YoPrimaryAmber,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "Search Movies & Series",
                fontSize = 14.sp,
                color = YoTextMuted,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onWatchlistClick, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = "Watchlist",
                    tint = YoPrimaryAmber,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onDownloadsClick, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Downloads",
                    tint = YoPrimaryAmber,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onAccountClick, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Account",
                    tint = YoPrimaryAmber,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun RailHeader(
    title: String,
    onViewAllClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(18.dp)
                    .clip(CircleShape)
                    .background(YoPrimaryAmber)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = YoTextPrimary
            )
        }

        IconButton(onClick = onViewAllClick, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "View All",
                tint = YoTextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun MovieRailSection(
    title: String,
    movies: List<Movie>,
    onMovieClick: (String) -> Unit,
    onViewAllClick: () -> Unit
) {
    Column {
        RailHeader(title = title, onViewAllClick = onViewAllClick)
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(movies) { movie ->
                PosterCard(
                    movie = movie,
                    onClick = { onMovieClick(movie.id) }
                )
            }
        }
    }
}
