package com.example.data.model

import com.squareup.moshi.Json

/**
 * Response shape for GET /api/home (movies-demo backend).
 * Reuses MdSubject for item payloads — same field names the browse /
 * trending endpoints already produce, so no extra parsing work.
 */
data class MdHomeResponse(
    @Json(name = "bannerCount")   val bannerCount: Int = 0,
    @Json(name = "banners")       val banners: List<MdSubject> = emptyList(),
    @Json(name = "sectionCount")  val sectionCount: Int = 0,
    @Json(name = "sections")      val sections: List<MdHomeSection> = emptyList(),
    @Json(name = "platformCount") val platformCount: Int = 0,
    @Json(name = "platforms")     val platforms: List<MdPlatform> = emptyList(),
)

data class MdHomeSection(
    @Json(name = "type")     val type: String = "",
    @Json(name = "title")    val title: String = "",
    @Json(name = "position") val position: Int = 0,
    @Json(name = "count")    val count: Int = 0,
    @Json(name = "items")    val items: List<MdSubject> = emptyList(),
)

data class MdPlatform(
    @Json(name = "name")     val name: String = "",
    @Json(name = "uploadBy") val uploadBy: String = "",
)