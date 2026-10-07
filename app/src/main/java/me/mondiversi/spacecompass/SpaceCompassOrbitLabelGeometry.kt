package me.mondiversi.spacecompass

import kotlin.math.*

/** A readable baseline cut from the actual projected curve, never an invented straight chord. */
internal fun spaceCompassOrbitLabelBaseline(segments: List<SpaceCompassSunPathSegment>,
    center: SpaceCompassSunScenePoint, span: Double): List<SpaceCompassSunScenePoint>? {
    if (!span.isFinite() || span <= 0 || !center.x.isFinite() || !center.y.isFinite()) return null
    fun distance(a: SpaceCompassSunScenePoint, b: SpaceCompassSunScenePoint) = hypot(a.x - b.x, a.y - b.y)
    val valid = segments.filter { listOf(it.start.x, it.start.y, it.end.x, it.end.y).all(Double::isFinite) }
    val runs = mutableListOf<MutableList<SpaceCompassSunScenePoint>>()
    valid.forEach { segment ->
        if (runs.lastOrNull()?.last()?.let { distance(it, segment.start) < .5 } != true)
            runs += mutableListOf(segment.start)
        runs.last() += segment.end
    }
    var best: List<SpaceCompassSunScenePoint>? = null
    var nearest = 8.0
    runs.forEach { run ->
        val lengths = mutableListOf(0.0)
        run.zipWithNext().forEach { (a, b) -> lengths += lengths.last() + distance(a, b) }
        var anchor = -1.0
        var runNearest = nearest
        run.zipWithNext().forEachIndexed { index, (a, b) ->
            val dx = b.x - a.x; val dy = b.y - a.y
            val squared = dx * dx + dy * dy
            if (squared < 1e-8) return@forEachIndexed
            val t = (((center.x - a.x) * dx + (center.y - a.y) * dy) / squared).coerceIn(0.0, 1.0)
            val d = distance(center, SpaceCompassSunScenePoint(a.x + dx * t, a.y + dy * t))
            val at = lengths[index] + sqrt(squared) * t
            if (d >= runNearest || at < span / 2 || lengths.last() - at < span / 2) return@forEachIndexed
            anchor = at
            runNearest = d
        }
        if (anchor >= 0) {
            fun pointAt(s: Double): SpaceCompassSunScenePoint {
                val i = (lengths.indexOfFirst { it >= s }.coerceAtLeast(1) - 1).coerceAtMost(run.lastIndex - 1)
                val f = ((s - lengths[i]) / (lengths[i + 1] - lengths[i])).coerceIn(0.0, 1.0)
                return SpaceCompassSunScenePoint(run[i].x + (run[i + 1].x - run[i].x) * f,
                    run[i].y + (run[i + 1].y - run[i].y) * f)
            }
            val low = anchor - span / 2; val high = anchor + span / 2
            val cut = listOf(pointAt(low)) + run.filterIndexed { i, _ -> lengths[i] > low && lengths[i] < high } + pointAt(high)
            // Keep text upright through camera roll and backwards apparent movement.
            best = if (cut.last().x < cut.first().x ||
                (abs(cut.last().x - cut.first().x) < 1e-5 && cut.last().y > cut.first().y)) cut.reversed() else cut
            nearest = runNearest
        }
    }
    return best
}

/** Equidistant azimuth/elevation panorama: north wraps at the seam, zenith/nadir at top/bottom. */
internal fun spaceCompassPanoramaPoint(position: SpaceCompassSunPosition, width: Double, height: Double,
    centerAzimuthDegrees: Double = 180.0): SpaceCompassSunScenePoint? {
    if (!width.isFinite() || !height.isFinite() || width <= 0 || height <= 0 ||
        !centerAzimuthDegrees.isFinite() || !position.azimuthDegrees.isFinite() || !position.elevationDegrees.isFinite() || position.elevationDegrees !in -90.0..90.0) return null
    val az = wrapSpaceCompassSunDegrees(position.azimuthDegrees - (centerAzimuthDegrees - 180.0))
    return SpaceCompassSunScenePoint(az / 360 * width, (90 - position.elevationDegrees) / 180 * height)
}

internal fun spaceCompassPanoramaVectorPoint(v: SpaceCompassSunVector, width: Double, height: Double,
    centerAzimuthDegrees: Double = 180.0): SpaceCompassSunScenePoint? =
    spaceCompassPanoramaPoint(SpaceCompassSunPosition(Math.toDegrees(atan2(v.east, v.north)),
        Math.toDegrees(atan2(v.up, hypot(v.east, v.north)))), width, height, centerAzimuthDegrees)

/** Split the north seam and horizon; never draw a 359°-to-1° chord across the image. */
internal fun spaceCompassPanoramaSegments(path: SpaceCompassSunDailyPath, width: Double, height: Double,
    centerAzimuthDegrees: Double = 180.0): List<SpaceCompassSunPathSegment> = buildList {
    path.samples.zipWithNext().forEach { (a, b) ->
        val start = spaceCompassPanoramaPoint(a.position, width, height, centerAzimuthDegrees) ?: return@forEach
        val originalEnd = spaceCompassPanoramaPoint(b.position, width, height, centerAzimuthDegrees) ?: return@forEach
        val dx = originalEnd.x - start.x
        val end = originalEnd.copy(x = originalEnd.x + if (dx > width / 2) -width else if (dx < -width / 2) width else 0.0)
        fun lerp(t: Double) = SpaceCompassSunScenePoint(start.x + (end.x - start.x) * t, start.y + (end.y - start.y) * t)
        val cuts = mutableListOf(0.0, 1.0)
        if ((start.y < height / 2) != (end.y < height / 2) && start.y != end.y)
            cuts += (height / 2 - start.y) / (end.y - start.y)
        if (end.x < 0) cuts += -start.x / (end.x - start.x)
        if (end.x > width) cuts += (width - start.x) / (end.x - start.x)
        cuts.distinct().sorted().zipWithNext().forEach { (lo, hi) ->
            if (hi - lo < 1e-9) return@forEach
            val mid = lerp((lo + hi) / 2)
            val shift = if (mid.x < 0) width else if (mid.x > width) -width else 0.0
            add(SpaceCompassSunPathSegment(lerp(lo).let { it.copy(x = (it.x + shift).coerceIn(0.0, width)) },
                lerp(hi).let { it.copy(x = (it.x + shift).coerceIn(0.0, width)) }, mid.y > height / 2))
        }
    }
}
