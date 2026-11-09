package me.mondiversi.spacecompass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
internal fun SpaceCompassCaptureButton(
    onCapture: () -> Unit,
    enabled: Boolean,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    val label = stringResource(R.string.panorama_capture)
    val haptics = rememberSpaceCompassHapticController()
    val background = MaterialTheme.colorScheme.primary
    val night = isSystemInDarkTheme()
    val foreground = spaceCompassCounterTextColor(night)
    // Keep the same optically balanced shutter in all three view modes.
    val glyphStroke = SpaceCompassFloatingIconStrokeWidth * .8f
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val restingBackground = background.copy(alpha = 1f)
    // Animate the circular surface itself instead of a platform ripple mask.
    val containerColor by animateColorAsState(
        targetValue = if (enabled && pressed) lerp(restingBackground, Color.White, .12f) else restingBackground,
        animationSpec = tween(durationMillis = if (pressed) 90 else 140),
        label = "capturePressedColor"
    )
    // Share one activation path for touch, keyboard and accessibility without double feedback.
    val capture: () -> Unit = {
        if (enabled) {
            haptics.spaceCompassHaptic(SpaceCompassHapticCue.COMMAND)
            onCapture()
        }
    }
    SpaceCompassFloatingControlHitRegion {
        Surface(
            modifier = modifier.spaceCompassFloatingControlVisual()
                .size(48.dp)
                .testTag("capture-panorama").spaceCompassTutorialTarget(SpaceCompassTutorialFeature.CAPTURE, enabled = enabled)
                .drawWithContent {
                    drawContent()
                    val edge = 1.dp.toPx()
                    drawCircle(borderColor, radius = (size.minDimension - edge) / 2, style = Stroke(edge))
                }
                .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = capture)
                .spaceCompassAccessibleAction(label, enabled = enabled, onClick = capture),
            shape = CircleShape,
            color = containerColor,
            contentColor = foreground,
            // Native elevation shadows show a polygon through the translucent disabled fill.
            shadowElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                SpaceCompassFloatingGlyph(spaceCompassFloatingInk(26f, .11f, .11f, .89f, .89f, glyphStroke.value),
                    circleSide = 48.dp) {
                    SpaceCompassSettingsMenuIcon("capture",foreground.copy(alpha = if (enabled) 1f else .38f),
                        modifier = Modifier.size(26.dp),iconStrokeWidth = glyphStroke)
                }
            }
        }
    }
}
