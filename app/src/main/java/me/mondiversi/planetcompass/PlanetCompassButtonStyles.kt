package me.mondiversi.planetcompass

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal val PlanetCompassDestructiveActionBaseColor = Color(0xFFD32F2F)
internal val PlanetCompassDestructiveOutlinedBorderWidth = 1.5.dp
internal const val PlanetCompassDestructiveOutlinedIconStrokeScale = 1.25f
/** Scoped to Settings/About bodies and data-export bottom buttons, never to dialog text actions or toolbars. */
internal val LocalPlanetCompassSettingsActionButtons = staticCompositionLocalOf { false }
internal val LocalPlanetCompassActionGlyphStrokeScale = staticCompositionLocalOf { 1f }

@Composable
private fun planetCompassOutlinedRestingContainer(color: Color): Color =
    if (LocalPlanetCompassSettingsActionButtons.current) Color.Transparent else color

@Composable
private fun planetCompassOutlinedDisabledContainer(): Color =
    if (LocalPlanetCompassSettingsActionButtons.current) {
        if (isSystemInDarkTheme()) Color(0xFF33343C) else Color(0xFFECEDEF)
    } else planetCompassDisabledActionContainerColor()

@Composable
private fun planetCompassOutlinedDisabledContent(): Color =
    if (LocalPlanetCompassSettingsActionButtons.current) {
        if (isSystemInDarkTheme()) Color(0xFFA1A5AE) else Color(0xFF808790)
    }
    else planetCompassDisabledActionContentColor()

@Composable
private fun planetCompassOutlinedButtonBorder(
    enabled: Boolean,
    color: Color,
    defaultWidth: Dp
): BorderStroke? = if (LocalPlanetCompassSettingsActionButtons.current) {
    val borderColor = if (enabled) {
        color.copy(alpha = if (isSystemInDarkTheme()) 0.48f else 0.38f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = if (isSystemInDarkTheme()) 0.20f else 0.12f)
    }
    BorderStroke(1.dp, borderColor)
} else if (enabled) {
    BorderStroke(defaultWidth, planetCompassQuietActionBorderColor(color))
} else null

internal const val PlanetCompassFloatingPressedTargetScale = 1.11f

/** Visual feedback only: the control's layout and touch target do not change. */
@Composable
internal fun planetCompassFloatingPressedScale(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true
): Float {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && pressed) PlanetCompassFloatingPressedTargetScale else 1f,
        animationSpec = tween(durationMillis = if (pressed) 90 else 140),
        label = "floatingButtonPressedScale"
    )
    return scale
}

/** Text actions and icons share the filled buttons' lighter red at night. */
internal val PlanetCompassDestructiveActionColor: Color
    @Composable get() = if (isSystemInDarkTheme())
        planetCompassDestructiveButtonContainerColor() else PlanetCompassDestructiveActionBaseColor

/** A quieter red reserved for the filled surface of destructive buttons.
 * Day text/icons keep their existing red; night text/icons reuse this surface red. */
@Composable
internal fun planetCompassDestructiveButtonContainerColor(): Color =
    if (isSystemInDarkTheme()) {
        Color(0xFFD94343)
    } else {
        Color(0xFFCA3434)
    }

/** Shared colors for every filled primary action, including a clearly visible
 * disabled state in both app themes. */
@Composable
internal fun planetCompassDisabledActionContainerColor(): Color =
    if (isSystemInDarkTheme()) {
        Color(0xFF6B6B74)
    } else {
        // Keep the disabled surface visually identical on white cards and on
        // the slightly tinted main background. A translucent Material color
        // was composited differently over the two containers.
        Color(0xFFE2E3E4)
    }

@Composable
internal fun planetCompassDisabledActionContentColor(): Color =
    if (isSystemInDarkTheme()) {
        Color(0xFFF4F4F6)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }

@Composable
internal fun planetCompassPrimaryButtonColors(): ButtonColors =
    ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        disabledContainerColor = planetCompassDisabledActionContainerColor(),
        disabledContentColor = planetCompassDisabledActionContentColor()
    )

/** Destructive filled actions use the same legible disabled treatment as
 * primary buttons and the shared red while enabled. */
