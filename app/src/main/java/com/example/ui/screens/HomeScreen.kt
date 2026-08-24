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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.data.model.FacetsResponse
import com.example.data.model.Movie
import com.example.data.model.SportsMatch
import com.example.data.model.formatDuration
import com.example.data.model.isNull_orEmpty
import com.example.repository.SportsRepository
import com.example.repository.YocinemaRepository
import com.example.ui.components.HomeSkeleton
import com.example.ui.components.LiveBadge
import com.example.ui.components.PosterCard
import com.example.ui.components.VJBadgeChip
import com.example.ui.components.VJChip
import com.example.ui.components.YoCinemaLogoPlaceholder
import com.example.ui.theme.YoBaseBackground
import com.example.ui.theme.YoBorder
import com.example.ui.theme.YoPrimaryAmber
import com.example.ui.theme.YoSurface
import com.example.ui.theme.YoSurfaceVariant
import com.example.ui.theme.YoTextMuted
import com.example.ui.theme.YoTextPrimary
import com.example.ui.util.SportsTimeUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
                .height(280.dp)
        ) { page ->
            val movie = movies[page]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(10.dp, RoundedCornerShape(22.dp), clip = false)
                    .clip(RoundedCornerShape(22.dp))
                    .background(YoSurface)
                    .border(1.dp, YoBorder, RoundedCornerShape(22.dp))
                    .clickable { onMovieClick(movie.id) }
            ) {
                SubcomposeAsyncImage(
                    model = movie.heroImage ?: movie.cover ?: movie.poster ?: movie.displayPosterUrl,
                    contentDescription = movie.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = { YoCinemaLogoPlaceholder() },
                    error = { YoCinemaLogoPlaceholder() }
                )

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
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = YoPrimaryAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = movie.imdbRating!!,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        val metaInfo = listOfNotNull(
                            com.example.data.model.extractYearOnly(movie.releaseDate),
                            if (movie.duration != null && movie.duration > 0) formatDuration(movie.duration) else null
                        ).joinToString(" • ")
                        if (metaInfo.isNotBlank()) {
                            Text(
                                text = metaInfo,
                                fontSize = 12.sp,
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
                            .size(if (isSelected) 9.dp else 7.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) YoPrimaryAmber else YoBorder)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: YocinemaRepository,
    sportsRepository: SportsRepository,
    onMovieClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onWatchlistClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onAccountClick: () -> Unit,
    onViewAllVJsClick: () -> Unit,
    onVJClick: (String) -> Unit,
    onViewAllCategoryClick: (title: String, sort: String?, type: String?, genre: String?) -> Unit,
    onMatchClick: (String) -> Unit,
    onViewAllSportsClick: () -> Unit
) {
    var popularMovies by remember { mutableStateOf(repository.cachedPopularMovies) }
    var latestMovies by remember { mutableStateOf(repository.cachedLatestMovies) }
    var seriesList by remember { mutableStateOf(repository.cachedSeriesList) }
    var genreMovies by remember { mutableStateOf(repository.cachedGenreMovies) }
    var vjsList by remember { mutableStateOf(repository.cachedVjsList) }
    var isLoading by remember { mutableStateOf(popularMovies.isEmpty() && latestMovies.isEmpty()) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Sports rail: prefer live matches; if there aren't any right now, fall
    // back to upcoming ones with a countdown instead of just hiding the rail.
    var sportsMatches by remember { mutableStateOf<List<SportsMatch>>(emptyList()) }
    var sportsHasLive by remember { mutableStateOf(false) }

    // Hero carousel needs the same rich payload Detail screen uses
    // (heroImage/cover + full description) — the list endpoint that
    // populates popularMovies/latestMovies returns a lighter shape
    // without those fields, which is why the hero was blank before.
    // Seeded from the repository cache so a revisit doesn't blank the
    // carousel out and silently re-fetch 5 movie details in the background.
    var heroMovies by remember { mutableStateOf(repository.cachedHeroMovies) }

    val scope = rememberCoroutineScope()

    suspend fun fetchHomeMovies(forceRefresh: Boolean) {
        if (!forceRefresh && repository.isHomeCacheFresh()) {
            return
        }

        if (popularMovies.isEmpty()) {
            isLoading = true
        }
        try {
            // These four don't depend on each other — they were being
            // fetched one after another (four full round trips before
            // the genre batch even started), which was the main reason
            // cold loads felt slow. Launching all four up front and
            // awaiting them after means the wait is bounded by the
            // slowest single call instead of the sum of all four.
            lateinit var pop: List<Movie>
            lateinit var lat: List<Movie>
            lateinit var ser: List<Movie>
            lateinit var facets: FacetsResponse

            coroutineScope {
                val popDeferred = async { repository.getMovies(sort = "popular", limit = 15, forceRefresh = forceRefresh) }
                val latDeferred = async { repository.getMovies(sort = "latest", limit = 15, forceRefresh = forceRefresh) }
                val serDeferred = async { repository.getMovies(type = "series", limit = 15, forceRefresh = forceRefresh) }
                val facetsDeferred = async { repository.getFacets() }

                pop = popDeferred.await()
                lat = latDeferred.await()
                ser = serDeferred.await()
                facets = facetsDeferred.await()
            }

            val allMoviesPool = (pop + lat + ser).distinctBy { it.id }
            val allGenres = facets.genres.orEmpty().filter { it.isNotBlank() }

            // Genre rails used to each cost their own network round trip
            // (one request per genre, fired in parallel) even though
            // pop+lat+series had usually already pulled in most of the
            // catalog's popular titles. Building each rail from that
            // in-memory pool first — and only falling back to a network
            // call on the rare genre with nothing in the pool — cuts
            // this from "N extra requests every load" to "close to zero"
            // on a typical catalog, which is most of what was making
            // cold loads slow.
            val genreResults = coroutineScope {
                allGenres.map { genre ->
                    async {
                        val fromPool = allMoviesPool.filter {
                            it.genre?.contains(genre, ignoreCase = true) == true
                        }
                        val movies = if (fromPool.size >= 5) {
                            fromPool.take(15)
                        } else {
                            repository.getMovies(genre = genre, limit = 15, forceRefresh = forceRefresh).ifEmpty { fromPool }
                        }
                        genre to movies
                    }
                }.awaitAll()
            }.filter { (_, movies) -> movies.isNotEmpty() }
                .associate { it }

            // VJs sorted by how many movies they've translated (using the
            // same in-memory pool, same trade-off as genre rails above: an
            // approximation from ~45 fetched titles rather than a full
            // catalog-wide count, but no extra network calls needed for it).
            // Facets is still the source of truth for WHICH vjs exist —
            // this only decides the order they're shown in.
            val vjCounts = allMoviesPool
                .mapNotNull { it.vjName?.takeIf { name -> name.isNotBlank() } }
                .groupingBy { it }
                .eachCount()
            val sortedVjs = (facets.vjs ?: emptyList())
                .distinct()
                .ifEmpty { vjCounts.keys.toList() }
                .sortedByDescending { vjCounts[it] ?: 0 }

            popularMovies = pop
            latestMovies = lat
            seriesList = ser
            genreMovies = genreResults
            vjsList = sortedVjs

            repository.cachedPopularMovies = pop
            repository.cachedLatestMovies = lat
            repository.cachedSeriesList = ser
            repository.cachedGenreMovies = genreResults
            repository.cachedVjsList = sortedVjs
            repository.cachedFacets = facets
            repository.markHomeCacheFresh()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    suspend fun fetchHomeSports() {
        try {
            val live = sportsRepository.getMatches(status = "live", limit = 10)
            val upcoming = sportsRepository.getMatches(status = "upcoming", limit = 10)
            val upcomingSorted = upcoming.matches.sortedBy { it.kickoff ?: "" }
            // Live matches lead the rail when there are any; upcoming ones
            // fill out the rest of it right after — previously upcoming was
            // only ever shown when there were zero live matches, which made
            // the rail go nearly empty during quiet live moments even though
            // there were perfectly good upcoming matches to show.
            sportsMatches = (live.matches + upcomingSorted).distinctBy { it.id }.take(15)
            sportsHasLive = live.matches.isNotEmpty()
        } catch (e: Exception) {
            // Sports is a secondary rail on Home — a failure here shouldn't
            // block or blank out the rest of the screen, it should just
            // leave the rail hidden (see the isNotEmpty() check where it's
            // rendered below).
        }
    }

    LaunchedEffect(Unit) {
        // This used to run the full fetch burst unconditionally on every
        // single composition of HomeScreen — i.e. every time the Home tab
        // was reopened, even seconds after the last visit — which is the
        // main reason it felt slow "every open". Now it's skipped entirely
        // once a fresh cache exists; the screen just paints from cache
        // instantly with zero network calls.
        scope.launch { fetchHomeMovies(forceRefresh = false) }
        scope.launch { fetchHomeSports() }
    }

    // Fetch full detail (heroImage/cover + description) for just the
    // handful of movies shown in the hero carousel — same call Detail
    // screen makes, so the hero uses the identical image and has a
    // description to show. Re-runs only when the underlying light list
    // actually changes (not on every recomposition) — the ID comparison
    // below is what makes a revisit with a fresh cache a no-op, since
    // heroMovies is now seeded from the repository cache too.
    LaunchedEffect(popularMovies, latestMovies) {
        val lightHeroList = latestMovies.take(5).ifEmpty { popularMovies.take(5) }
        if (lightHeroList.isEmpty()) return@LaunchedEffect

        val alreadyEnriched = heroMovies.map { it.id } == lightHeroList.map { it.id }
        if (alreadyEnriched) return@LaunchedEffect

        try {
            val enriched = coroutineScope {
                lightHeroList.map { light ->
                    async {
                        try {
                            repository.getMovieDetail(light.id) ?: light
                        } catch (e: Exception) {
                            light
                        }
                    }
                }.awaitAll()
            }
            heroMovies = enriched
            repository.cachedHeroMovies = enriched
        } catch (e: Exception) {
            heroMovies = lightHeroList
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(YoBaseBackground)
    ) {
        HomeSearchBar(
            onSearchClick = onSearchClick,
            onWatchlistClick = onWatchlistClick,
            onDownloadsClick = onDownloadsClick,
            onAccountClick = onAccountClick
        )

        if (isLoading) {
            HomeSkeleton()
        } else {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        fetchHomeMovies(forceRefresh = true)
                        fetchHomeSports()
                        isRefreshing = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                val heroList = heroMovies.ifEmpty { latestMovies.take(10).ifEmpty { popularMovies.take(10) } }
                if (heroList.isNotEmpty()) {
                    item {
                        HeroSliderPager(
                            movies = heroList,
                            onMovieClick = onMovieClick
                        )
                    }
                }

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

                if (sportsMatches.isNotEmpty()) {
                    item {
                        HomeSportsRail(
                            matches = sportsMatches,
                            isLive = sportsHasLive,
                            onMatchClick = onMatchClick,
                            onViewAllClick = onViewAllSportsClick
                        )
                    }
                }

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

                genreMovies.forEach { (genre, movies) ->
                    item(key = "genre-$genre") {
                        MovieRailSection(
                            title = genre,
                            movies = movies,
                            onMovieClick = onMovieClick,
                            onViewAllClick = { onViewAllCategoryClick(genre, null, null, genre) }
                        )
                    }
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
            .shadow(4.dp, RoundedCornerShape(20.dp), clip = false)
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
                    imageVector = Icons.Default.VideoLibrary,
                    contentDescription = "Library",
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

        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(YoSurfaceVariant)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = LocalIndication.current,
                    onClick = onViewAllClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = "View All",
                tint = YoTextMuted,
                modifier = Modifier.size(18.dp)
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
    val screenWidthDp = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
    val cardWidthDp = ((screenWidthDp - 32 - 28) / 3).coerceAtLeast(100)

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
                    onClick = { onMovieClick(movie.id) },
                    widthDp = cardWidthDp
                )
            }
        }
    }
}
/**
 * Football rail for Home — shows live matches when there are any, falling
 * back to upcoming ones with a live countdown otherwise. Uses its own
 * compact card (not SportsComponents' MatchCard, which is built for a
 * full-width vertical list, not a fixed-width horizontal rail item).
 */
@Composable
fun HomeSportsRail(
    matches: List<SportsMatch>,
    isLive: Boolean,
    onMatchClick: (String) -> Unit,
    onViewAllClick: () -> Unit
) {
    if (matches.isEmpty()) return

    // Ticks periodically so an upcoming match's countdown actually counts
    // down instead of being frozen at whatever it read when the rail first
    // composed — shared by every card in the rail via a single timer.
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            tick++
        }
    }

    Column {
        RailHeader(
            // The rail itself is a mix of live + upcoming now (each card
            // shows its own live badge or countdown), so the header just
            // flags whether anything's live right now rather than claiming
            // the whole rail is one or the other.
            title = if (isLive) "Football \u2022 Live" else "Football",
            onViewAllClick = onViewAllClick
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(matches, key = { it.id }) { match ->
                key(tick) {
                    HomeMatchCard(match = match, onClick = { onMatchClick(match.id.toString()) })
                }
            }
        }
    }
}

@Composable
private fun HomeMatchCard(match: SportsMatch, onClick: () -> Unit) {
    val accentColor = if (match.live) Color(0xFFE53935) else YoPrimaryAmber

    Column(
        modifier = Modifier
            .width(150.dp)
            .shadow(4.dp, RoundedCornerShape(14.dp), clip = false)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(YoSurfaceVariant, YoSurface)
                )
            )
            .border(1.dp, YoBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (match.league?.img != null) {
                AsyncImage(
                    model = match.league.img,
                    contentDescription = null,
                    modifier = Modifier.size(11.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = match.league?.name ?: "Football",
                fontSize = 9.sp,
                color = YoTextMuted,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (match.live) {
                LiveBadge(compact = true)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeTeamBadge(name = match.home?.name, imageUrl = match.home?.img)
            Text("vs", fontSize = 9.sp, color = YoTextMuted, fontWeight = FontWeight.Bold)
            HomeTeamBadge(name = match.away?.name, imageUrl = match.away?.img)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(7.dp))
                .background(accentColor.copy(alpha = 0.14f))
                .padding(vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (match.live) {
                    "Watch Live"
                } else {
                    SportsTimeUtils.formatCountdown(match.kickoff).ifBlank {
                        SportsTimeUtils.formatTime(match.kickoff)
                    }
                },
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HomeTeamBadge(name: String?, imageUrl: String?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(50.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(YoBorder.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = name,
                    modifier = Modifier.size(21.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = name ?: "TBD",
            fontSize = 9.sp,
            color = YoTextPrimary,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
