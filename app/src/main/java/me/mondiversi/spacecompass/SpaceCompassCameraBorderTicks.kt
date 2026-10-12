package me.mondiversi.spacecompass

import kotlin.math.*

/** Read the actual shutter rays on each border, independently of curve tessellation or phone roll. */
internal fun spaceCompassCameraBorderTicks(orientation: SpaceCompassSunOrientation,
    perspective: SpaceCompassPerspective, width: Double, height: Double,
    frame: SpaceCompassSunSceneFrame): List<SpaceCompassCameraGridTick> {
    val scale = min(width, height) / 1080.0
    return SpaceCompassCameraGridEdge.entries.flatMap { edge ->
        val horizontal = edge == SpaceCompassCameraGridEdge.TOP || edge == SpaceCompassCameraGridEdge.BOTTOM
        val axis = if (horizontal) SpaceCompassCameraGridAxis.AZIMUTH else SpaceCompassCameraGridAxis.ELEVATION
        val length = if (horizontal) frame.width else frame.height
        val spacing = (if (horizontal) 100.0 else 70.0) * scale
        fun point(t: Double) = if (horizontal) SpaceCompassSunScenePoint(frame.left + t * frame.width,
            if (edge == SpaceCompassCameraGridEdge.TOP) frame.top else frame.top + frame.height)
            else SpaceCompassSunScenePoint(if (edge == SpaceCompassCameraGridEdge.LEFT) frame.left else frame.left + frame.width,
                frame.top + t * frame.height)
        fun angle(t: Double): Double {
            val p = point(t)
            val ray = spaceCompassCameraPixelPosition(p.x, p.y, width, height, orientation, perspective)
            return if (horizontal) ray.azimuthDegrees else ray.elevationDegrees
        }
        fun delta(a: Double, b: Double) = if (horizontal) wrapSpaceCompassSunDegrees(a - b + 180) - 180 else a - b
        // Leave room for corner labels. Unwrap north continuously, without creating a 360-degree jump.
        val positions = (0..128).map { .06 + .88 * it / 128 }
        val raw = positions.map(::angle)
        val values = mutableListOf(raw.first())
        for (i in 1 until raw.size) values += values.last() + delta(raw[i], raw[i - 1])
        val span = values.max() - values.min()
        val desired = span / (length / spacing).coerceAtLeast(2.0)
        val power = 10.0.pow(floor(log10(desired.coerceAtLeast(.001))))
        val step = (listOf(1.0, 2.0, 5.0, 10.0).first { it * power >= desired } * power).coerceIn(.01, 30.0)
        val decimals = max(0, -floor(log10(step)).toInt())
        val candidates = mutableListOf<Pair<Double, SpaceCompassCameraGridTick>>()
        if (span > 1e-7) {
            for (i in 0 until positions.lastIndex) {
                val a = values[i]; val b = values[i + 1]
                // Azimuth is undefined at a pole; never interpolate across that singular ray.
                if (abs(b - a) > 60 || abs(b - a) < 1e-12) continue
                val first = ceil(min(a, b) / step).toInt()
                val last = floor(max(a, b) / step).toInt()
                for (number in first..last) {
                    val target = number * step
                    var low = positions[i]; var high = positions[i + 1]
                    repeat(35) {
                        val mid = (low + high) / 2
                        val value = a + delta(angle(mid), raw[i])
                        if ((value < target) == (b > a)) low = mid else high = mid
                    }
                    val t = (low + high) / 2
                    val value = if (horizontal) wrapSpaceCompassSunDegrees(target) else target
                    candidates += t to SpaceCompassCameraGridTick(axis, if (abs(value) < 1e-9) 0.0 else value, edge, point(t), decimals)
                }
            }
        }
        val kept = mutableListOf<Pair<Double, SpaceCompassCameraGridTick>>()
        // Give the centre priority and prevent dense labels close to zenith/nadir.
        candidates.sortedBy { abs(it.first - .5) }.forEach { candidate ->
            if (kept.none { abs(it.first - candidate.first) * length < spacing * .7 }) kept += candidate
        }
        // A nearly constant axis (e.g. a phone rolled 90 degrees) still has a truthful reading.
        if (kept.isEmpty()) kept += .5 to SpaceCompassCameraGridTick(axis, angle(.5), edge, point(.5), if (span < 1e-7) 1 else max(1, decimals))
        kept.sortedBy { it.first }.map { it.second }
    }
}
