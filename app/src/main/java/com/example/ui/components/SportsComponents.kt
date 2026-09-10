package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.SportsLeague
import com.example.data.model.SportsMatch
import com.example.ui.theme.YoBorder
import com.example.ui.theme.YoLiveRed
import com.example.ui.theme.YoPrimaryViolet
import com.example.ui.theme.YoSurface
import com.example.ui.theme.YoTextMuted
import com.example.ui.theme.YoTextPrimary
import com.example.ui.util.SportsTimeUtils

/** Small pulsing "LIVE" pill — used on match cards and inside the player. */
@Composable
fun LiveBadge(compact: Boolean = false) {
    val transition = rememberInfiniteTransition(label = "live-pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse),
        label = "dot-alpha"
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(YoLiveRed)
            .padding(horizontal = if (compact) 8.dp else 10.dp, vertical = if (compact) 3.dp else 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = alpha))
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text("LIVE", color = Color.White, fontSize = if (compact) 10.sp else 11.sp, fontWeight = FontWeight.Bold)
    }
}

/** Horizontal-scroll filter chip for a league (or the "All" pseudo-league). */
@Composable
fun LeagueChip(
    name: String,
    imageUrl: String?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .let { if (isSelected) it.shadow(6.dp, RoundedCornerShape(20.dp), clip = false) else it }
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) YoPrimaryViolet else YoSurface)
            .border(1.dp, if (isSelected) Color.Transparent else YoBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = name,
                modifier = Modifier.size(18.dp).clip(CircleShape),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) Color.Black else YoTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Match summary card for list screens. Tapping always navigates to detail —
 * whether a stream exists yet is just shown as a hint, decided on the detail screen. */
@Composable
fun MatchCard(
    match: SportsMatch,
    onClick: () -> Unit
) {
    val accentColor = if (match.live) YoLiveRed else YoPrimaryViolet

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(14.dp), clip = false)
            .clip(RoundedCornerShape(14.dp))
            .background(YoSurface)
            .border(1.dp, YoBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        // Left accent stripe — the one cue that makes a live match register
        // at a glance while scanning a long list, instead of every card
        // looking identical until you read the badge text.
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(4.dp)
                .background(accentColor)
        )

        Column(modifier = Modifier.padding(14.dp)) {
        // League row
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (match.league?.img != null) {
                AsyncImage(
                    model = match.league.img,
                    contentDescription = match.league.name,
                    modifier = Modifier.size(16.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = match.league?.name ?: "Football",
                color = YoTextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (match.live) {
                LiveBadge(compact = true)
            } else {
                Text(
                    text = SportsTimeUtils.formatTime(match.kickoff),
                    color = YoTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Teams row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TeamColumn(name = match.home?.name, imageUrl = match.home?.img, modifier = Modifier.weight(1f))

            Text(
                text = "VS",
                color = YoTextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp)
            )

            TeamColumn(
                name = match.away?.name,
                imageUrl = match.away?.img,
                modifier = Modifier.weight(1f),
                alignEnd = true
            )
        }

        val countdown = if (!match.live) SportsTimeUtils.formatCountdown(match.kickoff) else ""
        if (match.hasStream || countdown.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = countdown,
                    color = YoTextMuted,
                    fontSize = 11.sp
                )
                if (match.hasStream) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(YoPrimaryViolet.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayCircle,
                            contentDescription = "Stream available",
                            tint = YoPrimaryViolet,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Watch", color = YoPrimaryViolet, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun TeamColumn(
    name: String?,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (alignEnd) {
                Text(
                    text = name ?: "TBD",
                    color = YoTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                TeamLogo(imageUrl)
            } else {
                TeamLogo(imageUrl)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = name ?: "TBD",
                    color = YoTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TeamLogo(imageUrl: String?) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(YoBorder.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.size(28.dp).clip(CircleShape),
                contentScale = ContentScale.Fit
            )
        }
    }
}

// ─── Loading skeletons ───
// Mirrors ShimmerSkeleton.kt's MovieCardSkeleton pattern rather than a
// spinner — lists shimmer, single-item/player screens keep a spinner
// (same split the rest of the app already uses).

@Composable
fun MatchCardSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(14.dp), clip = false)
            .clip(RoundedCornerShape(14.dp))
            .background(YoSurface)
            .border(1.dp, YoBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShimmerSkeleton(modifier = Modifier.width(90.dp).height(12.dp), shapeRadius = 4.dp)
            Spacer(modifier = Modifier.weight(1f))
            ShimmerSkeleton(modifier = Modifier.width(50.dp).height(12.dp), shapeRadius = 4.dp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ShimmerSkeleton(modifier = Modifier.size(28.dp), shapeRadius = 14.dp)
            Spacer(modifier = Modifier.width(8.dp))
            ShimmerSkeleton(modifier = Modifier.width(80.dp).height(14.dp), shapeRadius = 4.dp)
            Spacer(modifier = Modifier.weight(1f))
            ShimmerSkeleton(modifier = Modifier.width(80.dp).height(14.dp), shapeRadius = 4.dp)
            Spacer(modifier = Modifier.width(8.dp))
            ShimmerSkeleton(modifier = Modifier.size(28.dp), shapeRadius = 14.dp)
        }
    }
}

@Composable
fun SportsListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 6
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false
    ) {
        items(itemCount) {
            MatchCardSkeleton()
        }
    }
}

/** Section header for grouped Live/Upcoming lists — same accent-bar + bold
 * title language as HomeScreen's RailHeader, plus an item-count pill since
 * there's no "view all" action to put in that spot here. */
@Composable
fun SportsSectionHeader(
    title: String,
    count: Int,
    accentColor: Color = YoPrimaryViolet
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = YoTextPrimary,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(YoSurface)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(text = count.toString(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = YoTextMuted)
        }
    }
}
