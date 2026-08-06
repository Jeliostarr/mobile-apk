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
    val filename: String,
    val localFilePath: String,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val status: String = "QUEUED", // QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED
    val speedBytesPerSec: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "movie_cache")
data class MovieCacheEntity(
    @PrimaryKey val movieId: String,
    val jsonContent: String,
    val updatedAt: Long = System.currentTimeMillis()
)
