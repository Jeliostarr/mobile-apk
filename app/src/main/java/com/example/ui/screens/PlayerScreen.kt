package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Build
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.download.DownloadWorker
import androidx.compose.material3.ButtonDefaults
import com.example.ui.components.PosterCard
import com.example.ui.components.ReportDialog
import com.example.ui.components.RequestDialog
import com.example.ui.components.YoCinemaLogoPlaceholder
import com.example.data.model.Movie
import com.example.data.model.formatDuration
import com.example.player.PlayerManager
import com.example.repository.YocinemaRepository
import com.example.ui.components.ModernLoader
import com.example.ui.components.VJBadgeChip
import com.example.ui.theme.YoBaseBackground
import com.example.ui.theme.YoBorder
import com.example.ui.theme.YoPrimaryAmber
import com.example.ui.theme.YoSurface
import com.example.ui.theme.YoSurfaceVariant
import com.example.ui.theme.YoTextMuted
import com.example.ui.theme.YoTextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    movieId: String,
    seasonNum: Int?,
    epNum: Int?,
    localFilePath: String? = null,
    initialPosMs: Long = 0L,
    repository: YocinemaRepository,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activity = context as? Activity

    var activeMovieId by remember { mutableStateOf(movieId) }
    var activeSeasonNum by remember { mutableStateOf(seasonNum) }
    var activeEpNum by remember { mutableStateOf(epNum) }
    var movie by remember { mutableStateOf<Movie?>(null) }
    var relatedMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var mediaUrl by remember { mutableStateOf("") }
    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var isControlsVisible by rememberSaveable { mutableStateOf(true) }
    var isSynopsisExpanded by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showRequestDialog by remember { mutableStateOf(false) }

    val playerManager = remember { PlayerManager(context, repository, scope) }
    val isPlaying by playerManager.isPlaying.collectAsState()
    val currentPosMs by playerManager.currentPositionMs.collectAsState()
    val durationMs by playerManager.durationMs.collectAsState()
    val isReconnecting by playerManager.isReconnecting.collectAsState()
    val playerError by playerManager.playerError.collectAsState()

    var gestureOverlayText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(activeMovieId, activeSeasonNum, activeEpNum, localFilePath) {
        if (!localFilePath.isNull_orBlank()) {
            movie = Movie(id = activeMovieId, title = "Offline Download")
            mediaUrl = localFilePath!!
            playerManager.playMedia(
                activeMovieId, localFilePath!!, activeSeasonNum, activeEpNum, initialPosMs,
                title = "Offline Download"
            )
        } else {
            // Check if downloaded in DB first
            val downloadId = if (activeSeasonNum != null && activeEpNum != null) "${activeMovieId}_S${activeSeasonNum}E${activeEpNum}" else activeMovieId
            val downloadedEntity = repository.downloadDao.getDownloadById(downloadId)
            if (downloadedEntity != null && downloadedEntity.status == "COMPLETED" && downloadedEntity.localFilePath.isNotBlank()) {
                val file = java.io.File(downloadedEntity.localFilePath)
                if (file.exists()) {
                    val m = repository.getMovieDetail(activeMovieId)
                    movie = m ?: Movie(id = activeMovieId, title = downloadedEntity.title)
                    mediaUrl = downloadedEntity.localFilePath
                    relatedMovies = repository.getRelatedMovies(activeMovieId)
                    playerManager.playMedia(
                        activeMovieId, downloadedEntity.localFilePath, activeSeasonNum, activeEpNum, initialPosMs,
                        title = movie?.title ?: downloadedEntity.title,
                        posterUrl = movie?.cover ?: movie?.poster ?: movie?.displayPosterUrl
                    )
                    return@LaunchedEffect
                }
            }

            val m = repository.getMovieDetail(activeMovieId)
            movie = m
            if (m != null) {
                val url = repository.getPlayUrl(m, activeSeasonNum, activeEpNum)
                mediaUrl = url
                relatedMovies = repository.getRelatedMovies(activeMovieId)
                playerManager.playMedia(
                    activeMovieId, url, activeSeasonNum, activeEpNum, initialPosMs,
                    title = m.title,
                    posterUrl = m.cover ?: m.poster ?: m.displayPosterUrl
                )
            }
        }
    }

    DisposableEffect(Unit) {
        // Keep the screen on for the whole time this screen is composed —
        // without this, the device's normal screen-timeout still applies
        // during playback and the display turns off mid-video.
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            playerManager.release()
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { win ->
                WindowInsetsControllerCompat(win, win.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // True immersive fullscreen: hide the status/nav bars while in fullscreen
    // mode, restore them the moment we leave it. Rotating the device alone
    // doesn't do this — the status bar staying visible on top of a
    // "fullscreen" landscape video is what makes it look unfinished.
    LaunchedEffect(isFullscreen) {
        val win = activity?.window ?: return@LaunchedEffect
        val controller = WindowInsetsControllerCompat(win, win.decorView)
        if (isFullscreen) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Auto-hide controls in fullscreen mode after 3s
    LaunchedEffect(isControlsVisible, isFullscreen) {
        if (isFullscreen && isControlsVisible && isPlaying) {
            delay(3000)
            isControlsVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(YoBaseBackground)
    ) {
        if (isFullscreen) {
            // Fullscreen Landscape View
            FullscreenPlayerView(
                playerManager = playerManager,
                movie = movie,
                seasonNum = seasonNum,
                epNum = epNum,
                isPlaying = isPlaying,
                currentPosMs = currentPosMs,
                durationMs = durationMs,
                isReconnecting = isReconnecting,
                isControlsVisible = isControlsVisible,
                gestureOverlayText = gestureOverlayText,
                onToggleControls = { isControlsVisible = !isControlsVisible },
                onTogglePlay = {
                    if (isPlaying) playerManager.exoPlayer.pause()
                    else playerManager.exoPlayer.play()
                },
                onSeekBy = { deltaMs ->
                    val newPos = (currentPosMs + deltaMs).coerceIn(0L, durationMs)
                    playerManager.exoPlayer.seekTo(newPos)
                    gestureOverlayText = if (deltaMs > 0) "+10s" else "-10s"
                    scope.launch {
                        delay(1000)
                        gestureOverlayText = null
                    }
                },
                onSeekTo = { pos -> playerManager.exoPlayer.seekTo(pos) },
                onToggleFullscreen = {
                    isFullscreen = false
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                },
                onBackClick = {
                    isFullscreen = false
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            )
        } else {
            // Portrait View: 16:9 Video Box with IN-PLAYER OVERLAY CONTROLS + Clean Feed Below
            Column(modifier = Modifier.fillMaxSize()) {
                // Video Player Container (Fixed 16:9 Aspect Ratio)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                        .clickable { isControlsVisible = !isControlsVisible }
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = playerManager.exoPlayer
                                useController = false // Custom overlay controls inside player
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Poster Thumbnail Overlay when video isn't playing yet
                    if (!isPlaying && movie != null) {
                        SubcomposeAsyncImage(
                            model = movie?.cover ?: movie?.poster ?: movie?.displayPosterUrl,
                            contentDescription = movie?.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            loading = { YoCinemaLogoPlaceholder() },
                            error = { YoCinemaLogoPlaceholder() }
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center
                        ) {
                            ModernLoader(size = 36.dp)
                        }
                    }

                    // Player Error / Reconnecting Overlay
                    if (playerError != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Playback Error",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = playerError ?: "Failed to load stream.",
                                    color = YoTextMuted,
                                    fontSize = 12.sp,
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        if (movie != null) {
                                            scope.launch {
                                                val url = repository.getPlayUrl(movie!!, activeSeasonNum, activeEpNum)
                                                playerManager.playMedia(
                                                    activeMovieId, url, activeSeasonNum, activeEpNum, currentPosMs,
                                                    title = movie?.title,
                                                    posterUrl = movie?.cover ?: movie?.poster ?: movie?.displayPosterUrl
                                                )
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = YoPrimaryAmber,
                                        contentColor = YoBaseBackground
                                    )
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retry", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retry Stream", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    } else if (isReconnecting) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                ModernLoader(size = 36.dp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Reconnecting stream...",
                                    color = YoPrimaryAmber,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // IN-PLAYER OVERLAY CONTROLS
                    if (isControlsVisible || !isPlaying) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.7f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.85f)
                                        )
                                    )
                                )
                                .padding(10.dp)
                        ) {
                            // Top Bar inside Player
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(
                                    onClick = onBackClick,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.5f))
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Text(
                                    text = movie?.title ?: "",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 12.dp)
                                )

                                IconButton(
                                    onClick = {
                                        isFullscreen = true
                                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.5f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fullscreen,
                                        contentDescription = "Fullscreen",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Center Play/Pause / Seek Overlay
                            Row(
                                modifier = Modifier.align(Alignment.Center),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        val newPos = (currentPosMs - 10000L).coerceAtLeast(0L)
                                        playerManager.exoPlayer.seekTo(newPos)
                                    },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.4f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FastRewind,
                                        contentDescription = "-10s",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(58.dp)
                                        .shadow(elevation = 6.dp, shape = CircleShape, clip = false)
                                        .clip(CircleShape)
                                        .background(YoPrimaryAmber)
                                        .clickable {
                                            if (isPlaying) playerManager.exoPlayer.pause()
                                            else playerManager.exoPlayer.play()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = YoBaseBackground,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val newPos = (currentPosMs + 10000L).coerceAtMost(durationMs)
                                        playerManager.exoPlayer.seekTo(newPos)
                                    },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.4f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FastForward,
                                        contentDescription = "+10s",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            // Bottom Bar inside Player (Progress bar + time)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                            ) {
                                Slider(
                                    value = currentPosMs.toFloat(),
                                    onValueChange = { playerManager.exoPlayer.seekTo(it.toLong()) },
                                    valueRange = 0f..(durationMs.coerceAtLeast(1L).toFloat()),
                                    colors = SliderDefaults.colors(
                                        thumbColor = YoPrimaryAmber,
                                        activeTrackColor = YoPrimaryAmber,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(24.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${formatMs(currentPosMs)} / ${formatMs(durationMs)}",
                                        fontSize = 11.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        IconButton(
                                            onClick = { activity?.enterPictureInPictureMode() },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PictureInPicture,
                                                contentDescription = "PiP",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // SCROLLABLE FEED DIRECTLY BELOW THE VIDEO PLAYER
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val m = movie
                    if (m != null) {
                        item {
                            Column {
                                // Title & VJ Badge
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = m.title,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = YoTextPrimary
                                        )
                                        if (activeSeasonNum != null && activeEpNum != null) {
                                            Text(
                                                text = "Season $activeSeasonNum · Episode $activeEpNum",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = YoPrimaryAmber
                                            )
                                        }
                                    }

                                    if (!m.vjName.isNullOrBlank()) {
                                        VJBadgeChip(vjName = m.vjName!!)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Metadata Badges (Rating, Release Year, Formatted Duration, Genre)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (!m.imdbRating.isNullOrBlank()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(YoSurface)
                                                .border(1.dp, YoBorder, RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
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

                                    val yearStr = com.example.data.model.extractYearOnly(m.releaseDate)
                                    if (yearStr.isNotBlank()) {
                                        Text(
                                            text = yearStr,
                                            fontSize = 12.sp,
                                            color = YoTextMuted,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    val durationStr = formatDuration(m.duration)
                                    if (durationStr.isNotBlank()) {
                                        Text(
                                            text = "• $durationStr",
                                            fontSize = 12.sp,
                                            color = YoTextMuted,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    if (!m.genre.isNullOrBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(YoSurfaceVariant)
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = m.genre!!,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = YoPrimaryAmber
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Quick Action Bar (Download, Favorite, Report)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val downloadId = "${m.id}_${if (activeSeasonNum != null && activeEpNum != null) "S${activeSeasonNum}E${activeEpNum}" else "movie"}"
                                            val data = workDataOf(
                                                DownloadWorker.KEY_DOWNLOAD_ID to downloadId,
                                                DownloadWorker.KEY_MOVIE_ID to m.id,
                                                DownloadWorker.KEY_TITLE to m.title,
                                                DownloadWorker.KEY_SEASON_NUM to (activeSeasonNum ?: -1),
                                                DownloadWorker.KEY_EP_NUM to (activeEpNum ?: -1)
                                            )
                                            val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                                                .setInputData(data)
                                                .build()
                                            WorkManager.getInstance(context).enqueue(request)
                                        },
                                        modifier = Modifier.weight(1f).height(42.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = YoSurface,
                                            contentColor = YoPrimaryAmber
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, YoBorder)
                                    ) {
                                        Icon(imageVector = Icons.Default.Download, contentDescription = "Download", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Download", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { showRequestDialog = true },
                                        modifier = Modifier.height(42.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, YoBorder),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = YoTextMuted)
                                    ) {
                                        Icon(imageVector = Icons.Default.MovieFilter, contentDescription = "Request", modifier = Modifier.size(16.dp))
                                    }

                                    OutlinedButton(
                                        onClick = { showReportDialog = true },
                                        modifier = Modifier.height(42.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, YoBorder),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = YoTextMuted)
                                    ) {
                                        Icon(imageVector = Icons.Default.Flag, contentDescription = "Report", modifier = Modifier.size(16.dp))
                                    }
                                }

                                // Synopsis Description
                                if (!m.description.isNullOrEmpty()) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = m.description!!,
                                        fontSize = 13.sp,
                                        color = YoTextMuted,
                                        lineHeight = 18.sp,
                                        maxLines = if (isSynopsisExpanded) Int.MAX_VALUE else 3,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.clickable { isSynopsisExpanded = !isSynopsisExpanded }
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isSynopsisExpanded) "SHOW LESS" else "MORE...",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = YoPrimaryAmber,
                                        modifier = Modifier.clickable { isSynopsisExpanded = !isSynopsisExpanded }
                                    )
                                }
                            }
                        }

                        // Related Movies Feed Section
                        if (relatedMovies.isNotEmpty()) {
                            item {
                                Column {
                                    Text(
                                        text = "You Might Also Like",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = YoTextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(relatedMovies) { rel ->
                                            PosterCard(
                                                movie = rel,
                                                onClick = {
                                                    activeMovieId = rel.id
                                                    activeSeasonNum = null
                                                    activeEpNum = null
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showReportDialog && movie != null) {
            ReportDialog(
                movieId = activeMovieId,
                movieTitle = movie?.title ?: "",
                onDismiss = { showReportDialog = false }
            )
        }

        if (showRequestDialog) {
            RequestDialog(onDismiss = { showRequestDialog = false })
        }
    }
}

@Composable
fun FullscreenPlayerView(
    playerManager: PlayerManager,
    movie: Movie?,
    seasonNum: Int?,
    epNum: Int?,
    isPlaying: Boolean,
    currentPosMs: Long,
    durationMs: Long,
    isReconnecting: Boolean,
    isControlsVisible: Boolean,
    gestureOverlayText: String?,
    onToggleControls: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeekBy: (Long) -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleFullscreen: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val activity = context as? Activity

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggleControls() },
                    onDoubleTap = { offset ->
                        val halfWidth = size.width / 2
                        if (offset.x < halfWidth) {
                            onSeekBy(-10000L)
                        } else {
                            onSeekBy(10000L)
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, dragAmount ->
                    val halfWidth = size.width / 2
                    if (change.position.x < halfWidth) {
                        // Left Side Drag: Brightness
                        activity?.window?.attributes?.let { attr ->
                            val current = if (attr.screenBrightness < 0) 0.5f else attr.screenBrightness
                            attr.screenBrightness = (current - dragAmount / 500f).coerceIn(0.01f, 1f)
                            activity.window.attributes = attr
                        }
                    } else {
                        // Right Side Drag: Volume
                        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        val delta = if (dragAmount > 0) -1 else 1
                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, delta, 0)
                    }
                }
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = playerManager.exoPlayer
                    useController = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Gesture feedback text (+10s / -10s)
        if (!gestureOverlayText.isNull_orBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = gestureOverlayText!!,
                    color = YoPrimaryAmber,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Animated Fullscreen Controls Overlay
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(16.dp)
            ) {
                // Top Header Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBackClick) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = YoTextPrimary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = movie?.title ?: "",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = YoTextPrimary,
                            maxLines = 1
                        )
                    }

                    IconButton(onClick = onToggleFullscreen) {
                        Icon(imageVector = Icons.Default.FullscreenExit, contentDescription = "Exit Fullscreen", tint = YoTextPrimary)
                    }
                }

                // Center Play / Pause Button
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(68.dp)
                        .shadow(elevation = 8.dp, shape = CircleShape, clip = false)
                        .clip(CircleShape)
                        .background(YoPrimaryAmber)
                        .clickable { onTogglePlay() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = YoBaseBackground,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Bottom Seek Slider
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    Slider(
                        value = currentPosMs.toFloat(),
                        onValueChange = { onSeekTo(it.toLong()) },
                        valueRange = 0f..(durationMs.coerceAtLeast(1L).toFloat()),
                        colors = SliderDefaults.colors(
                            thumbColor = YoPrimaryAmber,
                            activeTrackColor = YoPrimaryAmber,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = formatMs(currentPosMs), fontSize = 12.sp, color = YoTextPrimary)
                        Text(text = formatMs(durationMs), fontSize = 12.sp, color = YoTextPrimary)
                    }
                }
            }
        }
    }
}

fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

private fun String?.isNull_orBlank(): Boolean = this == null || this.trim().isEmpty()
