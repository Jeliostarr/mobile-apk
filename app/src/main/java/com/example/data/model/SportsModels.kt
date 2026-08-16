package com.example.data.model

import com.squareup.moshi.JsonClass

// Sports data models — shapes match /api/v1/sports responses exactly as
// confirmed against live backend output. Nothing here is persisted locally
// (no Room entity, no repository cache); every screen fetches fresh.

@JsonClass(generateAdapter = true)
data class SportsTeam(
    val name: String = "",
    val img: String? = null
)

@JsonClass(generateAdapter = true)
data class SportsLeague(
    val name: String = "",
    val img: String? = null,
    // Only populated by /sports/leagues — zero on the copy nested in a match, harmless.
    val total: Int = 0,
    val live: Int = 0,
    val upcoming: Int = 0,
    val finished: Int = 0
)

@JsonClass(generateAdapter = true)
data class SportsMatch(
    val id: Long = 0L,
    val fixture: Long = 0L,
    val league: SportsLeague? = null,
    val kickoff: String? = null,
    val live: Boolean = false,
    val hot: Boolean = false,
    val home: SportsTeam? = null,
    val away: SportsTeam? = null,
    val hasStream: Boolean = false
) {
    val matchTitle: String
        get() = "${home?.name ?: "TBD"} vs ${away?.name ?: "TBD"}"
}

/** One playable quality option. label/quality are pre-formatted by the backend ("SD"/"480p" etc). */
@JsonClass(generateAdapter = true)
data class SportsStream(
    val id: Int = 0,
    val label: String = "",
    val quality: String? = null,
    val playUrl: String = "",
    val expiresIn: Long = 0
)

@JsonClass(generateAdapter = true)
data class SportsMatchesResponse(
    val success: Boolean = false,
    val status: String = "",
    val page: Int = 1,
    val limit: Int = 24,
    val total: Int = 0,
    val matches: List<SportsMatch> = emptyList(),
    val timestamp: String? = null
)

@JsonClass(generateAdapter = true)
data class SportsLeaguesResponse(
    val success: Boolean = false,
    val count: Int = 0,
    val leagues: List<SportsLeague> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SportsMatchDetailResponse(
    val success: Boolean = false,
    val match: SportsMatch? = null,
    val streams: List<SportsStream> = emptyList(),
    val timestamp: String? = null
)

enum class SportsFilter(val apiValue: String, val label: String) {
    // ALL isn't a single backend call — the screen fetches live + upcoming
    // in parallel and merges them (live first). apiValue is unused for ALL.
    ALL("all", "All"),
    LIVE("live", "Live"),
    UPCOMING("upcoming", "Upcoming"),
    ENDED("ended", "Ended")
}
