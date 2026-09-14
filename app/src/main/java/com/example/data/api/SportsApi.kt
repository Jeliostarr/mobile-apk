package com.example.data.api

import com.example.data.model.SportsItemEnvelope
import com.example.data.model.SportsLeaguesEnvelope
import com.example.data.model.SportsListEnvelope
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SportsApi {

    @GET("api/sports/leagues")
    suspend fun getLeagues(): Response<SportsLeaguesEnvelope>

    @GET("api/sports/live")
    suspend fun getLive(
        @Query("limit") limit: Int? = null,
        @Query("cursor") cursor: String? = null,
        @Query("sport") sport: String? = null,
        @Query("leagueId") leagueId: String? = null,
    ): Response<SportsListEnvelope>

    @GET("api/sports/schedule")
    suspend fun getSchedule(
        @Query("limit") limit: Int? = null,
        @Query("cursor") cursor: String? = null,
        @Query("sport") sport: String? = null,
        @Query("leagueId") leagueId: String? = null,
        @Query("date") date: String? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
    ): Response<SportsListEnvelope>

    @GET("api/sports/events/{id}")
    suspend fun getEvent(
        @Path("id") id: String,
    ): Response<SportsItemEnvelope>
}