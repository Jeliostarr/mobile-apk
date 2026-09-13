package com.example.data.model

import com.squareup.moshi.JsonClass

const val MOVIES_DEMO_BASE_URL = "https://movie-bo-api.vercel.app"
const val MOVIES_DEMO_TELEGRAM_URL = "https://t.me/jeliostarrdev"
const val MOVIES_DEMO_WHATSAPP_URL = "https://wa.me/256707934960"

/** The country whose titles get their own "Nigerian" tab instead of sitting in the general non-translated catalog. */
const val MOVIES_DEMO_NIGERIA_COUNTRY = "Nigeria"

/**
 * Builds the URL the app must actually fetch/stream/download a media file
 * from — every raw CDN url (video, subtitle, trailer) has to be routed
 * through this proxy: the CDN requires a Referer header a mobile HTTP
 * client can set freely (unlike a browser <video> tag), but this is also
 * where our own X-API-Key gate and usage metering live, so bypassing the
 * proxy isn't just unnecessary, the CDN would reject the request anyway.
 */
fun moviesDemoStreamUrl(mediaUrl: String): String =
    "$MOVIES_DEMO_BASE_URL/api/stream?url=" + java.net.URLEncoder.encode(mediaUrl, "UTF-8")

fun moviesDemoDownloadUrl(mediaUrl: String, filename: String): String =
    "$MOVIES_DEMO_BASE_URL/api/download?url=" + java.net.URLEncoder.encode(mediaUrl, "UTF-8") +
        "&filename=" + java.net.URLEncoder.encode(filename, "UTF-8")

/** Matches the reference site's own filename convention exactly (Oppenheimer_1080P.mp4 / Lucifer_S01E013_1080P.mp4), so files saved from the app look the same as files saved from the website. */
fun moviesDemoDownloadFilename(title: String, resolution: Int, season: Int? = null, episode: Int? = null): String {
    val safeTitle = title.replace(Regex("[^\\w\\- ]"), "").trim().replace(Regex("\\s+"), "_")
    val tag = if (season != null && episode != null) {
        "_S" + season.toString().padStart(2, '0') + "E" + episode.toString().padStart(3, '0')
    } else ""
    return "${safeTitle}${tag}_${resolution}P.mp4"
}

/** The common item shape returned by /browse, /catalog, /trending, and /search — same `norm()` projection server-side regardless of which endpoint it came from. */
@JsonClass(generateAdapter = true)
data class MdSubject(
    val title: String? = null,
    val subjectId: String? = null,
    val subjectType: Int? = null,
    val detailPath: String? = null,
    val type: String? = null, // "movie" | "tv" | "other"
    val genre: String? = null,
    val imdbRating: String? = null,
    val imdbRatingValue: String? = null,
    val imdbRatingCount: Int? = null,
    val country: String? = null,
    val countryName: String? = null,
    val description: String? = null,
    val releaseDate: String? = null,
    val duration: Int? = null,
    val cover: String? = null,
    val hasResource: Boolean? = null
) {
    /** country/countryName is populated inconsistently depending on which endpoint the item came from — this is the one field every screen should actually read. */
    val effectiveCountry: String? get() = country?.ifBlank { null } ?: countryName?.ifBlank { null }
    val effectiveRating: String? get() = imdbRating?.ifBlank { null } ?: imdbRatingValue?.ifBlank { null }
    val isNigerian: Boolean get() = effectiveCountry.equals(MOVIES_DEMO_NIGERIA_COUNTRY, ignoreCase = true)
}

@JsonClass(generateAdapter = true)
data class MdBrowseResponse(
    val items: List<MdSubject>? = null,
    val results: List<MdSubject>? = null, // /search uses this key instead of `items`
    val total: Int? = null,
    val count: Int? = null,
    val page: Int? = null
) {
    val effectiveItems: List<MdSubject> get() = items ?: results ?: emptyList()
}

@JsonClass(generateAdapter = true)
data class MdTrailer(
    val url: String? = null,
    val videoId: String? = null,
    val duration: Int? = null,
    val width: Int? = null,
    val height: Int? = null,
    val cover: String? = null
)

@JsonClass(generateAdapter = true)
data class MdCastMember(
    val staffId: String? = null,
    val staffType: Int? = null, // 1 = cast, 2 = director
    val role: String? = null,
    val name: String? = null,
    val character: String? = null,
    val avatarUrl: String? = null,
    val detailPath: String? = null
)

