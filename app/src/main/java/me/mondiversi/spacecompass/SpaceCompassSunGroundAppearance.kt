package me.mondiversi.spacecompass

internal data class SpaceCompassSunGroundPalette(val farArgb: Int, val nearArgb: Int, val hazeArgb: Int)

/** Illustrative ground illumination follows the same local solar phase as the sky, not app theme/weather. */
internal fun spaceCompassSunGroundPalette(phase: SpaceCompassSunSkyPhase): SpaceCompassSunGroundPalette = when (phase) {
    SpaceCompassSunSkyPhase.NIGHT -> SpaceCompassSunGroundPalette(
        0xFF202D30.toInt(), 0xFF0D171C.toInt(), 0xFF303F52.toInt())
    SpaceCompassSunSkyPhase.DAWN -> SpaceCompassSunGroundPalette(
        0xFF9A8573.toInt(), 0xFF435541.toInt(), 0xFFC1A28D.toInt())
    SpaceCompassSunSkyPhase.MORNING -> SpaceCompassSunGroundPalette(
        0xFF9BA17B.toInt(), 0xFF536D43.toInt(), 0xFFC7D1C8.toInt())
    SpaceCompassSunSkyPhase.AFTERNOON -> SpaceCompassSunGroundPalette(
        0xFF8C9870.toInt(), 0xFF4A623B.toInt(), 0xFFBACBC7.toInt())
    SpaceCompassSunSkyPhase.SUNSET -> SpaceCompassSunGroundPalette(
        0xFF98755E.toInt(), 0xFF3F4E37.toInt(), 0xFFC2997B.toInt())
    SpaceCompassSunSkyPhase.EVENING -> SpaceCompassSunGroundPalette(
        0xFF42404B.toInt(), 0xFF1F2C2B.toInt(), 0xFF635C6C.toInt())
}
