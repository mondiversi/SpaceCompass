package me.mondiversi.spacecompass

internal data class SpaceCompassCurveCaptionPlacement(val layout: SpaceCompassCurveTextLayout, val textSize: Float)

/** Bounded, projection-specific history with hysteresis and occasional whole-curve recovery. */
internal class SpaceCompassCurveCaptionPlacementState(private val monotonicNanos: () -> Long = System::nanoTime) {
    private var viewport: Triple<Int, Int, Float>? = null
    private var projection: SpaceCompassPerspective? = null
    private val captions = linkedMapOf<String, SpaceCompassCurveCaptionPlacement>()
    private val nextRecovery = mutableMapOf<String, Long>()

    fun viewport(width: Int, height: Int, scale: Float, perspective: SpaceCompassPerspective? = null) {
        val next = Triple(width, height, scale)
        if (next != viewport || perspective != projection) {
            captions.clear(); nextRecovery.clear(); viewport = next; projection = perspective
        }
    }

    fun forget(key: String) { captions.remove(key); nextRecovery.remove(key) }

    fun retainKeys(visible: Set<String>) {
        captions.keys.filterNot { it in visible }.forEach(::forget)
    }

    fun place(key: String, segments: List<SpaceCompassSunPathSegment>,
        candidates: List<SpaceCompassSunScenePoint>, textSizes: List<Float>, offset: Double,
        width: Double, height: Double, minimumY: Double, occupied: List<SpaceCompassSunSceneFrame>,
        scale: Float, followPreviousAnchor: Boolean = true,
        measure: (Float) -> Double): SpaceCompassCurveCaptionPlacement? {
        if (!scale.isFinite() || scale <= 0 || !offset.isFinite() || offset < 0 ||
            segments.isEmpty() || candidates.isEmpty()) { forget(key); return null }
        val previous = captions[key]
        val available = textSizes.filter { it.isFinite() && it > 0 }
        val sizes = (listOfNotNull(previous?.textSize?.takeIf { it in available }) + available).distinct()
        val widths = mutableMapOf<Float, Double>()
        fun textWidth(size: Float) = widths.getOrPut(size) { measure(size) }
        fun record(layout: SpaceCompassCurveTextLayout, size: Float): SpaceCompassCurveCaptionPlacement =
            SpaceCompassCurveCaptionPlacement(layout, size).also {
                captions[key] = it
                if (!layout.below && layout.additionalOffset == 0.0) nextRecovery.remove(key)
                if (captions.size > 64) forget(captions.keys.first())
            }
        fun at(point: SpaceCompassSunScenePoint, size: Float, below: Boolean, gap: Double,
            reserved: List<SpaceCompassSunSceneFrame>): SpaceCompassCurveCaptionPlacement? =
            spaceCompassChooseCurveTextLayout(segments, listOf(point), textWidth(size), size.toDouble(),
                offset, width, height, minimumY, reserved, below, preciseClearance = true,
                additionalOffset = gap)?.let { record(it, size) }
        fun choose(points: List<SpaceCompassSunScenePoint>, reservations: List<List<SpaceCompassSunSceneFrame>>,
            improve: SpaceCompassCurveCaptionPlacement? = null): SpaceCompassCurveCaptionPlacement? {
            val oldDistance = improve?.let { it.layout.additionalOffset / it.textSize }
            // A near above run anywhere outranks a below fallback, even when it is near a viewport edge.
            for (distance in listOf(0.0, .5, 1.0, 2.0, 3.0)) for (below in listOf(false, true)) {
                if (oldDistance != null && !(distance < oldDistance - 1e-8 ||
                    kotlin.math.abs(distance - oldDistance) < 1e-8 && improve?.layout?.below == true && !below)) continue
                for (reserved in reservations) for (size in sizes) for (point in points)
                    at(point, size, below, size * distance, reserved)?.let { return it }
            }
            return null
        }
        val retained = previous?.let {
            if (followPreviousAnchor) spaceCompassNearestSkyReferenceAnchor(segments, it.layout.anchor)
            else candidates.firstOrNull()
        }
        val retainedLayout = if (previous != null && retained != null && previous.textSize in available)
            spaceCompassChooseCurveTextLayout(segments, listOf(retained), textWidth(previous.textSize),
                previous.textSize.toDouble(), offset, width, height, minimumY, occupied,
                previous.layout.below, preciseClearance = true, additionalOffset = previous.layout.additionalOffset)
        else null
        val recover = previous != null && retained != null && previous.textSize in available &&
            (previous.layout.below || previous.layout.additionalOffset > 0)
        if (retainedLayout != null && !recover) return record(retainedLayout, previous!!.textSize)

        // Acquisition needs a wider gap than retention, resisting small hand/sensor movements.
        val padding = 6.0 * scale
        val reserved = occupied.map {
            SpaceCompassSunSceneFrame(it.left - padding, it.top - padding, it.width + padding * 2, it.height + padding * 2)
        }
        fun edges(margin: Double) = listOf(SpaceCompassSunSceneFrame(0.0, minimumY, margin, height - minimumY),
            SpaceCompassSunSceneFrame(width - margin, minimumY, margin, height - minimumY),
            SpaceCompassSunSceneFrame(0.0, minimumY, width, margin),
            SpaceCompassSunSceneFrame(0.0, height - margin, width, margin))
        val points = (listOfNotNull(retained) + candidates).distinct()
        if (recover) {
            at(retained!!, previous!!.textSize, false, 0.0, reserved + edges(padding))?.let { return it }
            val now = monotonicNanos()
            val deadline = nextRecovery[key]
            if (retainedLayout == null || deadline == null || now - deadline >= 0) {
                // Bound global searches while allowing recovery after a pan, not just at the old anchor.
                nextRecovery[key] = now + 200_000_000L
                choose(points, listOf(reserved + edges(16.0 * scale), reserved + edges(padding)), previous)
                    ?.let { return it }
            }
        }
        if (retainedLayout != null) return record(retainedLayout, previous!!.textSize)
        return choose(points, listOf(reserved + edges(16.0 * scale), reserved))
            .also { if (it == null) forget(key) }
    }
}
