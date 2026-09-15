package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.player.PlayerCaption
import com.example.player.PlayerQuality
import com.example.repository.YocinemaRepository

@Composable
fun MoviesDemoPlayerScreen(
    detailPath: String,
    title: String,
    seasonNum: Int?,
    epNum: Int?,
    repository: YocinemaRepository,
    onBackClick: () -> Unit,
    onEpisodeSelected: ((season: Int, episode: Int) -> Unit)? = null,
) {
    var qualities by remember { mutableStateOf<List<PlayerQuality>>(emptyList()) }
    var captions by remember { mutableStateOf<List<PlayerCaption>>(emptyList()) }
    var primaryUrl by remember { mutableStateOf<String?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var seasonsState by remember {
        mutableStateOf<com.example.data.model.MdSeasonsResponse?>(null)
    }

    LaunchedEffect(detailPath, seasonNum, epNum) {
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
                loadError = "No playable source"
                return@LaunchedEffect
            }

            // The backend returns worker URLs (cdn.yocinema.dpdns.org/m/<token>)
            // in the `url` field. Pass them straight through — no client-side
            // wrapping needed.
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
    }

    val episodes = remember(seasonsState, seasonNum) {
        val info = seasonsState ?: return@remember emptyList()
        if (seasonNum == null) return@remember emptyList()
        val seasonData = info.seasons?.firstOrNull { it.season == seasonNum }
            ?: return@remember emptyList()
        val maxEp = seasonData.maxEp ?: 0
        (1..maxEp).map { ep ->
            PlayerEpisode(
                season = seasonNum,
                episode = ep,
                title = "Episode $ep",
                stillUrl = null,
                streamUrl = "",
            )
        }
    }

    val url = primaryUrl
    if (loadError != null || url.isNullOrBlank()) {
        // Surface the error through the unified player's own error state —
        // passing a blank stream means the player shows "No playable source"
        // and the back button still works.
        UnifiedPlayerScreen(
            spec = UnifiedPlayerSpec(
                mediaId = detailPath,
                title = title,
                subtitle = null,
                qualities = emptyList(),
                captions = emptyList(),
                episodes = emptyList(),
            ),
            streamUrl = "",
            repository = repository,
            onBackClick = onBackClick,
        )
        return
    }

    val spec = remember(qualities, captions, episodes, seasonNum, epNum) {
        UnifiedPlayerSpec(
            mediaId = detailPath,
            title = title,
            subtitle = if (seasonNum != null && epNum != null) "S$seasonNum · E$epNum" else null,
            posterUrl = null,
            qualities = qualities,
            captions = captions,
            episodes = episodes,
            currentSeason = seasonNum,
            currentEpisode = epNum,
            onEpisodeSelected = { playerEp ->
                if (playerEp.season != null && playerEp.episode != null) {
                    onEpisodeSelected?.invoke(playerEp.season, playerEp.episode)
                }
            },
        )
    }

    UnifiedPlayerScreen(
        spec = spec,
        streamUrl = url,
        repository = repository,
        onBackClick = onBackClick,
    )
}