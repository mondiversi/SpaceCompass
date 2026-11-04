package me.mondiversi.spacecompass

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp

/** A daily path arc with a fitted 24 and the same painted bounds as Export. */
@Composable
internal fun SpaceCompassDailyPathIcon(iconSize: Dp, strokeWidth: Dp) {
    val color = LocalContentColor.current
    val numeric = LocalSpaceCompassNumericFormat.current
    val locale = LocalSpaceCompassDeviceLocale.current
    val number = remember(numeric, locale) {
        formatSpaceCompassNumber(24.0, 0, numeric, grouping = false, systemLocale = locale)
    }
    val paint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
    }
    val bounds = remember { Rect() }
    Canvas(Modifier.size(iconSize)) {
        val width = strokeWidth.toPx()
        val ink = spaceCompassExportIconInkSize(strokeWidth, iconSize)
        val spanWidth = ink.width.toPx() - width
        val spanHeight = ink.height.toPx() - width
        val left = (size.width - spanWidth) / 2
        val top = (size.height - spanHeight) / 2
        drawArc(color, startAngle = 180f, sweepAngle = 180f, useCenter = false,
            topLeft = Offset(left, top), size = Size(spanWidth, spanHeight * 2),
            style = Stroke(width, cap = StrokeCap.Round))

        // The number is part of the glyph: fit its painted bounds rather than using font scaling.
        paint.color = color.toArgb()
        paint.textSize = size.minDimension * .42f
        paint.getTextBounds(number, 0, number.length, bounds)
        val fit = minOf(1f, spanWidth * .65f / bounds.width().coerceAtLeast(1),
            spanHeight * .42f / bounds.height().coerceAtLeast(1))
        paint.textSize *= fit
        paint.getTextBounds(number, 0, number.length, bounds)
        val textLeft = center.x - (bounds.left + bounds.right) / 2f
        val baseline = top + spanHeight * .78f - (bounds.top + bounds.bottom) / 2f
        drawContext.canvas.nativeCanvas.drawText(number, textLeft, baseline, paint)
    }
}
