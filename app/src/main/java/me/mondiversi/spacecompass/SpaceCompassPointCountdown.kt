package me.mondiversi.spacecompass

import java.time.Duration
import java.time.Instant
import java.util.Locale

/** Elapsed UTC duration, independent of local timezone and daylight-saving transitions. */
internal fun formatSpaceCompassPointCountdown(timeMs: Long, nowMs: Long, locale: Locale = Locale.ROOT): String {
    val future = timeMs > nowMs
    val duration = Duration.between(Instant.ofEpochMilli(nowMs), Instant.ofEpochMilli(timeMs)).abs()
    val minutes = duration.toMinutes()
    val sign = if (future) "T−" else "T+"
    return sign + String.format(locale, "%02d:%02d", minutes / 60, minutes % 60)
}
