package com.example.repository

import com.example.data.api.SportsApi
import com.example.data.model.Match
import com.example.data.model.SportsLeagueDto
import com.example.data.model.toDomain

/**
 * Sports data — always fetched fresh. No caching: match status and
 * stream URLs change by the minute, and worker tokens expire.
 *
 * The API returns worker URLs already, so the app plays them directly.
 */
class SportsRepository(private val api: SportsApi) {

    suspend fun getLeagues(): List<SportsLeagueDto> = runCatching {
        val res = api.getLeagues()
        if (res.isSuccessful) res.body()?.items ?: emptyList() else emptyList()
    }.getOrDefault(emptyList())

    suspend fun getLive(limit: Int = 20, leagueId: String? = null): List<Match> = runCatching {
        val res = api.getLive(limit = limit, leagueId = leagueId)
        if (res.isSuccessful) res.body()?.items?.map { it.toDomain() } ?: emptyList() else emptyList()
    }.getOrDefault(emptyList())

    suspend fun getSchedule(
        limit: Int = 30,
        leagueId: String? = null,
        date: String? = null,
    ): List<Match> = runCatching {
        val res = api.getSchedule(limit = limit, leagueId = leagueId, date = date)
        if (res.isSuccessful) res.body()?.items?.map { it.toDomain() } ?: emptyList() else emptyList()
    }.getOrDefault(emptyList())

    suspend fun getMatch(id: String): Match? = runCatching {
        val res = api.getEvent(id)
        if (res.isSuccessful) res.body()?.item?.toDomain() else null
    }.getOrNull()
}