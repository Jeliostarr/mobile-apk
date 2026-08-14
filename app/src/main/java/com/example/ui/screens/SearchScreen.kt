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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.ui.components.YoCinemaLogoPlaceholder
import com.example.data.model.Movie
import com.example.data.model.formatDuration
import com.example.repository.YocinemaRepository
import com.example.ui.components.ModernLoader
import com.example.ui.components.MovieGridSkeleton
import com.example.ui.components.VJBadgeChip
import com.example.ui.theme.YoBaseBackground
import com.example.ui.theme.YoBorder
import com.example.ui.theme.YoPrimaryAmber
import com.example.ui.theme.YoSurface
import com.example.ui.theme.YoSurfaceVariant
import com.example.ui.theme.YoTextMuted
import com.example.ui.theme.YoTextPrimary
import kotlinx.coroutines.delay

@Composable
fun SearchScreen(
    repository: YocinemaRepository,
    onBackClick: () -> Unit,
    onMovieClick: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var selectedFilter by remember { mutableStateOf("All") }
    var selectedSort by remember { mutableStateOf("Popularity") }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(query, selectedFilter, selectedSort) {
        if (query.trim().isBlank()) {
            searchResults = emptyList()
            isSearching = false
            return@LaunchedEffect
        }

        isSearching = true
        delay(300)

        try {
            val typeParam = when (selectedFilter) {
                "Movies" -> "movie"
                "Series" -> "series"
                else -> null
            }
            val vjParam = if (selectedFilter == "VJs") query.trim() else null
            val sortParam = when (selectedSort) {
                "Popularity" -> "popular"
                "Latest Release" -> "latest"
                "Rating" -> "rating"
                "Featured" -> "featured"
                else -> "popular"
            }

            var res = repository.getMovies(
                search = query.trim(),
                type = typeParam,
                vj = vjParam,
                sort = sortParam,
                limit = 40
            )

            if (selectedFilter == "VJs") {
                res = res.filter { !it.vjName.isNull_orEmpty() }
            } else if (selectedFilter == "Movies") {
                res = res.filter { !it.isSeries }
            } else if (selectedFilter == "Series") {
                res = res.filter { it.isSeries }
            }

            res = when (selectedSort) {
                "Latest Release" -> res.sortedByDescending { it.releaseDate ?: "" }
                "Rating" -> res.sortedByDescending { it.imdbRating?.toFloatOrNull() ?: 0f }
                "Popularity" -> res.sortedByDescending { it.imdbRatingCount ?: 0 }
                else -> res
            }

            searchResults = res
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isSearching = false
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
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = YoTextPrimary
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search titles, genres, VJs...", color = YoTextMuted, fontSize = 13.sp) },
                singleLine = true,
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = YoTextMuted
                            )
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = YoPrimaryAmber,
                    unfocusedBorderColor = YoBorder,
                    focusedContainerColor = YoSurface,
                    unfocusedContainerColor = YoSurface,
                    focusedTextColor = YoTextPrimary,
                    unfocusedTextColor = YoTextPrimary
                )
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            val filters = listOf("All", "Movies", "Series", "VJs")
            items(filters) { filter ->
                val isSelected = filter == selectedFilter
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = YoPrimaryAmber,
                        selectedLabelColor = YoBaseBackground,
                        containerColor = YoSurface,
                        labelColor = YoTextMuted
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = YoBorder,
                        selectedBorderColor = YoPrimaryAmber,
                        enabled = true,
                        selected = isSelected
                    )
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            val sortOptions = listOf("Popularity", "Latest Release", "Rating", "Featured")
            items(sortOptions) { sortOpt ->
                val isSelected = sortOpt == selectedSort
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedSort = sortOpt },
                    label = { Text("Sort: $sortOpt", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = YoSurfaceVariant,
                        selectedLabelColor = YoPrimaryAmber,
                        containerColor = YoBaseBackground,
                        labelColor = YoTextMuted
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (isSelected) YoPrimaryAmber else YoBorder,
                        selectedBorderColor = YoPrimaryAmber,
                        enabled = true,
                        selected = isSelected
                    )
                )
            }
        }

        if (isSearching) {
            MovieGridSkeleton(columns = 3, itemCount = 9)
        } else if (query.isBlank()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = YoBorder,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Search YOCINEMA Catalogue",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = YoTextMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Type a title, genre (e.g. Action) or VJ (e.g. Soul)",
                        fontSize = 12.sp,
                        color = YoTextMuted
                    )
                }
            }
        } else if (searchResults.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No titles found matching \"$query\"",
                    fontSize = 14.sp,
                    color = YoTextMuted
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(searchResults) { movie ->
                    SearchResultRow(
                        movie = movie,
                        onClick = { onMovieClick(movie.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun SearchResultRow(
    movie: Movie,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(14.dp), clip = false)
            .clip(RoundedCornerShape(14.dp))
            .background(YoSurface)
            .border(1.dp, YoBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(70.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(10.dp))
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

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = movie.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = YoTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (!movie.vjName.isNull_orEmpty()) {
                VJBadgeChip(vjName = movie.vjName!!)
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (!movie.genre.isNull_orEmpty()) {
                Text(
                    text = movie.genre!!,
                    fontSize = 12.sp,
                    color = YoPrimaryAmber,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (movie.duration != null && movie.duration > 0) {
                Text(
                    text = formatDuration(movie.duration),
                    fontSize = 11.sp,
                    color = YoTextMuted
                )
            }
        }
    }
}

private fun String?.isNull_orEmpty(): Boolean = this == null || this.trim().isEmpty()