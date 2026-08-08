package com.example.repository

import android.content.Context
import com.example.data.api.AuthInterceptor
import com.example.data.api.ResponseEnvelopeExtractor
import com.example.data.api.YocinemaApi
import com.example.data.local.AppDatabase
import com.example.data.local.DownloadEntity
import com.example.data.local.HistoryEntity
import com.example.data.local.MovieCacheEntity
import com.example.data.local.TokenManager
import com.example.data.local.WatchlistEntity
import com.example.data.model.AccountUser
import com.example.data.model.BASE_URL
import com.example.data.model.CastDetail
import com.example.data.model.Episode
import com.example.data.model.FacetsResponse
import com.example.data.model.Movie
import com.example.data.model.cleanMediaUrl
import com.example.data.player.StreamTokenManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class YocinemaRepository(context: Context) {
    val tokenManager = TokenManager(context)
    private val db = AppDatabase.getInstance(context)
    val watchlistDao = db.watchlistDao()
    val historyDao = db.historyDao()
    val downloadDao = db.downloadDao()
    val movieCacheDao = db.movieCacheDao()

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(tokenManager))
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        })
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val api: YocinemaApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(YocinemaApi::class.java)

    val streamTokenManager = StreamTokenManager(api)

    // In-memory cache for Home screen sections (Stale-While-Revalidate)
    var cachedPopularMovies: List<Movie> = emptyList()
    var cachedLatestMovies: List<Movie> = emptyList()
    var cachedSeriesList: List<Movie> = emptyList()
    var cachedActionMovies: List<Movie> = emptyList()
    var cachedComedyMovies: List<Movie> = emptyList()
    var cachedVjsList: List<String> = emptyList()
    var cachedFacets: FacetsResponse? = null

    val isLoggedInFlow: Flow<Boolean> = tokenManager.apiKeyFlow.map { !it.isNullOrBlank() }

    fun isLoggedIn(): Boolean = !tokenManager.getApiKey().isNullOrBlank()

    suspend fun loginWithKey(key: String): Result<AccountUser> {
        return try {
            tokenManager.saveApiKey(key)
            val response = api.getAccountMe()
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()?.actualUser
                if (user != null) {
                    Result.success(user)
                } else {
                    Result.success(AccountUser(name = "User", email = "", balance = "0"))
                }
            } else {
                val code = response.code()
                val errorBody = response.errorBody()?.string() ?: ""
                tokenManager.clearApiKey()

                val errorMessage = when {
                    code == 401 -> "Invalid API key"
                    code == 403 && (errorBody.contains("exhaust", ignoreCase = true) || errorBody.contains("QUOTA_EXCEEDED", ignoreCase = true)) ->
                        "Daily quota exhausted — resets at midnight UTC"
                    code == 403 -> "Key expired, revoked, or account inactive"
                    code == 429 -> "Rate limit hit"
                    code >= 500 -> "Server temporarily unavailable"
                    else -> "Authentication failed (Code $code)"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            tokenManager.clearApiKey()
            Result.failure(Exception("Could not reach api.yocinema.dpdns.org"))
        }
    }

    suspend fun getAccountMe(): AccountUser? {
        return try {
            val response = api.getAccountMe()
            if (response.isSuccessful) response.body()?.actualUser else null
        } catch (e: Exception) {
            null
        }
    }

    fun logout() {
        tokenManager.clearApiKey()
    }

    private val memoryMovieCache = java.util.concurrent.ConcurrentHashMap<String, List<Movie>>()

    suspend fun getMovies(
        type: String? = null,
        sort: String? = null,
        genre: String? = null,
        vj: String? = null,
        country: String? = null,
        year: String? = null,
        search: String? = null,
        limit: Int = 20,
        page: Int = 1
    ): List<Movie> {
        val cacheKey = "${type}_${sort}_${genre}_${vj}_${country}_${year}_${search}_${limit}_$page"
        val cached = memoryMovieCache[cacheKey]
        if (cached != null && cached.isNotEmpty()) {
            return cached
        }

        return try {
            val response = api.getMovies(type, sort, genre, vj, country, year, search, limit, page)
            var movies = if (response.isSuccessful && response.body() != null) {
                ResponseEnvelopeExtractor.extractMovieList(response.body()!!.string())
            } else emptyList()

            if (movies.isEmpty() && search.isNullOrBlank() && genre.isNullOrBlank() && vj.isNullOrBlank()) {
                val pubResponse = api.getPublicMovies(limit = limit, page = page)
                if (pubResponse.isSuccessful && pubResponse.body() != null) {
                    movies = ResponseEnvelopeExtractor.extractMovieList(pubResponse.body()!!.string())
                }
            }

            if (movies.isNotEmpty()) {
                memoryMovieCache[cacheKey] = movies
                movies.forEach { movie ->
                    if (movie.id.isNotBlank()) {
                        movieCacheDao.cacheMovie(
                            MovieCacheEntity(
                                movieId = movie.id,
                                jsonContent = moshi.adapter(Movie::class.java).toJson(movie)
                            )
                        )
                    }
                }
                movies
            } else {
                loadCachedMoviesFallback()
            }
        } catch (e: Exception) {
            val pubResponse = try {
                api.getPublicMovies(limit = limit, page = page)
            } catch (ex: Exception) { null }

            if (pubResponse?.isSuccessful == true && pubResponse.body() != null) {
                val pubMovies = ResponseEnvelopeExtractor.extractMovieList(pubResponse.body()!!.string())
                if (pubMovies.isNotEmpty()) {
                    memoryMovieCache[cacheKey] = pubMovies
                    return pubMovies
                }
            }
            loadCachedMoviesFallback()
        }
    }

    private suspend fun loadCachedMoviesFallback(): List<Movie> {
        return try {
            val cachedEntities = movieCacheDao.getAllCachedMovies()
            val adapter = moshi.adapter(Movie::class.java)
            cachedEntities.mapNotNull { entity ->
                adapter.fromJson(entity.jsonContent)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getMovieDetail(movieId: String): Movie? {
        return try {
            val response = api.getMovieDetail(movieId)
            if (response.isSuccessful && response.body() != null) {
                val jsonStr = response.body()!!.string()
                val movie = ResponseEnvelopeExtractor.extractSingleMovie(jsonStr)
                if (movie != null && movie.id.isNotBlank()) {
                    movieCacheDao.cacheMovie(
                        MovieCacheEntity(
                            movieId = movie.id,
                            jsonContent = moshi.adapter(Movie::class.java).toJson(movie)
                        )
                    )
                }
                movie ?: getCachedMovie(movieId)
            } else {
                getCachedMovie(movieId)
            }
        } catch (e: Exception) {
            getCachedMovie(movieId)
        }
    }

    private suspend fun getCachedMovie(movieId: String): Movie? {
        return try {
            val entity = movieCacheDao.getCachedMovie(movieId) ?: return null
            moshi.adapter(Movie::class.java).fromJson(entity.jsonContent)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getMovieEpisodes(movieId: String): List<Episode> {
        return try {
            val response = api.getMovieEpisodes(movieId)
            if (response.isSuccessful && response.body() != null) {
                val jsonStr = response.body()!!.string()
                val genericAdapter = moshi.adapter(Any::class.java)
                val jsonObj = genericAdapter.fromJson(jsonStr)
                // Extract episodes
                if (jsonObj is Map<*, *>) {
                    val epData = jsonObj["episodes"] ?: jsonObj["data"]
                    if (epData is List<*>) {
                        val epJson = genericAdapter.toJson(epData)
                        val epAdapter = moshi.adapter<List<Episode>>(
                            com.squareup.moshi.Types.newParameterizedType(List::class.java, Episode::class.java)
                        )
                        return epAdapter.fromJson(epJson) ?: emptyList()
                    }
                } else if (jsonObj is List<*>) {
                    val epAdapter = moshi.adapter<List<Episode>>(
                        com.squareup.moshi.Types.newParameterizedType(List::class.java, Episode::class.java)
                    )
                    return epAdapter.fromJson(jsonStr) ?: emptyList()
                }
            }
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getRelatedMovies(movieId: String): List<Movie> {
        return try {
            val response = api.getRelatedMovies(movieId)
            if (response.isSuccessful && response.body() != null) {
                ResponseEnvelopeExtractor.extractMovieList(response.body()!!.string())
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getFacets(): FacetsResponse {
        return try {
            val response = api.getFacets()
            if (response.isSuccessful) response.body() ?: FacetsResponse() else FacetsResponse()
        } catch (e: Exception) {
            FacetsResponse()
        }
    }

    suspend fun getCastDetail(castId: String): CastDetail? {
        return try {
            val response = api.getCastDetail(castId)
            if (response.isSuccessful && response.body() != null) {
                val jsonStr = response.body()!!.string()
                val genericAdapter = moshi.adapter(Any::class.java)
                val jsonObj = genericAdapter.fromJson(jsonStr)
                if (jsonObj is Map<*, *>) {
                    if (jsonObj.containsKey("name") || jsonObj.containsKey("filmography")) {
                        return moshi.adapter(CastDetail::class.java).fromJson(jsonStr)
                    }
                    val dataObj = jsonObj["data"]
                    if (dataObj is Map<*, *>) {
                        val dataJson = genericAdapter.toJson(dataObj)
                        return moshi.adapter(CastDetail::class.java).fromJson(dataJson)
                    }
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun reportMovie(movieId: String, reason: String): Result<Unit> {
        return try {
            val response = api.reportMovie(movieId, mapOf("reason" to reason))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Failed to send report"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPlayUrl(movie: Movie, seasonNum: Int? = null, epNum: Int? = null, type: String = "translated"): String {
        val rawUrl = if (movie.isSeries && seasonNum != null && epNum != null) {
            cleanMediaUrl(movie.episodes?.find { it.sNum == seasonNum && it.eNum == epNum }?.streamUrl)
                ?: "$BASE_URL/api/v1/movies/${movie.id}/episodes/$seasonNum/$epNum/play?type=$type"
        } else {
            cleanMediaUrl(movie.streamUrl) ?: "$BASE_URL/api/v1/movies/${movie.id}/play?type=$type"
        }
        return streamTokenManager.attachStreamToken(rawUrl, movie.id)
    }

    suspend fun getDownloadMediaUrl(movie: Movie, seasonNum: Int? = null, epNum: Int? = null, type: String = "translated"): String {
        val rawUrl = if (movie.isSeries && seasonNum != null && epNum != null) {
            cleanMediaUrl(movie.episodes?.find { it.sNum == seasonNum && it.eNum == epNum }?.downloadUrl)
                ?: "$BASE_URL/api/v1/movies/${movie.id}/episodes/$seasonNum/$epNum/download?type=$type"
        } else {
            cleanMediaUrl(movie.downloadUrl) ?: "$BASE_URL/api/v1/movies/${movie.id}/download?type=$type"
        }
        return streamTokenManager.attachStreamToken(rawUrl, movie.id)
    }

    // Watchlist
    val watchlist = watchlistDao.getAllWatchlist()

    fun isWatchlisted(movieId: String) = watchlistDao.isWatchlisted(movieId)

    suspend fun toggleWatchlist(movie: Movie) {
        val exists = watchlistDao.isWatchlistedSync(movie.id)
        if (exists) {
            watchlistDao.deleteWatchlist(movie.id)
        } else {
            watchlistDao.insertWatchlist(
                WatchlistEntity(
                    movieId = movie.id,
                    title = movie.title,
                    poster = movie.displayPosterUrl,
                    vjName = movie.vjName,
                    type = if (movie.isSeries) "series" else "movie"
                )
            )
        }
    }

    // History
    val history = historyDao.getAllHistory()

    suspend fun saveHistory(
        movieId: String,
        episodeId: String?,
        title: String,
        poster: String,
        vjName: String?,
        seasonNum: Int?,
        epNum: Int?,
        epTitle: String?,
        positionMs: Long,
        durationMs: Long
    ) {
        val id = "${movieId}_${episodeId ?: "movie"}"
        historyDao.saveHistory(
            HistoryEntity(
                id = id,
                movieId = movieId,
                episodeId = episodeId,
                title = title,
                poster = poster,
                vjName = vjName,
                seasonNumber = seasonNum,
                episodeNumber = epNum,
                episodeTitle = epTitle,
                positionMs = positionMs,
                durationMs = durationMs,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    // Downloads
    val allDownloads = downloadDao.getAllDownloads()
    val activeDownloads = downloadDao.getActiveDownloads()
    val completedDownloads = downloadDao.getCompletedDownloads()

    suspend fun deleteDownload(downloadId: String) {
        downloadDao.deleteDownload(downloadId)
    }

    suspend fun reportViewProgress(movieId: String, viewerId: String, watchedSeconds: Long): Boolean {
        return try {
            val response = api.postViewProgress(movieId, com.example.data.model.ViewProgressRequest(viewerId, watchedSeconds))
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
