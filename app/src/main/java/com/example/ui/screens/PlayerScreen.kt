package com.example.ui.screens

import android.app.Activity
import android.util.Log
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
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
import com.example.ui.theme.YoPrimaryAmber
import com.example.ui.theme.YoTextMuted
import kotlinx.coroutines.delay

private const val TAG = "PlayerScreen"

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    movieId: String,
    seasonNum: Int?,
    epNum: Int?,
    localFilePath: String? = null,
    directStreamUrl: String? = null,  // for trailers or direct video playback
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

    val isPlaying by playerManager.isPlaying.collectAsState()
    val isBuffering by playerManager.isBuffering.collectAsState()
    val isReconnecting by playerManager.isReconnecting.collectAsState()
    val playerError by playerManager.playerError.collectAsState()
    val currentPosMs by playerManager.currentPositionMs.collectAsState()
    val durationMs by playerManager.durationMs.collectAsState()

    // Keep screen on (orientation is forced by manifest)
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            playerManager.release()
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.window?.let { win ->
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

    // Load movie and start playback
    LaunchedEffect(movieId, seasonNum, epNum, localFilePath, directStreamUrl) {
        loadError = null
        try {
            // 1) Check API key
            val apiKey = repository.tokenManager.getApiKey()
            if (apiKey.isNullOrBlank()) {
                loadError = "❌ API key missing. Please enter a valid key in settings."
                Log.e(TAG, "API key is null or empty")
                return@LaunchedEffect
            }

            // 2) Direct stream (e.g., trailer)
            if (!directStreamUrl.isNullOrBlank()) {
                // Use a dummy movie for title/poster
                movie = Movie(id = movieId, title = "Trailer")
                playerManager.playMedia(
                    movieId, directStreamUrl, seasonNum, epNum, initialPosMs,
                    title = movie?.title ?: "Trailer",
                    posterUrl = movie?.cover ?: movie?.poster ?: movie?.displayPosterUrl
                )
                return@LaunchedEffect
            }

            // 3) Offline / downloaded file
            if (!localFilePath.isNullOrBlank()) {
                movie = Movie(id = movieId, title = "Offline Download")
                playerManager.playMedia(movieId, localFilePath, seasonNum, epNum, initialPosMs, title = "Offline Download")
                return@LaunchedEffect
            }

            val downloadId = if (seasonNum != null && epNum != null) "${movieId}_S${seasonNum}E${epNum}" else movieId
            val downloadedEntity = repository.downloadDao.getDownloadById(downloadId)
            val downloadedFile = downloadedEntity?.localFilePath?.takeIf { it.isNotBlank() }?.let { java.io.File(it) }

            if (downloadedEntity != null && downloadedEntity.status == "COMPLETED" && downloadedFile?.exists() == true) {
                Log.d(TAG, "Playing from local file: ${downloadedEntity.localFilePath}")
                val m = repository.getMovieDetail(movieId)
                movie = m ?: Movie(id = movieId, title = downloadedEntity.title)
                playerManager.playMedia(
                    movieId, downloadedEntity.localFilePath, seasonNum, epNum, initialPosMs,
                    title = movie?.title ?: downloadedEntity.title,
                    posterUrl = movie?.cover ?: movie?.poster ?: movie?.displayPosterUrl
                )
                return@LaunchedEffect
            }

            // 4) Online stream
            Log.d(TAG, "Fetching movie detail for $movieId")
            val m = repository.getMovieDetail(movieId)
            if (m == null) {
                loadError = "Movie not found. Please check your connection."
                Log.e(TAG, "getMovieDetail returned null")
                return@LaunchedEffect
            }
            movie = m

            Log.d(TAG, "Getting play URL")
            val url = repository.getPlayUrl(m, seasonNum, epNum)
            if (url.isBlank()) {
                loadError = "Play URL is empty – the video may be unavailable."
                Log.e(TAG, "Play URL is blank")
                return@LaunchedEffect
            }
            Log.d(TAG, "Play URL: $url")

            playerManager.playMedia(
                movieId, url, seasonNum, epNum, initialPosMs,
                title = m.title,
                posterUrl = m.cover ?: m.poster ?: m.displayPosterUrl
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Fatal error while loading movie", e)
            loadError = "Failed to start playback: ${e.message ?: "unknown error"}"
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
        if (seasonNum != null && epNum != null) "S$seasonNum · E$epNum" else null
    ).joinToString(" · ").ifBlank { null }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { isControlsVisible = !isControlsVisible })
            }
    ) {
        // Video view
        if (loadError == null) {
            AndroidView(
                factory = { ctx ->
                    try {
                        PlayerView(ctx).apply {
                            player = playerManager.exoPlayer
                            useController = false
                            this.resizeMode = resizeMode
                            // Use TextureView for better PiP behaviour (doesn't destroy surface)
                            setUseTextureView(true)
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

        // Load error overlay
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
                            containerColor = YoPrimaryAmber,
                            contentColor = YoBaseBackground
                        )
                    ) {
                        Text("Go Back")
                    }
                }
            }
        }

        // PlayerManager error / reconnecting
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
                    Text(playerError ?: "Failed to load stream.", color = YoTextMuted, fontSize = 12.sp, maxLines = 2)
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(onClick = { playerManager.attemptReconnect() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
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
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Reconnecting…", color = Color.White, fontSize = 13.sp)
                }
            }
        }

        // Controls (only if no load error)
        if (loadError == null) {
            AnimatedVisibility(
                visible = isControlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Top scrim + bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)))
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ControlIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", onClick = onBackClick)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(displayTitle, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (displaySubtitle != null) {
                                Text(displaySubtitle, color = YoTextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .clickable { showSpeedMenu = true }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (playbackSpeed == 1f) "Normal" else "${playbackSpeed}x",
                                    color = Color.White,
                                    fontSize = 12.sp,
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
                        horizontalArrangement = Arrangement.spacedBy(36.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ControlIconButton(
                            icon = Icons.Default.Replay10,
                            contentDescription = "Rewind 10s",
                            size = 46.dp,
                            onClick = { playerManager.exoPlayer.seekTo((playerManager.exoPlayer.currentPosition - 10_000).coerceAtLeast(0)) }
                        )

                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .shadow(elevation = 8.dp, shape = CircleShape, clip = false)
                                .clip(CircleShape)
                                .background(YoPrimaryAmber)
                                .clickable(enabled = !isBuffering) {
                                    if (isPlaying) playerManager.exoPlayer.pause() else playerManager.exoPlayer.play()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isBuffering) {
                                SpinningLoader(color = YoBaseBackground, size = 28.dp)
                            } else {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = YoBaseBackground,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        ControlIconButton(
                            icon = Icons.Default.Forward10,
                            contentDescription = "Forward 10s",
                            size = 46.dp,
                            onClick = { playerManager.exoPlayer.seekTo((playerManager.exoPlayer.currentPosition + 10_000).coerceAtMost(durationMs)) }
                        )
                    }

                    // Bottom scrim + seek bar
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
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
                                thumbColor = YoPrimaryAmber,
                                activeTrackColor = YoPrimaryAmber,
                                inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.fillMaxWidth().height(24.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${formatTime(displayPos)} / ${formatTime(durationMs)}", color = Color.White, fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                ControlIconButton(
                                    icon = Icons.Default.AspectRatio,
                                    contentDescription = "Resize",
                                    size = 36.dp,
                                    onClick = {
                                        resizeMode = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT)
                                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                        else AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    }
                                )
                                ControlIconButton(
                                    icon = Icons.Default.PictureInPicture,
                                    contentDescription = "Picture in picture",
                                    size = 36.dp,
                                    onClick = { activity?.enterPictureInPictureModeSafely() }
                                )
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
    size: androidx.compose.ui.unit.Dp = 40.dp,
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

@Composable
private fun SpinningLoader(color: Color = YoPrimaryAmber, size: androidx.compose.ui.unit.Dp = 32.dp) {
    androidx.compose.material3.CircularProgressIndicator(
        modifier = Modifier.size(size),
        color = color,
        strokeWidth = 3.dp
    )
}

private fun Activity.enterPictureInPictureModeSafely() {
    try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                enterPictureInPictureMode(android.app.PictureInPictureParams.Builder().build())
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}