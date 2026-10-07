package me.mondiversi.spacecompass

internal data class SpaceCompassCurveTextLayout(val baseline: List<SpaceCompassSunScenePoint>,
    val bounds: SpaceCompassSunSceneFrame, val anchor: SpaceCompassSunScenePoint)

/** Shared with orbit names: shaped-text advances belong to the actual offset curve. */
internal fun spaceCompassCurveTextLayout(segments: List<SpaceCompassSunPathSegment>,
    center: SpaceCompassSunScenePoint, textWidth: Double, textSize: Double, offset: Double): SpaceCompassCurveTextLayout? {
    val baseline = spaceCompassOrbitTextBaseline(segments, center, textWidth, textSize, offset) ?: return null
    val left = baseline.minOf { it.x } - textSize
    val top = baseline.minOf { it.y } - textSize * 1.6
    val right = baseline.maxOf { it.x } + textSize
    val bottom = baseline.maxOf { it.y } + textSize
    return SpaceCompassCurveTextLayout(baseline, SpaceCompassSunSceneFrame(left, top, right-left, bottom-top), center)
}

/** Reserve legible text inside the scene; never bridge a clipped/seam fragment or force overlap. */
internal fun spaceCompassChooseCurveTextLayout(segments: List<SpaceCompassSunPathSegment>,
    candidates: List<SpaceCompassSunScenePoint>, textWidth: Double, textSize: Double, offset: Double,
    width: Double, height: Double, minimumY: Double = 0.0,
    occupied: List<SpaceCompassSunSceneFrame> = emptyList()): SpaceCompassCurveTextLayout? {
    if (!width.isFinite() || !height.isFinite() || !minimumY.isFinite() || width <= 0 || height <= minimumY) return null
    fun overlaps(a: SpaceCompassSunSceneFrame, b: SpaceCompassSunSceneFrame): Boolean =
        a.left < b.left+b.width && b.left < a.left+a.width && a.top < b.top+b.height && b.top < a.top+a.height
    for (candidate in candidates) {
        val layout = spaceCompassCurveTextLayout(segments, candidate, textWidth, textSize, offset) ?: continue
        val b = layout.bounds
        if (b.left < 0 || b.top < minimumY || b.left+b.width > width || b.top+b.height > height ||
            occupied.any { overlaps(b, it) }) continue
        return layout
    }
    return null
}
