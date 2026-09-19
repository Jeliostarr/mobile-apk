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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Match
import com.example.data.model.MatchStatus
import com.example.data.model.SportsLeagueDto
import com.example.repository.SportsRepository
import com.example.ui.theme.YoBaseBackground
import com.example.ui.theme.YoBorder
import com.example.ui.theme.YoLiveRed
import com.example.ui.theme.YoPrimaryViolet
import com.example.ui.theme.YoSurface
import com.example.ui.theme.YoSurfaceVariant
import com.example.ui.theme.YoTextMuted
import com.example.ui.theme.YoTextPrimary
import com.example.ui.util.SportsTimeUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private enum class SportsTab(val label: String) {
    LIVE("Live"),
    SCHEDULE("Schedule"),
}

@Composable
fun SportsScreen(
    sportsRepository: SportsRepository,
    onMatchClick: (matchId: String) -> Unit,
) {
    var selectedTab by remember { mutableStateOf(SportsTab.LIVE) }
    var selectedLeagueId by remember { mutableStateOf<String?>(null) }
    var leagues by remember { mutableStateOf<List<SportsLeagueDto>>(emptyList()) }

    var liveMatches by remember { mutableStateOf<List<Match>>(emptyList()) }
    var scheduleMatches by remember { mutableStateOf<List<Match>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshTick by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        leagues = sportsRepository.getLeagues()
    }

    LaunchedEffect(selectedLeagueId, refreshTick) {
        isLoading = true
        coroutineScope {
            // getAllLive walks every page until hasMore is false, so the
            // Live tab shows the full live list rather than the first 20.
            val liveDeferred = async { sportsRepository.getAllLive(leagueId = selectedLeagueId) }
            val schedDeferred = async {
                sportsRepository.getSchedule(limit = 40, leagueId = selectedLeagueId)
            }
            liveMatches = liveDeferred.await()
            scheduleMatches = schedDeferred.await()
        }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(YoBaseBackground),
    ) {
        SportsHeader(
            liveCount = liveMatches.size,
            onRefresh = { refreshTick++ },
        )

        SportsTabs(selected = selectedTab, onSelect = { selectedTab = it })

        if (leagues.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            LeagueChipsRow(
                leagues = leagues,
                selectedLeagueId = selectedLeagueId,
                onSelect = { selectedLeagueId = it },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = YoPrimaryViolet)
            }
        } else {
            when (selectedTab) {
                SportsTab.LIVE -> LiveList(
                    matches = liveMatches,
                    onMatchClick = onMatchClick,
                )
                SportsTab.SCHEDULE -> ScheduleList(
                    matches = scheduleMatches,
                    onMatchClick = onMatchClick,
                )
            }
        }
    }
}

@Composable
private fun SportsHeader(liveCount: Int, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(YoPrimaryViolet.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.SportsSoccer,
                contentDescription = null,
                tint = YoPrimaryViolet,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Sports", fontSize = 22.sp, fontWeight = FontWeight.Black, color = YoTextPrimary)
            Text(
                text = if (liveCount > 0) "$liveCount live now" else "Fixtures & results",
                fontSize = 12.sp,
                color = if (liveCount > 0) YoLiveRed else YoTextMuted,
                fontWeight = if (liveCount > 0) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
        IconButton(onClick = onRefresh) {
            Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = YoTextMuted)
        }
    }
}

@Composable
private fun SportsTabs(selected: SportsTab, onSelect: (SportsTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(YoSurface)
            .border(1.dp, YoBorder, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SportsTab.values().forEach { tab ->
            val isSelected = tab == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) YoPrimaryViolet else Color.Transparent)
                    .clickable { onSelect(tab) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tab.label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) YoBaseBackground else YoTextMuted,
                )
            }
        }
    }
}

