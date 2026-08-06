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
import com.example.data.model.Movie
import com.example.repository.YocinemaRepository
import com.example.ui.components.HomeSkeleton
import com.example.ui.components.PosterCard
import com.example.ui.components.ShimmerSkeleton
import com.example.ui.components.SpotlightCard
import com.example.ui.components.VJChip
import com.example.ui.theme.YoBaseBackground
import com.example.ui.theme.YoBorder
import com.example.ui.theme.YoPrimaryAmber
import com.example.ui.theme.YoSurface
import com.example.ui.theme.YoTextMuted
import com.example.ui.theme.YoTextPrimary
import kotlinx.coroutines.launch

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
                // Spotlight Card
                spotlightMovie?.let { movie ->
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            SpotlightCard(
                                movie = movie,
                                onClick = { onMovieClick(movie.id) }
                            )
                        }
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
