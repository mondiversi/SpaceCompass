package me.mondiversi.spacecompass

import kotlin.math.*

/** Space shaped glyphs on their actual offset curve, not on the orbit underneath. */
internal fun spaceCompassOrbitTextBaseline(
    segments: List<SpaceCompassSunPathSegment>, center: SpaceCompassSunScenePoint,
    textWidth: Double, textSize: Double, offset: Double
): List<SpaceCompassSunScenePoint>? {
    if (!textWidth.isFinite() || textWidth <= 0 || !textSize.isFinite() || textSize <= 0 ||
        !offset.isFinite() || offset < 0) return null
    val needed = textWidth + textSize * .8
    var span = needed
    repeat(3) {
        val orbit = spaceCompassOrbitLabelBaseline(segments, center, span) ?: return null
        val points = mutableListOf<SpaceCompassSunScenePoint>()
        orbit.forEach { point ->
            if (points.lastOrNull()?.let { hypot(point.x - it.x, point.y - it.y) > 1e-5 } != false) points += point
        }
        if (points.size < 2) return null
        val directions = points.zipWithNext().map { (a, b) ->
            val length = hypot(b.x - a.x, b.y - a.y)
            SpaceCompassSunScenePoint((b.x - a.x) / length, (b.y - a.y) / length)
        }
        val shifted = points.mapIndexed { i, point ->
            val before = directions[(i - 1).coerceAtLeast(0)]
            val after = directions[i.coerceAtMost(directions.lastIndex)]
            val denominator = 1 + before.x * after.x + before.y * after.y
            if (denominator < 1.5) return null // Reject corners that would create a sharp offset spike.
            val amount = offset / denominator
            SpaceCompassSunScenePoint(point.x + (before.y + after.y) * amount,
                point.y - (before.x + after.x) * amount)
        }
        val length = shifted.zipWithNext().sumOf { (a, b) -> hypot(b.x - a.x, b.y - a.y) }
        if (length + 1e-5 < needed) {
            span = span * needed / length + textSize * .05
        } else {
            val baseline = trimSpaceCompassOrbitTextCurve(shifted, (length - needed) / 2, needed)
            // Ascenders/outlines converge on the inside of a tight bend, even with correct advances.
            val steps = baseline.zipWithNext().map { (a, b) ->
                Triple(b.x - a.x, b.y - a.y, hypot(b.x - a.x, b.y - a.y))
            }
            var totalTurn = 0.0
            steps.zipWithNext().forEach { (a, b) ->
                val turn = abs(atan2(a.first * b.second - a.second * b.first,
                    a.first * b.first + a.second * b.second))
                totalTurn += turn
                if (turn * textSize * 2.5 > (a.third + b.third) / 2) return null
            }
            if (totalTurn > PI / 2) return null
            return baseline
        }
    }
    return null
}

private fun trimSpaceCompassOrbitTextCurve(points: List<SpaceCompassSunScenePoint>, start: Double,
    length: Double): List<SpaceCompassSunScenePoint> {
    val result = mutableListOf<SpaceCompassSunScenePoint>()
    var traveled = 0.0
    points.zipWithNext().forEach { (a, b) ->
        val step = hypot(b.x - a.x, b.y - a.y)
        if (step > 1e-5 && traveled + step > start && traveled < start + length) {
            fun at(distance: Double): SpaceCompassSunScenePoint {
                val fraction = ((distance - traveled) / step).coerceIn(0.0, 1.0)
                return SpaceCompassSunScenePoint(a.x + (b.x - a.x) * fraction, a.y + (b.y - a.y) * fraction)
            }
            if (result.isEmpty()) result += at(start)
            result += at(minOf(traveled + step, start + length))
        }
        traveled += step
    }
    return result
}
