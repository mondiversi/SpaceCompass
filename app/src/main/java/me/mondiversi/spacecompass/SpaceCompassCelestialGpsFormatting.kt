package me.mondiversi.spacecompass

import kotlin.math.abs
import kotlin.math.roundToLong

/** Observer coordinates are displayed locally in DMS, never added to remote requests/logs.
 * Round the whole angle first so seconds/minutes carry correctly at their boundaries.
 * N/S/E/W are the international coordinate hemisphere notation, independent of UI language. */
internal fun formatSpaceCompassCelestialGpsCoordinates(latitude: Double?, longitude: Double?,
    numeric: SpaceCompassNumericFormat): String? {
    if (latitude == null || longitude == null || !latitude.isFinite() || !longitude.isFinite() ||
        latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
    fun dms(angle: Double, positive: String, negative: String): String {
        val ticks = (abs(angle) * 36_000).roundToLong()
        val degrees = formatSpaceCompassNumber((ticks / 36_000).toDouble(), 0, numeric, grouping = false)
        val zero = formatSpaceCompassNumber(0.0, 0, numeric, grouping = false).first()
        val minutes = formatSpaceCompassNumber((ticks % 36_000 / 600).toDouble(), 0, numeric,
            grouping = false).padStart(2, zero)
        val seconds = formatSpaceCompassNumber(ticks % 600 / 10.0, 1, numeric, minimumDigits = 1)
        return "$degrees° $minutes′ $seconds″ ${if (angle < 0) negative else positive}"
    }
    return dms(latitude, "N", "S") + "\n" + dms(longitude, "E", "W")
}
