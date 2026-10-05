package me.mondiversi.spacecompass

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

internal val SpaceCompassDestructiveActionBaseColor = Color(0xFFD32F2F)
internal val SpaceCompassDestructiveOutlinedBorderWidth = 1.5.dp
internal const val SpaceCompassDestructiveOutlinedIconStrokeScale = 1.25f
/** Scoped to Settings/About bodies and data-export bottom buttons, never to dialog text actions or toolbars. */
internal val LocalSpaceCompassSettingsActionButtons = staticCompositionLocalOf { false }
internal val LocalSpaceCompassActionGlyphStrokeScale = staticCompositionLocalOf { 1f }

@Composable
private fun spaceCompassOutlinedRestingContainer(color: Color): Color =
    if (LocalSpaceCompassSettingsActionButtons.current) Color.Transparent else color

@Composable
private fun spaceCompassOutlinedDisabledContainer(): Color =
    if (LocalSpaceCompassSettingsActionButtons.current) {
        if (isSystemInDarkTheme()) Color(0xFF33343C) else Color(0xFFECEDEF)
    } else spaceCompassDisabledActionContainerColor()

@Composable
private fun spaceCompassOutlinedDisabledContent(): Color =
    if (LocalSpaceCompassSettingsActionButtons.current) {
        if (isSystemInDarkTheme()) Color(0xFFA1A5AE) else Color(0xFF808790)
    }
    else spaceCompassDisabledActionContentColor()

@Composable
private fun spaceCompassOutlinedButtonBorder(
    enabled: Boolean,
    color: Color,
    defaultWidth: Dp
): BorderStroke? = if (LocalSpaceCompassSettingsActionButtons.current) {
    val borderColor = if (enabled) {
        color.copy(alpha = if (isSystemInDarkTheme()) 0.48f else 0.38f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = if (isSystemInDarkTheme()) 0.20f else 0.12f)
    }
    BorderStroke(1.dp, borderColor)
} else if (enabled) {
    BorderStroke(defaultWidth, spaceCompassQuietActionBorderColor(color))
} else null

internal const val SpaceCompassFloatingPressedTargetScale = 1.11f

/** Visual feedback only: the control's layout and touch target do not change. */
@Composable
internal fun spaceCompassFloatingPressedScale(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true
): Float {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && pressed) SpaceCompassFloatingPressedTargetScale else 1f,
        animationSpec = tween(durationMillis = if (pressed) 90 else 140),
        label = "floatingButtonPressedScale"
    )
    return scale
}

/** Text actions and icons share the filled buttons' lighter red at night. */
internal val SpaceCompassDestructiveActionColor: Color
    @Composable get() = if (isSystemInDarkTheme())
        spaceCompassDestructiveButtonContainerColor() else SpaceCompassDestructiveActionBaseColor

/** A quieter red reserved for the filled surface of destructive buttons.
 * Day text/icons keep their existing red; night text/icons reuse this surface red. */
@Composable
internal fun spaceCompassDestructiveButtonContainerColor(): Color =
    if (isSystemInDarkTheme()) {
        Color(0xFFD94343)
    } else {
        Color(0xFFCA3434)
    }

/** Shared colors for every filled primary action, including a clearly visible
 * disabled state in both app themes. */
@Composable
internal fun spaceCompassDisabledActionContainerColor(): Color =
    if (isSystemInDarkTheme()) {
        Color(0xFF6B6B74)
    } else {
        // Keep the disabled surface visually identical on white cards and on
        // the slightly tinted main background. A translucent Material color
        // was composited differently over the two containers.
        Color(0xFFE2E3E4)
    }

@Composable
internal fun spaceCompassDisabledActionContentColor(): Color =
    if (isSystemInDarkTheme()) {
        Color(0xFFF4F4F6)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }

@Composable
internal fun spaceCompassPrimaryButtonColors(): ButtonColors =
    ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        disabledContainerColor = spaceCompassDisabledActionContainerColor(),
        disabledContentColor = spaceCompassDisabledActionContentColor()
    )

/** Destructive filled actions use the same legible disabled treatment as
 * primary buttons and the shared red while enabled. */
