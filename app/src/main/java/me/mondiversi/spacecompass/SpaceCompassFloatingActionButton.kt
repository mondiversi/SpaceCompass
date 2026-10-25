package me.mondiversi.spacecompass

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.clearAndSetSemantics

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
    backgroundAlpha: Float = 1f,
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable () -> Unit
) {
    SpaceCompassFloatingControlHitRegion {
        Box(Modifier.spaceCompassFloatingControlVisual().size(48.dp)) {
            Surface(onClick = onClick, enabled = enabled,
                modifier = Modifier.fillMaxSize().then(modifier).spaceCompassAccessibleAction(
                    label, enabled = enabled, selectedState = selectedState,
                    stateText = stateText, onClick = onClick),
                shape = CircleShape, color = background.copy(alpha = backgroundAlpha),
                contentColor = color.copy(alpha = if (enabled) 1f else .38f),
                border = BorderStroke(1.dp, color.copy(alpha = .35f)), shadowElevation = 3.dp) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
            }
            overlay()
        }
    }
}

/** Filled accent counter, enlarged by 10% from the original 20 dp badge.
 * Minimum sizing lets accessibility text grow and longer counts widen without clipping. */
@Composable
internal fun SpaceCompassFloatingCountBadge(count: String, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    Box(modifier.defaultMinSize(minWidth = 22.dp, minHeight = 22.dp)
        .background(accent, CircleShape)
        .clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        Text(count, modifier = Modifier.padding(horizontal = 3.3.dp, vertical = 2.dp),
            color = spaceCompassCounterTextColor(isSystemInDarkTheme()),
            fontSize = 12.1.sp, lineHeight = 14.3.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}
