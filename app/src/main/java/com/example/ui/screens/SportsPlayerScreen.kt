package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import android.util.Log
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.repository.YocinemaRepository
import com.example.ui.theme.YoBaseBackground
import com.example.ui.theme.YoPrimaryViolet
import com.example.ui.theme.YoTextMuted

private const val TAG = "SportsPlayer"

// Same UA string the movies player and the proxy use. The proxy sends its
// own UA upstream, so this only matters for the app→proxy leg — but keeping
// it consistent avoids any CDN-side UA-based logic if you ever bypass the proxy.
private const val PLAYER_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

@OptIn(UnstableApi::class)
@Composable
fun SportsPlayerScreen(
    matchId: String,
    streamUrl: String,
    title: String,
    repository: YocinemaRepository,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var isBuffering by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var retryTick by remember { mutableStateOf(0) }

    val apiKey = remember { repository.tokenManager.getApiKey() }

    // Attach X-API-Key to every player request — the proxy's /api/stream
    // endpoint requires it, exactly like every other endpoint in the app.
    // Without this the app silently 401s and ExoPlayer shows "source error".
    val exoPlayer = remember {
        val dsFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(PLAYER_USER_AGENT)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(
                if (!apiKey.isNullOrBlank()) mapOf("X-API-Key" to apiKey) else emptyMap()
            )

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(context).setDataSourceFactory(dsFactory)
            )
            .build()
            .apply { playWhenReady = true }
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

    LaunchedEffect(streamUrl, retryTick) {
        if (streamUrl.isBlank()) {
            loadError = "Stream URL is missing"
            return@LaunchedEffect
        }
        loadError = null
        isBuffering = true

        val mime = inferMimeType(streamUrl)
        Log.d(TAG, "playing url=$streamUrl mime=$mime hasKey=${!apiKey.isNullOrBlank()}")

        try {
            val item = MediaItem.Builder()
                .setUri(Uri.parse(streamUrl))
                .setMimeType(mime)
                .build()
            exoPlayer.setMediaItem(item)

            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    isBuffering = state == Player.STATE_BUFFERING
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    Log.e(TAG, "playback error", error)
                    loadError = error.message ?: "Playback failed"
                }
            }
            exoPlayer.addListener(listener)
            exoPlayer.prepare()
            exoPlayer.play()
        } catch (e: Exception) {
            Log.e(TAG, "start failed", e)
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
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp),
                ) {
                    Text(
                        text = "Playback Error",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = loadError ?: "",
                        color = YoTextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = { retryTick++ },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = YoPrimaryViolet,
                            contentColor = YoBaseBackground,
                        ),
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Retry", fontWeight = FontWeight.Bold)
                    }
                }
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

private fun inferMimeType(streamUrl: String): String {
    // URL is /api/stream?url=<encoded>, so the real media URL (and its
    // extension) lives inside the `url` query param.
    val underlying = runCatching {
        Uri.parse(streamUrl).getQueryParameter("url") ?: streamUrl
    }.getOrDefault(streamUrl)

    return when {
        underlying.contains(".m3u8", ignoreCase = true) -> MimeTypes.APPLICATION_M3U8
        underlying.contains(".mpd", ignoreCase = true) -> MimeTypes.APPLICATION_MPD
        else -> MimeTypes.VIDEO_MP4
    }
}