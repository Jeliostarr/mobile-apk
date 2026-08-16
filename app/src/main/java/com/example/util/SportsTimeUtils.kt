package com.example.ui.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Kickoff timestamps come back as UTC ISO-8601 ("2026-08-15T15:30:00.000Z").
 * SimpleDateFormat rather than java.time — minSdk 24 doesn't have java.time
 * without core library desugaring, which this project doesn't enable.
 */
object SportsTimeUtils {
    private val isoParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dayFormatter = SimpleDateFormat("EEE, MMM d", Locale.getDefault())

    fun parseMillis(kickoff: String?): Long? {
        if (kickoff.isNullOrBlank()) return null
        return try {
            synchronized(isoParser) { isoParser.parse(kickoff)?.time }
        } catch (e: Exception) {
            null
        }
    }

    /** "3:30 PM" in the device's local time zone. */
    fun formatTime(kickoff: String?): String {
        val ms = parseMillis(kickoff) ?: return "--:--"
        return synchronized(timeFormatter) { timeFormatter.format(Date(ms)) }
    }

    /** "Sat, Aug 15" in the device's local time zone. */
    fun formatDay(kickoff: String?): String {
        val ms = parseMillis(kickoff) ?: return ""
        return synchronized(dayFormatter) { dayFormatter.format(Date(ms)) }
    }

    /** "in 25m" / "in 3h" / "" (blank once kickoff has passed) — for upcoming-match badges. */
    fun formatCountdown(kickoff: String?): String {
        val ms = parseMillis(kickoff) ?: return ""
        val diff = ms - System.currentTimeMillis()
        if (diff <= 0) return ""
        val minutes = diff / 60_000
        return when {
            minutes < 1 -> "starting soon"
            minutes < 60 -> "in ${minutes}m"
            minutes < 1440 -> "in ${minutes / 60}h"
            else -> "in ${minutes / 1440}d"
        }
    }
}
