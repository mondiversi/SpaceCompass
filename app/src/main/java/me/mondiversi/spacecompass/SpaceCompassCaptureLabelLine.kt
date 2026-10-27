package me.mondiversi.spacecompass

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect

/** Align each angle glyph to the visible ink of its own value, not the font's shared baseline. */
internal fun drawSpaceCompassCaptureLabelLine(canvas: Canvas, line: String, x: Float,
    baseline: Float, paint: Paint) {
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
