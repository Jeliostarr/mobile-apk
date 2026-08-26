package com.example.push

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

/**
 * Receives pushes sent to the "new_movies" topic (see the backend's
 * utils/pushNotifications.js — every install subscribes to this same topic
 * in YoApplication.onCreate(), so there's no per-device token registration
 * needed anywhere). Two payload shapes share this one topic:
 *  - New movie: data.type absent/"new_movie", optionally data.movieId —
 *    tapping opens the app, deep-linking to that movie if present.
 *  - Manual update reminder (admin panel's "Send Update Reminder" button):
 *    data.type = "update_available", data.apkUrl set — tapping opens the
 *    download link directly in the browser instead of the app, since a
 *    device old enough to need this reminder for real may not even have
 *    the in-app update-check/download flow yet.
 *
 * Two delivery paths, both handled:
 *  - App in foreground: onMessageReceived below fires and we build the
 *    notification ourselves.
 *  - App backgrounded/killed: the system builds it automatically using the
 *    "notification" block the backend sends plus the default channel/icon
 *    declared in AndroidManifest.xml — onMessageReceived may still fire
 *    with just the data payload in that case, which is why the extraction
 *    below doesn't assume notification.title/body are present.
 */
class YoFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "new_movies"
        const val EXTRA_MOVIE_ID = "com.example.push.EXTRA_MOVIE_ID"
        private const val TYPE_UPDATE_AVAILABLE = "update_available"
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val type = message.data["type"]
        val apkUrl = message.data["apkUrl"]

        if (type == TYPE_UPDATE_AVAILABLE && !apkUrl.isNullOrBlank()) {
            val title = message.notification?.title ?: "⬆️ Update Available"
            val body = message.notification?.body ?: "A new version of YOCINEMA is ready. Tap to download."
            showNotification(title, body, buildBrowserPendingIntent(apkUrl))
            return
        }

        val title = message.notification?.title ?: "🎬 New Movie Added!"
        val body = message.notification?.body ?: "Check out what's new on YOCINEMA."
        val movieId = message.data["movieId"]
        showNotification(title, body, buildAppPendingIntent(movieId))
    }

    private fun buildAppPendingIntent(movieId: String?): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!movieId.isNullOrBlank()) {
                putExtra(EXTRA_MOVIE_ID, movieId)
            }
        }
        return PendingIntent.getActivity(
            this,
            Random.nextInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildBrowserPendingIntent(apkUrl: String): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return PendingIntent.getActivity(
            this,
            Random.nextInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun showNotification(title: String, body: String, pendingIntent: PendingIntent) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // POST_NOTIFICATIONS is requested at runtime in MainActivity, but a
        // user can still deny it — notify() would throw a SecurityException
        // in that case rather than just silently no-op, so this has to be
        // guarded explicitly.
        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            NotificationManagerCompat.from(this).notify(Random.nextInt(), notification)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Topic-based messaging (see YoApplication.onCreate) doesn't need
        // this token registered anywhere — nothing to do here. Kept as an
        // override only so a future per-device-targeted feature (e.g.
        // "notify me when MY requested movie is added") has an obvious
        // place to start from.
    }
}
