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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Brush
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
import com.example.data.model.MediaClip
import com.example.data.model.Team
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

@Composable
fun SportsMatchDetailScreen(
    matchId: String,
    sportsRepository: SportsRepository,
    onBackClick: () -> Unit,
    onWatch: (matchId: String, mediaUrl: String) -> Unit,
) {
    var match by remember { mutableStateOf<Match?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshTick by remember { mutableStateOf(0) }

    LaunchedEffect(matchId, refreshTick) {
        isLoading = true
        match = sportsRepository.getMatch(matchId)
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(YoBaseBackground),
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
                text = "Match",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = YoTextPrimary,
            )
        }

        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = YoPrimaryViolet)
            }
            match == null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Match not found", color = YoTextMuted, fontSize = 15.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Retry",
                    color = YoPrimaryViolet,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { refreshTick++ },
                )
            }
            else -> MatchDetailContent(
                match = match!!,
                onWatch = onWatch,
            )
        }
    }
}

@Composable
private fun MatchDetailContent(
    match: Match,
    onWatch: (String, String) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { ScoreboardHero(match) }

        if (match.canWatchLive && !match.liveStreamUrl.isNullOrBlank()) {
            item {
                WatchLiveButton(
                    onClick = { onWatch(match.id, match.liveStreamUrl) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                )
            }
        }

        if (match.channels.isNotEmpty()) {
            item {
                MediaSection(
                    title = "Channels",
                    clips = match.channels,
                    onClipClick = { onWatch(match.id, it.url) },
                )
            }
        }

        if (match.highlights.isNotEmpty()) {
            item {
                MediaSection(
                    title = "Highlights",
                    clips = match.highlights,
                    onClipClick = { onWatch(match.id, it.url) },
                )
            }
        }

        if (match.replay.isNotEmpty()) {
            item {
                MediaSection(
                    title = "Full Replay",
                    clips = match.replay,
                    onClipClick = { onWatch(match.id, it.url) },
                )
            }
        }
    }
}

@Composable
private fun ScoreboardHero(match: Match) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(YoPrimaryViolet.copy(alpha = 0.20f), YoSurface),
                ),
            )
            .padding(20.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = match.league.ifBlank { "Football" },
                    fontSize = 12.sp,
                    color = YoTextMuted,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (match.round.isNotBlank()) {
                    Text(
                        text = " • ${match.round}",
                        fontSize = 12.sp,
                        color = YoTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            MatchStatusLine(match)
            Spacer(Modifier.height(22.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BigTeam(match.home, Modifier.weight(1f))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(90.dp),
                ) {
                    val score = if (match.home.score != null && match.away.score != null) {
                        "${match.home.score} - ${match.away.score}"
                    } else {
                        "vs"
                    }
                    Text(
                        text = score,
                        fontSize = if (match.home.score != null) 34.sp else 22.sp,
                        fontWeight = FontWeight.Black,
                        color = YoTextPrimary,
                    )
                }
                BigTeam(match.away, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MatchStatusLine(match: Match) {
    val (label, color) = when (match.status) {
        MatchStatus.LIVE -> "LIVE" to YoLiveRed
        MatchStatus.FINISHED -> "Full time" to YoTextMuted
        MatchStatus.SCHEDULED -> "${SportsTimeUtils.formatDay(match.startTime?.toString())} · ${SportsTimeUtils.formatTime(match.startTime?.toString())}" to YoTextMuted
        MatchStatus.POSTPONED -> "Postponed" to YoTextMuted
        MatchStatus.CANCELLED -> "Cancelled" to YoTextMuted
        MatchStatus.UNKNOWN -> "" to YoTextMuted
    }
    if (label.isNotBlank()) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            letterSpacing = if (match.status == MatchStatus.LIVE) 1.sp else 0.sp,
        )
    }
}

@Composable
private fun BigTeam(team: Team, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(YoSurfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (!team.logo.isNullOrBlank()) {
                AsyncImage(
                    model = team.logo,
                    contentDescription = team.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(56.dp),
                )
            } else {
                Text(
                    text = team.abbreviation.ifBlank { team.name.take(3).uppercase() },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = YoTextMuted,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = team.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = YoTextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun WatchLiveButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = YoPrimaryViolet,
            contentColor = YoBaseBackground,
        ),
    ) {
        Icon(Icons.Filled.PlayArrow, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Watch Live", fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
private fun MediaSection(
    title: String,
    clips: List<MediaClip>,
    onClipClick: (MediaClip) -> Unit,
) {
    Column {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = YoTextPrimary,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(clips, key = { it.id }) { clip ->
                MediaCard(clip = clip, onClick = { onClipClick(clip) })
            }
        }
    }
}

@Composable
private fun MediaCard(clip: MediaClip, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(200.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(YoSurface)
            .border(1.dp, YoBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .background(YoSurfaceVariant),
        ) {
            if (!clip.cover.isNullOrBlank()) {
                AsyncImage(
                    model = clip.cover,
                    contentDescription = clip.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            if (clip.durationSeconds != null && clip.durationSeconds > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = formatDuration(clip.durationSeconds),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        }
        if (!clip.title.isNullOrBlank()) {
            Text(
                text = clip.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = YoTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(10.dp),
            )
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}