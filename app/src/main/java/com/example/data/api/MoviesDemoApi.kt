package com.example.data.api

import com.example.data.model.MdBrowseResponse
import com.example.data.model.MdCaptionsResponse
import com.example.data.model.MdDetails
import com.example.data.model.MdFiltersResponse
import com.example.data.model.MdHomeResponse
import com.example.data.model.MdSeasonsResponse
import com.example.data.model.MdStreamResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Client for the separate movies-demo project (movie-bo-api.vercel.app) —
 * non-translated/original titles, filtered client-side into "Nigerian"
 * (Nollywood) vs everything else. Authenticated the same way as the main
 * YocinemaApi (X-API-Key, via the same AuthInterceptor/TokenManager), but
 * it's a genuinely separate backend with no shared database, so this is a
 * separate Retrofit interface rather than added to YocinemaApi. A 403 here
 * with "not enabled for movies-demo access" means the signed-in key hasn't
 * been granted access — see ApiKeyIssue.MoviesDemoNotEnabled.
 */
interface MoviesDemoApi {

    /**
     * Full home feed — one call returns banners + every ordered section
     * (SUBJECTS_MOVIE, CUSTOM, PLAY_LIST, APPOINTMENT_LIST) with the
     * items already normalized. The MoviesDemoHomeScreen renders these
     * straight as rails, so this replaces the previous fan-out of many
     * browse() calls.
     */
    @GET("api/home")
    suspend fun home(): Response<MdHomeResponse>

    @GET("api/browse")
    suspend fun browse(
        @Query("type") type: String? = null, // "movie" | "tv" | "animation" | "all"
        @Query("genre") genre: String? = null,
        @Query("country") country: String? = null,
        @Query("year") year: String? = null,
        @Query("sort") sort: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<MdBrowseResponse>

    @GET("api/browse")
    suspend fun browseFilters(
        @Query("filters") filters: Boolean = true
    ): Response<MdFiltersResponse>

    @GET("api/trending")
    suspend fun trending(
        @Query("limit") limit: Int = 20
    ): Response<MdBrowseResponse>

    @GET("api/catalog")
    suspend fun catalog(
        @Query("type") type: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<MdBrowseResponse>

    @GET("api/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("limit") limit: Int = 20
    ): Response<MdBrowseResponse>

    @GET("api/details/{detailPath}")
    suspend fun details(
        @Path("detailPath") detailPath: String
    ): Response<MdDetails>

    @GET("api/movie/{detailPath}")
    suspend fun movieStream(
        @Path("detailPath") detailPath: String
    ): Response<MdStreamResponse>

    @GET("api/tv/{detailPath}")
    suspend fun tvStream(
        @Path("detailPath") detailPath: String,
        @Query("season") season: Int,
        @Query("episode") episode: Int
    ): Response<MdStreamResponse>

    @GET("api/seasons/{detailPath}")
    suspend fun seasons(
        @Path("detailPath") detailPath: String
    ): Response<MdSeasonsResponse>

    @GET("api/captions/{detailPath}")
    suspend fun captions(
        @Path("detailPath") detailPath: String,
        @Query("season") season: Int? = null,
        @Query("episode") episode: Int? = null
    ): Response<MdCaptionsResponse>
}