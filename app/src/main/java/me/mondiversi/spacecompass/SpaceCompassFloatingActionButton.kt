package me.mondiversi.spacecompass

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.clearAndSetSemantics

/** Muted off states remain actionable; null keeps ordinary actions neutral. */
@Composable
internal fun spaceCompassFloatingControlTint(color: Color, active: Boolean?, enabled: Boolean = true): Color = when {
    !enabled -> color
    active == true -> MaterialTheme.colorScheme.primary
    active == false -> lerp(color, MaterialTheme.colorScheme.surface, .55f)
    else -> color
}

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
    glyphInk: SpaceCompassFloatingInk? = null,
    border: BorderStroke = BorderStroke(1.dp, color.copy(alpha = .35f)),
    overlay: @Composable BoxScope.() -> Unit = {},
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    SpaceCompassFloatingControlHitRegion {
        Box(Modifier.spaceCompassFloatingControlVisual().size(48.dp)) {
            val target = Modifier.fillMaxSize().then(modifier).spaceCompassAccessibleAction(
                label, enabled = enabled, selectedState = selectedState,
                onLongClickLabel = onLongClickLabel, onLongClick = onLongClick,
                stateText = stateText, onClick = onClick)
            val face: @Composable () -> Unit = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (glyphInk == null) content() else SpaceCompassFloatingGlyph(glyphInk, content = content)
                }
            }
            if (onLongClick == null) {
                Surface(onClick = onClick, enabled = enabled, modifier = target,
                    shape = CircleShape, color = background.copy(alpha = backgroundAlpha),
                    contentColor = color.copy(alpha = if (enabled) 1f else .38f),
                    border = border, shadowElevation = 3.dp, content = face)
            } else {
                // One recognizer owns both gestures, so releasing a long press never triggers a click.
                Surface(modifier = target, shape = CircleShape, color = background.copy(alpha = backgroundAlpha),
                    contentColor = color.copy(alpha = if (enabled) 1f else .38f),
                    border = border, shadowElevation = 3.dp) {
                    Box(Modifier.fillMaxSize().combinedClickable(enabled = enabled,
                        onClickLabel = label, onLongClickLabel = onLongClickLabel,
                        onLongClick = onLongClick, hapticFeedbackEnabled = false, onClick = onClick)) { face() }
                }
            }
            overlay()
        }
    }
}

/** Filled counter with accent defaults, enlarged by 10% from the original 20 dp badge.
 * Minimum sizing lets accessibility text grow and longer counts widen without clipping. */
@Composable
internal fun SpaceCompassFloatingCountBadge(count: String, modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = spaceCompassCounterTextColor(isSystemInDarkTheme())) {
    Box(modifier.defaultMinSize(minWidth = 22.dp, minHeight = 22.dp)
        .background(accent, CircleShape)
        .clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        Text(count, modifier = Modifier.padding(horizontal = 3.3.dp, vertical = 2.dp),
            color = contentColor,
            fontSize = 12.1.sp, lineHeight = 14.3.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}
