package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
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

@Composable
internal fun SpaceCompassAntipodesButton(active: Boolean, onToggle: () -> Unit, color: Color, background: Color,
    selectedCount: Int, onClear: () -> Unit, modifier: Modifier = Modifier) {
    if (selectedCount <= 0) return
    val labelId = if (active) R.string.scene_selected_points_antipodes else R.string.scene_selected_points
    val label = stringResource(labelId)
    val count = formatSpaceCompassNumber(selectedCount.toDouble(), 0,
        LocalSpaceCompassNumericFormat.current, grouping = false)
    // The two symbols express different concepts; the control and counter keep their neutral palette.
    val ink = color
    val haptics = rememberSpaceCompassHapticController()
    SpaceCompassFloatingActionButton(label, onToggle, ink, background, modifier.testTag("toggle-antipodes")
        .spaceCompassTutorialTarget(SpaceCompassTutorialFeature.ANTIPODES, title = labelId), selectedState = active, stateText = count,
        onLongClickLabel = stringResource(R.string.scene_clear_points), onLongClick = {
            haptics.spaceCompassHaptic(SpaceCompassHapticCue.COMMAND)
            onClear()
        },
        glyphInk = spaceCompassFloatingInk(26f, .125f, .125f, .875f, .875f), overlay = {
            SpaceCompassFloatingCountBadge(count, Modifier.align(Alignment.TopEnd).testTag("antipodes-count"),
                accent = color,
                contentColor = spaceCompassCounterTextColor(isSystemInDarkTheme()))
        }) {
        val width = SpaceCompassFloatingIconStrokeWidth * LocalSpaceCompassFloatingStrokeScale.current
        Canvas(Modifier.size(26.dp)) {
            val radius = size.minDimension * 9f / 24f
            if (active) drawSpaceCompassAntipode(center, radius, ink, width.toPx())
            else {
                // Reuse the original selected-point symbol; holding still clears the selected points.
                fun point(x: Float, y: Float) = Offset(x * size.width / 24, y * size.height / 24)
                val center = point(12f, 12f)
                drawCircle(ink, radius = radius, center = center, style = Stroke(width.toPx()))
                drawCircle(ink, radius = size.width * 2f / 24f, center = center)
                drawLine(ink, point(8f, 8f), point(16f, 16f), width.toPx(), cap = StrokeCap.Round)
                drawLine(ink, point(16f, 8f), point(8f, 16f), width.toPx(), cap = StrokeCap.Round)
            }
        }
    }
}
