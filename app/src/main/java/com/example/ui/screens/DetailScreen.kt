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
import com.example.ui.components.YoCinemaLogoPlaceholder
import com.example.ui.theme.YoBaseBackground
import com.example.ui.theme.YoBorder
import com.example.ui.theme.YoPrimaryAmber
import com.example.ui.theme.YoSurface
import com.example.ui.theme.YoSurfaceVariant
import com.example.ui.theme.YoTextMuted
import com.example.ui.theme.YoTextPrimary
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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

            // getMovieDetail and getRelatedMovies both only need movieId —
            // no reason to wait for detail before starting related. Episodes
            // stays sequential after detail resolves since it's conditional
            // on m.isSeries, which we don't know until then; fetching it
            // speculatively for every movie would waste a call on every
            // non-series title just to save latency on series ones.
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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    ModernLoader()
                }
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
                            containerColor = YoPrimaryAmber,
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
                                            tint = YoPrimaryAmber,
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
                                    color = YoPrimaryAmber
                                )
                            }

                            Spacer(modifier = Modifier.height(22.dp))

                            // Play & Download Buttons
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
                                        containerColor = YoPrimaryAmber,
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

                            if (!m.trailerUrl.isNull_orEmpty()) {
                                Spacer(modifier = Modifier.height(20.dp))
                                InlineTrailerSection(
                                    trailerUrl = m.trailerUrl!!,
                                    movieTitle = m.title,
                                    posterFallbackUrl = m.cover ?: m.poster ?: m.displayPosterUrl,
                                    expanded = trailerExpanded,
                                    onToggle = { trailerExpanded = !trailerExpanded },
                                    repository = repository
                                )
                            }

                            Spacer(modifier = Modifier.height(22.dp))

                            // Synopsis
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
                                                    .background(if (isSelected) YoPrimaryAmber else YoSurface)
                                                    .border(1.dp, if (isSelected) YoPrimaryAmber else YoBorder, RoundedCornerShape(20.dp))
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

                    // Cast Rail
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

                    // Related Movies Rail
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
                                            widthDp = 100   // 👈 fixed size
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
        } else if (url.contains("youtube.com/shorts/")) {
            url.substringAfter("youtube.com/shorts/").substringBefore("?").substringBefore("&")
        } else null
    } catch (e: Exception) {
        null
    }
}

/**
 * Builds a real, directly-navigable embed URL — mirrors the website's
 * embedUrl() in WatchDialog.tsx exactly. This is what actually gets loaded
 * (via webView.loadUrl, a genuine navigation), not wrapped in synthetic HTML.
 * Returns null for anything that isn't a known embeddable platform, in which
 * case the caller falls back to the authenticated/hosted player.
 */
