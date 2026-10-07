package me.mondiversi.spacecompass

import kotlin.math.abs

/** Far-range presentation is independent of Solar System unit preferences. */
internal fun formatSpaceCompassExtrasolarDistance(km: Double?, numeric: SpaceCompassNumericFormat): String? =
    km?.takeIf { it.isFinite() && it >= SPACE_COMPASS_CELESTIAL_CATALOG_LY_THRESHOLD_AU * SPACE_COMPASS_AU_KM }
        ?.let { "${formatSpaceCompassNumber(it / SPACE_COMPASS_LIGHT_YEAR_KM, 1, numeric)} ly" }

/** Convert only at the display boundary; orbital calculations always use SI. */
internal fun formatSpaceCompassSelectedDistance(body: SpaceCompassCelestialBody, km: Double?,
    numeric: SpaceCompassNumericFormat, unit: String, normalFeet: Boolean = false): String? {
    if (body.isEarthSatellite || body == SpaceCompassCelestialBody.MOON || body == SpaceCompassCelestialBody.EARTH_CENTER) {
        if (km == null || !km.isFinite() || km < 0) return null
        val miles = normalFeet || unit == "mi"
        return formatSpaceCompassPhysicalLength(km * 1000, 2, numeric, miles, large = true)
    }
    formatSpaceCompassExtrasolarDistance(km, numeric)?.let { return it }
    if (unit == "default" || unit == "mkm") return formatSpaceCompassCelestialTableDistance(body, km, numeric)
    if (km == null || !km.isFinite() || km < 0) return null
    val divisor = when (unit) { "mi" -> 1.609344; "mmi" -> 1.609344e6; "mkm" -> 1e6; "au" -> SPACE_COMPASS_AU_KM; else -> 1.0 }
    val suffix = when (unit) { "mi" -> "mi"; "mmi" -> "Mmi"; "mkm" -> "Mkm"; "au" -> "AU"; else -> "km" }
    val digits = if (unit == "mmi") when { body.isEarthSatellite || body.isVoyager -> 6; body == SpaceCompassCelestialBody.MOON -> 4; else -> 2 } else if (unit == "au") 6 else 2
    val minimum = Math.pow(10.0, -digits.toDouble())
    val value = km / divisor
    val number = if (value > 0 && value < minimum) "< ${formatSpaceCompassNumber(minimum, digits, numeric, minimumDigits = 0)}"
        else formatSpaceCompassNumber(value, digits, numeric, minimumDigits = 0)
    return "$number $suffix" + if (unit in listOf("mkm", "mmi")) " · ${formatSpaceCompassCelestialAu(km, numeric)} AU" else ""
}

internal fun formatSpaceCompassSelectedCoordinates(latitude: Double?, longitude: Double?,
    numeric: SpaceCompassNumericFormat, dms: Boolean, systemLocale: java.util.Locale = java.util.Locale.getDefault()): String? {
    if (dms) return formatSpaceCompassCelestialGpsCoordinates(latitude, longitude, numeric, systemLocale)
    if (latitude == null || longitude == null || !latitude.isFinite() || !longitude.isFinite() ||
        latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
    fun coordinate(value: Double, positive: String, negative: String) =
        "${formatSpaceCompassNumber(abs(value), 6, numeric, grouping = false, systemLocale = systemLocale)}° ${if (value < 0) negative else positive}"
    return "${coordinate(latitude, "N", "S")}\n${coordinate(longitude, "E", "W")}"
}

/** Physical sizes follow normal-distance units; large diameters use their km/mi multiples. */
internal fun formatSpaceCompassPhysicalLength(meters: Double?, digits: Int,
    numeric: SpaceCompassNumericFormat, feet: Boolean, large: Boolean = false,
    systemLocale: java.util.Locale = java.util.Locale.getDefault()): String {
    if (meters == null || !meters.isFinite() || meters < 0) return "—"
    val divisor = if (large) { if (feet) 1609.344 else 1000.0 } else { if (feet) 0.3048 else 1.0 }
    val unit = if (large) { if (feet) "mi" else "km" } else { if (feet) "ft" else "m" }
    return "${formatSpaceCompassNumber(meters / divisor, digits, numeric, systemLocale = systemLocale)} $unit"
}

/** Speeds arrive in km/s; signed outward probe speeds remain signed after conversion. */
internal fun formatSpaceCompassSpeed(value: Double?, numeric: SpaceCompassNumericFormat,
    miles: Boolean, allowNegative: Boolean = false): String? =
    value?.takeIf { it.isFinite() && (it >= 0 || allowNegative) }?.let {
        formatSpaceCompassNumber(if (miles) it / 1.609344 else it, 2, numeric)
    }
