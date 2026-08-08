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
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager))
            .build()
        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .crossfade(true)
            .build()
    }
}
