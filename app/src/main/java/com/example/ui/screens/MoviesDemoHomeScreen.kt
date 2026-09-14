package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.data.model.MdSubject
import com.example.data.model.cleanedForFeed
import com.example.repository.YocinemaRepository
import com.example.ui.components.MdPosterCard
import com.example.ui.components.MovieRailSkeleton
import com.example.ui.components.YoCinemaLogoPlaceholder
import com.example.ui.theme.YoBaseBackground
import com.example.ui.theme.YoGlowGradient
import com.example.ui.theme.YoPrimaryViolet
import com.example.ui.theme.YoRatingGold
import com.example.ui.theme.YoTextMuted
import com.example.ui.theme.YoTextPrimary
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

private data class MdRail(val title: String, val genre: String?, val sort: String?, val items: List<MdSubject>)

@Composable
fun MoviesDemoHomeScreen(
    title: String,
    countryFilter: String?,
    repository: YocinemaRepository,
    onBackClick: () -> Unit,
    onItemClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onViewAllClick: (railTitle: String, genre: String?, sort: String?) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var heroItems by remember { mutableStateOf<List<MdSubject>>(emptyList()) }
    var rails by remember { mutableStateOf<List<MdRail>>(emptyList()) }

    LaunchedEffect(countryFilter) {
        isLoading = true
        coroutineScope {
            val popularDeferred = async {
                runCatching { repository.moviesDemoApi.browse(country = countryFilter, sort = "Hottest", limit = 15) }.getOrNull()
            }
            val latestDeferred = async {
                runCatching { repository.moviesDemoApi.browse(country = countryFilter, sort = "Latest", limit = 15) }.getOrNull()
            }
            val filtersDeferred = async {
                runCatching { repository.moviesDemoApi.browseFilters() }.getOrNull()
            }

            val popularResp = popularDeferred.await()
            val popular = (if (popularResp?.isSuccessful == true) popularResp.body()?.effectiveItems else null)
                ?.cleanedForFeed(countryFilter) ?: emptyList()
            val latestResp = latestDeferred.await()
            val latest = (if (latestResp?.isSuccessful == true) latestResp.body()?.effectiveItems else null)
                ?.cleanedForFeed(countryFilter) ?: emptyList()
            val filtersResp = filtersDeferred.await()
            val allGenres = (if (filtersResp?.isSuccessful == true) filtersResp.body()?.genres else null) ?: emptyList()

            heroItems = (popular.ifEmpty { latest }).take(6)

            val topGenres = allGenres.take(6)
            val genreDeferreds = topGenres.map { g ->
                async {
                    g to runCatching {
                        repository.moviesDemoApi.browse(country = countryFilter, genre = g, limit = 15)
                    }.getOrNull()
                }
            }
            val genreResults = genreDeferreds.awaitAll()

            val builtRails = mutableListOf<MdRail>()
            if (popular.isNotEmpty()) builtRails.add(MdRail("Popular", genre = null, sort = "Hottest", items = popular))
            if (latest.isNotEmpty()) builtRails.add(MdRail("Latest", genre = null, sort = "Latest", items = latest))
            genreResults.forEach { (genre, response) ->
                val genreItems = (if (response?.isSuccessful == true) response.body()?.effectiveItems else null)?.cleanedForFeed(countryFilter) ?: emptyList()
                if (genreItems.isNotEmpty()) builtRails.add(MdRail(genre, genre = genre, sort = null, items = genreItems))
            }
            rails = builtRails
            isLoading = false
        }
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = YoTextPrimary)
            }
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = YoTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = YoPrimaryViolet)
            }
        }

        if (isLoading) {
            Column {
                MovieRailSkeleton()
                MovieRailSkeleton()
            }
        } else if (heroItems.isEmpty() && rails.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nothing here yet", color = YoTextMuted, fontSize = 14.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (heroItems.isNotEmpty()) {
                    item { MdHeroPager(items = heroItems, onItemClick = onItemClick) }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                items(rails) { rail ->
                    MdRailSection(
                        rail = rail,
                        onItemClick = onItemClick,
                        onViewAllClick = { onViewAllClick(rail.title, rail.genre, rail.sort) }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun MdHeroPager(items: List<MdSubject>, onItemClick: (String) -> Unit) {
    val pagerState = rememberPagerState(pageCount = { items.size })

    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
        val item = items[page]
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .clickable { item.detailPath?.let(onItemClick) }
        ) {
            SubcomposeAsyncImage(
                model = item.cover,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = { YoCinemaLogoPlaceholder() },
                error = { YoCinemaLogoPlaceholder() }
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
            )
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                val rating = item.effectiveRating
                if (!rating.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = YoRatingGold, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(rating, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Text(
                    text = item.title ?: "",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { item.detailPath?.let(onItemClick) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = YoPrimaryViolet, contentColor = YoBaseBackground)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("View", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }

    if (items.size > 1) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(items.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .size(if (isSelected) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Brush.linearGradient(YoGlowGradient) else Brush.linearGradient(listOf(YoTextMuted, YoTextMuted)))
                )
            }
        }
    }
}

@Composable
private fun MdRailSection(rail: MdRail, onItemClick: (String) -> Unit, onViewAllClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
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
                Text(rail.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = YoTextPrimary)
            }
            Text(
                text = "View All",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = YoPrimaryViolet,
                modifier = Modifier.clickable(onClick = onViewAllClick)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(rail.items) { subject ->
                MdPosterCard(
                    subject = subject,
                    onClick = { subject.detailPath?.let(onItemClick) },
                    modifier = Modifier.width(120.dp)
                )
            }
        }
    }
}
