package com.example.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
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
                    _durationMs.value = exoPlayer.duration.coerceAtLeast(0L)
                } else if (playbackState == Player.STATE_BUFFERING) {
                    // Check stall
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                error.printStackTrace()
                // Re-mint stream token and attempt auto-reconnect
                attemptReconnect()
            }
        })
    }

    private var posJob: Job? = null
    private fun startPositionUpdates() {
        posJob?.cancel()
        posJob = scope.launch {
            while (true) {
                val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                val dur = exoPlayer.duration.coerceAtLeast(0L)
                _currentPositionMs.value = pos
                _durationMs.value = dur
                lastRecordedPos = pos

                // Save to history every 5s
                if (currentMovieId.isNotBlank() && dur > 0) {
                    saveHistoryPosition(pos, dur)
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
            }
        }
    }

    private var progressPingJob: Job? = null

    private fun startProgressPing() {
        progressPingJob?.cancel()
        progressPingJob = scope.launch(Dispatchers.IO) {
            while (true) {
                delay(30_000)
                if (exoPlayer.isPlaying && currentMovieId.isNotBlank()) {
                    val viewerId = repository.tokenManager.getOrCreateViewerId()
                    val seconds = exoPlayer.currentPosition / 1000
                    if (seconds > 0) {
                        repository.reportViewProgress(currentMovieId, viewerId, seconds)
                    }
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
            val movie = repository.getMovieDetail(currentMovieId) ?: return@launch
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
        }
    }

    fun playMedia(
        movieId: String,
        mediaUrl: String,
        seasonNum: Int? = null,
        epNum: Int? = null,
        initialPositionMs: Long = 0L
    ) {
        currentMovieId = movieId
        currentSeasonNum = seasonNum
        currentEpNum = epNum
        currentEpisodeId = if (seasonNum != null && epNum != null) "S${seasonNum}E${epNum}" else null
        currentMediaUrl = mediaUrl
        lastRecordedPos = initialPositionMs

        loadAndPlaySource(mediaUrl, initialPositionMs)
    }

    private fun loadAndPlaySource(url: String, seekPosMs: Long) {
        scope.launch {
            _isReconnecting.value = false
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

            val uri = Uri.parse(url)
            val mediaItem = MediaItem.fromUri(uri)

            val mediaSource: MediaSource = try {
                androidx.media3.exoplayer.source.DefaultMediaSourceFactory(httpDataSourceFactory)
                    .createMediaSource(mediaItem)
            } catch (e: Exception) {
                if (url.contains(".m3u8", ignoreCase = true)) {
                    HlsMediaSource.Factory(httpDataSourceFactory).createMediaSource(mediaItem)
                } else {
                    ProgressiveMediaSource.Factory(httpDataSourceFactory).createMediaSource(mediaItem)
                }
            }

            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()
            if (seekPosMs > 0) {
                exoPlayer.seekTo(seekPosMs)
            }
            exoPlayer.playWhenReady = true
            startProgressPing()
        }
    }

    fun attemptReconnect() {
        if (_isReconnecting.value) return
        _isReconnecting.value = true
        scope.launch(Dispatchers.IO) {
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
                }
            }
        }
    }

    fun release() {
        stopPositionUpdates()
        exoPlayer.release()
    }
}
