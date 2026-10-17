package me.mondiversi.spacecompass

/** New moon velocities use direct geometric states relative to their own primary.
 * Other Horizons objects retain the existing Sun-centred motion/caches. */
internal val SpaceCompassCelestialBody.horizonsMotionPrimary: SpaceCompassCelestialBody?
    get() = if (additionalCatalogEntry?.type == SpaceCompassCatalogType.NATURAL_SATELLITE)
        spaceCompassAdditionalCatalogFacts(this)?.parent else null

internal val SpaceCompassCelestialBody.horizonsCentreCode: Int get() = when (this) {
    SpaceCompassCelestialBody.MARS -> 499
    SpaceCompassCelestialBody.JUPITER -> 599
    SpaceCompassCelestialBody.SATURN -> 699
    SpaceCompassCelestialBody.URANUS -> 799
    SpaceCompassCelestialBody.NEPTUNE -> 899
    SpaceCompassCelestialBody.PLUTO -> 999
    else -> error("Unsupported moon primary: $this")
}
