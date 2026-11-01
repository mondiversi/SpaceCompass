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
    viewportWidth: Float = canvas.width.toFloat(), viewportHeight: Float = canvas.height.toFloat(),
    placement: SpaceCompassCurveCaptionPlacementState? = null, placementKey: String = "",
    projection: SpaceCompassPerspective? = null): Boolean {
    val layout = if (placement != null) {
        val scale = paint.textSize / 24f
        placement.viewport(viewportWidth.toInt(), viewportHeight.toInt(), scale, projection)
        placement.place(placementKey, segments, listOf(arrow.center), listOf(paint.textSize), offset.toDouble(),
            viewportWidth.toDouble(), viewportHeight.toDouble(), 0.0, occupied.map {
                SpaceCompassSunSceneFrame(it.left.toDouble(), it.top.toDouble(), it.width().toDouble(), it.height().toDouble())
            }, scale, followPreviousAnchor = false) { paint.measureText(name).toDouble() }?.layout
    } else spaceCompassCurveTextLayout(segments, arrow.center, paint.measureText(name).toDouble(),
        paint.textSize.toDouble(), offset.toDouble())
    if (layout == null) return false
    val bounds = (if (placement == null) listOf(layout.bounds) else layout.collisionFrames).map { b ->
        RectF(b.left.toFloat(), b.top.toFloat(), (b.left+b.width).toFloat(), (b.top+b.height).toFloat())
    }
    if (bounds.any { run -> run.left < 0 || run.top < 0 || run.right > viewportWidth || run.bottom > viewportHeight ||
        occupied.any { RectF.intersects(it, run) } }) return false
    val baseline = layout.baseline
    drawSpaceCompassCurveText(canvas, name, baseline, paint, tint)
    occupied.addAll(bounds)
    return true
}


/** One shaped string on its measured baseline; orbit and reference names share glyph spacing/outline. */
internal fun drawSpaceCompassCurveText(canvas: Canvas, name: String,
    baseline: List<SpaceCompassSunScenePoint>, paint: Paint, tint: Int) {
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
    paint.strokeWidth = paint.textSize * .12f
    paint.strokeJoin = Paint.Join.ROUND
    canvas.drawTextOnPath(name, path, start, 0f, paint)
    paint.color = tint
    paint.style = Paint.Style.FILL
    canvas.drawTextOnPath(name, path, start, 0f, paint)
    paint.textAlign = textAlign
}
