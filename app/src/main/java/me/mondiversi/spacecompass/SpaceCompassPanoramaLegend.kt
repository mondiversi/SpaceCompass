package me.mondiversi.spacecompass

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.ui.graphics.toArgb
import kotlin.math.ceil

internal data class SpaceCompassPanoramaLegendEntry(val label: StaticLayout, val tint: Int, val width: Float)
internal data class SpaceCompassPanoramaLegendRow(val entries: List<SpaceCompassPanoramaLegendEntry>, val width: Float)

/** A separate footer preserves the panoramic grid and the photograph's full optical frame. */
internal class SpaceCompassPanoramaLegend(val width: Int, val height: Int,
    private val rows: List<SpaceCompassPanoramaLegendRow>, private val scale: Float, private val rowHeight: Float,
    private val markerDiameter: Float) {
    fun draw(canvas: Canvas, top: Float, groundColor: Int) {
        if (rows.isEmpty()) return
        canvas.save()
        canvas.translate(0f, top)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = groundColor }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.color = 0x99000000.toInt()
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        val markerAdvance = markerDiameter + 16 * scale
        rows.forEachIndexed { index, row ->
            var x = (width - row.width) / 2f
            val y = 12 * scale + index * (rowHeight + 12 * scale)
            row.entries.forEach { entry ->
                drawSpaceCompassCelestialColorDot(canvas, x + markerDiameter / 2,
                    y + rowHeight / 2, markerDiameter, entry.tint, entry.label.paint.color,
                    markerDiameter / 14f, paint)
                canvas.save()
                canvas.translate(x + markerAdvance, y + (rowHeight - entry.label.height) / 2)
                entry.label.draw(canvas)
                canvas.restore()
                x += entry.width + 48 * scale
            }
        }
        canvas.restore()
    }
}

/** Frozen localized names and split above/below-horizon identity colors, independent of current elevation. */
internal fun prepareSpaceCompassPanoramaLegend(objects: List<SpaceCompassPanoramaObject>, width: Int,
    scale: Float = width / 4096f): SpaceCompassPanoramaLegend {
    require(width > 0 && scale.isFinite() && scale > 0)
    val available = width - 128 * scale
    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 42 * scale
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    // Match visible letter height, as in the main island, rather than the full line box.
    val markerDiameter = paint.textSize * .75f
    val markerAdvance = markerDiameter + 16 * scale
    val rows = mutableListOf<SpaceCompassPanoramaLegendRow>()
    var entries = mutableListOf<SpaceCompassPanoramaLegendEntry>()
    var rowWidth = 0f
    objects.distinctBy { it.body }.forEach { item ->
        val labelWidth = ceil(Layout.getDesiredWidth(item.name, paint)).toInt()
            .coerceIn(1, (available - markerAdvance).toInt().coerceAtLeast(1))
        val label = StaticLayout.Builder.obtain(item.name, 0, item.name.length, paint, labelWidth)
            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR).setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false).setMaxLines(1).setEllipsize(TextUtils.TruncateAt.END).build()
        val entry = SpaceCompassPanoramaLegendEntry(label,
            spaceCompassCelestialPathTint(item.body).toArgb(),
            markerAdvance + labelWidth)
        val gap = if (entries.isEmpty()) 0f else 48 * scale
        if (entries.isNotEmpty() && rowWidth + gap + entry.width > available) {
            rows += SpaceCompassPanoramaLegendRow(entries, rowWidth)
            entries = mutableListOf()
            rowWidth = 0f
        }
        if (entries.isNotEmpty()) rowWidth += 48 * scale
        entries += entry
        rowWidth += entry.width
    }
    if (entries.isNotEmpty()) rows += SpaceCompassPanoramaLegendRow(entries, rowWidth)
    val rowHeight = rows.flatMap { it.entries }.maxOfOrNull { it.label.height }?.toFloat() ?: 0f
    val height = if (rows.isEmpty()) 0 else ceil(24 * scale + rows.size * rowHeight +
        (rows.size - 1) * 12 * scale).toInt()
    return SpaceCompassPanoramaLegend(width, height, rows, scale, rowHeight, markerDiameter)
}
