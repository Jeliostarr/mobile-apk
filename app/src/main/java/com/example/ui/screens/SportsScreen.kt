package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsSoccer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SportsFilter
import com.example.data.model.SportsLeague
import com.example.data.model.SportsMatch
import com.example.repository.SportsRepository
import com.example.ui.components.LeagueChip
import com.example.ui.components.MatchCard
import com.example.ui.components.SportsListSkeleton
import com.example.ui.components.SportsSectionHeader
import com.example.ui.theme.YoBaseBackground
import com.example.ui.theme.YoPrimaryViolet
import com.example.ui.theme.YoSurface
import com.example.ui.theme.YoTextMuted
import com.example.ui.theme.YoTextPrimary
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@Composable
fun SportsScreen(
    sportsRepository: SportsRepository,
    onMatchClick: (matchId: String) -> Unit
) {
    // Defaults to ALL (live + upcoming combined) so the screen is never
    // empty just because nothing happens to be live right now — a live-only
    // filter is still one tap away.
    var selectedFilter by remember { mutableStateOf(SportsFilter.ALL) }
    var selectedLeague by remember { mutableStateOf<String?>(null) }
    var leagues by remember { mutableStateOf<List<SportsLeague>>(emptyList()) }

    // Populated for the ALL view (sectioned). Single-status views use `matches` instead.
    var liveMatches by remember { mutableStateOf<List<SportsMatch>>(emptyList()) }
    var upcomingMatches by remember { mutableStateOf<List<SportsMatch>>(emptyList()) }
    var matches by remember { mutableStateOf<List<SportsMatch>>(emptyList()) }

    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var refreshTick by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        leagues = sportsRepository.getLeagues()
    }

    LaunchedEffect(selectedFilter, selectedLeague, refreshTick) {
        isLoading = true
        loadError = null

        fun applyLeagueFilter(list: List<SportsMatch>): List<SportsMatch> {
            val needle = selectedLeague?.lowercase() ?: return list
            return list.filter { (it.league?.name ?: "").lowercase() == needle }
        }

        try {
            if (selectedFilter == SportsFilter.ALL) {
                val (liveResp, upcomingResp) = coroutineScope {
                    val liveDeferred = async { sportsRepository.getMatches(status = "live", limit = 30) }
                    val upcomingDeferred = async { sportsRepository.getMatches(status = "upcoming", limit = 30) }
                    liveDeferred.await() to upcomingDeferred.await()
                }
                liveMatches = applyLeagueFilter(liveResp.matches)
                upcomingMatches = applyLeagueFilter(upcomingResp.matches)
                    .sortedBy { it.kickoff ?: "" }
                matches = emptyList()

                if (!liveResp.success && !upcomingResp.success) {
                    loadError = "Couldn't load matches — check your connection and try again."
                }
            } else {
                val response = sportsRepository.getMatches(
                    status = selectedFilter.apiValue,
                    league = selectedLeague,
                    limit = 30
                )
                matches = response.matches
                liveMatches = emptyList()
                upcomingMatches = emptyList()
                if (!response.success && response.matches.isEmpty()) {
                    loadError = "Couldn't load matches — check your connection and try again."
                }
            }
        } catch (e: Exception) {
            loadError = "Couldn't load matches — check your connection and try again."
        }

        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(YoBaseBackground)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.SportsSoccer,
                contentDescription = null,
                tint = YoPrimaryViolet,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Sports",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = YoTextPrimary
                )
                val totalCount = if (selectedFilter == SportsFilter.ALL) {
                    liveMatches.size + upcomingMatches.size
                } else {
                    matches.size
                }
                if (!isLoading && totalCount > 0) {
                    Text(
                        text = if (liveMatches.isNotEmpty() && selectedFilter == SportsFilter.ALL)
                            "${liveMatches.size} live now"
                        else
                            "$totalCount match${if (totalCount == 1) "" else "es"}",
                        fontSize = 12.sp,
                        color = YoTextMuted
                    )
                }
            }
            IconButton(onClick = { refreshTick++ }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = YoTextMuted)
            }
        }

        // Status filter pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SportsFilter.values().forEach { filter ->
                val isSelected = filter == selectedFilter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) YoPrimaryViolet else YoSurface)
                        .clickable { selectedFilter = filter }
                        .padding(horizontal = 18.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = filter.label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) Color.Black else YoTextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // League filter row
        if (leagues.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    LeagueChip(
                        name = "All leagues",
                        imageUrl = null,
                        isSelected = selectedLeague == null,
                        onClick = { selectedLeague = null }
                    )
                }
                items(leagues) { league ->
                    LeagueChip(
                        name = league.name,
                        imageUrl = league.img,
                        isSelected = selectedLeague == league.name,
                        onClick = { selectedLeague = if (selectedLeague == league.name) null else league.name }
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Content
        Box(modifier = Modifier.fillMaxSize()) {
            val isEmpty = if (selectedFilter == SportsFilter.ALL) {
                liveMatches.isEmpty() && upcomingMatches.isEmpty()
            } else {
                matches.isEmpty()
            }

            when {
                isLoading -> {
                    SportsListSkeleton()
                }
                loadError != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("⚠️", fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(loadError ?: "", color = YoTextMuted, fontSize = 14.sp, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            "Tap to retry",
                            color = YoPrimaryViolet,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { refreshTick++ }
                        )
                    }
                }
                isEmpty -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SportsSoccer,
                            contentDescription = null,
                            tint = YoTextMuted,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = when (selectedFilter) {
                                SportsFilter.ALL -> "No live or upcoming matches right now"
                                SportsFilter.LIVE -> "No matches live right now"
                                SportsFilter.UPCOMING -> "No upcoming matches found"
                                SportsFilter.ENDED -> "No finished matches yet"
                            },
                            color = YoTextMuted,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        if (selectedLeague != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Clear the \"$selectedLeague\" filter",
                                color = YoPrimaryViolet,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable { selectedLeague = null }
                            )
                        }
                    }
                }
                selectedFilter == SportsFilter.ALL -> {
                    // Live first, sectioned, exactly as requested.
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (liveMatches.isNotEmpty()) {
                            item(key = "header-live") {
                                SportsSectionHeader(title = "Live Now", count = liveMatches.size)
                            }
                            items(liveMatches, key = { "live-${it.id}" }) { match ->
                                MatchCard(match = match, onClick = { onMatchClick(match.id.toString()) })
                            }
                        }
                        if (upcomingMatches.isNotEmpty()) {
                            item(key = "header-upcoming") {
                                Spacer(modifier = Modifier.height(if (liveMatches.isNotEmpty()) 8.dp else 0.dp))
                                SportsSectionHeader(
                                    title = "Upcoming",
                                    count = upcomingMatches.size,
                                    accentColor = YoTextMuted
                                )
                            }
                            items(upcomingMatches, key = { "upcoming-${it.id}" }) { match ->
                                MatchCard(match = match, onClick = { onMatchClick(match.id.toString()) })
                            }
                        }
                        item { Spacer(modifier = Modifier.height(12.dp)) }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(matches, key = { it.id }) { match ->
                            MatchCard(match = match, onClick = { onMatchClick(match.id.toString()) })
                        }
                        item { Spacer(modifier = Modifier.height(12.dp)) }
                    }
                }
            }
        }
    }
}
