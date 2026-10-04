package me.mondiversi.planetcompass

internal data class PlanetCompassSunGroundPalette(val farArgb: Int, val nearArgb: Int, val hazeArgb: Int)

/** Illustrative ground illumination follows the same local solar phase as the sky, not app theme/weather. */
internal fun planetCompassSunGroundPalette(phase: PlanetCompassSunSkyPhase): PlanetCompassSunGroundPalette = when (phase) {
    PlanetCompassSunSkyPhase.NIGHT -> PlanetCompassSunGroundPalette(
        0xFF202D30.toInt(), 0xFF0D171C.toInt(), 0xFF303F52.toInt())
    PlanetCompassSunSkyPhase.DAWN -> PlanetCompassSunGroundPalette(
        0xFF9A8573.toInt(), 0xFF435541.toInt(), 0xFFC1A28D.toInt())
    PlanetCompassSunSkyPhase.MORNING -> PlanetCompassSunGroundPalette(
        0xFF9BA17B.toInt(), 0xFF536D43.toInt(), 0xFFC7D1C8.toInt())
    PlanetCompassSunSkyPhase.AFTERNOON -> PlanetCompassSunGroundPalette(
        0xFF8C9870.toInt(), 0xFF4A623B.toInt(), 0xFFBACBC7.toInt())
    PlanetCompassSunSkyPhase.SUNSET -> PlanetCompassSunGroundPalette(
        0xFF98755E.toInt(), 0xFF3F4E37.toInt(), 0xFFC2997B.toInt())
    PlanetCompassSunSkyPhase.EVENING -> PlanetCompassSunGroundPalette(
        0xFF42404B.toInt(), 0xFF1F2C2B.toInt(), 0xFF635C6C.toInt())
}
