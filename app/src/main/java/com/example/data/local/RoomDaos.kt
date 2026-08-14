package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    fun getAllWatchlist(): Flow<List<WatchlistEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE movieId = :movieId)")
    fun isWatchlisted(movieId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE movieId = :movieId)")
    suspend fun isWatchlistedSync(movieId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchlist(item: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE movieId = :movieId")
    suspend fun deleteWatchlist(movieId: String)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY updatedAt DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE id = :id LIMIT 1")
    suspend fun getHistoryById(id: String): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveHistory(history: HistoryEntity)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteHistory(id: String)
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY updatedAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status IN ('QUEUED', 'DOWNLOADING', 'PAUSED') ORDER BY updatedAt DESC")
    fun getActiveDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED' ORDER BY updatedAt DESC")
    fun getCompletedDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE downloadId = :downloadId LIMIT 1")
    suspend fun getDownloadById(downloadId: String): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE downloadId = :downloadId LIMIT 1")
    fun getDownloadFlowById(downloadId: String): Flow<DownloadEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDownload(download: DownloadEntity)

    @Query("UPDATE downloads SET downloadedBytes = :downloadedBytes, totalBytes = :totalBytes, speedBytesPerSec = :speed, status = :status, updatedAt = :updatedAt WHERE downloadId = :downloadId")
    suspend fun updateProgress(downloadId: String, downloadedBytes: Long, totalBytes: Long, speed: Long, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE downloads SET status = :status WHERE downloadId = :downloadId")
    suspend fun updateStatus(downloadId: String, status: String)

    @Query("UPDATE downloads SET localFilePath = :path, downloadedAt = :time, status = 'COMPLETED', updatedAt = :updatedAt WHERE downloadId = :downloadId")
    suspend fun markCompleted(downloadId: String, path: String, time: Long = System.currentTimeMillis(), updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM downloads WHERE downloadId = :downloadId")
    suspend fun deleteDownload(downloadId: String)
}

// ========== ADD THIS MISSING DAO ==========
@Dao
interface MovieCacheDao {
    @Query("SELECT * FROM movie_cache WHERE movieId = :movieId LIMIT 1")
    suspend fun getCachedMovie(movieId: String): MovieCacheEntity?

    @Query("SELECT * FROM movie_cache ORDER BY updatedAt DESC LIMIT 50")
    suspend fun getAllCachedMovies(): List<MovieCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun cacheMovie(cache: MovieCacheEntity)

    @Query("DELETE FROM movie_cache WHERE movieId = :movieId")
    suspend fun deleteCachedMovie(movieId: String)

    @Query("DELETE FROM movie_cache WHERE updatedAt < :cutoff")
    suspend fun clearStale(cutoff: Long)
}