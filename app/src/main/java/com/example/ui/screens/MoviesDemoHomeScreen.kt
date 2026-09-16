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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MdSubject
import com.example.data.model.cleanedForFeed
import com.example.repository.MdHomeRail
import com.example.repository.YocinemaRepository
import com.example.ui.components.MdPosterCard
import com.example.ui.components.MovieRailSkeleton
import com.example.ui.theme.YoBaseBackground
import com.example.ui.theme.YoBorder
import com.example.ui.theme.YoGlowGradient
import com.example.ui.theme.YoPrimaryViolet
import com.example.ui.theme.YoSurfaceVariant
import com.example.ui.theme.YoTextMuted
import com.example.ui.theme.YoTextPrimary

/**
 * Movies-demo home.
 *
 * Data path: one call to /api/home (which proxies the upstream app's
 * home feed), which already gives us ordered sections. Everything the
 * backend emits becomes a rail — no per-genre fan-out on the client.
 *
 * Header layout is the MovieBox pattern: sticky top bar with back /
 * title / search, then a horizontally-scrolling chip row for jumping
 * into a genre, then rails.
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
    var filters by remember { mutableStateOf(repository.cachedMdHomeFilters) }
    var isLoading by remember { mutableStateOf(rails.isEmpty()) }

    LaunchedEffect(countryFilter) {
        if (rails.isEmpty()) isLoading = true
        try {
            // Prefer the /api/home feed — one request, ordered sections.
            val home = repository.moviesDemoRepository.home()

            val builtRails = mutableListOf<MdHomeRail>()
            if (home != null) {
                home.sections.forEach { section ->
                    val items = section.items
                        .cleanedForFeed(countryFilter)
                        .filter { !it.detailPath.isNullOrBlank() }
                    if (items.isNotEmpty()) {
                        // Sections come pre-ordered from the backend — pass
                        // genre=null / sort=null so "View All" simply opens
                        // a browse screen for that title's items.
                        builtRails.add(MdHomeRail(section.title, null, null, items))
                    }
                }
            }

            // Fallback: if /api/home came back empty (network hiccup,
            // backend not deployed yet) keep the old behaviour so the
            // screen is never blank.
            if (builtRails.isEmpty()) {
                val popular = repository.moviesDemoRepository.browse(
                    country = countryFilter, sort = "Hottest", limit = 40
                )?.effectiveItems?.cleanedForFeed(countryFilter).orEmpty()
                val latest = repository.moviesDemoRepository.browse(
                    country = countryFilter, sort = "Latest", limit = 40
                )?.effectiveItems?.cleanedForFeed(countryFilter).orEmpty()
                if (popular.isNotEmpty()) builtRails.add(MdHomeRail("Popular Now", null, "Hottest", popular))
                if (latest.isNotEmpty())  builtRails.add(MdHomeRail("Latest Releases", null, "Latest", latest))
            }

            val filtersBody = repository.moviesDemoRepository.browseFilters()

            rails = builtRails
            filters = filtersBody
            repository.cachedMdHomeRails = builtRails
            repository.cachedMdHomeFilters = filtersBody
            repository.markMdHomeCacheFresh()
        } catch (e: Exception) {
            e.printStackTrace()
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

        MdCategoryChips(
            genres = filters?.genres.orEmpty(),
            onCategoryClick = { genre ->
                if (genre == null) onViewAllClick("All Titles", null, "Hottest")
                else               onViewAllClick(genre, genre, null)
            },
        )

        Spacer(modifier = Modifier.height(6.dp))

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

            rails.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nothing here yet", color = YoTextMuted, fontSize = 14.sp)
                }
            }

            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
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
            .padding(horizontal = 12.dp, vertical = 12.dp),
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
private fun MdCategoryChips(
    genres: List<String>,
    onCategoryClick: (String?) -> Unit,
) {
    val categories = remember(genres) {
        buildList {
            add("For You")
            genres.forEach { g -> if (g.isNotBlank()) add(g) }
        }
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 4.dp),
    ) {
        items(categories) { cat ->
            val isPrimary = cat == "For You"
            MdCategoryChip(
                label = cat,
                isPrimary = isPrimary,
                onClick = {
                    if (cat == "For You") onCategoryClick(null)
                    else                 onCategoryClick(cat)
                },
            )
        }
    }
}

@Composable
private fun MdCategoryChip(
    label: String,
    isPrimary: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (isPrimary) YoPrimaryViolet else YoSurfaceVariant)
            .border(
                width = 1.dp,
                color = if (isPrimary) Color.Transparent else YoBorder,
                shape = RoundedCornerShape(50),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isPrimary) YoBaseBackground else YoTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// Rail
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