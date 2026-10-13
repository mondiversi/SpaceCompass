package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
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

/** Clear all pinned points; share the object's compact corner badge and accessible count. */
@Composable
internal fun SpaceCompassClearOrbitPointsButton(onClear: () -> Unit, color: Color, background: Color,
    selectedCount: Int, modifier: Modifier = Modifier) {
    if (selectedCount <= 0) return
    val label = stringResource(R.string.scene_clear_points)
    val count = formatSpaceCompassNumber(selectedCount.toDouble(), 0,
        LocalSpaceCompassNumericFormat.current, grouping = false)
    val haptics = rememberSpaceCompassHapticController()
    val clear: () -> Unit = { haptics.spaceCompassHaptic(SpaceCompassHapticCue.COMMAND); onClear() }
    SpaceCompassFloatingActionButton(label, clear, color, background,
        modifier.testTag("clear-orbit-points"), stateText = count,
        overlay = {
            SpaceCompassFloatingCountBadge(count, color, background,
                Modifier.align(Alignment.TopEnd).testTag("clear-orbit-points-count"))
        }) {
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
