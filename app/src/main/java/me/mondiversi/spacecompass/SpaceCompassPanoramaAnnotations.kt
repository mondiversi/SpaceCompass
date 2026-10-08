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


/** Time pills stay near their own orbital point; leader lines make displaced labels unambiguous. */
internal fun drawSpaceCompassPanoramaTimeLabel(canvas: Canvas, value: String, point: SpaceCompassSunScenePoint,
    width: Int, height: Int, scale: Float, tint: Int, occupied: MutableList<RectF>): RectF {
    val text = spaceCompassOrbitTextPaint(23 * scale).apply { textAlign = Paint.Align.LEFT }
    val labelWidth = text.measureText(value) + 18 * scale
    val labelHeight = 34 * scale
    val half = labelWidth / 2
    val x = point.x.toFloat().coerceIn(half + 4 * scale, width - half - 4 * scale)
    fun candidate(row: Int, shift: Float): RectF {
        val cx = (x + shift * scale).coerceIn(half + 4 * scale, width - half - 4 * scale)
        val top = (point.y.toFloat() + (16 + row * 38) * scale).coerceIn(76 * scale, height - labelHeight - 48 * scale)
        return RectF(cx - half, top, cx + half, top + labelHeight)
    }
    val candidates = (0..4).flatMap { row -> listOf(0f, -48f, 48f, -96f, 96f).map { candidate(row, it) } }
    val bounds = candidates.firstOrNull { c -> occupied.none { RectF.intersects(it, c) } }
        ?: candidates.minBy { c -> occupied.count { RectF.intersects(it, c) } }
    occupied += bounds
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = tint; strokeWidth = 1.5f * scale }
    canvas.drawLine(point.x.toFloat(), point.y.toFloat() + 8 * scale, bounds.centerX(), bounds.top, paint)
    paint.color = 0xd9101418.toInt() // Near-black with approximately 15% background transparency.
    canvas.drawRoundRect(bounds, 8 * scale, 8 * scale, paint)
    paint.color = tint; paint.style = Paint.Style.STROKE; paint.strokeWidth = scale
    canvas.drawRoundRect(bounds, 8 * scale, 8 * scale, paint)
    text.color = Color.WHITE; text.style = Paint.Style.FILL
    canvas.drawText(value, bounds.left + 9 * scale, bounds.top + 25 * scale, text)
    return bounds
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
