package me.mondiversi.spacecompass

import kotlin.math.hypot

internal const val SPACE_COMPASS_SUN_PATH_ARROW_INTERVAL = 4

internal data class SpaceCompassSunPathDirection(
    val timeMs: Long, val start: SpaceCompassSunVector, val end: SpaceCompassSunVector, val center: SpaceCompassSunVector
)

internal data class SpaceCompassSunPathArrow(
    val center: SpaceCompassSunScenePoint, val tip: SpaceCompassSunScenePoint,
    val left: SpaceCompassSunScenePoint, val right: SpaceCompassSunScenePoint, val belowHorizon: Boolean
)

/** One arrow after every fourth regular dot, between dots rather than over them.
 * Named events never change this cadence. Cache with the path, not orientation frames.
 * Use the existing dense samples: no extra ephemeris calls or invented end-to-start chord.
 */
internal fun spaceCompassSunPathDirections(path: SpaceCompassSunDailyPath): List<SpaceCompassSunPathDirection> {
    if (path.samples.size < 2) return emptyList()
    val regular = path.markers.filter { it.event == SpaceCompassSunPathEvent.HOUR }.sortedBy { it.timeMs }
    return buildList {
        for (index in SPACE_COMPASS_SUN_PATH_ARROW_INTERVAL - 1 until regular.size step SPACE_COMPASS_SUN_PATH_ARROW_INTERVAL) {
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
            val center = SpaceCompassSunVector(
                a.direction.east + (b.direction.east - a.direction.east) * fraction,
                a.direction.north + (b.direction.north - a.direction.north) * fraction,
                a.direction.up + (b.direction.up - a.direction.up) * fraction).normalized()
            add(SpaceCompassSunPathDirection(time, a.direction, b.direction, center))
        }
    }
}

/** Tangent always points from the earlier dense sample toward the later one.
 * Perspective/roll can reverse screen directions, never the direction of time.
 * Reject hidden, stationary or edge-clipped arrows instead of drawing false directions.
 */
internal fun projectSpaceCompassSunPathArrow(direction: SpaceCompassSunPathDirection, orientation: SpaceCompassSunOrientation,
    width: Double, height: Double, halfLength: Double, perspective: SpaceCompassPerspective? = null): SpaceCompassSunPathArrow? {
    if (!width.isFinite() || !height.isFinite() || width <= 0 || height <= 0 ||
        !halfLength.isFinite() || halfLength <= 0) return null
    val depth = direction.center.dot(orientation.forward)
    if (!depth.isFinite() || depth <= 1e-5) return null
    val focal = perspective?.focalY(height) ?: spaceCompassSunProjectionFocalLength(height)
    val focalX = perspective?.focalX(width) ?: focal
    val center = SpaceCompassSunScenePoint((perspective?.principalX ?: .5) * width + focalX * direction.center.dot(orientation.right) / depth,
        (perspective?.principalY ?: .5) * height - focal * direction.center.dot(orientation.screenUp) / depth)
    val segment = projectSpaceCompassSunPathSegment(direction.start, direction.end, orientation,
        width, height, direction.center.up < 0, perspective) ?: return null
    val dx = segment.end.x - segment.start.x
    val dy = segment.end.y - segment.start.y
    val length = hypot(dx, dy)
    if (!length.isFinite() || length < 0.5) return null
    val x = dx / length * halfLength
    val y = dy / length * halfLength
    val tip = SpaceCompassSunScenePoint(center.x + x, center.y + y)
    val left = SpaceCompassSunScenePoint(center.x - x - y * 0.7, center.y - y + x * 0.7)
    val right = SpaceCompassSunScenePoint(center.x - x + y * 0.7, center.y - y - x * 0.7)
    if (listOf(center, tip, left, right).any { !it.x.isFinite() || !it.y.isFinite() ||
            it.x !in 2.0..(width - 2.0) || it.y !in 2.0..(height - 2.0) }) return null
    return SpaceCompassSunPathArrow(center, tip, left, right, direction.center.up < 0)
}
