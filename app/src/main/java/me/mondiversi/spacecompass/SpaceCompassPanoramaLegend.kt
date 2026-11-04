package me.mondiversi.spacecompass

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import androidx.compose.ui.graphics.toArgb
import kotlin.math.ceil

internal data class SpaceCompassPanoramaLegendEntry(val label: StaticLayout, val tint: Int, val width: Float,
    val ink: RectF)
internal data class SpaceCompassPanoramaLegendRow(val entries: List<SpaceCompassPanoramaLegendEntry>, val width: Float) {
    fun height(markerDiameter: Float) = maxOf(markerDiameter, entries.maxOfOrNull { it.ink.height() } ?: 0f)
}

/** The footer shares the heading's exact text style; its height grows only with wrapped content. */
internal class SpaceCompassPanoramaLegend(val width: Int, val height: Int,
    private val rows: List<SpaceCompassPanoramaLegendRow>, private val style: SpaceCompassCaptureBandStyle,
    private val markerDiameter: Float) {
    fun draw(canvas: Canvas, top: Float, groundColor: Int) {
        if (rows.isEmpty()) return
        canvas.save(); canvas.translate(0f, top)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = groundColor }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.color = 0x99000000.toInt()
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        val markerAdvance = markerDiameter + style.markerGap
        var y = style.verticalInset
        rows.forEach { row ->
            var x = (width - row.width) / 2f
            val rowHeight = row.height(markerDiameter)
            row.entries.forEach { entry ->
                drawSpaceCompassCelestialColorDot(canvas, x + markerDiameter / 2,
                    y + rowHeight / 2, markerDiameter, entry.tint, entry.label.paint.color,
                    markerDiameter / 14f, paint)
                canvas.save()
                canvas.translate(x + markerAdvance - entry.ink.left,
                    y + (rowHeight - entry.ink.height()) / 2 - entry.ink.top)
                entry.label.draw(canvas); canvas.restore()
                x += entry.width + style.itemGap
            }
            y += rowHeight + style.rowGap
        }
        canvas.restore()
    }
}

/** Frozen localized names and identity colors; long names wrap at full size instead of truncating. */
internal fun prepareSpaceCompassPanoramaLegend(objects: List<SpaceCompassPanoramaObject>, width: Int,
    style: SpaceCompassCaptureBandStyle = spaceCompassCaptureBandStyle(width)): SpaceCompassPanoramaLegend {
    require(width > 0)
    val available = (width - style.horizontalInset * 2).coerceAtLeast(1f)
    val paint = style.paint()
    val markerDiameter = paint.textSize * .75f
    val markerAdvance = markerDiameter + style.markerGap
    val rows = mutableListOf<SpaceCompassPanoramaLegendRow>()
    var entries = mutableListOf<SpaceCompassPanoramaLegendEntry>()
    var rowWidth = 0f
    objects.distinctBy { it.body to it.comparison?.id }.forEach { item ->
        val labelWidth = ceil(Layout.getDesiredWidth(item.name, paint)).toInt()
            .coerceIn(1, (available - markerAdvance).toInt().coerceAtLeast(1))
        val label = StaticLayout.Builder.obtain(item.name, 0, item.name.length, paint, labelWidth)
            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR).setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE).setIncludePad(false).build()
        val ink = spaceCompassCaptureTextInk(item.name, label)
        val entry = SpaceCompassPanoramaLegendEntry(label, item.tint.toArgb(), markerAdvance + ink.width(), ink)
        val gap = if (entries.isEmpty()) 0f else style.itemGap
        if (entries.isNotEmpty() && rowWidth + gap + entry.width > available) {
            rows += SpaceCompassPanoramaLegendRow(entries, rowWidth); entries = mutableListOf(); rowWidth = 0f
        }
        if (entries.isNotEmpty()) rowWidth += style.itemGap
        entries += entry; rowWidth += entry.width
    }
    if (entries.isNotEmpty()) rows += SpaceCompassPanoramaLegendRow(entries, rowWidth)
    val height = if (rows.isEmpty()) 0 else ceil(style.verticalInset * 2 + rows.sumOf {
        it.height(markerDiameter).toDouble()
    } + (rows.size - 1) * style.rowGap).toInt()
    return SpaceCompassPanoramaLegend(width, height, rows, style, markerDiameter)
}
