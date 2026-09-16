package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.example.data.model.MdSeasonsResponse
import com.example.player.PlayerCaption
import com.example.player.PlayerQuality
import com.example.repository.YocinemaRepository
import com.example.ui.theme.YoBaseBackground
import com.example.ui.theme.YoTextMuted

@Composable
fun MoviesDemoPlayerScreen(
    detailPath: String,
    title: String,
    seasonNum: Int?,
    epNum: Int?,
    repository: YocinemaRepository,
    onBackClick: () -> Unit,
) {
    var qualities by remember { mutableStateOf<List<PlayerQuality>>(emptyList()) }
    var captions by remember { mutableStateOf<List<PlayerCaption>>(emptyList()) }
    var primaryUrl by remember { mutableStateOf<String?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var seasonsState by remember { mutableStateOf<MdSeasonsResponse?>(null) }
    var ready by remember { mutableStateOf(false) }

    LaunchedEffect(detailPath, seasonNum, epNum) {
        ready = false
        loadError = null

        try {
            val streamRes = if (seasonNum != null && epNum != null) {
                repository.moviesDemoApi.tvStream(detailPath, seasonNum, epNum)
            } else {
                repository.moviesDemoApi.movieStream(detailPath)
            }
            val stream = if (streamRes.isSuccessful) streamRes.body() else null
            val rawQualities = stream?.freeQualities ?: emptyList()
            val defaultQ = stream?.defaultQuality

            if (rawQualities.isEmpty() && defaultQ == null) {
                loadError = "No playable source for this title"
                ready = true
                return@LaunchedEffect
            }

            // Backend returns complete URLs (either /api/stream?url=... or
            // cdn.yocinema.dpdns.org/m/...). Pass through as-is; PlayerManager
            // adds X-API-Key automatically when the URL is on our API host.
            qualities = (rawQualities.ifEmpty { listOfNotNull(defaultQ) }).mapNotNull { q ->
                val u = q.url ?: return@mapNotNull null
                PlayerQuality(
                    label = "${q.resolution ?: "?"}P",
                    url = u,
                    resolution = q.resolution,
                )
            }.sortedByDescending { it.resolution ?: 0 }

            val capRes = repository.moviesDemoApi.captions(detailPath, seasonNum, epNum)
            val rawCaps = if (capRes.isSuccessful) capRes.body()?.captions else null
            captions = rawCaps.orEmpty().mapNotNull { c ->
                val u = c.url ?: return@mapNotNull null
                PlayerCaption(
                    language = c.lan ?: c.lanName,
                    displayName = c.lanName ?: c.lan,
                    url = u,
                )
            }

            if (seasonNum != null) {
                val seasonsRes = repository.moviesDemoApi.seasons(detailPath)
                seasonsState = if (seasonsRes.isSuccessful) seasonsRes.body() else null
            }

            primaryUrl = qualities.firstOrNull()?.url ?: defaultQ?.url
        } catch (e: Exception) {
            loadError = e.message ?: "Couldn't start playback"
        }
        ready = true
    }

    val url = primaryUrl

    if (ready && (loadError != null || url.isNullOrBlank())) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(YoBaseBackground),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = loadError ?: "No playable source for this title",
                color = YoTextMuted,
                fontSize = 14.sp,
            )
        }
        return
    }

    if (!ready || url.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    // Build the spec for the unified player. Episodes list is empty because
    // this screen doesn't yet support in-player episode switching — the
    // user backs out and taps another episode from the detail screen.
    val spec = remember(qualities, captions, seasonNum, epNum, title, detailPath) {
        UnifiedPlayerSpec(
            mediaId = detailPath,
            title = title,
            subtitle = if (seasonNum != null && epNum != null) "S$seasonNum · E$epNum" else null,
            posterUrl = null,
            qualities = qualities,
            captions = captions,
            episodes = emptyList(),
            currentSeason = seasonNum,
            currentEpisode = epNum,
            onEpisodeSelected = null,
        )
    }

    UnifiedPlayerScreen(
        spec = spec,
        streamUrl = url,
        repository = repository,
        onBackClick = onBackClick,
    )
}