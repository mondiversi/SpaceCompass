package me.mondiversi.spacecompass

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Accent only a persistent active state; disabled controls retain their neutral disabled tint. */
@Composable
internal fun spaceCompassFloatingControlTint(color: Color, active: Boolean, enabled: Boolean = true): Color =
    if (active && enabled) MaterialTheme.colorScheme.primary else color

/** A fixed-size floating action; badges share its scale without being clipped by the circle. */
@Composable
internal fun SpaceCompassFloatingActionButton(
    label: String,
    onClick: () -> Unit,
    color: Color,
    background: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selectedState: Boolean? = null,
    stateText: String? = null,
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable () -> Unit
) {
    SpaceCompassFloatingControlHitRegion {
        Box(Modifier.spaceCompassFloatingControlVisual().size(48.dp)) {
            Surface(onClick = onClick, enabled = enabled,
                modifier = Modifier.fillMaxSize().then(modifier).spaceCompassAccessibleAction(
                    label, enabled = enabled, selectedState = selectedState,
                    stateText = stateText, onClick = onClick),
                shape = CircleShape, color = background.copy(alpha = .94f),
                contentColor = color.copy(alpha = if (enabled) 1f else .38f),
                border = BorderStroke(1.dp, color.copy(alpha = .35f)), shadowElevation = 3.dp) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
            }
            overlay()
        }
    }
}
