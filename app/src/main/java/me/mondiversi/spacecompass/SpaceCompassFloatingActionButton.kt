package me.mondiversi.spacecompass

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
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
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable () -> Unit
) {
    SpaceCompassFloatingControlHitRegion {
        Box(Modifier.spaceCompassFloatingControlVisual().size(48.dp)) {
            Surface(onClick = onClick, enabled = enabled,
                modifier = Modifier.fillMaxSize().then(modifier).spaceCompassAccessibleAction(
                    label, enabled = enabled, selectedState = selectedState,
                    stateText = stateText, onClick = onClick),
                shape = CircleShape, color = background.copy(alpha = 1f),
                contentColor = color.copy(alpha = if (enabled) 1f else .38f),
                border = BorderStroke(1.dp, color.copy(alpha = .35f)), shadowElevation = 3.dp) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
            }
            overlay()
        }
    }
}

/** Shared compact badge; longer counts widen while retaining the same height and corner anchor. */
@Composable
internal fun SpaceCompassFloatingCountBadge(count: String, color: Color, background: Color,
    modifier: Modifier = Modifier) {
    Box(modifier.height(16.dp).widthIn(min = 16.dp)
        .background(background, CircleShape).border(1.dp, color.copy(alpha = .5f), CircleShape)
        .clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        Text(count, modifier = Modifier.padding(horizontal = 3.dp), color = color,
            fontSize = 9.sp, lineHeight = 10.sp, maxLines = 1)
    }
}
