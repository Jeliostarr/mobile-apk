package com.example.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
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

@OptIn(UnstableApi::class)
class PlayerManager(
    private val context: Context,
    private val repository: YocinemaRepository,
    private val scope: CoroutineScope
) {
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setHandleAudioBecomingNoisy(true)
        .build()

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

    private var currentMovieId: String = ""
    private var currentEpisodeId: String? = null
    private var currentSeasonNum: Int? = null
    private var currentEpNum: Int? = null
    private var currentMediaUrl: String = ""
    // Cached once per playMedia() call instead of refetched on every history
    // tick — avoids hammering the repository/network every second.
    private var currentMovieForHistory: Movie? = null
    private var lastHistorySaveAt: Long = 0L
    private var currentTitle: String? = null
    private var currentPosterUrl: String? = null
    private var mediaSession: MediaSession? = null

    private var lastRecordedPos: Long = 0L
    private var stallCheckJob: Job? = null
    private var lastPosCheckTime: Long = System.currentTimeMillis()

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
                if (playing) {
                    startPositionUpdates()
                } else {
                    stopPositionUpdates()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _isReconnecting.value = false
                    _playerError.value = null
                    _durationMs.value = exoPlayer.duration.coerceAtLeast(0L)
                } else if (playbackState == Player.STATE_BUFFERING) {
                    // Check stall
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                error.printStackTrace()
                _playerError.value = "Playback error: ${error.message ?: "Stream network error"}"
                attemptReconnect()
            }
        })
    }

    private var posJob: Job? = null
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

                    // Save to history every 5s — throttled by elapsed time, not
                    // by tick count, so this stays accurate regardless of the
                    // update interval below.
                    val now = System.currentTimeMillis()
                    if (currentMovieId.isNotBlank() && dur > 0 && now - lastHistorySaveAt >= 5000) {
                        lastHistorySaveAt = now
                        saveHistoryPosition(pos, dur)
                    }
                } catch (e: Exception) {
                    // A single bad tick (e.g. player mid-teardown) should never
                    // kill this loop or the app — just skip and try again.
                    e.printStackTrace()
                }
                delay(1000)
            }
        }

        // Stall detector (>25s no progress while playing)
        stallCheckJob?.cancel()
        stallCheckJob = scope.launch {
            var lastPos = 0L
            var stallSeconds = 0
            while (true) {
                delay(1000)
                try {
                    if (exoPlayer.isPlaying) {
                        val pos = exoPlayer.currentPosition
                        if (pos == lastPos && pos > 0) {
                            stallSeconds++
                            if (stallSeconds >= 25) {
                                stallSeconds = 0
                                attemptReconnect()
                            }
                        } else {
                            lastPos = pos
                            stallSeconds = 0
                        }
                    } else {
                        stallSeconds = 0
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private var progressPingJob: Job? = null

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
                // Fetch once per playback session and reuse — not once a
                // second. repository.getMovieDetail() already falls back to
                // cache on failure, but calling it on every tick was both
                // wasteful and, combined with an unguarded DB write further
                // down, the actual cause of the app crashing a few seconds
                // into playback (an exception here had nothing to catch it).
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
                // Never let a history-save failure take down playback.
                e.printStackTrace()
            }
        }
    }

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
        // New title/episode — the cached movie (if any) belonged to whatever
        // was playing before and must not leak into this session's history.
        currentMovieForHistory = null
        lastHistorySaveAt = 0L
        currentMediaUrl = mediaUrl
        currentTitle = title
        currentPosterUrl = posterUrl
        lastRecordedPos = initialPositionMs

        ensureMediaSession()
        loadAndPlaySource(mediaUrl, initialPositionMs)
    }

    /**
     * Builds a MediaSession around this manager's real, playing ExoPlayer
     * and registers it with PlaybackService — this is what makes lock-screen
     * and notification controls reflect the actual video that's actually
     * playing, instead of a second, disconnected player.
     */
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
            // Lock-screen controls are a nice-to-have, not something that
            // should ever be able to break in-app playback if it fails.
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

    private fun loadAndPlaySource(url: String, seekPosMs: Long) {
        scope.launch {
            try {
                _isReconnecting.value = false
                val uri = Uri.parse(url)
                val isLocalFile = url.startsWith("/") || url.startsWith("file://") || uri.scheme == null || uri.scheme == "file" || java.io.File(url).exists()
                val metadata = buildMetadata()

                val mediaSource: MediaSource = if (isLocalFile) {
                    val fileUri = if (url.startsWith("/")) Uri.fromFile(java.io.File(url)) else uri
                    val localItem = MediaItem.Builder().setUri(fileUri).setMediaMetadata(metadata).build()
                    androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context)
                        .createMediaSource(localItem)
                } else {
                    val headers = mutableMapOf<String, String>()
                    headers["User-Agent"] = "YoCinema-Android-Player/1.0"
                    val apiKey = repository.tokenManager.getApiKey()
                    if (!apiKey.isNullOrBlank()) {
                        headers["X-API-Key"] = apiKey
                        headers["x-api-key"] = apiKey
                    }

                    val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                        .setConnectTimeoutMs(60_000)
                        .setReadTimeoutMs(60_000)
                        .setAllowCrossProtocolRedirects(true)
                        .setDefaultRequestProperties(headers)

                    // Lock-screen/notification art and title come from this
                    // metadata via the MediaSession — without it, PlaybackService
                    // has a real, working session but nothing to actually show.
                    val mediaItem = MediaItem.Builder().setUri(uri).setMediaMetadata(metadata).build()
                    try {
                        androidx.media3.exoplayer.source.DefaultMediaSourceFactory(httpDataSourceFactory)
                            .createMediaSource(mediaItem)
                    } catch (e: Exception) {
                        if (url.contains(".m3u8", ignoreCase = true)) {
                            HlsMediaSource.Factory(httpDataSourceFactory).createMediaSource(mediaItem)
                        } else {
                            ProgressiveMediaSource.Factory(httpDataSourceFactory).createMediaSource(mediaItem)
                        }
                    }
                }

                exoPlayer.setMediaSource(mediaSource)
                exoPlayer.prepare()
                if (seekPosMs > 0) {
                    exoPlayer.seekTo(seekPosMs)
                }
                exoPlayer.playWhenReady = true
                startProgressPing()
            } catch (e: Exception) {
                // A bad URL, a missing/corrupt local file, or a data-source
                // failure here would otherwise crash the whole app instead
                // of just failing this one playback attempt.
                e.printStackTrace()
                _playerError.value = "Couldn't start playback: ${e.message ?: "unknown error"}"
            }
        }
    }

    fun attemptReconnect() {
        if (_isReconnecting.value) return
        _isReconnecting.value = true
        scope.launch(Dispatchers.IO) {
            try {
                repository.streamTokenManager.invalidateToken(currentMovieId)
                val movie = repository.getMovieDetail(currentMovieId)
                if (movie != null) {
                    val freshUrl = repository.getPlayUrl(movie, currentSeasonNum, currentEpNum)
                    currentMediaUrl = freshUrl
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
}
