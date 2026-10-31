package me.mondiversi.spacecompass

import java.time.Instant
import java.time.ZoneId

internal val spaceCompassObservationYears = 1900..2100

internal fun parseSpaceCompassObservationYear(text: String): Int? {
    val digits = text.trim()
    if (digits.length != 4) return null
    val normalized = digits.map { Character.digit(it, 10).takeIf { value -> value >= 0 } ?: return null }
        .joinToString("")
    return normalized.toIntOrNull()?.takeIf { it in spaceCompassObservationYears }
}

/** Consult another annual calendar without changing the Location, including leap days and DST. */
internal fun spaceCompassObservationYearReference(timeMs: Long, year: Int, zone: ZoneId): Long {
    require(year in spaceCompassObservationYears)
    return Instant.ofEpochMilli(timeMs).atZone(zone).withYear(year).toInstant().toEpochMilli()
}
