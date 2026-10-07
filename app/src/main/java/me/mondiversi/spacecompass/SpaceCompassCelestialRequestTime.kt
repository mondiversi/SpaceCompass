package me.mondiversi.spacecompass

/** Public JPL vectors cover the viewed moment; satellite providers publish current elements only. */
internal fun spaceCompassCelestialRequestTime(body: SpaceCompassCelestialBody, downloadTimeMs: Long,
    observationTimeOverrideMs: Long?): Long =
    if (body.usesHorizons) observationTimeOverrideMs ?: downloadTimeMs else downloadTimeMs

/** Existing models may be valid data while not covering the chosen observation date. */
internal fun spaceCompassCelestialDataOutsideDate(body: SpaceCompassCelestialBody, observationTimeMs: Long,
    remote: SpaceCompassCelestialRemoteData): Boolean = when {
    body.isEarthSatellite -> remote.satelliteOrbit(body)?.let { !it.usable(observationTimeMs) } == true
    body.usesHorizons -> {
        val position = remote.ephemerides[body]
        val motion = remote.motions[body]
        (position != null || motion != null) && position?.at(observationTimeMs) == null &&
            motion?.speedAt(observationTimeMs) == null
    }
    else -> false
}
