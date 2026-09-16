package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MdSubject
import com.example.data.model.cleanedForFeed
import com.example.repository.YocinemaRepository
import com.example.ui.components.MdPosterCard
import com.example.ui.components.MovieGridSkeleton
import com.example.ui.theme.YoBaseBackground
import com.example.ui.theme.YoPrimaryViolet
import com.example.ui.theme.YoSurface
import com.example.ui.theme.YoTextMuted
import com.example.ui.theme.YoTextPrimary
import kotlinx.coroutines.launch

@Composable
fun MoviesDemoBrowseScreen(
    title: String,
    countryFilter: String?,
    initialGenre: String? = null,
    initialSort: String? = null,
    searchQuery: String? = null,
    repository: YocinemaRepository,
    onBackClick: () -> Unit,
    onItemClick: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var items by remember { mutableStateOf<List<MdSubject>>(emptyList()) }
    var page by remember { mutableStateOf(1) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    suspend fun load(targetPage: Int, append: Boolean, forceRefresh: Boolean = false) {
        val typeParam = when (selectedTab) {
            1 -> "movie"
            2 -> "tv"
            else -> null
        }
        val results = if (!searchQuery.isNullOrBlank()) {
            val body = repository.moviesDemoRepository.search(query = searchQuery, limit = 24)
            (body?.effectiveItems ?: emptyList()).cleanedForFeed(countryFilter)
        } else {
            val body = repository.moviesDemoRepository.browse(
                type = typeParam,
                genre = initialGenre,
                country = countryFilter,
                sort = initialSort,
                page = targetPage,
                limit = 24,
                forceRefresh = forceRefresh,
            )
            (body?.effectiveItems ?: emptyList()).cleanedForFeed(countryFilter)
        }
        items = if (append) items + results else results
        hasMore = searchQuery.isNullOrBlank() && results.size >= 24
        loadFailed = results.isEmpty() && items.isEmpty()
    }

    LaunchedEffect(selectedTab, countryFilter, initialGenre, initialSort, searchQuery) {
        // Only show shimmer if we have nothing to display yet.
        if (items.isEmpty()) isLoading = true
        page = 1
        scope.launch {
            load(targetPage = 1, append = false)
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
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = YoTextPrimary
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = YoTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = YoSurface,
            contentColor = YoPrimaryViolet,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = YoPrimaryViolet
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("All", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Movies", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("TV Series", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isLoading && items.isEmpty()) {
            MovieGridSkeleton(columns = 3, itemCount = 12)
        } else if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MovieFilter,
                        contentDescription = null,
                        tint = YoTextMuted,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = if (loadFailed) "Couldn't load this right now" else "Nothing here yet",
                        fontSize = 14.sp,
                        color = YoTextMuted
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items) { subject ->
                    MdPosterCard(
                        subject = subject,
                        onClick = { subject.detailPath?.let(onItemClick) }
                    )
                }

                if (hasMore) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoadingMore) {
                                CircularProgressIndicator(color = YoPrimaryViolet, modifier = Modifier.padding(8.dp))
                            } else {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            isLoadingMore = true
                                            page += 1
                                            load(targetPage = page, append = true)
                                            isLoadingMore = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = YoSurface, contentColor = YoPrimaryViolet)
                                ) {
                                    Text("Load more", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}