@Composable
internal fun spaceCompassDestructiveButtonColors(): ButtonColors =
    ButtonDefaults.buttonColors(
        containerColor = spaceCompassDestructiveButtonContainerColor(),
        contentColor = Color.White,
        disabledContainerColor = spaceCompassDisabledActionContainerColor(),
        disabledContentColor = spaceCompassDisabledActionContentColor()
    )

/** Warning actions in settings stay red without a solid red surface. */
@Composable
internal fun spaceCompassDestructiveOutlinedActionColor(): Color =
    if (isSystemInDarkTheme()) Color(0xFFFF6B6B)
    else SpaceCompassDestructiveActionBaseColor

@Composable
internal fun spaceCompassDestructiveOutlinedButtonColors(): ButtonColors =
    ButtonDefaults.outlinedButtonColors(
        containerColor = spaceCompassOutlinedRestingContainer(spaceCompassSubtleOutlinedContainerColor(spaceCompassDestructiveOutlinedActionColor())),
        contentColor = spaceCompassDestructiveOutlinedActionColor(),
        disabledContainerColor = spaceCompassOutlinedDisabledContainer(),
        disabledContentColor = spaceCompassOutlinedDisabledContent()
    )

@Composable
@Suppress("UNUSED_PARAMETER")
internal fun spaceCompassDestructiveOutlinedButtonBorder(
    enabled: Boolean,
    secondaryText: Color
): BorderStroke? = spaceCompassOutlinedButtonBorder(enabled, spaceCompassDestructiveOutlinedActionColor(), SpaceCompassDestructiveOutlinedBorderWidth)

/** Purple actions outside dialogs use the same quiet outlined treatment as
 * the red warning actions. The theme provides the correct day/night purple. */
@Composable
internal fun spaceCompassPrimaryOutlinedButtonColors(): ButtonColors =
    ButtonDefaults.outlinedButtonColors(
        containerColor = spaceCompassOutlinedRestingContainer(spaceCompassSubtleOutlinedContainerColor(MaterialTheme.colorScheme.primary)),
        contentColor = MaterialTheme.colorScheme.primary,
        disabledContainerColor = spaceCompassOutlinedDisabledContainer(),
        disabledContentColor = spaceCompassOutlinedDisabledContent()
    )

@Composable
@Suppress("UNUSED_PARAMETER")
internal fun spaceCompassPrimaryOutlinedButtonBorder(
    enabled: Boolean,
    secondaryText: Color
): BorderStroke? = spaceCompassOutlinedButtonBorder(enabled, MaterialTheme.colorScheme.primary, 1.dp)

/** Settings/About use transparent active surfaces and a neutral disabled fill.
 * Other flows retain their existing palette. */
@Composable
internal fun spaceCompassOutlinedActionColors(contentColor: Color): ButtonColors =
    ButtonDefaults.outlinedButtonColors(
        containerColor = spaceCompassOutlinedRestingContainer(spaceCompassNeutralOutlinedContainerColor(contentColor)),
        contentColor = contentColor,
        disabledContainerColor = spaceCompassOutlinedDisabledContainer(),
        disabledContentColor = spaceCompassOutlinedDisabledContent()
    )

/** A faint tint makes full-size outlined buttons recognizable without adding
 * visual weight to dialog text actions or toolbar icons. */
@Composable
internal fun spaceCompassSubtleOutlinedContainerColor(color: Color): Color =
    color.copy(alpha = if (isSystemInDarkTheme()) 0.16f else 0.10f)

/** Neutral actions stay lighter than the opaque disabled surface in daylight. */
@Composable
internal fun spaceCompassNeutralOutlinedContainerColor(color: Color): Color =
    color.copy(alpha = if (isSystemInDarkTheme()) 0.16f else 0.04f)

@Composable
private fun spaceCompassQuietActionBorderColor(color: Color): Color =
    color.copy(alpha = if (isSystemInDarkTheme()) 0.38f else 0.30f)

@Composable
internal fun spaceCompassOutlinedActionBorder(
    enabled: Boolean,
    secondaryText: Color
): BorderStroke? = spaceCompassOutlinedButtonBorder(enabled, secondaryText, 1.dp)
