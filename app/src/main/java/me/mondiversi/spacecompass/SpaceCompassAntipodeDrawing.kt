package me.mondiversi.spacecompass

import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

internal fun DrawScope.drawSpaceCompassAntipode(center: Offset, radius: Float, tint: Color, width: Float,
    halo: Boolean = true) {
    if (halo) drawCircle(Color.Black.copy(alpha = .65f), radius + width, center)
    drawCircle(tint, radius, center, style = Stroke(width))
    val delta = Offset(radius * .48f, -radius * .48f)
    drawLine(tint, center - delta, center + delta, width)
    drawCircle(tint, radius * .22f, center - delta)
    drawCircle(tint, radius * .22f, center + delta)
}

internal fun drawSpaceCompassAntipode(canvas: Canvas, x: Float, y: Float, radius: Float, tint: Int, width: Float) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = tint; strokeWidth = width; style = Paint.Style.STROKE }
    canvas.drawCircle(x, y, radius, paint)
    val delta = radius * .48f
    canvas.drawLine(x - delta, y + delta, x + delta, y - delta, paint)
    paint.style = Paint.Style.FILL
    canvas.drawCircle(x - delta, y + delta, radius * .22f, paint)
    canvas.drawCircle(x + delta, y - delta, radius * .22f, paint)
}
