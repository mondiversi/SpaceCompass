package me.mondiversi.spacecompass

/** Only the GPS vertical uncertainty belongs beside altitude, never horizontal accuracy. */
internal fun formatSpaceCompassCelestialAltitude(altitudeMeters: Double?, verticalAccuracyMeters: Double?,
    numeric: SpaceCompassNumericFormat, feet: Boolean = false,
    systemLocale: java.util.Locale = java.util.Locale.getDefault()): String? {
    val altitude = altitudeMeters?.takeIf { it.isFinite() } ?: return null
    val accuracy = verticalAccuracyMeters?.takeIf { it.isFinite() && it >= 0.0 }
    val factor = if (feet) 1.0 / 0.3048 else 1.0
    val unit = if (feet) "ft" else "m"
    return "${formatSpaceCompassNumber(altitude * factor, 0, numeric, systemLocale = systemLocale)} $unit" +
        (accuracy?.let { " (±${formatSpaceCompassNumber(it * factor, 0, numeric, systemLocale = systemLocale)} $unit)" } ?: "")
}
