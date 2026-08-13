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

fun formatDuration(durationVal: Int?): String {
    if (durationVal == null || durationVal <= 0) return ""
    // If > 300, treat as seconds; otherwise treat as minutes
    val totalMinutes = if (durationVal > 300) durationVal / 60 else durationVal
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60
    return when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        mins > 0 -> "${mins}m"
        else -> ""
    }
}

fun formatEpisodeDuration(minutes: Int?): String {
    if (minutes == null || minutes <= 0) return ""
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        mins > 0 -> "${mins}m"
        else -> ""
    }
}

fun formatDateOrYear(dateStr: String?): String {
    if (dateStr.isNullOrBlank()) return ""
    val trimmed = dateStr.trim()
    if (trimmed.length >= 10 && trimmed[4] == '-' && trimmed[7] == '-') {
        try {
            val parts = trimmed.take(10).split("-")
            if (parts.size == 3) {
                val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                val monthIdx = parts[1].toIntOrNull()?.minus(1)
                if (monthIdx in 0..11) {
                    return "${parts[2]} ${monthNames[monthIdx!!]} ${parts[0]}"
                }
            }
        } catch (e: Exception) {
            // fallback
        }
    }
    val yearRegex = Regex("^(\\d{4})")
    val match = yearRegex.find(trimmed)
    return match?.groupValues?.get(1) ?: trimmed
}

fun extractYearOnly(dateStr: String?): String {
    if (dateStr.isNullOrBlank()) return ""
    val trimmed = dateStr.trim()
    val yearRegex = Regex("^(\\d{4})")
    val match = yearRegex.find(trimmed)
    return match?.groupValues?.get(1) ?: trimmed.take(4)
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
    /** Wide landscape banner image — distinct from [cover]/[poster], which are
     * tall. Use this (never cover/poster) for any wide hero/backdrop area;
     * stretching a poster into a landscape box crops it into an unrecognizable
     * sliver. */
    val heroImage: String? = null,
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
