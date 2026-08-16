package com.example.repository

import com.example.data.api.YocinemaApi
import com.example.data.model.SportsLeague
import com.example.data.model.SportsLeaguesResponse
import com.example.data.model.SportsMatchDetailResponse
import com.example.data.model.SportsMatchesResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * Sports data + streams — always fetched live, nothing cached to disk or
 * memory here (unlike [YocinemaRepository]'s movie caches), since match
 * status and stream availability change by the minute.
 *
 * Reuses the same authenticated Retrofit client as [YocinemaRepository]
 * (same API key interceptor) — construct with `repository.api`, don't
 * build a second Retrofit/OkHttp instance.
 */
class SportsRepository(private val api: YocinemaApi) {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    /**
     * Our backend wraps every response as { api, version, creator, success, data: {...} }.
     * This unwraps that one level to get sports.js's own response object —
     * same idea as YocinemaRepository.getFacets().
     */
    private fun unwrapData(jsonString: String): String {
        return try {
            val genericAdapter = moshi.adapter(Any::class.java)
            val root = genericAdapter.fromJson(jsonString)
            if (root is Map<*, *>) {
                val data = root["data"]
                if (data != null) return genericAdapter.toJson(data)
            }
            jsonString
        } catch (e: Exception) {
            jsonString
        }
    }

    suspend fun getLeagues(): List<SportsLeague> {
        return try {
            val response = api.getSportsLeagues()
            if (response.isSuccessful && response.body() != null) {
                val json = unwrapData(response.body()!!.string())
                moshi.adapter(SportsLeaguesResponse::class.java).fromJson(json)?.leagues ?: emptyList()
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getMatches(
        status: String = "all",
        league: String? = null,
        page: Int = 1,
        limit: Int = 24
    ): SportsMatchesResponse {
        return try {
            val response = api.getSportsMatches(status, league, page, limit)
            if (response.isSuccessful && response.body() != null) {
                val json = unwrapData(response.body()!!.string())
                moshi.adapter(SportsMatchesResponse::class.java).fromJson(json)
                    ?: SportsMatchesResponse(status = status)
            } else SportsMatchesResponse(status = status)
        } catch (e: Exception) {
            SportsMatchesResponse(status = status)
        }
    }

    suspend fun getMatchDetail(matchId: String): SportsMatchDetailResponse? {
        return try {
            val response = api.getSportsMatchDetail(matchId)
            if (response.isSuccessful && response.body() != null) {
                val json = unwrapData(response.body()!!.string())
                moshi.adapter(SportsMatchDetailResponse::class.java).fromJson(json)
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
