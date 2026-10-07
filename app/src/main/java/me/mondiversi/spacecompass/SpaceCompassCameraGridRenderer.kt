package me.mondiversi.spacecompass

import android.graphics.*
import kotlin.math.hypot

/** A quiet angular frame over the full JPEG; no image crop, rotation or invented 360-degree axis. */
internal fun drawSpaceCompassCameraAngularGrid(canvas: Canvas, width: Int, height: Int,
    orientation: SpaceCompassSunOrientation, perspective: SpaceCompassPerspective,
    snapshot: SpaceCompassPanoramaSnapshot, scale: Float, occupied: MutableList<RectF>) {
    // Reserve the complete top-axis glyph (ascent, padding and shadow) below the caption.
    val left = 72f * scale; val top = (SPACE_COMPASS_PANORAMA_HEADER_HEIGHT + 42f) * scale
    val right = width - 24f * scale; val bottom = height - 42f * scale
    if (right - left < 80f * scale || bottom - top < 80f * scale) return
    val frame = SpaceCompassSunSceneFrame(left.toDouble(), top.toDouble(), (right-left).toDouble(), (bottom-top).toDouble())
    val grid = spaceCompassCameraAngularGrid(orientation, perspective, width.toDouble(), height.toDouble(), frame)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = scale }
    for (line in grid.lines) {
        val path = Path(); var end: SpaceCompassSunScenePoint? = null
        line.segments.forEach { segment ->
            if (end == null || hypot(end!!.x - segment.start.x, end!!.y - segment.start.y) > .2)
                path.moveTo(segment.start.x.toFloat(), segment.start.y.toFloat())
            path.lineTo(segment.end.x.toFloat(), segment.end.y.toFloat()); end = segment.end
        }
        paint.color = 0x50000000; paint.strokeWidth = 2.5f * scale; canvas.drawPath(path, paint)
        paint.color = 0x45ffffff; paint.strokeWidth = scale; canvas.drawPath(path, paint)
    }
    paint.color = 0x90ffffff.toInt(); paint.strokeWidth = scale
    canvas.drawRect(left, top, right, bottom, paint)
    val text = spaceCompassOrbitTextPaint(21 * scale).apply { color = Color.WHITE; textAlign = Paint.Align.CENTER }
    val axisBounds = mutableListOf<RectF>()
    fun value(degrees: Double, elevation: Boolean): String =
        (if (elevation && degrees > 0) "+" else "") + formatSpaceCompassNumber(degrees, 0,
            snapshot.formatting.numeric, grouping = false, systemLocale = snapshot.formatting.deviceLocale) + "°"
    fun label(value: String, x: Float, baseline: Float, size: Float = 21 * scale, fill: Boolean = false): RectF? {
        text.textSize = size
        val half = text.measureText(value) / 2
        val bounds = RectF(x-half-5*scale, baseline+text.fontMetrics.ascent-3*scale,
            x+half+5*scale, baseline+text.fontMetrics.descent+3*scale)
        if (bounds.left < 2*scale || bounds.right > width-2*scale || bounds.top < SPACE_COMPASS_PANORAMA_HEADER_HEIGHT*scale ||
            bounds.bottom > height-2*scale || axisBounds.any { RectF.intersects(it, bounds) }) return null
        if (fill) {
            val pill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xc9101418.toInt() }
            canvas.drawRoundRect(bounds, 5*scale, 5*scale, pill)
        }
        text.setShadowLayer(3*scale, 0f, scale, Color.BLACK)
        canvas.drawText(value, x, baseline, text); text.clearShadowLayer()
        axisBounds += bounds
        return bounds
    }
    for (edge in listOf(SpaceCompassCameraGridEdge.TOP, SpaceCompassCameraGridEdge.BOTTOM, SpaceCompassCameraGridEdge.LEFT)) {
        val ticks = grid.ticks.filter { it.edge == edge }.sortedBy { if (edge == SpaceCompassCameraGridEdge.LEFT) it.point.y else it.point.x }
        for (tick in ticks) {
            val x = tick.point.x.toFloat(); val y = tick.point.y.toFloat()
            when (edge) {
                SpaceCompassCameraGridEdge.TOP -> {
                    canvas.drawLine(x, top, x, top+6*scale, paint)
                    label(value(tick.degrees, false), x, top-8*scale)
                }
                SpaceCompassCameraGridEdge.BOTTOM -> {
                    canvas.drawLine(x, bottom-6*scale, x, bottom, paint)
                    label(value(tick.degrees, false), x, bottom+28*scale)
                }
                SpaceCompassCameraGridEdge.LEFT -> {
                    canvas.drawLine(left, y, left+6*scale, y, paint)
                    label(value(tick.degrees, true), left-34*scale, y-text.fontMetrics.ascent/2)
                }
                else -> Unit
            }
        }
    }
    // Cardinal marks are actual zero-elevation rays, never a fixed horizontal row.
    val symbols = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val count = snapshot.cardinalNames.size
    if (count == 4 || count == 8) for (index in 0 until count) {
        val p = projectSpaceCompassSun(SpaceCompassSunPosition(index*360.0/count, 0.0), orientation,
            width.toDouble(), height.toDouble(), perspective)
        if (!p.visible || p.x !in frame.left..(frame.left+frame.width) || p.y !in frame.top..(frame.top+frame.height)) continue
        val x = p.x.toFloat(); val y = p.y.toFloat()
        paint.style = Paint.Style.FILL; paint.color = 0xeeffffff.toInt()
        canvas.drawCircle(x, y, 3.5f*scale, paint)
        label(snapshot.cardinalNames[index], x, y-12*scale, 23*scale, fill = true)
        label(symbols[index*(8/count)], x, y+29*scale, 20*scale, fill = true)
    }
    occupied += axisBounds
    // Time/name pills stay inside the axes rather than covering tick labels.
    occupied += RectF(0f, 0f, width.toFloat(), top)
    occupied += RectF(0f, bottom, width.toFloat(), height.toFloat())
    occupied += RectF(0f, top, left, bottom)
}
