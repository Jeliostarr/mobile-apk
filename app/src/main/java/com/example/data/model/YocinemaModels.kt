package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

const val BASE_URL = "https://api.yocinema.dpdns.org"

fun cleanMediaUrl(url: String?): String? {
    if (url == null || url.trim().isEmpty()) return url
    return url.replace(Regex("([?&])(key|apiKey|api_key|x-api-key)=[^&]*", RegexOption.IGNORE_CASE)) { matchResult ->
        if (matchResult.groupValues[1] == "?") "?" else ""
    }.trimEnd('?', '&')
}

fun String?.isNull_orEmpty(): Boolean = this == null || this.trim().isEmpty()

fun formatDuration(seconds: Int?): String {
    if (seconds == null || seconds <= 0) return ""
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        minutes > 0 -> "${minutes} Mins"
        else -> "${seconds}s"
    }
}

fun formatEpisodeDuration(minutes: Int?): String {
    if (minutes == null || minutes <= 0) return ""
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins} Mins"
    }
}

fun getPublicCoverUrl(movieId: String): String {
    return "$BASE_URL/api/v1/movies/public/cover/$movieId"
}

@JsonClass(generateAdapter = true)
data class Movie(
    @Json(name = "_id") val id: String = "",
    val title: String = "",
    val description: String? = null,
    val poster: String? = null,
    val cover: String? = null,
    val stills: String? = null,
    val genre: String? = null,
    val vjName: String? = null,
    val imdbRating: String? = null,
    val imdbRatingCount: Int? = null,
    val duration: Int? = null, // seconds
    val country: String? = null,
    val releaseDate: String? = null,
    val type: String? = null, // "movie" or "series"
    val trailerUrl: String? = null,
    val streamUrl: String? = null,
    val downloadUrl: String? = null,
    val cast: List<CastMember>? = emptyList(),
    val crew: Crew? = null,
    val seasons: List<Season>? = emptyList(),
    val episodes: List<Episode>? = emptyList()
) {
    val displayPosterUrl: String
        get() = if (id.isNotBlank()) getPublicCoverUrl(id) else (cover ?: poster ?: "")

    val isSeries: Boolean
        get() = type.equals("series", ignoreCase = true) || (seasons?.isNotEmpty() == true) || (episodes?.isNotEmpty() == true)
}

@JsonClass(generateAdapter = true)
data class CastMember(
    @Json(name = "_id") val id: String? = null,
    val castId: String? = null,
    val name: String = "",
    val character: String? = null,
    val role: String? = null, // "cast", "director", "writer", "producer"
    val avatarUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class Crew(
    val directors: List<CastMember>? = emptyList(),
    val writers: List<CastMember>? = emptyList(),
    val producers: List<CastMember>? = emptyList(),
    val cast: List<CastMember>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class Season(
    val seasonNumber: Int? = null,
    val season: Int? = null,
    val title: String? = null,
    val episodes: List<Episode>? = emptyList()
) {
    val number: Int
        get() = seasonNumber ?: season ?: 1
}

@JsonClass(generateAdapter = true)
data class Episode(
    @Json(name = "_id") val id: String? = null,
    val seasonNumber: Int? = null,
    val season: Int? = null,
    val episodeNumber: Int? = null,
    val episode: Int? = null,
    val title: String? = null,
    val duration: Int? = null, // seconds
    val still: String? = null,
    val thumbnail: String? = null,
    val poster: String? = null,
    val streamUrl: String? = null,
    val downloadUrl: String? = null
) {
    val sNum: Int get() = seasonNumber ?: season ?: 1
    val eNum: Int get() = episodeNumber ?: episode ?: 1

    fun getDisplayStill(movieId: String): String {
        val st = cleanMediaUrl(still ?: thumbnail ?: poster)
        return if (!st.isNull_orEmpty()) st!! else getPublicCoverUrl(movieId)
    }
}

@JsonClass(generateAdapter = true)
data class AccountUser(
    val name: String? = null,
    val email: String? = null,
    val balance: String? = null
)

@JsonClass(generateAdapter = true)
data class MeResponse(
    val user: AccountUser? = null,
    val data: MeDataWrapper? = null
) {
    val actualUser: AccountUser? get() = user ?: data?.user
}

@JsonClass(generateAdapter = true)
data class MeDataWrapper(
    val user: AccountUser? = null
)

@JsonClass(generateAdapter = true)
data class KeyInfo(
    val key: String? = null,
    val name: String? = null
)

@JsonClass(generateAdapter = true)
data class KeysResponse(
    val keys: List<KeyInfo>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class FacetsResponse(
    val genres: List<String>? = emptyList(),
    val vjs: List<String>? = emptyList(),
    val countries: List<String>? = emptyList(),
    val years: List<String>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class StreamTokenResponse(
    val streamToken: String = "",
    val expiresIn: Long = 3600 // seconds
)

@JsonClass(generateAdapter = true)
data class CastDetail(
    val name: String = "",
    val bio: String? = null,
    val birthday: String? = null,
    val placeOfBirth: String? = null,
    val photo: String? = null,
    val knownFor: String? = null,
    val filmography: List<FilmographyItem>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class FilmographyItem(
    val id: String? = null,
    val tmdbId: String? = null,
    val mediaType: String? = null,
    val title: String = "",
    val character: String? = null,
    val releaseDate: String? = null,
    val poster: String? = null,
    val onYocinema: Boolean = false,
    val versions: List<VersionItem>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class VersionItem(
    val movieId: String = "",
    val vjName: String? = null
)

@JsonClass(generateAdapter = true)
data class ViewProgressRequest(
    val viewerId: String,
    val watchedSeconds: Long
)
