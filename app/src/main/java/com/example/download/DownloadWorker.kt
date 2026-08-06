package com.example.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.local.DownloadEntity
import com.example.repository.YocinemaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
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

        val movie = repository.getMovieDetail(movieId) ?: return@withContext Result.failure()
        val downloadUrl = repository.getDownloadMediaUrl(movie, seasonNum, epNum)

        val storageDir = context.getExternalFilesDir(null) ?: context.filesDir
        val safeFilename = "${title.replace(Regex("[^a-zA-Z0-9._-]"), "_")}.mp4"
        val targetFile = File(storageDir, safeFilename)

        var existingBytes = if (targetFile.exists()) targetFile.length() else 0L

        var entity = downloadDao.getDownloadById(downloadId)
        if (entity == null) {
            entity = DownloadEntity(
                downloadId = downloadId,
                movieId = movieId,
                episodeId = episodeId,
                title = title,
                filename = safeFilename,
                localFilePath = targetFile.absolutePath,
                totalBytes = 0L,
                downloadedBytes = existingBytes,
                status = "DOWNLOADING",
                speedBytesPerSec = 0L
            )
            downloadDao.saveDownload(entity)
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
                downloadDao.updateProgress(downloadId, existingBytes, entity.totalBytes, 0, "FAILED")
                return@withContext Result.failure()
            }

            val body = response.body ?: run {
                downloadDao.updateProgress(downloadId, existingBytes, entity.totalBytes, 0, "FAILED")
                return@withContext Result.failure()
            }

            val contentLength = body.contentLength()
            val totalBytes = if (response.code == 206) existingBytes + contentLength else contentLength

            val raf = RandomAccessFile(targetFile, "rw")
            if (response.code == 206) {
                raf.seek(existingBytes)
            } else {
                raf.setLength(0)
                existingBytes = 0
            }

            val inputStream = body.byteStream()
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var downloaded = existingBytes
            var lastUpdateMs = System.currentTimeMillis()
            var bytesSinceLastUpdate = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (isStopped) {
                    raf.close()
                    downloadDao.updateProgress(downloadId, downloaded, totalBytes, 0, "PAUSED")
                    return@withContext Result.retry()
                }

                raf.write(buffer, 0, bytesRead)
                downloaded += bytesRead
                bytesSinceLastUpdate += bytesRead

                val now = System.currentTimeMillis()
                val delta = now - lastUpdateMs
                if (delta >= 1000) {
                    val speed = (bytesSinceLastUpdate * 1000) / delta
                    downloadDao.updateProgress(downloadId, downloaded, totalBytes, speed, "DOWNLOADING")
                    lastUpdateMs = now
                    bytesSinceLastUpdate = 0L
                }
            }

            raf.close()
            downloadDao.updateProgress(downloadId, downloaded, totalBytes, 0, "COMPLETED")
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            downloadDao.updateProgress(downloadId, existingBytes, entity.totalBytes, 0, "FAILED")
            Result.failure()
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
