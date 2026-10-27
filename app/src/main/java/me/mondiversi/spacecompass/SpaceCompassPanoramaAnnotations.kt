package me.mondiversi.spacecompass

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

internal data class SpaceCompassPanoramaTimeLabel(val text: String, val point: SpaceCompassSunScenePoint,
    val tint: Int, val principal: Boolean, val pointRadius: Float = if (principal) 17f else 6f,
    val current: Boolean = false, val selected: Boolean = false)

/** Lay out the whole selected set together. Draw leaders first so later lines cannot cross earlier text. */
internal fun drawSpaceCompassPanoramaTimeLabels(canvas: Canvas, labels: List<SpaceCompassPanoramaTimeLabel>,
    width: Int, height: Int, scale: Float, occupied: MutableList<RectF>) {
    val text = spaceCompassOrbitTextPaint(23 * scale).apply { textAlign = Paint.Align.LEFT }
    val metrics = text.fontMetrics
    val lineHeight = maxOf(26 * scale, metrics.descent - metrics.ascent + 3 * scale)
    val obstacles = occupied.mapTo(mutableListOf()) {
        SpaceCompassSunSceneFrame(it.left.toDouble(), it.top.toDouble(), it.width().toDouble(), it.height().toDouble())
    }
    val placed = labels.sortedBy { if (it.selected) 0 else if (it.current) 1 else if (it.principal) 2 else 3 }.mapNotNull { label ->
        val lines = label.text.lines()
        val labelWidth = lines.maxOf(text::measureText) + 18 * scale
        val heightForLabel = maxOf(34 * scale, lineHeight * lines.size + 10 * scale)
        val nearby = spaceCompassPanoramaLabelBounds(label.point, labelWidth.toDouble(), heightForLabel.toDouble(),
            width.toDouble(), height.toDouble(), scale.toDouble(), obstacles, label.pointRadius.toDouble())
        val layout = nearby ?: if (label.selected) placeSpaceCompassOrbitBadge(width.toDouble(), height.toDouble(),
            labelWidth.toDouble(), heightForLabel.toDouble(), label.point, 4.0 * scale, 8.0 * scale,
            excluded = listOf(SpaceCompassSunSceneFrame(0.0, 0.0, width.toDouble(), 76.0 * scale)),
            occupied = obstacles, pinned = true)?.let { SpaceCompassSunSceneFrame(it.x, it.y,
                labelWidth.toDouble(), heightForLabel.toDouble()) } else null
        layout ?: return@mapNotNull null
        obstacles += layout
        val bounds = RectF(layout.left.toFloat(), layout.top.toFloat(),
            (layout.left + layout.width).toFloat(), (layout.top + layout.height).toFloat())
        occupied += bounds
        Triple(label, bounds, lines)
    }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    placed.forEach { (label, bounds, _) ->
        paint.color = label.tint; paint.strokeWidth = 1.5f * scale
        // Connect to the nearest edge, whether the pill moved above, below or to a side.
        val x = label.point.x.toFloat(); val y = label.point.y.toFloat()
        val targetX = x.coerceIn(bounds.left, bounds.right)
        val targetY = y.coerceIn(bounds.top, bounds.bottom)
        val dx = targetX - x; val dy = targetY - y
        val length = kotlin.math.hypot(dx, dy).coerceAtLeast(1f)
        val radius = (label.pointRadius + 3) * scale
        canvas.drawLine(x + dx / length * radius, y + dy / length * radius, targetX, targetY, paint)
    }
    text.color = Color.WHITE; text.style = Paint.Style.FILL
    placed.forEach { (label, bounds, lines) ->
        paint.style = Paint.Style.FILL
        paint.color = 0xd9101418.toInt() // Near-black with approximately 15% background transparency.
        canvas.drawRoundRect(bounds, 8 * scale, 8 * scale, paint)
        paint.color = label.tint; paint.style = Paint.Style.STROKE; paint.strokeWidth = (if (label.selected) 3f else 2f) * scale
        canvas.drawRoundRect(bounds, 8 * scale, 8 * scale, paint)
        val firstBaseline = bounds.top +
            (bounds.height() - lineHeight * (lines.size - 1) - metrics.descent - metrics.ascent) / 2
        lines.forEachIndexed { index, line ->
            drawSpaceCompassCaptureLabelLine(canvas, line, bounds.left + 9 * scale, firstBaseline + index * lineHeight, text)
        }
    }
}

