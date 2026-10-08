package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

private val celestialZoomTargetSize = 48.dp
private val celestialZoomIconSize = 18.dp
private val celestialZoomChoiceSpacing = 30.dp
private val celestialZoomIconInset = (celestialZoomTargetSize - celestialZoomChoiceSpacing) / 2

/** Bare glyphs over the model; sibling hit targets keep taps out of the model's drag gestures. */
@Composable
internal fun SpaceCompassCelestialZoomControls(viewport: SpaceCompassCelestialViewportState,
    onZoom: (Double) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Row(modifier.testTag("celestial-view-zoom-controls"), verticalAlignment = Alignment.CenterVertically) {
        CelestialZoomButton(true, stringResource(R.string.celestial_view_zoom_in),
            enabled && viewport.zoom < SPACE_COMPASS_CELESTIAL_MAX_ZOOM,
            Modifier.testTag("celestial-view-zoom-in"), iconOffset = celestialZoomIconInset) { onZoom(1.25) }
        CelestialZoomButton(false, stringResource(R.string.celestial_view_zoom_out),
            enabled && viewport.zoom > SPACE_COMPASS_CELESTIAL_MIN_ZOOM,
            Modifier.testTag("celestial-view-zoom-out"), iconOffset = -celestialZoomIconInset) { onZoom(.8) }
    }
}

@Composable
private fun CelestialZoomButton(plus: Boolean, label: String, enabled: Boolean,
    modifier: Modifier, iconOffset: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val tint = Color.White.copy(alpha = if (!enabled) .35f else if (pressed) .7f else .95f)
    // No outline, container, ripple or pressed background; keep a full accessible target.
    Box(modifier.size(celestialZoomTargetSize)
        .clickable(interactionSource = interaction, indication = null, enabled = enabled,
            role = Role.Button, onClick = onClick)
        .spaceCompassAccessibleAction(label, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center) {
        // Match the switch: 18 dp glyphs, 30 dp between centres, with separate 48 dp touch targets.
        Canvas(Modifier.offset(x = iconOffset).size(celestialZoomIconSize)) {
            val inset = 2.dp.toPx()
            drawLine(tint, Offset(inset, center.y), Offset(size.width - inset, center.y),
                1.6.dp.toPx(), StrokeCap.Round)
            if (plus) drawLine(tint, Offset(center.x, inset), Offset(center.x, size.height - inset),
                1.6.dp.toPx(), StrokeCap.Round)
        }
    }
}