@JsonClass(generateAdapter = true)
data class MdDub(
    val subjectId: String? = null,
    val lanName: String? = null,
    val lanCode: String? = null,
    val original: Boolean? = null,
    val type: Int? = null, // 0 = dub (alternate audio), 1 = subtitle-language variant
    val kind: String? = null, // "dub" | "subtitle"
    val detailPath: String? = null
)

@JsonClass(generateAdapter = true)
data class MdDetails(
    val detailPath: String? = null,
    val subjectId: String? = null,
    val subjectType: Int? = null,
    val type: String? = null,
    val title: String? = null,
    val description: String? = null,
    val genre: String? = null,
    val releaseDate: String? = null,
    val duration: Int? = null,
    val durationText: String? = null,
    val imdbRatingValue: String? = null,
    val imdbRatingCount: Int? = null,
    val countryName: String? = null,
    val subtitles: String? = null,
    val cover: String? = null,
    val coverWidth: Int? = null,
    val coverHeight: Int? = null,
    val hasResource: Boolean? = null,
    val trailer: MdTrailer? = null,
    val cast: List<MdCastMember>? = null,
    val castCount: Int? = null,
    val dubs: List<MdDub>? = null,
    val dubCount: Int? = null
) {
    val isNigerian: Boolean get() = countryName.equals(MOVIES_DEMO_NIGERIA_COUNTRY, ignoreCase = true)
}

@JsonClass(generateAdapter = true)
data class MdQuality(
    val resolution: Int? = null,
    val size_mb: Double? = null,
    val duration_sec: Int? = null,
    val codec: String? = null,
    val vipLocked: Boolean? = null,
    val url: String? = null
)

/** Shared shape for both /api/movie/:detailPath and /api/tv/:detailPath — the TV one additionally carries season/episode/available_seasons, all nullable here since a movie response won't have them. */
@JsonClass(generateAdapter = true)
data class MdStreamResponse(
    val title: String? = null,
    val subjectId: String? = null,
    val detailPath: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val watch_url: String? = null,
    val source: String? = null,
    val qualities: List<MdQuality>? = null,
    val best_free: MdQuality? = null
) {
    /** Never offer a VIP-locked quality — this API has no VIP account to unlock it with, so the URL wouldn't actually play. */
    val freeQualities: List<MdQuality> get() = (qualities ?: emptyList())
        .filter { it.vipLocked != true && !it.url.isNullOrBlank() }
        .sortedByDescending { it.resolution ?: 0 }

    val defaultQuality: MdQuality? get() = best_free?.takeIf { it.vipLocked != true && !it.url.isNullOrBlank() }
        ?: freeQualities.firstOrNull()
}

@JsonClass(generateAdapter = true)
data class MdSeasonResolution(
    val resolution: Int? = null,
    val epNum: Int? = null
)

@JsonClass(generateAdapter = true)
data class MdSeason(
    val season: Int? = null,
    val maxEp: Int? = null,
    val resolutions: List<MdSeasonResolution>? = null,
    val availableResolutions: List<Int>? = null
)

@JsonClass(generateAdapter = true)
data class MdSeasonsResponse(
    val detailPath: String? = null,
    val title: String? = null,
    val seasonCount: Int? = null,
    val totalEpisodes: Int? = null,
    val globalResolutions: List<Int>? = null,
    val seasons: List<MdSeason>? = null
)

@JsonClass(generateAdapter = true)
data class MdCaption(
    val id: String? = null,
    val lan: String? = null,
    val lanName: String? = null,
    val url: String? = null,
    val size: Int? = null,
    val delay: Int? = null
) {
    val isEnglish: Boolean get() = lan.equals("en", ignoreCase = true) || lanName.equals("English", ignoreCase = true)
}

@JsonClass(generateAdapter = true)
data class MdCaptionsResponse(
    val detailPath: String? = null,
    val title: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val videoId: String? = null,
    val captionCount: Int? = null,
    val captions: List<MdCaption>? = null
) {
    /** English if available, otherwise nothing selected — never guess at a random other language by default. */
    val defaultCaption: MdCaption? get() = captions?.firstOrNull { it.isEnglish }
}
