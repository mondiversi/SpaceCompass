package me.mondiversi.planetcompass

import androidx.compose.ui.graphics.Color

/** Object-specific, sky-readable colours stay identical for one or many selected objects. */
internal fun planetCompassCelestialPathTint(body: PlanetCompassCelestialBody): Color = when (body) {
    PlanetCompassCelestialBody.SUN -> Color(0xFFFFD447)
    PlanetCompassCelestialBody.MOON -> Color(0xFFDCE3F0)
    PlanetCompassCelestialBody.MERCURY -> Color(0xFFBDAEA5)
    PlanetCompassCelestialBody.VENUS -> Color(0xFFFFE5B4)
    PlanetCompassCelestialBody.MARS -> Color(0xFFFF806D)
    PlanetCompassCelestialBody.JUPITER -> Color(0xFFFFB27B)
    PlanetCompassCelestialBody.IO -> Color(0xFFF2EB88)
    PlanetCompassCelestialBody.EUROPA -> Color(0xFF9EDAC9)
    PlanetCompassCelestialBody.SATURN -> Color(0xFFDCC895)
    PlanetCompassCelestialBody.URANUS -> Color(0xFF78DFE8)
    PlanetCompassCelestialBody.NEPTUNE -> Color(0xFF849AFF)
    PlanetCompassCelestialBody.PLUTO -> Color(0xFFCAA1C5)
    PlanetCompassCelestialBody.SEDNA -> Color(0xFFE991A4)
    PlanetCompassCelestialBody.ISS -> Color(0xFFA3EDA7)
    PlanetCompassCelestialBody.STARLINK_V3 -> Color(0xFF55C6FF)
    PlanetCompassCelestialBody.VOYAGER_1 -> Color(0xFFDBA5FF)
    PlanetCompassCelestialBody.VOYAGER_2 -> Color(0xFFC1CFFF)
    PlanetCompassCelestialBody.POLARIS -> Color(0xFFFFFFFF)
}
