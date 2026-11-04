package me.mondiversi.spacecompass

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.text.StaticLayout
import android.text.TextPaint
import kotlin.math.ceil

/** One photo-sized type and spacing scale for both information bands, independent of orientation. */
internal data class SpaceCompassCaptureBandStyle(val scale: Float) {
    init { require(scale.isFinite() && scale > 0) }
    val textSize = 26 * scale
    val horizontalInset = 16 * scale
    val verticalInset = 8 * scale
    val markerGap = 8 * scale
    val itemGap = 24 * scale
    val rowGap = 8 * scale
    fun paint() = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = this@SpaceCompassCaptureBandStyle.textSize
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
}

internal fun spaceCompassCaptureBandStyle(width: Int, height: Int = (width / 2).coerceAtLeast(1)):
    SpaceCompassCaptureBandStyle {
    require(width > 0 && height > 0)
    return SpaceCompassCaptureBandStyle((minOf(width, height) / 1080f).coerceAtLeast(.45f))
}

/** Center visible glyphs rather than the font's invisible ascender/descender padding. */
internal fun spaceCompassCaptureTextInk(value: String, layout: StaticLayout): RectF {
    val ink = Rect()
    val result = RectF()
    val paint = layout.paint
    for (line in 0 until layout.lineCount) {
        paint.getTextBounds(value, layout.getLineStart(line), layout.getLineVisibleEnd(line), ink)
        if (ink.isEmpty) { ink.top = paint.fontMetrics.ascent.toInt(); ink.bottom = ceil(paint.fontMetrics.descent).toInt() }
        val baseline = layout.getLineBaseline(line)
        result.union(RectF(layout.getLineLeft(line), (baseline + ink.top).toFloat(),
            layout.getLineRight(line), (baseline + ink.bottom).toFloat()))
    }
    return result
}
