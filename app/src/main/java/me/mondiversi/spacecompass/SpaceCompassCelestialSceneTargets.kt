package me.mondiversi.spacecompass

import kotlin.math.hypot

/** A single shared hit/focus list keeps the body's identity and each marker's exact provenance. */
internal data class SpaceCompassCelestialSceneTarget(val body: SpaceCompassCelestialBody, val point: SpaceCompassSunPathPoint,
    val projection: SpaceCompassSunScenePoint, val path: SpaceCompassSunDailyPath? = null, val isCurrent: Boolean = false)

internal fun projectSpaceCompassCelestialSceneTargets(paths: Map<SpaceCompassCelestialBody, SpaceCompassSunDailyPath>,
    current: Map<SpaceCompassCelestialBody, SpaceCompassSunPathPoint>, orientation: SpaceCompassSunOrientation?,
    width: Double, height: Double, perspective: SpaceCompassPerspective? = null): List<SpaceCompassCelestialSceneTarget> {
    if (orientation == null || !width.isFinite() || !height.isFinite() || width <= 0 || height <= 0) return emptyList()
    fun target(body: SpaceCompassCelestialBody, point: SpaceCompassSunPathPoint, path: SpaceCompassSunDailyPath?, live: Boolean): SpaceCompassCelestialSceneTarget? =
        projectSpaceCompassSun(point.position, orientation, width, height, perspective).takeIf { it.visible }?.let {
            SpaceCompassCelestialSceneTarget(body, point, SpaceCompassSunScenePoint(it.x, it.y), path, live)
        }
    // Live positions, then named events, then hourly points: the order resolves exact ties only.
    return spaceCompassAllCelestialOrder.mapNotNull { body -> current[body]?.let { target(body, it, paths[body], true) } } +
        spaceCompassAllCelestialOrder.flatMap { body -> paths[body]?.let { path ->
            path.markers.mapNotNull { target(body, it, path, false) }
        }.orEmpty() }.sortedBy { if (it.point.event == SpaceCompassSunPathEvent.HOUR) 1 else 0 }
}

internal fun focusedSpaceCompassCelestialSceneTarget(targets: List<SpaceCompassCelestialSceneTarget>, width: Double, height: Double,
    radius: Double): SpaceCompassCelestialSceneTarget? = focusedSpaceCompassCelestialPathPoint(
    targets.map { it to it.projection }, width, height, radius) { it.isCurrent || it.point.event != SpaceCompassSunPathEvent.HOUR }

internal fun tappedSpaceCompassCelestialSceneTarget(targets: List<SpaceCompassCelestialSceneTarget>, tap: SpaceCompassSunScenePoint,
    radius: Double): SpaceCompassCelestialSceneTarget? {
    if (!tap.x.isFinite() || !tap.y.isFinite() || !radius.isFinite() || radius < 0) return null
    return targets.filter { hypot(it.projection.x - tap.x, it.projection.y - tap.y) <= radius }
        .minWithOrNull(compareBy<SpaceCompassCelestialSceneTarget> { hypot(it.projection.x - tap.x, it.projection.y - tap.y) }
            .thenBy { if (it.isCurrent) 0 else if (it.point.event != SpaceCompassSunPathEvent.HOUR) 1 else 2 })
}
