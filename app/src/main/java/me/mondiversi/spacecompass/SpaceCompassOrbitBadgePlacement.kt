package me.mondiversi.spacecompass

import kotlin.math.hypot

/** Try nearby free areas before moving a pinned caption farther from its dot. */
internal fun placeSpaceCompassOrbitBadge(width: Double, height: Double, labelWidth: Double, labelHeight: Double,
    anchor: SpaceCompassSunScenePoint, margin: Double, gap: Double,
    excluded: List<SpaceCompassSunSceneFrame>, occupied: List<SpaceCompassSunSceneFrame>, pinned: Boolean): SpaceCompassSunScenePoint? {
    val initial = placeSpaceCompassCelestialTimeBadge(width, height, labelWidth, labelHeight, anchor, margin, gap, excluded + occupied)
    if (initial != null) return initial
    if (listOf(width, height, labelWidth, labelHeight, margin, gap, anchor.x, anchor.y).any { !it.isFinite() } ||
        labelWidth <= 0 || labelHeight <= 0 || width < labelWidth + margin * 2 || height < labelHeight + margin * 2) return null
    fun overlap(point: SpaceCompassSunScenePoint, frame: SpaceCompassSunSceneFrame): Double =
        (minOf(point.x + labelWidth, frame.left + frame.width) - maxOf(point.x, frame.left)).coerceAtLeast(0.0) *
            (minOf(point.y + labelHeight, frame.top + frame.height) - maxOf(point.y, frame.top)).coerceAtLeast(0.0)
    val candidates = buildList {
        val step = maxOf(8.0, labelHeight / 2)
        var y = margin
        while (y <= height - labelHeight - margin) {
            var x = margin
            while (x <= width - labelWidth - margin) { add(SpaceCompassSunScenePoint(x, y)); x += step }
            add(SpaceCompassSunScenePoint(width - labelWidth - margin, y))
            y += step
        }
    }.filter { point -> excluded.none { overlap(point, it) > 0 } }
    val nearest = candidates.sortedBy { hypot(it.x + labelWidth / 2 - anchor.x, it.y + labelHeight / 2 - anchor.y) }
    return nearest.firstOrNull { point -> occupied.none { overlap(point, it) > 0 } }
        ?: if (pinned) nearest.minByOrNull { point -> occupied.sumOf { overlap(point, it) } } else null
}
