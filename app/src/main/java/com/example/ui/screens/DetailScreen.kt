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
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.data.model.CastMember
import com.example.data.model.Episode
import com.example.data.model.Movie
import com.example.data.model.Season
import com.example.data.model.formatDuration
import android.widget.Toast
import androidx.compose.material3.CircularProgressIndicator
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.example.data.local.DownloadEntity
import com.example.download.startDownloadWorker
import com.example.repository.YocinemaRepository
import com.example.ui.components.CastAvatarCard
import com.example.ui.components.EpisodeCard
import com.example.ui.components.EpisodeDownloadSheet
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
    var errorState by remember { mutableStateOf<String?>(null) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showGateSheet by remember { mutableStateOf(false) }
    var trailerExpanded by remember { mutableStateOf(false) }
    var showDownloadStartedDialog by remember { mutableStateOf(false) }
    var isSynopsisExpanded by remember { mutableStateOf(false) }
    var selectedSeasonNumber by remember { mutableStateOf(1) }
    var showEpisodeDownloadSheet by remember { mutableStateOf(false) }

    val isWatchlisted by repository.isWatchlisted(movieId).collectAsState(initial = false)
    val gateSheetState = rememberModalBottomSheetState()
    val episodeDownloadSheetState = rememberModalBottomSheetState()

    // Existing downloads for THIS movie (active + completed), keyed as "S{season}E{episode}"
    // for series or the bare movie id for a plain movie. Used to: (1) block starting a
    // duplicate download and show a clear message instead, and (2) grey out episodes in
    // the picker that are already downloading/downloaded.
    val activeDownloads by repository.activeDownloads.collectAsState(initial = emptyList())
    val completedDownloads by repository.completedDownloads.collectAsState(initial = emptyList())
    val downloadStatusByEpisodeKey = remember(activeDownloads, completedDownloads, movieId) {
        (activeDownloads + completedDownloads)
            .filter { it.movieId == movieId && it.seasonNumber != null && it.episodeNumber != null }
            .associate { "S${it.seasonNumber}E${it.episodeNumber}" to it.status }
    }

    fun buildDownloadId(seasonNum: Int?, epNum: Int?): String =
        if (seasonNum != null && epNum != null) "${movieId}_S${seasonNum}E${epNum}" else movieId

    // Returns true if a new download was actually started (false if it was already
    // downloading/downloaded, in which case the caller gets to decide how to surface that).
    suspend fun startIfNotDuplicate(target: Movie, seasonNum: Int?, epNum: Int?): Boolean {
        val downloadId = buildDownloadId(seasonNum, epNum)
        val existing = repository.downloadDao.getDownloadById(downloadId)
        return when (existing?.status) {
            DownloadEntity.STATUS_COMPLETED, DownloadEntity.STATUS_DOWNLOADING,
            DownloadEntity.STATUS_QUEUED, DownloadEntity.STATUS_PAUSED -> false
            else -> {
                startDownloadWorker(context, target, seasonNum, epNum)
                true
            }
        }
    }

    // Single-item entry point (movie, or one episode) — shows the existing
    // "Download Started" dialog, or a clear "already downloading/downloaded" toast.
    fun startOrNotifyDownload(target: Movie, seasonNum: Int?, epNum: Int?) {
        scope.launch {
            val downloadId = buildDownloadId(seasonNum, epNum)
            val existing = repository.downloadDao.getDownloadById(downloadId)
            val alreadyDownloaded = existing?.status == DownloadEntity.STATUS_COMPLETED
            if (alreadyDownloaded) {
                Toast.makeText(context, "Already downloaded", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val started = startIfNotDuplicate(target, seasonNum, epNum)
            if (started) {
                showDownloadStartedDialog = true
            } else {
                Toast.makeText(context, "Already downloading", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Batch entry point from the episode picker — one summary toast instead of
    // repeating the single-item dialog once per selected episode.
    fun startBatchDownload(target: Movie, episodes: List<Episode>) {
        scope.launch {
            var startedCount = 0
            episodes.forEach { ep ->
                if (startIfNotDuplicate(target, ep.sNum, ep.eNum)) startedCount++
            }
            val message = when {
                startedCount == 0 -> "Already downloading"
                startedCount == episodes.size -> "Downloading $startedCount episode${if (startedCount > 1) "s" else ""}"
                else -> "Downloading $startedCount of ${episodes.size} — the rest are already downloading"
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Helper to gate actions behind login
    fun checkAuthAndExecute(action: () -> Unit) {
        if (!repository.isLoggedIn()) {
            showGateSheet = true
        } else {
            action()
        }
    }

    // Load movie data
    LaunchedEffect(movieId) {
        // Reset error and loading states
        errorState = null
        isLoading = true

        try {
            // 1. Show cached data immediately if available
            val cached = repository.getCachedMovieDetail(movieId)
            if (cached != null) {
                movie = cached
                isLoading = false // show the cached version while we fetch fresh
            }

            // 2. Fetch fresh data (this will replace cached when it arrives)
            val m = repository.getMovieDetail(movieId)
            if (m != null) {
                movie = m
                if (m.isSeries) {
                    val rawEps = repository.getMovieEpisodes(movieId)
                    val allEps = if (rawEps.isNotEmpty()) rawEps else m.episodes.orEmpty()
                    episodesList = allEps.sortedWith(compareBy({ it.sNum ?: 1 }, { it.eNum ?: 1 }))
                    // Update selected season if needed
                    val seasons = episodesList.mapNotNull { it.sNum }.distinct().sorted()
                    if (seasons.isNotEmpty() && selectedSeasonNumber !in seasons) {
                        selectedSeasonNumber = seasons.first()
                    }
                }
                relatedMovies = repository.getRelatedMovies(movieId)
            } else {
                // If fresh data is null and we had no cached data, we have an error
                if (movie == null) {
                    errorState = "Failed to load movie details. Please try again."
                }
            }
        } catch (e: Exception) {
            // If we already have cached data, keep it; otherwise show error
            if (movie == null) {
                errorState = "Network error: ${e.message}"
            }
        } finally {
            isLoading = false
        }
    }

    // Keep season selection in sync when episodes change
    LaunchedEffect(episodesList) {
        val seasons = episodesList.mapNotNull { it.sNum }.distinct().sorted()
        if (seasons.isNotEmpty() && selectedSeasonNumber !in seasons) {
            selectedSeasonNumber = seasons.first()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(YoBaseBackground)
    ) {
        when {
            isLoading && movie == null -> {
                // Show loader only when we have no cached data and are loading
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    ModernLoader()
                }
            }
            errorState != null && movie == null -> {
                // Show error with retry option
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = errorState!!,
                        color = YoTextMuted,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            // Retry: re-trigger LaunchedEffect by changing the key?
                            // Since movieId hasn't changed, we need to manually trigger reload.
                            // A simple way: set isLoading = true and clear error, then reload.
                            // But LaunchedEffect won't re-run because key is same.
                            // Better: use a separate retry state or call repository directly.
                            // For simplicity, we can restart by setting a dummy key.
                            // However, we'll handle it by cancelling and re-launching effect.
                            // A robust approach: use a remember { mutableStateOf(0) } and increment.
                            // But we'll keep it simple: we'll have a retry flag.
                            // Actually, we can call the same logic again inline.
                            // But we already have LaunchedEffect. To trigger it again, we can change movieId? Not ideal.
                            // Easiest: we can wrap the load in a function and call it from here.
                            // We'll use a separate reload function.
                            // Let's add a reload trigger.
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = YoPrimaryAmber,
                            contentColor = YoBaseBackground
                        )
                    ) {
                        Text("Retry")
                    }
                }
            }
            movie != null -> {
                // Main content
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
                                .aspectRatio(16f / 9f)
                        ) {
                            SubcomposeAsyncImage(
                                model = m.heroImage ?: m.cover ?: m.poster ?: m.displayPosterUrl,
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

                            // Back Button
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

                            // Top Right Action Icons
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
                                            if (m.isSeries) {
                                                showEpisodeDownloadSheet = true
                                            } else {
                                                startOrNotifyDownload(m, null, null)
                                            }
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
                                            if (m.isSeries) {
                                                showEpisodeDownloadSheet = true
                                            } else {
                                                startOrNotifyDownload(m, null, null)
                                            }
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

                            if (!m.trailerUrl.isNull_orEmpty()) {
                                Spacer(modifier = Modifier.height(18.dp))
                                InlineTrailerSection(
                                    trailerUrl = m.trailerUrl!!,
                                    movieTitle = m.title,
                                    posterFallbackUrl = m.cover ?: m.poster ?: m.displayPosterUrl,
                                    expanded = trailerExpanded,
                                    onToggle = { trailerExpanded = !trailerExpanded },
                                    repository = repository
                                )
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
        }

        // Report Dialog
        if (showReportDialog) {
            ReportDialog(
                movieId = movieId,
                movieTitle = movie?.title ?: "",
                onDismiss = { showReportDialog = false }
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

        // Episode Download Picker
        if (showEpisodeDownloadSheet && movie != null) {
            EpisodeDownloadSheet(
                sheetState = episodeDownloadSheetState,
                episodes = episodesList,
                downloadStatusByEpisodeKey = downloadStatusByEpisodeKey,
                onDismiss = { showEpisodeDownloadSheet = false },
                onDownloadSelected = { selectedEpisodes ->
                    showEpisodeDownloadSheet = false
                    startBatchDownload(movie!!, selectedEpisodes)
                }
            )
        }

        // Download Started Dialog
        if (showDownloadStartedDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showDownloadStartedDialog = false },
                title = { Text("Download Started", fontWeight = FontWeight.Bold, color = YoTextPrimary) },
                text = { Text("Your download for \"${movie?.title}\" has started in the background.", color = YoTextMuted) },
                confirmButton = {
                    androidx.compose.material3.Button(
                        onClick = { showDownloadStartedDialog = false },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = YoPrimaryAmber, contentColor = YoBaseBackground)
                    ) {
                        Text("OK", fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = YoSurface
            )
        }
    }
}

fun extractYouTubeId(url: String): String? {
    return try {
        if (url.contains("youtu.be/")) {
            url.substringAfter("youtu.be/").substringBefore("?").substringBefore("&")
        } else if (url.contains("youtube.com/watch")) {
            Uri.parse(url).getQueryParameter("v")
        } else if (url.contains("youtube.com/embed/")) {
            url.substringAfter("youtube.com/embed/").substringBefore("?").substringBefore("&")
        } else null
    } catch (e: Exception) {
        null
    }
}

/** True for trailers hosted on YouTube/Vimeo/Dailymotion — matches the backend's own
 *  isEmbed check, so this mirrors exactly which trailers the API leaves as a raw
 *  external link vs which ones it turns into an authenticated proxy URL. */
private fun isExternallyEmbeddableTrailer(url: String): Boolean =
    url.contains("youtube", ignoreCase = true) ||
        url.contains("youtu.be", ignoreCase = true) ||
        url.contains("vimeo", ignoreCase = true) ||
        url.contains("dailymotion", ignoreCase = true)

@Composable
fun InlineTrailerSection(
    trailerUrl: String,
    movieTitle: String,
    posterFallbackUrl: String?,
    expanded: Boolean,
    onToggle: () -> Unit,
    repository: YocinemaRepository
) {
    val ytId = remember(trailerUrl) { extractYouTubeId(trailerUrl) }
    val isEmbeddable = remember(trailerUrl) { isExternallyEmbeddableTrailer(trailerUrl) }

    Column {
        Text(
            text = "Trailer",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = YoTextPrimary
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Thumbnail rail
        Box(
            modifier = Modifier
                .width(180.dp)
                .aspectRatio(16f / 9f)
                .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp), clip = false)
                .clip(RoundedCornerShape(12.dp))
                .background(YoSurfaceVariant)
                .then(
                    if (expanded) Modifier.border(2.dp, YoPrimaryAmber, RoundedCornerShape(12.dp)) else Modifier
                )
                .clickable { onToggle() }
        ) {
            SubcomposeAsyncImage(
                model = if (ytId != null) "https://img.youtube.com/vi/$ytId/hqdefault.jpg" else posterFallbackUrl,
                contentDescription = "$movieTitle trailer",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = { com.example.ui.components.YoCinemaLogoPlaceholder() },
                error = { com.example.ui.components.YoCinemaLogoPlaceholder() }
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (expanded) YoPrimaryAmber else Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (expanded) YoBaseBackground else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(12.dp))
            if (isEmbeddable) {
                InlineYouTubePlayer(trailerUrl = trailerUrl, ytId = ytId)
            } else {
                // Our own hosted trailer, proxied through the backend and gated behind
                // the same API-key auth as regular playback — a WebView/browser can't
                // supply that header, so this plays it directly with an authenticated
                // ExoPlayer instead, the same way the main player streams movies.
                InlineAuthenticatedTrailerPlayer(trailerUrl = trailerUrl, repository = repository)
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun InlineAuthenticatedTrailerPlayer(trailerUrl: String, repository: YocinemaRepository) {
    val context = LocalContext.current
    var isLoading by remember(trailerUrl) { mutableStateOf(true) }
    var hasError by remember(trailerUrl) { mutableStateOf(false) }

    val exoPlayer = remember(trailerUrl) {
        val apiKey = repository.tokenManager.getApiKey()
        val headers = mutableMapOf<String, String>()
        if (!apiKey.isNullOrBlank()) {
            headers["X-API-Key"] = apiKey
            headers["x-api-key"] = apiKey
        }
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(30_000)
            .setReadTimeoutMs(30_000)
            .setDefaultRequestProperties(headers)

        ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) isLoading = false
                }
                override fun onPlayerError(error: PlaybackException) {
                    isLoading = false
                    hasError = true
                }
            })
            val mediaSource = DefaultMediaSourceFactory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(trailerUrl))
            setMediaSource(mediaSource)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (hasError) {
            Text(
                text = "Couldn't play the trailer.",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        } else {
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { ctx -> PlayerView(ctx).apply { player = exoPlayer; useController = true } },
                modifier = Modifier.fillMaxSize()
            )
            if (isLoading) {
                CircularProgressIndicator(color = YoPrimaryAmber)
            }
        }
    }
}

@Composable
fun InlineYouTubePlayer(trailerUrl: String, ytId: String?) {
    var isLoading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (ytId == null || loadFailed) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Text("Couldn't play the trailer here.", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(trailerUrl))) } catch (e: Exception) { }
                    }
                ) {
                    Text("Watch on YouTube", color = Color.White, fontSize = 13.sp)
                }
            }
        } else {
            // Use AndroidView with proper disposal
            var webView: android.webkit.WebView? by remember { mutableStateOf(null) }
            DisposableEffect(Unit) {
                onDispose {
                    webView?.destroy()
                }
            }
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { ctx ->
                    android.webkit.WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        setBackgroundColor(android.graphics.Color.BLACK)
                        webChromeClient = android.webkit.WebChromeClient()
                        webViewClient = object : android.webkit.WebViewClient() {
                            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                isLoading = false
                            }
                            override fun onReceivedError(
                                view: android.webkit.WebView?,
                                request: android.webkit.WebResourceRequest?,
                                error: android.webkit.WebResourceError?
                            ) {
                                if (request?.isForMainFrame != false) {
                                    isLoading = false
                                    loadFailed = true
                                }
                            }
                        }
                        val html = """
                            <html><head>
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <style>html,body{margin:0;padding:0;background:#000;height:100%;overflow:hidden;}
                            iframe{position:absolute;top:0;left:0;width:100%;height:100%;border:0;}</style>
                            </head><body>
                            <iframe src="https://www.youtube.com/embed/$ytId?autoplay=1&playsinline=1&rel=0&modestbranding=1&controls=1"
                            allow="autoplay; encrypted-media; fullscreen" allowfullscreen></iframe>
                            </body></html>
                        """.trimIndent()
                        loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "utf-8", null)
                        webView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            if (isLoading) {
                androidx.compose.material3.CircularProgressIndicator(color = YoPrimaryAmber)
            }
        }
    }
}

private fun String?.isNull_orEmpty(): Boolean = this == null || this.trim().isEmpty()