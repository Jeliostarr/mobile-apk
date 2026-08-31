package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import com.example.ui.components.YoCinemaLogoPlaceholder
import com.example.data.model.Episode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.Movie
import com.example.player.PlayerManager
import com.example.repository.YocinemaRepository
import com.example.ui.theme.YoBaseBackground
import com.example.ui.theme.YoPrimaryViolet
import com.example.ui.theme.YoTextMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "PlayerScreen"

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

@OptIn(UnstableApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
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

    val playerManager = remember { PlayerManager(context, repository, scope) }

    var movie by remember { mutableStateOf<Movie?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var isDragging by remember { mutableStateOf(false) }
    var dragPositionMs by remember { mutableFloatStateOf(0f) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    // ─── New: lock, volume/brightness swipe, double-tap seek, hold-for-2x ───
    var isLocked by remember { mutableStateOf(false) }
    // Controls whether the small unlock icon is currently shown — behaves
    // like the normal controls (auto-hides after a few seconds, tapping
    // the screen while locked brings it back) rather than staying
    // permanently on screen.
    var showLockHint by remember { mutableStateOf(true) }

    val audioManager = remember {
        context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager
    }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var volumeLevel by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume)
    }
    // Screen-brightness override lives on the Activity's own window — no
    // special permission needed (unlike writing the system-wide setting),
    // and it's reset back to "use system default" in the DisposableEffect
    // below so it never leaks into other screens after leaving the player.
    var brightnessLevel by remember {
        mutableFloatStateOf(
            try {
                Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128) / 255f
            } catch (e: Exception) {
                0.5f
            }
        )
    }
    var showVolumeIndicator by remember { mutableStateOf(false) }
    var showBrightnessIndicator by remember { mutableStateOf(false) }
    // true = forward (right side double-tap), false = back (left side), null = hidden
    var seekIndicatorSide by remember { mutableStateOf<Boolean?>(null) }
    var isFastForwarding by remember { mutableStateOf(false) }

    fun applyBrightness(value: Float) {
        val win = activity?.window ?: return
        val params = win.attributes
        params.screenBrightness = value.coerceIn(0.01f, 1f)
        win.attributes = params
    }

    LaunchedEffect(isLocked) {
        if (isLocked) {
            showLockHint = true
            delay(3000)
            showLockHint = false
        }
    }

    // Mutable current position within the series — lets "Play Next" and
    // the episode picker advance playback in place without needing the
    // caller/nav graph to re-launch this screen with new args.
    var currentSeasonNum by remember { mutableStateOf(seasonNum) }
    var currentEpNum by remember { mutableStateOf(epNum) }
    var episodesList by remember { mutableStateOf<List<Episode>>(emptyList()) }
    var showEpisodesSheet by remember { mutableStateOf(false) }
    // hasLoadedOnce: lets the load effect skip the redundant full
    // getMovieDetail()/getMovieEpisodes() network round trip on every
    // episode switch — that redundant fetch was the actual cause of
    // "Play Next" feeling unresponsive.
    var hasLoadedOnce by remember { mutableStateOf(false) }
    // isSwitchingEpisode: flips true the instant the user taps Play Next
    // or picks an episode, so there's immediate visual feedback before
    // the (now much shorter) network work even starts.
    var isSwitchingEpisode by remember { mutableStateOf(false) }

    val isPlaying by playerManager.isPlaying.collectAsState()
    val isBuffering by playerManager.isBuffering.collectAsState()
    val isReconnecting by playerManager.isReconnecting.collectAsState()
    val playerError by playerManager.playerError.collectAsState()
    val currentPosMs by playerManager.currentPositionMs.collectAsState()
    val durationMs by playerManager.durationMs.collectAsState()

    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            playerManager.release()
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { win ->
                // -1f (BRIGHTNESS_OVERRIDE_NONE) hands control back to the
                // system default — without this, whatever level the user
                // last dragged to here would stick on every other screen
                // in the app too.
                val params = win.attributes
                params.screenBrightness = -1f
                win.attributes = params
                WindowInsetsControllerCompat(win, win.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    LaunchedEffect(Unit) {
        val win = activity?.window ?: return@LaunchedEffect
        val controller = WindowInsetsControllerCompat(win, win.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    LaunchedEffect(movieId, currentSeasonNum, currentEpNum, localFilePath) {
        loadError = null
        try {
            val apiKey = repository.tokenManager.getApiKey()
            if (apiKey.isNullOrBlank()) {
                loadError = "❌ API key missing. Please enter a valid key in settings."
                Log.e(TAG, "API key is null or empty")
                return@LaunchedEffect
            }

            if (!localFilePath.isNullOrBlank()) {
                movie = Movie(id = movieId, title = "Offline Download")
                playerManager.playMedia(movieId, localFilePath, currentSeasonNum, currentEpNum, initialPosMs, title = "Offline Download")
                hasLoadedOnce = true
                isSwitchingEpisode = false
                return@LaunchedEffect
            }

            val downloadId = if (currentSeasonNum != null && currentEpNum != null) "${movieId}_S${currentSeasonNum}E${currentEpNum}" else movieId
            val downloadedEntity = repository.downloadDao.getDownloadById(downloadId)
            val downloadedFile = downloadedEntity?.localFilePath?.takeIf { it.isNotBlank() }?.let { java.io.File(it) }

            if (downloadedEntity != null && downloadedEntity.status == "COMPLETED" && downloadedFile?.exists() == true) {
                Log.d(TAG, "Playing from local file: ${downloadedEntity.localFilePath}")
                movie = Movie(id = movieId, title = downloadedEntity.title)
                playerManager.playMedia(
                    movieId, downloadedEntity.localFilePath, currentSeasonNum, currentEpNum, initialPosMs,
                    title = downloadedEntity.title,
                    posterUrl = downloadedEntity.posterUrl
                )
                try {
                    repository.getMovieDetail(movieId)?.let { m -> movie = m }
                } catch (e: Exception) {
                    Log.d(TAG, "Metadata refresh skipped (likely offline): ${e.message}")
                }
                hasLoadedOnce = true
                isSwitchingEpisode = false
                return@LaunchedEffect
            }

            // FAST PATH — switching episodes of a series that's already
            // loaded. Skips getMovieDetail()/getMovieEpisodes() entirely
            // (nothing about the movie or episode list changed, only which
            // episode is playing) and goes straight to resolving the new
            // play URL. This is what makes Play Next feel instant instead
            // of re-running the whole initial-load pipeline.
            val alreadyLoadedMovie = movie
            if (hasLoadedOnce && alreadyLoadedMovie != null && alreadyLoadedMovie.id == movieId) {
                Log.d(TAG, "Fast episode switch — skipping detail/episode refetch")
                val url = repository.getPlayUrl(alreadyLoadedMovie, currentSeasonNum, currentEpNum)
                if (url.isBlank()) {
                    loadError = "Play URL is empty – the video may be unavailable."
                    isSwitchingEpisode = false
                    return@LaunchedEffect
                }
                playerManager.playMedia(
                    movieId, url, currentSeasonNum, currentEpNum, 0L, // a newly-selected episode always starts fresh, not at the previous episode's position
                    title = alreadyLoadedMovie.title,
                    posterUrl = alreadyLoadedMovie.cover ?: alreadyLoadedMovie.poster ?: alreadyLoadedMovie.displayPosterUrl
                )
                isSwitchingEpisode = false
                return@LaunchedEffect
            }

            Log.d(TAG, "Fetching movie detail for $movieId")
            val m = repository.getMovieDetail(movieId)
            if (m == null) {
                loadError = "Movie not found. Please check your connection."
                Log.e(TAG, "getMovieDetail returned null")
                isSwitchingEpisode = false
                return@LaunchedEffect
            }
            movie = m

            if (m.isSeries) {
                try {
                    val rawEps = repository.getMovieEpisodes(movieId)
                    episodesList = (if (rawEps.isNotEmpty()) rawEps else m.episodes.orEmpty())
                        .sortedWith(compareBy({ it.sNum ?: 1 }, { it.eNum ?: 1 }))
                } catch (e: Exception) {
                    Log.d(TAG, "Episode list fetch skipped: ${e.message}")
                }
            }

            Log.d(TAG, "Getting play URL")
            val url = repository.getPlayUrl(m, currentSeasonNum, currentEpNum)
            if (url.isBlank()) {
                loadError = "Play URL is empty – the video may be unavailable."
                Log.e(TAG, "Play URL is blank")
                isSwitchingEpisode = false
                return@LaunchedEffect
            }
            Log.d(TAG, "Play URL: $url")

            playerManager.playMedia(
                movieId, url, currentSeasonNum, currentEpNum, initialPosMs,
                title = m.title,
                posterUrl = m.cover ?: m.poster ?: m.displayPosterUrl
            )
            hasLoadedOnce = true
            isSwitchingEpisode = false
        } catch (e: Throwable) {
            Log.e(TAG, "Fatal error while loading movie", e)
            loadError = "Failed to start playback: ${e.message ?: "unknown error"}"
            isSwitchingEpisode = false
        }
    }

    LaunchedEffect(isControlsVisible, isPlaying) {
        if (isControlsVisible && isPlaying) {
            delay(3500)
            isControlsVisible = false
        }
    }

    val displayTitle = movie?.title ?: "YOCINEMA"
    val displaySubtitle = listOfNotNull(
        movie?.genre,
        if (currentSeasonNum != null && currentEpNum != null) "S$currentSeasonNum · E$currentEpNum" else null
    ).joinToString(" · ").ifBlank { null }

    // Next episode in the same season, if one exists — powers "Play Next"
    val nextEpisode = remember(episodesList, currentSeasonNum, currentEpNum) {
        if (currentSeasonNum == null || currentEpNum == null) {
            null
        } else {
            episodesList
                .filter { it.sNum == currentSeasonNum }
                .sortedBy { it.eNum ?: 0 }
                .firstOrNull { (it.eNum ?: 0) > currentEpNum!! }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (loadError == null) {
            AndroidView(
                factory = { ctx ->
                    try {
                        PlayerView(ctx).apply {
                            player = playerManager.exoPlayer
                            useController = false
                            this.resizeMode = resizeMode
                        }
                    } catch (e: Throwable) {
                        Log.e(TAG, "Error creating PlayerView", e)
                        loadError = "Failed to create video surface: ${e.message}"
                        android.widget.FrameLayout(ctx).apply {
                            setBackgroundColor(android.graphics.Color.BLACK)
                        }
                    }
                },
                update = { view ->
                    if (view is PlayerView) {
                        view.resizeMode = resizeMode
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // ─── Gesture zones — left half (brightness + rewind), right half
        // (volume + forward). Placed above the video but below the real
        // control buttons rendered further down, so a tap that lands on an
        // actual button (back, play/pause, seek bar, etc) is consumed by
        // that button first and never reaches these zones underneath.
        //
        // Each zone uses ONE unified gesture detector (playerZoneGestures
        // below) rather than a separate detectTapGestures +
        // detectVerticalDragGestures pair — running two independent
        // detectors on the same touch stream let them race each other:
        // a real double-tap always has tiny jitter between the two taps,
        // and the drag detector could pick that up and consume it before
        // the tap detector saw it, which is what was breaking double-tap
        // (and making it seem like only one direction ever worked).
        if (loadError == null) {
            fun revertFastForwardIfNeeded() {
                if (isFastForwarding) {
                    isFastForwarding = false
                    playerManager.exoPlayer.setPlaybackSpeed(playbackSpeed)
                }
            }

            fun revealLockHint() {
                showLockHint = true
                scope.launch {
                    delay(3000)
                    showLockHint = false
                }
            }

            // Left zone — brightness drag, rewind double-tap
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f)
                    .pointerInput(isLocked) {
                        playerZoneGestures(
                            onSingleTap = {
                                if (isLocked) revealLockHint() else isControlsVisible = !isControlsVisible
                            },
                            onDoubleTap = {
                                if (!isLocked) {
                                    playerManager.exoPlayer.seekTo(
                                        (playerManager.exoPlayer.currentPosition - 10_000).coerceAtLeast(0)
                                    )
                                    seekIndicatorSide = false
                                    scope.launch {
                                        delay(600)
                                        if (seekIndicatorSide == false) seekIndicatorSide = null
                                    }
                                }
                            },
                            onLongPressStart = {
                                if (!isLocked) {
                                    isFastForwarding = true
                                    playerManager.exoPlayer.setPlaybackSpeed(2f)
                                }
                            },
                            onLongPressEnd = { revertFastForwardIfNeeded() },
                            onDragStart = { if (!isLocked) showBrightnessIndicator = true },
                            onVerticalDrag = { deltaY ->
                                if (!isLocked) {
                                    val delta = -deltaY / size.height.toFloat()
                                    brightnessLevel = (brightnessLevel + delta).coerceIn(0f, 1f)
                                    applyBrightness(brightnessLevel)
                                }
                            },
                            onDragEnd = {
                                if (!isLocked) {
                                    scope.launch {
                                        delay(800)
                                        showBrightnessIndicator = false
                                    }
                                }
                            }
                        )
                    }
            ) {
                AnimatedVisibility(
                    visible = seekIndicatorSide == false,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    SeekBumpIndicator(forward = false)
                }
                AnimatedVisibility(
                    visible = showBrightnessIndicator,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    LevelIndicator(
                        level = brightnessLevel,
                        icon = if (brightnessLevel < 0.5f) Icons.Default.BrightnessLow else Icons.Default.BrightnessHigh
                    )
                }
            }

            // Right zone — volume drag, forward double-tap
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f)
                    .pointerInput(isLocked) {
                        playerZoneGestures(
                            onSingleTap = {
                                if (isLocked) revealLockHint() else isControlsVisible = !isControlsVisible
                            },
                            onDoubleTap = {
                                if (!isLocked) {
                                    playerManager.exoPlayer.seekTo(
                                        (playerManager.exoPlayer.currentPosition + 10_000).coerceAtMost(durationMs)
                                    )
                                    seekIndicatorSide = true
                                    scope.launch {
                                        delay(600)
                                        if (seekIndicatorSide == true) seekIndicatorSide = null
                                    }
                                }
                            },
                            onLongPressStart = {
                                if (!isLocked) {
                                    isFastForwarding = true
                                    playerManager.exoPlayer.setPlaybackSpeed(2f)
                                }
                            },
                            onLongPressEnd = { revertFastForwardIfNeeded() },
                            onDragStart = { if (!isLocked) showVolumeIndicator = true },
                            onVerticalDrag = { deltaY ->
                                if (!isLocked) {
                                    val delta = -deltaY / size.height.toFloat()
                                    volumeLevel = (volumeLevel + delta).coerceIn(0f, 1f)
                                    audioManager.setStreamVolume(
                                        AudioManager.STREAM_MUSIC,
                                        (volumeLevel * maxVolume).toInt(),
                                        0
                                    )
                                }
                            },
                            onDragEnd = {
                                if (!isLocked) {
                                    scope.launch {
                                        delay(800)
                                        showVolumeIndicator = false
                                    }
                                }
                            }
                        )
                    }
            ) {
                AnimatedVisibility(
                    visible = seekIndicatorSide == true,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    SeekBumpIndicator(forward = true)
                }
                AnimatedVisibility(
                    visible = showVolumeIndicator,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    LevelIndicator(
                        level = volumeLevel,
                        icon = when {
                            volumeLevel <= 0f -> Icons.Default.VolumeOff
                            volumeLevel < 0.5f -> Icons.Default.VolumeDown
                            else -> Icons.Default.VolumeUp
                        }
                    )
                }
            }

            // Hold-for-2x indicator — centered near the top, visible from
            // either zone since long-press is symmetric on both sides.
            AnimatedVisibility(
                visible = isFastForwarding,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 90.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.FastForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("2x speed", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (loadError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text("⚠️ Error", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        loadError!!,
                        color = YoTextMuted,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
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
        }

        if (loadError == null && playerError != null && !isReconnecting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Playback Error", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(playerError ?: "Failed to load stream.", color = YoTextMuted, fontSize = 13.sp, maxLines = 2)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { playerManager.attemptReconnect() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry")
                    }
                }
            }
        } else if (loadError == null && isReconnecting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SpinningLoader()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Reconnecting…", color = Color.White, fontSize = 14.sp)
                }
            }
        }

        if (loadError == null) {
            AnimatedVisibility(
                visible = isControlsVisible && !isLocked,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)))
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ControlIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", onClick = onBackClick)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(displayTitle, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (displaySubtitle != null) {
                                Text(displaySubtitle, color = YoTextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))

                        if (movie?.isSeries == true && episodesList.isNotEmpty()) {
                            ControlIconButton(
                                icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                                contentDescription = "Episodes",
                                onClick = { showEpisodesSheet = !showEpisodesSheet }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .clickable { showSpeedMenu = true }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (playbackSpeed == 1f) "Normal" else "${playbackSpeed}x",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            DropdownMenu(expanded = showSpeedMenu, onDismissRequest = { showSpeedMenu = false }) {
                                listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                                    DropdownMenuItem(
                                        text = { Text(if (speed == 1f) "Normal" else "${speed}x") },
                                        onClick = {
                                            playbackSpeed = speed
                                            playerManager.exoPlayer.setPlaybackSpeed(speed)
                                            showSpeedMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Center transport controls
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(40.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ControlIconButton(
                            icon = Icons.Default.Replay10,
                            contentDescription = "Rewind 10s",
                            size = 52.dp,
                            onClick = { playerManager.exoPlayer.seekTo((playerManager.exoPlayer.currentPosition - 10_000).coerceAtLeast(0)) }
                        )

                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .shadow(12.dp, CircleShape, clip = false)
                                .clip(CircleShape)
                                .background(YoPrimaryViolet)
                                .clickable(enabled = !isBuffering) {
                                    if (isPlaying) playerManager.exoPlayer.pause() else playerManager.exoPlayer.play()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isBuffering) {
                                SpinningLoader(color = YoBaseBackground, size = 32.dp)
                            } else {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = YoBaseBackground,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        ControlIconButton(
                            icon = Icons.Default.Forward10,
                            contentDescription = "Forward 10s",
                            size = 52.dp,
                            onClick = { playerManager.exoPlayer.seekTo((playerManager.exoPlayer.currentPosition + 10_000).coerceAtMost(durationMs)) }
                        )

                        if (nextEpisode != null) {
                            ControlIconButton(
                                icon = Icons.Default.SkipNext,
                                contentDescription = "Play Next Episode",
                                size = 52.dp,
                                onClick = {
                                    if (!isSwitchingEpisode) {
                                        isSwitchingEpisode = true
                                        currentSeasonNum = nextEpisode.sNum
                                        currentEpNum = nextEpisode.eNum
                                    }
                                }
                            )
                        }
                    }

                    // Bottom scrim + seek bar
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        val displayPos = if (isDragging) dragPositionMs.toLong() else currentPosMs
                        Slider(
                            value = displayPos.toFloat(),
                            valueRange = 0f..(durationMs.coerceAtLeast(1L)).toFloat(),
                            onValueChange = {
                                isDragging = true
                                dragPositionMs = it
                            },
                            onValueChangeFinished = {
                                playerManager.exoPlayer.seekTo(dragPositionMs.toLong())
                                isDragging = false
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = YoPrimaryViolet,
                                activeTrackColor = YoPrimaryViolet,
                                inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.fillMaxWidth().height(28.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${formatTime(displayPos)} / ${formatTime(durationMs)}", color = Color.White, fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ControlIconButton(
                                    icon = Icons.Default.LockOpen,
                                    contentDescription = "Lock",
                                    size = 40.dp,
                                    onClick = {
                                        isLocked = true
                                        // Hides the normal controls the same
                                        // instant — the AnimatedVisibility
                                        // gate below (isControlsVisible &&
                                        // !isLocked) picks this up immediately
                                        // rather than waiting for the
                                        // auto-hide timer.
                                    }
                                )
                                ControlIconButton(
                                    icon = Icons.Default.AspectRatio,
                                    contentDescription = "Resize",
                                    size = 40.dp,
                                    onClick = {
                                        resizeMode = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT)
                                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                        else AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Unlock affordance — reachable while locked, but now behaves
            // like the normal controls: fades out a few seconds after
            // locking, and a tap anywhere in either gesture zone (see
            // revealLockHint above) brings it back rather than leaving it
            // permanently on screen.
            AnimatedVisibility(
                visible = isLocked && showLockHint,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .size(48.dp)
                        .shadow(6.dp, CircleShape, clip = false)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable { isLocked = false },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Unlock",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Immediate acknowledgement for Play Next / episode picks — shows
        // the instant isSwitchingEpisode flips true (same frame as the
        // tap), well before the new stream is actually ready. This is
        // what makes the switch feel instant instead of "did that even
        // register?"
        if (isSwitchingEpisode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = YoPrimaryViolet,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        // Episode panel — docked to the right edge like a TV-style side
        // rail, not a bottom sheet. Slides in over the video, current
        // episode highlighted, thumbnail + episode number + title per row.
        AnimatedVisibility(
            visible = showEpisodesSheet,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(360.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.92f))
                        )
                    )
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        // Tapping the panel (anywhere that isn't an episode
                        // row, which has its own clickable and consumes the
                        // tap first) now dismisses it — same "tap to
                        // dismiss" behavior as the video controls, instead
                        // of just swallowing the tap and leaving the panel
                        // stuck open with no way to close it.
                        onClick = { showEpisodesSheet = false }
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 20.dp, bottom = 20.dp, start = 20.dp, end = 16.dp)
                ) {
                    Text(
                        text = "Episodes",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(episodesList) { ep ->
                            val isCurrent = ep.sNum == currentSeasonNum && ep.eNum == currentEpNum
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isCurrent) YoPrimaryViolet.copy(alpha = 0.22f) else Color.Transparent)
                                    .clickable {
                                        if (!isSwitchingEpisode) {
                                            isSwitchingEpisode = true
                                            showEpisodesSheet = false
                                            currentSeasonNum = ep.sNum
                                            currentEpNum = ep.eNum
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(96.dp)
                                        .aspectRatio(16f / 9f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                ) {
                                    SubcomposeAsyncImage(
                                        model = ep.getDisplayStill(movieId),
                                        contentDescription = ep.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        loading = { YoCinemaLogoPlaceholder() },
                                        error = { YoCinemaLogoPlaceholder() }
                                    )
                                    if (isCurrent) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.35f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isCurrent) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = YoPrimaryViolet,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = "Episode ${ep.eNum}",
                                            color = if (isCurrent) YoPrimaryViolet else YoTextMuted,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = ep.title ?: "Episode ${ep.eNum}",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
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

@Composable
private fun ControlIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    size: androidx.compose.ui.unit.Dp = 44.dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(size * 0.5f))
    }
}

/**
 * Single unified gesture recognizer for one player zone — tap, double-tap,
 * long-press(-and-release), and vertical drag, all arbitrated from ONE
 * pointer event stream instead of two independent detectors racing each
 * other. That race was the actual cause of double-tap becoming unreliable
 * (a real double-tap always has a little jitter between taps, and a
 * separate drag detector watching the same stream could consume that
 * jitter as the start of a drag before the tap detector ever saw it).
 *
 * Per gesture: wait for the first down, then watch for up to
 * [longPressTimeoutMs] for either (a) enough vertical movement to count as
 * a drag, or (b) release. If neither happens before the timeout, it's a
 * long-press. If released quickly with no real movement, wait briefly for
 * a second down to decide tap vs double-tap.
 */
private suspend fun PointerInputScope.playerZoneGestures(
    onSingleTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onLongPressStart: () -> Unit,
    onLongPressEnd: () -> Unit,
    onDragStart: () -> Unit,
    onVerticalDrag: (deltaY: Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val longPressTimeoutMs = viewConfiguration.longPressTimeoutMillis
    val touchSlop = viewConfiguration.touchSlop
    val doubleTapTimeoutMs = 300L

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val pointerId = down.id
        val startPosition = down.position
        var isDrag = false
        var timedOut = false

        val completed = withTimeoutOrNull(longPressTimeoutMs) {
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == pointerId }
                if (change == null || !change.pressed) return@withTimeoutOrNull
                // Cumulative displacement from where the finger first went
                // down — NOT the delta since the previous event. Consecutive
                // touch samples during a real drag are usually only a few
                // pixels apart, far under the slop threshold individually,
                // so comparing each event's own tiny delta against slop
                // almost never triggered — this is why the indicator could
                // appear (drag got detected eventually, rarely) but then
                // seemed to just sit there not responding to the rest of
                // the drag. Comparing against the ORIGINAL down position
                // is the correct way to detect "has the finger moved far
                // enough to count as a drag yet".
                val totalDeltaY = change.position.y - startPosition.y
                if (kotlin.math.abs(totalDeltaY) > touchSlop) {
                    isDrag = true
                    change.consume()
                    return@withTimeoutOrNull
                }
            }
            @Suppress("UNREACHABLE_CODE")
            Unit
        }
        if (completed == null) timedOut = true

        when {
            isDrag -> {
                onDragStart()
                // Once actively dragging, per-event positionChange() (the
                // delta since the LAST event, not since down) is exactly
                // what's wanted here — it's what makes each incremental
                // movement translate into a smooth, continuously updating
                // level rather than one single jump.
                drag(pointerId) { change ->
                    change.consume()
                    onVerticalDrag(change.positionChange().y)
                }
                onDragEnd()
            }

            timedOut -> {
                // Timed out while still held with no real movement — long press.
                onLongPressStart()
                waitForUpOrCancellation()
                onLongPressEnd()
            }

            else -> {
                // Released quickly without dragging — tap candidate. Wait
                // briefly for a second down to decide tap vs double-tap.
                val secondDown = withTimeoutOrNull(doubleTapTimeoutMs) {
                    awaitFirstDown(requireUnconsumed = false)
                }
                if (secondDown != null) {
                    waitForUpOrCancellation()
                    onDoubleTap()
                } else {
                    onSingleTap()
                }
            }
        }
    }
}

/** Brief "-10s"/"+10s" bump shown centered in whichever half of the screen was double-tapped. */
@Composable
private fun SeekBumpIndicator(forward: Boolean) {
    Column(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (forward) Icons.Default.FastForward else Icons.Default.FastRewind,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(30.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (forward) "+10s" else "-10s",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Vertical pill showing the current volume/brightness level while dragging — same shape used for both. */
@Composable
private fun LevelIndicator(level: Float, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        modifier = Modifier
            .width(56.dp)
            .height(150.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(vertical = 14.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))

        Box(
            modifier = Modifier
                .width(6.dp)
                .weight(1f)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.25f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(level.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(3.dp))
                    .background(YoPrimaryViolet)
            )
        }

        Text(
            text = "${(level * 100).toInt()}%",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SpinningLoader(color: Color = YoPrimaryViolet, size: androidx.compose.ui.unit.Dp = 36.dp) {
    androidx.compose.material3.CircularProgressIndicator(
        modifier = Modifier.size(size),
        color = color,
        strokeWidth = 3.dp
    )
}