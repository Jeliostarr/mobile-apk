package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.moviesDemoStreamUrl
import com.example.repository.YocinemaRepository
import com.example.ui.components.ModernLoader
import com.example.ui.theme.YoTextMuted

private const val PLAYER_USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

@OptIn(UnstableApi::class)
@Composable
fun MoviesDemoPlayerScreen(
    detailPath: String,
    title: String,
    seasonNum: Int?,
    epNum: Int?,
    isTrailer: Boolean,
    repository: YocinemaRepository,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    val onBack = rememberUpdatedState(onBackClick)

    // Same full-screen landscape treatment as the main PlayerScreen — locks
    // orientation and hides system bars for the duration of this screen,
    // restores both on the way out.
    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            exoPlayer?.release()
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
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

    LaunchedEffect(detailPath, seasonNum, epNum, isTrailer) {
        isLoading = true
        loadError = null

        val apiKey = repository.tokenManager.getApiKey()
        if (apiKey.isNullOrBlank()) {
            loadError = "No API key set"
            isLoading = false
            return@LaunchedEffect
        }

        try {
            var videoUrl: String? = null
            var subtitleUrl: String? = null

            if (isTrailer) {
                val res = repository.moviesDemoApi.details(detailPath)
                videoUrl = if (res.isSuccessful) res.body()?.trailer?.url else null
            } else {
                val streamRes = if (seasonNum != null && epNum != null) {
                    repository.moviesDemoApi.tvStream(detailPath, seasonNum, epNum)
                } else {
                    repository.moviesDemoApi.movieStream(detailPath)
                }
                val stream = if (streamRes.isSuccessful) streamRes.body() else null
                videoUrl = stream?.defaultQuality?.url

                // English captions by default when available — the reference
                // site actually defaults subtitles to off, this is a
                // deliberate improvement, not a copy of that behavior.
                val capRes = repository.moviesDemoApi.captions(detailPath, seasonNum, epNum)
                val captions = if (capRes.isSuccessful) capRes.body() else null
                subtitleUrl = captions?.defaultCaption?.url
            }

            if (videoUrl.isNullOrBlank()) {
                loadError = "No playable video found for this title"
                isLoading = false
                return@LaunchedEffect
            }

            val headers = mapOf("X-API-Key" to apiKey)
            val dataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(PLAYER_USER_AGENT)
                .setConnectTimeoutMs(15_000)
                .setReadTimeoutMs(30_000)
                .setAllowCrossProtocolRedirects(true)
                .setDefaultRequestProperties(headers)

            val mediaItemBuilder = MediaItem.Builder()
                .setUri(Uri.parse(moviesDemoStreamUrl(videoUrl)))
                .setMimeType(MimeTypes.VIDEO_MP4)

            if (!subtitleUrl.isNullOrBlank()) {
                val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(Uri.parse(moviesDemoStreamUrl(subtitleUrl)))
                    .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                    .setLanguage("en")
                    .setSelectionFlags(androidx.media3.common.C.SELECTION_FLAG_DEFAULT)
                    .build()
                mediaItemBuilder.setSubtitleConfigurations(listOf(subtitleConfig))
            }

            val player = ExoPlayer.Builder(context)
                .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(dataSourceFactory))
                .build()
            player.setMediaItem(mediaItemBuilder.build())
            player.playWhenReady = true
            player.prepare()
            exoPlayer = player
            isLoading = false
        } catch (e: Exception) {
            loadError = e.message ?: "Couldn't play this video"
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val player = exoPlayer
        if (player != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = true
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ModernLoader()
            }
        }

        if (loadError != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = loadError ?: "", color = YoTextMuted, fontSize = 14.sp)
            }
        }

        IconButton(
            onClick = { onBack.value() },
            modifier = Modifier
                .padding(16.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        if (title.isNotBlank()) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 16.dp, start = 64.dp, end = 16.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}