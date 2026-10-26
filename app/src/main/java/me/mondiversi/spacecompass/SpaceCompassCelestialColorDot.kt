package me.mondiversi.spacecompass

import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

/** The same above/below-horizon key is used by the live island and captured legends. */
@Composable
internal fun SpaceCompassCelestialColorDot(above: Color, outline: Color, modifier: Modifier) {
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG) }
    ComposeCanvas(modifier) {
        drawSpaceCompassCelestialColorDot(drawContext.canvas.nativeCanvas,
            size.width / 2f, size.height / 2f, size.minDimension,
            above.toArgb(), outline.toArgb(), .75.dp.toPx(), paint)
    }
}

/** Keep the split horizontal and inset the complete outline inside the measured circle. */
internal fun drawSpaceCompassCelestialColorDot(canvas: Canvas, x: Float, y: Float,
    diameter: Float, above: Int, outline: Int, borderWidth: Float, paint: Paint) {
    val stroke = borderWidth.coerceIn(0f, diameter)
    val radius = ((diameter - stroke) / 2f).coerceAtLeast(0f)
    paint.style = Paint.Style.FILL
    paint.color = above
    canvas.drawCircle(x, y, radius, paint)
    // Paint over the lower half of a complete circle, avoiding an antialias seam at the equator.
    canvas.save()
    canvas.clipRect(x - radius, y, x + radius, y + radius)
    paint.color = spaceCompassCelestialPathVisibilityTint(Color(above), true).toArgb()
    canvas.drawCircle(x, y, radius, paint)
    canvas.restore()
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = stroke
    paint.color = outline
    canvas.drawCircle(x, y, radius, paint)
    paint.style = Paint.Style.FILL
}
