package me.mondiversi.spacecompass

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextDirectionHeuristics
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint

/** Enough room for a centered full-size line and the reserved +90 degree marker. */
internal const val SPACE_COMPASS_PANORAMA_HEADER_HEIGHT = 144f

/** Both capture modes share a translucent black band and one centered, shaped caption line. */
internal fun drawSpaceCompassPanoramaCaption(canvas: Canvas, value: String, width: Int, scale: Float): RectF {
    val background = Paint().apply { color = 0x99000000.toInt() }
    canvas.drawRect(0f, 0f, width.toFloat(), SPACE_COMPASS_PANORAMA_HEADER_HEIGHT * scale, background)
    val inset = 32 * scale
    val available = (width - inset * 2).toInt().coerceAtLeast(1)
    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 34 * scale
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setShadowLayer(3 * scale, 0f, scale, Color.BLACK)
    }
    val desired = Layout.getDesiredWidth(value, paint)
    if (desired > available) paint.textSize *= available / desired
    val block = StaticLayout.Builder.obtain(value, 0, value.length, paint, available)
        .setAlignment(Layout.Alignment.ALIGN_CENTER).setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR)
        .setIncludePad(false).setMaxLines(1).build()
    val top = (SPACE_COMPASS_PANORAMA_HEADER_HEIGHT * scale - block.height) / 2f
    canvas.save(); canvas.translate(inset, top); block.draw(canvas); canvas.restore()
    val ink = Layout.getDesiredWidth(value, paint).coerceAtMost(available.toFloat())
    return RectF((width - ink) / 2, top, (width + ink) / 2, top + block.height)
}


internal data class SpaceCompassPanoramaTimeLabel(val text: String, val point: SpaceCompassSunScenePoint,
    val tint: Int, val principal: Boolean, val pointRadius: Float = if (principal) 17f else 6f,
    val current: Boolean = false)

/** Lay out the whole selected set together. Draw leaders first so later lines cannot cross earlier text. */
internal fun drawSpaceCompassPanoramaTimeLabels(canvas: Canvas, labels: List<SpaceCompassPanoramaTimeLabel>,
    width: Int, height: Int, scale: Float, occupied: MutableList<RectF>) {
    val text = spaceCompassOrbitTextPaint(23 * scale).apply { textAlign = Paint.Align.LEFT }
    val metrics = text.fontMetrics
    val labelHeight = maxOf(34 * scale, metrics.descent - metrics.ascent + 10 * scale)
    val obstacles = occupied.mapTo(mutableListOf()) {
        SpaceCompassSunSceneFrame(it.left.toDouble(), it.top.toDouble(), it.width().toDouble(), it.height().toDouble())
    }
    val placed = labels.sortedBy { if (it.current) 0 else if (it.principal) 1 else 2 }.mapNotNull { label ->
        val labelWidth = text.measureText(label.text) + 18 * scale
        val layout = spaceCompassPanoramaLabelBounds(label.point, labelWidth.toDouble(), labelHeight.toDouble(),
            width.toDouble(), height.toDouble(), scale.toDouble(), obstacles, label.pointRadius.toDouble()) ?: return@mapNotNull null
        obstacles += layout
        val bounds = RectF(layout.left.toFloat(), layout.top.toFloat(),
            (layout.left + layout.width).toFloat(), (layout.top + layout.height).toFloat())
        occupied += bounds
        label to bounds
    }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    placed.forEach { (label, bounds) ->
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
    placed.forEach { (label, bounds) ->
        paint.style = Paint.Style.FILL
        paint.color = 0xd9101418.toInt() // Near-black with approximately 15% background transparency.
        canvas.drawRoundRect(bounds, 8 * scale, 8 * scale, paint)
        paint.color = label.tint; paint.style = Paint.Style.STROKE; paint.strokeWidth = 2f * scale
        canvas.drawRoundRect(bounds, 8 * scale, 8 * scale, paint)
        canvas.drawText(label.text, bounds.left + 9 * scale,
            bounds.top + (labelHeight - metrics.descent - metrics.ascent) / 2, text)
    }
}

/** Current markers have a pill even without a daily path, frozen to the capture instant. */
internal fun spaceCompassPanoramaCurrentLabel(snapshot: SpaceCompassPanoramaSnapshot,
    position: SpaceCompassSunPosition, point: SpaceCompassSunScenePoint, tint: Int, radius: Float): SpaceCompassPanoramaTimeLabel {
    val time = snapshot.currentTime ?: formatSpaceCompassPanoramaExportTime(snapshot.timeMs,
        java.util.TimeZone.getTimeZone("UTC"), snapshot.formatting)
    val value = formatSpaceCompassPanoramaPointLabel(time, position.elevationDegrees, snapshot.formatting,
        snapshot.currentPointName)
    return SpaceCompassPanoramaTimeLabel(value, point, tint, principal = true, pointRadius = radius, current = true)
}

/** The live orbit's event vocabulary: up/down triangles, culmination diamond and minimum bar. */
internal fun drawSpaceCompassPanoramaEventMarker(canvas: Canvas, event: SpaceCompassSunPathEvent,
    point: SpaceCompassSunScenePoint, scale: Float, tint: Int) {
    val x = point.x.toFloat(); val y = point.y.toFloat()
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = tint }
    if (event == SpaceCompassSunPathEvent.HOUR) {
        canvas.drawCircle(x, y, 6 * scale, paint)
        return
    }
    paint.color = 0x73000000
    canvas.drawCircle(x, y, 15.5f * scale, paint)
    paint.color = tint
    canvas.drawCircle(x, y, 14 * scale, paint)
    paint.color = 0xdd000000.toInt()
    val d = 6 * scale
    if (event == SpaceCompassSunPathEvent.MINIMUM) {
        paint.strokeWidth = 2.4f * scale; paint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(x - d, y, x + d, y, paint)
    } else {
        val glyph = android.graphics.Path().apply {
            when (event) {
                SpaceCompassSunPathEvent.SUNRISE -> { moveTo(x, y - d)
                    lineTo(x + d, y + d); lineTo(x - d, y + d) }
                SpaceCompassSunPathEvent.SUNSET -> { moveTo(x, y + d)
                    lineTo(x + d, y - d); lineTo(x - d, y - d) }
                else -> { moveTo(x, y - d); lineTo(x + d, y)
                    lineTo(x, y + d); lineTo(x - d, y) }
            }
            close()
        }
        canvas.drawPath(glyph, paint)
    }
}
