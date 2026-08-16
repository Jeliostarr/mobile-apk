package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.SportsMatchDetailResponse
import com.example.data.model.SportsStream
import com.example.repository.SportsRepository
import com.example.ui.components.LiveBadge
import com.example.ui.theme.YoBaseBackground
import com.example.ui.theme.YoBorder
import com.example.ui.theme.YoPrimaryAmber
import com.example.ui.theme.YoSurface
import com.example.ui.theme.YoTextMuted
import com.example.ui.theme.YoTextPrimary
import com.example.ui.util.SportsTimeUtils

@Composable
fun SportsMatchDetailScreen(
    matchId: String,
    sportsRepository: SportsRepository,
    onBackClick: () -> Unit,
    onWatch: (matchId: String, streamId: Int) -> Unit
) {
    var detail by remember { mutableStateOf<SportsMatchDetailResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var selectedStream by remember { mutableStateOf<SportsStream?>(null) }
    var refreshTick by remember { mutableStateOf(0) }

    LaunchedEffect(matchId, refreshTick) {
        isLoading = true
        loadError = null
        val result = sportsRepository.getMatchDetail(matchId)
        if (result?.match == null) {
            loadError = "Match not found — it may have been removed."
        } else {
            detail = result
            // Default to the highest quality available (HD > MD > SD is the
            // order Nova returns them in, so just prefer the last entry).
            selectedStream = result.streams.lastOrNull()
        }
        isLoading = false
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = YoTextPrimary)
            }
            Text(
                text = "Match Details",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = YoTextPrimary
            )
        }

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = YoPrimaryAmber)
                }
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
                        color = YoPrimaryAmber,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { refreshTick++ }
                    )
                }
            }
            else -> {
                val match = detail?.match
                val streams = detail?.streams ?: emptyList()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                ) {
                    // League + status
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (match?.league?.img != null) {
                            AsyncImage(
                                model = match.league.img,
                                contentDescription = match.league.name,
                                modifier = Modifier.size(18.dp),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = match?.league?.name ?: "Football",
                            color = YoTextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        if (match?.live == true) {
                            LiveBadge()
                        } else {
                            Text(
                                text = "${SportsTimeUtils.formatDay(match?.kickoff)} · ${SportsTimeUtils.formatTime(match?.kickoff)}",
                                color = YoTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Matchup
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BigTeam(name = match?.home?.name, imageUrl = match?.home?.img)
                        Text("VS", color = YoTextMuted, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        BigTeam(name = match?.away?.name, imageUrl = match?.away?.img)
                    }

                    Spacer(modifier = Modifier.height(36.dp))

                    if (streams.isNotEmpty()) {
                        Text(
                            text = "Choose quality",
                            color = YoTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            streams.forEach { stream ->
                                val isSelected = stream.id == selectedStream?.id
                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) YoPrimaryAmber else YoSurface)
                                        .clickable { selectedStream = stream }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = stream.label,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.Black else YoTextPrimary
                                    )
                                    if (stream.quality != null) {
                                        Text(
                                            text = stream.quality,
                                            fontSize = 11.sp,
                                            color = if (isSelected) Color.Black.copy(alpha = 0.7f) else YoTextMuted
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                selectedStream?.let { s -> onWatch(matchId, s.id) }
                            },
                            enabled = selectedStream != null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = YoPrimaryAmber,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (match?.live == true) "Watch Live" else "Watch",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(YoSurface)
                                .padding(20.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = if (match?.live == true)
                                        "No stream found for this match right now."
                                    else
                                        "Stream isn't available yet — check back closer to kickoff.",
                                    color = YoTextMuted,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    "Refresh",
                                    color = YoPrimaryAmber,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable { refreshTick++ }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BigTeam(name: String?, imageUrl: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(YoBorder.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = name,
                    modifier = Modifier.size(72.dp).clip(CircleShape),
                    contentScale = ContentScale.Fit
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = name ?: "TBD",
            color = YoTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(100.dp)
        )
    }
}