@Composable
internal fun planetCompassDestructiveButtonColors(): ButtonColors =
    ButtonDefaults.buttonColors(
        containerColor = planetCompassDestructiveButtonContainerColor(),
        contentColor = Color.White,
        disabledContainerColor = planetCompassDisabledActionContainerColor(),
        disabledContentColor = planetCompassDisabledActionContentColor()
    )

/** Warning actions in settings stay red without a solid red surface. */
@Composable
internal fun planetCompassDestructiveOutlinedActionColor(): Color =
    if (isSystemInDarkTheme()) Color(0xFFFF6B6B)
    else PlanetCompassDestructiveActionBaseColor

@Composable
internal fun planetCompassDestructiveOutlinedButtonColors(): ButtonColors =
    ButtonDefaults.outlinedButtonColors(
        containerColor = planetCompassOutlinedRestingContainer(planetCompassSubtleOutlinedContainerColor(planetCompassDestructiveOutlinedActionColor())),
        contentColor = planetCompassDestructiveOutlinedActionColor(),
        disabledContainerColor = planetCompassOutlinedDisabledContainer(),
        disabledContentColor = planetCompassOutlinedDisabledContent()
    )

@Composable
@Suppress("UNUSED_PARAMETER")
internal fun planetCompassDestructiveOutlinedButtonBorder(
    enabled: Boolean,
    secondaryText: Color
): BorderStroke? = planetCompassOutlinedButtonBorder(enabled, planetCompassDestructiveOutlinedActionColor(), PlanetCompassDestructiveOutlinedBorderWidth)

/** Purple actions outside dialogs use the same quiet outlined treatment as
 * the red warning actions. The theme provides the correct day/night purple. */
@Composable
internal fun planetCompassPrimaryOutlinedButtonColors(): ButtonColors =
    ButtonDefaults.outlinedButtonColors(
        containerColor = planetCompassOutlinedRestingContainer(planetCompassSubtleOutlinedContainerColor(MaterialTheme.colorScheme.primary)),
        contentColor = MaterialTheme.colorScheme.primary,
        disabledContainerColor = planetCompassOutlinedDisabledContainer(),
        disabledContentColor = planetCompassOutlinedDisabledContent()
    )

@Composable
@Suppress("UNUSED_PARAMETER")
internal fun planetCompassPrimaryOutlinedButtonBorder(
    enabled: Boolean,
    secondaryText: Color
): BorderStroke? = planetCompassOutlinedButtonBorder(enabled, MaterialTheme.colorScheme.primary, 1.dp)

/** Settings/About use transparent active surfaces and a neutral disabled fill.
 * Other flows retain their existing palette. */
@Composable
internal fun planetCompassOutlinedActionColors(contentColor: Color): ButtonColors =
    ButtonDefaults.outlinedButtonColors(
        containerColor = planetCompassOutlinedRestingContainer(planetCompassNeutralOutlinedContainerColor(contentColor)),
        contentColor = contentColor,
        disabledContainerColor = planetCompassOutlinedDisabledContainer(),
        disabledContentColor = planetCompassOutlinedDisabledContent()
    )

/** A faint tint makes full-size outlined buttons recognizable without adding
 * visual weight to dialog text actions or toolbar icons. */
@Composable
internal fun planetCompassSubtleOutlinedContainerColor(color: Color): Color =
    color.copy(alpha = if (isSystemInDarkTheme()) 0.16f else 0.10f)

/** Neutral actions stay lighter than the opaque disabled surface in daylight. */
@Composable
internal fun planetCompassNeutralOutlinedContainerColor(color: Color): Color =
    color.copy(alpha = if (isSystemInDarkTheme()) 0.16f else 0.04f)

@Composable
private fun planetCompassQuietActionBorderColor(color: Color): Color =
    color.copy(alpha = if (isSystemInDarkTheme()) 0.38f else 0.30f)

@Composable
internal fun planetCompassOutlinedActionBorder(
    enabled: Boolean,
    secondaryText: Color
): BorderStroke? = planetCompassOutlinedButtonBorder(enabled, secondaryText, 1.dp)
