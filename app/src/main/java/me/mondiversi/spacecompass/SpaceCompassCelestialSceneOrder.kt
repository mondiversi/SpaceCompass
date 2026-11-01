package me.mondiversi.spacecompass

/** Build the catalog rank once; frame work scales with displayed bodies, not the catalog. */
private val spaceCompassCelestialSceneRanks by lazy {
    spaceCompassAllCelestialOrder.withIndex().associate { it.value to it.index }
}

/** Preserve the canonical paint/navigation order and omit retired identities. */
internal fun spaceCompassOrderedCelestialSubset(bodies: Set<SpaceCompassCelestialBody>): List<SpaceCompassCelestialBody> =
    bodies.filter { it in spaceCompassCelestialSceneRanks }.sortedBy { spaceCompassCelestialSceneRanks.getValue(it) }
