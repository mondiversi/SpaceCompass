package me.mondiversi.planetcompass

import kotlin.math.abs
import kotlin.math.roundToLong

/** Observer coordinates are displayed locally in DMS, never added to remote requests/logs.
 * Round the whole angle first so seconds/minutes carry correctly at their boundaries.
 * N/S/E/W are the international coordinate hemisphere notation, independent of UI language. */
internal fun formatPlanetCompassCelestialGpsCoordinates(latitude: Double?, longitude: Double?,
    numeric: PlanetCompassNumericFormat): String? {
    if (latitude == null || longitude == null || !latitude.isFinite() || !longitude.isFinite() ||
        latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
    fun dms(angle: Double, positive: String, negative: String): String {
        val ticks = (abs(angle) * 36_000).roundToLong()
        val degrees = ticks / 36_000
        val minutes = (ticks % 36_000 / 600).toString().padStart(2, '0')
        val seconds = formatPlanetCompassNumber(ticks % 600 / 10.0, 1, numeric, minimumDigits = 1)
        return "$degrees° $minutes′ $seconds″ ${if (angle < 0) negative else positive}"
    }
    return dms(latitude, "N", "S") + "\n" + dms(longitude, "E", "W")
}
