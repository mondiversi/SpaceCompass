package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    // The same antipode symbol and counter share the other toggles' muted/active palette.
    val ink = spaceCompassFloatingControlTint(color, active)
    val haptics = rememberSpaceCompassHapticController()
    SpaceCompassFloatingActionButton(label, onToggle, ink, background, modifier.testTag("toggle-antipodes")
        .spaceCompassTutorialTarget(SpaceCompassTutorialFeature.ANTIPODES, title = labelId), selectedState = active, stateText = count,
        onLongClickLabel = stringResource(R.string.scene_clear_points), onLongClick = {
            haptics.spaceCompassHaptic(SpaceCompassHapticCue.COMMAND)
            onClear()
        },
        glyphInk = spaceCompassFloatingInk(26f, .125f, .125f, .875f, .875f), overlay = {
            SpaceCompassFloatingCountBadge(count, Modifier.align(Alignment.TopEnd).testTag("antipodes-count"),
                accent = ink,
                contentColor = spaceCompassCounterTextColor(isSystemInDarkTheme()))
        }) {
        val width = SpaceCompassFloatingIconStrokeWidth * LocalSpaceCompassFloatingStrokeScale.current
        Canvas(Modifier.size(26.dp)) {
            val radius = size.minDimension * 9f / 24f
            drawSpaceCompassAntipode(center, radius, ink, width.toPx())
        }
    }
}