/** Current markers have a pill even without a daily path, frozen to the capture instant. */
internal fun spaceCompassPanoramaCurrentLabel(snapshot: SpaceCompassPanoramaSnapshot,
    position: SpaceCompassSunPosition, point: SpaceCompassSunScenePoint, tint: Int, radius: Float): SpaceCompassPanoramaTimeLabel {
    val time = snapshot.currentTime ?: formatSpaceCompassPanoramaExportTime(snapshot.timeMs,
        java.util.TimeZone.getTimeZone("UTC"), snapshot.formatting)
    val value = formatSpaceCompassPanoramaPointLabel(time, position, snapshot.formatting,
        snapshot.currentPointName, formatSpaceCompassPanoramaPointCountdown(snapshot.timeMs, snapshot.timeMs, snapshot.formatting))
    return SpaceCompassPanoramaTimeLabel(value, point, tint, principal = true, pointRadius = radius, current = true)
}

/** The live orbit's event vocabulary: rise/set semicircles, culmination plus and minimum bar. */
internal fun drawSpaceCompassPanoramaEventMarker(canvas: Canvas, event: SpaceCompassSunPathEvent,
    point: SpaceCompassSunScenePoint, scale: Float, tint: Int) {
    val x = point.x.toFloat(); val y = point.y.toFloat()
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = tint }
    if (event == SpaceCompassSunPathEvent.HOUR) {
        canvas.drawCircle(x, y, 6 * scale, paint)
        return
    }
    if (event == SpaceCompassSunPathEvent.APPROACH) {
        paint.color = 0xcc07101b.toInt()
        canvas.drawCircle(x, y, 18 * scale, paint)
        drawSpaceCompassApproachGlyph(canvas, x, y, 12 * scale, tint, 2.6f * scale)
        return
    }
    paint.color = 0x73000000
    canvas.drawCircle(x, y, 15.5f * scale, paint)
    paint.color = tint
    canvas.drawCircle(x, y, 14 * scale, paint)
    paint.color = 0xdd000000.toInt()
    val d = 6 * scale
    if (event == SpaceCompassSunPathEvent.MINIMUM || event == SpaceCompassSunPathEvent.CULMINATION) {
        paint.strokeWidth = 2.4f * scale; paint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(x - d, y, x + d, y, paint)
        if (event == SpaceCompassSunPathEvent.CULMINATION)
            canvas.drawLine(x, y - d, x, y + d, paint)
    } else {
        val glyph = android.graphics.Path().apply {
            spaceCompassHorizonEventGlyph(event, x, y, d).forEachIndexed { index, (px, py) ->
                if (index == 0) moveTo(px, py) else lineTo(px, py)
            }
            close()
        }
        canvas.drawPath(glyph, paint)
    }
}

/** Approaches are visible even with labels off, and must survive later thumbnails/hourly dots. */
internal fun drawSpaceCompassCaptureApproachMarkers(canvas: Canvas,
    markers: List<Triple<SpaceCompassSunPathPoint, SpaceCompassSunScenePoint, Int>>, scale: Float,
    wrapWidth: Int? = null) {
    val occupied = mutableListOf<Pair<Long, SpaceCompassSunScenePoint>>()
    markers.filter { SpaceCompassSunPathEvent.APPROACH in it.first.events }.forEach { (marker, point, tint) ->
        if (spaceCompassApproachMarkerIsDuplicate(marker.timeMs, point, occupied, 36.0 * scale, wrapWidth?.toDouble())) return@forEach
        occupied += marker.timeMs to point
        drawSpaceCompassPanoramaEventMarker(canvas, SpaceCompassSunPathEvent.APPROACH, point, scale, tint)
        wrapWidth?.let { width ->
            if (point.x < 20 * scale) drawSpaceCompassPanoramaEventMarker(canvas, SpaceCompassSunPathEvent.APPROACH,
                point.copy(x = point.x + width), scale, tint)
            if (point.x > width - 20 * scale) drawSpaceCompassPanoramaEventMarker(canvas, SpaceCompassSunPathEvent.APPROACH,
                point.copy(x = point.x - width), scale, tint)
        }
    }
}
