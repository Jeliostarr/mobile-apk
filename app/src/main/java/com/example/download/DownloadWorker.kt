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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

        // Serializes every doWork() call for the same downloadId within
        // this process. enqueueUniqueWork(REPLACE) (see startDownloadWorker
        // below) stops WorkManager from ever INTENDING to run two workers
        // for the same download, but there's still a small timing gap
        // between "WorkManager cancels the old worker" and "that worker's
        // coroutine actually notices isStopped and returns" — without this
        // lock, a resume tapped inside that gap could still start a second
        // doWork() that opens the same temp file while the first one is
        // mid-write. This lock makes that second call simply wait its turn
        // instead of racing.
        val mutex = downloadMutexes.getOrPut(downloadId) { Mutex() }
        mutex.withLock {
            // A worker that was queued behind the lock above can wake up
            // AFTER the download already finished legitimately (e.g. the
            // holder of the lock just completed it) — without this check
            // it would still be a "zombie" that goes on to re-run the
            // download loop or, worse, eventually write a non-COMPLETED
            // status over top of a row that already finished correctly.
            // This is exactly what was making a fully completed download
            // disappear from the Completed tab.
            val alreadyDone = downloadDao.getDownloadById(downloadId)
            if (alreadyDone?.status == DownloadEntity.STATUS_COMPLETED) {
                return@withLock Result.success()
            }

            runDownload(downloadId, movieId, episodeId, title, seasonNum, epNum, repository, downloadDao)
        }
    }

    private suspend fun runDownload(
        downloadId: String,
        movieId: String,
        episodeId: String?,
        title: String,
        seasonNum: Int?,
        epNum: Int?,
        repository: YocinemaRepository,
        downloadDao: com.example.data.local.DownloadDao
    ): Result {
        // Get movie details (for metadata)
        val movie = repository.getMovieDetail(movieId)
        if (movie == null) {
            downloadDao.updateStatus(downloadId, DownloadEntity.STATUS_FAILED)
            return Result.failure()
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
                return Result.failure()
            }

            val body = response.body ?: run {
                downloadDao.updateStatus(downloadId, DownloadEntity.STATUS_FAILED)
                return Result.failure()
            }

            val contentLength = body.contentLength()
            // Prefer the server's own authoritative total from the
            // Content-Range header ("bytes start-end/total") over
            // existingBytes + contentLength — the local calculation is
            // only as good as existingBytes being exactly right at this
            // instant, and while the lock above should now guarantee
            // that, reading the number the server itself reports removes
            // the dependency on local file state entirely as a second,
            // independent safety net.
            val totalBytes = if (response.code == 206) {
                response.header("Content-Range")
                    ?.substringAfterLast('/')
                    ?.toLongOrNull()
                    ?: (existingBytes + contentLength)
            } else {
                contentLength
            }
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
                    return Result.retry()
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
                        return Result.retry()
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
                return Result.failure()
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

        // One Mutex per active downloadId, shared across every DownloadWorker
        // instance in this process — see the comment in doWork() for why
        // this exists on top of enqueueUniqueWork(REPLACE). Entries are
        // never explicitly removed; ConcurrentHashMap.getOrPut means a
        // stale Mutex for a finished download just sits unused (cheap,
        // just a handful of bytes) rather than needing careful cleanup
        // that could itself race with a new download starting.
        private val downloadMutexes = java.util.concurrent.ConcurrentHashMap<String, Mutex>()
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

    // enqueueUniqueWork + REPLACE, not plain enqueue(). This is the actual
    // root fix for the "many pause/resume actions corrupt the file"
    // bug: plain enqueue() let every Resume tap start a BRAND NEW worker
    // instance even if the previous one hadn't finished shutting down
    // yet, and with nothing stopping two workers from opening the same
    // temp file in append mode at once, rapid pause/resume cycles could
    // spawn several workers all writing to the same file concurrently —
    // that's what was corrupting the file and making its size jump
    // around. REPLACE tells WorkManager there must only ever be one
    // worker running under this downloadId; a new request cancels
    // whatever was there first. See the in-worker Mutex in
    // DownloadWorker.doWork() for the second half of this fix — it closes
    // the small remaining timing gap between WorkManager cancelling the
    // old worker and that worker's coroutine actually finishing.
    androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
        downloadId,
        androidx.work.ExistingWorkPolicy.REPLACE,
        request
    )
}