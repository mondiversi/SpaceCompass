package me.mondiversi.spacecompass

internal data class SpaceCompassCurveTextLayout(val baseline: List<SpaceCompassSunScenePoint>,
    val bounds: SpaceCompassSunSceneFrame, val anchor: SpaceCompassSunScenePoint, val below: Boolean, val textSize: Double, val additionalOffset: Double) {
    // Short glyph runs avoid treating the empty corners of an inclined label as occupied text.
    val collisionFrames: List<SpaceCompassSunSceneFrame> by lazy(LazyThreadSafetyMode.NONE) {
        spaceCompassCurveTextClearance(baseline, textSize)
    }
}

/** Shared with orbit names: shaped-text advances belong to the actual offset curve. */
internal fun spaceCompassCurveTextLayout(segments: List<SpaceCompassSunPathSegment>,
    center: SpaceCompassSunScenePoint, textWidth: Double, textSize: Double, offset: Double,
    below: Boolean = false, additionalOffset: Double = 0.0): SpaceCompassCurveTextLayout? {
    if (!offset.isFinite() || offset < 0 || !additionalOffset.isFinite() || additionalOffset < 0) return null
    // Match the clearance ribbon's ascenders: the nearest ink stays one offset from its own curve.
    val baselineOffset = (if (below) offset + textSize * 1.35 else offset) + additionalOffset
    val baseline = spaceCompassOrbitTextBaseline(segments, center, textWidth, textSize, baselineOffset, below) ?: return null
    val left = baseline.minOf { it.x } - textSize
    val top = baseline.minOf { it.y } - textSize * 1.6
    val right = baseline.maxOf { it.x } + textSize
    val bottom = baseline.maxOf { it.y } + textSize
    return SpaceCompassCurveTextLayout(baseline, SpaceCompassSunSceneFrame(left, top, right-left, bottom-top), center, below, textSize, additionalOffset)
}

/** Reserve legible text inside the scene; never bridge a clipped/seam fragment or force overlap. */
internal fun spaceCompassChooseCurveTextLayout(segments: List<SpaceCompassSunPathSegment>,
    candidates: List<SpaceCompassSunScenePoint>, textWidth: Double, textSize: Double, offset: Double,
    width: Double, height: Double, minimumY: Double = 0.0,
    occupied: List<SpaceCompassSunSceneFrame> = emptyList(), below: Boolean = false, preciseClearance: Boolean = false, additionalOffset: Double = 0.0): SpaceCompassCurveTextLayout? {
    if (!width.isFinite() || !height.isFinite() || !minimumY.isFinite() || width <= 0 || height <= minimumY) return null
    fun overlaps(a: SpaceCompassSunSceneFrame, b: SpaceCompassSunSceneFrame): Boolean =
        a.left < b.left+b.width && b.left < a.left+a.width && a.top < b.top+b.height && b.top < a.top+a.height
    for (candidate in candidates) {
        val layout = spaceCompassCurveTextLayout(segments, candidate, textWidth, textSize, offset, below, additionalOffset) ?: continue
        val runs = if (preciseClearance) layout.collisionFrames else listOf(layout.bounds)
        if (runs.any { b -> b.left < 0 || b.top < minimumY || b.left+b.width > width || b.top+b.height > height ||
            occupied.any { overlaps(b, it) } }) continue
        return layout
    }
    return null
}
