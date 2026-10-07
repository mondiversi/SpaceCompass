package me.mondiversi.spacecompass

import android.graphics.*
import kotlin.math.hypot

internal data class SpaceCompassSkyReferenceLabel(val value: String, val bounds: RectF,
    val anchor: SpaceCompassSunScenePoint, val tint: Int)
internal data class SpaceCompassSkyReferenceCurveLabel(val value: String, val layout: SpaceCompassCurveTextLayout,
    val tint: Int, val textSize: Float)
internal data class SpaceCompassSkyReferenceDrawing(val labels: List<SpaceCompassSkyReferenceLabel>,
    val poles: List<SpaceCompassSunScenePoint>, val curveLabels: List<SpaceCompassSkyReferenceCurveLabel> = emptyList())

/** Draw quiet dashed guides behind the objects; reserve their labels for the final text pass. */
internal fun drawSpaceCompassSkyReferences(canvas: Canvas, latitude: Double, timeMs: Long,
    width: Int, height: Int, scale: Float, occupied: MutableList<RectF>,
    orientation: SpaceCompassSunOrientation? = null, perspective: SpaceCompassPerspective? = null,
    minimumLabelY: Float = 0f,
    circles: List<SpaceCompassSkyReferenceCircle>? = null,
    referenceNames: Map<SpaceCompassSkyReference, String> = emptyMap(),
    poleNames: Pair<String, String> = "North celestial pole" to "South celestial pole",
    allowLabelOverlap: Boolean = true, showLabels: Boolean = true,
    numericFormat: SpaceCompassNumericFormat = SpaceCompassNumericFormat.INTERNATIONAL,
    systemLocale: java.util.Locale = java.util.Locale.getDefault(),
    centerAzimuthDegrees: Double = 180.0,
    observerPointNames: Pair<String, String> = "Earth centre" to "Zenith",
    observerAltitude: Double = 0.0): SpaceCompassSkyReferenceDrawing {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND
        pathEffect = DashPathEffect(floatArrayOf(11 * scale, 8 * scale), 0f)
    }
    val labels = mutableListOf<SpaceCompassSkyReferenceLabel>()
    val text = spaceCompassOrbitTextPaint(24 * scale)
    val inset = 8 * scale
    fun label(value: String, candidates: List<SpaceCompassSunScenePoint>, tint: Int,
        allowOverlap: Boolean = allowLabelOverlap): SpaceCompassSkyReferenceLabel? {
        if (!showLabels) return null // Hidden annotations reserve no space; pole crosses remain geometric guides.
        val available = (width - 4 * inset).coerceAtLeast(1f)
        text.textSize = 24 * scale
        if (text.measureText(value) > available) text.textSize *= available / text.measureText(value)
        val labelWidth = text.measureText(value) + 16 * scale
        val labelHeight = 34 * scale
        val low = minimumLabelY + inset
        val high = height - labelHeight - inset
        if (high < low) return null
        val options = candidates.flatMap { point -> listOf(-1, 1).map { side ->
            val x = point.x.toFloat().coerceIn(labelWidth / 2 + inset, width - labelWidth / 2 - inset)
            val y = (point.y.toFloat() + if (side < 0) -labelHeight - 12 * scale else 12 * scale).coerceIn(low, high)
            SpaceCompassSkyReferenceLabel(value, RectF(x - labelWidth / 2, y, x + labelWidth / 2, y + labelHeight), point, tint)
        } }
        val chosen = options.firstOrNull { c -> occupied.none { RectF.intersects(it, c.bounds) } }
            ?: if (allowOverlap) options.minByOrNull { c -> occupied.count { RectF.intersects(it, c.bounds) } } else null
        if (chosen == null) return null
        occupied += chosen.bounds
        labels += chosen
        return chosen
    }
    val curveLabels = mutableListOf<SpaceCompassSkyReferenceCurveLabel>()
    val curveRuns = mutableListOf<Triple<SpaceCompassSkyReferenceCircle, List<SpaceCompassSunPathSegment>, List<SpaceCompassSunScenePoint>>>()
    fun curveLabel(value: String, candidates: List<SpaceCompassSunScenePoint>,
        segments: List<SpaceCompassSunPathSegment>, tint: Int): SpaceCompassSkyReferenceCurveLabel? {
        if (!showLabels || candidates.isEmpty()) return null
        val reserved = occupied.map { SpaceCompassSunSceneFrame(it.left.toDouble(), it.top.toDouble(),
            it.width().toDouble(), it.height().toDouble()) }
        // A modest reduction accommodates longer translations without crowding a tight bend.
        for (size in listOf(24f, 22f, 20f)) {
            text.textSize = size * scale
            val layout = spaceCompassChooseCurveTextLayout(segments, candidates, text.measureText(value).toDouble(),
                text.textSize.toDouble(), (12f * scale).toDouble(), width.toDouble(), height.toDouble(),
                minimumLabelY.toDouble(), reserved) ?: continue
            val b = layout.bounds
            occupied += RectF(b.left.toFloat(), b.top.toFloat(), (b.left+b.width).toFloat(), (b.top+b.height).toFloat())
            return SpaceCompassSkyReferenceCurveLabel(value, layout, tint, text.textSize).also { curveLabels += it }
        }
        return null
    }
    (circles ?: spaceCompassSkyReferenceCircles(latitude, timeMs)).forEach { circle ->
        val segments = if (orientation != null)
            projectSpaceCompassSkyReference(circle, orientation, width.toDouble(), height.toDouble(), perspective)
        else spaceCompassSkyReferencePanoramaSegments(circle, width.toDouble(), height.toDouble(), centerAzimuthDegrees)
        val path = Path(); var end: SpaceCompassSunScenePoint? = null
        segments.forEach { segment ->
            if (end == null || hypot(end!!.x - segment.start.x, end!!.y - segment.start.y) > .1)
                path.moveTo(segment.start.x.toFloat(), segment.start.y.toFloat())
            path.lineTo(segment.end.x.toFloat(), segment.end.y.toFloat()); end = segment.end
        }
        paint.color = 0x80000000.toInt(); paint.strokeWidth = 4 * scale; canvas.drawPath(path, paint)
        paint.color = circle.reference.tint; paint.alpha = 185; paint.strokeWidth = 1.8f * scale; canvas.drawPath(path, paint)
        if (showLabels) {
            val visible = segments.map { SpaceCompassSunScenePoint((it.start.x + it.end.x) / 2, (it.start.y + it.end.y) / 2) }
                .filter { it.y > minimumLabelY + 12 * scale && it.y < height - 12 * scale }
            curveRuns += Triple(circle, segments, visible)
        }
    }
    val poles = mutableListOf<SpaceCompassSunScenePoint>()
    spaceCompassSkyGuidePoints(latitude, observerAltitude).forEach { guide ->
        val name = when (guide.kind) {
            SpaceCompassSkyGuidePointKind.NORTH_POLE -> poleNames.first
            SpaceCompassSkyGuidePointKind.SOUTH_POLE -> poleNames.second
            SpaceCompassSkyGuidePointKind.EARTH_CENTER -> observerPointNames.first
            SpaceCompassSkyGuidePointKind.ZENITH -> observerPointNames.second
        }
        val point = spaceCompassSkyGuidePointProjection(guide, orientation, width.toDouble(), height.toDouble(),
            perspective, centerAzimuthDegrees)
        point?.let {
            val copies = if (orientation == null && (it.x < 12 * scale || it.x > width - 12 * scale))
                listOf(it, it.copy(x = it.x + if (it.x < width / 2) width.toDouble() else -width.toDouble())) else listOf(it)
            poles += copies
            copies.forEach { p -> occupied += RectF(p.x.toFloat() - 12 * scale, p.y.toFloat() - 12 * scale,
                p.x.toFloat() + 12 * scale, p.y.toFloat() + 12 * scale) }
            val degrees = guide.labelDegrees?.let { value ->
                "  ${formatSpaceCompassSkyReferenceDegrees(value, numericFormat, systemLocale)}" }.orEmpty()
            label(name + degrees, listOf(it), Color.WHITE)
        }
    }
    // Point captions retain their horizontal boxes. Curved names/degree repeats use free, readable runs.
    curveRuns.forEach { (circle, segments, visible) ->
        val candidates = visible
            .sortedBy { kotlin.math.abs(it.x - width * .38) + kotlin.math.abs(it.y - height * .4) * .1 }
            .filterIndexed { index, _ -> index % (if (allowLabelOverlap) 12 else 3) == 0 }.take(32)
        val value = formatSpaceCompassSkyReferenceDegrees(circle.declinationDegrees, numericFormat, systemLocale)
        val name = referenceNames[circle.reference] ?: circle.reference.label
        val caption = curveLabel("$name  $value", candidates, segments, circle.reference.tint)
        val repeatCandidates = visible.filter { it.x > 48 * scale && it.x < width - 48 * scale &&
            it.y > minimumLabelY + 48 * scale && it.y < height - 48 * scale }
        val anchors = spaceCompassSkyReferenceDegreeAnchors(repeatCandidates, 640.0 * scale,
            reserved = listOfNotNull(caption?.layout?.anchor), wrapWidth = if (orientation == null) width.toDouble() else null,
            maximumLabels = if (orientation == null) 8 else 3)
        anchors.forEach { curveLabel(value, listOf(it), segments, circle.reference.tint) }
    }
    return SpaceCompassSkyReferenceDrawing(labels, poles, curveLabels)
}

