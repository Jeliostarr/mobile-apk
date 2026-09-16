package com.example.repository

import com.example.data.api.SportsApi
import com.example.data.cache.ApiCache
import com.example.data.model.Match
import com.example.data.model.SportsLeagueDto
import com.example.data.model.toDomain

/**
 * Caching wrapper around SportsApi.
 *
 * Sports changes faster than a movie catalog, so TTLs are shorter:
 *
 *   - Leagues: 1 hour (fixed list, changes ~never)
 *   - Live matches: 20 seconds — needs to feel current
 *   - Schedule: 3 minutes
 *   - Match detail: 45 seconds
 */
class SportsRepository(
    private val api: SportsApi,
    private val cache: ApiCache = ApiCache(),
) {

    suspend fun getLeagues(forceRefresh: Boolean = false): List<SportsLeagueDto> =
        cache.get("sports.leagues", 3600, forceRefresh) {
            runCatching {
                val res = api.getLeagues()
                if (res.isSuccessful) res.body()?.items ?: emptyList() else emptyList()
            }.getOrDefault(emptyList())
        }

    suspend fun getLive(
        limit: Int = 20,
        leagueId: String? = null,
        forceRefresh: Boolean = false,
    ): List<Match> =
        cache.get("sports.live.$limit.$leagueId", 20, forceRefresh) {
            runCatching {
                val res = api.getLive(limit = limit, leagueId = leagueId)
                if (res.isSuccessful) res.body()?.items?.map { it.toDomain() } ?: emptyList()
                else emptyList()
            }.getOrDefault(emptyList())
        }

    suspend fun getSchedule(
        limit: Int = 30,
        leagueId: String? = null,
        date: String? = null,
        forceRefresh: Boolean = false,
    ): List<Match> =
        cache.get("sports.schedule.$limit.$leagueId.$date", 180, forceRefresh) {
            runCatching {
                val res = api.getSchedule(limit = limit, leagueId = leagueId, date = date)
                if (res.isSuccessful) res.body()?.items?.map { it.toDomain() } ?: emptyList()
                else emptyList()
            }.getOrDefault(emptyList())
        }

    suspend fun getMatch(
        id: String,
        forceRefresh: Boolean = false,
    ): Match? =
        cache.get("sports.match.$id", 45, forceRefresh) {
            runCatching {
                val res = api.getEvent(id)
                if (res.isSuccessful) res.body()?.item?.toDomain() else null
            }.getOrNull()
        }

    fun clearCache() = cache.clear()
}