package me.mondiversi.spacecompass

/** Only online models need warming; local planets/stars already have distances without downloads. */
internal fun spaceCompassCelestialRefreshBodies(selected: Set<SpaceCompassCelestialBody>,
    motionBodies: Set<SpaceCompassCelestialBody>, prefetchCatalog: Boolean): List<SpaceCompassCelestialBody> =
    spaceCompassCelestialCatalogOrder.filter {
        (prefetchCatalog || it in selected) && (it.usesHorizons || it.isEarthSatellite)
    }.sortedBy {
        when {
            it in selected && it in motionBodies -> 0
            it in selected -> 1
            else -> 2
        }
    }

internal fun spaceCompassCelestialRefreshRequests(body: SpaceCompassCelestialBody,
    selected: Set<SpaceCompassCelestialBody>, motionBodies: Set<SpaceCompassCelestialBody>,
    prefetchCatalog: Boolean, pointingUsable: Boolean, motionUsable: Boolean): List<SpaceCompassCelestialRequest> {
    val position = SpaceCompassCelestialRequest(body)
    val motion = SpaceCompassCelestialRequest(body, motion = true)
    if (body.isEarthSatellite || (!prefetchCatalog && body !in motionBodies)) return listOf(position)
    // An unchecked Horizons object's heliocentric vectors fill its catalog distance/speed first.
    return if ((prefetchCatalog && body !in selected) || (pointingUsable && !motionUsable))
        listOf(motion, position) else listOf(position, motion)
}
