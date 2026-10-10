package me.mondiversi.spacecompass

/** The globe is illustrative: shared solar-height bands drive its shader's light angle.
 * Interface theme never substitutes midnight/noon for the observer's daylight.
 * Without a solar fix, retain the local clock instead of inventing a GPS position.
 */
internal fun spaceCompassScreensaverLightHour(solarElevation: Double?, localHour: Double): Double {
    if (solarElevation == null || !solarElevation.isFinite())
        return if (localHour.isFinite()) ((localHour % 24) + 24) % 24 else 12.0
    val light = spaceCompassSolarLighting(solarElevation)
    fun hour(band: SpaceCompassSolarLightBand): Double = when (band) {
        SpaceCompassSolarLightBand.NIGHT -> 0.0
        SpaceCompassSolarLightBand.TWILIGHT -> 5.5
        SpaceCompassSolarLightBand.HORIZON -> 6.5
        SpaceCompassSolarLightBand.DAY -> 12.0
    }
    val from = hour(light.from)
    return from + (hour(light.to) - from) * light.mix
}
