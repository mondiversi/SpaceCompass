package me.mondiversi.spacecompass

import android.animation.ValueAnimator
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.runtime.rememberUpdatedState
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
    modifier: Modifier = Modifier,
    cameraEnabled: Boolean = false,
    animateBorder: Boolean = true
) {
    val label = stringResource(R.string.panorama_capture)
    val haptics = rememberSpaceCompassHapticController()
    val background = MaterialTheme.colorScheme.primary
    val foreground = spaceCompassCounterTextColor(isSystemInDarkTheme())
    // Balance the denser shutter/orbit artwork with the neighboring open symbols.
    val glyphStroke = SpaceCompassFloatingIconStrokeWidth * .8f
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val restingBackground = background.copy(alpha = 1f)
    // Read the animated opacity only while drawing, keeping layout and scene composition stable.
    val borderOpacity = if (enabled && animateBorder && ValueAnimator.areAnimatorsEnabled()) {
        rememberInfiniteTransition(label = "captureBorderPulse").animateFloat(
            initialValue = .40f, targetValue = .70f,
            animationSpec = infiniteRepeatable(tween(1_600), RepeatMode.Reverse),
            label = "captureBorderOpacity"
        )
    } else rememberUpdatedState(if (enabled) .35f else .12f)
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
                    val edge = 2.dp.toPx()
                    drawCircle(Color.White.copy(alpha = borderOpacity.value),
                        radius = (size.minDimension - edge) / 2, style = Stroke(edge))
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
                SpaceCompassFloatingGlyph(if (cameraEnabled) spaceCompassFloatingInk(26f, .11f, .11f, .89f, .89f, glyphStroke.value)
                    else spaceCompassFloatingInk(26f, .12f, .22f, .88f, .76f, glyphStroke.value * 2.5f),
                    circleSide = 48.dp) {
                    SpaceCompassSettingsMenuIcon(
                        if (cameraEnabled) "capture" else "panorama",
                        foreground.copy(alpha = if (enabled) 1f else .38f),
                        modifier = Modifier.size(26.dp),
                        iconStrokeWidth = glyphStroke
                    )
                }
            }
        }
    }
}
