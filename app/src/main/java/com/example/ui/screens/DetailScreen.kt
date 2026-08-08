package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.data.model.CastMember
import com.example.data.model.Episode
import com.example.data.model.Movie
import com.example.data.model.Season
import com.example.data.model.formatDuration
import com.example.download.DownloadWorker
import com.example.repository.YocinemaRepository
import com.example.ui.components.CastAvatarCard
import com.example.ui.components.EpisodeCard
import com.example.ui.components.GateModalBottomSheet
import com.example.ui.components.ModernLoader
import com.example.ui.components.PosterCard
import com.example.ui.components.ReportDialog
import com.example.ui.components.VJBadgeChip
import com.example.ui.theme.YoBaseBackground
import com.example.ui.theme.YoBorder
import com.example.ui.theme.YoPrimaryAmber
import com.example.ui.theme.YoSurface
import com.example.ui.theme.YoSurfaceVariant
import com.example.ui.theme.YoTextMuted
import com.example.ui.theme.YoTextPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    movieId: String,
    repository: YocinemaRepository,
    onBackClick: () -> Unit,
    onPlayClick: (movieId: String, seasonNum: Int?, epNum: Int?) -> Unit,
    onCastClick: (String) -> Unit,
    onRelatedMovieClick: (String) -> Unit,
    onEnterApiKeyRequested: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var movie by remember { mutableStateOf<Movie?>(null) }
    var episodesList by remember { mutableStateOf<List<Episode>>(emptyList()) }
    var relatedMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showGateSheet by remember { mutableStateOf(false) }
    var isSynopsisExpanded by remember { mutableStateOf(false) }
    var selectedSeasonNumber by remember { mutableStateOf(1) }

    val isWatchlisted by repository.isWatchlisted(movieId).collectAsState(initial = false)
    val gateSheetState = rememberModalBottomSheetState()

    fun checkAuthAndExecute(action: () -> Unit) {
        if (!repository.isLoggedIn()) {
            showGateSheet = true
        } else {
            action()
        }
    }

    LaunchedEffect(movieId) {
        scope.launch {
            isLoading = true
            val m = repository.getMovieDetail(movieId)
            movie = m
            if (m != null) {
                if (m.isSeries) {
                    val rawEps = repository.getMovieEpisodes(movieId)
                    val allEps = if (rawEps.isNotEmpty()) rawEps else m.episodes.orEmpty()
                    episodesList = allEps.sortedWith(compareBy({ it.sNum ?: 1 }, { it.eNum ?: 1 }))
                    val seasons = episodesList.mapNotNull { it.sNum }.distinct().sorted()
                    if (seasons.isNotEmpty()) {
                        selectedSeasonNumber = seasons.first()
                    }
                }
                relatedMovies = repository.getRelatedMovies(movieId)
            }
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(YoBaseBackground)
    ) {
        if (isLoading || movie == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                ModernLoader()
            }
        } else {
            val m = movie!!

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Backdrop with Gradient Scrim
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    ) {
                        SubcomposeAsyncImage(
                            model = m.cover ?: m.poster ?: m.displayPosterUrl,
                            contentDescription = m.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            loading = { com.example.ui.components.YoCinemaLogoPlaceholder() },
                            error = { com.example.ui.components.YoCinemaLogoPlaceholder() }
                        )

                        // Top Scrim
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                                    )
                                )
                        )

                        // Bottom Scrim
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, YoBaseBackground)
                                    )
                                )
                        )

                        // Back Button Top Left
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp)
                                .clip(CircleShape)
                                .background(YoBaseBackground.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = YoTextPrimary
                            )
                        }

                        // Top Right Action Icons: Watchlist, Download, Report
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    checkAuthAndExecute {
                                        scope.launch { repository.toggleWatchlist(m) }
                                    }
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(YoBaseBackground.copy(alpha = 0.6f))
                            ) {
                                Icon(
                                    imageVector = if (isWatchlisted) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "Watchlist",
                                    tint = if (isWatchlisted) YoPrimaryAmber else YoTextPrimary
                                )
                            }

                            IconButton(
                                onClick = {
                                    checkAuthAndExecute {
                                        startDownloadWorker(context, m, null, null)
                                    }
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(YoBaseBackground.copy(alpha = 0.6f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download",
                                    tint = YoTextPrimary
                                )
                            }

                            IconButton(
                                onClick = { showReportDialog = true },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(YoBaseBackground.copy(alpha = 0.6f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BugReport,
                                    contentDescription = "Report Issue",
                                    tint = YoTextPrimary
                                )
                            }
                        }
                    }
                }

                // Title, VJ Badge, Ratings, Metadata
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = m.title,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = YoTextPrimary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (!m.vjName.isNull_orEmpty()) {
                                VJBadgeChip(vjName = m.vjName!!)
                            }

                            if (!m.imdbRating.isNull_orEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(YoSurface)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = YoPrimaryAmber,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = m.imdbRating!!,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = YoTextPrimary
                                    )
                                }
                            }

                            if (m.duration != null && m.duration > 0) {
                                Text(
                                    text = formatDuration(m.duration),
                                    fontSize = 12.sp,
                                    color = YoTextMuted
                                )
                            }

                            if (!m.releaseDate.isNull_orEmpty()) {
                                Text(
                                    text = m.releaseDate!!.take(4),
                                    fontSize = 12.sp,
                                    color = YoTextMuted
                                )
                            }
                        }

                        if (!m.genre.isNull_orEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = m.genre!!,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = YoPrimaryAmber
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Play Now & Download Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    checkAuthAndExecute {
                                        onPlayClick(m.id, null, null)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = YoPrimaryAmber,
                                    contentColor = YoBaseBackground
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Watch Now", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    checkAuthAndExecute {
                                        startDownloadWorker(context, m, null, null)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, YoBorder),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = YoTextPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Synopsis
                        if (!m.description.isNull_orEmpty()) {
                            Column(modifier = Modifier.animateContentSize()) {
                                Text(
                                    text = m.description!!,
                                    fontSize = 14.sp,
                                    color = YoTextMuted,
                                    lineHeight = 20.sp,
                                    maxLines = if (isSynopsisExpanded) Int.MAX_VALUE else 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isSynopsisExpanded) "SHOW LESS" else "MORE...",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = YoPrimaryAmber,
                                    modifier = Modifier
                                        .clickable { isSynopsisExpanded = !isSynopsisExpanded }
                                        .padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Episodes Section (for TV Series)
                if (m.isSeries && episodesList.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Episodes",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = YoTextPrimary,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            val availableSeasons = episodesList.mapNotNull { it.sNum }.distinct().sorted()
                            if (availableSeasons.size > 1) {
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(availableSeasons) { seasonNum ->
                                        val isSelected = seasonNum == selectedSeasonNumber
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(if (isSelected) YoPrimaryAmber else YoSurface)
                                                .border(1.dp, if (isSelected) YoPrimaryAmber else YoBorder, RoundedCornerShape(20.dp))
                                                .clickable { selectedSeasonNumber = seasonNum }
                                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "Season $seasonNum",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) YoBaseBackground else YoTextPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val seasonFiltered = episodesList.filter { it.sNum == selectedSeasonNumber }.ifEmpty { episodesList }

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(seasonFiltered) { ep ->
                                    EpisodeCard(
                                        episode = ep,
                                        movieId = m.id,
                                        onClick = {
                                            checkAuthAndExecute {
                                                onPlayClick(m.id, ep.sNum, ep.eNum)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Cast Rail
                val allCast = m.cast.orEmpty()
                if (allCast.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Cast & Crew",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = YoTextPrimary,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(allCast) { c ->
                                    CastAvatarCard(
                                        cast = c,
                                        movieId = m.id,
                                        onClick = {
                                            val castId = c.castId ?: c.id ?: c.name
                                            onCastClick(castId)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Related Movies Rail
                if (relatedMovies.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "More Like This",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = YoTextPrimary,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(relatedMovies) { rel ->
                                    PosterCard(
                                        movie = rel,
                                        onClick = { onRelatedMovieClick(rel.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Report Dialog
        if (showReportDialog) {
            ReportDialog(
                onDismiss = { showReportDialog = false },
                onSubmitReport = { reason ->
                    showReportDialog = false
                    scope.launch {
                        repository.reportMovie(movieId, reason)
                    }
                }
            )
        }

        // Gate Modal Sheet
        if (showGateSheet) {
            GateModalBottomSheet(
                sheetState = gateSheetState,
                onDismiss = { showGateSheet = false },
                onEnterKeyClicked = onEnterApiKeyRequested
            )
        }
    }
}

fun startDownloadWorker(
    context: android.content.Context,
    movie: Movie,
    seasonNum: Int?,
    epNum: Int?
) {
    val downloadId = "${movie.id}_${if (seasonNum != null && epNum != null) "S${seasonNum}E${epNum}" else "movie"}"
    val data = workDataOf(
        DownloadWorker.KEY_DOWNLOAD_ID to downloadId,
        DownloadWorker.KEY_MOVIE_ID to movie.id,
        DownloadWorker.KEY_TITLE to movie.title,
        DownloadWorker.KEY_SEASON_NUM to (seasonNum ?: -1),
        DownloadWorker.KEY_EP_NUM to (epNum ?: -1)
    )

    val request = OneTimeWorkRequestBuilder<DownloadWorker>()
        .setInputData(data)
        .build()

    WorkManager.getInstance(context).enqueue(request)
}

private fun String?.isNull_orEmpty(): Boolean = this == null || this.trim().isEmpty()
