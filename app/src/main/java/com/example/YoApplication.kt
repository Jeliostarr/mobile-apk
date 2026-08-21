package com.example

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.data.api.AuthInterceptor
import com.example.data.local.TokenManager
import okhttp3.OkHttpClient

class YoApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        val imageLoader = newImageLoader()
        Coil.setImageLoader(imageLoader)
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
