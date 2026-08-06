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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import com.example.ui.components.YoCinemaLogoPlaceholder
import com.example.data.model.Movie
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

    var movie by remember { mutableStateOf<Movie?>(null) }
    var mediaUrl by remember { mutableStateOf("") }
    var isFullscreen by remember { mutableStateOf(false) }
    var isControlsVisible by remember { mutableStateOf(true) }

    val playerManager = remember { PlayerManager(context, repository, scope) }
    val isPlaying by playerManager.isPlaying.collectAsState()
    val currentPosMs by playerManager.currentPositionMs.collectAsState()
    val durationMs by playerManager.durationMs.collectAsState()
    val isReconnecting by playerManager.isReconnecting.collectAsState()

    var gestureOverlayText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(movieId, seasonNum, epNum, localFilePath) {
        if (!localFilePath.isNull_orBlank()) {
            movie = Movie(id = movieId, title = "Offline Download")
            mediaUrl = localFilePath!!
            playerManager.playMedia(movieId, localFilePath!!, seasonNum, epNum, initialPosMs)
        } else {
            val m = repository.getMovieDetail(movieId)
            movie = m
            if (m != null) {
                val url = repository.getPlayUrl(m, seasonNum, epNum)
                mediaUrl = url
                playerManager.playMedia(movieId, url, seasonNum, epNum, initialPosMs)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            playerManager.release()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
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
            // Portrait View: Video at top + PERSISTENT controls BELOW video
            Column(modifier = Modifier.fillMaxSize()) {
                // Video Player Container (Fixed 16:9 Aspect Ratio)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = playerManager.exoPlayer
                                useController = false // Custom Controls Below
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

                    // Reconnecting / Loading Indicator Overlay
                    if (isReconnecting) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                ModernLoader(size = 40.dp)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Reconnecting stream...",
                                    color = YoPrimaryAmber,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Top Left Back Button
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = YoTextPrimary
                        )
                    }

                    // Top Right Fullscreen Button
                    IconButton(
                        onClick = {
                            isFullscreen = true
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Fullscreen",
                            tint = YoTextPrimary
                        )
                    }
                }

                // PERSISTENT CONTROLS DIRECTLY BELOW THE VIDEO (Never Overlaying!)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(YoSurface)
                        .padding(16.dp)
                ) {
                    // Movie Title + VJ Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = movie?.title ?: "Loading...",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = YoTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (seasonNum != null && epNum != null) {
                                Text(
                                    text = "Season $seasonNum · Episode $epNum",
                                    fontSize = 12.sp,
                                    color = YoPrimaryAmber
                                )
                            }
                        }

                        if (!movie?.vjName.isNull_orBlank()) {
                            VJBadgeChip(vjName = movie!!.vjName!!)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Seek Slider & Time Display
                    Column {
                        Slider(
                            value = currentPosMs.toFloat(),
                            onValueChange = { playerManager.exoPlayer.seekTo(it.toLong()) },
                            valueRange = 0f..(durationMs.coerceAtLeast(1L).toFloat()),
                            colors = SliderDefaults.colors(
                                thumbColor = YoPrimaryAmber,
                                activeTrackColor = YoPrimaryAmber,
                                inactiveTrackColor = YoBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatMs(currentPosMs),
                                fontSize = 12.sp,
                                color = YoTextMuted
                            )
                            Text(
                                text = formatMs(durationMs),
                                fontSize = 12.sp,
                                color = YoTextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary Playback Buttons (±10s, Play/Pause)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val newPos = (currentPosMs - 10000L).coerceAtLeast(0L)
                                playerManager.exoPlayer.seekTo(newPos)
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastRewind,
                                contentDescription = "-10s",
                                tint = YoTextPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        Box(
                            modifier = Modifier
                                .size(56.dp)
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
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        IconButton(
                            onClick = {
                                val newPos = (currentPosMs + 10000L).coerceAtMost(durationMs)
                                playerManager.exoPlayer.seekTo(newPos)
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = "+10s",
                                tint = YoTextPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Secondary Action Row (PiP, Subtitles, Fullscreen)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    activity?.enterPictureInPictureMode()
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Default.PictureInPicture, contentDescription = "PiP", tint = YoTextMuted)
                        }

                        IconButton(onClick = { /* Toggle Subtitles */ }) {
                            Icon(imageVector = Icons.Default.ClosedCaption, contentDescription = "Subtitles", tint = YoTextMuted)
                        }

                        IconButton(
                            onClick = {
                                isFullscreen = true
                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = YoPrimaryAmber)
                        }
                    }
                }
            }
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
                        .size(64.dp)
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
