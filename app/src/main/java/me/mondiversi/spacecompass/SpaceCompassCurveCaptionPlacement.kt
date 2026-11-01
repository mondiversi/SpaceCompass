package me.mondiversi.spacecompass

internal data class SpaceCompassCurveCaptionPlacement(val layout: SpaceCompassCurveTextLayout, val textSize: Float)

/** Per-view, bounded placement history; a free retained side never gets pulled back above the curve. */
internal class SpaceCompassCurveCaptionPlacementState {
    private var viewport: Triple<Int, Int, Float>? = null
    private val captions = linkedMapOf<String, SpaceCompassCurveCaptionPlacement>()

    fun viewport(width: Int, height: Int, scale: Float) {
        val next = Triple(width, height, scale)
        if (next != viewport) { captions.clear(); viewport = next }
    }

    fun place(key: String, segments: List<SpaceCompassSunPathSegment>,
        candidates: List<SpaceCompassSunScenePoint>, textSizes: List<Float>, offset: Double,
        width: Double, height: Double, minimumY: Double, occupied: List<SpaceCompassSunSceneFrame>,
        scale: Float, followPreviousAnchor: Boolean = true, measure: (Float) -> Double): SpaceCompassCurveCaptionPlacement? {
        if (!scale.isFinite() || scale <= 0 || !offset.isFinite() || offset < 0) return null
        val previous = captions[key]
        val available = textSizes.filter { it.isFinite() && it > 0 }
        val sizes = (listOfNotNull(previous?.textSize?.takeIf { it in available }) + available).distinct()
        val sides = listOf(previous?.layout?.below ?: false, !(previous?.layout?.below ?: false))
        fun record(layout: SpaceCompassCurveTextLayout, size: Float): SpaceCompassCurveCaptionPlacement =
            SpaceCompassCurveCaptionPlacement(layout, size).also {
                captions[key] = it
                if (captions.size > 64) captions.remove(captions.keys.first())
            }
        fun choose(points: List<SpaceCompassSunScenePoint>, reserved: List<SpaceCompassSunSceneFrame>): SpaceCompassCurveCaptionPlacement? {
            for (size in sizes) for (point in points) for (below in sides) {
                // A neighbouring orbit's markers can occupy the first below-curve slot as well.
                val gaps = (listOfNotNull(previous?.layout?.takeIf { it.below == below }?.additionalOffset) +
                    if (below) listOf(0.0, size.toDouble(), size * 2.0, size * 3.0) else listOf(0.0)).distinct()
                for (gap in gaps) {
                    val layout = spaceCompassChooseCurveTextLayout(segments, listOf(point), measure(size), size.toDouble(),
                        offset, width, height, minimumY, reserved, below, preciseClearance = true,
                        additionalOffset = gap) ?: continue
                    return record(layout, size)
                }
            }
            return null
        }
        val retained = previous?.let {
            if (followPreviousAnchor) spaceCompassNearestSkyReferenceAnchor(segments, it.layout.anchor)
            else candidates.firstOrNull()
        }
        // Retain the precise side/size while clear; acquisition needs a wider gap to resist sensor jitter.
        if (previous != null && retained != null && previous.textSize in available) {
            spaceCompassChooseCurveTextLayout(segments, listOf(retained), measure(previous.textSize),
                previous.textSize.toDouble(), offset, width, height, minimumY, occupied, previous.layout.below, preciseClearance = true, additionalOffset = previous.layout.additionalOffset)
                ?.let { return record(it, previous.textSize) }
        }
        val padding = 6.0 * scale
        val reserved = occupied.map {
            SpaceCompassSunSceneFrame(it.left - padding, it.top - padding, it.width + padding * 2, it.height + padding * 2)
        }
        val margin = 16.0 * scale
        val edges = listOf(SpaceCompassSunSceneFrame(0.0, minimumY, margin, height - minimumY),
            SpaceCompassSunSceneFrame(width - margin, minimumY, margin, height - minimumY),
            SpaceCompassSunSceneFrame(0.0, minimumY, width, margin),
            SpaceCompassSunSceneFrame(0.0, height - margin, width, margin))
        val points = (listOfNotNull(retained) + candidates).distinct()
        return choose(points, reserved + edges) ?: choose(points, reserved)
    }
}
