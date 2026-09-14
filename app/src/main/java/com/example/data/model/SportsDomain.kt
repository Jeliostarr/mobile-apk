package com.example.data.model

import java.time.Instant

/**
 * Domain model the UI consumes. Converts the wire DTO into something the
 * screens don't have to reason about — status as an enum, times as Instant,
 * playability pre-computed.
 */

enum class MatchStatus { SCHEDULED, LIVE, FINISHED, POSTPONED, CANCELLED, UNKNOWN }

enum class MediaKind { LIVE, HIGHLIGHT, REPLAY, CHANNEL }

data class Team(
    val id: String,
    val name: String,
    val abbreviation: String,
    val score: Int?,
    val logo: String?,
)

data class MediaClip(
    val id: String,
    val title: String?,
    val url: String,
    val cover: String?,
    val durationSeconds: Int?,
    val kind: MediaKind,
)

data class Match(
    val id: String,
    val league: String,
    val leagueId: String,
    val round: String,
    val season: String,
    val home: Team,
    val away: Team,
    val status: MatchStatus,
    val startTime: Instant?,
    val endTime: Instant?,
    val canWatchLive: Boolean,
    val liveStreamUrl: String?,
    val highlights: List<MediaClip>,
    val replay: List<MediaClip>,
    val channels: List<MediaClip>,
) {
    val title: String get() = "${home.name} vs ${away.name}"
}

fun SportsEventDto.toDomain(): Match {
    val start = parseIso(startTime)
    val end = parseIso(endTime)
    val isLive = status == "live"

    // Only surface the live stream URL when the API says it is genuinely playable.
    val liveUrl = liveStream.url?.takeIf { liveStream.available && isLive && it.isNotBlank() }

    return Match(
        id = id,
        league = league,
        leagueId = leagueId,
        round = round,
        season = season,
        home = teams.home.toTeam(),
        away = teams.away.toTeam(),
        status = mapStatus(status),
        startTime = start,
        endTime = end,
        canWatchLive = liveUrl != null,
        liveStreamUrl = liveUrl,
        highlights = highlights.map { it.toClip(MediaKind.HIGHLIGHT) },
        replay = replay.map { it.toClip(MediaKind.REPLAY) },
        channels = streams.map { it.toClip(MediaKind.CHANNEL) },
    )
}

private fun SportsTeamDto.toTeam() = Team(
    id = id,
    name = name,
    abbreviation = abbreviation,
    score = score,
    logo = logo,
)

private fun SportsMediaDto.toClip(kind: MediaKind) = MediaClip(
    id = id,
    title = title,
    url = url,
    cover = cover,
    durationSeconds = durationSeconds,
    kind = kind,
)

private fun SportsStreamDto.toClip(kind: MediaKind) = MediaClip(
    id = id,
    title = title,
    url = url,
    cover = cover,
    durationSeconds = null,
    kind = kind,
)

private fun mapStatus(raw: String): MatchStatus = when (raw) {
    "live" -> MatchStatus.LIVE
    "finished" -> MatchStatus.FINISHED
    "scheduled" -> MatchStatus.SCHEDULED
    "postponed" -> MatchStatus.POSTPONED
    "cancelled" -> MatchStatus.CANCELLED
    else -> MatchStatus.UNKNOWN
}

private fun parseIso(value: String?): Instant? {
    if (value.isNullOrBlank()) return null
    return try { Instant.parse(value) } catch (e: Exception) { null }
}