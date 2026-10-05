package me.mondiversi.spacecompass

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.hypot

internal fun spaceCompassOrbitTextPaint(size: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    textSize = size
    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    textAlign = Paint.Align.CENTER
}

/** Android shapes the whole string on the path, including complex scripts; no per-character rotations. */
internal fun drawSpaceCompassOrbitName(canvas: Canvas, name: String, segments: List<SpaceCompassSunPathSegment>,
    arrow: SpaceCompassSunPathArrow, paint: Paint, tint: Int, offset: Float,
    occupied: MutableList<RectF> = mutableListOf(),
    viewportWidth: Float = canvas.width.toFloat(), viewportHeight: Float = canvas.height.toFloat()): Boolean {
    val span = paint.measureText(name).toDouble() + paint.textSize * .8
    val baseline = spaceCompassOrbitLabelBaseline(segments, arrow.center, span) ?: return false
    val bounds = RectF(baseline.minOf { it.x }.toFloat() - paint.textSize,
        baseline.minOf { it.y }.toFloat() - offset - paint.textSize * 1.6f,
        baseline.maxOf { it.x }.toFloat() + paint.textSize,
        baseline.maxOf { it.y }.toFloat() + paint.textSize)
    if (bounds.left < 0 || bounds.top < 0 || bounds.right > viewportWidth || bounds.bottom > viewportHeight ||
        occupied.any { RectF.intersects(it, bounds) }) return false
    val path = Path().apply {
        moveTo(baseline.first().x.toFloat(), baseline.first().y.toFloat())
        baseline.drop(1).forEach { lineTo(it.x.toFloat(), it.y.toFloat()) }
    }
    val length = baseline.zipWithNext().sumOf { (a, b) -> hypot(a.x - b.x, a.y - b.y) }.toFloat()
    val textAlign = paint.textAlign
    paint.textAlign = Paint.Align.LEFT
    val start = (length - paint.measureText(name)) / 2
    paint.color = android.graphics.Color.BLACK
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = paint.textSize * .20f
    paint.strokeJoin = Paint.Join.ROUND
    canvas.drawTextOnPath(name, path, start, -offset, paint)
    paint.color = tint
    paint.style = Paint.Style.FILL
    canvas.drawTextOnPath(name, path, start, -offset, paint)
    paint.textAlign = textAlign
    occupied += bounds
    return true
}
