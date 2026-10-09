package me.mondiversi.spacecompass

import kotlin.math.min

/** Use measured text sizes and leave a real gap; a crowded capture must never force text overlap. */
internal fun spaceCompassPanoramaLabelBounds(point: SpaceCompassSunScenePoint, labelWidth: Double,
    labelHeight: Double, width: Double, height: Double, scale: Double,
    occupied: List<SpaceCompassSunSceneFrame>, pointRadius: Double = 0.0): SpaceCompassSunSceneFrame? {
    if (listOf(point.x, point.y, labelWidth, labelHeight, width, height, scale, pointRadius).any { !it.isFinite() } ||
        labelWidth <= 0 || labelHeight <= 0 || scale <= 0 || pointRadius < 0) return null
    val edge = 4 * scale
    val gap = 8 * scale
    val minimumY = 76 * scale
    val maximumY = height - 48 * scale - labelHeight
    if (labelWidth > width - edge * 2 || minimumY > maximumY) return null
    val half = labelWidth / 2
    val centerX = point.x.coerceIn(half + edge, width - half - edge)
    val startGap = (pointRadius + 16) * scale
    val stepY = labelHeight + gap
    val stepX = labelWidth + gap
    val rows = min(12, ((maximumY - minimumY) / stepY).toInt())
    var best: SpaceCompassSunSceneFrame? = null
    var bestDistance = Double.POSITIVE_INFINITY
    for (row in 0..rows) for (shift in listOf(0.0, -stepX / 2, stepX / 2, -stepX, stepX)) {
        val x = (centerX + shift).coerceIn(half + edge, width - half - edge)
        for (above in listOf(false, true)) {
            val y = if (above) point.y - startGap - labelHeight - row * stepY
                else point.y + startGap + row * stepY
            if (y < minimumY || y > maximumY) continue
            val dx = x - point.x; val dy = y + labelHeight / 2 - point.y
            val distance = dx * dx + dy * dy
            if (distance >= bestDistance) continue
            val candidate = SpaceCompassSunSceneFrame(x - half, y, labelWidth, labelHeight)
            if (occupied.any { b -> candidate.left < b.left + b.width + gap && b.left < candidate.left + labelWidth + gap &&
                    candidate.top < b.top + b.height + gap && b.top < candidate.top + labelHeight + gap }) continue
            best = candidate
            bestDistance = distance
        }
    }
    return best
}