private fun buildEmbedUrl(url: String): String? {
    return try {
        val uri = Uri.parse(url)
        val host = uri.host?.removePrefix("www.")?.lowercase() ?: return null
        when {
            host == "youtu.be" -> {
                val id = uri.pathSegments.firstOrNull()?.substringBefore("?")
                id?.let { "https://www.youtube.com/embed/$it?autoplay=1&rel=0&playsinline=1" }
            }
            host.contains("youtube.com") -> {
                val id = extractYouTubeId(url)
                id?.let { "https://www.youtube.com/embed/$it?autoplay=1&rel=0&playsinline=1" }
            }
            host.contains("vimeo.com") -> {
                val id = uri.pathSegments.lastOrNull()
                id?.let { "https://player.vimeo.com/video/$it?autoplay=1" }
            }
            host.contains("dailymotion.com") -> {
                val id = url.substringAfter("/video/", "").substringBefore("_").ifBlank { null }
                id?.let { "https://www.dailymotion.com/embed/video/$it?autoplay=1" }
            }
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}

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
    val embedUrl = remember(trailerUrl) { buildEmbedUrl(trailerUrl) }

    Column {
        Text(
            text = "Trailer",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = YoTextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .width(180.dp)
                .aspectRatio(16f / 9f)
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(14.dp), clip = false)
                .clip(RoundedCornerShape(14.dp))
                .background(YoSurfaceVariant)
                .then(
                    if (expanded) Modifier.border(2.dp, YoPrimaryAmber, RoundedCornerShape(14.dp)) else Modifier
                )
                .clickable { onToggle() }
        ) {
            SubcomposeAsyncImage(
                model = if (ytId != null) "https://img.youtube.com/vi/$ytId/hqdefault.jpg" else posterFallbackUrl,
                contentDescription = "$movieTitle trailer",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = { YoCinemaLogoPlaceholder() },
                error = { YoCinemaLogoPlaceholder() }
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (expanded) YoPrimaryAmber else Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (expanded) YoBaseBackground else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(14.dp))
            if (embedUrl != null) {
                InlineEmbedTrailerPlayer(trailerUrl = trailerUrl, embedUrl = embedUrl)
            } else {
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
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (hasError) {
            Text(
                text = "Couldn't play the trailer.",
                color = Color.White,
                fontSize = 14.sp,
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
fun InlineEmbedTrailerPlayer(trailerUrl: String, embedUrl: String) {
    var isLoading by remember(embedUrl) { mutableStateOf(true) }
    var loadFailed by remember(embedUrl) { mutableStateOf(false) }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (loadFailed) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Text("Couldn't play the trailer here.", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(trailerUrl))) } catch (e: Exception) { }
                    }
                ) {
                    Text("Watch externally", color = Color.White, fontSize = 14.sp)
                }
            }
        } else {
            var webView: android.webkit.WebView? by remember { mutableStateOf(null) }
            DisposableEffect(Unit) {
                onDispose {
                    webView?.destroy()
                }
            }
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { ctx ->
                    // Third-party cookies must be explicitly enabled — off by
                    // default since API 21 — or YouTube's consent/session
                    // cookies get silently blocked and playback just hangs.
                    android.webkit.CookieManager.getInstance().setAcceptCookie(true)
                    android.webkit.WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        setBackgroundColor(android.graphics.Color.BLACK)
                        android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webChromeClient = android.webkit.WebChromeClient()
                        webViewClient = object : android.webkit.WebViewClient() {
                            // onPageFinished fires as soon as navigation completes — but
                            // "completed" can mean YouTube's own error page loaded (e.g.
                            // embedding disabled for that video), which paints white.
                            // Hiding the spinner on onPageFinished alone reveals that
                            // white flash underneath. onPageCommitVisible (API 23+, safe
                            // here since minSdk 24) fires only once real pixels have
                            // actually been painted to the screen, so the spinner stays
                            // up until there's genuinely something to look at.
                            override fun onPageCommitVisible(view: android.webkit.WebView?, url: String?) {
                                isLoading = false
                                // Content check for the case onPageCommitVisible/
                                // onPageFinished can't catch: YouTube's own embed
                                // page loading "successfully" but showing its own
                                // white error card (e.g. "playback on other
                                // websites has been disabled by the video owner").
                                // A working embed injects a real <video> element;
                                // give it a moment to settle, then check.
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    try {
                                        view?.evaluateJavascript(
                                            "(function(){return !!document.querySelector('video');})();"
                                        ) { result ->
                                            if (result == "false") {
                                                loadFailed = true
                                            }
                                        }
                                    } catch (e: Exception) {
                                        // WebView may have been destroyed (user collapsed
                                        // the trailer / navigated away) before this fired.
                                    }
                                }, 2500)
                            }
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
                        // Navigate straight to the real embed URL — same as
                        // the website's <iframe src="..."> — instead of
                        // wrapping it in a synthetic loadDataWithBaseURL host
                        // page. That synthetic-page trick is what was
                        // breaking playback: the WebView never actually
                        // navigates to youtube.com, so referrer/consent/
                        // postMessage checks the real embed player relies on
                        // don't line up, and it silently hangs instead of
                        // erroring — nothing for onReceivedError to catch.
                        loadUrl(embedUrl)
                        webView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            if (isLoading) {
                CircularProgressIndicator(color = YoPrimaryAmber)
            }
        }
    }
}

private fun String?.isNull_orEmpty(): Boolean = this == null || this.trim().isEmpty()