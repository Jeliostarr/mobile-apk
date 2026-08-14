package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val movieId: String,
    val title: String,
    val poster: String,
    val vjName: String?,
    val type: String?, // "movie" or "series"
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val id: String, // "$movieId_${episodeId ?: "movie"}"
    val movieId: String,
    val episodeId: String? = null,
    val title: String,
    val poster: String,
    val vjName: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val episodeTitle: String? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val downloadId: String, // "$movieId_${episodeId ?: "movie"}"
    val movieId: String,
    val episodeId: String? = null,
    val title: String,
    val posterUrl: String? = null,
    val vjName: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val episodeTitle: String? = null,
    val filename: String,
    val localFilePath: String? = null,
    val tempFilePath: String? = null,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val status: String = STATUS_QUEUED,
    val speedBytesPerSec: Long = 0L,
    val downloadedAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_QUEUED = "QUEUED"
        const val STATUS_DOWNLOADING = "DOWNLOADING"
        const val STATUS_PAUSED = "PAUSED"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_FAILED = "FAILED"
    }
}

@Entity(tableName = "movie_cache")
data class MovieCacheEntity(
    @PrimaryKey val movieId: String,
    val jsonContent: String,
    val updatedAt: Long = System.currentTimeMillis()
)