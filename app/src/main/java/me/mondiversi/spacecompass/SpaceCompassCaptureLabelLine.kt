package me.mondiversi.spacecompass

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect

/** Reserve one em for the same vector used by Compose and the Globe renderer. */
internal fun measureSpaceCompassCaptureLabelLine(line: String, paint: Paint): Float =
    line.split(SPACE_COMPASS_ANTIPODE_CAPTION_ICON).let { parts ->
        parts.sumOf { paint.measureText(it).toDouble() }.toFloat() + (parts.size - 1) * paint.textSize
    }

/** Align each angle glyph to the visible ink of its own value, not the font's shared baseline. */
internal fun drawSpaceCompassCaptureLabelLine(canvas: Canvas, line: String, x: Float,
    baseline: Float, paint: Paint) {
    if (paint.textAlign != Paint.Align.LEFT) {
        val width = measureSpaceCompassCaptureLabelLine(line,paint)
        val left = x - if (paint.textAlign == Paint.Align.CENTER) width/2 else width
        drawSpaceCompassCaptureLabelLine(canvas,line,left,baseline,Paint(paint).apply { textAlign = Paint.Align.LEFT })
        return
    }
    if (SPACE_COMPASS_ANTIPODE_CAPTION_ICON in line) {
        val split = line.split(SPACE_COMPASS_ANTIPODE_CAPTION_ICON)
        val rtl = !java.text.Bidi(line,java.text.Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT).baseIsLeftToRight()
        val parts = if (rtl) split.reversed() else split
        val metrics = paint.fontMetrics
        val centerY = baseline + (metrics.ascent + metrics.descent)/2
        var cursor = x
        parts.forEachIndexed { index,part ->
            canvas.drawText(part,cursor,baseline,paint)
            cursor += paint.measureText(part)
            if (index < parts.lastIndex) {
                drawSpaceCompassAntipode(canvas,cursor+paint.textSize/2,centerY,
                    paint.textSize*.37f,paint.color,paint.textSize*.08f)
                cursor += paint.textSize
            }
        }
        return
    }
    val elevationIndex = line.indexOf(" · ↕ ").takeIf { line.startsWith("↔ ") && it >= 0 }?.plus(3)
    if (elevationIndex == null) {
        canvas.drawText(line, x, baseline, paint)
        return
    }
    val symbolBounds = Rect()
    val valueBounds = Rect()
    var start = 0
    for (index in listOf(0, elevationIndex)) {
        if (start < index) canvas.drawText(line, start, index,
            x + paint.measureText(line, 0, start), baseline, paint)
        val valueEnd = line.indexOf('·', index + 1).takeIf { it >= 0 } ?: line.length
        paint.getTextBounds(line, index, index + 1, symbolBounds)
        paint.getTextBounds(line, index + 1, valueEnd, valueBounds)
        val shift = if (symbolBounds.isEmpty || valueBounds.isEmpty) 0f else
            valueBounds.exactCenterY() - symbolBounds.exactCenterY()
        canvas.drawText(line, index, index + 1, x + paint.measureText(line, 0, index),
            baseline + shift, paint)
        start = index + 1
    }
    canvas.drawText(line, start, line.length, x + paint.measureText(line, 0, start), baseline, paint)
}
