package com.example.push

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

/**
 * Receives pushes sent to the "new_movies" topic (see the backend's
 * utils/pushNotifications.js — every install subscribes to this same topic
 * in YoApplication.onCreate(), so there's no per-device token registration
 * needed anywhere). Two payload shapes share this one topic:
 *  - New movie: data.type absent/"new_movie", optionally data.movieId and
 *    data.description — tapping opens the app, deep-linking to that movie
 *    if present. The notification tries to show the movie's hero/backdrop
 *    image (fetchBitmap below); if that fails for any reason — network,
 *    a URL that turns out to need auth we can't provide here, anything —
 *    it falls back to showing the movie's own description as the
 *    notification's expanded text instead of just the generic body line.
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
 *    "notification" block the backend sends (including its imageUrl, if
 *    any) plus the default channel/icon declared in AndroidManifest.xml —
 *    onMessageReceived may still fire with just the data payload in that
 *    case, which is why the extraction below doesn't assume
 *    notification.title/body are present.
 */
class YoFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "new_movies"
        const val EXTRA_MOVIE_ID = "com.example.push.EXTRA_MOVIE_ID"
        private const val TYPE_UPDATE_AVAILABLE = "update_available"
        private const val IMAGE_FETCH_TIMEOUT_MS = 5000
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val type = message.data["type"]
        val apkUrl = message.data["apkUrl"]

        if (type == TYPE_UPDATE_AVAILABLE && !apkUrl.isNullOrBlank()) {
            val title = message.notification?.title ?: "⬆️ Update Available"
            val body = message.notification?.body ?: "A new version of YOCINEMA is ready. Tap to download."
            showTextNotification(title, body, buildBrowserPendingIntent(apkUrl))
            return
        }

        val title = message.notification?.title ?: "🎬 New Movie Added!"
        val body = message.notification?.body ?: "Check out what's new on YOCINEMA."
        val movieId = message.data["movieId"]
        val description = message.data["description"]
        val imageUrl = message.notification?.imageUrl?.toString()

        // Blocking fetch is fine here — FCM already runs onMessageReceived
        // on its own background thread, not the main thread.
        val bitmap = imageUrl?.let { fetchBitmap(it) }

        showMovieNotification(title, body, description, bitmap, buildAppPendingIntent(movieId))
    }

    /**
     * Best-effort image fetch — returns null (never throws) on literally
     * any failure: no network, a timeout, a 404, or a URL that turns out
     * to require auth headers this plain connection can't supply. The
     * caller treats null exactly the same regardless of which of those it
     * was, and falls back to text.
     */
    private fun fetchBitmap(imageUrl: String): Bitmap? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(imageUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = IMAGE_FETCH_TIMEOUT_MS
                readTimeout = IMAGE_FETCH_TIMEOUT_MS
                doInput = true
            }
            connection.connect()
            if (connection.responseCode !in 200..299) {
                null
            } else {
                connection.inputStream.use { BitmapFactory.decodeStream(it) }
            }
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
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

    private fun showTextNotification(title: String, body: String, pendingIntent: PendingIntent) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notifyIfEnabled(notification)
    }

    private fun showMovieNotification(
        title: String,
        body: String,
        description: String?,
        bitmap: Bitmap?,
        pendingIntent: PendingIntent
    ) {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (bitmap != null) {
            builder
                .setLargeIcon(bitmap)
                .setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        // Collapses the large icon back to the app icon
                        // once the notification is expanded — standard
                        // pattern so the big picture isn't duplicated as a
                        // thumbnail too.
                        .bigLargeIcon(null as Bitmap?)
                )
        } else {
            // Image unavailable for any reason (network, timeout, or a
            // URL that turns out to need auth we can't supply here) —
            // show the movie's own description instead of just the
            // generic body line, per the fallback this was built for.
            val expandedText = description?.takeIf { it.isNotBlank() } ?: body
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
        }

        notifyIfEnabled(builder.build())
    }

    private fun notifyIfEnabled(notification: android.app.Notification) {
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
