package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Match
import com.example.data.model.MatchStatus
import com.example.data.model.Team
import com.example.ui.theme.YoBorder
import com.example.ui.theme.YoLiveRed
import com.example.ui.theme.YoPrimaryViolet
import com.example.ui.theme.YoSurface
import com.example.ui.theme.YoSurfaceVariant
import com.example.ui.theme.YoTextMuted
import com.example.ui.theme.YoTextPrimary
import com.example.ui.util.SportsTimeUtils

@Composable
fun HomeSportsRail(
    title: String,
    matches: List<Match>,
    onMatchClick: (String) -> Unit,
    onViewAllClick: () -> Unit,
) {
    if (matches.isEmpty()) return
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(18.dp)
                        .clip(CircleShape)
                        .background(YoPrimaryViolet),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = YoTextPrimary,
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
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(matches, key = { it.id }) { match ->
                HomeMatchCard(match = match, onClick = { onMatchClick(match.id) })
            }
        }
    }
}

@Composable
private fun HomeMatchCard(match: Match, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(170.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(YoSurface)
            .border(1.dp, YoBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Text(
            text = match.league.ifBlank { "Football" },
            fontSize = 10.sp,
            color = YoTextMuted,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MiniTeam(match.home, Modifier.weight(1f))
            Text(
                text = if (match.status == MatchStatus.LIVE && match.home.score != null) {
                    "${match.home.score}-${match.away.score}"
                } else {
                    "vs"
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = YoTextMuted,
            )
            MiniTeam(match.away, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (match.status == MatchStatus.LIVE) YoLiveRed.copy(alpha = 0.14f)
                    else YoPrimaryViolet.copy(alpha = 0.14f),
                )
                .padding(vertical = 5.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = when (match.status) {
                    MatchStatus.LIVE -> "LIVE"
                    MatchStatus.FINISHED -> "FT"
                    else -> SportsTimeUtils.formatTime(match.startTime?.toString())
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (match.status == MatchStatus.LIVE) YoLiveRed else YoPrimaryViolet,
            )
        }
    }
}

@Composable
private fun MiniTeam(team: Team, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(YoSurfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (!team.logo.isNullOrBlank()) {
                AsyncImage(
                    model = team.logo,
                    contentDescription = team.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(22.dp),
                )
            } else {
                Text(
                    text = team.abbreviation.ifBlank { team.name.take(3).uppercase() },
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = YoTextMuted,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = team.name,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = YoTextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
