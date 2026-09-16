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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.data.model.MdSubject
import com.example.data.model.cleanedForFeed
import com.example.repository.MdHomeRail
import com.example.repository.YocinemaRepository
import com.example.ui.components.MdPosterCard
import com.example.ui.components.MovieRailSkeleton
import com.example.ui.components.YoCinemaLogoPlaceholder
import com.example.ui.theme.YoBaseBackground
import com.example.ui.theme.YoBorder
import com.example.ui.theme.YoGlowGradient
import com.example.ui.theme.YoPrimaryViolet
import com.example.ui.theme.YoRatingGold
import com.example.ui.theme.YoSurface
import com.example.ui.theme.YoSurfaceVariant
import com.example.ui.theme.YoTextMuted
import com.example.ui.theme.YoTextPrimary
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

/**
 * Movies-demo home — standalone mirror of the translated HomeScreen.
 *
 * Layout: top bar → search bar → hero carousel → rails.
 *
 * Rails:
 *   1. "Trending Now"  ← /api/trending (real popularity signal)
 *   2. One rail per genre ← /api/browse?genre=X&sort=Hottest,
 *      fetched in parallel batches of 4
 *
 * No "Latest Releases" rail — trending replaces it.
 * No chip row, no CTA buttons on the hero. Genre browsing happens by
 * scrolling.
 */
@Composable
fun MoviesDemoHomeScreen(
    title: String,
    countryFilter: String?,
    repository: YocinemaRepository,
    onBackClick: () -> Unit,
    onItemClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onViewAllClick: (railTitle: String, genre: String?, sort: String?) -> Unit,
) {
    var rails by remember { mutableStateOf(repository.cachedMdHomeRails) }
    var isLoading by remember { mutableStateOf(rails.isEmpty()) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var reloadTick by remember { mutableStateOf(0) }

    LaunchedEffect(countryFilter, reloadTick) {
        if (rails.isEmpty()) isLoading = true
        loadError = null
        try {
            coroutineScope {
                // Genres drive the rail count.
                val filtersBody = repository.moviesDemoRepository.browseFilters()
                val genres = filtersBody?.genres.orEmpty()
                    .map { it.trim() }
                    .filter { it.isNotBlank() && !it.equals("all", ignoreCase = true) }
                    .distinct()

                // Rail #1: trending. This is the popularity signal we
                // also feed into the hero carousel below.
                val trendingDeferred = async {
                    repository.moviesDemoRepository.trending(limit = 30)
                        ?.effectiveItems
                        ?.cleanedForFeed(countryFilter)
                        .orEmpty()
                }
                val trending = trendingDeferred.await()

                val builtRails = mutableListOf<MdHomeRail>()
                if (trending.isNotEmpty()) {
                    builtRails.add(MdHomeRail("Trending Now", null, "Hottest", trending))
                }

                // Genre rails — same popularity ordering, fetched in
                // parallel batches of 4 to keep concurrency tame.
                val topGenres = genres.take(20)
                topGenres.chunked(4).forEach { chunk ->
                    val chunkResults = coroutineScope {
                        chunk.map { genre ->
                            async {
                                val body = repository.moviesDemoRepository.browse(
                                    country = countryFilter,
                                    genre = genre,
                                    sort = "Hottest",   // popularity, not Latest
                                    limit = 30,
                                )
                                genre to (body?.effectiveItems ?: emptyList())
                                    .cleanedForFeed(countryFilter)
                            }
                        }.awaitAll()
                    }
                    chunkResults.forEach { (genre, items) ->
                        if (items.isNotEmpty()) {
                            builtRails.add(MdHomeRail(genre, genre, "Hottest", items))
                        }
                    }
                }

                rails = builtRails
                if (builtRails.isNotEmpty()) {
                    repository.cachedMdHomeRails = builtRails
                    repository.cachedMdHomeFilters = filtersBody
                    repository.markMdHomeCacheFresh()
                }
            }
        } catch (e: Exception) {
            loadError = e.message ?: "Network error"
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(YoBaseBackground)
    ) {
        MdTopBar(
            title = title,
            onBackClick = onBackClick,
            onSearchClick = onSearchClick,
        )

        MdSearchBar(onClick = onSearchClick)

        Spacer(modifier = Modifier.height(12.dp))

        when {
            isLoading && rails.isEmpty() -> {
                Column {
                    MovieRailSkeleton()
                    Spacer(modifier = Modifier.height(8.dp))
                    MovieRailSkeleton()
                    Spacer(modifier = Modifier.height(8.dp))
                    MovieRailSkeleton()
                }
            }

            loadError != null && rails.isEmpty() -> {
                MdErrorState(
                    message = loadError!!,
                    onRetry = { reloadTick++ },
                )
            }

            rails.isEmpty() -> {
                MdErrorState(
                    message = "No content available right now.",
                    onRetry = { reloadTick++ },
                )
            }

            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    // Hero — first rail's items (i.e. trending). Same
                    // items appear again as the top rail right below.
                    val heroItems = rails.firstOrNull()?.items.orEmpty().take(6)
                    if (heroItems.isNotEmpty()) {
                        item {
                            MdHeroCarousel(
                                items = heroItems,
                                onItemClick = onItemClick,
                            )
                        }
                        item { Spacer(modifier = Modifier.height(22.dp)) }
                    }

                    items(rails) { rail ->
                        MdRailSection(
                            rail = rail,
                            onItemClick = onItemClick,
                            onViewAllClick = {
                                onViewAllClick(rail.title, rail.genre, rail.sort)
                            },
                        )
                        Spacer(modifier = Modifier.height(22.dp))
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Header
// ─────────────────────────────────────────────────────────────────

@Composable
private fun MdTopBar(
    title: String,
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = YoTextPrimary,
            )
        }
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = YoTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onSearchClick) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = YoPrimaryViolet,
            )
        }
    }
}

