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

    /**
     * Result of a single page fetch. `items` is the current page;
     * `hasMore`/`nextCursor` tell the caller whether to loop.
     */
    data class LivePage(
        val items: List<Match>,
        val hasMore: Boolean,
        val nextCursor: String?,
    )

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

    /**
     * Single-page fetch that preserves pagination metadata. Does NOT go
     * through the 20s cache — paging loops need fresh cursors and mixing
     * cached and fresh pages produces confusing duplicates. Callers that
     * want caching should use getLive().
     */
    suspend fun getLivePage(
        limit: Int = 50,
        cursor: String? = null,
        leagueId: String? = null,
    ): LivePage = runCatching {
        val res = api.getLive(limit = limit, cursor = cursor, leagueId = leagueId)
        if (!res.isSuccessful) return@runCatching LivePage(emptyList(), false, null)
        val body = res.body() ?: return@runCatching LivePage(emptyList(), false, null)
        LivePage(
            items = body.items.map { it.toDomain() },
            hasMore = body.hasMore,
            nextCursor = body.nextCursor,
        )
    }.getOrDefault(LivePage(emptyList(), false, null))

    /**
     * Fetches every live-match page up to a hard cap. Use when the UI
     * wants the complete live list rather than the first page.
     *
     * Safety: caps at `maxPages` iterations so a server bug that keeps
     * returning the same cursor can't loop forever.
     */
    suspend fun getAllLive(
        leagueId: String? = null,
        pageLimit: Int = 50,
        maxPages: Int = 20,
    ): List<Match> {
        val all = mutableListOf<Match>()
        val seen = HashSet<String>()
        var cursor: String? = null
        var pages = 0
        while (pages < maxPages) {
            val page = getLivePage(limit = pageLimit, cursor = cursor, leagueId = leagueId)
            for (m in page.items) {
                if (seen.add(m.id)) all += m
            }
            if (!page.hasMore || page.nextCursor.isNullOrBlank()) break
            cursor = page.nextCursor
            pages++
        }
        return all
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