package com.example.data.model

import com.squareup.moshi.JsonClass

const val MOVIES_DEMO_BASE_URL = "https://movie-bo-api.vercel.app"
const val MOVIES_DEMO_TELEGRAM_URL = "https://t.me/jeliostarrdev"
const val MOVIES_DEMO_WHATSAPP_URL = "https://wa.me/256707934960"

/** The country whose titles get their own "Nigerian" tab instead of sitting in the general non-translated catalog. */
const val MOVIES_DEMO_NIGERIA_COUNTRY = "Nigeria"
/**
 * The backend now returns opaque worker URLs for all media
 * (cdn.yocinema.dpdns.org/m/<token>). This is a pass-through — no
 * wrapping. Falls back to the legacy Vercel proxy only if a raw CDN
 * URL ever appears (a rolling-deploy edge case where the backend is
 * still emitting old URLs).
 */
fun moviesDemoStreamUrl(mediaUrl: String): String {
    if (mediaUrl.isBlank()) return ""
    if (mediaUrl.startsWith("https://cdn.yocinema.dpdns.org/")) return mediaUrl
    return "$MOVIES_DEMO_BASE_URL/api/stream?url=" +
        java.net.URLEncoder.encode(mediaUrl, "UTF-8")
}

/**
 * Download URLs now come from the backend already wrapped — with the
 * filename + Content-Disposition: attachment baked into the token — so
 * the client just passes them through. `filename` parameter is ignored
 * but kept for source compatibility with existing call sites.
 */
fun moviesDemoDownloadUrl(mediaUrl: String, @Suppress("UNUSED_PARAMETER") filename: String): String {
    if (mediaUrl.isBlank()) return ""
    if (mediaUrl.startsWith("https://cdn.yocinema.dpdns.org/")) return mediaUrl
    return "$MOVIES_DEMO_BASE_URL/api/download?url=" +
        java.net.URLEncoder.encode(mediaUrl, "UTF-8")
}

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

    /** The scraped catalog isn't purely movies/series — wrestling, live events, and other non-fiction "other" content comes back mixed in with type=="other" (or occasionally hasResource==false, nothing actually playable). Every screen listing content should filter through this rather than showing raw browse/trending results as-is. */
    val isPlayableTitle: Boolean get() = (type == "movie" || type == "tv") && hasResource != false

    val primaryGenre: String? get() = genre?.split(",")?.firstOrNull()?.trim()?.ifBlank { null }
    val formattedDuration: String? get() = duration?.takeIf { it > 0 }?.let { "${it / 60}h ${it % 60}m" }
}

/** True for content that actually belongs in a movies-demo feed — filters out non-movie/series scrape noise (wrestling, live events, anything type=="other"). When browsing the general non-translated catalog (countryFilter == null), also keeps Nigerian titles out since they have their own dedicated screen; when browsing a specific country, keeps only titles actually matching it. */
fun List<MdSubject>.cleanedForFeed(countryFilter: String?): List<MdSubject> = filter { subject ->
    if (!subject.isPlayableTitle) return@filter false
    if (countryFilter == null && subject.isNigerian) return@filter false
    if (countryFilter != null && !subject.effectiveCountry.equals(countryFilter, ignoreCase = true)) return@filter false
    true
}

@JsonClass(generateAdapter = true)
data class MdFiltersResponse(
    val genres: List<String>? = null,
    val countries: List<String>? = null,
    val years: List<String>? = null,
    val sortOptions: List<String>? = null
)

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

    /** Always the highest actual resolution available, not whatever the server's own best_free hint says — that hint has been wrong before, and "movies should start playing in high quality" means literally the best one on the list, not a server guess. */
    val defaultQuality: MdQuality? get() = freeQualities.firstOrNull()
        ?: best_free?.takeIf { it.vipLocked != true && !it.url.isNullOrBlank() }
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
