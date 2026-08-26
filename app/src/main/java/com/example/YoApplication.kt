package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import coil.Coil
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.data.api.AuthInterceptor
import com.example.data.local.TokenManager
import com.example.push.YoFirebaseMessagingService
import com.google.firebase.messaging.FirebaseMessaging
import okhttp3.OkHttpClient

class YoApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        val imageLoader = newImageLoader()
        Coil.setImageLoader(imageLoader)

        createNotificationChannel()

        // Subscribing is idempotent — safe to call on every single launch,
        // Firebase no-ops if already subscribed. This is the entire
        // "registration" step; there's no per-device token to send
        // anywhere since the backend pushes to this topic directly.
        FirebaseMessaging.getInstance().subscribeToTopic("new_movies")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            YoFirebaseMessagingService.CHANNEL_ID,
            "New Movies",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifies you when a new movie or series is added"
        }
        manager.createNotificationChannel(channel)
    }

    override fun newImageLoader(): ImageLoader {
        val tokenManager = TokenManager(this)
        // AuthInterceptor reports failed authenticated requests (expired/
        // revoked/rate-limited key, etc) into ApiIssueReporter, which is a
        // singleton — so this interceptor automatically feeds the same
        // global warning modal as YocinemaRepository's own client, with no
        // extra wiring needed here. A poster/cover image failing to load
        // because the key was revoked will trigger the same dialog as any
        // other screen's failed fetch.
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager))
            .build()
        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .crossfade(true)
            .build()
    }
}