internal fun drawSpaceCompassSkyReferenceLabels(canvas: Canvas, drawing: SpaceCompassSkyReferenceDrawing, scale: Float) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    drawing.poles.forEach { point ->
        val x = point.x.toFloat(); val y = point.y.toFloat(); val r = 10 * scale
        paint.color = 0xbb000000.toInt(); paint.strokeWidth = 5 * scale
        canvas.drawLine(x - r, y, x + r, y, paint); canvas.drawLine(x, y - r, x, y + r, paint)
        paint.color = Color.WHITE; paint.strokeWidth = 2 * scale
        canvas.drawLine(x - r, y, x + r, y, paint); canvas.drawLine(x, y - r, x, y + r, paint)
    }
    drawing.curveLabels.forEach { label ->
        drawSpaceCompassCurveText(canvas, label.value, label.layout.baseline,
            spaceCompassOrbitTextPaint(label.textSize), label.tint)
    }
    drawing.labels.forEach { label ->
        val b = label.bounds
        paint.color = label.tint; paint.strokeWidth = scale
        canvas.drawLine(label.anchor.x.toFloat(), label.anchor.y.toFloat(), b.centerX(),
            if (label.anchor.y < b.centerY()) b.top else b.bottom, paint)
        paint.style = Paint.Style.FILL; paint.color = 0xc9101418.toInt()
        canvas.drawRoundRect(b, 7 * scale, 7 * scale, paint)
        val text = android.text.TextPaint(spaceCompassOrbitTextPaint(24 * scale)).apply {
            color = label.tint; textAlign = Paint.Align.LEFT // StaticLayout owns centering and bidirectional shaping.
        }
        val available = (b.width() - 16 * scale).coerceAtLeast(1f)
        val desired = android.text.Layout.getDesiredWidth(label.value, text)
        if (desired > available) text.textSize *= available / desired
        val block = android.text.StaticLayout.Builder.obtain(label.value, 0, label.value.length, text,
            kotlin.math.ceil(available.toDouble()).toInt())
            .setAlignment(android.text.Layout.Alignment.ALIGN_CENTER)
            .setTextDirection(android.text.TextDirectionHeuristics.FIRSTSTRONG_LTR)
            .setIncludePad(false).setMaxLines(1).build()
        canvas.save(); canvas.translate(b.centerX() - available / 2, b.centerY() - block.height / 2f)
        block.draw(canvas); canvas.restore()
    }
}
