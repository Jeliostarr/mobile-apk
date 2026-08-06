package com.example.data.api

import com.example.data.model.CastDetail
import com.example.data.model.FacetsResponse
import com.example.data.model.KeysResponse
import com.example.data.model.MeResponse
import com.example.data.model.StreamTokenResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface YocinemaApi {

    @GET("api/v1/account/me")
    suspend fun getAccountMe(): Response<MeResponse>

    @GET("api/v1/keys")
    suspend fun getKeys(): Response<KeysResponse>

    @GET("api/v1/movies")
    suspend fun getMovies(
        @Query("type") type: String? = null,
        @Query("sort") sort: String? = null, // popular | latest | rating | featured
        @Query("genre") genre: String? = null,
        @Query("vj") vj: String? = null,
        @Query("country") country: String? = null,
        @Query("year") year: String? = null,
        @Query("search") search: String? = null,
        @Query("limit") limit: Int? = 20,
        @Query("page") page: Int? = 1
    ): Response<ResponseBody>

    @GET("api/v1/movies/{id}")
    suspend fun getMovieDetail(
        @Path("id") movieId: String
    ): Response<ResponseBody>

    @GET("api/v1/movies/{id}/episodes")
    suspend fun getMovieEpisodes(
        @Path("id") movieId: String
    ): Response<ResponseBody>

    @GET("api/v1/movies/{id}/related")
    suspend fun getRelatedMovies(
        @Path("id") movieId: String,
        @Query("limit") limit: Int = 12
    ): Response<ResponseBody>

    @GET("api/v1/movies/facets")
    suspend fun getFacets(): Response<FacetsResponse>

    @GET("api/v1/movies/cast/{castId}")
    suspend fun getCastDetail(
        @Path("castId") castId: String
    ): Response<CastDetail>

    @Headers("Content-Type: application/json")
    @POST("api/v1/movies/{id}/report")
    suspend fun reportMovie(
        @Path("id") movieId: String,
        @Body body: Map<String, String>
    ): Response<ResponseBody>

    @POST("api/v1/movies/{id}/stream-token")
    suspend fun mintStreamToken(
        @Path("id") movieId: String
    ): Response<StreamTokenResponse>
}
