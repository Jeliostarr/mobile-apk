package com.example.repository

import android.content.Context
import com.example.data.api.ApiIssueClassifier
import com.example.data.api.ApiIssueReporter
import com.example.data.api.ApiKeyIssue
import com.example.data.api.AuthInterceptor
import com.example.data.api.ResponseEnvelopeExtractor
import com.example.data.api.MoviesDemoApi
import com.example.data.api.SportsApi
import com.example.data.api.YocinemaApi
import com.example.data.local.AppDatabase
import com.example.data.local.DownloadEntity
import com.example.data.local.HistoryEntity
import com.example.data.local.MovieCacheEntity
import com.example.data.local.TokenManager
import com.example.data.local.WatchlistEntity
import com.example.data.model.AccountUser
import com.example.data.model.AppUpdateState
import com.example.data.model.AppVersionResponse
import com.example.data.model.BASE_URL
import com.example.data.model.MOVIES_DEMO_BASE_URL
import com.example.data.model.CastDetail
import com.example.data.model.Episode
import com.example.data.model.FacetsResponse
import com.example.data.model.KeyInfo
import com.example.data.model.MdFiltersResponse
import com.example.data.model.MdSubject
import com.example.data.model.Movie
import com.example.data.model.UsageResponse
import com.example.data.model.cleanMediaUrl
import com.example.data.player.StreamTokenManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/** Snapshot of a single rail on the movies-demo home screen. */
data class MdHomeRail(
    val title: String,
    val genre: String?,
    val sort: String?,
    val items: List<MdSubject>,
)

data class AccountBundle(
    val user: AccountUser? = null,
    val keys: List<KeyInfo> = emptyList(),
    val usage: UsageResponse? = null,
    val isStale: Boolean = false,
    val lastLoadedAt: Long = 0L
) {
    val activeKeyUsage: KeyInfo?
        get() = null
}

class YocinemaRepository(context: Context) {
    val tokenManager = TokenManager(context)
    private val db = AppDatabase.getInstance(context)
    val watchlistDao = db.watchlistDao()
    val historyDao = db.historyDao()
    val downloadDao = db.downloadDao()
    val movieCacheDao = db.movieCacheDao()

    val apiKeyIssueFlow: StateFlow<ApiKeyIssue?> = ApiIssueReporter.issueFlow
    fun clearApiKeyIssue() = ApiIssueReporter.clear()

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

