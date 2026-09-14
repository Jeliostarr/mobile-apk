package com.example.data.model

import com.squareup.moshi.JsonClass

/**
 * DTOs for the new sports API. Names match the JSON exactly — do not rename
 * without updating the corresponding JSON keys.
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
    val id: String,
    val name: String,
    val localName: String = name,
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
    val id: String,
    val title: String? = null,
    val url: String,
    val cover: String? = null,
    val authorized: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class SportsMediaDto(
    val id: String,
    val title: String? = null,
    val url: String,
    val cover: String? = null,
    val durationSeconds: Int? = null,
    val createdAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class SportsEventDto(
    val id: String,
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