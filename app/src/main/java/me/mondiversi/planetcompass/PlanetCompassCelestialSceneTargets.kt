package me.mondiversi.planetcompass

import kotlin.math.hypot

/** A single shared hit/focus list keeps the body's identity and each marker's exact provenance. */
internal data class PlanetCompassCelestialSceneTarget(val body: PlanetCompassCelestialBody, val point: PlanetCompassSunPathPoint,
    val projection: PlanetCompassSunScenePoint, val path: PlanetCompassSunDailyPath? = null, val isCurrent: Boolean = false)

internal fun projectPlanetCompassCelestialSceneTargets(paths: Map<PlanetCompassCelestialBody, PlanetCompassSunDailyPath>,
    current: Map<PlanetCompassCelestialBody, PlanetCompassSunPathPoint>, orientation: PlanetCompassSunOrientation?,
    width: Double, height: Double): List<PlanetCompassCelestialSceneTarget> {
    if (orientation == null || !width.isFinite() || !height.isFinite() || width <= 0 || height <= 0) return emptyList()
    fun target(body: PlanetCompassCelestialBody, point: PlanetCompassSunPathPoint, path: PlanetCompassSunDailyPath?, live: Boolean): PlanetCompassCelestialSceneTarget? =
        projectPlanetCompassSun(point.position, orientation, width, height).takeIf { it.visible }?.let {
            PlanetCompassCelestialSceneTarget(body, point, PlanetCompassSunScenePoint(it.x, it.y), path, live)
        }
    // Live positions, then named events, then hourly points: the order resolves exact ties only.
    return planetCompassCelestialCatalogOrder.mapNotNull { body -> current[body]?.let { target(body, it, paths[body], true) } } +
        planetCompassCelestialCatalogOrder.flatMap { body -> paths[body]?.let { path ->
            path.markers.mapNotNull { target(body, it, path, false) }
        }.orEmpty() }.sortedBy { if (it.point.event == PlanetCompassSunPathEvent.HOUR) 1 else 0 }
}

internal fun focusedPlanetCompassCelestialSceneTarget(targets: List<PlanetCompassCelestialSceneTarget>, width: Double, height: Double,
    radius: Double): PlanetCompassCelestialSceneTarget? = focusedPlanetCompassCelestialPathPoint(
    targets.map { it to it.projection }, width, height, radius) { it.isCurrent || it.point.event != PlanetCompassSunPathEvent.HOUR }

internal fun tappedPlanetCompassCelestialSceneTarget(targets: List<PlanetCompassCelestialSceneTarget>, tap: PlanetCompassSunScenePoint,
    radius: Double): PlanetCompassCelestialSceneTarget? {
    if (!tap.x.isFinite() || !tap.y.isFinite() || !radius.isFinite() || radius < 0) return null
    return targets.filter { hypot(it.projection.x - tap.x, it.projection.y - tap.y) <= radius }
        .minWithOrNull(compareBy<PlanetCompassCelestialSceneTarget> { hypot(it.projection.x - tap.x, it.projection.y - tap.y) }
            .thenBy { if (it.isCurrent) 0 else if (it.point.event != PlanetCompassSunPathEvent.HOUR) 1 else 2 })
}
