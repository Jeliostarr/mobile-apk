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
    var showTrailerModal by remember { mutableStateOf(false) }
    var showDownloadStartedDialog by remember { mutableStateOf(false) }
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
            // Paint instantly from cache on a revisit — only show the full
            // loading state on a genuinely first-ever view of this title.
            val cached = repository.getCachedMovieDetail(movieId)
            if (cached != null) {
                movie = cached
                isLoading = false
            } else {
                isLoading = true
            }

            // Always still fetch the latest copy — this silently replaces
            // the cached data above once it arrives, so revisits feel
            // instant without ever going stale.
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
                            .aspectRatio(16f / 9f)
                    ) {
                        SubcomposeAsyncImage(
                            // heroImage is the proper wide banner shot — cover/poster
                            // are tall and only used as a last-resort fallback, since
                            // stretching them here crops them into an unrecognizable sliver.
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
                                        showDownloadStartedDialog = true
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
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { showTrailerModal = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, YoPrimaryAmber),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = YoPrimaryAmber
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Watch Trailer", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                movieId = movieId,
                movieTitle = movie?.title ?: "Unknown Movie",
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

        if (showTrailerModal && !movie?.trailerUrl.isNullOrBlank()) {
            InAppTrailerModal(
                trailerUrl = movie!!.trailerUrl!!,
                onDismiss = { showTrailerModal = false }
            )
        }
    }
}

fun extractYouTubeId(url: String): String? {
    return try {
        if (url.contains("youtu.be/")) {
            url.substringAfter("youtu.be/").substringBefore("?").substringBefore("&")
        } else if (url.contains("youtube.com/watch")) {
            android.net.Uri.parse(url).getQueryParameter("v")
        } else if (url.contains("youtube.com/embed/")) {
            url.substringAfter("youtube.com/embed/").substringBefore("?").substringBefore("&")
        } else null
    } catch (e: Exception) {
        null
    }
}

@Composable
fun InAppTrailerModal(
    trailerUrl: String,
    onDismiss: () -> Unit
) {
    val ytId = extractYouTubeId(trailerUrl)
    var isLoading by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
    var loadFailed by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Official Trailer",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (ytId == null || loadFailed) {
                        // Never leave a dead black box — give a clear way
                        // forward instead of a silent, confusing failure.
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = "Couldn't play the trailer here.",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            androidx.compose.material3.OutlinedButton(
                                onClick = {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(trailerUrl)))
                                    } catch (e: Exception) { }
                                }
                            ) {
                                Text("Watch on YouTube", color = Color.White)
                            }
                        }
                    } else {
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
                                    // Navigating a WebView directly to
                                    // youtube.com/embed/... as a top-level page
                                    // is the actual reason trailers were silently
                                    // failing to play — YouTube's embed player
                                    // expects to be loaded inside an iframe on a
                                    // real page with a real origin, not opened
                                    // directly as the page itself. Wrapping it in
                                    // a minimal local HTML page with a real
                                    // youtube.com base URL is what makes this
                                    // work reliably across devices.
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
                                }
                            },
                            update = { /* no-op: content is fixed per dialog instance */ },
                            modifier = Modifier.fillMaxSize()
                        )

                        if (isLoading) {
                            androidx.compose.material3.CircularProgressIndicator(color = YoPrimaryAmber)
                        }
                    }
                }
            }
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
