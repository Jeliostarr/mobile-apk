package com.example.ui.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment

/**
 * Downloads a movies-demo quality file via the system DownloadManager —
 * same mechanism ApkInstaller.kt uses for app updates, so it shows in the
 * notification shade and survives the app being backgrounded, just pointed
 * at the public Downloads folder instead of app-private storage since
 * these are files the person actually wants to find and share afterward.
 *
 * The download URL is the /api/download proxy (see moviesDemoDownloadUrl
 * in MoviesDemoModels.kt), which requires the same X-API-Key header as
 * every other movies-demo request — DownloadManager doesn't go through
 * OkHttp/the app's AuthInterceptor, so the header has to be attached here
 * explicitly.
 */
object MoviesDemoDownloader {
    fun startDownload(context: Context, downloadUrl: String, filename: String, apiKey: String?): Long {
        val request = DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle(filename)
            .setDescription("Downloading…")
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)

        if (!apiKey.isNullOrBlank()) {
            request.addRequestHeader("X-API-Key", apiKey)
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return downloadManager.enqueue(request)
    }
}
