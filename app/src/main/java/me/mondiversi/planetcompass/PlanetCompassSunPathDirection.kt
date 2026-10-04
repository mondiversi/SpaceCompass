package me.mondiversi.planetcompass

import kotlin.math.hypot

internal const val PLANET_COMPASS_SUN_PATH_ARROW_INTERVAL = 4

internal data class PlanetCompassSunPathDirection(
    val timeMs: Long, val start: PlanetCompassSunVector, val end: PlanetCompassSunVector, val center: PlanetCompassSunVector
)

internal data class PlanetCompassSunPathArrow(
    val center: PlanetCompassSunScenePoint, val tip: PlanetCompassSunScenePoint,
    val left: PlanetCompassSunScenePoint, val right: PlanetCompassSunScenePoint, val belowHorizon: Boolean
)

/** One arrow after every fourth regular dot, between dots rather than over them.
 * Named events never change this cadence. Cache with the path, not orientation frames.
 * Use the existing dense samples: no extra ephemeris calls or invented end-to-start chord.
 */
internal fun planetCompassSunPathDirections(path: PlanetCompassSunDailyPath): List<PlanetCompassSunPathDirection> {
    if (path.samples.size < 2) return emptyList()
    val regular = path.markers.filter { it.event == PlanetCompassSunPathEvent.HOUR }.sortedBy { it.timeMs }
    return buildList {
        for (index in PLANET_COMPASS_SUN_PATH_ARROW_INTERVAL - 1 until regular.size step PLANET_COMPASS_SUN_PATH_ARROW_INTERVAL) {
            val from = regular[index].timeMs
            val until = regular.getOrNull(index + 1)?.timeMs ?: path.samples.last().timeMs
            if (until <= from) continue
            val time = from + (until - from) / 2
            val found = path.samples.binarySearchBy(time) { it.timeMs }
            val segmentIndex = if (found >= 0) found else -found - 2
            val a = path.samples.getOrNull(segmentIndex) ?: continue
            val b = path.samples.getOrNull(segmentIndex + 1) ?: continue
            if (b.timeMs <= a.timeMs) continue
            val fraction = (time - a.timeMs).toDouble() / (b.timeMs - a.timeMs)
            val center = PlanetCompassSunVector(
                a.direction.east + (b.direction.east - a.direction.east) * fraction,
                a.direction.north + (b.direction.north - a.direction.north) * fraction,
                a.direction.up + (b.direction.up - a.direction.up) * fraction).normalized()
            add(PlanetCompassSunPathDirection(time, a.direction, b.direction, center))
        }
    }
}

/** Tangent always points from the earlier dense sample toward the later one.
 * Perspective/roll can reverse screen directions, never the direction of time.
 * Reject hidden, stationary or edge-clipped arrows instead of drawing false directions.
 */
internal fun projectPlanetCompassSunPathArrow(direction: PlanetCompassSunPathDirection, orientation: PlanetCompassSunOrientation,
    width: Double, height: Double, halfLength: Double): PlanetCompassSunPathArrow? {
    if (!width.isFinite() || !height.isFinite() || width <= 0 || height <= 0 ||
        !halfLength.isFinite() || halfLength <= 0) return null
    val depth = direction.center.dot(orientation.forward)
    if (!depth.isFinite() || depth <= 1e-5) return null
    val focal = planetCompassSunProjectionFocalLength(height)
    val center = PlanetCompassSunScenePoint(width / 2 + focal * direction.center.dot(orientation.right) / depth,
        height / 2 - focal * direction.center.dot(orientation.screenUp) / depth)
    val segment = projectPlanetCompassSunPathSegment(direction.start, direction.end, orientation,
        width, height, direction.center.up < 0) ?: return null
    val dx = segment.end.x - segment.start.x
    val dy = segment.end.y - segment.start.y
    val length = hypot(dx, dy)
    if (!length.isFinite() || length < 0.5) return null
    val x = dx / length * halfLength
    val y = dy / length * halfLength
    val tip = PlanetCompassSunScenePoint(center.x + x, center.y + y)
    val left = PlanetCompassSunScenePoint(center.x - x - y * 0.7, center.y - y + x * 0.7)
    val right = PlanetCompassSunScenePoint(center.x - x + y * 0.7, center.y - y - x * 0.7)
    if (listOf(center, tip, left, right).any { !it.x.isFinite() || !it.y.isFinite() ||
            it.x !in 2.0..(width - 2.0) || it.y !in 2.0..(height - 2.0) }) return null
    return PlanetCompassSunPathArrow(center, tip, left, right, direction.center.up < 0)
}
