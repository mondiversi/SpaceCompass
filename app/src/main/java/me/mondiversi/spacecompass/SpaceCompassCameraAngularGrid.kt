package me.mondiversi.spacecompass

import kotlin.math.*

internal enum class SpaceCompassCameraGridAxis { AZIMUTH, ELEVATION }
internal enum class SpaceCompassCameraGridEdge { TOP, BOTTOM, LEFT, RIGHT }
internal data class SpaceCompassCameraGridLine(val axis: SpaceCompassCameraGridAxis, val degrees: Double,
    val segments: List<SpaceCompassSunPathSegment>)
internal data class SpaceCompassCameraGridTick(val axis: SpaceCompassCameraGridAxis, val degrees: Double,
    val edge: SpaceCompassCameraGridEdge, val point: SpaceCompassSunScenePoint)
internal data class SpaceCompassCameraAngularGrid(val lines: List<SpaceCompassCameraGridLine>,
    val ticks: List<SpaceCompassCameraGridTick>)

/** Pixel ray uses the JPEG optical center/focal axes, including roll and off-center optics. */
internal fun spaceCompassCameraPixelPosition(x: Double, y: Double, width: Double, height: Double,
    orientation: SpaceCompassSunOrientation, perspective: SpaceCompassPerspective): SpaceCompassSunPosition {
    require(width > 0 && height > 0 && x.isFinite() && y.isFinite())
    val right = (x - perspective.principalX * width) / perspective.focalX(width)
    val up = -(y - perspective.principalY * height) / perspective.focalY(height)
    val vector = SpaceCompassSunVector(
        orientation.forward.east + orientation.right.east * right + orientation.screenUp.east * up,
        orientation.forward.north + orientation.right.north * right + orientation.screenUp.north * up,
        orientation.forward.up + orientation.right.up * right + orientation.screenUp.up * up)
    return SpaceCompassSunPosition(wrapSpaceCompassSunDegrees(Math.toDegrees(atan2(vector.east, vector.north))),
        Math.toDegrees(atan2(vector.up, hypot(vector.east, vector.north))))
}

/** Further clip already front-facing projected segments to the unobscured photographic grid. */
internal fun spaceCompassCameraGridClip(segment: SpaceCompassSunPathSegment,
    frame: SpaceCompassSunSceneFrame): SpaceCompassSunPathSegment? {
    val dx = segment.end.x - segment.start.x; val dy = segment.end.y - segment.start.y
    var low = 0.0; var high = 1.0
    for ((p, q) in listOf(-dx to segment.start.x - frame.left,
        dx to frame.left + frame.width - segment.start.x, -dy to segment.start.y - frame.top,
        dy to frame.top + frame.height - segment.start.y)) {
        if (abs(p) < 1e-12) { if (q < 0) return null } else {
            val t = q / p
            if (p < 0) low = max(low, t) else high = min(high, t)
            if (low > high) return null
        }
    }
    fun point(t: Double) = SpaceCompassSunScenePoint(segment.start.x + dx * t, segment.start.y + dy * t)
    return segment.copy(start = point(low), end = point(high))
}

/** Curves use the same shutter-time projection as objects/orbits; back-facing rays never create ticks. */
internal fun spaceCompassCameraAngularGrid(orientation: SpaceCompassSunOrientation,
    perspective: SpaceCompassPerspective, width: Double, height: Double,
    frame: SpaceCompassSunSceneFrame, step: Int = 10): SpaceCompassCameraAngularGrid {
    require(width > 0 && height > 0 && frame.width > 0 && frame.height > 0 && step in listOf(5, 10, 15, 30))
    val lines = mutableListOf<SpaceCompassCameraGridLine>()
    val ticks = mutableListOf<SpaceCompassCameraGridTick>()
    fun direction(az: Double, el: Double): SpaceCompassSunVector {
        val a = Math.toRadians(az); val e = Math.toRadians(el)
        return SpaceCompassSunVector(cos(e) * sin(a), cos(e) * cos(a), sin(e))
    }
    fun line(axis: SpaceCompassCameraGridAxis, degrees: Double, rays: List<SpaceCompassSunVector>) {
        val segments = rays.zipWithNext().mapNotNull { (a, b) ->
            projectSpaceCompassSunPathSegment(a, b, orientation, width, height, (a.up + b.up) < 0, perspective)
                ?.let { spaceCompassCameraGridClip(it, frame) }
        }
        if (segments.isEmpty()) return
        lines += SpaceCompassCameraGridLine(axis, degrees, segments)
        for (p in segments.flatMap { listOf(it.start, it.end) }) {
            val edge = when {
                abs(p.y - frame.top) < 1e-5 -> SpaceCompassCameraGridEdge.TOP
                abs(p.y - frame.top - frame.height) < 1e-5 -> SpaceCompassCameraGridEdge.BOTTOM
                abs(p.x - frame.left) < 1e-5 -> SpaceCompassCameraGridEdge.LEFT
                abs(p.x - frame.left - frame.width) < 1e-5 -> SpaceCompassCameraGridEdge.RIGHT
                else -> null
            } ?: continue
            if ((axis == SpaceCompassCameraGridAxis.AZIMUTH && edge !in setOf(SpaceCompassCameraGridEdge.TOP, SpaceCompassCameraGridEdge.BOTTOM)) ||
                (axis == SpaceCompassCameraGridAxis.ELEVATION && edge != SpaceCompassCameraGridEdge.LEFT)) continue
            if (ticks.none { it.axis == axis && it.degrees == degrees && it.edge == edge && hypot(it.point.x - p.x, it.point.y - p.y) < .1 })
                ticks += SpaceCompassCameraGridTick(axis, degrees, edge, p)
        }
    }
    for (az in 0 until 360 step step) line(SpaceCompassCameraGridAxis.AZIMUTH, az.toDouble(),
        (-90..90 step 2).map { direction(az.toDouble(), it.toDouble()) })
    for (el in -90..90 step step) if (abs(el) < 90) line(SpaceCompassCameraGridAxis.ELEVATION, el.toDouble(),
        (0..360 step 2).map { direction(it.toDouble(), el.toDouble()) })
    return SpaceCompassCameraAngularGrid(lines, ticks)
}
