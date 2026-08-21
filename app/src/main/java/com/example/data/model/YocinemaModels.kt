package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

const val BASE_URL = "https://api.yocinema.dpdns.org"
const val DASHBOARD_URL = "https://dash.yocinema.dpdns.org"
const val WATCH_WEB_URL = "https://watch.yocinema.dpdns.org"

fun cleanMediaUrl(url: String?): String? {
    if (url == null || url.trim().isEmpty()) return url
    return url.replace(Regex("([?&])(key|apiKey|api_key|x-api-key)=[^&]*", RegexOption.IGNORE_CASE)) { matchResult ->
        if (matchResult.groupValues[1] == "?") "?" else ""
    }.trimEnd('?', '&')
}


/**
 * NOTE: this is intentionally kept as a real, separately-named extension
 * (not just an alias for kotlin's own isNullOrEmpty()) — other screens in
 * the codebase (e.g. HomeScreen.kt) import this exact symbol directly from
 * com.example.data.model, so removing it breaks their compile.
 */
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

/** Formats a small integer amount of Ugandan Shillings for display, e.g. 15000 -> "UGX 15,000". */
fun formatUgx(amount: Double?): String {
    if (amount == null) return "UGX 0"
    val rounded = amount.toLong()
    val str = rounded.toString()
    val sb = StringBuilder()
    for ((i, c) in str.reversed().withIndex()) {
        if (i != 0 && i % 3 == 0) sb.append(',')
        sb.append(c)
    }
    return "UGX ${sb.reverse()}"
}

/** Parses a handful of common ISO-ish date formats without pulling in java.time / desugaring requirements. */
fun parseIsoMillis(dateStr: String?): Long? {
    if (dateStr.isNullOrBlank()) return null
    return try {
        val cleaned = dateStr.trim()
        val datePart = cleaned.take(10)
        val parts = datePart.split("-")
        if (parts.size != 3) return null
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val day = parts[2].toInt()
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        cal.clear()
        cal.set(year, month - 1, day, 0, 0, 0)
        cal.timeInMillis
    } catch (e: Exception) {
        null
    }
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
        return if (!st.isNullOrEmpty()) st!! else getPublicCoverUrl(movieId)
    }
}

/**
 * The logged-in user. [balance] is the wallet balance in UGX — the backend's
 * `/account/me` returns the full Mongo user document, and `balance` there is
 * a number, not a string, so this must decode as Double (Moshi will happily
 * decode `15000` or `15000.0` into a Double either way).
 */
@JsonClass(generateAdapter = true)
data class AccountUser(
    @Json(name = "_id") val id: String? = null,
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val balance: Double? = null,
    val accountType: String? = null,
    val status: String? = null
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
data class PlanInfo(
    val name: String? = null,
    val dailyLimit: Int? = null,
    val priceUGX: Int? = null,
    val duration: Int? = null
)

/** One of the user's API keys, as returned by GET /api/v1/keys. */
@JsonClass(generateAdapter = true)
data class KeyInfo(
    @Json(name = "_id") val id: String? = null,
    val key: String? = null,
    val name: String? = null,
    val status: String? = null, // active | paused | suspended | revoked | deleted
    val dailyLimit: Int? = null,
    val usageToday: Int? = null,
    val usageTotal: Int? = null,
    val expiresAt: String? = null,
    val createdAt: String? = null,
    val plan: PlanInfo? = null
) {
    val isExpired: Boolean
        get() {
            val ms = parseIsoMillis(expiresAt) ?: return false
            // expiresAt is date-only precision here; treat "expires today" as
            // still valid until the day actually rolls over.
            return ms + (24 * 60 * 60 * 1000) < System.currentTimeMillis()
        }

    val isUsedUp: Boolean
        get() = (dailyLimit ?: 0) > 0 && (usageToday ?: 0) >= (dailyLimit ?: 0)

    val isUsable: Boolean
        get() = status.equals("active", ignoreCase = true) && !isExpired

    val maskedKey: String
        get() {
            val k = key ?: return ""
            return if (k.length > 10) k.take(6) + "••••••" + k.takeLast(4) else k
        }

    val usageFraction: Float
        get() {
            val limit = dailyLimit ?: 0
            if (limit <= 0) return 0f
            return ((usageToday ?: 0).toFloat() / limit.toFloat()).coerceIn(0f, 1f)
        }
}

/**
 * index.js applies a global `wrapResponse` middleware to every route, which
 * — going by how MeResponse already had to handle it — wraps the route's
 * own res.json({...}) inside an outer {success, data: {...}} envelope. The
 * keys.js route itself calls `res.json({ keys })`, so the real payload is
 * `{ data: { keys: [...] } }`, not a bare `{ keys: [...] }`. [actualKeys]
 * is what the repository/UI should read, not [keys] directly.
 */
@JsonClass(generateAdapter = true)
data class KeysResponse(
    val keys: List<KeyInfo>? = null,
    val data: KeysDataWrapper? = null
) {
    val actualKeys: List<KeyInfo> get() = keys ?: data?.keys ?: emptyList()
}

@JsonClass(generateAdapter = true)
data class KeysDataWrapper(
    val keys: List<KeyInfo>? = null
)

@JsonClass(generateAdapter = true)
data class UsagePoint(
    val date: String? = null,
    val count: Int? = 0
)

@JsonClass(generateAdapter = true)
data class KeyUsage(
    val name: String? = null,
    val usageToday: Int? = 0,
    val dailyLimit: Int? = 0
)

/** Same envelope situation as [KeysResponse] — see that doc comment. */
@JsonClass(generateAdapter = true)
data class UsageResponse(
    val series: List<UsagePoint>? = null,
    val perKey: List<KeyUsage>? = null,
    val totalToday: Int? = null,
    val data: UsageDataWrapper? = null
) {
    val actualSeries: List<UsagePoint> get() = series ?: data?.series ?: emptyList()
    val actualPerKey: List<KeyUsage> get() = perKey ?: data?.perKey ?: emptyList()
    val actualTotalToday: Int get() = totalToday ?: data?.totalToday ?: 0
}

@JsonClass(generateAdapter = true)
data class UsageDataWrapper(
    val series: List<UsagePoint>? = null,
    val perKey: List<KeyUsage>? = null,
    val totalToday: Int? = null
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
