package me.mondiversi.spacecompass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** Ground sits behind the translucent controls and the Sun marker, never masking the marker. */
@Composable
internal fun SpaceCompassSunGroundBackdrop(
    orientation: SpaceCompassSunOrientation?, frame: SpaceCompassSunSceneFrame?, phase: SpaceCompassSunSkyPhase, modifier: Modifier
) {
    val palette = spaceCompassSunGroundPalette(phase)
    val far by animateColorAsState(Color(palette.farArgb), tween(2_000), label = "solar ground distance")
    val near by animateColorAsState(Color(palette.nearArgb), tween(2_000), label = "solar ground foreground")
    val haze by animateColorAsState(Color(palette.hazeArgb), tween(2_000), label = "solar ground horizon")
    Canvas(modifier.testTag("sun-finder-ground")) {
        if (orientation == null || frame == null) return@Canvas
        val projection = projectSpaceCompassSunGround(orientation, frame, size.width.toDouble(), size.height.toDouble())
            ?: return@Canvas
        if (projection.ground.size < 3) return@Canvas
        fun offset(point: SpaceCompassSunScenePoint) = Offset(point.x.toFloat(), point.y.toFloat())
        val origin = projection.horizonOrigin?.let(::offset)
        val direction = offset(projection.groundDirection)
        val groundPath = Path().apply {
            projection.ground.forEachIndexed { index, point ->
                if (index == 0) moveTo(point.x.toFloat(), point.y.toFloat()) else lineTo(point.x.toFloat(), point.y.toFloat())
            }
            close()
        }
        if (origin == null) {
            // Looking vertically down: no false horizon or haze stripe across the ground.
            drawPath(groundPath, near)
            return@Canvas
        }
        val depth = projection.depthScale.toFloat()
        // Soft atmospheric perspective rather than a solid wall with a hard white edge.
        // Distances follow the same gravity projection as the horizon, including pitch/roll.
        drawPath(groundPath, Brush.linearGradient(
            0f to haze.copy(alpha = 0.55f), 0.025f to haze.copy(alpha = 0.88f),
            0.12f to far, 0.40f to lerp(far, near, 0.72f), 1f to near,
            start = origin, end = origin + direction * depth))
        val tangent = Offset(-direction.y, direction.x)
        clipPath(groundPath) {
            // Sparse decorative ground marks get larger/spread out toward the foreground.
            // They suggest a plane, not surveyed terrain, roads, mountains or real obstacles.
            repeat(9) { row ->
                val distance = depth * (0.035f + row * row * 0.018f)
                val spread = distance * 1.5f + 8.dp.toPx()
                repeat(7) { column ->
                    val jitter = ((row * 17 + column * 13) % 11) / 11f
                    val point = origin + direction * (distance + spread * jitter * 0.04f) +
                        tangent * ((column - 3f + jitter * 0.6f) * spread)
                    val length = (2 + jitter * 4).dp.toPx() + distance * 0.022f
                    drawLine(near.copy(alpha = 0.10f), point, point + tangent * length,
                        (0.5.dp.toPx() + distance * 0.002f).coerceAtMost(3.dp.toPx()), StrokeCap.Round)
                }
            }
        }
    }
}
