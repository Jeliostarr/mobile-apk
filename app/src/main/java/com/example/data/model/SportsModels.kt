package com.example.data.model

import com.squareup.moshi.JsonClass

/**
 * Wire-format DTOs for the sports API. Field names match the JSON
 * exactly — every property is nullable-or-defaulted so a missing key
 * never causes Moshi to throw.
 *
 * The `url` fields on these DTOs are always worker tokens
 * (cdn.yocinema.dpdns.org/m/<token>), never raw CDN URLs. The app plays
 * them directly; no client-side wrapping is needed or wanted.
 */

@JsonClass(generateAdapter = true)
data class SportsListEnvelope(
    val items: List<SportsEventDto> = emptyList(),
    val count: Int = 0,
    val hasMore: Boolean = false,
    val nextCursor: String? = null,
)

@JsonClass(generateAdapter = true)
data class SportsItemEnvelope(
    val item: SportsEventDto,
)

@JsonClass(generateAdapter = true)
data class SportsLeaguesEnvelope(
    val items: List<SportsLeagueDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class SportsLeagueDto(
    val id: String = "",
    val name: String = "",
    val localName: String = "",
)

@JsonClass(generateAdapter = true)
data class SportsTeamDto(
    val id: String = "",
    val name: String = "",
    val abbreviation: String = "",
    val score: Int? = null,
    val logo: String? = null,
)

@JsonClass(generateAdapter = true)
data class SportsTeamsDto(
    val home: SportsTeamDto = SportsTeamDto(),
    val away: SportsTeamDto = SportsTeamDto(),
)

@JsonClass(generateAdapter = true)
data class SportsLiveStreamDto(
    val available: Boolean = false,
    val url: String? = null,
    val format: String? = null,
    val authorized: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class SportsStreamDto(
    val id: String = "",
    val title: String? = null,
    val url: String = "",
    val cover: String? = null,
    val authorized: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class SportsMediaDto(
    val id: String = "",
    val title: String? = null,
    val url: String = "",
    val cover: String? = null,
    val durationSeconds: Int? = null,
    val createdAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class SportsEventDto(
    val id: String = "",
    val sport: String = "",
    val league: String = "",
    val leagueId: String = "",
    val round: String = "",
    val season: String = "",
    val teams: SportsTeamsDto = SportsTeamsDto(),
    val status: String = "scheduled",
    val startTime: String = "",
    val endTime: String? = null,
    val liveStream: SportsLiveStreamDto = SportsLiveStreamDto(),
    val streams: List<SportsStreamDto> = emptyList(),
    val replay: List<SportsMediaDto> = emptyList(),
    val highlights: List<SportsMediaDto> = emptyList(),
)