package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FacetsResponse
import com.example.data.model.Movie
import com.example.repository.YocinemaRepository
import com.example.ui.components.ModernLoader
import com.example.ui.components.MovieGridSkeleton
import com.example.ui.components.PosterCard
import com.example.ui.theme.YoBaseBackground
import com.example.ui.theme.YoBorder
import com.example.ui.theme.YoPrimaryAmber
import com.example.ui.theme.YoSurface
import com.example.ui.theme.YoSurfaceVariant
import com.example.ui.theme.YoTextMuted
import com.example.ui.theme.YoTextPrimary
import kotlinx.coroutines.launch

@Composable
fun ExploreScreen(
    repository: YocinemaRepository,
    initialCategoryTitle: String? = null,
    initialSort: String? = null,
    initialType: String? = null,
    initialGenre: String? = null,
    onMovieClick: (String) -> Unit
) {
    var facets by remember { mutableStateOf(FacetsResponse()) }
    var moviesList by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var selectedGenre by remember { mutableStateOf(initialGenre ?: "All") }
    var selectedVj by remember { mutableStateOf("All") }
    var selectedCountry by remember { mutableStateOf("All") }
    var selectedYear by remember { mutableStateOf("All") }
    var selectedSort by remember { mutableStateOf(initialSort ?: "popular") }
    var selectedType by remember { mutableStateOf(initialType ?: "All") }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        facets = repository.getFacets()
    }

    LaunchedEffect(selectedGenre, selectedVj, selectedCountry, selectedYear, selectedSort, selectedType) {
        scope.launch {
            isLoading = true
            val genreParam = if (selectedGenre == "All") null else selectedGenre
            val vjParam = if (selectedVj == "All") null else selectedVj
            val countryParam = if (selectedCountry == "All") null else selectedCountry
            val yearParam = if (selectedYear == "All") null else selectedYear
            val sortParam = if (selectedSort == "All") null else selectedSort
            val typeParam = if (selectedType == "All") null else selectedType.lowercase()

            var res = repository.getMovies(
                genre = genreParam,
                vj = vjParam,
                country = countryParam,
                year = yearParam,
                sort = sortParam,
                type = typeParam,
                limit = 48
            )

            if (selectedGenre != "All") res = res.filter { it.genre?.contains(selectedGenre, ignoreCase = true) == true }
            if (selectedVj != "All") res = res.filter { it.vjName?.equals(selectedVj, ignoreCase = true) == true }
            if (selectedCountry != "All") res = res.filter { it.country?.equals(selectedCountry, ignoreCase = true) == true }
            if (selectedYear != "All") res = res.filter { it.releaseDate?.contains(selectedYear) == true }

            moviesList = res
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(YoBaseBackground)
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)) {
            Text(
                text = initialCategoryTitle ?: "Explore Catalogue",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = YoTextPrimary
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            val typeOptions = listOf("All", "Movies", "Series")
            items(typeOptions) { tOpt ->
                val isSelected = (tOpt == selectedType)
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedType = tOpt },
                    label = { Text(tOpt, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
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

            val sortOptions = listOf("popular", "latest", "rating", "featured")
            items(sortOptions) { sortOpt ->
                val isSelected = (sortOpt == selectedSort)
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedSort = sortOpt },
                    label = { Text("Sort: ${sortOpt.replaceFirstChar { it.uppercase() }}", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = YoSurfaceVariant,
                        selectedLabelColor = YoPrimaryAmber,
                        containerColor = YoSurface,
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

        val genreOptions = listOf("All") + (facets.genres ?: listOf("Action", "Comedy", "Drama", "Sci-Fi", "Thriller", "Horror", "Animation"))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 2.dp)
        ) {
            item {
                Text(
                    text = "Genre:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = YoTextMuted,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
            items(genreOptions) { g ->
                val isSelected = (g == selectedGenre)
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedGenre = g },
                    label = { Text(g, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = YoPrimaryAmber.copy(alpha = 0.2f),
                        selectedLabelColor = YoPrimaryAmber,
                        containerColor = YoSurface,
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

        if (!facets.vjs.isNullOrEmpty()) {
            val vjOptions = listOf("All") + facets.vjs!!
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                item {
                    Text(
                        text = "VJ:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = YoTextMuted,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                items(vjOptions) { vj ->
                    val isSelected = (vj == selectedVj)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedVj = vj },
                        label = { Text(vj, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = YoPrimaryAmber.copy(alpha = 0.2f),
                            selectedLabelColor = YoPrimaryAmber,
                            containerColor = YoSurface,
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
        }

        if (!facets.years.isNullOrEmpty()) {
            val yearOptions = listOf("All") + facets.years!!
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                item {
                    Text(
                        text = "Year:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = YoTextMuted,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                items(yearOptions) { yr ->
                    val isSelected = (yr == selectedYear)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedYear = yr },
                        label = { Text(yr, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = YoPrimaryAmber.copy(alpha = 0.2f),
                            selectedLabelColor = YoPrimaryAmber,
                            containerColor = YoSurface,
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
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isLoading) {
            MovieGridSkeleton(columns = 3, itemCount = 12)
        } else if (moviesList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No movies found for selected filters",
                    fontSize = 14.sp,
                    color = YoTextMuted
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(moviesList) { movie ->
                    PosterCard(
                        movie = movie,
                        onClick = { onMovieClick(movie.id) },
                        widthDp = null
                    )
                }
            }
        }
    }
}