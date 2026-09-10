package com.example.ui.screens

import android.net.Uri
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.SubcomposeAsyncImage
import com.example.data.model.Episode
import com.example.data.model.Movie
import com.example.data.model.formatDuration
import com.example.data.model.isNull_orEmpty
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
import com.example.ui.components.DetailSkeleton
import com.example.ui.components.PosterCard
import com.example.ui.components.ReportDialog
import com.example.ui.components.VJBadgeChip
import com.example.ui.components.YoCinemaLogoPlaceholder
import com.example.ui.theme.YoBaseBackground
import com.example.ui.theme.YoBorder
import com.example.ui.theme.YoPrimaryViolet
import com.example.ui.theme.YoAccentCyan
import com.example.ui.theme.YoRatingGold
import com.example.ui.theme.YoSurface
import com.example.ui.theme.YoTextMuted
import com.example.ui.theme.YoTextPrimary
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

// YouTube imports
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

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
    var showDownloadStartedDialog by remember { mutableStateOf(false) }
    var isSynopsisExpanded by remember { mutableStateOf(false) }
    var selectedSeasonNumber by remember { mutableStateOf(1) }
    var showEpisodeDownloadSheet by remember { mutableStateOf(false) }

    val isWatchlisted by repository.isWatchlisted(movieId).collectAsState(initial = false)
    val gateSheetState = rememberModalBottomSheetState()
    val episodeDownloadSheetState = rememberModalBottomSheetState()

    val activeDownloads by repository.activeDownloads.collectAsState(initial = emptyList())
    val completedDownloads by repository.completedDownloads.collectAsState(initial = emptyList())
    val downloadStatusByEpisodeKey = remember(activeDownloads, completedDownloads, movieId) {
        (activeDownloads + completedDownloads)
            .filter { it.movieId == movieId && it.seasonNumber != null && it.episodeNumber != null }
            .associate { "S${it.seasonNumber}E${it.episodeNumber}" to it.status }
    }

    fun buildDownloadId(seasonNum: Int?, epNum: Int?): String =
        if (seasonNum != null && epNum != null) "${movieId}_S${seasonNum}E${epNum}" else movieId

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

    fun checkAuthAndExecute(action: () -> Unit) {
        if (!repository.isLoggedIn()) {
            showGateSheet = true
        } else {
            action()
        }
    }

    LaunchedEffect(movieId) {
        errorState = null
        isLoading = true
        try {
            val cached = repository.getCachedMovieDetail(movieId)
            if (cached != null) {
                movie = cached
                isLoading = false
            }

            val (m, related) = coroutineScope {
                val detailDeferred = async { repository.getMovieDetail(movieId) }
                val relatedDeferred = async { repository.getRelatedMovies(movieId) }
                detailDeferred.await() to relatedDeferred.await()
            }

            if (m != null) {
                movie = m
                relatedMovies = related
                if (m.isSeries) {
                    val rawEps = repository.getMovieEpisodes(movieId)
                    val allEps = if (rawEps.isNotEmpty()) rawEps else m.episodes.orEmpty()
                    episodesList = allEps.sortedWith(compareBy({ it.sNum ?: 1 }, { it.eNum ?: 1 }))
                    val seasons = episodesList.mapNotNull { it.sNum }.distinct().sorted()
                    if (seasons.isNotEmpty() && selectedSeasonNumber !in seasons) {
                        selectedSeasonNumber = seasons.first()
                    }
                }
            } else {
                if (movie == null) {
                    errorState = "Failed to load movie details. Please try again."
                }
            }
        } catch (e: Exception) {
            if (movie == null) {
                errorState = "Network error: ${e.message}"
            }
        } finally {
            isLoading = false
        }
    }

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
                DetailSkeleton()
            }
            errorState != null && movie == null -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = errorState!!,
                        color = YoTextMuted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onBackClick() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = YoPrimaryViolet,
                            contentColor = YoBaseBackground
                        )
                    ) {
                        Text("Go Back")
                    }
                }
            }
            movie != null -> {
                val m = movie!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    // Backdrop — fixed height (not a strict 16:9 crop) so the
                    // hero image reads as a full backdrop rather than a
                    // half-cropped poster
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        ) {
                            SubcomposeAsyncImage(
                                model = m.heroImage ?: m.cover ?: m.poster ?: m.displayPosterUrl,
                                contentDescription = m.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                loading = { YoCinemaLogoPlaceholder() },
                                error = { YoCinemaLogoPlaceholder() }
                            )

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
                                        tint = if (isWatchlisted) YoPrimaryViolet else YoTextPrimary
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

                    // Title, etc.
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = m.title,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = YoTextPrimary
                            )

                            Spacer(modifier = Modifier.height(12.dp))

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
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = YoRatingGold,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = m.imdbRating!!,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = YoTextPrimary
                                        )
                                    }
                                }

                                if (m.duration != null && m.duration > 0) {
                                    Text(
                                        text = formatDuration(m.duration),
                                        fontSize = 13.sp,
                                        color = YoTextMuted
                                    )
                                }

                                if (!m.releaseDate.isNull_orEmpty()) {
                                    Text(
                                        text = m.releaseDate!!.take(4),
                                        fontSize = 13.sp,
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
                                    color = YoAccentCyan
                                )
                            }

                            Spacer(modifier = Modifier.height(22.dp))

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
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = YoPrimaryViolet,
                                        contentColor = YoBaseBackground
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Watch Now", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                                    shape = RoundedCornerShape(14.dp),
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
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Download", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(22.dp))

                            if (!m.description.isNull_orEmpty()) {
                                Column(modifier = Modifier.animateContentSize()) {
                                    Text(
                                        text = m.description!!,
                                        fontSize = 14.sp,
                                        color = YoTextMuted,
                                        lineHeight = 22.sp,
                                        maxLines = if (isSynopsisExpanded) Int.MAX_VALUE else 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isSynopsisExpanded) "SHOW LESS" else "MORE...",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = YoPrimaryViolet,
                                        modifier = Modifier
                                            .clickable { isSynopsisExpanded = !isSynopsisExpanded }
                                            .padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Episodes
                    if (m.isSeries && episodesList.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(28.dp))
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
                                    Spacer(modifier = Modifier.height(12.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(availableSeasons) { seasonNum ->
                                            val isSelected = seasonNum == selectedSeasonNumber
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(20.dp))
                                                    .background(if (isSelected) YoPrimaryViolet else YoSurface)
                                                    .border(1.dp, if (isSelected) YoPrimaryViolet else YoBorder, RoundedCornerShape(20.dp))
                                                    .clickable { selectedSeasonNumber = seasonNum }
                                                    .padding(horizontal = 16.dp, vertical = 8.dp)
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

                                Spacer(modifier = Modifier.height(14.dp))

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

                    // Cast
                    val allCast = m.cast.orEmpty()
                    if (allCast.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(28.dp))
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Cast & Crew",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = YoTextPrimary,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )

                                Spacer(modifier = Modifier.height(14.dp))

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

                    // Related
                    if (relatedMovies.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(28.dp))
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "More Like This",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = YoTextPrimary,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(relatedMovies) { rel ->
                                        PosterCard(
                                            movie = rel,
                                            onClick = { onRelatedMovieClick(rel.id) },
                                            widthDp = 100
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dialogs and sheets
        if (showReportDialog) {
            ReportDialog(
                movieId = movieId,
                movieTitle = movie?.title ?: "",
                onDismiss = { showReportDialog = false }
            )
        }

        if (showGateSheet) {
            GateModalBottomSheet(
                sheetState = gateSheetState,
                onDismiss = { showGateSheet = false },
                onEnterKeyClicked = onEnterApiKeyRequested
            )
        }

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

        if (showDownloadStartedDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showDownloadStartedDialog = false },
                title = { Text("Download Started", fontWeight = FontWeight.Bold, color = YoTextPrimary) },
                text = { Text("Your download for \"${movie?.title}\" has started in the background.", color = YoTextMuted) },
                confirmButton = {
                    androidx.compose.material3.Button(
                        onClick = { showDownloadStartedDialog = false },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = YoPrimaryViolet, contentColor = YoBaseBackground)
                    ) {
                        Text("OK", fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = YoSurface
            )
        }
    }
}

