package me.mondiversi.spacecompass

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection

/** Fixed configuration glyphs: upright camera-axis pointing is 90°, flat lengthwise pointing is 0°. */
@Composable
internal fun SpaceCompassPointingAxisGlyph(horizontal: Boolean, label: String, tint: Color, modifier: Modifier) {
    val numeric = LocalSpaceCompassNumericFormat.current
    val locale = LocalSpaceCompassDeviceLocale.current
    val angles = remember(numeric, locale) {
        listOf(0.0, 90.0).map { formatSpaceCompassNumber(it, 0, numeric, grouping = false, systemLocale = locale) + "°" }
    }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val paint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
    }
    val textBounds = remember { Rect() }
    val numberBounds = remember { Rect() }
    Canvas(modifier.semantics { contentDescription = label }) {
        val scale = size.minDimension
        val stroke = scale * .065f
        val lineHalf = scale * .33f
        paint.color = tint.toArgb()
        paint.textSize = scale * .56f
        // One fitted text size keeps 0° and 90° visually consistent, including localized digits.
        val widest = angles.maxOf { paint.measureText(it) }
        val available = size.width * .70f
        if (widest > available) paint.textSize *= available / widest
        val text = angles[if (horizontal) 0 else 1]
        // Lower the whole 0° glyph slightly for optical centering inside its circular control.
        val horizontalOffsetY = scale * .05f
        val textCenter = if (horizontal) Offset(center.x, size.height * .335f + horizontalOffsetY)
            else Offset(size.width * if (rtl) .35f else .65f, center.y)
        if (horizontal) drawLine(tint, Offset(center.x - lineHalf, size.height * .725f + horizontalOffsetY),
            Offset(center.x + lineHalf, size.height * .725f + horizontalOffsetY), stroke, StrokeCap.Round)
        else {
            val x = size.width * if (rtl) .86f else .14f
            drawLine(tint, Offset(x, center.y - lineHalf), Offset(x, center.y + lineHalf), stroke, StrokeCap.Round)
        }
        paint.getTextBounds(text, 0, text.length, textBounds)
        val baseline = textCenter.y - (textBounds.top + textBounds.bottom) / 2f
        // Center the visible zero itself over the bar; the degree mark remains to its right.
        // The vertical option centers the full 90° label beside its line.
        val anchor = if (horizontal) {
            paint.getTextBounds(text, 0, text.length - 1, numberBounds)
            numberBounds
        } else textBounds
        val left = textCenter.x - (anchor.left + anchor.right) / 2f
        drawContext.canvas.nativeCanvas.drawText(text, left, baseline, paint)
    }
}
