package com.example.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.upstream.DefaultAllocator
import androidx.media3.session.MediaSession
import com.example.data.model.Movie
import com.example.repository.YocinemaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

@OptIn(UnstableApi::class)
class PlayerManager(
    private val context: Context,
    private val repository: YocinemaRepository,
    private val scope: CoroutineScope
) {
    private val streamHttpClient: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .retryOnConnectionFailure(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val loadControl = DefaultLoadControl.Builder()
        .setAllocator(DefaultAllocator(true, 64 * 1024))
        .setBufferDurationsMs(30_000, 120_000, 1_500, 3_000)
        .setTargetBufferBytes(64 * 1024 * 1024)
        .setPrioritizeTimeOverSizeThresholds(true)
        .setBackBuffer(30_000, true)
        .build()

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setHandleAudioBecomingNoisy(true)
        .setLoadControl(loadControl)
        .build()

    // ─── Public state flows ───

    private val _isReconnecting = MutableStateFlow(false)
    val isReconnecting: StateFlow<Boolean> = _isReconnecting

    private val _playerError = MutableStateFlow<String?>(null)
    val playerError: StateFlow<String?> = _playerError

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering

    // ─── Quality + caption state (non-translated, sports) ───

    private val _qualities = MutableStateFlow<List<PlayerQuality>>(emptyList())
    val qualities: StateFlow<List<PlayerQuality>> = _qualities

    private val _captions = MutableStateFlow<List<PlayerCaption>>(emptyList())
    val captions: StateFlow<List<PlayerCaption>> = _captions

    private val _selectedQuality = MutableStateFlow<PlayerQuality?>(null)
    val selectedQuality: StateFlow<PlayerQuality?> = _selectedQuality

    private val _selectedCaptionLang = MutableStateFlow(CAPTIONS_OFF)
    val selectedCaptionLang: StateFlow<String> = _selectedCaptionLang

    // ─── Playback bookkeeping ───

    private var currentMovieId: String = ""
    private var currentEpisodeId: String? = null
    private var currentSeasonNum: Int? = null
    private var currentEpNum: Int? = null
    private var currentMediaUrl: String = ""
    private var resolvedMediaUrl: String? = null

    private var currentMovieForHistory: Movie? = null
    private var lastHistorySaveAt: Long = 0L
    private var currentTitle: String? = null
    private var currentPosterUrl: String? = null
    private var mediaSession: MediaSession? = null

    private var lastRecordedPos: Long = 0L
    private var stallCheckJob: Job? = null
    private var posJob: Job? = null
    private var progressPingJob: Job? = null

    private var softRetries: Int = 0
    private var hardRetries: Int = 0

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
                if (playing) startPositionUpdates() else stopPositionUpdates()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _isBuffering.value = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    _isReconnecting.value = false
                    _playerError.value = null
                    softRetries = 0
                    hardRetries = 0
                    _durationMs.value = exoPlayer.duration.coerceAtLeast(0L)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                error.printStackTrace()
                if (softRetries < 3 && resolvedMediaUrl != null) {
                    softRetries++
                    _isReconnecting.value = true
                    scope.launch {
                        delay(1_000L * softRetries)
                        loadAndPlaySource(currentMediaUrl, lastRecordedPos, reuseResolved = true)
                    }
                } else {
                    _playerError.value =
                        "Playback error: ${error.message ?: "Stream network error"}"
                    attemptReconnect()
                }
            }
        })
    }

    // ─── Position updates + stall detection ───

    private fun startPositionUpdates() {
        posJob?.cancel()
        posJob = scope.launch {
            while (true) {
                try {
                    val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val dur = exoPlayer.duration.coerceAtLeast(0L)
                    _currentPositionMs.value = pos
                    _durationMs.value = dur
                    lastRecordedPos = pos

                    val now = System.currentTimeMillis()
                    if (currentMovieId.isNotBlank() && dur > 0 && now - lastHistorySaveAt >= 5000) {
                        lastHistorySaveAt = now
                        saveHistoryPosition(pos, dur)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(1000)
            }
        }

        stallCheckJob?.cancel()
        stallCheckJob = scope.launch {
            var lastPos = -1L
            var stallSeconds = 0
            while (true) {
                delay(1000)
                try {
                    val stuck = exoPlayer.playWhenReady &&
                        exoPlayer.playbackState == Player.STATE_BUFFERING
                    val pos = exoPlayer.currentPosition
                    if (stuck && pos == lastPos) {
                        stallSeconds++
                        if (stallSeconds >= 45 && hardRetries < 2) {
                            stallSeconds = 0
                            attemptReconnect()
                        }
                    } else {
                        lastPos = pos
                        stallSeconds = 0
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun startProgressPing() {
        progressPingJob?.cancel()
        progressPingJob = scope.launch(Dispatchers.IO) {
            while (true) {
                delay(30_000)
                try {
                    if (exoPlayer.isPlaying && currentMovieId.isNotBlank()) {
                        val viewerId = repository.tokenManager.getOrCreateViewerId()
                        val seconds = exoPlayer.currentPosition / 1000
                        if (seconds > 0) {
                            repository.reportViewProgress(currentMovieId, viewerId, seconds)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun stopPositionUpdates() {
        posJob?.cancel()
        stallCheckJob?.cancel()
        progressPingJob?.cancel()
    }

    private fun saveHistoryPosition(pos: Long, dur: Long) {
        scope.launch(Dispatchers.IO) {
            try {
                val movie = currentMovieForHistory
                    ?: repository.getMovieDetail(currentMovieId)?.also { currentMovieForHistory = it }
                    ?: return@launch
                val epTitle = if (currentSeasonNum != null && currentEpNum != null) {
                    movie.episodes?.find { it.sNum == currentSeasonNum && it.eNum == currentEpNum }?.title
                } else null

                repository.saveHistory(
                    movieId = currentMovieId,
                    episodeId = currentEpisodeId,
                    title = movie.title,
                    poster = movie.displayPosterUrl,
                    vjName = movie.vjName,
                    seasonNum = currentSeasonNum,
                    epNum = currentEpNum,
                    epTitle = epTitle,
                    positionMs = pos,
                    durationMs = dur
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ─── Public API ───

    fun playMedia(
        movieId: String,
        mediaUrl: String,
        seasonNum: Int? = null,
        epNum: Int? = null,
        initialPositionMs: Long = 0L,
        title: String? = null,
        posterUrl: String? = null
    ) {
        currentMovieId = movieId
        currentSeasonNum = seasonNum
        currentEpNum = epNum
        currentEpisodeId = if (seasonNum != null && epNum != null) "S${seasonNum}E${epNum}" else null
        currentMovieForHistory = null
        lastHistorySaveAt = 0L
        currentMediaUrl = mediaUrl
        resolvedMediaUrl = null
        softRetries = 0
        hardRetries = 0
        currentTitle = title
        currentPosterUrl = posterUrl
        lastRecordedPos = initialPositionMs

        ensureMediaSession()
        loadAndPlaySource(mediaUrl, initialPositionMs)
    }

    fun setQualities(list: List<PlayerQuality>, default: PlayerQuality? = null) {
        _qualities.value = list
        _selectedQuality.value = default ?: list.firstOrNull()
    }

    fun setCaptions(list: List<PlayerCaption>) {
        _captions.value = list
        _selectedCaptionLang.value = list.firstOrNull { it.isEnglish }?.language ?: CAPTIONS_OFF
        applyCaptionSelection()
    }

    fun switchQuality(quality: PlayerQuality) {
        val currentPos = exoPlayer.currentPosition
        val wasPlaying = exoPlayer.isPlaying
        val url = quality.url

        scope.launch {
            val uri = Uri.parse(url)
            val isHls = url.contains(".m3u8", ignoreCase = true)
            val factory = httpFactory(forApiHost = false)
            val itemBuilder = MediaItem.Builder()
                .setUri(uri)
                .setMimeType(if (isHls) MimeTypes.APPLICATION_M3U8 else MimeTypes.VIDEO_MP4)
                .setMediaMetadata(buildMetadata())
            if (_captions.value.isNotEmpty()) {
                itemBuilder.setSubtitleConfigurations(buildSubtitleConfigs())
            }
            val mediaItem = itemBuilder.build()

            val source = if (isHls) {
                HlsMediaSource.Factory(factory)
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(mediaItem)
            } else {
                ProgressiveMediaSource.Factory(factory)
                    .setContinueLoadingCheckIntervalBytes(1024 * 1024)
                    .createMediaSource(mediaItem)
            }

            exoPlayer.setMediaSource(source, currentPos)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = wasPlaying
            _selectedQuality.value = quality
            applyCaptionSelection()
        }
    }

    fun switchCaption(language: String) {
        _selectedCaptionLang.value = language
        applyCaptionSelection()
    }

    private fun applyCaptionSelection() {
        val lang = _selectedCaptionLang.value
        exoPlayer.trackSelectionParameters = if (lang == CAPTIONS_OFF) {
            exoPlayer.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
        } else {
            exoPlayer.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setPreferredTextLanguage(lang)
                .build()
        }
    }

    private fun buildSubtitleConfigs(): List<MediaItem.SubtitleConfiguration> =
        _captions.value.mapNotNull { cap ->
            val url = cap.url ?: return@mapNotNull null
            MediaItem.SubtitleConfiguration.Builder(Uri.parse(url))
                .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                .setLanguage(cap.language ?: "und")
                .setSelectionFlags(if (cap.isEnglish) C.SELECTION_FLAG_DEFAULT else 0)
                .build()
        }

    private fun ensureMediaSession() {
        if (mediaSession != null) return
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            val pendingIntent = launchIntent?.let {
                PendingIntent.getActivity(
                    context, 0, it,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
            val builder = MediaSession.Builder(context, exoPlayer)
            if (pendingIntent != null) builder.setSessionActivity(pendingIntent)
            val session = builder.build()
            mediaSession = session
            com.example.player.PlaybackService.setActiveSession(session)
            context.startService(Intent(context, com.example.player.PlaybackService::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildMetadata(): MediaMetadata {
        val builder = MediaMetadata.Builder()
            .setTitle(currentTitle ?: "YOCINEMA")
            .setArtist("YOCINEMA")
        currentPosterUrl?.let { builder.setArtworkUri(Uri.parse(it)) }
        return builder.build()
    }

    private suspend fun resolveFinalUrl(url: String): String = withContext(Dispatchers.IO) {
        var current = url
        try {
            repeat(5) {
                val builder = Request.Builder().url(current).head()
                    .header("User-Agent", STREAM_USER_AGENT)
                if (current.startsWith(com.example.data.model.BASE_URL)) {
                    repository.tokenManager.getApiKey()?.takeIf { it.isNotBlank() }?.let { key ->
                        builder.header("X-API-Key", key)
                    }
                }
                streamHttpClient.newCall(builder.build()).execute().use { resp ->
                    val location = resp.header("Location")
                    if (resp.isRedirect && !location.isNullOrBlank()) {
                        current = resp.request.url.resolve(location)?.toString()
                            ?: return@withContext current
                    } else {
                        return@withContext current
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        current
    }

    private fun httpFactory(forApiHost: Boolean): DataSource.Factory {
        val headers = mutableMapOf("User-Agent" to STREAM_USER_AGENT)
        if (forApiHost) {
            repository.tokenManager.getApiKey()?.takeIf { it.isNotBlank() }?.let { key ->
                headers["X-API-Key"] = key
            }
        }
        return try {
            OkHttpDataSource.Factory(
                OkHttpClient.Builder()
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .retryOnConnectionFailure(true)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build()
            )
                .setUserAgent(STREAM_USER_AGENT)
                .setDefaultRequestProperties(headers)
        } catch (e: Throwable) {
            e.printStackTrace()
            DefaultHttpDataSource.Factory()
                .setConnectTimeoutMs(15_000)
                .setReadTimeoutMs(30_000)
                .setAllowCrossProtocolRedirects(true)
                .setKeepPostFor302Redirects(true)
                .setDefaultRequestProperties(headers)
        }
    }

    private fun loadAndPlaySource(
        url: String,
        seekPosMs: Long,
        reuseResolved: Boolean = false
    ) {
        scope.launch {
            try {
                val uri0 = Uri.parse(url)
                val isLocalFile = url.startsWith("/") || url.startsWith("file://") ||
                    uri0.scheme == "content" || uri0.scheme == null || uri0.scheme == "file" ||
                    java.io.File(url).exists()
                val metadata = buildMetadata()

                val mediaSource: MediaSource = if (isLocalFile) {
                    _isReconnecting.value = false
                    val fileUri = if (uri0.scheme == null && url.startsWith("/")) {
                        Uri.fromFile(java.io.File(url))
                    } else uri0
                    val localItem = MediaItem.Builder()
                        .setUri(fileUri)
                        .setMediaMetadata(metadata)
                        .build()
                    androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context)
                        .createMediaSource(localItem)
                } else {
                    val playUrl = (if (reuseResolved) resolvedMediaUrl else null)
                        ?: resolveFinalUrl(url).also { resolvedMediaUrl = it }
                    _isReconnecting.value = false

                    val uri = Uri.parse(playUrl)
                    val isHls = playUrl.contains(".m3u8", ignoreCase = true)
                    val onApiHost = playUrl.startsWith(com.example.data.model.BASE_URL)
                    val factory = httpFactory(onApiHost)

                    val itemBuilder = MediaItem.Builder()
                        .setUri(uri)
                        .setMimeType(if (isHls) MimeTypes.APPLICATION_M3U8 else MimeTypes.VIDEO_MP4)
                        .setMediaMetadata(metadata)
                    if (_captions.value.isNotEmpty()) {
                        itemBuilder.setSubtitleConfigurations(buildSubtitleConfigs())
                    }
                    val mediaItem = itemBuilder.build()

                    if (isHls) {
                        HlsMediaSource.Factory(factory)
                            .setAllowChunklessPreparation(true)
                            .createMediaSource(mediaItem)
                    } else {
                        ProgressiveMediaSource.Factory(factory)
                            .setContinueLoadingCheckIntervalBytes(1024 * 1024)
                            .createMediaSource(mediaItem)
                    }
                }

                exoPlayer.setMediaSource(mediaSource)
                applyCaptionSelection()
                exoPlayer.prepare()
                if (seekPosMs > 0) {
                    exoPlayer.seekTo(seekPosMs)
                }
                exoPlayer.playWhenReady = true
                startProgressPing()
            } catch (e: Exception) {
                e.printStackTrace()
                _isReconnecting.value = false
                _playerError.value = "Couldn't start playback: ${e.message ?: "unknown error"}"
            }
        }
    }

    fun attemptReconnect() {
        if (_isReconnecting.value) return
        if (hardRetries >= 3) {
            _playerError.value = "Stream keeps dropping — please check your connection and retry."
            return
        }
        hardRetries++
        _isReconnecting.value = true
        scope.launch(Dispatchers.IO) {
            try {
                repository.streamTokenManager.invalidateToken(currentMovieId)
                val movie = repository.getMovieDetail(currentMovieId)
                if (movie != null) {
                    val freshUrl = repository.getPlayUrl(movie, currentSeasonNum, currentEpNum)
                    currentMediaUrl = freshUrl
                    resolvedMediaUrl = null
                    softRetries = 0
                    withContext(Dispatchers.Main) {
                        loadAndPlaySource(freshUrl, lastRecordedPos)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _isReconnecting.value = false
                        _playerError.value = "Couldn't reconnect — check your connection and try again."
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _isReconnecting.value = false
                    _playerError.value = "Couldn't reconnect — check your connection and try again."
                }
            }
        }
    }

    fun release() {
        stopPositionUpdates()
        mediaSession?.let { session ->
            com.example.player.PlaybackService.setActiveSession(null)
            session.release()
        }
        mediaSession = null
        exoPlayer.release()
        try {
            context.stopService(Intent(context, com.example.player.PlaybackService::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val STREAM_USER_AGENT = "YoCinema-Android-Player/1.0"
    }
}

// ─── Top-level types (outside PlayerManager, so the whole app can use them) ───

/** A selectable video quality for the current content. */
data class PlayerQuality(
    val label: String,
    val url: String,
    val resolution: Int? = null,
)

/** A subtitle track. */
data class PlayerCaption(
    val language: String?,
    val displayName: String?,
    val url: String?,
) {
    val isEnglish: Boolean
        get() = language.equals("en", true) || displayName.equals("English", true)
}

private const val CAPTIONS_OFF = "off"