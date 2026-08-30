package com.example.download

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.local.DownloadEntity
import com.example.repository.YocinemaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class DownloadWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val downloadId = inputData.getString(KEY_DOWNLOAD_ID) ?: return@withContext Result.failure()
        val movieId = inputData.getString(KEY_MOVIE_ID) ?: return@withContext Result.failure()
        val episodeId = inputData.getString(KEY_EPISODE_ID)
        val title = inputData.getString(KEY_TITLE) ?: "Video"
        val seasonNum = inputData.getInt(KEY_SEASON_NUM, -1).takeIf { it != -1 }
        val epNum = inputData.getInt(KEY_EP_NUM, -1).takeIf { it != -1 }

        val repository = YocinemaRepository(context)
        val db = AppDatabase.getInstance(context)
        val downloadDao = db.downloadDao()

        // Get movie details (for metadata)
        val movie = repository.getMovieDetail(movieId)
        if (movie == null) {
            downloadDao.updateStatus(downloadId, DownloadEntity.STATUS_FAILED)
            return@withContext Result.failure()
        }
        val downloadUrl = repository.getDownloadMediaUrl(movie, seasonNum, epNum)

        val safeFilename = "${title.replace(Regex("[^a-zA-Z0-9._-]"), "_")}.mp4"

        // Temp file in internal cache (supports resume)
        val cacheDir = context.cacheDir
        val tempFile = File(cacheDir, "$downloadId.part")
        var existingBytes = if (tempFile.exists()) tempFile.length() else 0L

        // Create or update DB entity with all metadata
        var entity = downloadDao.getDownloadById(downloadId)
        if (entity == null) {
            entity = DownloadEntity(
                downloadId = downloadId,
                movieId = movieId,
                episodeId = episodeId,
                title = title,
                posterUrl = movie.displayPosterUrl,
                vjName = movie.vjName,
                seasonNumber = seasonNum,
                episodeNumber = epNum,
                episodeTitle = null, // can be filled if needed
                filename = safeFilename,
                tempFilePath = tempFile.absolutePath,
                totalBytes = 0L,
                downloadedBytes = existingBytes,
                status = DownloadEntity.STATUS_DOWNLOADING,
                localFilePath = null,
                downloadedAt = null
            )
            downloadDao.saveDownload(entity)
        } else {
            downloadDao.updateStatus(downloadId, DownloadEntity.STATUS_DOWNLOADING)
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        val requestBuilder = Request.Builder().url(downloadUrl)
        val apiKey = repository.tokenManager.getApiKey()
        if (!apiKey.isNullOrBlank()) {
            requestBuilder.header("x-api-key", apiKey)
        }
        if (existingBytes > 0) {
            requestBuilder.header("Range", "bytes=$existingBytes-")
        }

        try {
            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful && response.code != 206) {
                downloadDao.updateStatus(downloadId, DownloadEntity.STATUS_FAILED)
                return@withContext Result.failure()
            }

            val body = response.body ?: run {
                downloadDao.updateStatus(downloadId, DownloadEntity.STATUS_FAILED)
                return@withContext Result.failure()
            }

            val contentLength = body.contentLength()
            val totalBytes = if (response.code == 206) existingBytes + contentLength else contentLength
            downloadDao.updateProgress(downloadId, existingBytes, totalBytes, 0, DownloadEntity.STATUS_DOWNLOADING)

            // Write to temp file (append)
            val outputStream = FileOutputStream(tempFile, true)
            val inputStream = body.byteStream()
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var downloaded = existingBytes
            var lastUpdateMs = System.currentTimeMillis()
            var bytesSinceLastUpdate = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (isStopped) {
                    outputStream.close()
                    // NonCancellable: by the time isStopped is true, this
                    // Worker's Job has already been cancelled (that's how
                    // WorkManager signals stop). A plain suspend DB call
                    // here would throw CancellationException immediately
                    // and never actually write PAUSED — which is exactly
                    // why pause was making downloads vanish instead of
                    // pausing. NonCancellable lets this one write complete
                    // regardless.
                    withContext(NonCancellable) {
                        downloadDao.updateProgress(downloadId, downloaded, totalBytes, 0, DownloadEntity.STATUS_PAUSED)
                    }
                    return@withContext Result.retry()
                }

                outputStream.write(buffer, 0, bytesRead)
                downloaded += bytesRead
                bytesSinceLastUpdate += bytesRead

                val now = System.currentTimeMillis()
                val delta = now - lastUpdateMs
                if (delta >= 1000) {
                    // Re-check isStopped right here: cancellation can land between
                    // the loop's top-of-iteration check and this write. Without this,
                    // a periodic "still downloading" write can race the pause button's
                    // PAUSED write and silently overwrite it, making pause look broken.
                    if (isStopped) {
                        outputStream.close()
                        withContext(NonCancellable) {
                            downloadDao.updateProgress(downloadId, downloaded, totalBytes, 0, DownloadEntity.STATUS_PAUSED)
                        }
                        return@withContext Result.retry()
                    }
                    val speed = (bytesSinceLastUpdate * 1000) / delta
                    downloadDao.updateProgress(downloadId, downloaded, totalBytes, speed, DownloadEntity.STATUS_DOWNLOADING)
                    lastUpdateMs = now
                    bytesSinceLastUpdate = 0L
                }
            }

            outputStream.close()
            inputStream.close()

            // --- Download complete: move to public Downloads ---
            val publicUri = moveToPublicDownloads(tempFile, safeFilename, title)
            if (publicUri == null) {
                downloadDao.updateStatus(downloadId, DownloadEntity.STATUS_FAILED)
                return@withContext Result.failure()
            }

            // Mark completed in DB with the public URI
            downloadDao.markCompleted(downloadId, publicUri.toString())
            // Delete temp file
            tempFile.delete()

            Result.success()

        } catch (e: kotlinx.coroutines.CancellationException) {
            // This is the pause/cancel path (WorkManager cancels the Job,
            // which surfaces here as CancellationException). The isStopped
            // branch above already wrote PAUSED via NonCancellable before
            // this could even be thrown. Do NOT treat this as a failure —
            // that was the bug: catching this as a generic Exception and
            // writing STATUS_FAILED silently overwrote PAUSED, which is
            // why paused downloads were disappearing from the Active tab.
            throw e
        } catch (e: java.io.IOException) {
            // Losing internet mid-download throws here (SocketException,
            // UnknownHostException, SocketTimeoutException, ConnectException,
            // SSLException — all IOException subclasses), NOT
            // CancellationException — WorkManager doesn't cancel the Job
            // just because a socket read failed, so the branch above never
            // fires for this. This was landing in the generic catch below
            // and getting marked FAILED, which is why losing connection
            // made a download vanish entirely: getActiveDownloads() only
            // selects QUEUED/DOWNLOADING/PAUSED, so a FAILED row is invisible
            // in both tabs even though it's still sitting in the database —
            // matching "disappears" and "redownloading resumes it" exactly,
            // since the temp file and row were never actually gone.
            //
            // A lost connection is transient and retryable, the same as a
            // manual pause — so it gets the same treatment: PAUSED, not
            // FAILED, and Result.retry() so WorkManager itself re-runs this
            // worker automatically once the network constraint below is
            // satisfied again, on top of the manual Resume button already
            // working the same way it does for a manual pause.
            e.printStackTrace()
            downloadDao.updateStatus(downloadId, DownloadEntity.STATUS_PAUSED)
            Result.retry()
        } catch (e: Exception) {
            e.printStackTrace()
            downloadDao.updateStatus(downloadId, DownloadEntity.STATUS_FAILED)
            Result.failure()
        }
    }

    // Move temp file to public Downloads (Android 10+ uses MediaStore, older uses file copy)
    private fun moveToPublicDownloads(tempFile: File, filename: String, displayName: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, filename)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: return null
            context.contentResolver.openOutputStream(uri)?.use { output ->
                tempFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            } ?: return null
            // Mark as not pending
            val values = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
            context.contentResolver.update(uri, values, null, null)
            uri
        } else {
            // Legacy: copy to external Downloads directory (requires WRITE_EXTERNAL_STORAGE permission)
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val destFile = File(downloadsDir, filename)
            try {
                destFile.outputStream().use { output ->
                    tempFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
                Uri.fromFile(destFile)
            } catch (e: Exception) {
                null
            }
        }
    }

    companion object {
        const val KEY_DOWNLOAD_ID = "download_id"
        const val KEY_MOVIE_ID = "movie_id"
        const val KEY_EPISODE_ID = "episode_id"
        const val KEY_TITLE = "title"
        const val KEY_SEASON_NUM = "season_num"
        const val KEY_EP_NUM = "ep_num"
    }
}

