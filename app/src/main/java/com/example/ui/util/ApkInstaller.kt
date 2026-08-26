package com.example.ui.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.delay
import java.io.File

/**
 * Downloads the update APK via the system DownloadManager (shows in the
 * notification shade like any other download, survives the app being
 * backgrounded) and hands it to the package installer via FileProvider —
 * a raw file:// Uri is blocked by StrictMode on modern Android, which is
 * why AndroidManifest.xml declares a FileProvider for this specifically.
 *
 * First-time install note: Android requires the user to grant "install
 * unknown apps" for this app specifically before the install prompt can
 * proceed — the system handles that automatically (its own settings
 * screen appears) the first time installApk() fires; nothing extra is
 * needed here beyond the REQUEST_INSTALL_PACKAGES manifest permission.
 */
object ApkInstaller {
    private const val FILE_NAME = "yocinema-update.apk"

    private fun destFile(context: Context): File =
        File(context.getExternalFilesDir(null), FILE_NAME)

    /** Starts the download and returns its DownloadManager id for progress polling. */
    fun startDownload(context: Context, apkUrl: String): Long {
        val file = destFile(context)
        if (file.exists()) file.delete()

        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("YOCINEMA Update")
            .setDescription("Downloading the latest version…")
            .setDestinationUri(Uri.fromFile(file))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return downloadManager.enqueue(request)
    }

    /**
     * Polls DownloadManager for progress every 400ms until the download
     * finishes or fails. Suspends until done — callers should launch this
     * in their own coroutine scope (e.g. rememberCoroutineScope in the
     * update dialog) so it doesn't block anything else.
     */
    suspend fun awaitDownload(
        context: Context,
        downloadId: Long,
        onProgress: (percent: Int) -> Unit,
        onSuccess: () -> Unit,
        onFailure: (message: String) -> Unit
    ) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        while (true) {
            val cursor = downloadManager.query(DownloadManager.Query().setFilterById(downloadId))
            if (!cursor.moveToFirst()) {
                cursor.close()
                onFailure("Download not found")
                return
            }

            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    cursor.close()
                    onSuccess()
                    return
                }
                DownloadManager.STATUS_FAILED -> {
                    val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                    cursor.close()
                    onFailure("Download failed (code $reason)")
                    return
                }
                else -> {
                    val bytesIdx = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val totalIdx = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    val bytes = cursor.getLong(bytesIdx)
                    val total = cursor.getLong(totalIdx)
                    cursor.close()
                    if (total > 0) {
                        onProgress(((bytes * 100) / total).toInt())
                    }
                }
            }
            delay(400)
        }
    }

    /** Fires the system package-installer prompt for the already-downloaded APK. */
    fun installApk(context: Context) {
        val file = destFile(context)
        if (!file.exists()) return

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Fires the install prompt, then kills this process shortly after.
     *
     * This matters specifically because MainActivity uses launchMode
     * "singleTop" (needed for notification deep-links) — without this,
     * reopening the app after installing (whether via the system
     * installer's own "Open" button or the launcher icon) can just resume
     * THIS still-alive process instead of starting a genuinely fresh one.
     * A running process keeps executing the code it already loaded into
     * memory even after the APK on disk has been replaced — which is
     * exactly why the force-update dialog was reappearing right after a
     * successful install. Killing the process ourselves guarantees the
     * next launch actually picks up the new code.
     */
    fun installApkAndTerminate(context: Context) {
        installApk(context)
        // Small delay so the system installer's own screen actually gets
        // a chance to come to the foreground before we pull the process
        // out from under it — killing immediately can race the intent
        // launch on slower devices.
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            android.os.Process.killProcess(android.os.Process.myPid())
            kotlin.system.exitProcess(0)
        }, 1000)
    }
}
