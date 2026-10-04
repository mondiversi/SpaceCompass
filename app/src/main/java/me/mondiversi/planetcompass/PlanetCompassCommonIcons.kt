package me.mondiversi.planetcompass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Title-bar icons share a fixed stroke; smaller in-content icons keep their own sizing. */
@Composable
internal fun PlanetCompassPasswordVisibilityIcon(
    visible: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    Canvas(
        modifier =
            modifier
                .size(22.dp)
                .graphicsLayer(alpha = tint.alpha)
    ) {
        val tint = tint.copy(alpha = 1f)
        val strokeWidth = maxOf(1.6.dp.toPx(), size.minDimension * 0.08f)
        val eye = Path().apply {
            moveTo(size.width * 0.08f, size.height * 0.50f)
            cubicTo(
                size.width * 0.28f,
                size.height * 0.18f,
                size.width * 0.72f,
                size.height * 0.18f,
                size.width * 0.92f,
                size.height * 0.50f
            )
            cubicTo(
                size.width * 0.72f,
                size.height * 0.82f,
                size.width * 0.28f,
                size.height * 0.82f,
                size.width * 0.08f,
                size.height * 0.50f
            )
        }
        drawPath(
            path = eye,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        drawCircle(
            color = tint,
            radius = size.minDimension * 0.12f,
            center = Offset(size.width * 0.50f, size.height * 0.50f)
        )
        if (!visible) {
            drawLine(
                color = tint,
                start = Offset(size.width * 0.18f, size.height * 0.16f),
                end = Offset(size.width * 0.82f, size.height * 0.84f),
                strokeWidth = strokeWidth * 1.15f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
internal fun PlanetCompassDisclosureChevron(
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    Canvas(
        modifier =
            modifier
                .size(
                    width = 10.dp,
                    height = 16.dp
                )
                .graphicsLayer(alpha = tint.alpha)
    ) {
        val tint = tint.copy(alpha = 1f)
        val centerY = size.height / 2f
        val startX = size.width * 0.22f
        val tipX = size.width * 0.76f
        val halfHeight = size.height * 0.32f
        val strokeWidth =
            maxOf(
                1.8.dp.toPx(),
                size.minDimension * 0.17f
            )

        drawLine(
            color = tint,
            start = Offset(startX, centerY - halfHeight),
            end = Offset(tipX, centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(tipX, centerY),
            end = Offset(startX, centerY + halfHeight),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}