// Helper function to start the download (place this in a separate file or use directly)
fun startDownloadWorker(
    context: Context,
    movie: com.example.data.model.Movie,
    seasonNum: Int?,
    epNum: Int?
) {
    val downloadId = if (seasonNum != null && epNum != null) {
        "${movie.id}_S${seasonNum}E${epNum}"
    } else {
        movie.id
    }
    val episodeId = if (seasonNum != null && epNum != null) "S${seasonNum}E${epNum}" else null

    val data = androidx.work.workDataOf(
        DownloadWorker.KEY_DOWNLOAD_ID to downloadId,
        DownloadWorker.KEY_MOVIE_ID to movie.id,
        DownloadWorker.KEY_EPISODE_ID to episodeId,
        DownloadWorker.KEY_TITLE to movie.title,
        DownloadWorker.KEY_SEASON_NUM to (seasonNum ?: -1),
        DownloadWorker.KEY_EP_NUM to (epNum ?: -1)
    )

    // Without this, WorkManager has no idea the worker even cares about
    // connectivity — it just runs it, the socket read fails mid-transfer,
    // and everything falls to the IOException catch in doWork() to sort
    // out. With it, WorkManager itself won't start (or will hold) this
    // worker while offline, and re-runs it automatically the moment the
    // network comes back — the IOException catch is still there as a
    // backstop for a connection that drops mid-transfer before WorkManager
    // reacts, but this constraint is what makes reconnection automatic
    // instead of requiring the user to tap Resume themselves.
    val constraints = androidx.work.Constraints.Builder()
        .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
        .build()

    val request = androidx.work.OneTimeWorkRequestBuilder<DownloadWorker>()
        .setInputData(data)
        .addTag(downloadId) // for cancellation
        .setConstraints(constraints)
        .build()

    androidx.work.WorkManager.getInstance(context).enqueue(request)
}