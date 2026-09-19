package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.HighQuality
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
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
import coil.compose.SubcomposeAsyncImage
import com.example.player.PlayerCaption
import com.example.player.PlayerManager
import com.example.player.PlayerQuality
import com.example.repository.YocinemaRepository
import com.example.ui.components.YoCinemaLogoPlaceholder
import com.example.ui.theme.YoBaseBackground
import com.example.ui.theme.YoPrimaryViolet
import com.example.ui.theme.YoTextMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "UnifiedPlayer"

data class PlayerEpisode(
    val season: Int?,
    val episode: Int?,
    val title: String?,
    val stillUrl: String?,
    val streamUrl: String,
)

data class UnifiedPlayerSpec(
    val mediaId: String,
    val title: String,
    val subtitle: String? = null,
    val posterUrl: String? = null,
    val qualities: List<PlayerQuality> = emptyList(),
    val captions: List<PlayerCaption> = emptyList(),
    val episodes: List<PlayerEpisode> = emptyList(),
    val currentSeason: Int? = null,
    val currentEpisode: Int? = null,
    val onEpisodeSelected: ((PlayerEpisode) -> Unit)? = null,
    val isSportsLive: Boolean = false,
)

@OptIn(UnstableApi::class)
@Composable
fun UnifiedPlayerScreen(
    spec: UnifiedPlayerSpec,
    streamUrl: String,
    repository: YocinemaRepository,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activity = context as? Activity

    val playerManager = remember { PlayerManager(context, repository, scope) }

    var isControlsVisible by remember { mutableStateOf(true) }
    var isDragging by remember { mutableStateOf(false) }
    var dragPositionMs by remember { mutableFloatStateOf(0f) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showQualityMenu by remember { mutableStateOf(false) }
    var showCaptionMenu by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var isLocked by remember { mutableStateOf(false) }
    var showLockHint by remember { mutableStateOf(true) }
    var showEpisodesPanel by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }

    val audioManager = remember {
        context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager
    }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var volumeLevel by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume)
    }
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
    var seekIndicatorSide by remember { mutableStateOf<Boolean?>(null) }
    var isFastForwarding by remember { mutableStateOf(false) }

    val isPlaying by playerManager.isPlaying.collectAsState()
    val isBuffering by playerManager.isBuffering.collectAsState()
    val isReconnecting by playerManager.isReconnecting.collectAsState()
    val playerError by playerManager.playerError.collectAsState()
    val currentPosMs by playerManager.currentPositionMs.collectAsState()
    val durationMs by playerManager.durationMs.collectAsState()
    val qualities by playerManager.qualities.collectAsState()
    val captions by playerManager.captions.collectAsState()
    val selectedQuality by playerManager.selectedQuality.collectAsState()
    val selectedCaptionLang by playerManager.selectedCaptionLang.collectAsState()

    val subtitleDelayMs by playerManager.subtitleDelayMs.collectAsState()
    val activeCues by playerManager.activeCues.collectAsState()

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

    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            playerManager.release()
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { win ->
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

    LaunchedEffect(spec.mediaId, streamUrl) {
        loadError = null
        try {
            if (streamUrl.isBlank()) {
                loadError = "No playable source"
                return@LaunchedEffect
            }
            playerManager.setQualities(spec.qualities, spec.qualities.firstOrNull())
            playerManager.setCaptions(spec.captions)
            playerManager.playMedia(
                movieId = spec.mediaId,
                mediaUrl = streamUrl,
                seasonNum = spec.currentSeason,
                epNum = spec.currentEpisode,
                initialPositionMs = 0L,
                title = spec.title,
                posterUrl = spec.posterUrl,
                isSportsLive = spec.isSportsLive,
            )
        } catch (e: Throwable) {
            Log.e(TAG, "load failed", e)
            loadError = e.message ?: "Couldn't start playback"
        }
    }

    LaunchedEffect(isControlsVisible, isPlaying) {
        if (isControlsVisible && isPlaying) {
            delay(3500)
            isControlsVisible = false
        }
    }

    val subtitleBottomPadding by animateDpAsState(
        targetValue = if (isControlsVisible) 130.dp else 60.dp,
        label = "subtitleBottomPadding",
    )

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
                            subtitleView?.let { it.alpha = 0f }
                        }
                    } catch (e: Throwable) {
                        Log.e(TAG, "PlayerView failed", e)
                        android.widget.FrameLayout(ctx).apply {
                            setBackgroundColor(android.graphics.Color.BLACK)
                        }
                    }
                },
                update = { view ->
                    if (view is PlayerView) {
                        view.resizeMode = resizeMode
                        view.subtitleView?.let { it.alpha = 0f }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (loadError == null) {
            fun revertFastForwardIfNeeded() {
                if (isFastForwarding) {
                    isFastForwarding = false
                    playerManager.exoPlayer.setPlaybackSpeed(playbackSpeed)
                }
            }
            fun revealLockHint() {
                showLockHint = true
                scope.launch { delay(3000); showLockHint = false }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f)
                    .pointerInput(isLocked) {
                        playerZoneGestures(
                            onSingleTap = { if (isLocked) revealLockHint() else isControlsVisible = !isControlsVisible },
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
                                    scope.launch { delay(800); showBrightnessIndicator = false }
                                }
                            }
                        )
                    }
            ) {
                AnimatedVisibility(
                    visible = seekIndicatorSide == false,
                    enter = fadeIn(), exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) { SeekBumpIndicator(forward = false) }
                AnimatedVisibility(
                    visible = showBrightnessIndicator,
                    enter = fadeIn(), exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    LevelIndicator(
                        level = brightnessLevel,
                        icon = if (brightnessLevel < 0.5f) Icons.Default.BrightnessLow else Icons.Default.BrightnessHigh
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f)
                    .pointerInput(isLocked) {
                        playerZoneGestures(
                            onSingleTap = { if (isLocked) revealLockHint() else isControlsVisible = !isControlsVisible },
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
                                    scope.launch { delay(800); showVolumeIndicator = false }
                                }
                            }
                        )
                    }
            ) {
                AnimatedVisibility(
                    visible = seekIndicatorSide == true,
                    enter = fadeIn(), exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) { SeekBumpIndicator(forward = true) }
                AnimatedVisibility(
                    visible = showVolumeIndicator,
                    enter = fadeIn(), exit = fadeOut(),
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

            AnimatedVisibility(
                visible = isFastForwarding,
                enter = fadeIn(), exit = fadeOut(),
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

        if (loadError == null && activeCues.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        bottom = subtitleBottomPadding,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = activeCues.joinToString("\n"),
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        if (loadError != null) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Text("⚠️ Error", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(loadError!!, color = YoTextMuted, fontSize = 15.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onBackClick,
                        colors = ButtonDefaults.buttonColors(containerColor = YoPrimaryViolet, contentColor = YoBaseBackground)
                    ) { Text("Go Back") }
                }
            }
        }

        if (loadError == null && playerError != null && !isReconnecting) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Playback Error", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(playerError ?: "Failed to load stream.", color = YoTextMuted, fontSize = 13.sp, maxLines = 2)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { playerManager.attemptReconnect() }) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry")
                    }
                }
            }
        } else if (loadError == null && isReconnecting) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = YoPrimaryViolet, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Reconnecting…", color = Color.White, fontSize = 14.sp)
                }
            }
        }

        if (loadError == null) {
            AnimatedVisibility(
                visible = isControlsVisible && !isLocked,
                enter = fadeIn(), exit = fadeOut(),
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
                            Text(spec.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (!spec.subtitle.isNullOrBlank()) {
                                Text(spec.subtitle, color = YoTextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))

                        if (spec.episodes.isNotEmpty() && spec.onEpisodeSelected != null) {
                            ControlIconButton(
                                icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                                contentDescription = "Episodes",
                                onClick = { showEpisodesPanel = !showEpisodesPanel }
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
                                    color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium
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

                        if (qualities.size > 1) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box {
                                ControlIconButton(
                                    icon = Icons.Default.HighQuality,
                                    contentDescription = "Quality",
                                    onClick = { showQualityMenu = true }
                                )
                                DropdownMenu(expanded = showQualityMenu, onDismissRequest = { showQualityMenu = false }) {
                                    qualities.forEach { q ->
                                        val isSel = q.url == selectedQuality?.url
                                        DropdownMenuItem(
                                            text = { Text(q.label, color = if (isSel) YoPrimaryViolet else Color.White) },
                                            onClick = { playerManager.switchQuality(q); showQualityMenu = false }
                                        )
                                    }
                                }
                            }
                        }

                        if (captions.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box {
                                ControlIconButton(
                                    icon = Icons.Default.ClosedCaption,
                                    contentDescription = "Captions",
                                    onClick = { showCaptionMenu = true }
                                )
                                DropdownMenu(expanded = showCaptionMenu, onDismissRequest = { showCaptionMenu = false }) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    ) {
                                        Text(
                                            text = "Subtitle delay: ${"%.1f".format(subtitleDelayMs / 1000.0)}s",
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontWeight = FontWeight.Medium,
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            DelayPill(
                                                label = "−0.5s",
                                                enabled = subtitleDelayMs > 0L,
                                            ) {
                                                playerManager.setSubtitleDelay(subtitleDelayMs - 500L)
                                            }
                                            DelayPill(
                                                label = "+0.5s",
                                                enabled = subtitleDelayMs < 15_000L,
                                            ) {
                                                playerManager.setSubtitleDelay(subtitleDelayMs + 500L)
                                            }
                                            if (subtitleDelayMs != 0L) {
                                                DelayPill(label = "Reset", enabled = true) {
                                                    playerManager.setSubtitleDelay(0L)
                                                }
                                            }
                                        }
                                    }
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Off",
                                                color = if (selectedCaptionLang == "off") YoPrimaryViolet else Color.White
                                            )
                                        },
                                        onClick = { playerManager.switchCaption("off"); showCaptionMenu = false }
                                    )
                                    captions.forEach { cap ->
                                        val lang = cap.language ?: return@forEach
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    cap.displayName ?: lang,
                                                    color = if (selectedCaptionLang == lang) YoPrimaryViolet else Color.White
                                                )
                                            },
                                            onClick = { playerManager.switchCaption(lang); showCaptionMenu = false }
                                        )
                                    }
                                }
                            }
                        }
                    }

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
                                CircularProgressIndicator(color = YoBaseBackground, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
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

                        val nextEp = spec.episodes
                            .filter { it.season == spec.currentSeason }
                            .sortedBy { it.episode ?: 0 }
                            .firstOrNull { (it.episode ?: 0) > (spec.currentEpisode ?: 0) }
                        if (nextEp != null && spec.onEpisodeSelected != null) {
                            ControlIconButton(
                                icon = Icons.Default.SkipNext,
                                contentDescription = "Play Next",
                                size = 52.dp,
                                onClick = { spec.onEpisodeSelected.invoke(nextEp) }
                            )
                        }
                    }

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
                            onValueChange = { isDragging = true; dragPositionMs = it },
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
                                    onClick = { isLocked = true }
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

            AnimatedVisibility(
                visible = isLocked && showLockHint,
                enter = fadeIn(), exit = fadeOut(),
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
                    Icon(Icons.Default.Lock, contentDescription = "Unlock", tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
        }

        AnimatedVisibility(
            visible = showEpisodesPanel,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(360.dp)
                    .background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.92f))))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = { showEpisodesPanel = false }
                    )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(top = 20.dp, bottom = 20.dp, start = 20.dp, end = 16.dp)
                ) {
                    Text("Episodes", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(spec.episodes) { ep ->
                            val isCurrent = ep.season == spec.currentSeason && ep.episode == spec.currentEpisode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isCurrent) YoPrimaryViolet.copy(alpha = 0.22f) else Color.Transparent)
                                    .clickable {
                                        showEpisodesPanel = false
                                        spec.onEpisodeSelected?.invoke(ep)
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
                                    if (!ep.stillUrl.isNullOrBlank()) {
                                        SubcomposeAsyncImage(
                                            model = ep.stillUrl,
                                            contentDescription = ep.title,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop,
                                            loading = { YoCinemaLogoPlaceholder() },
                                            error = { YoCinemaLogoPlaceholder() }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Episode ${ep.episode}",
                                        color = if (isCurrent) YoPrimaryViolet else YoTextMuted,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = ep.title ?: "Episode ${ep.episode}",
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

@Composable
private fun DelayPill(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (enabled) Color.White.copy(alpha = 0.14f)
                else Color.White.copy(alpha = 0.05f)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.35f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

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
                val totalDeltaY = change.position.y - startPosition.y
                if (kotlin.math.abs(totalDeltaY) > touchSlop) {
                    isDrag = true
                    change.consume()
                    return@withTimeoutOrNull
                }
            }
            @Suppress("UNREACHABLE_CODE") Unit
        }
        if (completed == null) timedOut = true

        when {
            isDrag -> {
                onDragStart()
                drag(pointerId) { change ->
                    change.consume()
                    onVerticalDrag(change.positionChange().y)
                }
                onDragEnd()
            }
            timedOut -> {
                onLongPressStart()
                waitForUpOrCancellation()
                onLongPressEnd()
            }
            else -> {
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
            color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold
        )
    }
}

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
        Text("${(level * 100).toInt()}%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}