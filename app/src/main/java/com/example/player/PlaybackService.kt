package com.example.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Hosts the lock-screen / notification media session. This service
 * deliberately never creates its own ExoPlayer — a separate player here
 * that's never fed any media is exactly what makes lock-screen controls
 * either do nothing or show a blank "Not Playing" state. Instead,
 * [PlayerManager] builds a MediaSession around the ONE real player that's
 * actually running and registers it here via [setActiveSession], so
 * whatever the system shows (notification, lock screen, Bluetooth, Android
 * Auto) is always driven by the real, currently-playing video.
 */
@UnstableApi
class PlaybackService : MediaSessionService() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return activeSession
    }

    override fun onDestroy() {
        // Don't touch activeSession here — PlayerManager owns that
        // lifecycle and releases it explicitly when playback truly ends.
        // The OS can recreate this service independently of that.
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "YOCINEMA Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "YOCINEMA media playback service"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "yocinema_playback_channel"
        const val NOTIFICATION_ID = 1001

        @Volatile
        private var activeSession: MediaSession? = null

        /**
         * Called by PlayerManager to register (or clear, with null) the
         * session wrapping the app's real playing ExoPlayer instance.
         */
        fun setActiveSession(session: MediaSession?) {
            activeSession = session
        }
    }
}
