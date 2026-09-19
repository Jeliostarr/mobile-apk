package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.data.model.Match
import com.example.repository.SportsRepository
import com.example.repository.YocinemaRepository

/**
 * Sports wrapper around UnifiedPlayerScreen. Sports content never has
 * quality variants or captions — the API gives one worker URL. So the
 * spec has empty lists and the unified player naturally hides those menus.
 *
 * isSportsLive = true tells PlayerManager to skip history/bookmark saving
 * (live has no meaningful resume position) and to handle reconnects by
 * re-playing the same URL rather than looking up a Movie detail.
 */
@Composable
fun SportsPlayerScreen(
    matchId: String,
    streamUrl: String,
    title: String,
    sportsRepository: SportsRepository,
    repository: YocinemaRepository,
    onBackClick: () -> Unit,
) {
    var match by remember { mutableStateOf<Match?>(null) }

    LaunchedEffect(matchId) {
        match = sportsRepository.getMatch(matchId)
    }

    val spec = remember(match, streamUrl) {
        UnifiedPlayerSpec(
            mediaId = matchId,
            title = match?.title ?: title,
            subtitle = match?.league?.takeIf { it.isNotBlank() },
            posterUrl = null,
            qualities = emptyList(),
            captions = emptyList(),
            episodes = emptyList(),
            isSportsLive = true,
        )
    }

    UnifiedPlayerScreen(
        spec = spec,
        streamUrl = streamUrl,
        repository = repository,
        onBackClick = onBackClick,
    )
}