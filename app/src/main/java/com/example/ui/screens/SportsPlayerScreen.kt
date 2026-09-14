package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.ui.theme.YoPrimaryViolet

@OptIn(UnstableApi::class)
@Composable
fun SportsPlayerScreen(
    matchId: String,
    streamUrl: String,
    title: String,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var isBuffering by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            exoPlayer.release()
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { win ->
                WindowInsetsControllerCompat(win, win.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    LaunchedEffect(Unit) {
        val win = activity?.window ?: return@LaunchedEffect
        val ctrl = WindowInsetsControllerCompat(win, win.decorView)
        ctrl.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        ctrl.hide(WindowInsetsCompat.Type.systemBars())
    }

    LaunchedEffect(streamUrl) {
        if (streamUrl.isBlank()) {
            loadError = "Stream URL is missing"
            return@LaunchedEffect
        }
        try {
            val item = MediaItem.Builder()
                .setUri(Uri.parse(streamUrl))
                // HLS URLs from this API always end in .m3u8, but be explicit
                // so ExoPlayer doesn't have to guess.
                .setMimeType(MimeTypes.APPLICATION_M3U8)
                .build()
            exoPlayer.setMediaItem(item)

            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    isBuffering = state == Player.STATE_BUFFERING
                }
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    loadError = error.message ?: "Playback failed"
                }
            }
            exoPlayer.addListener(listener)
            exoPlayer.prepare()
            exoPlayer.play()
        } catch (e: Exception) {
            loadError = e.message ?: "Couldn't start playback"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (loadError == null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (isBuffering && loadError == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = YoPrimaryViolet)
            }
        }

        if (loadError != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = loadError ?: "",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }

        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .padding(16.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f)),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
            )
        }

        if (title.isNotBlank()) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 16.dp, start = 64.dp, end = 16.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
    }
}