package me.mondiversi.spacecompass

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/** A clear command with the supplied theme accent; visible only while points are selected. */
@Composable
internal fun SpaceCompassClearOrbitPointsButton(onClear: () -> Unit, color: Color, background: Color,
    modifier: Modifier = Modifier) {
    val label = stringResource(R.string.scene_clear_points)
    val haptics = rememberSpaceCompassHapticController()
    val clear: () -> Unit = { haptics.spaceCompassHaptic(SpaceCompassHapticCue.COMMAND); onClear() }
    SpaceCompassFloatingControlHitRegion {
        Surface(onClick = clear, modifier = modifier.spaceCompassFloatingControlVisual().size(48.dp)
            .testTag("clear-orbit-points").spaceCompassAccessibleAction(label, enabled = true, onClick = clear),
            shape = CircleShape, color = background.copy(alpha = 1f), contentColor = color,
            border = BorderStroke(1.dp, color.copy(alpha = .35f)), shadowElevation = 3.dp) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(26.dp)) {
                    fun point(x: Float, y: Float) = Offset(x * size.width / 24, y * size.height / 24)
                    val center = point(12f, 12f)
                    drawCircle(color, radius = size.width * 9f / 24f, center = center,
                        style = Stroke(2.dp.toPx()))
                    drawCircle(color, radius = size.width * 2f / 24f, center = center)
                    drawLine(color, point(8f, 8f), point(16f, 16f), 2.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(color, point(16f, 8f), point(8f, 16f), 2.dp.toPx(), cap = StrokeCap.Round)
                }
            }
        }
    }
}