@Composable
private fun LeagueChipsRow(
    leagues: List<SportsLeagueDto>,
    selectedLeagueId: String?,
    onSelect: (String?) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            LeagueChip(
                label = "All leagues",
                isSelected = selectedLeagueId == null,
                onClick = { onSelect(null) },
            )
        }
        items(leagues, key = { it.id }) { league ->
            LeagueChip(
                label = league.name,
                isSelected = selectedLeagueId == league.id,
                onClick = { onSelect(if (selectedLeagueId == league.id) null else league.id) },
            )
        }
    }
}

@Composable
private fun LeagueChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (isSelected) YoPrimaryViolet else YoSurfaceVariant)
            .border(
                width = 1.dp,
                color = if (isSelected) Color.Transparent else YoBorder,
                shape = RoundedCornerShape(50),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) YoBaseBackground else YoTextPrimary,
        )
    }
}

@Composable
private fun LiveList(matches: List<Match>, onMatchClick: (String) -> Unit) {
    if (matches.isEmpty()) {
        EmptyState(
            title = "No live matches right now",
            subtitle = "Check the Schedule tab for upcoming fixtures",
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(matches, key = { it.id }) { match ->
            LiveMatchCard(match = match, onClick = { onMatchClick(match.id) })
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun ScheduleList(matches: List<Match>, onMatchClick: (String) -> Unit) {
    if (matches.isEmpty()) {
        EmptyState(
            title = "No fixtures scheduled",
            subtitle = "Try a different league",
        )
        return
    }
    val grouped = matches.groupBy { m ->
        val ms = m.startTime?.toEpochMilli()
        if (ms == null) "Unknown" else SportsTimeUtils.formatDay(m.startTime.toString())
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        grouped.forEach { (dayLabel, dayMatches) ->
            item(key = "header-$dayLabel") {
                Text(
                    text = dayLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = YoTextMuted,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
            }
            items(dayMatches, key = { it.id }) { match ->
                ScheduleMatchCard(match = match, onClick = { onMatchClick(match.id) })
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun LiveMatchCard(match: Match, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(YoSurface)
            .border(1.dp, YoLiveRed.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = match.league.ifBlank { "Football" },
                fontSize = 12.sp,
                color = YoTextMuted,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            LivePulseBadge()
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TeamBlock(match.home, Modifier.weight(1f))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(80.dp),
            ) {
                Text(
                    text = "${match.home.score ?: 0} - ${match.away.score ?: 0}",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = YoTextPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "LIVE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = YoLiveRed,
                    letterSpacing = 1.sp,
                )
            }
            TeamBlock(match.away, Modifier.weight(1f))
        }
        if (match.canWatchLive) {
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(YoPrimaryViolet)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Watch Live",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = YoBaseBackground,
                )
            }
        }
    }
}

@Composable
private fun ScheduleMatchCard(match: Match, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(YoSurface)
            .border(1.dp, YoBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.width(60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = SportsTimeUtils.formatTime(match.startTime?.toString()),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = YoTextPrimary,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = match.home.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = YoTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = match.away.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = YoTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (match.status == MatchStatus.FINISHED && match.home.score != null) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${match.home.score}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = YoTextMuted,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${match.away.score}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = YoTextMuted,
                )
            }
        } else {
            Text(
                text = "FT".takeIf { match.status == MatchStatus.FINISHED } ?: "VS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = YoTextMuted,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun TeamBlock(team: com.example.data.model.Team, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(YoSurfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (!team.logo.isNullOrBlank()) {
                AsyncImage(
                    model = team.logo,
                    contentDescription = team.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(38.dp),
                )
            } else {
                Text(
                    text = team.abbreviation.ifBlank { team.name.take(3).uppercase() },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = YoTextMuted,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = team.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = YoTextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LivePulseBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(YoLiveRed.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(YoLiveRed),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = "LIVE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = YoLiveRed,
                letterSpacing = 0.5.sp,
            )
        }
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.SportsSoccer,
            contentDescription = null,
            tint = YoTextMuted,
            modifier = Modifier.size(44.dp),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = YoTextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = YoTextMuted,
            textAlign = TextAlign.Center,
        )
    }
}