@Composable
private fun MdSearchBar(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(4.dp, RoundedCornerShape(20.dp), clip = false)
            .clip(RoundedCornerShape(20.dp))
            .background(YoSurface)
            .border(1.dp, YoBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = YoPrimaryViolet,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Search movies, series, genres…",
                fontSize = 14.sp,
                color = YoTextMuted,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Hero carousel — image + info only, no CTA buttons
// ─────────────────────────────────────────────────────────────────

@Composable
private fun MdHeroCarousel(
    items: List<MdSubject>,
    onItemClick: (String) -> Unit,
) {
    if (items.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { items.size })

    LaunchedEffect(items) {
        if (items.size > 1) {
            while (true) {
                delay(4500)
                val next = (pagerState.currentPage + 1) % items.size
                pagerState.animateScrollToPage(next)
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
        ) { page ->
            val item = items[page]
            MdHeroCard(
                item = item,
                onClick = { item.detailPath?.let(onItemClick) },
            )
        }

        if (items.size > 1) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(items.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .size(if (isSelected) 9.dp else 7.dp)
                            .clip(CircleShape)
                            .background(
                                brush = if (isSelected) {
                                    Brush.linearGradient(YoGlowGradient)
                                } else {
                                    SolidColor(YoBorder)
                                }
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun MdHeroCard(
    item: MdSubject,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .shadow(10.dp, RoundedCornerShape(22.dp), clip = false)
            .clip(RoundedCornerShape(22.dp))
            .background(YoSurface)
            .border(1.dp, YoBorder, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
    ) {
        SubcomposeAsyncImage(
            model = item.cover,
            contentDescription = item.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            loading = { YoCinemaLogoPlaceholder() },
            error = { YoCinemaLogoPlaceholder() }
        )

        // Gradient so title/meta stay legible over any poster.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.35f),
                            Color.Black.copy(alpha = 0.92f),
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val rating = item.effectiveRating
                if (!rating.isNullOrBlank()) {
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
                            tint = YoRatingGold,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = rating,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }

                val meta = listOfNotNull(
                    item.primaryGenre ?: item.effectiveCountry,
                    item.formattedDuration,
                ).joinToString(" • ")
                if (meta.isNotBlank()) {
                    Text(
                        text = meta,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.title ?: "",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (!item.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.description,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Rails
// ─────────────────────────────────────────────────────────────────

@Composable
private fun MdRailSection(
    rail: MdHomeRail,
    onItemClick: (String) -> Unit,
    onViewAllClick: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(16.dp)
                        .clip(CircleShape)
                        .background(Brush.verticalGradient(YoGlowGradient))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    rail.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = YoTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "View All",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = YoPrimaryViolet,
                modifier = Modifier.clickable(onClick = onViewAllClick),
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(rail.items) { subject: MdSubject ->
                MdPosterCard(
                    subject = subject,
                    onClick = { subject.detailPath?.let(onItemClick) },
                    modifier = Modifier.width(120.dp),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Error state
// ─────────────────────────────────────────────────────────────────

@Composable
private fun MdErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                text = "Couldn't load content",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = YoTextPrimary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                color = YoTextMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = YoPrimaryViolet,
                    contentColor = YoBaseBackground,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry", fontWeight = FontWeight.Bold)
            }
        }
    }
}