    val moviesDemoApi: MoviesDemoApi = Retrofit.Builder()
        .baseUrl(MOVIES_DEMO_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(MoviesDemoApi::class.java)

    // Caching wrapper. Screens should call this instead of moviesDemoApi
    // directly, so back navigation doesn't re-hit the network.
    val moviesDemoRepository = MoviesDemoRepository(moviesDemoApi)

    val sportsApi: SportsApi = Retrofit.Builder()
        .baseUrl(MOVIES_DEMO_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(SportsApi::class.java)

    val sportsRepository = SportsRepository(sportsApi)

    val streamTokenManager = StreamTokenManager(api)

    // ─── Main Home cache (VJ movies, hero carousel, sports rail) ───

    var cachedPopularMovies: List<Movie> = emptyList()
    var cachedLatestMovies: List<Movie> = emptyList()
    var cachedSeriesList: List<Movie> = emptyList()
    var cachedGenreMovies: Map<String, List<Movie>> = emptyMap()
    var cachedVjsList: List<String> = emptyList()
    var cachedFacets: FacetsResponse? = null
    var cachedHeroMovies: List<Movie> = emptyList()

    private val homeCacheTtlMs = 3 * 60_000L
    private var homeCacheTimestamp = 0L

    fun isHomeCacheFresh(): Boolean =
        cachedPopularMovies.isNotEmpty() &&
        (System.currentTimeMillis() - homeCacheTimestamp) < homeCacheTtlMs

    fun markHomeCacheFresh() {
        homeCacheTimestamp = System.currentTimeMillis()
    }

    fun invalidateHomeCache() {
        homeCacheTimestamp = 0L
    }

    // ─── Movies-demo Home cache ───
    // Preserved across navigation so the shimmer only shows on the
    // very first cold open of the screen.

    var cachedMdHomeHero: List<MdSubject> = emptyList()
    var cachedMdHomeRails: List<MdHomeRail> = emptyList()
    var cachedMdHomeFilters: MdFiltersResponse? = null

    private val mdHomeCacheTtlMs = 3 * 60_000L
    private var mdHomeCacheTimestamp = 0L

    fun isMdHomeCacheFresh(): Boolean =
        (cachedMdHomeHero.isNotEmpty() || cachedMdHomeRails.isNotEmpty()) &&
        (System.currentTimeMillis() - mdHomeCacheTimestamp) < mdHomeCacheTtlMs

    fun markMdHomeCacheFresh() {
        mdHomeCacheTimestamp = System.currentTimeMillis()
    }

    fun invalidateMdHomeCache() {
        mdHomeCacheTimestamp = 0L
    }

    val isLoggedInFlow: Flow<Boolean> = tokenManager.apiKeyFlow.map { !it.isNullOrBlank() }

    fun isLoggedIn(): Boolean = !tokenManager.getApiKey().isNullOrBlank()

    suspend fun loginWithKey(key: String): Result<AccountUser> {
        return try {
            tokenManager.saveApiKey(key)
            ApiIssueReporter.suppressed = true
            val response = try { api.getAccountMe() } finally {
                ApiIssueReporter.suppressed = false
            }
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()?.actualUser
                Result.success(user ?: AccountUser(name = "User", email = "", balance = 0.0))
            } else {
                val code = response.code()
                val errorBody = response.errorBody()?.string() ?: ""
                tokenManager.clearApiKey()
                val classified = ApiIssueClassifier.classify(code, errorBody)
                val errorMessage = when (classified) {
                    is ApiKeyIssue.Missing -> "No API key was provided"
                    is ApiKeyIssue.Invalid -> "Invalid API key"
                    is ApiKeyIssue.Revoked -> "This key has been revoked"
                    is ApiKeyIssue.Deleted -> "This key no longer exists"
                    is ApiKeyIssue.Paused -> "This key is paused"
                    is ApiKeyIssue.Suspended -> "This key is suspended"
                    is ApiKeyIssue.Expired -> "This key has expired"
                    is ApiKeyIssue.AccountInactive -> "Your account is inactive"
                    is ApiKeyIssue.RateLimited -> "Daily quota exhausted — resets at midnight UTC"
                    is ApiKeyIssue.MoviesDemoNotEnabled -> classified.message
                    is ApiKeyIssue.Other -> classified.detail
                    null -> if (code >= 500) "Server temporarily unavailable" else "Authentication failed (Code $code)"
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

    suspend fun checkAppVersion(): AppVersionResponse? {
        return try {
            val response = api.getAppVersion()
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            null
        }
    }

    fun classifyAppUpdate(currentVersionCode: Int, response: AppVersionResponse): AppUpdateState {
        val latest = response.actualLatestVersionCode
        val minSupported = response.actualMinSupportedVersionCode
        val versionName = response.actualLatestVersionName
        val releaseNotes = response.actualReleaseNotes
        val apkUrl = response.actualApkUrl
        return when {
            currentVersionCode < minSupported || response.actualForceUpdate ->
                AppUpdateState.Required(versionName, releaseNotes, apkUrl)
            currentVersionCode < latest ->
                AppUpdateState.Optional(versionName, releaseNotes, apkUrl)
            else -> AppUpdateState.UpToDate
        }
    }

    // ─── Account bundle ───

    private val accountCacheTtlMs = 90_000L
    private var accountCacheTimestamp = 0L
    private val _accountBundle = MutableStateFlow(AccountBundle())
    val accountBundleFlow: StateFlow<AccountBundle> = _accountBundle

    suspend fun getAccountBundle(forceRefresh: Boolean = false): AccountBundle {
        val now = System.currentTimeMillis()
        val cached = _accountBundle.value
        val isFresh = cached.lastLoadedAt > 0 && (now - accountCacheTimestamp) < accountCacheTtlMs
        if (!forceRefresh && isFresh) return cached
        val user = getAccountMe()
        val keys = getKeys()
        val usage = getUsage()
        val bundle = AccountBundle(user = user, keys = keys, usage = usage, isStale = false, lastLoadedAt = now)
        accountCacheTimestamp = now
        _accountBundle.value = bundle
        return bundle
    }

    fun getCachedAccountBundle(): AccountBundle? =
        _accountBundle.value.takeIf { it.lastLoadedAt > 0 }

    fun invalidateAccountCache() {
        accountCacheTimestamp = 0L
    }

    suspend fun getKeys(): List<KeyInfo> {
        return try {
            val response = api.getKeys()
            if (response.isSuccessful) response.body()?.actualKeys ?: emptyList() else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getUsage(days: Int = 7): UsageResponse? {
        return try {
            val response = api.getUsage(days)
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            null
        }
    }

    fun switchActiveKey(newKey: String) {
        tokenManager.saveApiKey(newKey)
        invalidateAccountCache()
        invalidateHomeCache()
        invalidateMdHomeCache()
        _accountBundle.value = AccountBundle()
        ApiIssueReporter.clear()
        moviesDemoRepository.clearCache()
        sportsRepository.clearCache()
    }

    fun logout() {
        tokenManager.clearApiKey()
        invalidateAccountCache()
        invalidateHomeCache()
        invalidateMdHomeCache()
        _accountBundle.value = AccountBundle()
        ApiIssueReporter.clear()
        moviesDemoRepository.clearCache()
        sportsRepository.clearCache()
    }

    // ─── Movie list / detail (VJ catalog) ───

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
        page: Int = 1,
        forceRefresh: Boolean = false
    ): List<Movie> {
        val cacheKey = "movies_${type}_${sort}_${genre}_${vj}_${country}_${year}_${search}_${limit}_$page"
        if (!forceRefresh) {
            val cached = memoryMovieCache[cacheKey]
            if (cached != null && cached.isNotEmpty()) return cached
        }
        return try {
            val response = api.getMovies(type, sort, genre, vj, country, year, search, limit, page)
            val movies = if (response.isSuccessful && response.body() != null) {
                ResponseEnvelopeExtractor.extractMovieList(response.body()!!.string())
            } else {
                emptyList()
            }

            if (movies.isNotEmpty()) {
                memoryMovieCache[cacheKey] = movies
                movies.forEach { movie ->
                    if (movie.id.isNotBlank()) {
                        movieCacheDao.cacheMovie(
                            MovieCacheEntity(movieId = movie.id, jsonContent = moshi.adapter(Movie::class.java).toJson(movie))
                        )
                    }
                }
                movies
            } else {
                if (search.isNullOrBlank() && genre.isNullOrBlank() && vj.isNullOrBlank()) {
                    val pubResponse = api.getPublicMovies(limit = limit, page = page)
                    if (pubResponse.isSuccessful && pubResponse.body() != null) {
                        ResponseEnvelopeExtractor.extractMovieList(pubResponse.body()!!.string())
                    } else {
                        loadCachedMoviesFallback()
                    }
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            if (search.isNullOrBlank() && genre.isNullOrBlank() && vj.isNullOrBlank()) {
                val pubResponse = try { api.getPublicMovies(limit = limit, page = page) } catch (ex: Exception) { null }
                if (pubResponse?.isSuccessful == true && pubResponse.body() != null) {
                    ResponseEnvelopeExtractor.extractMovieList(pubResponse.body()!!.string())
                } else {
                    loadCachedMoviesFallback()
                }
            } else {
                emptyList()
            }
        }
    }

    private suspend fun loadCachedMoviesFallback(): List<Movie> {
        return try {
            val cachedEntities = movieCacheDao.getAllCachedMovies()
            val adapter = moshi.adapter(Movie::class.java)
            cachedEntities.mapNotNull { entity -> adapter.fromJson(entity.jsonContent) }
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
                        MovieCacheEntity(movieId = movie.id, jsonContent = moshi.adapter(Movie::class.java).toJson(movie))
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

    suspend fun getCachedMovieDetail(movieId: String): Movie? = getCachedMovie(movieId)

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
                if (jsonObj is Map<*, *>) {
                    val epData = jsonObj["episodes"] ?: jsonObj["data"]
                    if (epData is List<*>) {
                        val epJson = genericAdapter.toJson(epData)
                        val epAdapter = moshi.adapter<List<Episode>>(
                            com.squareup.moshi.Types.newParameterizedType(List::class.java, Episode::class.java)
                        )
                        epAdapter.fromJson(epJson) ?: emptyList()
                    } else {
                        emptyList()
                    }
                } else if (jsonObj is List<*>) {
                    val epAdapter = moshi.adapter<List<Episode>>(
                        com.squareup.moshi.Types.newParameterizedType(List::class.java, Episode::class.java)
                    )
                    epAdapter.fromJson(jsonStr) ?: emptyList()
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getRelatedMovies(movieId: String): List<Movie> {
        return try {
            val response = api.getRelatedMovies(movieId)
            if (response.isSuccessful && response.body() != null) {
                ResponseEnvelopeExtractor.extractMovieList(response.body()!!.string())
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getFacets(): FacetsResponse {
        return try {
            val response = api.getFacets()
            if (response.isSuccessful && response.body() != null) {
                val jsonStr = response.body()!!.string()
                val genericAdapter = moshi.adapter(Any::class.java)
                val jsonObj = genericAdapter.fromJson(jsonStr)
                if (jsonObj is Map<*, *>) {
                    val dataObj = jsonObj["data"]
                    if (dataObj is Map<*, *>) {
                        val dataJson = genericAdapter.toJson(dataObj)
                        moshi.adapter(FacetsResponse::class.java).fromJson(dataJson) ?: FacetsResponse()
                    } else if (jsonObj.containsKey("genres") || jsonObj.containsKey("vjs")) {
                        moshi.adapter(FacetsResponse::class.java).fromJson(jsonStr) ?: FacetsResponse()
                    } else {
                        FacetsResponse()
                    }
                } else {
                    FacetsResponse()
                }
            } else {
                FacetsResponse()
            }
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
                        moshi.adapter(CastDetail::class.java).fromJson(jsonStr)
                    } else {
                        val dataObj = jsonObj["data"]
                        if (dataObj is Map<*, *>) {
                            moshi.adapter(CastDetail::class.java).fromJson(genericAdapter.toJson(dataObj))
                        } else {
                            null
                        }
                    }
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
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

    suspend fun requestCastMovie(
        castId: String,
        tmdbId: String,
        mediaType: String,
        title: String,
        poster: String? = null
    ): Result<Unit> {
        return try {
            val body = mapOf(
                "tmdbId" to tmdbId,
                "mediaType" to mediaType,
                "title" to title,
                "poster" to (poster ?: "")
            )
            val response = api.requestCastMovie(castId, body)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Request failed"))
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

    // ─── Watchlist ───
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

    // ─── History ───
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
        try {
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ─── Downloads ───
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