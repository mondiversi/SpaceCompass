package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
internal fun SpaceCompassAntipodesButton(active: Boolean, onToggle: () -> Unit, color: Color, background: Color,
    selectedCount: Int, onClear: () -> Unit, modifier: Modifier = Modifier) {
    if (selectedCount <= 0) return
    val label = stringResource(R.string.scene_antipodes)
    val count = formatSpaceCompassNumber(selectedCount.toDouble(), 0,
        LocalSpaceCompassNumericFormat.current, grouping = false)
    val ink = spaceCompassFloatingControlTint(color, active)
    val haptics = rememberSpaceCompassHapticController()
    SpaceCompassFloatingActionButton(label, {
        haptics.spaceCompassHaptic(if (active) SpaceCompassHapticCue.TOGGLE_OFF else SpaceCompassHapticCue.TOGGLE_ON)
        onToggle()
    }, ink, background, modifier.testTag("toggle-antipodes")
        .spaceCompassTutorialTarget(SpaceCompassTutorialFeature.ANTIPODES), selectedState = active, stateText = count,
        onLongClickLabel = stringResource(R.string.scene_clear_points), onLongClick = {
            haptics.spaceCompassHaptic(SpaceCompassHapticCue.COMMAND)
            onClear()
        },
        glyphInk = spaceCompassFloatingInk(26f, .125f, .125f, .875f, .875f), overlay = {
            SpaceCompassFloatingCountBadge(count, Modifier.align(Alignment.TopEnd).testTag("antipodes-count"))
        }) {
        val width = SpaceCompassFloatingIconStrokeWidth * LocalSpaceCompassFloatingStrokeScale.current
        Canvas(Modifier.size(26.dp)) {
            drawSpaceCompassAntipode(center, size.minDimension * .32f, ink, width.toPx(), halo = false)
            if (!active) drawLine(ink, Offset(size.width * .17f, size.height * .17f),
                Offset(size.width * .83f, size.height * .83f), width.toPx(), StrokeCap.Round)
        }
    }
}
