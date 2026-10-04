package me.mondiversi.planetcompass

/** Only the GPS vertical uncertainty belongs beside altitude, never horizontal accuracy. */
internal fun formatPlanetCompassCelestialAltitude(altitudeMeters: Double?, verticalAccuracyMeters: Double?,
    numeric: PlanetCompassNumericFormat): String? {
    val altitude = altitudeMeters?.takeIf { it.isFinite() } ?: return null
    val accuracy = verticalAccuracyMeters?.takeIf { it.isFinite() && it >= 0.0 }
    return "${formatPlanetCompassNumber(altitude, 0, numeric)} m" +
        (accuracy?.let { " (±${formatPlanetCompassNumber(it, 0, numeric)} m)" } ?: "")
}
