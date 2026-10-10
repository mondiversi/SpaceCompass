package me.mondiversi.spacecompass

internal data class SpaceCompassSunSkyPalette(val topArgb: Int, val horizonArgb: Int)

private fun spaceCompassSunPhasePalette(phase: SpaceCompassSunSkyPhase): SpaceCompassSunSkyPalette = when (phase) {
    SpaceCompassSunSkyPhase.NIGHT -> SpaceCompassSunSkyPalette(0xFF071327.toInt(), 0xFF203B5C.toInt())
    SpaceCompassSunSkyPhase.DAWN -> SpaceCompassSunSkyPalette(0xFF5D71A2.toInt(), 0xFFF4C091.toInt())
    SpaceCompassSunSkyPhase.MORNING -> SpaceCompassSunSkyPalette(0xFF267EBD.toInt(), 0xFFBEE9F1.toInt())
    SpaceCompassSunSkyPhase.AFTERNOON -> SpaceCompassSunSkyPalette(0xFF176AAD.toInt(), 0xFF94D7ED.toInt())
    SpaceCompassSunSkyPhase.SUNSET -> SpaceCompassSunSkyPalette(0xFF5C477B.toInt(), 0xFFFFB074.toInt())
    SpaceCompassSunSkyPhase.EVENING -> SpaceCompassSunSkyPalette(0xFF182344.toInt(), 0xFF80607A.toInt())
}

internal fun spaceCompassSunSkyPalette(phase: SpaceCompassSunSkyPhase,
    lighting: SpaceCompassSolarLighting? = null): SpaceCompassSunSkyPalette =
    spaceCompassSolarLightingPalette(phase, lighting, ::spaceCompassSunPhasePalette) { a, b, t ->
        SpaceCompassSunSkyPalette(spaceCompassSolarArgb(a.topArgb, b.topArgb, t),
            spaceCompassSolarArgb(a.horizonArgb, b.horizonArgb, t))
    }
