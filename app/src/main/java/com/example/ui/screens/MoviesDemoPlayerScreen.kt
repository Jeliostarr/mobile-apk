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
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.MdCaption
import com.example.data.model.MdQuality
import com.example.data.model.moviesDemoStreamUrl
import com.example.repository.YocinemaRepository
import com.example.ui.components.ModernLoader
import com.example.ui.theme.YoPrimaryViolet
import com.example.ui.theme.YoTextMuted
import com.example.ui.theme.YoTextPrimary

private const val PLAYER_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
private const val CAPTIONS_OFF = "off"

@OptIn(UnstableApi::class)
@Composable
fun MoviesDemoPlayerScreen(
    detailPath: String,
    title: String,
    seasonNum: Int?,
    epNum: Int?,
    repository: YocinemaRepository,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var qualities by remember { mutableStateOf<List<MdQuality>>(emptyList()) }
    var captions by remember { mutableStateOf<List<MdCaption>>(emptyList()) }
    var selectedQuality by remember { mutableStateOf<MdQuality?>(null) }
    var selectedCaptionLang by remember { mutableStateOf(CAPTIONS_OFF) }
    var showQualityMenu by remember { mutableStateOf(false) }
    var showCaptionMenu by remember { mutableStateOf(false) }

    val onBack = rememberUpdatedState(onBackClick)
    val apiKey = remember { repository.tokenManager.getApiKey() }

    // The backend returns /api/stream?url=... URLs which require the
    // X-API-Key header on every request (video + subtitle). Attach it
    // here so ExoPlayer's fetch goes through authenticated.
    fun httpDataSourceFactory() = DefaultHttpDataSource.Factory()
        .setUserAgent(PLAYER_USER_AGENT)
        .setConnectTimeoutMs(15_000)
        .setReadTimeoutMs(30_000)
        .setAllowCrossProtocolRedirects(true)
        .setDefaultRequestProperties(
            if (!apiKey.isNullOrBlank()) mapOf("X-API-Key" to apiKey) else emptyMap()
        )

    fun buildMediaItem(videoUrl: String): MediaItem {
        val builder = MediaItem.Builder()
            .setUri(Uri.parse(moviesDemoStreamUrl(videoUrl)))
            .setMimeType(MimeTypes.VIDEO_MP4)

        // Attach every available subtitle track up front so switching
        // languages is a track-selection change, not a reload. English
        // gets the default flag so it plays first if present.
        if (captions.isNotEmpty()) {
            val subtitleConfigs = captions.mapNotNull { cap ->
                val url = cap.url ?: return@mapNotNull null
                MediaItem.SubtitleConfiguration.Builder(Uri.parse(moviesDemoStreamUrl(url)))
                    .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                    .setLanguage(cap.lan ?: cap.lanName ?: "und")
                    .setSelectionFlags(if (cap.isEnglish) C.SELECTION_FLAG_DEFAULT else 0)
                    .build()
            }
            builder.setSubtitleConfigurations(subtitleConfigs)
        }
        return builder.build()
    }

    // Lock orientation, keep screen on, hide system bars for the
    // duration of this screen — restores both on the way out.
    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            exoPlayer?.release()
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
        val controller = WindowInsetsControllerCompat(win, win.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    LaunchedEffect(detailPath, seasonNum, epNum) {
        isLoading = true
        loadError = null

        if (apiKey.isNullOrBlank()) {
            loadError = "No API key set"
            isLoading = false
            return@LaunchedEffect
        }

        try {
            val streamRes = if (seasonNum != null && epNum != null) {
                repository.moviesDemoApi.tvStream(detailPath, seasonNum, epNum)
            } else {
                repository.moviesDemoApi.movieStream(detailPath)
            }
            val stream = if (streamRes.isSuccessful) streamRes.body() else null
            qualities = stream?.freeQualities ?: emptyList()
            val defaultQ = stream?.defaultQuality

            val capRes = repository.moviesDemoApi.captions(detailPath, seasonNum, epNum)
            captions = (if (capRes.isSuccessful) capRes.body()?.captions else null)
                ?.filter { !it.url.isNullOrBlank() }
                ?: emptyList()
            selectedCaptionLang = captions.firstOrNull { it.isEnglish }?.lan ?: CAPTIONS_OFF

            val videoUrl = defaultQ?.url
            if (videoUrl.isNullOrBlank()) {
                loadError = "No playable video found for this title"
                isLoading = false
                return@LaunchedEffect
            }
            selectedQuality = defaultQ

            val player = ExoPlayer.Builder(context)
                .setMediaSourceFactory(
                    DefaultMediaSourceFactory(context)
                        .setDataSourceFactory(httpDataSourceFactory())
                )
                .build()
            player.setMediaItem(buildMediaItem(videoUrl))
            player.playWhenReady = true
            player.prepare()
            if (selectedCaptionLang == CAPTIONS_OFF) {
                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                    .build()
            }
            exoPlayer = player
            isLoading = false
        } catch (e: Exception) {
            loadError = e.message ?: "Couldn't play this video"
            isLoading = false
        }
    }

    fun switchQuality(quality: MdQuality) {
        val url = quality.url ?: return
        val player = exoPlayer ?: return
        val position = player.currentPosition
        val wasPlaying = player.isPlaying
        selectedQuality = quality
        player.setMediaItem(buildMediaItem(url), position)
        player.playWhenReady = wasPlaying
        player.prepare()
    }

    fun switchCaption(lang: String) {
        val player = exoPlayer ?: return
        selectedCaptionLang = lang
        player.trackSelectionParameters = if (lang == CAPTIONS_OFF) {
            player.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
        } else {
            player.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setPreferredTextLanguage(lang)
                .build()
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
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        if (title.isNotBlank()) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 16.dp, start = 64.dp, end = 130.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Quality + captions — top-right, only shown once the player has
        // something loaded to switch between.
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
        ) {
            if (captions.isNotEmpty()) {
                Box {
                    IconButton(
                        onClick = { showCaptionMenu = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                    ) {
                        Icon(
                            Icons.Default.ClosedCaption,
                            contentDescription = "Captions",
                            tint = if (selectedCaptionLang != CAPTIONS_OFF) YoPrimaryViolet else Color.White
                        )
                    }
                    DropdownMenu(
                        expanded = showCaptionMenu,
                        onDismissRequest = { showCaptionMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Off",
                                    color = if (selectedCaptionLang == CAPTIONS_OFF)
                                        YoPrimaryViolet else YoTextPrimary
                                )
                            },
                            onClick = {
                                switchCaption(CAPTIONS_OFF)
                                showCaptionMenu = false
                            }
                        )
                        captions.forEach { cap ->
                            val lang = cap.lan ?: cap.lanName ?: return@forEach
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        cap.lanName ?: lang,
                                        color = if (selectedCaptionLang == lang)
                                            YoPrimaryViolet else YoTextPrimary
                                    )
                                },
                                onClick = {
                                    switchCaption(lang)
                                    showCaptionMenu = false
                                }
                            )
                        }
                    }
                }
            }
            if (qualities.size > 1) {
                Box {
                    IconButton(
                        onClick = { showQualityMenu = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                    ) {
                        Icon(
                            Icons.Default.HighQuality,
                            contentDescription = "Quality",
                            tint = Color.White
                        )
                    }
                    DropdownMenu(
                        expanded = showQualityMenu,
                        onDismissRequest = { showQualityMenu = false }
                    ) {
                        qualities.forEach { q ->
                            val isSelected = q.resolution == selectedQuality?.resolution
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "${q.resolution ?: "?"}P",
                                        color = if (isSelected) YoPrimaryViolet else YoTextPrimary
                                    )
                                },
                                onClick = {
                                    switchQuality(q)
                                    showQualityMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}