package me.mondiversi.spacecompass

import kotlin.math.hypot

internal data class SpaceCompassSkyReferenceCaptionPlacement(val layout: SpaceCompassCurveTextLayout, val textSize: Float)

/** Project continuously onto the visible curve instead of jumping between sampled midpoints. */
internal fun spaceCompassNearestSkyReferenceAnchor(segments: List<SpaceCompassSunPathSegment>,
    target: SpaceCompassSunScenePoint): SpaceCompassSunScenePoint? {
    var best: SpaceCompassSunScenePoint? = null
    var distance = Double.POSITIVE_INFINITY
    for (segment in segments) {
        val dx = segment.end.x - segment.start.x; val dy = segment.end.y - segment.start.y
        val squared = dx * dx + dy * dy
        val t = if (squared == 0.0) 0.0 else ((target.x - segment.start.x) * dx +
            (target.y - segment.start.y) * dy).div(squared).coerceIn(0.0, 1.0)
        val x = segment.start.x + t * dx; val y = segment.start.y + t * dy
        val candidate = (x - target.x) * (x - target.x) + (y - target.y) * (y - target.y)
        if (candidate < distance) { distance = candidate; best = SpaceCompassSunScenePoint(x, y) }
    }
    return best
}

/** One above-cross placement clamped to the viewport; no obstacle-dependent above/below flip. */
internal fun spaceCompassSkyGuideCaptionBounds(anchor: SpaceCompassSunScenePoint, textWidth: Double,
    textHeight: Double, width: Double, height: Double, minimumY: Double, inset: Double,
    gap: Double): SpaceCompassSunSceneFrame? {
    if (width < textWidth + inset * 2 || height - minimumY < textHeight + inset * 2) return null
    val center = anchor.x.coerceIn(textWidth / 2 + inset, width - textWidth / 2 - inset)
    val top = (anchor.y - textHeight - gap).coerceIn(minimumY + inset, height - textHeight - inset)
    return SpaceCompassSunSceneFrame(center - textWidth / 2, top, textWidth, textHeight)
}

/** Live-only bounded history. Captured images keep their independent one-shot layout. */
internal class SpaceCompassSkyReferencePlacementState {
    private var viewport: Triple<Int, Int, Float>? = null
    private val captions = mutableMapOf<String, SpaceCompassSkyReferenceCaptionPlacement>()
    private val repeats = mutableMapOf<SpaceCompassSkyReference, List<SpaceCompassSunScenePoint>>()

    fun viewport(width: Int, height: Int, scale: Float) {
        val next = Triple(width, height, scale)
        if (next != viewport) { captions.clear(); repeats.clear(); viewport = next }
    }

    fun caption(key: String, segments: List<SpaceCompassSunPathSegment>,
        candidates: List<SpaceCompassSunScenePoint>, scale: Float, width: Double, height: Double,
        minimumY: Double, fixed: List<SpaceCompassSunSceneFrame>, occupied: List<SpaceCompassSunSceneFrame>,
        measure: (Float) -> Double): SpaceCompassSkyReferenceCaptionPlacement? {
        val previous = captions[key]
        val sizes = (listOfNotNull(previous?.textSize) + listOf(24f, 22f, 20f).map { it * scale }).distinct()
        fun choose(points: List<SpaceCompassSunScenePoint>, reserved: List<SpaceCompassSunSceneFrame>): SpaceCompassSkyReferenceCaptionPlacement? {
            for (size in sizes) {
                val layout = spaceCompassChooseCurveTextLayout(segments, points, measure(size),
                    size.toDouble(), 12.0 * scale, width, height, minimumY, reserved) ?: continue
                return SpaceCompassSkyReferenceCaptionPlacement(layout, size).also { captions[key] = it }
            }
            return null
        }
        // Other moving guide captions/crosses do not evict an established name.
        previous?.let { last -> spaceCompassNearestSkyReferenceAnchor(segments, last.layout.anchor)?.let {
            choose(listOf(it), fixed)?.let { chosen -> return chosen }
        } }
        // New anchors need extra edge clearance; retained ones may approach the edge before moving.
        val margin = 16.0 * scale
        val edges = listOf(SpaceCompassSunSceneFrame(0.0, minimumY, margin, height - minimumY),
            SpaceCompassSunSceneFrame(width - margin, minimumY, margin, height - minimumY),
            SpaceCompassSunSceneFrame(0.0, minimumY, width, margin),
            SpaceCompassSunSceneFrame(0.0, height - margin, width, margin))
        return choose(candidates, occupied + edges) ?: choose(candidates, occupied)
    }

    fun degreeAnchors(reference: SpaceCompassSkyReference, segments: List<SpaceCompassSunPathSegment>,
        candidates: List<SpaceCompassSunScenePoint>, spacing: Double, reserved: List<SpaceCompassSunScenePoint>,
        width: Double, height: Double, minimumY: Double, inset: Double): List<SpaceCompassSunScenePoint> {
        val accepted = mutableListOf<SpaceCompassSunScenePoint>()
        fun add(point: SpaceCompassSunScenePoint, separation: Double) {
            if (accepted.size < 3 && point.x in inset..(width - inset) &&
                point.y in (minimumY + inset)..(height - inset) &&
                (reserved + accepted).none { hypot(it.x - point.x, it.y - point.y) < separation }) accepted += point
        }
        repeats[reference].orEmpty().forEach { last ->
            spaceCompassNearestSkyReferenceAnchor(segments, last)?.let { add(it, spacing * .85) }
        }
        candidates.forEach { add(it, spacing) }
        return accepted.toList().also { repeats[reference] = it }
    }
}
