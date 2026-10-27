package me.mondiversi.spacecompass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

private const val CaptureControlSizeMultiplier = 1.2f

@Composable
internal fun SpaceCompassCaptureButton(
    onCapture: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    cameraEnabled: Boolean = false
) {
    val label = stringResource(R.string.panorama_capture)
    val haptics = rememberSpaceCompassHapticController()
    val background = MaterialTheme.colorScheme.primary
    val foreground = Color.White
    val iconColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.background else foreground
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val restingBackground = background.copy(alpha = 1f)
    // Animate the circular surface itself instead of a platform ripple mask.
    val containerColor by animateColorAsState(
        targetValue = if (enabled && pressed) lerp(restingBackground, foreground, .12f) else restingBackground,
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
                .size(48.dp * CaptureControlSizeMultiplier)
                .testTag("capture-panorama")
                .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = capture)
                .spaceCompassAccessibleAction(label, enabled = enabled, onClick = capture),
            shape = CircleShape,
            color = containerColor,
            contentColor = foreground,
            border = BorderStroke(2.dp, foreground.copy(alpha = if (enabled) .35f else .12f)),
            // Native elevation shadows show a polygon through the translucent disabled fill.
            shadowElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                SpaceCompassSettingsMenuIcon(
                    if (cameraEnabled) "capture" else "panorama",
                    iconColor.copy(alpha = if (enabled) 1f else .38f),
                    modifier = Modifier.size(26.dp * CaptureControlSizeMultiplier),
                    iconStrokeWidth = SpaceCompassFloatingIconStrokeWidth
                )
            }
        }
    }
}
