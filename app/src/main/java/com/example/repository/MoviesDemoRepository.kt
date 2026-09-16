package com.example.repository

import com.example.data.api.MoviesDemoApi
import com.example.data.cache.ApiCache
import com.example.data.model.MdBrowseResponse
import com.example.data.model.MdCaptionsResponse
import com.example.data.model.MdDetails
import com.example.data.model.MdFiltersResponse
import com.example.data.model.MdHomeResponse
import com.example.data.model.MdSeasonsResponse
import com.example.data.model.MdStreamResponse

/**
 * Caching wrapper around MoviesDemoApi.
 *
 * TTLs are picked per endpoint based on how fast the data actually
 * changes:
 *
 *   - Images/details/seasons/captions: 15-30 min — catalog doesn't
 *     change second-to-second, and every fetch burns API-key quota.
 *   - Stream URLs: 60 seconds — the URL is signed with a long expiry,
 *     so re-fetching every time is pure waste. 60s is generous
 *     headroom for the user opening a title twice in quick succession.
 *   - Home feed: 5 min — the upstream home rarely changes more often
 *     than that, and it's the single most expensive call on this repo.
 *   - Search: NOT cached — user typed it, they expect live results.
 */
class MoviesDemoRepository(
    private val api: MoviesDemoApi,
    private val cache: ApiCache = ApiCache(),
) {

    // ─── Home ───

    /**
     * Full home feed — banners + every ordered section in one request.
     * Replaces the previous client-side fan-out of many browse() calls
     * to assemble rails for MoviesDemoHomeScreen.
     */
    suspend fun home(
        forceRefresh: Boolean = false,
    ): MdHomeResponse? {
        return cache.get("md.home", 300, forceRefresh) {
            runCatching { api.home().body() }.getOrNull()
        }
    }

    // ─── Browse ───
    suspend fun browse(
        type: String? = null,
        genre: String? = null,
        country: String? = null,
        year: String? = null,
        sort: String? = null,
        page: Int = 1,
        limit: Int = 20,
        forceRefresh: Boolean = false,
    ): MdBrowseResponse? {
        val key = "md.browse.$type.$genre.$country.$year.$sort.$page.$limit"
        return cache.get(key, 300, forceRefresh) {
            runCatching { api.browse(type, genre, country, year, sort, page, limit).body() }.getOrNull()
        }
    }

    suspend fun browseFilters(
        forceRefresh: Boolean = false,
    ): MdFiltersResponse? {
        return cache.get("md.filters", 1800, forceRefresh) {
            runCatching { api.browseFilters(filters = true).body() }.getOrNull()
        }
    }

    suspend fun trending(
        limit: Int = 20,
        forceRefresh: Boolean = false,
    ): MdBrowseResponse? {
        return cache.get("md.trending.$limit", 300, forceRefresh) {
            runCatching { api.trending(limit).body() }.getOrNull()
        }
    }

    suspend fun catalog(
        type: String,
        page: Int = 1,
        limit: Int = 20,
        forceRefresh: Boolean = false,
    ): MdBrowseResponse? {
        return cache.get("md.catalog.$type.$page.$limit", 300, forceRefresh) {
            runCatching { api.catalog(type, page, limit).body() }.getOrNull()
        }
    }

    // ─── Search — NOT cached ───
    suspend fun search(query: String, limit: Int = 20): MdBrowseResponse? {
        return runCatching { api.search(query, limit).body() }.getOrNull()
    }

    // ─── Details ───
    suspend fun details(
        detailPath: String,
        forceRefresh: Boolean = false,
    ): MdDetails? {
        return cache.get("md.details.$detailPath", 1800, forceRefresh) {
            runCatching { api.details(detailPath).body() }.getOrNull()
        }
    }

    // ─── Streams (short TTL — URLs are signed with long expiry) ───
    suspend fun movieStream(
        detailPath: String,
        forceRefresh: Boolean = false,
    ): MdStreamResponse? {
        return cache.get("md.movie.$detailPath", 60, forceRefresh) {
            runCatching { api.movieStream(detailPath).body() }.getOrNull()
        }
    }

    suspend fun tvStream(
        detailPath: String,
        season: Int,
        episode: Int,
        forceRefresh: Boolean = false,
    ): MdStreamResponse? {
        return cache.get("md.tv.$detailPath.$season.$episode", 60, forceRefresh) {
            runCatching { api.tvStream(detailPath, season, episode).body() }.getOrNull()
        }
    }

    // ─── Seasons + captions — rarely change ───
    suspend fun seasons(
        detailPath: String,
        forceRefresh: Boolean = false,
    ): MdSeasonsResponse? {
        return cache.get("md.seasons.$detailPath", 3600, forceRefresh) {
            runCatching { api.seasons(detailPath).body() }.getOrNull()
        }
    }

    suspend fun captions(
        detailPath: String,
        season: Int? = null,
        episode: Int? = null,
        forceRefresh: Boolean = false,
    ): MdCaptionsResponse? {
        val key = "md.captions.$detailPath.$season.$episode"
        return cache.get(key, 1800, forceRefresh) {
            runCatching { api.captions(detailPath, season, episode).body() }.getOrNull()
        }
    }

    fun clearCache() = cache.clear()
}